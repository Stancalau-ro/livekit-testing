package ro.stancalau.test.framework.webhook;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public class WebhookEventPoller {
    
    private final WebhookService webhookService;
    private final Duration timeout;
    private final Duration pollInterval;
    
    public WebhookEventPoller(WebhookService webhookService) {
        this(webhookService, Duration.ofSeconds(10), Duration.ofMillis(200));
    }
    
    public WebhookEventPoller(WebhookService webhookService, Duration timeout, Duration pollInterval) {
        this.webhookService = webhookService;
        this.timeout = timeout;
        this.pollInterval = pollInterval;
    }
    
    public Optional<WebhookEvent> waitForEvent(MockServerClient mockServerClient, Predicate<WebhookEvent> eventMatcher) {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(timeout);
        
        while (Instant.now().isBefore(endTime)) {
            try {
                List<WebhookEvent> webhookEvents = webhookService.getWebhookEvents(mockServerClient);
                
                Optional<WebhookEvent> matchingEvent = webhookEvents.stream()
                    .filter(eventMatcher)
                    .findFirst();
                
                if (matchingEvent.isPresent()) {
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    log.debug("Found matching webhook event after {}ms", elapsed.toMillis());
                    return matchingEvent;
                }
                
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Webhook event polling interrupted", e);
                break;
            } catch (Exception e) {
                log.warn("Error during webhook event polling", e);
                try {
                    Thread.sleep(pollInterval.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        Duration elapsed = Duration.between(startTime, Instant.now());
        log.warn("No matching webhook event found after {}ms timeout", elapsed.toMillis());
        return Optional.empty();
    }
    
    public Optional<WebhookEvent> waitForEventByType(MockServerClient mockServerClient, String eventType) {
        return waitForEvent(mockServerClient, event -> eventType.equals(event.getEvent()));
    }
    
    public Optional<WebhookEvent> waitForEventByTypeAndRoom(MockServerClient mockServerClient, String eventType, String roomName) {
        return waitForEvent(mockServerClient, event -> 
            eventType.equals(event.getEvent()) && 
            event.getRoom() != null && 
            roomName.equals(event.getRoom().getName())
        );
    }
    
    public Optional<WebhookEvent> waitForEventByTypeAndParticipant(MockServerClient mockServerClient, String eventType, String participantIdentity) {
        return waitForEvent(mockServerClient, event -> 
            eventType.equals(event.getEvent()) && 
            event.getParticipant() != null && 
            participantIdentity.equals(event.getParticipant().getIdentity())
        );
    }
    
    public Optional<WebhookEvent> waitForEventByTypeParticipantAndRoom(MockServerClient mockServerClient, String eventType, String participantIdentity, String roomName) {
        return waitForEvent(mockServerClient, event -> 
            eventType.equals(event.getEvent()) && 
            event.getParticipant() != null && 
            participantIdentity.equals(event.getParticipant().getIdentity()) &&
            event.getRoom() != null && 
            roomName.equals(event.getRoom().getName())
        );
    }
    
    public Optional<WebhookEvent> waitForEventByTypeAndTrackType(MockServerClient mockServerClient, String eventType, String trackType) {
        return waitForEvent(mockServerClient, event -> 
            eventType.equals(event.getEvent()) && 
            event.getTrack() != null && 
            trackType.equals(event.getTrack().getType())
        );
    }
    
    public Optional<WebhookEvent> waitForEventByTypeTrackTypeAndRoom(MockServerClient mockServerClient, String eventType, String trackType, String roomName) {
        return waitForEvent(mockServerClient, event -> 
            eventType.equals(event.getEvent()) && 
            event.getTrack() != null && 
            trackType.equals(event.getTrack().getType()) &&
            event.getRoom() != null && 
            roomName.equals(event.getRoom().getName())
        );
    }
}