package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.MockHttpServerContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.webhook.WebhookEvent;
import ro.stancalau.test.framework.webhook.WebhookEventPoller;
import ro.stancalau.test.framework.webhook.WebhookService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitWebhookSteps {

    private ContainerStateManager containerManager;
    private WebhookService webhookService;
    private WebhookEventPoller webhookEventPoller;
    private String currentScenarioLogPath;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Before
    public void setUpWebhookSteps(Scenario scenario) {
        containerManager = ContainerStateManager.getInstance();
        webhookService = new WebhookService();
        webhookEventPoller = new WebhookEventPoller(webhookService);
                
        String featureName = extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String sanitizedFeatureName = sanitizeFileName(featureName);
        String sanitizedScenarioName = sanitizeFileName(scenarioName);
        
        currentScenarioLogPath = "out/bdd/scenarios/" + sanitizedFeatureName + "/" + 
                                sanitizedScenarioName + "/" + timestamp;
    }

    @After
    public void tearDownWebhookSteps() {
        if (containerManager != null) {
            containerManager.cleanup(MockHttpServerContainer.class);
        }
    }

    @Given("a mock HTTP server is running in a container with service name {string}")
    public void aMockHttpServerIsRunningInAContainerWithServiceName(String serviceName) {
        if (!containerManager.isContainerRunning(serviceName)) {
            log.info("Starting mock HTTP server container with service name {}", serviceName);
            Network network = containerManager.getOrCreateNetwork();

            MockHttpServerContainer mockServer = new MockHttpServerContainer(currentScenarioLogPath, serviceName);
            mockServer.withNetworkAliasAndStart(network, serviceName);

            assertTrue(mockServer.isRunning(), "Mock HTTP server container should be running");
            log.info("Mock HTTP server started successfully at: {}", mockServer.getNetworkUrl(serviceName));
            
            containerManager.registerContainer(serviceName, mockServer);
        } else {
            log.info("Mock HTTP server container with service name {} is already running", serviceName);
        }
    }

    @Then("{string} should have received a request containing {string}")
    public void theMockServerShouldHaveReceivedARequestContaining(String serviceName, String expectedContent) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            
            Optional<WebhookEvent> foundEvent = webhookEventPoller.waitForEventByType(mockServerClient, expectedContent);
            
            if (foundEvent.isEmpty()) {
                List<WebhookEvent> allEvents = webhookService.getWebhookEvents(mockServerClient);
                fail("Expected mock server to have received webhook event of type '" + expectedContent + 
                    "' but received events: " + allEvents.stream().map(WebhookEvent::getEvent).toList());
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }

    @Then("{string} should have received a {string} event for room {string}")
    public void theMockServerShouldHaveReceivedAnEventForRoom(String serviceName, String eventType, String roomName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            
            Optional<WebhookEvent> foundEvent = webhookEventPoller.waitForEventByTypeAndRoom(mockServerClient, eventType, roomName);
            
            if (foundEvent.isEmpty()) {
                List<WebhookEvent> allEvents = webhookService.getWebhookEvents(mockServerClient);
                fail("Expected mock server to have received '" + eventType + "' event for room '" + roomName + 
                    "' but received events: " + allEvents.stream().map(e -> e.getEvent() + 
                    (e.getRoom() != null ? " (room: " + e.getRoom().getName() + ")" : "")).toList());
            }
            
            log.info("Found {} event for room {}", eventType, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }

    @Then("{string} should have received a {string} event for participant {string} in room {string}")
    public void theMockServerShouldHaveReceivedAnEventForParticipant(String serviceName, String eventType, String participantIdentity, String roomName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            Optional<WebhookEvent> foundEvent = webhookEventPoller.waitForEventByTypeParticipantAndRoom(mockServerClient, eventType, participantIdentity, roomName);
            
            if (foundEvent.isEmpty()) {
                List<WebhookEvent> allEvents = webhookService.getWebhookEvents(mockServerClient);
                fail("Expected mock server to have received '" + eventType + "' event for participant '" + participantIdentity + 
                    "' in room '" + roomName + "' but received events: " + allEvents.stream().map(e -> e.getEvent() + 
                    (e.getParticipant() != null ? " (participant: " + e.getParticipant().getIdentity() + ")" : "") +
                    (e.getRoom() != null ? " (room: " + e.getRoom().getName() + ")" : "")).toList());
            }
            
            log.info("Found {} event for participant {} in room {}", eventType, participantIdentity, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }

    @Then("{string} should have received a {string} event for track type {string} in room {string}")
    public void theMockServerShouldHaveReceivedAnEventForTrackType(String serviceName, String eventType, String trackType, String roomName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            Optional<WebhookEvent> foundEvent = webhookEventPoller.waitForEventByTypeTrackTypeAndRoom(mockServerClient, eventType, trackType, roomName);
            
            if (foundEvent.isEmpty()) {
                List<WebhookEvent> allEvents = webhookService.getWebhookEvents(mockServerClient);
                fail("Expected mock server to have received '" + eventType + "' event for track type '" + trackType + 
                    "' in room '" + roomName + "' but received events: " + allEvents.stream().map(e -> e.getEvent() + 
                    (e.getTrack() != null ? " (track type: " + e.getTrack().getType() + ")" : "") +
                    (e.getRoom() != null ? " (room: " + e.getRoom().getName() + ")" : "")).toList());
            }
            
            log.info("Found {} event for track type {} in room {}", eventType, trackType, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }
    
    @When("{string} webhook events are cleared")
    public void theMockServerWebhookEventsAreCleared(String serviceName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            webhookService.clearRecordedEvents(mockServerClient);
            log.info("Cleared all webhook events from mock server {}", serviceName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear webhook events from mock server", e);
        }
    }
    
    @Then("{string} should have received exactly {int} webhook event(s)")
    public void theMockServerShouldHaveReceivedExactlyWebhookEvents(String serviceName, int expectedCount) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            List<WebhookEvent> webhookEvents = webhookService.getWebhookEvents(mockServerClient);
            
            if (expectedCount != webhookEvents.size()) {
                log.warn("Event count mismatch. Expected: {}, Found: {}. Detailed events:", expectedCount, webhookEvents.size());
                webhookEvents.forEach(event -> {
                    log.warn("  - Event: {}, Room: {}, ID: {}, CreatedAt: {}", 
                        event.getEvent(), 
                        event.getRoom() != null ? event.getRoom().getName() : "N/A",
                        event.getId(),
                        event.getCreatedAt());
                });
            }
            
            assertEquals(expectedCount, webhookEvents.size(), 
                String.format("Expected exactly %d webhook events but found %d. Events: %s", 
                    expectedCount, webhookEvents.size(), 
                    webhookEvents.stream().map(WebhookEvent::getEvent).toList()));
            
            log.info("Verified mock server has exactly {} webhook events", expectedCount);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }
    
    @Then("{string} should not have received a {string} event for room {string}")
    public void theMockServerShouldNotHaveReceivedAnEventForRoom(String serviceName, String eventType, String roomName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            List<WebhookEvent> webhookEvents = webhookService.getWebhookEvents(mockServerClient);
            
            Optional<WebhookEvent> foundEvent = webhookEvents.stream()
                .filter(event -> eventType.equals(event.getEvent()))
                .filter(event -> event.getRoom() != null && roomName.equals(event.getRoom().getName()))
                .findFirst();
            
            assertFalse(foundEvent.isPresent(), 
                String.format("Expected NOT to find '%s' event for room '%s' but it was present", 
                    eventType, roomName));
            
            log.info("Verified no {} event exists for room {}", eventType, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }
    
    @Then("{string} should not have received a {string} event for participant {string} in room {string}")
    public void theMockServerShouldNotHaveReceivedAnEventForParticipant(String serviceName, String eventType, String participantIdentity, String roomName) {
        MockHttpServerContainer mockServer = containerManager.getContainer(serviceName, MockHttpServerContainer.class);
        
        if (mockServer == null) {
            throw new RuntimeException("Mock server with service name " + serviceName + " not found");
        }
        
        try {
            MockServerClient mockServerClient = mockServer.getMockServerClient();
            List<WebhookEvent> webhookEvents = webhookService.getWebhookEvents(mockServerClient);
            
            Optional<WebhookEvent> foundEvent = webhookEvents.stream()
                .filter(event -> eventType.equals(event.getEvent()))
                .filter(event -> event.getParticipant() != null && participantIdentity.equals(event.getParticipant().getIdentity()))
                .filter(event -> event.getRoom() != null && roomName.equals(event.getRoom().getName()))
                .findFirst();
            
            assertFalse(foundEvent.isPresent(), 
                String.format("Expected NOT to find '%s' event for participant '%s' in room '%s' but it was present", 
                    eventType, participantIdentity, roomName));
            
            log.info("Verified no {} event exists for participant {} in room {}", eventType, participantIdentity, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
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
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_+", "_")
                      .replaceAll("^_|_$", "");
    }
}