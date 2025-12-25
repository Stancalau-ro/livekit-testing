package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import java.util.List;
import java.util.Map;

@Slf4j
public class DataChannelStateManager {

    private static final long MESSAGE_SEND_INTERVAL_MS = 50;
    private static final long TIMESTAMPED_MESSAGE_INTERVAL_MS = 100;
    private static final long WAIT_FOR_MESSAGES_MS = 2000;
    private static final long WAIT_FOR_BLOCK_MS = 1000;

    private final MeetSessionStateManager meetSessionStateManager;

    public DataChannelStateManager(MeetSessionStateManager meetSessionStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
    }

    public void sendMessage(String participantName, String message, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.sendDataMessage(message, reliable);
    }

    public void sendBroadcastMessage(String participantName, String message, boolean reliable) {
        sendMessage(participantName, message, reliable);
    }

    public void sendTargetedMessage(String sender, String message, String recipient, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(sender);
        meetInstance.sendDataMessageTo(message, recipient, reliable);
    }

    public void sendMessageOfSize(String participantName, int sizeBytes, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.sendDataMessageOfSize(sizeBytes, reliable);
    }

    public void sendMultipleMessages(String participantName, int count, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        for (int i = 0; i < count; i++) {
            String label = reliable ? "Reliable" : "Unreliable";
            meetInstance.sendDataMessage(label + " message " + (i + 1), reliable);
            BrowserPollingHelper.safeSleep(MESSAGE_SEND_INTERVAL_MS);
        }
    }

    public void sendTimestampedMessages(String participantName, int count, boolean reliable) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        for (int i = 0; i < count; i++) {
            meetInstance.sendTimestampedDataMessage("Latency test message " + (i + 1), reliable);
            BrowserPollingHelper.safeSleep(TIMESTAMPED_MESSAGE_INTERVAL_MS);
        }
    }

    public void attemptSendMessage(String participantName, String message) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        try {
            meetInstance.sendDataMessage(message, true);
        } catch (Exception e) {
            log.info("Data message send attempt failed as expected: {}", e.getMessage());
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
        return meetInstance.getReceivedDataMessageCount();
    }

    public boolean isDataPublishingBlocked(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_BLOCK_MS);
        return meetInstance.isDataPublishingBlocked();
    }

    public String getLastDataChannelError(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getLastDataChannelError();
    }

    public double getAverageLatency(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        return meetInstance.getAverageDataChannelLatency();
    }

    public boolean hasReceivedMessageAfterWait(String participantName, String message) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        return meetInstance.hasReceivedDataMessage(message, null, 1000);
    }

    public int getReceivedMessageCountAfterWait(String participantName) {
        BrowserPollingHelper.safeSleep(WAIT_FOR_MESSAGES_MS);
        return getReceivedMessageCount(participantName);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getReceivedMessages(String participantName) {
        WebDriver driver = meetSessionStateManager.getWebDriver(participantName);
        return (List<Map<String, Object>>) ((JavascriptExecutor) driver)
            .executeScript("return window.LiveKitTestHelpers.getReceivedDataMessages();");
    }

    public void clearAll() {
        log.info("Clearing data channel state");
    }
}
