package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.config.S3Config;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.EgressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.MinIOContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.MinioS3Client;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;
import ro.stancalau.test.bdd.util.EgressTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitMinIORecordingSteps {

    
    private final Map<String, MinioS3Client> s3Clients = new HashMap<>();
    private String currentScenarioLogPath;

    @Before
    public void setUpMinIORecordingSteps(Scenario scenario) {
        String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();

        String sanitizedFeatureName = FileUtils.sanitizeFileName(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileName(scenarioName);

        currentScenarioLogPath = PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
    }

    @After
    public void tearDownMinIORecordingSteps() {
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

    @Given("a MinIO server is running in a container with service name {string}")
    public void aMinIOServerIsRunningInContainer(String serviceName) {
        aMinIOServerIsRunningInContainer(serviceName, MinIOContainer.DEFAULT_ACCESS_KEY, MinIOContainer.DEFAULT_SECRET_KEY);
    }

    @Given("a MinIO server is running in a container with service name {string} with access key {string} and secret key {string}")
    public void aMinIOServerIsRunningInContainer(String serviceName, String accessKey, String secretKey) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        MinIOContainer minioContainer = MinIOContainer.createContainer(
                serviceName,
                containerManager.getOrCreateNetwork(),
                accessKey,
                secretKey,
                serviceLogPath
        );

        minioContainer.start();
        assertTrue(minioContainer.isRunning(), "MinIO container with service name " + serviceName + " should be running");
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

    @Given("a LiveKit egress service is running in a container with service name {string} connected to LiveKit service {string} with S3 output to MinIO service {string}")
    public void aLiveKitEgressServiceWithS3IsRunningInContainer(String serviceName, String livekitServiceName, String minioServiceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        RedisContainer redisContainer = containerManager.getContainer("redis", RedisContainer.class);
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        MinIOContainer minioContainer = containerManager.getContainer(minioServiceName, MinIOContainer.class);
        
        String livekitWsUrl = liveKitContainer.getNetworkUrl();
        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        S3Config s3Config = new S3Config(
            minioContainer.getNetworkS3EndpointUrl(),
            minioContainer.getAccessKey(),
            minioContainer.getSecretKey(),
            "recordings"
        );

        EgressContainer egressContainer = EgressContainer.createContainerWithS3(
                serviceName,
                containerManager.getOrCreateNetwork(),
                TestConfig.getEgressVersion(),
                livekitWsUrl,
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET,
                serviceLogPath,
                redisContainer.getNetworkRedisUrl(),
                s3Config
        );

        egressContainer.start();
        assertTrue(egressContainer.isRunning(), "Egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("Egress service {} started with S3 output configured", serviceName);
    }

    @When("room composite recording is started for room {string} using LiveKit service {string} with S3 output to bucket {string}")
    public void startRoomCompositeRecordingWithS3(String roomName, String livekitServiceName, String bucketName) throws Exception {
        startRoomCompositeRecordingWithS3AndPrefix(roomName, livekitServiceName, bucketName, "");
    }

    @When("room composite recording is started for room {string} using LiveKit service {string} with S3 output to bucket {string} with prefix {string}")
    public void startRoomCompositeRecordingWithS3AndPrefix(String roomName, String livekitServiceName, String bucketName, String prefix) throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = prefix + "recording-" + roomName + "-" + timestamp + ".mp4";

        LivekitEgress.S3Upload s3Upload = LivekitEgress.S3Upload.newBuilder()
                .setBucket(bucketName)
                .setAccessKey(minioContainer.getAccessKey())
                .setSecret(minioContainer.getSecretKey())
                .setEndpoint(minioContainer.getNetworkS3EndpointUrl())
                .setRegion("us-east-1")
                .setForcePathStyle(true)
                .putMetadata("room", roomName)
                .putMetadata("timestamp", timestamp)
                .build();

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath(fileName)
                .setS3(s3Upload)
                .build();

        log.info("Starting room composite recording for room: {} to S3 bucket: {} with key: {}", 
                roomName, bucketName, fileName);
        
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.startRoomCompositeEgress(roomName, fileOutput, "grid").execute().body();
            log.info("S3 Egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "S3 Egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start S3 egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(roomName, egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info("Started room composite recording for room {} with egress ID: {} to S3", roomName, egressId);
    }

    @When("track composite recording is started for participant {string} in room {string} using LiveKit service {string} with S3 output to bucket {string}")
    public void startTrackCompositeRecordingWithS3(String participantIdentity, String roomName, String livekitServiceName, String bucketName) throws Exception {
        startTrackCompositeRecordingWithS3AndPrefix(participantIdentity, roomName, livekitServiceName, bucketName, "");
    }

    @When("track composite recording is started for participant {string} in room {string} using LiveKit service {string} with S3 output to bucket {string} with prefix {string}")
    public void startTrackCompositeRecordingWithS3AndPrefix(String participantIdentity, String roomName, String livekitServiceName, String bucketName, String prefix) throws Exception {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

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

        log.info("Starting track composite recording for participant: {} to S3 bucket: {} with key: {}", 
                participantIdentity, bucketName, fileName);

        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.startTrackCompositeEgress(roomName, fileOutput, audioTrackId, videoTrackId).execute().body();
            log.info("Track composite S3 egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Track composite S3 egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start track composite S3 egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(participantIdentity + "_track", egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info("Started track composite recording for participant {} with egress ID: {} to S3", participantIdentity, egressId);
    }

    @Then("the recording file exists in MinIO bucket {string} for room {string}")
    public void verifyRecordingFileExistsInMinIO(String bucketName, String roomName) throws InterruptedException {
        verifyRecordingFileExistsInMinIOWithPrefix(bucketName, "", roomName);
    }

    @Then("the recording file exists in MinIO bucket {string} with prefix {string} for room {string}")
    public void verifyRecordingFileExistsInMinIOWithPrefix(String bucketName, String prefix, String roomName) throws InterruptedException {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, bucketName);

        String recordingKey = null;
        int maxAttempts = 20;
        int attempt = 0;

        while (attempt < maxAttempts) {
            List<String> objects = s3Client.listObjects(prefix);
            
            for (String key : objects) {
                if (key.contains("recording-" + roomName) && 
                    (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {
                    
                    long size = s3Client.getObjectSize(key);
                    if (size > 0) {
                        recordingKey = key;
                        break;
                    } else {
                        log.debug("Recording file exists in S3 but is empty: {} (attempt {}/{})", key, attempt + 1, maxAttempts);
                    }
                }
            }

            if (recordingKey != null) {
                break;
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not found in S3 yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            attempt++;
        }

        assertNotNull(recordingKey, "No recording files found in S3 bucket " + bucketName + 
                " for room " + roomName + " after " + maxAttempts + " attempts");
        
        long size = s3Client.getObjectSize(recordingKey);
        assertTrue(size > 0, "Recording file in S3 is empty: " + recordingKey);

        log.info("Recording file found in S3: {} (size: {} bytes)", recordingKey, size);
    }

    @Then("the track composite recording file exists in MinIO bucket {string} for participant {string}")
    public void verifyTrackCompositeRecordingFileExistsInMinIO(String bucketName, String participantIdentity) throws InterruptedException {
        verifyTrackCompositeRecordingFileExistsInMinIOWithPrefix(bucketName, "", participantIdentity);
    }

    @Then("the track composite recording file exists in MinIO bucket {string} with prefix {string} for participant {string}")
    public void verifyTrackCompositeRecordingFileExistsInMinIOWithPrefix(String bucketName, String prefix, String participantIdentity) throws InterruptedException {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, bucketName);

        String recordingKey = null;
        int maxAttempts = 20;
        int attempt = 0;

        while (attempt < maxAttempts) {
            List<String> objects = s3Client.listObjects(prefix);
            
            for (String key : objects) {
                if (key.contains("track-composite-" + participantIdentity) && 
                    (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {
                    
                    long size = s3Client.getObjectSize(key);
                    if (size > 0) {
                        recordingKey = key;
                        break;
                    }
                }
            }

            if (recordingKey != null) {
                break;
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Track composite recording not found in S3 yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            attempt++;
        }

        assertNotNull(recordingKey, "No track composite recording files found in S3 bucket " + bucketName + 
                " for participant " + participantIdentity + " after " + maxAttempts + " attempts");

        log.info("Track composite recording file found in S3: {} (size: {} bytes)", 
                recordingKey, s3Client.getObjectSize(recordingKey));
    }

    @Then("the recording file in MinIO contains actual video content")
    public void verifyRecordingInMinIOContainsVideoContent() {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, "recordings");

        List<String> objects = s3Client.listObjects("");
        String recordingKey = null;
        
        for (String key : objects) {
            if ((key.contains("recording-") || key.contains("track-composite-")) &&
                (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {
                recordingKey = key;
                break;
            }
        }

        assertNotNull(recordingKey, "No recording files found in MinIO");
        
        long size = s3Client.getObjectSize(recordingKey);
        assertTrue(size > 70000,
                "Recording file in S3 too small (" + size + " bytes), " +
                "likely does not contain actual video content. Expected > 70KB for real video.");

        log.info("Verified S3 recording contains actual video content: {} ({} bytes)", recordingKey, size);
    }

    @Then("the recording files in MinIO contain actual video content")
    public void verifyMultipleRecordingsInMinIOContainVideoContent() {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        MinIOContainer minioContainer = containerManager.getContainer("minio", MinIOContainer.class);
        MinioS3Client s3Client = getOrCreateS3Client("minio", minioContainer, "recordings");

        List<String> objects = s3Client.listObjects("");
        int validRecordings = 0;
        
        for (String key : objects) {
            if ((key.contains("recording-") || key.contains("track-composite-")) &&
                (key.endsWith(".mp4") || key.endsWith(".webm") || key.endsWith(".mkv"))) {
                
                long size = s3Client.getObjectSize(key);
                if (size > 70000) {
                    validRecordings++;
                    log.info("Valid recording in S3: {} ({} bytes)", key, size);
                }
            }
        }

        assertTrue(validRecordings >= 2, 
                "Expected at least 2 valid recordings in S3, found " + validRecordings);
    }

    @Then("no recording file exists in the local output directory for room {string}")
    public void verifyNoLocalRecordingFileExists(String roomName) {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "recordings");
        File recordingsDir = new File(recordingsPath);
        
        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            log.info("Local recordings directory does not exist, which is expected");
            return;
        }

        File[] files = recordingsDir.listFiles((dir, name) ->
                name.contains("recording-" + roomName) &&
                (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
        );

        assertTrue(files == null || files.length == 0,
                "Found unexpected local recording files: " + 
                (files != null ? String.join(", ", java.util.Arrays.stream(files).map(File::getName).toArray(String[]::new)) : ""));
    }

    @Then("no track composite recording file exists in the local output directory for participant {string}")
    public void verifyNoLocalTrackCompositeRecordingFileExists(String participantIdentity) {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "recordings");
        File recordingsDir = new File(recordingsPath);
        
        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            log.info("Local recordings directory does not exist, which is expected");
            return;
        }

        File[] files = recordingsDir.listFiles((dir, name) ->
                name.contains("track-composite-" + participantIdentity) &&
                (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
        );

        assertTrue(files == null || files.length == 0,
                "Found unexpected local track composite recording files: " + 
                (files != null ? String.join(", ", java.util.Arrays.stream(files).map(File::getName).toArray(String[]::new)) : ""));
    }

    private MinioS3Client getOrCreateS3Client(String minioServiceName, MinIOContainer minioContainer, String bucketName) {
        String key = minioServiceName + "-" + bucketName;
        return s3Clients.computeIfAbsent(key, k -> 
            new MinioS3Client(
                minioContainer.getS3EndpointUrl(),
                minioContainer.getAccessKey(),
                minioContainer.getSecretKey(),
                bucketName
            )
        );
    }

    private String getCurrentScenarioLogPath() {
        return currentScenarioLogPath != null ? currentScenarioLogPath : PathUtils.currentScenarioPath();
    }

}