package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.bdd.state.ContainerStateManager;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LiveKitLifecycleSteps {

    private ContainerStateManager containerManager;

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

    @Given("a LiveKit server is running in a container with service name {string}")
    public void aLiveKitServerIsRunningInAContainerWithServiceName(String serviceName) {
        getOrCreateContainer(serviceName, null);
    }

    /**
     * @param configPath ConfigPath pattern: src/test/resources/livekit/config/config.yaml
     */
    @Given("a LiveKit server is running in a container with service name {string} using config {string}")
    public void aLiveKitServerIsRunningInAContainerWithServiceName(String serviceName, String configPath) {
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