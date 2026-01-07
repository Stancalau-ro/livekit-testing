package ro.stancalau.test.framework.state;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

@Slf4j
public class DataChannelStateManager {

    private static final long MESSAGE_SEND_INTERVAL_MS = 50;
    private static final long TIMESTAMPED_MESSAGE_INTERVAL_MS = 100;
    private static final long WAIT_FOR_MESSAGES_MS = 2000;
    private static final long WAIT_FOR_BLOCK_MS = 1000;
    private static final long MESSAGE_RECEIVE_TIMEOUT_MS = 1000;

    private final MeetSessionStateManager meetSessionStateManager;

    public DataChannelStateManager(MeetSessionStateManager meetSessionStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
    }

    public void sendMessage(String participantName, String message, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        boolean success = meetInstance.getDataChannel().sendDataMessage(message, reliable);
        if (!success) {
            String error = meetInstance.getDataChannel().getLastDataChannelError();
            throw new RuntimeException("Failed to send data message: " + error);
        }
    }

    public void sendTargetedMessage(String sender, String message, String recipient, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(sender);
        boolean success = meetInstance.getDataChannel().sendDataMessageTo(message, recipient, reliable);
        if (!success) {
            String error = meetInstance.getDataChannel().getLastDataChannelError();
            throw new RuntimeException("Failed to send targeted data message: " + error);
        }
    }

    public void sendMessageOfSize(String participantName, int sizeBytes, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        boolean success = meetInstance.getDataChannel().sendDataMessageOfSize(sizeBytes, reliable);
        if (!success) {
            String error = meetInstance.getDataChannel().getLastDataChannelError();
            throw new RuntimeException("Failed to send data message of size " + sizeBytes + ": " + error);
        }
    }

    public void sendMultipleMessages(String participantName, int count, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        for (int i = 0; i < count; i++) {
            String label = reliable ? "Reliable" : "Unreliable";
            String message = label + " message " + (i + 1);
            boolean success = meetInstance.getDataChannel().sendDataMessage(message, reliable);
            if (!success) {
                String error = meetInstance.getDataChannel().getLastDataChannelError();
                throw new RuntimeException("Failed to send data message: " + error);
            }
            BrowserPollingHelper.safeSleep(MESSAGE_SEND_INTERVAL_MS);
        }
    }

    public void sendTimestampedMessages(String participantName, int count, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        for (int i = 0; i < count; i++) {
            String message = "Latency test message " + (i + 1);
            boolean success = meetInstance.getDataChannel().sendTimestampedDataMessage(message, reliable);
            if (!success) {
                String error = meetInstance.getDataChannel().getLastDataChannelError();
                throw new RuntimeException("Failed to send timestamped data message: " + error);
            }
            BrowserPollingHelper.safeSleep(TIMESTAMPED_MESSAGE_INTERVAL_MS);
        }
    }

    public void attemptSendMessage(String participantName, String message) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        boolean success = meetInstance.getDataChannel().sendDataMessage(message, true);
        if (!success) {
            String error = meetInstance.getDataChannel().getLastDataChannelError();
            log.info("Data message send attempt failed as expected: {}", error);
        }
    }

    public boolean hasReceivedMessage(String receiver, String message, String sender) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(receiver);
        return meetInstance.hasReceivedDataMessage(message, sender, LiveKitMeet.DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
    }

    public boolean waitForMessageCount(String participantName, int minCount, long timeoutMs) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.waitForDataMessageCount(minCount, timeoutMs);
    }

    public int getReceivedMessageCount(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getDataChannel().getReceivedDataMessageCount();
    }

    public boolean isDataPublishingBlocked(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_BLOCK_MS);
        return meetInstance.getDataChannel().isDataPublishingBlocked();
    }

    public String getLastDataChannelError(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getDataChannel().getLastDataChannelError();
    }

    public double getAverageLatency(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        Map<String, Object> stats = meetInstance.getDataChannel().getDataChannelLatencyStats();
        Object avgObj = stats.get("average");
        if (avgObj instanceof Number) {
            return ((Number) avgObj).doubleValue();
        }
        return 0.0;
    }

    public boolean hasReceivedMessageAfterWait(String participantName, String message) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        return meetInstance.hasReceivedDataMessage(message, null, MESSAGE_RECEIVE_TIMEOUT_MS);
    }

    public int getReceivedMessageCountAfterWait(String participantName) {
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        return getReceivedMessageCount(participantName);
    }

    public List<Map<String, Object>> getReceivedMessages(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getDataChannel().getReceivedDataMessages();
    }

    public void clearAll() {
        log.info("Clearing data channel state");
        meetSessionStateManager.getAllMeetInstances().values().forEach(meet -> {
            try {
                meet.getDataChannel().clearDataChannelState();
            } catch (Exception e) {
                log.warn("Error clearing data channel state: {}", e.getMessage());
            }
        });
    }
}
