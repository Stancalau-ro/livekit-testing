package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.state.WebDriverStateManager;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;
import ro.stancalau.test.framework.util.TestConfig;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LiveKitLifecycleSteps {

    private ContainerStateManager containerManager;
    private String defaultConfigProfile = "basic";
    private String currentScenarioLogPath;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Before
    public void setUpLiveKitLifecycleSteps(Scenario scenario) {
        containerManager = ContainerStateManager.getInstance();
        
        // Create scenario-specific log path
        String featureName = extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String sanitizedFeatureName = sanitizeFileName(featureName);
        String sanitizedScenarioName = sanitizeFileName(scenarioName);
        
        currentScenarioLogPath = "out/bdd/scenarios/" + sanitizedFeatureName + "/" + 
                                sanitizedScenarioName + "/" + timestamp;
        
        log.info("Setting up scenario log path: {}", currentScenarioLogPath);
        
        // Set scenario recording path for WebDrivers
        WebDriverStateManager.getInstance().setScenarioRecordingPath(currentScenarioLogPath);
    }

    @After
    public void tearDownLiveKitLifecycleSteps() {
        if (containerManager != null) {
            containerManager.cleanup(LiveKitContainer.class);
        }
    }

    @Given("the LiveKit config is set to {string}")
    public void theLiveKitConfigIsSetTo(String profileName) {
        this.defaultConfigProfile = profileName;
        log.info("LiveKit config profile set to: {}", profileName);
    }

    @Given("a LiveKit server is running in a container with service name {string}")
    public void aLiveKitServerIsRunningInAContainerWithServiceName(String serviceName) {
        String configPath = TestConfig.resolveConfigPath(defaultConfigProfile);
        getOrCreateContainer(serviceName, configPath);
    }

    @Given("a LiveKit server is running in a container with service name {string} using config profile {string}")
    public void aLiveKitServerIsRunningInAContainerWithServiceNameUsingConfigProfile(String serviceName, String profileName) {
        String configPath = TestConfig.resolveConfigPath(profileName);
        getOrCreateContainer(serviceName, configPath);
    }

    private void getOrCreateContainer(String serviceName, @Nullable String configPath) {
        if (!containerManager.isContainerRunning(serviceName)) {
            log.info("Starting LiveKit container with service name {} for BDD test", serviceName);
            Network network = containerManager.getOrCreateNetwork();

            // Create container with scenario-specific log path
            String serviceLogPath = currentScenarioLogPath + "/docker/" + serviceName;
            LiveKitContainer liveKitContainer = LiveKitContainerFactory.createBddContainerWithScenarioLogs(
                    serviceName, network, configPath, serviceLogPath);

            liveKitContainer.start();
            assertTrue(liveKitContainer.isRunning(), "LiveKit container with service name " + serviceName + " should be running");

            log.info("LiveKit container started successfully at: {} with logs at: {}", 
                    liveKitContainer.getHttpLink(), serviceLogPath);
            containerManager.registerContainer(serviceName, liveKitContainer);
        } else {
            log.info("LiveKit container with service name {} is already running", serviceName);
        }
    }

    private String extractFeatureName(String uri) {
        try {
            // Extract filename from URI (e.g., "file:///path/to/livekit_webrtc_publish.feature")
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            
            // Remove .feature extension
            if (fileName.endsWith(".feature")) {
                fileName = fileName.substring(0, fileName.length() - 8);
            }
            
            // Convert underscores to spaces and capitalize
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
        // Replace special characters with underscores
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_+", "_")  // Replace multiple underscores with single
                      .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
    }
}