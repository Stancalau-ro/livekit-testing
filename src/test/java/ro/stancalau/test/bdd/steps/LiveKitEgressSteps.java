package ro.stancalau.test.bdd.steps;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.EgressServiceClient;
import livekit.LivekitEgress;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.docker.EgressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitEgressSteps {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Map<String, String> activeRecordings = new HashMap<>();
    private String currentScenarioLogPath;
    private final Map<String, LivekitEgress.EgressInfo> completedRecordings = new HashMap<>();
    
    @Before
    public void setUpEgressSteps(Scenario scenario) {
        String featureName = extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String sanitizedFeatureName = sanitizeFileName(featureName);
        String sanitizedScenarioName = sanitizeFileName(scenarioName);
        
        currentScenarioLogPath = "out/bdd/scenarios/" + sanitizedFeatureName + "/" + 
                                sanitizedScenarioName + "/" + timestamp;
        log.debug("EgressSteps: Set scenario log path to: {}", currentScenarioLogPath);
    }
    
    @Given("a Redis server is running in a container with service name {string}")
    public void aRedisServerIsRunningInContainer(String serviceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        String serviceLogPath = getCurrentScenarioLogPath() + "/docker/" + serviceName;
        
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
    
    @Given("a LiveKit egress service is running in a container with service name {string} connected to LiveKit service {string}")
    public void aLiveKitEgressServiceIsRunningInContainer(String serviceName, String livekitServiceName) {
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();

        RedisContainer redisContainer = containerManager.getContainer("redis", RedisContainer.class);

        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        String livekitWsUrl = liveKitContainer.getNetworkWs();

        String serviceLogPath = getCurrentScenarioLogPath() + "/docker/" + serviceName;
        
        EgressContainer egressContainer = EgressContainer.createContainer(
            serviceName, 
            containerManager.getOrCreateNetwork(), 
            "v1.8.4",
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
        
        String wsUrl = liveKitContainer.getlocalWs();
        EgressServiceClient egressClient = EgressServiceClient.createClient(
            wsUrl.replace("ws://", "http://"),
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET
        );

        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss"));
        String fileName = "recording-" + roomName + "-" + timestamp + ".mp4";
        
        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("/out/recordings/" + fileName)
                .build();

        log.info("Starting room composite recording for room: {} using LiveKit server: {}", roomName, wsUrl);
        LivekitEgress.EgressInfo egressInfo = null;
        try {
            egressInfo = egressClient.startRoomCompositeEgress(roomName, fileOutput, "grid").execute().body();
            log.info("Egress API response: {}", egressInfo);
            assertNotNull(egressInfo, "Egress recording should have started");
        } catch (Exception e) {
            log.error("Failed to start egress recording", e);
            throw e;
        }
        
        String egressId = egressInfo.getEgressId();
        activeRecordings.put(roomName, egressId);
        
        log.info("Started room composite recording for room {} with egress ID: {}", roomName, egressId);
    }
    
    @When("the recording runs for {int} seconds")
    public void recordingRunsForDuration(int seconds) throws InterruptedException {
        log.info("Allowing recording to capture for {} seconds", seconds);
        TimeUnit.SECONDS.sleep(seconds);
    }
    
    @When("room composite recording is stopped for room {string} using LiveKit service {string}")
    public void stopRoomCompositeRecording(String roomName, String livekitServiceName) throws Exception {
        String egressId = activeRecordings.get(roomName);
        assertNotNull(egressId, "No active recording found for room " + roomName);
        
        ContainerStateManager containerManager = ManagerProvider.getContainerManager();
        LiveKitContainer liveKitContainer = containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
        
        String wsUrl = liveKitContainer.getlocalWs();
        EgressServiceClient egressClient = EgressServiceClient.createClient(
            wsUrl.replace("ws://", "http://"),
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET
        );

        log.info("Stopping egress recording with ID: {}", egressId);
        LivekitEgress.EgressInfo egressInfo = null;
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
        
        completedRecordings.put(roomName, egressInfo);
        activeRecordings.remove(roomName);
        
        log.info("Stopped room composite recording for room {} with final status: {}", 
            roomName, egressInfo.getStatus());
    }
    
    @Then("the recording file exists in the output directory for room {string}")
    public void verifyRecordingFileExists(String roomName) throws InterruptedException {
        String recordingsPath = getCurrentScenarioLogPath() + "/recordings";
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
            
            if (files != null && files.length > 0 && files[0].length() > 0) {
                recordingFile = files[0];
                break;
            }
            
            if (attempt < maxAttempts - 1) {
                log.debug("Recording file not ready yet, waiting... (attempt {}/{})", attempt + 1, maxAttempts);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            attempt++;
        }
        
        assertNotNull(recordingFile, "No recording files found for room " + roomName + 
            " after " + maxAttempts + " attempts. Files in directory: " + String.join(", ", recordingsDir.list()));
        assertTrue(recordingFile.exists(), "Recording file does not exist: " + recordingFile.getAbsolutePath());
        assertTrue(recordingFile.length() > 0, "Recording file is empty: " + recordingFile.getAbsolutePath());
        
        log.info("Recording file found: {} (size: {} bytes)", 
            recordingFile.getName(), recordingFile.length());
    }
    
    @And("the recording file contains actual video content")
    public void verifyRecordingContainsVideoContent() {
        String recordingsPath = getCurrentScenarioLogPath() + "/recordings";
        File recordingsDir = new File(recordingsPath);
        File[] files = recordingsDir.listFiles((dir, name) -> 
            name.startsWith("recording-") && 
            (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv"))
        );
        
        assertNotNull(files, "No recording files found");
        assertTrue(files.length > 0, "No recording files found");
        
        File recordingFile = files[0];

        assertTrue(recordingFile.length() > 50000, 
            "Recording file too small (" + recordingFile.length() + " bytes), " +
            "likely does not contain actual video content. Expected > 50KB for real video.");
        
        log.info("Verified recording contains actual video content: {} ({} bytes)", 
            recordingFile.getName(), recordingFile.length());
    }
    
    @And("the recording file contains actual video content from multiple participants")
    public void verifyRecordingContainsMultipleParticipants() throws InterruptedException {
        String recordingsPath = getCurrentScenarioLogPath() + "/recordings";
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

        assertTrue(recordingFile.length() > 50000, 
            "Multi-participant recording file too small (" + recordingFile.length() + " bytes), " +
            "expected > 50KB for composite video with multiple participants.");
        
        log.info("Verified multi-participant recording: {} ({} bytes)", 
            recordingFile.getName(), recordingFile.length());
    }
    
    private String getCurrentScenarioLogPath() {
        return currentScenarioLogPath != null ? currentScenarioLogPath : "out/bdd/scenarios/current";
    }
    
    private String extractFeatureName(String uri) {
        try {
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            if (fileName.endsWith(".feature")) {
                fileName = fileName.substring(0, fileName.length() - 8);
            }
            String[] words = fileName.replace('_', ' ').toLowerCase().split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(word.substring(0, 1).toUpperCase())
                          .append(word.substring(1));
                }
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("Failed to extract feature name from URI: {}", uri, e);
            return "Unknown Feature";
        }
    }
    
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\-_\\s]", "")
                      .replaceAll("\\s+", "_")
                      .replaceAll("_+", "_")
                      .replaceAll("^_|_$", "");
    }
}