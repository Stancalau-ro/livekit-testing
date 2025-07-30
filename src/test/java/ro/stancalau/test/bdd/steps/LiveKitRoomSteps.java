package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.state.RoomClientStateManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class LiveKitRoomSteps {

    public static final int POLLING_INTERVAL_MS = 500;
    private List<LivekitModels.Room> lastFetchedRooms;


    @After
    public void tearDownLiveKitRoomSteps() {
        ManagerProvider.rooms().clearAll();
        lastFetchedRooms = null;
    }

    public RoomServiceClient getRoomServiceClient(String serviceName) {
        return ManagerProvider.rooms().getRoomServiceClient(serviceName);
    }

    public LivekitModels.Room createRoom(String serviceName, String roomName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            LivekitModels.Room room = client.createRoom(roomName).execute().body();
            if (room != null) {
            }
            return room;
        } catch (Exception e) {
            log.error("Failed to create room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to create room: " + roomName, e);
        }
    }

    public List<LivekitModels.Room> getRooms(String serviceName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            List<LivekitModels.Room> rooms = client.listRooms().execute().body();
            return rooms;
        } catch (Exception e) {
            log.error("Failed to fetch rooms using service '{}': {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch rooms", e);
        }
    }

    @When("all rooms are fetched from service {string}")
    public void allRoomsAreFetchedFromService(String serviceName) {
        lastFetchedRooms = getRooms(serviceName);
    }

    @Then("the room count should be {int}")
    public void theRoomCountShouldBe(int expectedCount) {
        assertNotNull(lastFetchedRooms, "Room list should have been fetched");
        assertEquals(expectedCount, lastFetchedRooms.size(), "Room count does not match expected value");
    }

    @When("room {string} is created using service {string}")
    public void aRoomIsCreatedUsingService(String roomName, String serviceName) {
        LivekitModels.Room createdRoom = createRoom(serviceName, roomName);
        assertNotNull(createdRoom, "Room should have been created");
    }

    public List<LivekitModels.ParticipantInfo> getParticipantInfo(String serviceName, String roomName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            List<LivekitModels.ParticipantInfo> result = client.listParticipants(roomName).execute().body();
            List<LivekitModels.ParticipantInfo> participants = isNull(result) ? Collections.emptyList() : result;
            return participants;
        } catch (IOException e) {
            log.error("Failed to fetch participant info for room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch participant info for room: " + roomName, e);
        }
    }

    @Then("room {string} should exist in service {string}")
    public void theRoomShouldExistInService(String roomName, String serviceName) {
        List<LivekitModels.Room> rooms = getRooms(serviceName);
        boolean roomExists = rooms.stream().anyMatch(room -> roomName.equals(room.getName()));
        assertEquals(true, roomExists, "Room '" + roomName + "' should exist in service '" + serviceName + "'");
    }

    @Then("room {string} should have {int} active participants in service {string}")
    public void theRoomShouldHaveActiveParticipantsInService(String roomName, int expectedCount, String serviceName) {
        int maxAttempts = 20; // Increase attempts for longer wait
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

            if (participants.size() == expectedCount) {
                return;
            }

            if (attempt < maxAttempts - 1) {
                try {
                    Thread.sleep(POLLING_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);
        log.warn("Participant count check failed after {} attempts. Expected: {}, Found: {}", 
            maxAttempts, expectedCount, participants.size());
        assertEquals(expectedCount, participants.size(), "Room '" + roomName + "' should have " + expectedCount + " active participants");
    }

    @Then("participant {string} should be publishing video in room {string} using service {string}")
    public void participantShouldBePublishingVideoInRoomUsingService(String participantIdentity, String roomName, String serviceName) {
        int max = 10;
        for (int attempt = 0; attempt <= max; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

            LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                    .filter(p -> participantIdentity.equals(p.getIdentity()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(targetParticipant, "Participant '" + participantIdentity + "' should exist in room '" + roomName + "'");

            // Check for published video tracks
            long videoTrackCount = targetParticipant.getTracksList().stream()
                    .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                    .count();

            if (videoTrackCount >= 1) {
                return;
            }

            try {
                Thread.sleep(POLLING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);
        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        long videoTrackCount = targetParticipant != null ?
                targetParticipant.getTracksList().stream()
                        .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                        .count() : 0;

        log.warn("Video publishing check failed for participant '{}' after 10 seconds. Video tracks: {}, Total tracks: {}",
                participantIdentity, videoTrackCount,
                targetParticipant != null ? targetParticipant.getTracksList().size() : 0);

        assertEquals(1, videoTrackCount, "Participant '" + participantIdentity + "' should have 1 published video track after waiting");
    }

    @Then("participant {string} should not be publishing video in room {string} using service {string}")
    public void participantShouldNotBePublishingVideoInRoomUsingService(String participantIdentity, String roomName, String serviceName) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        assertNotNull(targetParticipant, "Participant '" + participantIdentity + "' should exist in room '" + roomName + "'");

        long videoTrackCount = targetParticipant.getTracksList().stream()
                .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                .count();


        assertEquals(0, videoTrackCount, "Participant '" + participantIdentity + "' should have 0 published video tracks");
    }

    @Then("participant {string} should see {int} remote video tracks in room {string} using service {string}")
    public void participantShouldSeeRemoteVideoTracksInRoomUsingService(String participantIdentity, int expectedCount, String roomName, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        long remoteVideoTrackCount = participants.stream()
                .filter(p -> !participantIdentity.equals(p.getIdentity()))
                .flatMap(p -> p.getTracksList().stream())
                .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                .count();

        assertEquals(expectedCount, remoteVideoTrackCount, "Participant '" + participantIdentity + "' should see " + expectedCount + " remote video tracks");
    }

    @When("room {string} is deleted using service {string}")
    public void roomIsDeletedUsingService(String roomName, String serviceName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            client.deleteRoom(roomName).execute();
        } catch (IOException e) {
            log.error("Failed to delete room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete room: " + roomName, e);
        }
    }
}
