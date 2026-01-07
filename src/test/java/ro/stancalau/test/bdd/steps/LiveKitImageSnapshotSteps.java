package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.util.EgressTestUtils;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.EgressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.MinIOContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.ImageValidationUtils;
import ro.stancalau.test.framework.util.MinioS3Client;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;

@Slf4j
public class LiveKitImageSnapshotSteps {

    private String currentScenarioLogPath;

    @Before
    public void setUpImageSnapshotSteps(Scenario scenario) {
        String featureName =
                ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();

        String sanitizedFeatureName = FileUtils.sanitizeFileName(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileName(scenarioName);

        currentScenarioLogPath = PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
    }

    @When(
            "an on-demand snapshot is captured to S3 for room {string} using LiveKit service {string} and MinIO service {string}")
    public void captureRoomSnapshotToS3(String roomName, String livekitServiceName, String minioServiceName)
            throws Exception {
        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "snapshot-room-" + roomName + "-" + timestamp;

        LivekitEgress.ImageOutput imageOutput = createS3ImageOutput(fileName, minioServiceName);
        captureRoomSnapshot(
                roomName,
                livekitServiceName,
                imageOutput,
                fileName,
                () -> cleanupBlackFramesFromS3("snapshots", fileName, minioServiceName),
                roomName);
    }

    @When(
            "an on-demand snapshot is captured to S3 for participant {string} video track in room {string} using LiveKit service {string} and MinIO service {string}")
    public void captureParticipantTrackSnapshotToS3(
            String participantIdentity, String roomName, String livekitServiceName, String minioServiceName)
            throws Exception {
        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "snapshot-track-" + participantIdentity + "-" + timestamp;

        LivekitEgress.ImageOutput imageOutput = createS3ImageOutput(fileName, minioServiceName);
        captureParticipantTrackSnapshot(
                participantIdentity,
                roomName,
                livekitServiceName,
                imageOutput,
                fileName,
                () -> cleanupBlackFramesFromS3("snapshots", fileName, minioServiceName),
                participantIdentity + "_track");
    }

    @Then("the S3 snapshot image file exists for room {string} using MinIO service {string}")
    public void verifyRoomSnapshotExistsInS3(String roomName, String minioServiceName) throws Exception {
        verifyS3SnapshotExists(roomName, "No snapshot recorded for room " + roomName, minioServiceName);
    }

    @Then("the S3 snapshot image file exists for participant {string} using MinIO service {string}")
    public void verifyParticipantSnapshotExistsInS3(String participantIdentity, String minioServiceName)
            throws Exception {
        verifyS3SnapshotExists(
                participantIdentity + "_track",
                "No track snapshot recorded for participant " + participantIdentity,
                minioServiceName);
    }

    @And("the S3 snapshot image is valid and contains actual image data")
    public void verifyS3SnapshotIsValidImage() throws Exception {
        validateSnapshotImage("snapshot-", ".jpg", "Snapshot");
    }

    private void captureTrackIds(String participantIdentity, String roomName, String livekitServiceName)
            throws Exception {
        RoomServiceClient roomClient = ManagerProvider.getRoomClientManager().getRoomServiceClient(livekitServiceName);

        List<LivekitModels.ParticipantInfo> participants =
                roomClient.listParticipants(roomName).execute().body();

        assertNotNull(participants, "Participants list should not be null");

        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> p.getIdentity().equals(participantIdentity))
                .findFirst()
                .orElse(null);

        assertNotNull(targetParticipant, "Participant " + participantIdentity + " not found in room " + roomName);

        Map<String, String> trackIds = new HashMap<>();

        for (LivekitModels.TrackInfo track : targetParticipant.getTracksList()) {
            String trackType = track.getType().name().toLowerCase();
            String trackId = track.getSid();
            trackIds.put(trackType, trackId);
            log.info("Captured track ID for participant {}: {} track = {}", participantIdentity, trackType, trackId);
        }

        ManagerProvider.getEgressStateManager().storeTrackIds(participantIdentity, trackIds);
        assertTrue(trackIds.containsKey("video"), "Video track not found for participant " + participantIdentity);
    }

    private String getCurrentScenarioLogPath() {
        return currentScenarioLogPath != null ? currentScenarioLogPath : PathUtils.currentScenarioPath();
    }

