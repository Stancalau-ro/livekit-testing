package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;

@Slf4j
public class LiveKitLifecycleSteps {

  private String defaultConfigProfile = "basic";
  private String currentScenarioLogPath;

  public LiveKitLifecycleSteps() {}

  @Before
  public void setUpLiveKitLifecycleSteps(Scenario scenario) {

    String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
    String scenarioName = scenario.getName();
    String timestamp = DateUtils.generateScenarioTimestamp();

    String sanitizedFeatureName = FileUtils.sanitizeFileNameStrict(featureName);
    String sanitizedScenarioName = FileUtils.sanitizeFileNameStrict(scenarioName);

    currentScenarioLogPath =
        PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
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

  @Given(
      "a LiveKit server is running in a container with service name {string} using config profile {string}")
  public void aLiveKitServerIsRunningInAContainerWithServiceNameUsingConfigProfile(
      String serviceName, String profileName) {
    String configPath = TestConfig.resolveConfigPath(profileName);
    getOrCreateContainer(serviceName, configPath);
  }

  private void getOrCreateContainer(String serviceName, @Nullable String configPath) {
    Network network = ManagerProvider.containers().getOrCreateNetwork();

    String serviceLogPath =
        PathUtils.containerLogPath(currentScenarioLogPath, "docker", serviceName);
    LiveKitContainer liveKitContainer =
        LiveKitContainerFactory.createBddContainerWithScenarioLogs(
            serviceName, network, configPath, serviceLogPath);

    liveKitContainer.start();
    assertTrue(
        liveKitContainer.isRunning(),
        "LiveKit container with service name " + serviceName + " should be running");

    ManagerProvider.containers().registerContainer(serviceName, liveKitContainer);
  }
}
