package ro.stancalau.test.framework.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;

@Slf4j
public class WebhookService {

    private static final String INIT_TEST_ROOM_NAME = "initTest";

    private final ObjectMapper objectMapper;

    public WebhookService() {
        this.objectMapper = new ObjectMapper();
    }

    public List<WebhookEvent> getWebhookEvents(MockServerClient mockServerClient) {
        try {
            HttpRequest[] recordedRequests = mockServerClient.retrieveRecordedRequests(null);

            List<WebhookEvent> allEvents = Arrays.stream(recordedRequests)
                    .filter(req -> req.getPath().getValue().equals("/webhook"))
                    .filter(req -> req.getMethod().getValue().equals("POST"))
                    .map(this::parseWebhookEvent)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            long initTestCount =
                    allEvents.stream().filter(this::isInitTestEvent).count();
            if (initTestCount > 0) {
                log.debug("Filtering out {} initTest room events", initTestCount);
            }

            return allEvents.stream().filter(event -> !isInitTestEvent(event)).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to retrieve webhook events", e);
            throw new RuntimeException("Failed to retrieve webhook events", e);
        }
    }

    private boolean isInitTestEvent(WebhookEvent event) {
        return event.getRoom() != null
                && INIT_TEST_ROOM_NAME.equals(event.getRoom().getName());
    }

    public Optional<WebhookEvent> findEventByType(List<WebhookEvent> events, String eventType) {
        return events.stream()
                .filter(event -> eventType.equals(event.getEvent()))
                .findFirst();
    }

    public List<WebhookEvent> findEventsByType(List<WebhookEvent> events, String eventType) {
        return events.stream()
                .filter(event -> eventType.equals(event.getEvent()))
                .collect(Collectors.toList());
    }

    private Optional<WebhookEvent> parseWebhookEvent(HttpRequest request) {
        try {
            String jsonBody = request.getBodyAsString();
            if (jsonBody == null || jsonBody.trim().isEmpty()) {
                log.warn("Empty webhook body in request");
                return Optional.empty();
            }

            WebhookEvent event = objectMapper.readValue(jsonBody, WebhookEvent.class);
            log.debug("Parsed webhook event: {}", event.getEvent());
            return Optional.of(event);

        } catch (Exception e) {
            log.error("Failed to parse webhook event from request body: {}", request.getBodyAsString(), e);
            return Optional.empty();
        }
    }

    public void clearRecordedEvents(MockServerClient mockServerClient) {
        try {
            mockServerClient.clear(HttpRequest.request().withPath("/webhook").withMethod("POST"));
            log.info("Cleared all recorded webhook events from MockServer");
        } catch (Exception e) {
            log.error("Failed to clear recorded webhook events", e);
            throw new RuntimeException("Failed to clear recorded webhook events", e);
        }
    }
}