    private void captureRoomSnapshot(
            String roomName,
            String livekitServiceName,
            LivekitEgress.ImageOutput imageOutput,
            String fileName,
            Runnable cleanupAction,
            String stateKey)
            throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);

        log.info(
                "Capturing on-demand snapshot for room: {} using LiveKit server: {}",
                roomName,
                liveKitContainer.getWsUrl());

        log.info("Waiting for video streams to stabilize and become visible in room composite...");
        TimeUnit.SECONDS.sleep(5);

        RoomServiceClient roomClient = ManagerProvider.getRoomClientManager().getRoomServiceClient(livekitServiceName);
        List<LivekitModels.ParticipantInfo> participants =
                roomClient.listParticipants(roomName).execute().body();
        boolean hasVideoTrack = participants.stream().anyMatch(p -> p.getTracksList().stream()
                .anyMatch(track -> track.getType() == LivekitModels.TrackType.VIDEO));
        log.info("Room {} has {} participants with video tracks: {}", roomName, participants.size(), hasVideoTrack);

        String layout = participants.size() == 1 ? "speaker" : "grid";
        log.info("Using layout '{}' for {} participants", layout, participants.size());

        LivekitEgress.EgressInfo egressInfo;
        try {
            retrofit2.Response<LivekitEgress.EgressInfo> response = egressClient
                    .startRoomCompositeEgress(roomName, imageOutput, layout)
                    .execute();
            log.info("Snapshot egress API HTTP response code: {}, message: {}", response.code(), response.message());
            if (!response.isSuccessful()) {
                log.error(
                        "Snapshot egress API failed with code: {}, error body: {}",
                        response.code(),
                        response.errorBody() != null ? response.errorBody().string() : "null");
            }
            egressInfo = response.body();
            log.info("Image snapshot egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Image snapshot egress should have started");
        } catch (Exception e) {
            log.error("Failed to start image snapshot egress", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();

        log.info("Waiting for snapshot egress to become active and start capturing...");
        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);

        log.info("Egress is active, capturing single frame...");
        TimeUnit.SECONDS.sleep(1);

        try {
            egressClient.stopEgress(egressId).execute().body();
            log.info("Stopped image snapshot egress with ID: {}", egressId);
        } catch (Exception e) {
            log.warn("Failed to stop snapshot egress (may have completed automatically)", e);
        }

        log.info("Waiting for file processing to complete...");
        TimeUnit.SECONDS.sleep(1);

        cleanupAction.run();

        ManagerProvider.getImageSnapshotStateManager().storeCapturedSnapshot(stateKey, fileName + ".jpg");
        log.info("Captured on-demand snapshot for room {} with filename: {}", roomName, fileName);
    }

    private void captureParticipantTrackSnapshot(
            String participantIdentity,
            String roomName,
            String livekitServiceName,
            LivekitEgress.ImageOutput imageOutput,
            String fileName,
            Runnable cleanupAction,
            String stateKey)
            throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        Map<String, String> trackIds = ManagerProvider.getEgressStateManager().getTrackIds(participantIdentity);
        if (trackIds == null) {
            captureTrackIds(participantIdentity, roomName, livekitServiceName);
            trackIds = ManagerProvider.getEgressStateManager().getTrackIds(participantIdentity);
        }

        assertNotNull(trackIds, "No track IDs found for participant " + participantIdentity);
        String videoTrackId = trackIds.get("video");
        assertNotNull(videoTrackId, "No video track ID found for participant " + participantIdentity);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);

        log.info(
                "Capturing on-demand snapshot for participant: {} (video track: {}) in room: {} using LiveKit server: {}",
                participantIdentity,
                videoTrackId,
                roomName,
                liveKitContainer.getWsUrl());
        log.info("Waiting for video track to stabilize...");
        TimeUnit.SECONDS.sleep(2);

        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient
                    .startTrackCompositeEgress(roomName, imageOutput, null, videoTrackId)
                    .execute()
                    .body();
            log.info("Track image snapshot egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Track image snapshot egress should have started");
        } catch (Exception e) {
            log.error("Failed to start track snapshot egress", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();

        log.info("Waiting for track image capture to complete...");
        TimeUnit.SECONDS.sleep(3);

        try {
            egressClient.stopEgress(egressId).execute().body();
            log.info("Stopped track snapshot egress with ID: {}", egressId);
        } catch (Exception e) {
            log.warn("Failed to stop egress (may have completed automatically)", e);
        }

        cleanupAction.run();

        ManagerProvider.getImageSnapshotStateManager().storeCapturedSnapshot(stateKey, fileName + ".jpg");
        log.info(
                "Captured on-demand track snapshot for participant {} with filename: {}",
                participantIdentity,
                fileName);
    }

    private void verifyS3SnapshotExists(String stateKey, String errorMessage, String minioServiceName)
            throws Exception {
        String expectedFileName = ManagerProvider.getImageSnapshotStateManager().getCapturedSnapshot(stateKey);
        assertNotNull(expectedFileName, errorMessage);

        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);

        MinioS3Client s3Client = new MinioS3Client(
                minioContainer.getS3EndpointUrl(),
                minioContainer.getAccessKey(),
                minioContainer.getSecretKey(),
                "snapshots");

        File snapshotsDir = new File(getCurrentScenarioLogPath(), "snapshots");
        snapshotsDir.mkdirs();

        String expectedFilePrefix = expectedFileName.replace(".jpg", "_");

        int maxAttempts = 20;
        int attempt = 0;
        File downloadedFile = null;

        while (attempt < maxAttempts && downloadedFile == null) {
            log.debug("Checking for snapshot files in S3 bucket... (attempt {}/{})", attempt + 1, maxAttempts);

            List<String> objectNames = s3Client.listObjects("");
            String bestObjectName = null;
            long maxSize = 0;

            // Find the largest file (which should contain actual content)
            for (String objectName : objectNames) {
                if (objectName.startsWith(expectedFilePrefix) && objectName.endsWith(".jpeg")) {
                    long fileSize = s3Client.getObjectSize(objectName);
                    if (fileSize > maxSize) {
                        maxSize = fileSize;
                        bestObjectName = objectName;
                    }
                }
            }

            if (bestObjectName != null) {
                String localFileName = bestObjectName.replace(".jpeg", ".jpg");
                File localFile = new File(snapshotsDir, localFileName);

                s3Client.downloadObject(bestObjectName, localFile.getAbsolutePath());
                downloadedFile = localFile;
                log.info(
                        "Downloaded best snapshot image from S3: {} -> {} (size: {} bytes)",
                        bestObjectName,
                        localFile.getAbsolutePath(),
                        maxSize);
                break;
            }

            if (downloadedFile == null && attempt < maxAttempts - 1) {
                TimeUnit.SECONDS.sleep(2);
            }
            attempt++;
        }

        assertNotNull(downloadedFile, "No snapshot image files found in S3 bucket for prefix: " + expectedFilePrefix);
        assertTrue(
                downloadedFile.exists(),
                "Downloaded snapshot file does not exist: " + downloadedFile.getAbsolutePath());
        assertTrue(
                downloadedFile.length() > 0, "Downloaded snapshot file is empty: " + downloadedFile.getAbsolutePath());

        log.info(
                "Snapshot file found and downloaded: {} (size: {} bytes)",
                downloadedFile.getAbsolutePath(),
                downloadedFile.length());
    }

    @Given(
            "a LiveKit S3 snapshot egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitS3SnapshotEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
        createEgressContainer(serviceName, livekitServiceName, EgressContainer::createContainer, "S3 snapshot");
    }

    @Given(
            "a LiveKit snapshot egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitSnapshotEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
        createEgressContainer(serviceName, livekitServiceName, EgressContainer::createContainer, "Snapshot");
    }

    @When("an on-demand snapshot is captured to local filesystem for room {string} using LiveKit service {string}")
    public void captureRoomSnapshotToLocalFilesystem(String roomName, String livekitServiceName) throws Exception {
        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "local-snapshot-room-" + roomName + "-" + timestamp;

        LivekitEgress.ImageOutput imageOutput =
                createLocalImageOutput(PathUtils.containerPath("/out/snapshots", fileName));
        captureRoomSnapshot(
                roomName,
                livekitServiceName,
                imageOutput,
                fileName,
                () -> cleanupLocalBlackFrames(fileName),
                roomName + "_local");
    }

    @When(
            "an on-demand snapshot is captured to local filesystem for participant {string} video track in room {string} using LiveKit service {string}")
    public void captureParticipantTrackSnapshotToLocalFilesystem(
            String participantIdentity, String roomName, String livekitServiceName) throws Exception {
        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "local-snapshot-track-" + participantIdentity + "-" + timestamp;

        LivekitEgress.ImageOutput imageOutput =
                createLocalImageOutput(PathUtils.containerPath("/out/snapshots", fileName));
        captureParticipantTrackSnapshot(
                participantIdentity,
                roomName,
                livekitServiceName,
                imageOutput,
                fileName,
                () -> cleanupLocalBlackFrames(fileName),
                participantIdentity + "_track_local");
    }

    @Then("the local snapshot image file exists for room {string}")
    public void verifyLocalRoomSnapshotExists(String roomName) throws Exception {
        String expectedFileName =
                ManagerProvider.getImageSnapshotStateManager().getCapturedSnapshot(roomName + "_local");
        assertNotNull(expectedFileName, "No local snapshot recorded for room " + roomName);

        String snapshotsPath = PathUtils.join(getCurrentScenarioLogPath(), "snapshots");
        File snapshotsDir = new File(snapshotsPath);
        snapshotsDir.mkdirs();

        String expectedFilePrefix = expectedFileName.replace(".jpg", "_");

        int maxAttempts = 20;
        int attempt = 0;
        File bestFile = null;
        long maxSize = 0;

        while (attempt < maxAttempts && bestFile == null) {
            log.debug("Checking for local snapshot files... (attempt {}/{})", attempt + 1, maxAttempts);

            File[] snapshotFiles = snapshotsDir.listFiles((dir, name) ->
                    name.startsWith(expectedFilePrefix.substring(expectedFilePrefix.lastIndexOf("/") + 1))
                            && (name.endsWith(".jpeg") || name.endsWith(".jpg")));

            if (snapshotFiles != null) {
                // Find the largest file (which should contain actual content)
                for (File file : snapshotFiles) {
                    if (file.length() > maxSize) {
                        maxSize = file.length();
                        bestFile = file;
                    }
                }
            }

            if (bestFile == null && attempt < maxAttempts - 1) {
                TimeUnit.SECONDS.sleep(2);
            }
            attempt++;
        }

        assertNotNull(bestFile, "No local snapshot image files found for prefix: " + expectedFilePrefix);
        assertTrue(bestFile.exists(), "Local snapshot file does not exist: " + bestFile.getAbsolutePath());
        assertTrue(bestFile.length() > 0, "Local snapshot file is empty: " + bestFile.getAbsolutePath());

        log.info("Local room snapshot file found: {} (size: {} bytes)", bestFile.getAbsolutePath(), bestFile.length());
    }

    @Then("the local snapshot image file exists for participant {string}")
    public void verifyLocalParticipantSnapshotExists(String participantIdentity) throws Exception {
        String expectedFileName = ManagerProvider.getImageSnapshotStateManager()
                .getCapturedSnapshot(participantIdentity + "_track_local");
        assertNotNull(expectedFileName, "No local track snapshot recorded for participant " + participantIdentity);

        String snapshotsPath = PathUtils.join(getCurrentScenarioLogPath(), "snapshots");
        File snapshotsDir = new File(snapshotsPath);
        snapshotsDir.mkdirs();

        String expectedFilePrefix = expectedFileName.replace(".jpg", "_");

        int maxAttempts = 20;
        int attempt = 0;
        File bestFile = null;
        long maxSize = 0;

        while (attempt < maxAttempts && bestFile == null) {
            log.debug(
                    "Checking for local participant track snapshot files... (attempt {}/{})", attempt + 1, maxAttempts);

            File[] snapshotFiles = snapshotsDir.listFiles((dir, name) ->
                    name.startsWith(expectedFilePrefix.substring(expectedFilePrefix.lastIndexOf("/") + 1))
                            && (name.endsWith(".jpeg") || name.endsWith(".jpg")));

            if (snapshotFiles != null) {
                // Find the largest file (which should contain actual content)
                for (File file : snapshotFiles) {
                    if (file.length() > maxSize) {
                        maxSize = file.length();
                        bestFile = file;
                    }
                }
            }

            if (bestFile == null && attempt < maxAttempts - 1) {
                TimeUnit.SECONDS.sleep(2);
            }
            attempt++;
        }

        assertNotNull(
                bestFile, "No local participant track snapshot image files found for prefix: " + expectedFilePrefix);
        assertTrue(
                bestFile.exists(),
                "Local participant track snapshot file does not exist: " + bestFile.getAbsolutePath());
        assertTrue(
                bestFile.length() > 0, "Local participant track snapshot file is empty: " + bestFile.getAbsolutePath());

        log.info(
                "Local participant track snapshot file found: {} (size: {} bytes)",
                bestFile.getAbsolutePath(),
                bestFile.length());
    }

    @And("the local snapshot image is valid and contains actual image data")
    public void verifyLocalSnapshotIsValidImage() throws Exception {
        validateSnapshotImage("local-snapshot-", ".jpg,.jpeg", "Local snapshot");
    }

    private void cleanupLocalBlackFrames(String filePrefix) {
        try {
            String snapshotsPath = getCurrentScenarioLogPath() + "/snapshots";
            File snapshotsDir = new File(snapshotsPath);
            snapshotsDir.mkdirs();

            String expectedFilePrefix = filePrefix + "_";

            File[] snapshotFiles = snapshotsDir.listFiles((dir, name) ->
                    name.startsWith(expectedFilePrefix.substring(expectedFilePrefix.lastIndexOf("/") + 1))
                            && (name.endsWith(".jpeg") || name.endsWith(".jpg")));

            if (snapshotFiles == null || snapshotFiles.length <= 1) {
                log.info(
                        "No duplicate local frames to cleanup (found {} files)",
                        snapshotFiles != null ? snapshotFiles.length : 0);
                return;
            }

            long largestSize = 0;
            File largestFile = null;
            List<File> filesToDelete = new ArrayList<>();

            for (File file : snapshotFiles) {
                long fileSize = file.length();

                if (fileSize > largestSize) {
                    if (largestFile != null) {
                        filesToDelete.add(largestFile);
                    }
                    largestSize = fileSize;
                    largestFile = file;
                } else {
                    filesToDelete.add(file);
                }
            }

            for (File fileToDelete : filesToDelete) {
                log.info(
                        "Deleting smaller/duplicate local frame: {} (size: {} bytes)",
                        fileToDelete.getName(),
                        fileToDelete.length());
                fileToDelete.delete();
            }

        } catch (Exception e) {
            log.warn("Failed to cleanup local black frames: {}", e.getMessage());
        }
    }

    private void cleanupBlackFramesFromS3(String bucketName, String filePrefix, String minioServiceName) {
        try {
            ContainerStateManager containerManager = ManagerProvider.getContainerManager();
            MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);

            MinioS3Client s3Client = new MinioS3Client(
                    minioContainer.getS3EndpointUrl(),
                    minioContainer.getAccessKey(),
                    minioContainer.getSecretKey(),
                    bucketName);

            List<String> objectNames = s3Client.listObjects("");
            String expectedFilePrefix = filePrefix + "_";

            long largestSize = 0;
            String largestFile = null;
            List<String> filesToDelete = new ArrayList<>();

            for (String objectName : objectNames) {
                if (objectName.startsWith(expectedFilePrefix)
                        && (objectName.endsWith(".jpeg") || objectName.endsWith(".jpg"))) {
                    long fileSize = s3Client.getObjectSize(objectName);

                    if (fileSize > largestSize) {
                        if (largestFile != null) {
                            filesToDelete.add(largestFile);
                        }
                        largestSize = fileSize;
                        largestFile = objectName;
                    } else {
                        filesToDelete.add(objectName);
                    }
                }
            }

            for (String objectToDelete : filesToDelete) {
                long size = s3Client.getObjectSize(objectToDelete);
                log.info("Deleting smaller/duplicate frame from S3: {} (size: {} bytes)", objectToDelete, size);
                s3Client.deleteObject(objectToDelete);
            }

            s3Client.close();
        } catch (Exception e) {
            log.warn("Failed to cleanup black frames from S3: {}", e.getMessage());
        }
    }

    private LivekitEgress.ImageOutput createS3ImageOutput(String fileName, String minioServiceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);

        LivekitEgress.S3Upload s3Upload = LivekitEgress.S3Upload.newBuilder()
                .setBucket("snapshots")
                .setAccessKey(minioContainer.getAccessKey())
                .setSecret(minioContainer.getSecretKey())
                .setEndpoint(minioContainer.getNetworkS3EndpointUrl())
                .setRegion("us-east-1")
                .setForcePathStyle(true)
                .build();

        return createBaseImageOutput(fileName).setS3(s3Upload).build();
    }

    private LivekitEgress.ImageOutput createLocalImageOutput(String filePath) {
        return createBaseImageOutput(filePath).build();
    }

    private LivekitEgress.ImageOutput.Builder createBaseImageOutput(String filenamePrefix) {
        return LivekitEgress.ImageOutput.newBuilder()
                .setCaptureInterval(0)
                .setWidth(1920)
                .setHeight(1080)
                .setFilenamePrefix(filenamePrefix)
                .setFilenameSuffix(LivekitEgress.ImageFileSuffix.IMAGE_SUFFIX_INDEX)
                .setImageCodec(LivekitModels.ImageCodec.IC_JPEG)
                .setDisableManifest(true);
    }

    private void validateSnapshotImage(String prefix, String extensions, String type) throws Exception {
        File snapshotsDir = new File(getCurrentScenarioLogPath(), "snapshots");
        assertTrue(
                snapshotsDir.exists() && snapshotsDir.isDirectory(),
                type + "s directory does not exist: " + snapshotsDir.getAbsolutePath());

        String[] exts = extensions.split(",");
        File[] snapshotFiles = snapshotsDir.listFiles((dir, name) -> {
            if (!name.startsWith(prefix)) return false;
            for (String ext : exts) {
                if (name.endsWith(ext.trim())) return true;
            }
            return false;
        });

        assertNotNull(
                snapshotFiles,
                "No " + type.toLowerCase() + " image files found in directory: " + snapshotsDir.getAbsolutePath());
        assertTrue(
                snapshotFiles.length > 0,
                "No " + type.toLowerCase() + " image files found in directory: " + snapshotsDir.getAbsolutePath());

        File snapshotFile = snapshotFiles[0];

        assertTrue(
                ImageValidationUtils.isValidImage(snapshotFile),
                type + " file is not a valid image: " + snapshotFile.getAbsolutePath());
        assertTrue(
                ImageValidationUtils.hasMinimumSize(snapshotFile, 1000),
                type + " image file too small (< 1KB), likely corrupted or empty: " + snapshotFile.getAbsolutePath());
        assertTrue(
                ImageValidationUtils.hasValidDimensions(snapshotFile, 100, 100),
                type + " image has invalid dimensions: " + snapshotFile.getAbsolutePath());

        log.info(
                "Verified " + type.toLowerCase() + " image file exists and is valid: {} ({} bytes)",
                snapshotFile.getAbsolutePath(),
                snapshotFile.length());
    }

    @FunctionalInterface
    private interface EgressContainerFactory {
        EgressContainer create(
                String alias,
                org.testcontainers.containers.Network network,
                String egressVersion,
                String livekitWsUrl,
                String apiKey,
                String apiSecret,
                String configFilePath,
                String logDestinationPath,
                String redisUrl);
    }

    private void createEgressContainer(
            String serviceName, String livekitServiceName, EgressContainerFactory factory, String containerType) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        RedisContainer redisContainer = containerManager.getContainer("redis", RedisContainer.class);
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        String livekitWsUrl = liveKitContainer.getNetworkUrl();
        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        EgressContainer egressContainer = factory.create(
                serviceName,
                containerManager.getOrCreateNetwork(),
                TestConfig.getEgressVersion(),
                livekitWsUrl,
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET,
                null,
                serviceLogPath,
                redisContainer.getNetworkRedisUrl());

        egressContainer.start();
        assertTrue(
                egressContainer.isRunning(),
                containerType + " egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("{} egress service {} started and registered", containerType, serviceName);
    }
}
