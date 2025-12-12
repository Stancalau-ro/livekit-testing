package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.*;

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
            return client.createRoom(roomName).execute().body();
        } catch (Exception e) {
            log.error("Failed to create room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to create room: " + roomName, e);
        }
    }

    public List<LivekitModels.Room> getRooms(String serviceName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            return client.listRooms().execute().body();
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
            return isNull(result) ? Collections.emptyList() : result;
        } catch (IOException e) {
            log.error("Failed to fetch participant info for room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch participant info for room: " + roomName, e);
        }
    }

    @Then("room {string} should exist in service {string}")
    public void theRoomShouldExistInService(String roomName, String serviceName) {
        List<LivekitModels.Room> rooms = getRooms(serviceName);
        boolean roomExists = rooms.stream().anyMatch(room -> roomName.equals(room.getName()));
        assertTrue(roomExists, "Room '" + roomName + "' should exist in service '" + serviceName + "'");
    }

    @Then("room {string} should have {int} active participants in service {string}")
    public void theRoomShouldHaveActiveParticipantsInService(String roomName, int expectedCount, String serviceName) {
        int maxAttempts = 20;
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

    @Then("participant {string} should have {int} remote video tracks available in room {string} using service {string}")
    public void participantShouldHaveRemoteVideoTracksAvailableInRoomUsingService(String participantIdentity, int expectedCount, String roomName, String serviceName) {
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

    @When("participant {string} is removed from room {string} using service {string}")
    public void participantIsRemovedFromRoomUsingService(String participantIdentity, String roomName, String serviceName) {
        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            client.removeParticipant(roomName, participantIdentity).execute();
            log.info("Successfully removed participant '{}' from room '{}' using service '{}'", participantIdentity, roomName, serviceName);
        } catch (IOException e) {
            log.error("Failed to remove participant '{}' from room '{}' using service '{}': {}", participantIdentity, roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to remove participant: " + participantIdentity, e);
        }
    }

    @Then("participant {string} should not exist in room {string} using service {string}")
    public void participantShouldNotExistInRoomUsingService(String participantIdentity, String roomName, String serviceName) {
        int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

            boolean participantExists = participants.stream()
                    .anyMatch(p -> participantIdentity.equals(p.getIdentity()));

            if (!participantExists) {
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
        boolean participantExists = participants.stream()
                .anyMatch(p -> participantIdentity.equals(p.getIdentity()));

        assertFalse(participantExists, "Participant '" + participantIdentity + "' should not exist in room '" + roomName + "'");
    }

    @Then("participant {string} should be publishing screen share in room {string} using service {string}")
    public void participantShouldBePublishingScreenShareInRoomUsingService(String participantIdentity, String roomName, String serviceName) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

            LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                    .filter(p -> participantIdentity.equals(p.getIdentity()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(targetParticipant, "Participant '" + participantIdentity + "' should exist in room '" + roomName + "'");

            long screenShareTrackCount = targetParticipant.getTracksList().stream()
                    .filter(track -> track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                    .count();

            if (screenShareTrackCount >= 1) {
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
        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        long screenShareTrackCount = targetParticipant != null ?
                targetParticipant.getTracksList().stream()
                        .filter(track -> track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                        .count() : 0;

        log.warn("Screen share publishing check failed for participant '{}' after {} seconds. Screen share tracks: {}, Total tracks: {}",
                participantIdentity, maxAttempts * POLLING_INTERVAL_MS / 1000, screenShareTrackCount,
                targetParticipant != null ? targetParticipant.getTracksList().size() : 0);

        assertEquals(1, screenShareTrackCount, "Participant '" + participantIdentity + "' should have 1 published screen share track");
    }

    @Then("participant {string} should not be publishing screen share in room {string} using service {string}")
    public void participantShouldNotBePublishingScreenShareInRoomUsingService(String participantIdentity, String roomName, String serviceName) {
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

        long screenShareTrackCount = targetParticipant.getTracksList().stream()
                .filter(track -> track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                .count();

        assertEquals(0, screenShareTrackCount, "Participant '" + participantIdentity + "' should have 0 published screen share tracks");
    }

    @Then("participant {string} should have {int} remote screen share tracks available in room {string} using service {string}")
    public void participantShouldHaveRemoteScreenShareTracksAvailableInRoomUsingService(String participantIdentity, int expectedCount, String roomName, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        long remoteScreenShareTrackCount = participants.stream()
                .filter(p -> !participantIdentity.equals(p.getIdentity()))
                .flatMap(p -> p.getTracksList().stream())
                .filter(track -> track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                .count();

        assertEquals(expectedCount, remoteScreenShareTrackCount, "Participant '" + participantIdentity + "' should see " + expectedCount + " remote screen share tracks");
    }

    @Then("participant {string} should have {int} published tracks in room {string} using service {string}")
    public void participantShouldHavePublishedTracksInRoomUsingService(String participantIdentity, int expectedCount, String roomName, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        assertNotNull(targetParticipant, "Participant '" + participantIdentity + "' should exist in room '" + roomName + "'");

        int trackCount = targetParticipant.getTracksList().size();

        assertEquals(expectedCount, trackCount, "Participant '" + participantIdentity + "' should have " + expectedCount + " published tracks");
    }

    private LivekitModels.TrackInfo getVideoTrackForParticipant(String serviceName, String roomName, String participantIdentity) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        if (targetParticipant == null) {
            return null;
        }

        return targetParticipant.getTracksList().stream()
                .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                .filter(track -> track.getSource() == LivekitModels.TrackSource.CAMERA)
                .findFirst()
                .orElse(null);
    }

    private List<LivekitModels.VideoLayer> getVideoLayersForParticipant(String serviceName, String roomName, String participantIdentity) {
        LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(serviceName, roomName, participantIdentity);

        if (videoTrack == null) {
            return Collections.emptyList();
        }

        return videoTrack.getLayersList();
    }

    @Then("participant {string} should have simulcast enabled for video in room {string} using service {string}")
    public void participantShouldHaveSimulcastEnabled(String identity, String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);

            if (videoTrack != null && videoTrack.getSimulcast()) {
                log.info("Simulcast verified as enabled for participant '{}' in room '{}'", identity, room);
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

        LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);
        assertNotNull(videoTrack, "Video track should exist for participant '" + identity + "'");
        assertTrue(videoTrack.getSimulcast(), "Simulcast should be enabled for participant '" + identity + "'");
    }

    @Then("participant {string} should have simulcast disabled for video in room {string} using service {string}")
    public void participantShouldHaveSimulcastDisabled(String identity, String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);

            if (videoTrack != null && !videoTrack.getSimulcast()) {
                log.info("Simulcast verified as disabled for participant '{}' in room '{}'", identity, room);
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

        LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);
        assertNotNull(videoTrack, "Video track should exist for participant '" + identity + "'");
        assertFalse(videoTrack.getSimulcast(), "Simulcast should be disabled for participant '" + identity + "'");
    }

    @Then("participant {string} video track should have at least {int} layers in room {string} using service {string}")
    public void videoTrackShouldHaveAtLeastLayers(String identity, int minLayers, String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);

            if (layers.size() >= minLayers) {
                log.info("Found {} layers for participant '{}' (minimum required: {})", layers.size(), identity, minLayers);
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

        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= minLayers,
            "Participant '" + identity + "' should have at least " + minLayers + " video layers, found: " + layers.size());
    }

    @Then("participant {string} video track should have exactly {int} layer in room {string} using service {string}")
    public void videoTrackShouldHaveExactlyLayers(String identity, int expectedLayers, String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);

            if (layers.size() == expectedLayers) {
                log.info("Found exactly {} layer(s) for participant '{}'", expectedLayers, identity);
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

        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertEquals(expectedLayers, layers.size(),
            "Participant '" + identity + "' should have exactly " + expectedLayers + " video layer(s)");
    }

    @Then("participant {string} video layers should have different resolutions in room {string} using service {string}")
    public void videoLayersShouldHaveDifferentResolutions(String identity, String room, String service) {
        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= 2, "Need at least 2 layers to compare resolutions, found: " + layers.size());

        Set<String> resolutions = layers.stream()
                .map(layer -> layer.getWidth() + "x" + layer.getHeight())
                .collect(Collectors.toSet());

        assertTrue(resolutions.size() > 1,
            "Video layers should have different resolutions, found: " + resolutions);
    }

    @Then("participant {string} video layers should have different bitrates in room {string} using service {string}")
    public void videoLayersShouldHaveDifferentBitrates(String identity, String room, String service) {
        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= 2, "Need at least 2 layers to compare bitrates, found: " + layers.size());

        Set<Integer> bitrates = layers.stream()
                .map(LivekitModels.VideoLayer::getBitrate)
                .collect(Collectors.toSet());

        assertTrue(bitrates.size() > 1,
            "Video layers should have different bitrates, found: " + bitrates);
    }

    @Then("participant {string} highest video layer should have greater resolution than lowest layer in room {string} using service {string}")
    public void highestLayerShouldHaveGreaterResolution(String identity, String room, String service) {
        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= 2, "Need at least 2 layers to compare, found: " + layers.size());

        LivekitModels.VideoLayer highest = layers.stream()
                .max(Comparator.comparingInt(l -> l.getWidth() * l.getHeight()))
                .orElse(null);

        LivekitModels.VideoLayer lowest = layers.stream()
                .min(Comparator.comparingInt(l -> l.getWidth() * l.getHeight()))
                .orElse(null);

        assertNotNull(highest, "Highest layer should exist");
        assertNotNull(lowest, "Lowest layer should exist");

        int highestResolution = highest.getWidth() * highest.getHeight();
        int lowestResolution = lowest.getWidth() * lowest.getHeight();

        assertTrue(highestResolution > lowestResolution,
            "Highest layer (" + highest.getWidth() + "x" + highest.getHeight() +
            ") should have greater resolution than lowest (" + lowest.getWidth() + "x" + lowest.getHeight() + ")");
    }

    @Then("the CLI publisher should have simulcast video layers in room {string} using service {string}")
    public void cliPublisherShouldHaveSimulcastLayers(String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

            boolean hasSimulcast = participants.stream()
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .anyMatch(t -> t.getSimulcast() && t.getLayersCount() > 1);

            if (hasSimulcast) {
                log.info("CLI publisher has simulcast video layers in room '{}'", room);
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

        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);
        boolean hasSimulcast = participants.stream()
                .flatMap(p -> p.getTracksList().stream())
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .anyMatch(t -> t.getSimulcast() && t.getLayersCount() > 1);

        assertTrue(hasSimulcast, "CLI publisher should have simulcast video layers");
    }

    @Then("the CLI publisher should have exactly {int} video layer in room {string} using service {string}")
    public void cliPublisherShouldHaveExactlyLayers(int expectedLayers, String room, String service) {
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

            int layerCount = participants.stream()
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .mapToInt(LivekitModels.TrackInfo::getLayersCount)
                    .max()
                    .orElse(0);

            if (layerCount == expectedLayers) {
                log.info("CLI publisher has exactly {} video layer(s)", expectedLayers);
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

        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);
        int layerCount = participants.stream()
                .flatMap(p -> p.getTracksList().stream())
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .mapToInt(LivekitModels.TrackInfo::getLayersCount)
                .max()
                .orElse(0);

        assertEquals(expectedLayers, layerCount, "CLI publisher should have exactly " + expectedLayers + " video layer(s)");
    }
}
