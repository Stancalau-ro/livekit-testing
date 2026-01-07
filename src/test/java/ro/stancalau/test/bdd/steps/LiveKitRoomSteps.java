package ro.stancalau.test.bdd.steps;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.RoomServiceClient;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.steps.params.EnabledState;
import ro.stancalau.test.bdd.steps.params.MuteState;
import ro.stancalau.test.framework.util.BrowserPollingHelper;
import ro.stancalau.test.framework.util.StringParsingUtils;

@Slf4j
public class LiveKitRoomSteps {

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

    @When("the system fetches all rooms from service {string}")
    public void allRoomsAreFetchedFromService(String serviceName) {
        lastFetchedRooms = getRooms(serviceName);
    }

    @Then("the room count should be {int}")
    public void theRoomCountShouldBe(int expectedCount) {
        assertNotNull(lastFetchedRooms, "Room list should have been fetched");
        assertEquals(expectedCount, lastFetchedRooms.size(), "Room count does not match expected value");
    }

    @When("the system creates room {string} using service {string}")
    @Given("room {string} is created using service {string}")
    public void aRoomIsCreatedUsingService(String roomName, String serviceName) {
        LivekitModels.Room createdRoom = createRoom(serviceName, roomName);
        assertNotNull(createdRoom, "Room should have been created");
    }

