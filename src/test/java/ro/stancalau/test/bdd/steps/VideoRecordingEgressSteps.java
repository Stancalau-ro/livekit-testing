package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.util.EgressTestUtils;
import ro.stancalau.test.framework.config.S3Config;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.EgressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.MinIOContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.util.BrowserPollingHelper;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.MinioS3Client;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;

@Slf4j
public class VideoRecordingEgressSteps {

    private final Map<String, MinioS3Client> s3Clients = new HashMap<>();
    private String currentScenarioLogPath;

    @Before
    public void setUpVideoRecordingEgressSteps(Scenario scenario) {
        String featureName =
                ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();

        String sanitizedFeatureName = FileUtils.sanitizeFileName(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileName(scenarioName);

        currentScenarioLogPath = PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
    }

    @After
    public void tearDownVideoRecordingEgressSteps() {
        if (currentScenarioLogPath != null) {
            String s3ExportPath = currentScenarioLogPath + "/minio-exports";
            for (MinioS3Client client : s3Clients.values()) {
                try {
                    client.exportBucketContents(s3ExportPath);
                    client.close();
                } catch (Exception e) {
                    log.error("Failed to export/close S3 client", e);
                }
            }
        }
        s3Clients.clear();
    }

    @Given("a Redis server is running in a container with service name {string}")
    public void aRedisServerIsRunningInContainer(String serviceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        RedisContainer redisContainer =
                RedisContainer.createContainer(serviceName, containerManager.getOrCreateNetwork(), serviceLogPath);

        redisContainer.start();
        assertTrue(
                redisContainer.isRunning(), "Redis container with service name " + serviceName + " should be running");
        containerManager.registerContainer(serviceName, redisContainer);
        log.info("Redis service {} started for egress communication", serviceName);
    }

    @Given("a MinIO server is running in a container with service name {string}")
    public void aMinIOServerIsRunningInContainer(String serviceName) {
        aMinIOServerIsRunningInContainerWithCredentials(
                serviceName, MinIOContainer.DEFAULT_ACCESS_KEY, MinIOContainer.DEFAULT_SECRET_KEY);
    }

    @Given(
            "a MinIO server is running in a container with service name {string} with access key {string} and secret key {string}")
    public void aMinIOServerIsRunningInContainerWithCredentials(
            String serviceName, String accessKey, String secretKey) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        MinIOContainer minioContainer = MinIOContainer.createContainer(
                serviceName, containerManager.getOrCreateNetwork(), accessKey, secretKey, serviceLogPath);

        minioContainer.start();
        assertTrue(
                minioContainer.isRunning(), "MinIO container with service name " + serviceName + " should be running");
        containerManager.registerContainer(serviceName, minioContainer);
        log.info("MinIO service {} started with endpoint: {}", serviceName, minioContainer.getS3EndpointUrl());
    }

    @Given("a bucket {string} is created in MinIO service {string}")
    public void bucketIsCreatedInMinIO(String bucketName, String minioServiceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);

        MinioS3Client s3Client = getOrCreateS3Client(minioServiceName, minioContainer, bucketName);
        s3Client.createBucket();

