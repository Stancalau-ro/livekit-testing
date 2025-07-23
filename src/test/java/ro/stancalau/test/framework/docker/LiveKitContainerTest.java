package ro.stancalau.test.framework.docker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LiveKitContainerTest {

    @Test
    public void testLiveKitContainerCreationAndStartup() {
        log.info("Starting LiveKit container test");
        
        try (Network network = Network.newNetwork()) {
            String configPath = "src/test/resources/livekit/config/config.yaml";
            String livekitVersion = "v1.8.4";
            LiveKitContainer container = LiveKitContainer.createContainer("test-livekit", network, configPath, livekitVersion);
            
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
            log.error("LiveKit container test failed", e);
            throw e;
        }
    }
}