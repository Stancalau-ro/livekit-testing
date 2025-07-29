package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;
import ro.stancalau.test.framework.util.TestConfig;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LiveKitLifecycleSteps {

    private ContainerStateManager containerManager;
    private String defaultConfigProfile = "basic";

    @Before
    public void setUpLiveKitLifecycleSteps() {
        containerManager = ContainerStateManager.getInstance();
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

            LiveKitContainer liveKitContainer = LiveKitContainerFactory.createBddContainer(serviceName, network, configPath);

            liveKitContainer.start();
            assertTrue(liveKitContainer.isRunning(), "LiveKit container with service name " + serviceName + " should be running");

            log.info("LiveKit container started successfully at: {}", liveKitContainer.getHttpLink());
            containerManager.registerContainer(serviceName, liveKitContainer);
        } else {
            log.info("LiveKit container with service name {} is already running", serviceName);
        }
    }
}