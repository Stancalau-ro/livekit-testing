package ro.stancalau.test.framework.state;

import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.docker.LiveKitContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RoomClientStateManager {

    private final ContainerStateManager containerStateManager;
    private final Map<String, RoomServiceClient> roomServiceClients = new HashMap<>();

    public RoomClientStateManager(ContainerStateManager containerStateManager) {
        this.containerStateManager = containerStateManager;
    }

    public RoomServiceClient getRoomServiceClient(String serviceName) {
        return roomServiceClients.computeIfAbsent(serviceName, this::createRoomServiceClient);
    }

    public void clearAll() {
        log.info("Clearing all RoomServiceClients and state");
        roomServiceClients.clear();
    }

    private RoomServiceClient createRoomServiceClient(String serviceName) {
        log.info("Creating RoomServiceClient for service: {}", serviceName);

        LiveKitContainer container = containerStateManager.getContainer(serviceName, LiveKitContainer.class);
        if (container == null) {
            throw new IllegalArgumentException("No LiveKit container found for service: " + serviceName);
        }

        if (!container.isRunning()) {
            throw new IllegalStateException("LiveKit container for service '" + serviceName + "' is not running");
        }

        String wsUrl = container.getlocalWs();
        log.debug("Creating RoomServiceClient with URL: {} for service: {}", wsUrl, serviceName);

        RoomServiceClient client = RoomServiceClient.create(
                "http://" + container.getHost() + ":" + container.getMappedPort(LiveKitContainer.HTTP_PORT),
                LiveKitContainer.API_KEY,
                LiveKitContainer.SECRET
        );

        waitUntilLiveKitServerIsCompletelyInitialized(client);

        return client;
    }

    @SneakyThrows
    private static void waitUntilLiveKitServerIsCompletelyInitialized(RoomServiceClient client) {
        for (int i = 0; i < 30; i++) {
            LivekitModels.Room testRoom = null;
            String initTestRoomName = "initTest";
            try {
                testRoom = client.createRoom(initTestRoomName).execute().body();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (testRoom == null) {
                Thread.sleep(300);
                continue;
            }
            client.deleteRoom(initTestRoomName).execute();
            return;
        }
        throw new IllegalStateException("Live Kit server has not been fully or correctly initialized!");
    }

}