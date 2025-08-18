package ro.stancalau.test.bdd.steps;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.docker.EgressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.bdd.util.EgressTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitEgressSteps {


    private String currentScenarioLogPath;

    @Before
    public void setUpEgressSteps(Scenario scenario) {
        String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();

        String sanitizedFeatureName = FileUtils.sanitizeFileName(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileName(scenarioName);

        currentScenarioLogPath = PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
    }

    @Given("a Redis server is running in a container with service name {string}")
    public void aRedisServerIsRunningInContainer(String serviceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        String serviceLogPath = PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

        RedisContainer redisContainer = RedisContainer.createContainer(
                serviceName,
                containerManager.getOrCreateNetwork(),
                serviceLogPath
        );

        redisContainer.start();
        assertTrue(redisContainer.isRunning(), "Redis container with service name " + serviceName + " should be running");
        containerManager.registerContainer(serviceName, redisContainer);
        log.info("Redis service {} started for egress communication", serviceName);
    }

    @Given("a LiveKit video recording egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitVideoRecordingEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
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
                redisContainer.getNetworkRedisUrl()
        );

        egressContainer.start();
        assertTrue(egressContainer.isRunning(), "Video recording egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("Video recording egress service {} started and registered", serviceName);
    }

    @Given("a LiveKit egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
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
                redisContainer.getNetworkRedisUrl()
        );

        egressContainer.start();
        assertTrue(egressContainer.isRunning(), "Egress container with service name " + serviceName + " should be running");

        containerManager.registerContainer(serviceName, egressContainer);
        log.info("Egress service {} started and registered", serviceName);
    }

    @When("room composite recording is started for room {string} using LiveKit service {string}")
    public void startRoomCompositeRecording(String roomName, String livekitServiceName) throws Exception {

        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

        String timestamp = DateUtils.generateRecordingTimestamp();
        String fileName = "recording-" + roomName + "-" + timestamp + ".mp4";

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("/out/video-recordings/" + fileName)
                .build();

        log.info("Starting room composite recording for room: {} using LiveKit server: {}", roomName, liveKitContainer.getWsUrl());
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.startRoomCompositeEgress(roomName, fileOutput, "grid").execute().body();
            log.info("Egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(roomName, egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info("Started room composite recording for room {} with egress ID: {}", roomName, egressId);
    }

    @When("the recording runs for {int} seconds")
    public void recordingRunsForDuration(int seconds) throws InterruptedException {
        log.info("Allowing recording to capture for {} seconds", seconds);
        TimeUnit.SECONDS.sleep(seconds);
    }

    @When("track composite recording is started for participant {string} in room {string} using LiveKit service {string}")
    public void startTrackCompositeRecording(String participantIdentity, String roomName, String livekitServiceName) throws Exception {

        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

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
        String fileName = "track-composite-" + participantIdentity + "-" + timestamp + ".mp4";

        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("/out/video-recordings/" + fileName)
                .build();

        log.info("Starting track composite recording for participant: {} (audio: {}, video: {}) in room: {} using LiveKit server: {}",
                participantIdentity, audioTrackId, videoTrackId, roomName, liveKitContainer.getWsUrl());

        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.startTrackCompositeEgress(roomName, fileOutput, audioTrackId, videoTrackId).execute().body();
            log.info("Track composite egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Track composite egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start track composite egress recording", e);
            throw e;
        }

        String egressId = egressInfo.getEgressId();
        ManagerProvider.getEgressStateManager().storeActiveRecording(participantIdentity + "_track", egressId);

        EgressTestUtils.waitForEgressToBeActive(egressClient, egressId, roomName);
        log.info("Started track composite recording for participant {} with egress ID: {}", participantIdentity, egressId);
    }

    @When("track composite recording is stopped for participant {string} using LiveKit service {string}")
    public void stopTrackCompositeRecording(String participantIdentity, String livekitServiceName) throws Exception {
        String egressId = ManagerProvider.getEgressStateManager().getActiveRecording(participantIdentity + "_track");
        assertNotNull(egressId, "No active track composite recording found for participant " + participantIdentity);

        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

        log.info("Stopping track composite egress recording with ID: {}", egressId);
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.stopEgress(egressId).execute().body();
            log.info("Stop track composite egress API response: {}", egressInfo);
            if (egressInfo == null) {
                log.warn("Stop egress API returned null - recording may have already completed");
                egressInfo = LivekitEgress.EgressInfo.newBuilder()
                        .setEgressId(egressId)
                        .setRoomName(participantIdentity)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to stop track composite egress recording", e);
            throw e;
        }

        ManagerProvider.getEgressStateManager().removeActiveRecording(participantIdentity + "_track");

        log.info("Stopped track composite recording for participant {} with final status: {}",
                participantIdentity, egressInfo.getStatus());
    }

    @When("room composite recording is stopped for room {string} using LiveKit service {string}")
    public void stopRoomCompositeRecording(String roomName, String livekitServiceName) throws Exception {
        String egressId = ManagerProvider.getEgressStateManager().getActiveRecording(roomName);
        assertNotNull(egressId, "No active recording found for room " + roomName);

        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);

        EgressServiceClient egressClient = EgressServiceClient.createClient(
                liveKitContainer.getHttpUrl(),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

        log.info("Stopping egress recording with ID: {}", egressId);
        LivekitEgress.EgressInfo egressInfo;
        try {
            egressInfo = egressClient.stopEgress(egressId).execute().body();
            log.info("Stop egress API response: {}", egressInfo);
            if (egressInfo == null) {
                log.warn("Stop egress API returned null - recording may have already completed");
                egressInfo = LivekitEgress.EgressInfo.newBuilder()
                        .setEgressId(egressId)
                        .setRoomName(roomName)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to stop egress recording", e);
            throw e;
        }

        ManagerProvider.getEgressStateManager().removeActiveRecording(roomName);

        log.info("Stopped room composite recording for room {} with final status: {}",
                roomName, egressInfo.getStatus());
    }

    @Given("track IDs are captured for participant {string} in room {string} using LiveKit service {string}")
    public void captureTrackIds(String participantIdentity, String roomName, String livekitServiceName) throws Exception {
        RoomServiceClient roomClient = ManagerProvider.getRoomClientManager().getRoomServiceClient(livekitServiceName);

        List<LivekitModels.ParticipantInfo> participants = roomClient
                .listParticipants(roomName).execute().body();

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
        assertTrue(trackIds.containsKey("audio"), "Audio track not found for participant " + participantIdentity);
        assertTrue(trackIds.containsKey("video"), "Video track not found for participant " + participantIdentity);
    }

    @Then("the recording file exists in the output directory for room {string}")
    public void verifyRecordingFileExists(String roomName) throws InterruptedException {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);
        assertTrue(recordingsDir.exists() && recordingsDir.isDirectory(),
                "Recordings directory does not exist: " + recordingsDir.getAbsolutePath());

        File recordingFile = null;
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            File[] files = recordingsDir.listFiles((dir, name) ->
                    name.contains("recording-" + roomName) &&
                            (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
            );

            if (files != null && files.length > 0) {
                File file = files[0];
                if (file.length() > 0) {
                    recordingFile = file;
                    break;
                } else {
                    log.debug("Recording file exists but is empty: {} (attempt {}/{})", file.getName(), attempt + 1, maxAttempts);
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not ready yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }

        File[] allFiles = recordingsDir.listFiles((dir, name) ->
                name.contains("recording-" + roomName) &&
                        (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
        );

        if (allFiles != null && allFiles.length > 0 && allFiles[0].length() == 0) {
            fail("Recording file exists but is empty (0 bytes). This may indicate the egress was aborted. " +
                    "File: " + allFiles[0].getName() + ". Check egress logs for errors.");
        }

        assertNotNull(recordingFile, "No recording files found for room " + roomName +
                " after " + maxAttempts + " attempts. Files in directory: " + String.join(", ", recordingsDir.list()));
        assertTrue(recordingFile.exists(), "Recording file does not exist: " + recordingFile.getAbsolutePath());
        assertTrue(recordingFile.length() > 0, "Recording file is empty: " + recordingFile.getAbsolutePath());

        log.info("Recording file found: {} (size: {} bytes)",
                recordingFile.getName(), recordingFile.length());
    }

    @Then("the track composite recording file exists for participant {string}")
    public void verifyTrackCompositeRecordingFileExists(String participantIdentity) throws InterruptedException {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);
        assertTrue(recordingsDir.exists() && recordingsDir.isDirectory(),
                "Recordings directory does not exist: " + recordingsDir.getAbsolutePath());

        File recordingFile = null;
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            File[] files = recordingsDir.listFiles((dir, name) ->
                    name.contains("track-composite-" + participantIdentity) &&
                            (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
            );

            if (files != null && files.length > 0) {
                File file = files[0];
                if (file.length() > 0) {
                    recordingFile = file;
                    break;
                } else {
                    log.debug("Track composite recording file exists but is empty: {} (attempt {}/{})", file.getName(), attempt + 1, maxAttempts);
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Track composite recording file not ready yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }

        assertNotNull(recordingFile, "No track composite recording files found for participant " + participantIdentity +
                " after " + maxAttempts + " attempts. Files in directory: " + String.join(", ", recordingsDir.list()));
        assertTrue(recordingFile.exists(), "Track composite recording file does not exist: " + recordingFile.getAbsolutePath());
        assertTrue(recordingFile.length() > 0, "Track composite recording file is empty: " + recordingFile.getAbsolutePath());

        log.info("Track composite recording file found: {} (size: {} bytes)",
                recordingFile.getName(), recordingFile.length());
    }

    @And("the recording file contains actual video content")
    public void verifyRecordingContainsVideoContent() {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);
        File[] files = recordingsDir.listFiles((dir, name) ->
                (name.startsWith("recording-") || name.startsWith("track-composite-")) &&
                        (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
        );

        assertNotNull(files, "No recording files found");
        assertTrue(files.length > 0, "No recording files found");

        File recordingFile = files[0];

        assertTrue(recordingFile.length() > 70000,
                "Recording file too small (" + recordingFile.length() + " bytes), " +
                        "likely does not contain actual video content. Expected > 70KB for real video.");

        log.info("Verified recording contains actual video content: {} ({} bytes)",
                recordingFile.getName(), recordingFile.length());
    }

    @And("the recording file contains actual video content from multiple participants")
    public void verifyRecordingContainsMultipleParticipants() throws InterruptedException {
        String recordingsPath = PathUtils.join(getCurrentScenarioLogPath(), "video-recordings");
        File recordingsDir = new File(recordingsPath);

        File recordingFile = null;
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            File[] files = recordingsDir.listFiles((dir, name) ->
                    name.startsWith("recording-") &&
                            (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
            );

            if (files != null && files.length > 0) {
                File file = files[0];
                // Wait for file to be reasonably sized before checking
                if (file.length() > 50000) {
                    recordingFile = file;
                    break;
                }
            }

            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not ready yet (size check), waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }

        assertNotNull(recordingFile, "No recording files found after " + maxAttempts + " attempts");

        assertTrue(recordingFile.length() > 70000,
                "Multi-participant recording file too small (" + recordingFile.length() + " bytes), " +
                        "expected > 70KB for composite video with multiple participants.");

        log.info("Verified multi-participant recording: {} ({} bytes)",
                recordingFile.getName(), recordingFile.length());
    }

    private String getCurrentScenarioLogPath() {
        return currentScenarioLogPath != null ? currentScenarioLogPath : PathUtils.currentScenarioPath();
    }
}