package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.bdd.state.ContainerStateManager;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitLifecycleSteps {

    private ContainerStateManager containerManager;

    @Before
    public void setUp() {
        log.info("Setting up LiveKit lifecycle environment");
        containerManager = ContainerStateManager.getInstance();
    }

    @After
    public void tearDown() {
        log.info("Tearing down LiveKit lifecycle environment");
        if (containerManager != null) {
            containerManager.cleanup();
        }
        ContainerStateManager.reset();
    }

    @Given("a LiveKit server is running in a container with service name {string}")
    public void aLiveKitServerIsRunningInAContainerWithServiceName(String serviceName) {
        if (!containerManager.isContainerRunning(serviceName)) {
            log.info("Starting LiveKit container with service name {} for BDD test", serviceName);
            Network network = containerManager.getOrCreateNetwork();
            
            String configPath = "src/test/resources/livekit/config/config.yaml";
            LiveKitContainer liveKitContainer = LiveKitContainerFactory.createBddContainer(serviceName, network, configPath);
            
            containerManager.registerContainer(serviceName, liveKitContainer);
            liveKitContainer.start();
            
            assertTrue(containerManager.isContainerRunning(serviceName), "LiveKit container with service name " + serviceName + " should be running");
            log.info("LiveKit container started successfully at: {}", liveKitContainer.getHttpLink());
        } else {
            log.info("LiveKit container with service name {} is already running", serviceName);
        }
    }
}