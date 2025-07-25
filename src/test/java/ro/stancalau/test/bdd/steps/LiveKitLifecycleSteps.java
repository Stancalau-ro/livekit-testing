package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitLifecycleSteps {

    private static Network network;
    private static LiveKitContainer liveKitContainer;

    @Before
    public void setUp() {
        log.info("Setting up LiveKit BDD test environment");
        if (network == null) {
            network = Network.newNetwork();
        }
    }

    @After
    public void tearDown() {
        log.info("Tearing down LiveKit BDD test environment");
        if (liveKitContainer != null && liveKitContainer.isRunning()) {
            liveKitContainer.stop();
            liveKitContainer = null;
        }
        if (network != null) {
            network.close();
            network = null;
        }
    }

    @Given("a LiveKit server is running in a container")
    public void aLiveKitServerIsRunningInAContainer() {
        if (liveKitContainer == null || !liveKitContainer.isRunning()) {
            log.info("Starting LiveKit container for BDD test");
            String configPath = "src/test/resources/livekit/config/config.yaml";
            liveKitContainer = LiveKitContainerFactory.createBddContainer("bdd-livekit", network, configPath);
            liveKitContainer.start();
        }
        
        assertTrue(liveKitContainer.isRunning(), "LiveKit container should be running");
        log.info("LiveKit container started successfully at: {}", liveKitContainer.getHttpLink());
    }

    public static LiveKitContainer getLiveKitContainer() {
        return liveKitContainer;
    }

    public static Network getNetwork() {
        return network;
    }
}