package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.state.ContainerStateManager;
import ro.stancalau.test.bdd.state.RoomClientStateManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class LiveKitRoomSteps {

    private ContainerStateManager containerManager;
    private RoomClientStateManager roomClientManager;
    private List<LivekitModels.Room> lastFetchedRooms;

    @Before
    public void setUpLiveKitRoomSteps() {
        containerManager = ContainerStateManager.getInstance();
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

}