    public List<LivekitModels.ParticipantInfo> getParticipantInfo(String serviceName, String roomName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            List<LivekitModels.ParticipantInfo> result =
                    client.listParticipants(roomName).execute().body();
            return isNull(result) ? Collections.emptyList() : result;
        } catch (IOException e) {
            log.error(
                    "Failed to fetch participant info for room '{}' using service '{}': {}",
                    roomName,
                    serviceName,
                    e.getMessage(),
                    e);
            throw new RuntimeException("Failed to fetch participant info for room: " + roomName, e);
        }
    }

    private LivekitModels.ParticipantInfo findParticipant(String serviceName, String roomName, String identity) {
        return getParticipantInfo(serviceName, roomName).stream()
                .filter(p -> identity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);
    }

    private LivekitModels.ParticipantInfo findParticipantOrFail(String serviceName, String roomName, String identity) {
        LivekitModels.ParticipantInfo participant = findParticipant(serviceName, roomName, identity);
        assertNotNull(participant, "Participant '" + identity + "' should exist in room '" + roomName + "'");
        return participant;
    }

    @Then("room {string} should exist in service {string}")
    public void theRoomShouldExistInService(String roomName, String serviceName) {
        List<LivekitModels.Room> rooms = getRooms(serviceName);
        boolean roomExists = rooms.stream().anyMatch(room -> roomName.equals(room.getName()));
        assertTrue(roomExists, "Room '" + roomName + "' should exist in service '" + serviceName + "'");
    }

    @Then("room {string} should have {int} active participants in service {string}")
    public void theRoomShouldHaveActiveParticipantsInService(String roomName, int expectedCount, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(
                () -> getParticipantInfo(serviceName, roomName).size() == expectedCount);
        if (!success) {
            int actualCount = getParticipantInfo(serviceName, roomName).size();
            log.warn("Participant count check failed. Expected: {}, Found: {}", expectedCount, actualCount);
            assertEquals(
                    expectedCount,
                    actualCount,
                    "Room '" + roomName + "' should have " + expectedCount + " active participants");
        }
    }

    @Then("participant {string} should be publishing video in room {string} using service {string}")
    public void participantShouldBePublishingVideoInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
            return p.getTracksList().stream().anyMatch(t -> t.getType() == LivekitModels.TrackType.VIDEO);
        });
        if (!success) {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            long videoTrackCount = p != null
                    ? p.getTracksList().stream()
                            .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                            .count()
                    : 0;
            log.warn(
                    "Video publishing check failed for participant '{}'. Video tracks: {}",
                    participantIdentity,
                    videoTrackCount);
            assertEquals(
                    1,
                    videoTrackCount,
                    "Participant '" + participantIdentity + "' should have 1 published video track");
        }
    }

    @Then("participant {string} should not be publishing video in room {string} using service {string}")
    public void participantShouldNotBePublishingVideoInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        BrowserPollingHelper.safeSleep(2000);
        LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
        long videoTrackCount = p.getTracksList().stream()
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .count();
        assertEquals(
                0, videoTrackCount, "Participant '" + participantIdentity + "' should have 0 published video tracks");
    }

    @Then(
            "participant {string} should have {int} remote video tracks available in room {string} using service {string}")
    public void participantShouldHaveRemoteVideoTracksAvailableInRoomUsingService(
            String participantIdentity, int expectedCount, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);
            long remoteVideoTrackCount = participants.stream()
                    .filter(p -> !participantIdentity.equals(p.getIdentity()))
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                    .count();
            return remoteVideoTrackCount == expectedCount;
        });

        if (!success) {
            List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);
            long remoteVideoTrackCount = participants.stream()
                    .filter(p -> !participantIdentity.equals(p.getIdentity()))
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(track -> track.getType() == LivekitModels.TrackType.VIDEO)
                    .count();
            assertEquals(
                    expectedCount,
                    remoteVideoTrackCount,
                    "Participant '" + participantIdentity + "' should see " + expectedCount + " remote video tracks");
        }
    }

    @When("the system deletes room {string} using service {string}")
    public void roomIsDeletedUsingService(String roomName, String serviceName) {

        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            client.deleteRoom(roomName).execute();
        } catch (IOException e) {
            log.error("Failed to delete room '{}' using service '{}': {}", roomName, serviceName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete room: " + roomName, e);
        }
    }

    @When("the system removes participant {string} from room {string} using service {string}")
    public void participantIsRemovedFromRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        RoomServiceClient client = getRoomServiceClient(serviceName);

        try {
            client.removeParticipant(roomName, participantIdentity).execute();
            log.info(
                    "Successfully removed participant '{}' from room '{}' using service '{}'",
                    participantIdentity,
                    roomName,
                    serviceName);
        } catch (IOException e) {
            log.error(
                    "Failed to remove participant '{}' from room '{}' using service '{}': {}",
                    participantIdentity,
                    roomName,
                    serviceName,
                    e.getMessage(),
                    e);
            throw new RuntimeException("Failed to remove participant: " + participantIdentity, e);
        }
    }

    @Then("participant {string} should not exist in room {string} using service {string}")
    public void participantShouldNotExistInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(
                () -> findParticipant(serviceName, roomName, participantIdentity) == null);
        if (!success) {
            assertFalse(
                    findParticipant(serviceName, roomName, participantIdentity) != null,
                    "Participant '" + participantIdentity + "' should not exist in room '" + roomName + "'");
        }
    }

    @Then("participant {string} should appear in room {string} using service {string}")
    public void participantShouldAppearInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(
                () -> findParticipant(serviceName, roomName, participantIdentity) != null);
        assertTrue(success, "Participant '" + participantIdentity + "' should appear in room '" + roomName + "'");
    }

    @Then("participant {string} should be publishing screen share in room {string} using service {string}")
    public void participantShouldBePublishingScreenShareInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
            return p.getTracksList().stream().anyMatch(t -> t.getSource() == LivekitModels.TrackSource.SCREEN_SHARE);
        });
        if (!success) {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            long screenShareCount = p != null
                    ? p.getTracksList().stream()
                            .filter(t -> t.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                            .count()
                    : 0;
            log.warn(
                    "Screen share publishing check failed for participant '{}'. Screen share tracks: {}",
                    participantIdentity,
                    screenShareCount);
            assertEquals(
                    1,
                    screenShareCount,
                    "Participant '" + participantIdentity + "' should have 1 published screen share track");
        }
    }

    @Then("participant {string} should not be publishing screen share in room {string} using service {string}")
    public void participantShouldNotBePublishingScreenShareInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        BrowserPollingHelper.safeSleep(2000);
        LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
        long screenShareCount = p.getTracksList().stream()
                .filter(t -> t.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                .count();
        assertEquals(
                0,
                screenShareCount,
                "Participant '" + participantIdentity + "' should have 0 published screen share tracks");
    }

    @Then(
            "participant {string} should have {int} remote screen share tracks available in room {string} using service {string}")
    public void participantShouldHaveRemoteScreenShareTracksAvailableInRoomUsingService(
            String participantIdentity, int expectedCount, String roomName, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        long remoteScreenShareTrackCount = participants.stream()
                .filter(p -> !participantIdentity.equals(p.getIdentity()))
                .flatMap(p -> p.getTracksList().stream())
                .filter(track -> track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
                .count();

        assertEquals(
                expectedCount,
                remoteScreenShareTrackCount,
                "Participant '"
                        + participantIdentity
                        + "' should see "
                        + expectedCount
                        + " remote screen share tracks");
    }

    @Then("participant {string} should have {int} published tracks in room {string} using service {string}")
    public void participantShouldHavePublishedTracksInRoomUsingService(
            String participantIdentity, int expectedCount, String roomName, String serviceName) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        LivekitModels.ParticipantInfo targetParticipant = participants.stream()
                .filter(p -> participantIdentity.equals(p.getIdentity()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                targetParticipant, "Participant '" + participantIdentity + "' should exist in room '" + roomName + "'");

        int trackCount = targetParticipant.getTracksList().size();

        assertEquals(
                expectedCount,
                trackCount,
                "Participant '" + participantIdentity + "' should have " + expectedCount + " published tracks");
    }

    private LivekitModels.TrackInfo getVideoTrackForParticipant(
            String serviceName, String roomName, String participantIdentity) {
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

    private List<LivekitModels.VideoLayer> getVideoLayersForParticipant(
            String serviceName, String roomName, String participantIdentity) {
        LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(serviceName, roomName, participantIdentity);

        if (videoTrack == null) {
            return Collections.emptyList();
        }

        return videoTrack.getLayersList();
    }

    @Then("participant {string} should have simulcast {enabledState} for video in room {string} using service {string}")
    public void participantShouldHaveSimulcastState(String identity, EnabledState state, String room, String service) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);
            return videoTrack != null && videoTrack.getSimulcast() == state.isEnabled();
        });
        if (success) {
            log.info("Simulcast verified as {} for participant '{}' in room '{}'", state, identity, room);
        } else {
            LivekitModels.TrackInfo videoTrack = getVideoTrackForParticipant(service, room, identity);
            assertNotNull(videoTrack, "Video track should exist for participant '" + identity + "'");
            assertEquals(
                    state.isEnabled(),
                    videoTrack.getSimulcast(),
                    "Simulcast should be " + state + " for participant '" + identity + "'");
        }
    }

    @Then("participant {string} video track should have {string} layers in room {string} using service {string}")
    public void videoTrackShouldHaveLayers(String identity, String layerExpression, String room, String service) {
        StringParsingUtils.ComparisonExpression comparison =
                StringParsingUtils.parseComparisonExpression(layerExpression);
        boolean success = BrowserPollingHelper.pollForCondition(() -> comparison.evaluate(
                getVideoLayersForParticipant(service, room, identity).size()));
        if (success) {
            log.info("Video layer check passed for participant '{}' (expression: {})", identity, layerExpression);
        } else {
            int layerCount =
                    getVideoLayersForParticipant(service, room, identity).size();
            assertTrue(
                    comparison.evaluate(layerCount),
                    comparison.formatMessage("Participant '" + identity + "' video layers", layerCount));
        }
    }

    @Then("participant {string} video layers should have different resolutions in room {string} using service {string}")
    public void videoLayersShouldHaveDifferentResolutions(String identity, String room, String service) {
        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= 2, "Need at least 2 layers to compare resolutions, found: " + layers.size());

        Set<String> resolutions = layers.stream()
                .map(layer -> layer.getWidth() + "x" + layer.getHeight())
                .collect(Collectors.toSet());

        assertTrue(resolutions.size() > 1, "Video layers should have different resolutions, found: " + resolutions);
    }

    @Then("participant {string} video layers should have different bitrates in room {string} using service {string}")
    public void videoLayersShouldHaveDifferentBitrates(String identity, String room, String service) {
        List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(service, room, identity);
        assertTrue(layers.size() >= 2, "Need at least 2 layers to compare bitrates, found: " + layers.size());

        Set<Integer> bitrates =
                layers.stream().map(LivekitModels.VideoLayer::getBitrate).collect(Collectors.toSet());

        assertTrue(bitrates.size() > 1, "Video layers should have different bitrates, found: " + bitrates);
    }

    @Then(
            "participant {string} highest video layer should have greater resolution than lowest layer in room {string} using service {string}")
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

        assertTrue(
                highestResolution > lowestResolution,
                "Highest layer ("
                        + highest.getWidth()
                        + "x"
                        + highest.getHeight()
                        + ") should have greater resolution than lowest ("
                        + lowest.getWidth()
                        + "x"
                        + lowest.getHeight()
                        + ")");
    }

    @Then("the CLI publisher should have simulcast video layers in room {string} using service {string}")
    public void cliPublisherShouldHaveSimulcastLayers(String room, String service) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> getParticipantInfo(service, room).stream()
                .flatMap(p -> p.getTracksList().stream())
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .anyMatch(t -> t.getSimulcast() && t.getLayersCount() > 1));
        if (success) {
            log.info("CLI publisher has simulcast video layers in room '{}'", room);
        } else {
            assertTrue(false, "CLI publisher should have simulcast video layers");
        }
    }

    @Then("the CLI publisher should have exactly {int} video layer in room {string} using service {string}")
    public void cliPublisherShouldHaveExactlyLayers(int expectedLayers, String room, String service) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            int layerCount = getParticipantInfo(service, room).stream()
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .mapToInt(LivekitModels.TrackInfo::getLayersCount)
                    .max()
                    .orElse(0);
            return layerCount == expectedLayers;
        });
        if (success) {
            log.info("CLI publisher has exactly {} video layer(s)", expectedLayers);
        } else {
            int layerCount = getParticipantInfo(service, room).stream()
                    .flatMap(p -> p.getTracksList().stream())
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .mapToInt(LivekitModels.TrackInfo::getLayersCount)
                    .max()
                    .orElse(0);
            assertEquals(
                    expectedLayers,
                    layerCount,
                    "CLI publisher should have exactly " + expectedLayers + " video layer(s)");
        }
    }

    @Then("participant {string} should have audio track {muteState} in room {string} using service {string}")
    public void participantShouldHaveAudioTrackStateInRoomUsingService(
            String participantIdentity, MuteState state, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
            return p.getTracksList().stream()
                    .filter(t -> t.getType() == LivekitModels.TrackType.AUDIO)
                    .anyMatch(t -> t.getMuted() == state.isMuted());
        });
        if (success) {
            log.info("Participant '{}' has audio track {} in room '{}'", participantIdentity, state, roomName);
        } else {
            assertTrue(false, "Participant '" + participantIdentity + "' should have audio track " + state);
        }
    }

    @Then("participant {string} should have video track {muteState} in room {string} using service {string}")
    public void participantShouldHaveVideoTrackStateInRoomUsingService(
            String participantIdentity, MuteState state, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
            return p.getTracksList().stream()
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .filter(t -> t.getSource() == LivekitModels.TrackSource.CAMERA)
                    .anyMatch(t -> t.getMuted() == state.isMuted());
        });
        if (success) {
            log.info("Participant '{}' has video track {} in room '{}'", participantIdentity, state, roomName);
        } else {
            assertTrue(false, "Participant '" + participantIdentity + "' should have video track " + state);
        }
    }

    @Then("participant {string} should have published audio track in room {string} using service {string}")
    public void participantShouldHavePublishedAudioTrackInRoomUsingService(
            String participantIdentity, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            if (p == null) return false;
            return p.getTracksList().stream().anyMatch(t -> t.getType() == LivekitModels.TrackType.AUDIO);
        });
        if (!success) {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            long audioTrackCount = p != null
                    ? p.getTracksList().stream()
                            .filter(t -> t.getType() == LivekitModels.TrackType.AUDIO)
                            .count()
                    : 0;
            log.warn(
                    "Audio publishing check failed for participant '{}'. Audio tracks: {}",
                    participantIdentity,
                    audioTrackCount);
            assertTrue(
                    audioTrackCount > 0,
                    "Participant '" + participantIdentity + "' should have at least 1 published audio track");
        }
    }

    @Then("participant {string} video track should have source {string} in room {string} using service {string}")
    public void participantVideoTrackShouldHaveSourceInRoomUsingService(
            String participantIdentity, String expectedSource, String roomName, String serviceName) {
        LivekitModels.TrackSource source = LivekitModels.TrackSource.valueOf(expectedSource);

        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            if (p == null) return false;
            return p.getTracksList().stream()
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .anyMatch(t -> t.getSource() == source);
        });
        assertTrue(
                success,
                "Participant '" + participantIdentity + "' should have video track with source " + expectedSource);
    }

    @Then("participant {string} video track should have maximum width {int} in room {string} using service {string}")
    public void participantVideoTrackShouldHaveMaximumWidthInRoomUsingService(
            String participantIdentity, int expectedMaxWidth, String roomName, String serviceName) {
        boolean success = BrowserPollingHelper.pollForCondition(() -> {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            if (p == null) return false;
            return p.getTracksList().stream()
                    .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                    .flatMap(t -> t.getLayersList().stream())
                    .anyMatch(layer -> layer.getWidth() <= expectedMaxWidth && layer.getWidth() > 0);
        });

        if (!success) {
            LivekitModels.ParticipantInfo p = findParticipant(serviceName, roomName, participantIdentity);
            if (p != null) {
                int actualMaxWidth = p.getTracksList().stream()
                        .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                        .flatMap(t -> t.getLayersList().stream())
                        .mapToInt(LivekitModels.VideoLayer::getWidth)
                        .max()
                        .orElse(0);
                log.warn(
                        "Video width check for '{}': expected max {}, found max {}",
                        participantIdentity,
                        expectedMaxWidth,
                        actualMaxWidth);
            }
            assertTrue(
                    false,
                    "Participant '" + participantIdentity + "' video track should have width <= " + expectedMaxWidth);
        }
    }
}
