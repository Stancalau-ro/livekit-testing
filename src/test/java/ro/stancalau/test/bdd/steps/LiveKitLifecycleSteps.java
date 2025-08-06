package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;
import ro.stancalau.test.framework.util.TestConfig;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LiveKitLifecycleSteps {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private String defaultConfigProfile = "basic";
    private String currentScenarioLogPath;

    public LiveKitLifecycleSteps() {
    }

    @Before
    public void setUpLiveKitLifecycleSteps(Scenario scenario) {
        
        String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String sanitizedFeatureName = sanitizeFileName(featureName);
        String sanitizedScenarioName = sanitizeFileName(scenarioName);
        
        currentScenarioLogPath = "out/bdd/scenarios/" + sanitizedFeatureName + "/" + 
                                sanitizedScenarioName + "/" + timestamp;
        ManagerProvider.webDrivers().setScenarioRecordingPath(currentScenarioLogPath);
    }

    @After
    public void tearDownLiveKitLifecycleSteps() {
        ManagerProvider.containers().cleanup(LiveKitContainer.class);
    }

    @Given("the LiveKit config is set to {string}")
    public void theLiveKitConfigIsSetTo(String profileName) {
        this.defaultConfigProfile = profileName;
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
        Network network = ManagerProvider.containers().getOrCreateNetwork();

        String serviceLogPath = currentScenarioLogPath + "/docker/" + serviceName;
        LiveKitContainer liveKitContainer = LiveKitContainerFactory.createBddContainerWithScenarioLogs(
                serviceName, network, configPath, serviceLogPath);

        liveKitContainer.start();
        assertTrue(liveKitContainer.isRunning(), "LiveKit container with service name " + serviceName + " should be running");

        ManagerProvider.containers().registerContainer(serviceName, liveKitContainer);
    }


    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_+", "_")  // Replace multiple underscores with single
                      .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
    }
}