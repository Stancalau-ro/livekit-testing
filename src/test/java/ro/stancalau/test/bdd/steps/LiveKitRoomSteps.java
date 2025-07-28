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

    private RoomClientStateManager roomClientManager;
    private List<LivekitModels.Room> lastFetchedRooms;

    @Before
    public void setUpLiveKitRoomSteps() {
        roomClientManager = RoomClientStateManager.getInstance();
    }

    @After
    public void tearDownLiveKitRoomSteps() {
        if (roomClientManager != null) {
            roomClientManager.clearAll();
        }
        lastFetchedRooms = null;
    }

    public RoomServiceClient getRoomServiceClient(String serviceName) {
        return roomClientManager.getRoomServiceClient(serviceName);
    }

    public LivekitModels.Room createRoom(String serviceName, String roomName) {
        log.info("Creating room '{}' using service '{}'", roomName, serviceName);

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            LivekitModels.Room room = client.createRoom(roomName).execute().body();
            if (room != null) {
                log.info("Successfully created room '{}' with SID: {}", roomName, room.getSid());
            }
            return room;
        } catch (Exception e) {
            log.error("Failed to create room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to create room: " + roomName, e);
        }
    }

    public List<LivekitModels.Room> getRooms(String serviceName) {
        log.info("Fetching all rooms using service '{}'", serviceName);

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            List<LivekitModels.Room> rooms = client.listRooms().execute().body();
            log.info("Successfully fetched {} rooms using service '{}'", rooms.size(), serviceName);
            return rooms;
        } catch (Exception e) {
            log.error("Failed to fetch rooms using service '{}': {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch rooms", e);
        }
    }

    @When("I fetch all rooms from service {string}")
    public void fetchAllRoomsFromService(String serviceName) {
        lastFetchedRooms = getRooms(serviceName);
    }

    @Then("the room count should be {int}")
    public void theRoomCountShouldBe(int expectedCount) {
        assertNotNull(lastFetchedRooms, "Room list should have been fetched");
        assertEquals(expectedCount, lastFetchedRooms.size(), "Room count does not match expected value");
    }

    @When("I create a room {string} using service {string}")
    public void createARoomUsingService(String roomName, String serviceName) {
        LivekitModels.Room createdRoom = createRoom(serviceName, roomName);
        assertNotNull(createdRoom, "Room should have been created");
        log.info("Created room '{}' with SID: {} using service '{}'", roomName, createdRoom.getSid(), serviceName);
    }

    public List<LivekitModels.ParticipantInfo> getParticipantInfo(String serviceName, String roomName) {
        log.info("Fetching participant info for room '{}' using service '{}'", roomName, serviceName);
        
        RoomServiceClient client = getRoomServiceClient(serviceName);
        
        try {
            List<LivekitModels.ParticipantInfo> result = client.listParticipants(roomName).execute().body();
            List<LivekitModels.ParticipantInfo> participants = isNull(result) ? Collections.emptyList() : result;
            log.info("Successfully fetched {} participants for room '{}' using service '{}'", participants.size(), roomName, serviceName);
            return participants;
        } catch (IOException e) {
            log.error("Failed to fetch participant info for room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch participant info for room: " + roomName, e);
        }
    }

    @Then("the room {string} should exist in service {string}")
    public void theRoomShouldExistInService(String roomName, String serviceName) {
        List<LivekitModels.Room> rooms = getRooms(serviceName);
        boolean roomExists = rooms.stream().anyMatch(room -> roomName.equals(room.getName()));
        assertEquals(true, roomExists, "Room '" + roomName + "' should exist in service '" + serviceName + "'");
        log.info("Verified room '{}' exists in service '{}'", roomName, serviceName);
    }

    @Then("the room {string} should have {int} active participants in service {string}")
    public void theRoomShouldHaveActiveParticipantsInService(String roomName, int expectedCount, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);
        assertEquals(expectedCount, participants.size(), "Room '" + roomName + "' should have " + expectedCount + " active participants");
        log.info("Verified room '{}' has {} active participants in service '{}'", roomName, expectedCount, serviceName);
    }

}
