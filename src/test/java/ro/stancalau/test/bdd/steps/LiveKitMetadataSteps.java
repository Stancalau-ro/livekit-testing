package ro.stancalau.test.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import livekit.LivekitModels;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitMetadataSteps {

    private static final long POLLING_INTERVAL_MS = BrowserPollingHelper.DEFAULT_DELAY_MS;
    private static final int MAX_POLL_ATTEMPTS = BrowserPollingHelper.EXTENDED_MAX_ATTEMPTS;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private <T> T pollUntil(Supplier<T> getter, java.util.function.Predicate<T> condition) {
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            T value = getter.get();
            if (condition.test(value)) {
                return value;
            }
            BrowserPollingHelper.safeSleep(POLLING_INTERVAL_MS);
        }
        return getter.get();
    }

    @When("room metadata for {string} is set to {string} using service {string}")
    public void setRoomMetadata(String roomName, String metadata, String serviceName) {
        ManagerProvider.metadata().setRoomMetadata(serviceName, roomName, metadata);
    }

    @When("room metadata for {string} is set to a string of {int} bytes using service {string}")
    public void setRoomMetadataOfSize(String roomName, int sizeBytes, String serviceName) {
        String metadata = ManagerProvider.metadata().generateStringOfSize(sizeBytes);
        ManagerProvider.metadata().setRoomMetadata(serviceName, roomName, metadata);
    }

    @When("room info for {string} is retrieved using service {string}")
    public void retrieveRoomInfo(String roomName, String serviceName) {
        ManagerProvider.metadata().retrieveRoomInfo(serviceName, roomName);
    }

    @Then("room {string} should have metadata {string} in service {string}")
    public void verifyRoomMetadata(String roomName, String expectedMetadata, String serviceName) {
        boolean success = ManagerProvider.metadata().pollUntilRoomMetadataEquals(serviceName, roomName, expectedMetadata);
        assertTrue(success, "Room " + roomName + " should have metadata: " + expectedMetadata);
    }

    @Then("room {string} should have empty metadata in service {string}")
    public void verifyRoomMetadataEmpty(String roomName, String serviceName) {
        String metadata = pollUntil(
                () -> ManagerProvider.metadata().getRoomMetadata(serviceName, roomName),
                m -> m == null || m.isEmpty());
        assertTrue(metadata == null || metadata.isEmpty(),
                "Room " + roomName + " should have empty metadata but got: " + metadata);
    }

    @Then("room {string} should have metadata of length {int} bytes in service {string}")
    public void verifyRoomMetadataLength(String roomName, int expectedLength, String serviceName) {
        String metadata = ManagerProvider.metadata().getRoomMetadata(serviceName, roomName);
        assertEquals(expectedLength, metadata.length(),
                "Room metadata length should be " + expectedLength);
    }

    @Then("the retrieved room info should have metadata {string}")
    public void verifyRetrievedRoomInfoMetadata(String expectedMetadata) {
        LivekitModels.Room roomInfo = ManagerProvider.metadata().getLastRetrievedRoomInfo();
        assertNotNull(roomInfo, "Room info should have been retrieved");
        assertEquals(expectedMetadata, roomInfo.getMetadata());
    }

    @Then("the retrieved room info should have empty metadata")
    public void verifyRetrievedRoomInfoEmptyMetadata() {
        LivekitModels.Room roomInfo = ManagerProvider.metadata().getLastRetrievedRoomInfo();
        assertNotNull(roomInfo, "Room info should have been retrieved");
        String metadata = roomInfo.getMetadata();
        assertTrue(metadata == null || metadata.isEmpty(),
                "Retrieved room info should have empty metadata");
    }

    @When("participant {string} metadata is updated to {string} in room {string} using service {string}")
    public void updateParticipantMetadata(String identity, String metadata, String roomName, String serviceName) {
        ManagerProvider.metadata().updateParticipantMetadata(serviceName, roomName, identity, metadata);
    }

    @When("participant {string} metadata is cleared in room {string} using service {string}")
    public void clearParticipantMetadata(String identity, String roomName, String serviceName) {
        ManagerProvider.metadata().updateParticipantMetadata(serviceName, roomName, identity, "");
    }

    @Then("participant {string} should have metadata {string} in room {string} using service {string}")
    public void verifyParticipantMetadata(String identity, String expectedMetadata, String roomName, String serviceName) {
        boolean success = ManagerProvider.metadata().pollUntilParticipantMetadataEquals(
                serviceName, roomName, identity, expectedMetadata);
        assertTrue(success, "Participant " + identity + " should have metadata: " + expectedMetadata);
    }

    @Then("participant {string} should have empty metadata in room {string} using service {string}")
    public void verifyParticipantMetadataEmpty(String identity, String roomName, String serviceName) {
        String metadata = pollUntil(
                () -> ManagerProvider.metadata().getParticipantMetadata(serviceName, roomName, identity),
                m -> m == null || m.isEmpty());
        assertTrue(metadata == null || metadata.isEmpty(),
                "Participant " + identity + " should have empty metadata but got: " + metadata);
    }

    @Then("participant {string} should have metadata containing {string} in room {string} using service {string}")
    public void verifyParticipantMetadataContains(String identity, String expectedSubstring, String roomName, String serviceName) {
        String metadata = pollUntil(
                () -> ManagerProvider.metadata().getParticipantMetadata(serviceName, roomName, identity),
                m -> m != null && m.contains(expectedSubstring));
        assertTrue(metadata != null && metadata.contains(expectedSubstring),
                "Participant " + identity + " metadata should contain: " + expectedSubstring);
    }

    @When("{string} starts listening for room metadata events")
    public void startListeningForRoomMetadataEvents(String participantName) {
        ManagerProvider.metadata().startListeningForRoomMetadataEvents(participantName);
    }

    @When("{string} starts listening for participant metadata events")
    public void startListeningForParticipantMetadataEvents(String participantName) {
        ManagerProvider.metadata().startListeningForParticipantMetadataEvents(participantName);
    }

    @Then("{string} should receive a room metadata update event with value {string}")
    public void verifyRoomMetadataEvent(String participantName, String expectedValue) {
        boolean received = ManagerProvider.metadata().waitForRoomMetadataEvent(participantName, expectedValue);
        if (!received) {
            List<Map<String, Object>> events = ManagerProvider.metadata().getRoomMetadataEvents(participantName);
            log.warn("{} did not receive room metadata event with value '{}'. Received events: {}",
                    participantName, expectedValue, events);
        }
        assertTrue(received, participantName + " should receive room metadata event with value: " + expectedValue);
    }

    @Then("{string} should receive a participant metadata update event for {string} with value {string}")
    public void verifyParticipantMetadataEvent(String participantName, String targetIdentity, String expectedValue) {
        boolean received = ManagerProvider.metadata().waitForParticipantMetadataEvent(
                participantName, targetIdentity, expectedValue);
        assertTrue(received, participantName + " should receive participant metadata event for " +
                targetIdentity + " with value: " + expectedValue);
    }

    @Then("{string} should have received {int} room metadata update events")
    public void verifyRoomMetadataEventCount(String participantName, int expectedCount) {
        BrowserPollingHelper.safeSleep(1000);
        int actualCount = ManagerProvider.metadata().getRoomMetadataEventCount(participantName);
        assertTrue(actualCount >= expectedCount,
                participantName + " should have received at least " + expectedCount +
                        " room metadata events but got: " + actualCount);
    }

    @Then("{string} should have received {int} participant metadata update events")
    public void verifyParticipantMetadataEventCount(String participantName, int expectedCount) {
        BrowserPollingHelper.safeSleep(1000);
        int actualCount = ManagerProvider.metadata().getParticipantMetadataEventCount(participantName);
        assertTrue(actualCount >= expectedCount,
                participantName + " should have received at least " + expectedCount +
                        " participant metadata events but got: " + actualCount);
    }

    @Then("{string} should see room metadata {string}")
    public void verifyBrowserRoomMetadata(String participantName, String expectedMetadata) {
        String actual = pollUntil(
                () -> ManagerProvider.metadata().getCurrentRoomMetadataFromBrowser(participantName),
                expectedMetadata::equals);
        assertEquals(expectedMetadata, actual,
                participantName + " should see room metadata: " + expectedMetadata);
    }

    @Then("{string} should see participant {string} with metadata {string}")
    public void verifyBrowserParticipantMetadata(String observerName, String targetIdentity, String expectedMetadata) {
        String actual = pollUntil(
                () -> ManagerProvider.metadata().getParticipantMetadataFromBrowser(observerName, targetIdentity),
                expectedMetadata::equals);
        assertEquals(expectedMetadata, actual,
                observerName + " should see participant " + targetIdentity + " with metadata: " + expectedMetadata);
    }

    @Then("{string} should see participant {string} with metadata containing {string}")
    public void verifyBrowserParticipantMetadataContains(String observerName, String targetIdentity, String expectedSubstring) {
        String actual = pollUntil(
                () -> ManagerProvider.metadata().getParticipantMetadataFromBrowser(observerName, targetIdentity),
                m -> m != null && m.contains(expectedSubstring));
        assertTrue(actual != null && actual.contains(expectedSubstring),
                observerName + " should see participant " + targetIdentity + " with metadata containing: " + expectedSubstring);
    }

    @Then("participant {string} metadata should be valid JSON in room {string} using service {string}")
    public void verifyParticipantMetadataIsValidJson(String identity, String roomName, String serviceName) {
        String metadata = ManagerProvider.metadata().getParticipantMetadata(serviceName, roomName, identity);
        assertNotNull(metadata, "Metadata should not be null");
        assertFalse(metadata.isEmpty(), "Metadata should not be empty");
        try {
            objectMapper.readTree(metadata);
        } catch (Exception e) {
            fail("Metadata is not valid JSON: " + e.getMessage() + ". Value: " + metadata);
        }
    }

    @Then("participant {string} should have metadata of approximately {int} bytes in room {string} using service {string}")
    public void verifyParticipantMetadataApproximateSize(String identity, int expectedBytes, String roomName, String serviceName) {
        String metadata = ManagerProvider.metadata().getParticipantMetadata(serviceName, roomName, identity);
        assertNotNull(metadata, "Metadata should not be null");
        int tolerance = expectedBytes / 10;
        assertTrue(Math.abs(metadata.length() - expectedBytes) <= tolerance,
                "Metadata length should be approximately " + expectedBytes +
                        " bytes (got " + metadata.length() + ")");
    }
}
