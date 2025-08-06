package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.MockHttpServerContainer;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;
import ro.stancalau.test.framework.webhook.WebhookEvent;
import ro.stancalau.test.framework.webhook.WebhookEventPoller;
import ro.stancalau.test.framework.webhook.WebhookService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitWebhookSteps {

    private final WebhookService webhookService;
    private final WebhookEventPoller webhookEventPoller;
    private String currentScenarioLogPath;

    public LiveKitWebhookSteps() {
        this.webhookService = new WebhookService();
        this.webhookEventPoller = new WebhookEventPoller(webhookService);
    }

    @Before
    public void setUpWebhookSteps(Scenario scenario) {
                
        String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();
        
        String sanitizedFeatureName = FileUtils.sanitizeFileNameStrict(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileNameStrict(scenarioName);
        
        currentScenarioLogPath = "out/bdd/scenarios/" + sanitizedFeatureName + "/" + 
                                sanitizedScenarioName + "/" + timestamp;
    }

    @After
    public void tearDownWebhookSteps() {
        if (ManagerProvider.containers() != null) {
            ManagerProvider.containers().cleanup(MockHttpServerContainer.class);
        }
    }

    @Given("a mock HTTP server is running in a container with service name {string}")
    public void aMockHttpServerIsRunningInAContainerWithServiceName(String serviceName) {
        if (!ManagerProvider.containers().isContainerRunning(serviceName)) {
            log.info("Starting mock HTTP server container with service name {}", serviceName);
            Network network = ManagerProvider.containers().getOrCreateNetwork();

            MockHttpServerContainer mockServer = new MockHttpServerContainer(currentScenarioLogPath, serviceName);
            mockServer.withNetworkAliasAndStart(network, serviceName);

            assertTrue(mockServer.isRunning(), "Mock HTTP server container should be running");
            log.info("Mock HTTP server started successfully at: {}", mockServer.getNetworkUrl(serviceName));
            
            ManagerProvider.containers().registerContainer(serviceName, mockServer);
        } else {
            log.info("Mock HTTP server container with service name {} is already running", serviceName);
        }
    }

    @Then("{string} should have received a request containing {string}")
    public void theMockServerShouldHaveReceivedARequestContaining(String serviceName, String expectedContent) {
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
                    (e.getTrack() != null ? " (track type: " + e.getTrack().getEffectiveType() + ")" : "") +
                    (e.getRoom() != null ? " (room: " + e.getRoom().getName() + ")" : "")).toList());
            }
            
            log.info("Found {} event for track type {} in room {}", eventType, trackType, roomName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }
    
    @When("{string} webhook events are cleared")
    public void theMockServerWebhookEventsAreCleared(String serviceName) {
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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

    @Then("{string} should have received a {string} event for participant {string} in room {string} with attributes {string}")
    public void theMockServerShouldHaveReceivedAnEventForParticipantWithAttributes(String serviceName, String eventType, String participantIdentity, String roomName, String expectedAttributes) {
        MockHttpServerContainer mockServer = ManagerProvider.containers().getContainer(serviceName, MockHttpServerContainer.class);
        
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
            
            WebhookEvent event = foundEvent.get();
            if (event.getParticipant() == null || event.getParticipant().getAttributes() == null) {
                fail("Webhook event for participant '" + participantIdentity + "' does not contain attributes");
            }
            
            Map<String, String> actualAttributes = event.getParticipant().getAttributes();
            Map<String, String> expectedAttributesMap = parseAttributes(expectedAttributes);
            
            for (Map.Entry<String, String> expectedEntry : expectedAttributesMap.entrySet()) {
                String key = expectedEntry.getKey();
                String expectedValue = expectedEntry.getValue();
                String actualValue = actualAttributes.get(key);
                
                assertEquals(expectedValue, actualValue, 
                    String.format("Expected attribute '%s' to have value '%s' but was '%s'. All attributes: %s", 
                        key, expectedValue, actualValue, actualAttributes));
            }
            
            log.info("Found {} event for participant {} in room {} with correct attributes: {}", 
                eventType, participantIdentity, roomName, actualAttributes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve webhook events from mock server", e);
        }
    }
    
    private Map<String, String> parseAttributes(String attributesString) {
        Map<String, String> attributes = new HashMap<>();
        if (attributesString == null || attributesString.trim().isEmpty()) {
            return attributes;
        }
        
        String[] pairs = attributesString.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim().replaceAll("\\\\,", ",");
                attributes.put(key, value);
            }
        }
        return attributes;
    }

}