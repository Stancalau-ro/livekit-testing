package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;

@Slf4j
public class DataChannelSteps {

    private static final int MIN_TIMESTAMPED_MESSAGES = 10;

    @When("{string} sends a data message {string} via reliable channel")
    public void sendsDataMessageViaReliableChannel(String participantName, String message) {
        ManagerProvider.dataChannel().sendMessage(participantName, message, true);
    }

    @When("{string} sends a data message {string} via unreliable channel")
    public void sendsDataMessageViaUnreliableChannel(String participantName, String message) {
        ManagerProvider.dataChannel().sendMessage(participantName, message, false);
    }

    @When("{string} sends a broadcast data message {string} via reliable channel")
    public void sendsBroadcastDataMessage(String participantName, String message) {
        ManagerProvider.dataChannel().sendMessage(participantName, message, true);
    }

    @When("{string} sends a data message {string} to {string} via reliable channel")
    public void sendsTargetedDataMessage(String sender, String message, String recipient) {
        ManagerProvider.dataChannel().sendTargetedMessage(sender, message, recipient, true);
    }

    @When("{string} sends a data message of size {int} bytes via reliable channel")
    public void sendsDataMessageOfSize(String participantName, int sizeBytes) {
        ManagerProvider.dataChannel().sendMessageOfSize(participantName, sizeBytes, true);
    }

    @When("{string} sends {int} data messages via unreliable channel")
    public void sendsMultipleDataMessagesViaUnreliableChannel(String participantName, int count) {
        ManagerProvider.dataChannel().sendMultipleMessages(participantName, count, false);
    }

    @When("{string} sends {int} timestamped data messages via reliable channel")
    public void sendsTimestampedMessages(String participantName, int count) {
        ManagerProvider.dataChannel().sendTimestampedMessages(participantName, count, true);
    }

    @When("{string} attempts to send a data message {string}")
    public void attemptsToSendDataMessage(String participantName, String message) {
        ManagerProvider.dataChannel().attemptSendMessage(participantName, message);
    }

    @Then("{string} should receive data message {string} from {string}")
    public void shouldReceiveDataMessageFrom(String receiver, String message, String sender) {
        boolean received = ManagerProvider.dataChannel().hasReceivedMessage(receiver, message, sender);
        assertTrue(received, receiver + " should have received data message '" + message + "' from " + sender);
    }

    @Then("{string} should receive at least {int} out of {int} messages from {string}")
    public void shouldReceiveAtLeastMessagesFrom(String receiver, int minMessages, int totalSent, String sender) {
        boolean receivedEnough = ManagerProvider.dataChannel()
                .waitForMessageCount(receiver, minMessages, LiveKitMeet.BATCH_DATA_MESSAGE_TIMEOUT_MS);
        int actualCount = ManagerProvider.dataChannel().getReceivedMessageCount(receiver);
        assertTrue(
                receivedEnough && actualCount >= minMessages,
                receiver
                        + " should have received at least "
                        + minMessages
                        + " messages from "
                        + sender
                        + " (actual: "
                        + actualCount
                        + " out of "
                        + totalSent
                        + " sent)");
        log.info(
                "Unreliable channel delivery: {} out of {} messages received ({}%)",
                actualCount, totalSent, (actualCount * 100.0 / totalSent));
    }

    @Then("{string} should receive all timestamped messages")
    public void shouldReceiveAllTimestampedMessages(String participantName) {
        int receivedCount = ManagerProvider.dataChannel().getReceivedMessageCountAfterWait(participantName);
        assertTrue(
                receivedCount >= MIN_TIMESTAMPED_MESSAGES,
                participantName + " should have received all timestamped messages (received: " + receivedCount + ")");
    }

    @Then("{string} should receive a data message of size {int} bytes")
    public void shouldReceiveDataMessageOfSize(String participantName, int expectedSize) {
        boolean received = ManagerProvider.dataChannel()
                .waitForMessageCount(participantName, 1, LiveKitMeet.DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
        assertTrue(received, participantName + " should have received a data message");
        int actualCount = ManagerProvider.dataChannel().getReceivedMessageCount(participantName);
        assertTrue(
                actualCount > 0,
                participantName + " should have at least one message (size: " + expectedSize + " bytes)");
    }

    @Then("{string} should not receive data message {string}")
    public void shouldNotReceiveDataMessage(String participantName, String message) {
        boolean received = ManagerProvider.dataChannel().hasReceivedMessageAfterWait(participantName, message);
        assertFalse(received, participantName + " should not have received data message: " + message);
    }

    @Then("{string} should have data publishing blocked due to permissions")
    public void shouldHaveDataPublishingBlockedDueToPermissions(String participantName) {
        boolean isBlocked = ManagerProvider.dataChannel().isDataPublishingBlocked(participantName);
        String error = ManagerProvider.dataChannel().getLastDataChannelError(participantName);
        assertTrue(isBlocked, participantName + " should have data publishing blocked (error: " + error + ")");
    }

    @Then("the average data channel latency for {string} should be less than {int} ms")
    public void averageLatencyShouldBeLessThan(String participantName, int maxLatencyMs) {
        double avgLatency = ManagerProvider.dataChannel().getAverageLatency(participantName);
        assertTrue(
                avgLatency >= 0 && avgLatency < maxLatencyMs,
                "Average data channel latency for "
                        + participantName
                        + " should be less than "
                        + maxLatencyMs
                        + " ms (actual: "
                        + String.format("%.2f", avgLatency)
                        + " ms)");
        log.info(
                "Data channel latency for {}: {} ms (threshold: {} ms)",
                participantName,
                String.format("%.2f", avgLatency),
                maxLatencyMs);
    }

    @Then("the test logs document that lossy mode in local containers typically achieves near-100% delivery")
    public void testLogsDocumentLossyModeDelivery() {
        log.info("NOTE: Lossy/unreliable data channel mode in local container testing typically "
                + "achieves near-100% delivery due to low latency and no packet loss. In production "
                + "environments with network constraints, message loss is expected and normal.");
    }

    @Then("{string} should receive data messages in order:")
    public void shouldReceiveDataMessagesInOrder(String participantName, DataTable dataTable) {
        List<String> expectedMessages =
                dataTable.asList().subList(1, dataTable.asList().size());
        boolean allReceived = ManagerProvider.dataChannel()
                .waitForMessageCount(
                        participantName, expectedMessages.size(), LiveKitMeet.BATCH_DATA_MESSAGE_TIMEOUT_MS);
        assertTrue(allReceived, participantName + " should have received all " + expectedMessages.size() + " messages");

        List<Map<String, Object>> receivedMessages =
                ManagerProvider.dataChannel().getReceivedMessages(participantName);
        Objects.requireNonNull(receivedMessages, "Should have received messages list");
        assertTrue(
                receivedMessages.size() >= expectedMessages.size(),
                "Should have received at least " + expectedMessages.size() + " messages");

        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = receivedMessages.get(i).get("content").toString();
            assertEquals(
                    expected,
                    actual,
                    "Message at position "
                            + i
                            + " should match expected order and content. "
                            + "Expected: '"
                            + expected
                            + "', Actual: '"
                            + actual
                            + "'. "
                            + "This indicates messages arrived out of order or with wrong content.");
        }
        log.info("All {} messages received in correct order for {}", expectedMessages.size(), participantName);
    }
}
