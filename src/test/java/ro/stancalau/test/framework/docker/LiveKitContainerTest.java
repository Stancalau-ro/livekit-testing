package ro.stancalau.test.framework.docker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.factory.LiveKitContainerFactory;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitContainerTest {

    @Test
    public void testLiveKitContainerCreationAndStartup() {
        log.info("Starting LiveKit container test");
        
        try (Network network = Network.newNetwork()) {
            LiveKitContainer container = LiveKitContainerFactory.createIntegrationTestContainer("test-livekit", network);
            
            assertNotNull(container, "Container should be created");
            
            log.info("Starting LiveKit container...");
            container.start();
            
            assertTrue(container.isRunning(), "Container should be running");
            
            String httpLink = container.getHttpLink();
            assertNotNull(httpLink, "HTTP link should not be null");
            log.info("LiveKit HTTP endpoint: {}", httpLink);
            
            String networkWs = container.getNetworkWs();
            assertNotNull(networkWs, "Network WebSocket URL should not be null");
            log.info("LiveKit Network WebSocket: {}", networkWs);
            
            log.info("LiveKit container test completed successfully");
        } catch (Exception e) {
            fail("LiveKit container test failed", e);
        }
    }
}