        log.info("Created bucket '{}' in MinIO service '{}'", bucketName, minioServiceName);
    }

    @Given(
            "a LiveKit video recording egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitVideoRecordingEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
        createEgressContainer(serviceName, livekitServiceName, null);
    }

    @Given(
            "a LiveKit egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
        createEgressContainer(serviceName, livekitServiceName, null);
    }

    @Given(
            "a LiveKit egress service is running in a container with service name {string} connected to LiveKit service {string} with S3 output to MinIO service {string}")
    public void aLiveKitEgressServiceWithS3IsRunningInContainer(
            String serviceName, String livekitServiceName, String minioServiceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);

        S3Config s3Config = new S3Config(
                minioContainer.getNetworkS3EndpointUrl(),
                minioContainer.getAccessKey(),
                minioContainer.getSecretKey(),
                "recordings");

        createEgressContainerWithS3(serviceName, livekitServiceName, s3Config);
    }

    @Given("track IDs are captured for participant {string} in room {string} using LiveKit service {string}")
    public void captureTrackIds(String participantIdentity, String roomName, String livekitServiceName)
            throws Exception {
        RoomServiceClient roomClient = ManagerProvider.getRoomClientManager().getRoomServiceClient(livekitServiceName);

        boolean tracksFound = BrowserPollingHelper.pollForCondition(() -> {
            try {
                List<LivekitModels.ParticipantInfo> participants =
                        roomClient.listParticipants(roomName).execute().body();
                if (participants == null) return false;

                LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                        .filter(p -> p.getIdentity().equals(participantIdentity))
                        .findFirst()
                        .orElse(null);

                if (targetParticipant == null) return false;

                boolean hasAudio = targetParticipant.getTracksList().stream()
                        .anyMatch(t -> t.getType() == LivekitModels.TrackType.AUDIO);
                boolean hasVideo = targetParticipant.getTracksList().stream()
                        .anyMatch(t -> t.getType() == LivekitModels.TrackType.VIDEO);

                return hasAudio && hasVideo;
            } catch (Exception e) {
                log.warn("Error checking tracks for participant {}: {}", participantIdentity, e.getMessage());
                return false;
            }
        });

        assertTrue(
                tracksFound,
                "Audio and video tracks not found for participant " + participantIdentity + " within timeout");

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
    }

    @When("the system starts room composite recording for room {string} using LiveKit service {string}")
    public void startRoomCompositeRecording(String roomName, String livekitServiceName) throws Exception {
        startRoomCompositeRecordingToLocal(roomName, livekitServiceName);
    }

    @When(
            "the system starts room composite recording for room {string} using LiveKit service {string} with S3 output to bucket {string}")
    public void startRoomCompositeRecordingWithS3(String roomName, String livekitServiceName, String bucketName)
            throws Exception {
        startRoomCompositeRecordingWithS3AndPrefix(roomName, livekitServiceName, bucketName, "");
    }

    @When(
            "the system starts room composite recording for room {string} using LiveKit service {string} with S3 output to bucket {string} with prefix {string}")
    public void startRoomCompositeRecordingWithS3AndPrefix(
            String roomName, String livekitServiceName, String bucketName, String prefix) throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);

        EgressServiceClient egressClient = createEgressClient(liveKitContainer);

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = prefix + "recording-" + roomName + "-" + timestamp + ".mp4";

        LivekitEgress.S3Upload s3Upload = createS3Upload(minioContainer, bucketName, roomName, timestamp);

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath(fileName)
                .setS3(s3Upload)
                .build();

        log.info(
                "Starting room composite recording for room: {} to S3 bucket: {} with key: {}",
                roomName,
                bucketName,
                fileName);

        startEgressRecording(egressClient, roomName, fileOutput, "S3");
    }

    @When("the recording runs for {int} seconds")
    public void recordingRunsForDuration(int seconds) throws InterruptedException {
        log.info("Allowing recording to capture for {} seconds", seconds);
        TimeUnit.SECONDS.sleep(seconds);
    }

    @When(
            "the system starts track composite recording for participant {string} in room {string} using LiveKit service {string}")
    public void startTrackCompositeRecording(String participantIdentity, String roomName, String livekitServiceName)
            throws Exception {
        startTrackCompositeRecordingToLocal(participantIdentity, roomName, livekitServiceName);
    }

    @When(
            "the system starts track composite recording for participant {string} in room {string} using LiveKit service {string} with S3 output to bucket {string}")
    public void startTrackCompositeRecordingWithS3(
            String participantIdentity, String roomName, String livekitServiceName, String bucketName)
            throws Exception {
        startTrackCompositeRecordingWithS3AndPrefix(participantIdentity, roomName, livekitServiceName, bucketName, "");
    }

    @When(
            "the system starts track composite recording for participant {string} in room {string} using LiveKit service {string} with S3 output to bucket {string} with prefix {string}")
    public void startTrackCompositeRecordingWithS3AndPrefix(
            String participantIdentity, String roomName, String livekitServiceName, String bucketName, String prefix)
            throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);

        EgressServiceClient egressClient = createEgressClient(liveKitContainer);

        Map<String, String> trackIds = ManagerProvider.getEgressStateManager().getTrackIds(participantIdentity);
        assertNotNull(trackIds, "No track IDs found for participant " + participantIdentity);
        String audioTrackId = trackIds.get("audio");
        String videoTrackId = trackIds.get("video");

        assertNotNull(audioTrackId, "No audio track ID found for participant " + participantIdentity);
        assertNotNull(videoTrackId, "No video track ID found for participant " + participantIdentity);

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = prefix + "track-composite-" + participantIdentity + "-" + timestamp + ".mp4";

        LivekitEgress.S3Upload s3Upload = LivekitEgress.S3Upload.newBuilder()
                .setBucket(bucketName)
                .setAccessKey(minioContainer.getAccessKey())
                .setSecret(minioContainer.getSecretKey())
                .setEndpoint(minioContainer.getNetworkS3EndpointUrl())
                .setRegion("us-east-1")
                .setForcePathStyle(true)
                .putMetadata("participant", participantIdentity)
                .putMetadata("room", roomName)
                .putMetadata("timestamp", timestamp)
                .build();

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath(fileName)
                .setS3(s3Upload)
                .build();

        log.info(
                "Starting track composite recording for participant: {} to S3 bucket: {} with key: {}",
                participantIdentity,
                bucketName,
                fileName);

        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient
                    .startTrackCompositeEgress(roomName, fileOutput, audioTrackId, videoTrackId)
                    .execute()
                    .body();
            log.info("Track composite S3 egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Track composite S3 egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start track composite S3 egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(participantIdentity + "_track", egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info(
                "Started track composite recording for participant {} with egress ID: {} to S3",
                participantIdentity,
                egressId);
    }

    @When("the system stops track composite recording for participant {string} using LiveKit service {string}")
    public void stopTrackCompositeRecording(String participantIdentity, String livekitServiceName) throws Exception {
        String egressId = ManagerProvider.getEgressStateManager().getActiveRecording(participantIdentity + "_track");
        assertNotNull(egressId, "No active track composite recording found for participant " + participantIdentity);

        stopEgressRecording(
                egressId,
                livekitServiceName,
                participantIdentity + "_track",
                "track composite recording for participant " + participantIdentity);
    }

    @When("the system stops room composite recording for room {string} using LiveKit service {string}")
    public void stopRoomCompositeRecording(String roomName, String livekitServiceName) throws Exception {
        String egressId = ManagerProvider.getEgressStateManager().getActiveRecording(roomName);
        assertNotNull(egressId, "No active recording found for room " + roomName);

        stopEgressRecording(egressId, livekitServiceName, roomName, "room composite recording for room " + roomName);
    }

    @Then("the recording file should exist in the output directory for room {string}")
    public void verifyRecordingFileExists(String roomName) throws InterruptedException {
        verifyLocalRecordingFile("recording-" + roomName, "room " + roomName);
    }

    @Then("the track composite recording file should exist for participant {string}")
    public void verifyTrackCompositeRecordingFileExists(String participantIdentity) throws InterruptedException {
        verifyLocalRecordingFile("track-composite-" + participantIdentity, "participant " + participantIdentity);
    }

    @Then("the recording file should exist in MinIO bucket {string} for room {string}")
    public void verifyRecordingFileExistsInMinIO(String bucketName, String roomName) throws InterruptedException {
        verifyRecordingFileExistsInMinIOWithPrefix(bucketName, "", roomName);
    }

    @Then("the recording file should exist in MinIO bucket {string} with prefix {string} for room {string}")
    public void verifyRecordingFileExistsInMinIOWithPrefix(String bucketName, String prefix, String roomName)
            throws InterruptedException {
        verifyS3RecordingFile(bucketName, prefix, "recording-" + roomName, "room " + roomName);
    }

    @Then("the track composite recording file should exist in MinIO bucket {string} for participant {string}")
    public void verifyTrackCompositeRecordingFileExistsInMinIO(String bucketName, String participantIdentity)
            throws InterruptedException {
        verifyTrackCompositeRecordingFileExistsInMinIOWithPrefix(bucketName, "", participantIdentity);
    }

    @Then(
            "the track composite recording file should exist in MinIO bucket {string} with prefix {string} for participant {string}")
    public void verifyTrackCompositeRecordingFileExistsInMinIOWithPrefix(
            String bucketName, String prefix, String participantIdentity) throws InterruptedException {
        verifyS3RecordingFile(
                bucketName, prefix, "track-composite-" + participantIdentity, "participant " + participantIdentity);
    }

    @And("the recording file should contain actual video content")
    public void verifyRecordingContainsVideoContent() {
        verifyLocalVideoContent();
    }

    @And("the recording file should contain actual video content from multiple participants")
    public void verifyRecordingContainsMultipleParticipants() throws InterruptedException {
        verifyLocalMultiParticipantContent();
    }

    @Then("the recording file in MinIO should contain actual video content")
    public void verifyRecordingInMinIOContainsVideoContent() {
        verifyS3VideoContent("recordings");
    }

    @Then("the recording files in MinIO should contain actual video content")
    public void verifyMultipleRecordingsInMinIOContainVideoContent() {
        verifyS3MultipleRecordingsContent("recordings");
    }

    @Then("no recording file should exist in the local output directory for room {string}")
    public void verifyNoLocalRecordingFileExists(String roomName) {
        verifyNoLocalFile("recording-" + roomName);
    }

    @Then("no track composite recording file should exist in the local output directory for participant {string}")
    public void verifyNoLocalTrackCompositeRecordingFileExists(String participantIdentity) {
        verifyNoLocalFile("track-composite-" + participantIdentity);
    }

    private void createEgressContainer(String serviceName, String livekitServiceName, S3Config s3Config) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        RedisContainer redisContainer = containerManager.getContainer("redis", RedisContainer.class);
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        String livekitWsUrl = liveKitContainer.getNetworkUrl();
        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        EgressContainer egressContainer = EgressContainer.createContainer(
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
                "Egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("Egress service {} started and registered", serviceName);
    }

    private void createEgressContainerWithS3(String serviceName, String livekitServiceName, S3Config s3Config) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        RedisContainer redisContainer = containerManager.getContainer("redis", RedisContainer.class);
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        String livekitWsUrl = liveKitContainer.getNetworkUrl();
        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        EgressContainer egressContainer = EgressContainer.createContainerWithS3(
                serviceName,
                containerManager.getOrCreateNetwork(),
                TestConfig.getEgressVersion(),
                livekitWsUrl,
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET,
                serviceLogPath,
                redisContainer.getNetworkRedisUrl(),
                s3Config);

        egressContainer.start();
        assertTrue(
                egressContainer.isRunning(),
                "Egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("Egress service {} started with S3 output configured", serviceName);
    }

    private void startRoomCompositeRecordingToLocal(String roomName, String livekitServiceName) throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = createEgressClient(liveKitContainer);

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "recording-" + roomName + "-" + timestamp + ".mp4";

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("/out/video-recordings/" + fileName)
                .build();

        log.info(
                "Starting room composite recording for room: {} using LiveKit server: {}",
                roomName,
                liveKitContainer.getWsUrl());

        startEgressRecording(egressClient, roomName, fileOutput, "local");
    }

    private void startTrackCompositeRecordingToLocal(
            String participantIdentity, String roomName, String livekitServiceName) throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = createEgressClient(liveKitContainer);

        Map<String, String> trackIds = ManagerProvider.getEgressStateManager().getTrackIds(participantIdentity);
        assertNotNull(trackIds, "No track IDs found for participant " + participantIdentity);
        String audioTrackId = trackIds.get("audio");
        String videoTrackId = trackIds.get("video");

        assertNotNull(audioTrackId, "No audio track ID found for participant " + participantIdentity);
        assertNotNull(videoTrackId, "No video track ID found for participant " + participantIdentity);

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "track-composite-" + participantIdentity + "-" + timestamp + ".mp4";

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("/out/video-recordings/" + fileName)
                .build();

        log.info(
                "Starting track composite recording for participant: {} (audio: {}, video: {}) in room: {} using LiveKit server: {}",
                participantIdentity,
                audioTrackId,
                videoTrackId,
                roomName,
                liveKitContainer.getWsUrl());

        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient
                    .startTrackCompositeEgress(roomName, fileOutput, audioTrackId, videoTrackId)
                    .execute()
                    .body();
            log.info("Track composite egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Track composite egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start track composite egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(participantIdentity + "_track", egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info(
                "Started track composite recording for participant {} with egress ID: {}",
                participantIdentity,
                egressId);
    }

    private void startEgressRecording(
            EgressServiceClient egressClient,
            String roomName,
            LivekitEgress.EncodedFileOutput fileOutput,
            String outputType)
            throws Exception {
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient
                    .startRoomCompositeEgress(roomName, fileOutput, "grid")
                    .execute()
                    .body();
            log.info("{} Egress API response: {}", outputType, egressInfo);
            assertNotNull(egressInfo, outputType + " Egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start {} egress recording", outputType, e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(roomName, egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info(
                "Started room composite recording for room {} with egress ID: {} to {}",
                roomName,
                egressId,
                outputType);
    }

    private void stopEgressRecording(String egressId, String livekitServiceName, String stateKey, String description)
            throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = createEgressClient(liveKitContainer);

        log.info("Stopping egress recording with ID: {}", egressId);
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.stopEgress(egressId).execute().body();
            log.info("Stop egress API response: {}", egressInfo);
            if (egressInfo == null) {
                log.warn("Stop egress API returned null - recording may have already completed");
                egressInfo = LivekitEgress.EgressInfo.newBuilder()
                        .setEgressId(egressId)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to stop egress recording", e);
            throw e;
        }

        ManagerProvider.getEgressStateManager().removeActiveRecording(stateKey);
        log.info("Stopped {} with final status: {}", description, egressInfo.getStatus());
    }

    private void verifyLocalRecordingFile(String filePattern, String description) throws InterruptedException {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);
        assertTrue(
                recordingsDir.exists() && recordingsDir.isDirectory(),
                "Recordings directory does not exist: " + recordingsDir.getAbsolutePath());

        File recordingFile = pollForFile(recordingsDir, filePattern, 10);

        assertNotNull(
                recordingFile,
                "No recording files found for "
                        + description
                        + " after 10 attempts. Files in directory: "
                        + String.join(", ", recordingsDir.list()));
        assertTrue(recordingFile.exists(), "Recording file does not exist: " + recordingFile.getAbsolutePath());
        assertTrue(recordingFile.length() > 0, "Recording file is empty: " + recordingFile.getAbsolutePath());

        log.info("Recording file found: {} (size: {} bytes)", recordingFile.getName(), recordingFile.length());
    }

    private void verifyS3RecordingFile(String bucketName, String prefix, String filePattern, String description)
            throws InterruptedException {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, bucketName);

        String recordingKey = pollForS3Object(s3Client, prefix, filePattern, 20);

        assertNotNull(
                recordingKey,
                "No recording files found in S3 bucket " + bucketName + " for " + description + " after 20 attempts");

        long size = s3Client.getObjectSize(recordingKey);
        assertTrue(size > 0, "Recording file in S3 is empty: " + recordingKey);

        log.info("Recording file found in S3: {} (size: {} bytes)", recordingKey, size);
    }

    private void verifyLocalVideoContent() {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);
        File[] files = recordingsDir.listFiles(
                (dir, name) -> (name.startsWith("recording-") || name.startsWith("track-composite-"))
                        && (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")));

        assertNotNull(files, "No recording files found");
        assertTrue(files.length > 0, "No recording files found");

        File recordingFile = files[0];

        assertTrue(
                recordingFile.length() > 70000,
                "Recording file too small ("
                        + recordingFile.length()
                        + " bytes), "
                        + "likely does not contain actual video content. Expected > 70KB for real video.");

        log.info(
                "Verified recording contains actual video content: {} ({} bytes)",
                recordingFile.getName(),
                recordingFile.length());
    }

    private void verifyLocalMultiParticipantContent() throws InterruptedException {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);

        File recordingFile = null;
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            File[] files = recordingsDir.listFiles((dir, name) -> name.startsWith("recording-")
                    && (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")));

            if (files != null && files.length > 0) {
                File file = files[0];
                if (file.length() > 50000) {
                    recordingFile = file;
                    break;
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug(
                        "Recording file not ready yet (size check), waiting... (attempt {}/{})",
                        attempt + 1,
                        maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }

        assertNotNull(recordingFile, "No recording files found after " + maxAttempts + " attempts");

        assertTrue(
                recordingFile.length() > 70000,
                "Multi-participant recording file too small ("
                        + recordingFile.length()
                        + " bytes), "
                        + "expected > 70KB for composite video with multiple participants.");

        log.info(
                "Verified multi-participant recording: {} ({} bytes)", recordingFile.getName(), recordingFile.length());
    }

    private void verifyS3VideoContent(String bucketName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, bucketName);

        List<String> objects = s3Client.listObjects("");
        String recordingKey = null;

        for (String key : objects) {
            if ((key.contains("recording-") || key.contains("track-composite-"))
                    && (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {
                recordingKey = key;
                break;
            }
        }

        assertNotNull(recordingKey, "No recording files found in MinIO");

        long size = s3Client.getObjectSize(recordingKey);
        assertTrue(
                size > 70000,
                "Recording file in S3 too small ("
                        + size
                        + " bytes), "
                        + "likely does not contain actual video content. Expected > 70KB for real video.");

        log.info("Verified S3 recording contains actual video content: {} ({} bytes)", recordingKey, size);
    }

    private void verifyS3MultipleRecordingsContent(String bucketName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, bucketName);

        List<String> objects = s3Client.listObjects("");
        int validRecordings = 0;

        for (String key : objects) {
            if ((key.contains("recording-") || key.contains("track-composite-"))
                    && (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {

                long size = s3Client.getObjectSize(key);
                if (size > 70000) {
                    validRecordings++;
                    log.info("Valid recording in S3: {} ({} bytes)", key, size);
                }
            }
        }

        assertTrue(validRecordings >= 2, "Expected at least 2 valid recordings in S3, found " + validRecordings);
    }

    private void verifyNoLocalFile(String filePattern) {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "recordings");
        File recordingsDir = new File(recordingsPath);

        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            log.info("Local recordings directory does not exist, which is expected");
            return;
        }

        File[] files = recordingsDir.listFiles((dir, name) -> name.contains(filePattern)
                && (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")));

        assertTrue(
                files == null || files.length == 0,
                "Found unexpected local recording files: "
                        + (files != null
                                ? String.join(
                                        ", ",
                                        java.util.Arrays.stream(files)
                                                .map(File::getName)
                                                .toArray(String[]::new))
                                : ""));
    }

    private File pollForFile(File directory, String pattern, int maxAttempts) throws InterruptedException {
        int attempt = 0;

        while (attempt < maxAttempts) {
            File[] files = directory.listFiles((dir, name) -> name.contains(pattern)
                    && (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv")));

            if (files != null && files.length > 0) {
                File file = files[0];
                if (file.length() > 0) {
                    return file;
                } else {
                    log.debug(
                            "Recording file exists but is empty: {} (attempt {}/{})",
                            file.getName(),
                            attempt + 1,
                            maxAttempts);
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not ready yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }

        return null;
    }

    private String pollForS3Object(MinioS3Client s3Client, String prefix, String pattern, int maxAttempts)
            throws InterruptedException {
        int attempt = 0;

        while (attempt < maxAttempts) {
            List<String> objects = s3Client.listObjects(prefix);

            for (String key : objects) {
                if (key.contains(pattern) && (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {

                    long size = s3Client.getObjectSize(key);
                    if (size > 0) {
                        return key;
                    } else {
                        log.debug(
                                "Recording file exists in S3 but is empty: {} (attempt {}/{})",
                                key,
                                attempt + 1,
                                maxAttempts);
                    }
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not found in S3 yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            attempt++;
        }

        return null;
    }

    private EgressServiceClient createEgressClient(LiveKitContainer liveKitContainer) {
        return EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);
    }

    private LivekitEgress.S3Upload createS3Upload(
            MinIOContainer minioContainer, String bucketName, String roomName, String timestamp) {
        return LivekitEgress.S3Upload.newBuilder()
                .setBucket(bucketName)
                .setAccessKey(minioContainer.getAccessKey())
                .setSecret(minioContainer.getSecretKey())
                .setEndpoint(minioContainer.getNetworkS3EndpointUrl())
                .setRegion("us-east-1")
                .setForcePathStyle(true)
                .putMetadata("room", roomName)
                .putMetadata("timestamp", timestamp)
                .build();
    }

    private MinioS3Client getOrCreateS3Client(
            String minioServiceName, MinIOContainer minioContainer, String bucketName) {
        String key = minioServiceName + "-" + bucketName;
        return s3Clients.computeIfAbsent(
                key,
                k -> new MinioS3Client(
                        minioContainer.getS3EndpointUrl(),
                        minioContainer.getAccessKey(),
                        minioContainer.getSecretKey(),
                        bucketName));
    }

    private String getCurrentScenarioLogPath() {
        return currentScenarioLogPath != null ? currentScenarioLogPath : PathUtils.currentScenarioPath();
    }
}
