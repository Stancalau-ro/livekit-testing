package ro.stancalau.test.framework.state;

import io.livekit.server.RoomServiceClient;
import java.util.List;
import java.util.Map;
import livekit.LivekitModels;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

@Slf4j
public class MetadataStateManager {

    private static final long POLL_TIMEOUT_MS = 10_000;

    private final MeetSessionStateManager meetSessionStateManager;
    private final RoomClientStateManager roomClientStateManager;

    @Getter
    private LivekitModels.Room lastRetrievedRoomInfo;

    public MetadataStateManager(
            MeetSessionStateManager meetSessionStateManager, RoomClientStateManager roomClientStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
        this.roomClientStateManager = roomClientStateManager;
    }

    @SneakyThrows
    public void setRoomMetadata(String serviceName, String roomName, String metadata) {
        log.info(
                "Setting room metadata for room {} in service {}: {}", roomName, serviceName, truncateForLog(metadata));
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        retrofit2.Response<LivekitModels.Room> response =
                client.updateRoomMetadata(roomName, metadata).execute();
        if (!response.isSuccessful()) {
            String errorBody =
                    response.errorBody() != null ? response.errorBody().string() : "unknown";
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
        retrofit2.Response<List<LivekitModels.Room>> response =
                client.listRooms(List.of(roomName)).execute();
        if (!response.isSuccessful()) {
            String errorBody =
                    response.errorBody() != null ? response.errorBody().string() : "unknown";
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
        log.info(
                "Retrieved room {} - metadata: '{}', numParticipants: {}",
                roomName,
                truncateForLog(metadata),
                room.getNumParticipants());
        return metadata != null ? metadata : "";
    }

    @SneakyThrows
    public LivekitModels.Room retrieveRoomInfo(String serviceName, String roomName) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        List<LivekitModels.Room> rooms =
                client.listRooms(List.of(roomName)).execute().body();
        if (rooms == null || rooms.isEmpty()) {
            log.warn("Room {} not found in service {}", roomName, serviceName);
            lastRetrievedRoomInfo = null;
            return null;
        }
        lastRetrievedRoomInfo = rooms.get(0);
        log.info(
                "Retrieved room info for {}: metadata={}",
                roomName,
                truncateForLog(lastRetrievedRoomInfo.getMetadata()));
        return lastRetrievedRoomInfo;
    }

    @SneakyThrows
    public void updateParticipantMetadata(String serviceName, String roomName, String identity, String metadata) {
        log.info(
                "Updating participant {} metadata in room {} service {}: {}",
                identity,
                roomName,
                serviceName,
                truncateForLog(metadata));
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        retrofit2.Response<LivekitModels.ParticipantInfo> response = client.updateParticipant(
                        roomName, identity, null, metadata, null)
                .execute();
        if (!response.isSuccessful()) {
            String errorBody =
                    response.errorBody() != null ? response.errorBody().string() : "unknown";
            log.error("Failed to update participant metadata. Code: {}, Error: {}", response.code(), errorBody);
            throw new RuntimeException("Failed to update participant metadata: " + response.code() + " - " + errorBody);
        }
        LivekitModels.ParticipantInfo updatedParticipant = response.body();
        if (updatedParticipant != null) {
            log.info(
                    "Participant {} metadata updated successfully. New metadata: {}",
                    identity,
                    truncateForLog(updatedParticipant.getMetadata()));
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
        boolean found = BrowserPollingHelper.pollForCondition(
                () -> {
                    List<Map<String, Object>> events =
                            meetInstance.getMetadata().getRoomMetadataEvents();
                    return events.stream()
                            .map(e -> String.valueOf(e.get("metadata")))
                            .anyMatch(expectedValue::equals);
                },
                POLL_TIMEOUT_MS,
                BrowserPollingHelper.DEFAULT_DELAY_MS);
        if (found) {
            log.info("Found matching room metadata event for {}: '{}'", participantName, expectedValue);
        } else {
            List<Map<String, Object>> finalEvents = meetInstance.getMetadata().getRoomMetadataEvents();
            log.warn(
                    "No matching room metadata event for {} after {}ms. Final events: {}",
                    participantName,
                    POLL_TIMEOUT_MS,
                    finalEvents);
        }
        return found;
    }

    public boolean waitForParticipantMetadataEvent(
            String participantName, String targetIdentity, String expectedValue) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        log.info(
                "Waiting for participant metadata event for {} targeting {} with value '{}'",
                participantName,
                targetIdentity,
                expectedValue);
        boolean found = BrowserPollingHelper.pollForCondition(
                () -> {
                    List<Map<String, Object>> events =
                            meetInstance.getMetadata().getParticipantMetadataEvents();
                    return events.stream()
                            .anyMatch(e -> targetIdentity.equals(String.valueOf(e.get("participantIdentity")))
                                    && expectedValue.equals(String.valueOf(e.get("metadata"))));
                },
                POLL_TIMEOUT_MS,
                BrowserPollingHelper.DEFAULT_DELAY_MS);
        if (found) {
            log.info(
                    "Found matching participant metadata event for {} targeting {}: '{}'",
                    participantName,
                    targetIdentity,
                    expectedValue);
        } else {
            List<Map<String, Object>> finalEvents = meetInstance.getMetadata().getParticipantMetadataEvents();
            log.warn(
                    "No matching participant metadata event for {} targeting {} after {}ms. Final events: {}",
                    participantName,
                    targetIdentity,
                    POLL_TIMEOUT_MS,
                    finalEvents);
        }
        return found;
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

    public boolean pollUntilRoomMetadataEquals(String serviceName, String roomName, String expectedMetadata) {
        return BrowserPollingHelper.pollForCondition(
                () -> expectedMetadata.equals(getRoomMetadata(serviceName, roomName)),
                POLL_TIMEOUT_MS,
                BrowserPollingHelper.DEFAULT_DELAY_MS);
    }

    public boolean pollUntilParticipantMetadataEquals(
            String serviceName, String roomName, String identity, String expectedMetadata) {
        return BrowserPollingHelper.pollForCondition(
                () -> expectedMetadata.equals(getParticipantMetadata(serviceName, roomName, identity)),
                POLL_TIMEOUT_MS,
                BrowserPollingHelper.DEFAULT_DELAY_MS);
    }

    public boolean participantMetadataContains(
            String serviceName, String roomName, String identity, String expectedSubstring) {
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
