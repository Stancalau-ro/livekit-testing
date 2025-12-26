package ro.stancalau.test.framework.state;

import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import java.util.List;
import java.util.Map;

@Slf4j
public class MetadataStateManager {

    private static final long POLLING_INTERVAL_MS = BrowserPollingHelper.DEFAULT_DELAY_MS;
    private static final int MAX_POLL_ATTEMPTS = BrowserPollingHelper.EXTENDED_MAX_ATTEMPTS;

    private final MeetSessionStateManager meetSessionStateManager;
    private final RoomClientStateManager roomClientStateManager;

    @Getter
    private LivekitModels.Room lastRetrievedRoomInfo;

    public MetadataStateManager(MeetSessionStateManager meetSessionStateManager,
                                 RoomClientStateManager roomClientStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
        this.roomClientStateManager = roomClientStateManager;
    }

    @SneakyThrows
    public void setRoomMetadata(String serviceName, String roomName, String metadata) {
        log.info("Setting room metadata for room {} in service {}: {}",
                roomName, serviceName, truncateForLog(metadata));
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        retrofit2.Response<LivekitModels.Room> response = client.updateRoomMetadata(roomName, metadata).execute();
        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
            log.error("Failed to update room metadata. Code: {}, Error: {}", response.code(), errorBody);
            throw new RuntimeException("Failed to update room metadata: " + response.code() + " - " + errorBody);
        }
        LivekitModels.Room updatedRoom = response.body();
        if (updatedRoom != null) {
            log.info("Room metadata updated successfully. New metadata: {}", truncateForLog(updatedRoom.getMetadata()));
        } else {
            log.warn("Room metadata update returned null body");
        }
    }

    @SneakyThrows
    public String getRoomMetadata(String serviceName, String roomName) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        retrofit2.Response<List<LivekitModels.Room>> response = client.listRooms(List.of(roomName)).execute();
        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
            log.error("Failed to list rooms. Code: {}, Error: {}", response.code(), errorBody);
            return "";
        }
        List<LivekitModels.Room> rooms = response.body();
        if (rooms == null || rooms.isEmpty()) {
            log.warn("Room {} not found in service {}", roomName, serviceName);
            return "";
        }
        LivekitModels.Room room = rooms.get(0);
        String metadata = room.getMetadata();
        log.info("Retrieved room {} - metadata: '{}', numParticipants: {}",
                roomName, truncateForLog(metadata), room.getNumParticipants());
        return metadata != null ? metadata : "";
    }

    @SneakyThrows
    public LivekitModels.Room retrieveRoomInfo(String serviceName, String roomName) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        List<LivekitModels.Room> rooms = client.listRooms(List.of(roomName)).execute().body();
        if (rooms == null || rooms.isEmpty()) {
            log.warn("Room {} not found in service {}", roomName, serviceName);
            lastRetrievedRoomInfo = null;
            return null;
        }
        lastRetrievedRoomInfo = rooms.get(0);
        log.info("Retrieved room info for {}: metadata={}", roomName,
                truncateForLog(lastRetrievedRoomInfo.getMetadata()));
        return lastRetrievedRoomInfo;
    }

    @SneakyThrows
    public void updateParticipantMetadata(String serviceName, String roomName,
                                           String identity, String metadata) {
        log.info("Updating participant {} metadata in room {} service {}: {}",
                identity, roomName, serviceName, truncateForLog(metadata));
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        retrofit2.Response<LivekitModels.ParticipantInfo> response =
                client.updateParticipant(roomName, identity, null, metadata, null).execute();
        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
            log.error("Failed to update participant metadata. Code: {}, Error: {}", response.code(), errorBody);
            throw new RuntimeException("Failed to update participant metadata: " + response.code() + " - " + errorBody);
        }
        LivekitModels.ParticipantInfo updatedParticipant = response.body();
        if (updatedParticipant != null) {
            log.info("Participant {} metadata updated successfully. New metadata: {}",
                    identity, truncateForLog(updatedParticipant.getMetadata()));
        } else {
            log.warn("Participant metadata update returned null body");
        }
    }

    @SneakyThrows
    public String getParticipantMetadata(String serviceName, String roomName, String identity) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        List<LivekitModels.ParticipantInfo> participants =
                client.listParticipants(roomName).execute().body();
        if (participants == null) {
            log.warn("No participants found in room {} service {}", roomName, serviceName);
            return "";
        }
        return participants.stream()
                .filter(p -> p.getIdentity().equals(identity))
                .map(LivekitModels.ParticipantInfo::getMetadata)
                .findFirst()
                .orElse("");
    }

    public void startListeningForRoomMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.getMetadata().startListeningForRoomMetadataEvents();
        log.debug("{} started listening for room metadata events", participantName);
    }

    public void startListeningForParticipantMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.getMetadata().startListeningForParticipantMetadataEvents();
        log.debug("{} started listening for participant metadata events", participantName);
    }

    public boolean waitForRoomMetadataEvent(String participantName, String expectedValue) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        log.info("Waiting for room metadata event for {} with value '{}'", participantName, expectedValue);
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            List<Map<String, Object>> events = meetInstance.getMetadata().getRoomMetadataEvents();
            log.debug("Poll {}/{}: {} has {} room metadata events: {}",
                    i + 1, MAX_POLL_ATTEMPTS, participantName, events.size(), events);
            for (Map<String, Object> event : events) {
                String metadata = String.valueOf(event.get("metadata"));
                if (expectedValue.equals(metadata)) {
                    log.info("Found matching room metadata event for {}: '{}'", participantName, metadata);
                    return true;
                }
            }
            BrowserPollingHelper.safeSleep(POLLING_INTERVAL_MS);
        }
        List<Map<String, Object>> finalEvents = meetInstance.getMetadata().getRoomMetadataEvents();
        log.warn("No matching room metadata event for {} after {} attempts. Final events: {}",
                participantName, MAX_POLL_ATTEMPTS, finalEvents);
        return false;
    }

    public boolean waitForParticipantMetadataEvent(String participantName,
                                                    String targetIdentity, String expectedValue) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        log.info("Waiting for participant metadata event for {} targeting {} with value '{}'",
                participantName, targetIdentity, expectedValue);
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            List<Map<String, Object>> events = meetInstance.getMetadata().getParticipantMetadataEvents();
            log.debug("Poll {}/{}: {} has {} participant metadata events",
                    i + 1, MAX_POLL_ATTEMPTS, participantName, events.size());
            for (Map<String, Object> event : events) {
                String identity = String.valueOf(event.get("participantIdentity"));
                String metadata = String.valueOf(event.get("metadata"));
                if (targetIdentity.equals(identity) && expectedValue.equals(metadata)) {
                    log.info("Found matching participant metadata event for {} targeting {}: '{}'",
                            participantName, targetIdentity, metadata);
                    return true;
                }
            }
            BrowserPollingHelper.safeSleep(POLLING_INTERVAL_MS);
        }
        List<Map<String, Object>> finalEvents = meetInstance.getMetadata().getParticipantMetadataEvents();
        log.warn("No matching participant metadata event for {} targeting {} after {} attempts. Final events: {}",
                participantName, targetIdentity, MAX_POLL_ATTEMPTS, finalEvents);
        return false;
    }

    public int getRoomMetadataEventCount(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getRoomMetadataEventCount();
    }

    public int getParticipantMetadataEventCount(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getParticipantMetadataEventCount();
    }

    public String getCurrentRoomMetadataFromBrowser(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getCurrentRoomMetadata();
    }

    public String getParticipantMetadataFromBrowser(String participantName, String targetIdentity) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getParticipantMetadata(targetIdentity);
    }

    public List<Map<String, Object>> getRoomMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getRoomMetadataEvents();
    }

    public List<Map<String, Object>> getParticipantMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getParticipantMetadataEvents();
    }

    public String generateStringOfSize(int sizeBytes) {
        return "X".repeat(sizeBytes);
    }

    public boolean pollUntilRoomMetadataEquals(String serviceName, String roomName,
                                                String expectedMetadata) {
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            String actual = getRoomMetadata(serviceName, roomName);
            if (expectedMetadata.equals(actual)) {
                return true;
            }
            BrowserPollingHelper.safeSleep(POLLING_INTERVAL_MS);
        }
        return false;
    }

    public boolean pollUntilParticipantMetadataEquals(String serviceName, String roomName,
                                                       String identity, String expectedMetadata) {
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            String actual = getParticipantMetadata(serviceName, roomName, identity);
            if (expectedMetadata.equals(actual)) {
                return true;
            }
            BrowserPollingHelper.safeSleep(POLLING_INTERVAL_MS);
        }
        return false;
    }

    public boolean participantMetadataContains(String serviceName, String roomName,
                                                String identity, String expectedSubstring) {
        String metadata = getParticipantMetadata(serviceName, roomName, identity);
        return metadata != null && metadata.contains(expectedSubstring);
    }

    public void clearAll() {
        log.info("Clearing metadata state");
        lastRetrievedRoomInfo = null;
        meetSessionStateManager.getAllMeetInstances().values().forEach(meet -> {
            try {
                meet.getMetadata().clearMetadataEvents();
            } catch (Exception e) {
                log.warn("Error clearing metadata events: {}", e.getMessage());
            }
        });
    }

    private String truncateForLog(String value) {
        if (value == null) return "null";
        return value.length() > 50 ? value.substring(0, 50) + "..." : value;
    }
}
