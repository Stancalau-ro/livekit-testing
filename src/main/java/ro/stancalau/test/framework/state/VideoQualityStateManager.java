package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class VideoQualityStateManager {

    private static final int QUALITY_CHANGE_DELAY_MS = 2000;
    private static final int BANDWIDTH_CHANGE_DELAY_MS = 3000;
    private static final int VIDEO_WIDTH_MAX_ATTEMPTS = 10;
    private static final int LOWER_LAYER_MAX_ATTEMPTS = 15;
    private static final long POLL_DELAY_MS = 500;
    private static final int BITRATE_MEASUREMENT_MS = 3000;

    private final MeetSessionStateManager meetSessionStateManager;
    private final Map<String, Boolean> simulcastPreferences = new HashMap<>();

    public VideoQualityStateManager(MeetSessionStateManager meetSessionStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
    }

    public void setSimulcastEnabled(String participantName, boolean enabled) {
        simulcastPreferences.put(participantName, enabled);
        log.info("Simulcast preference set to {} for participant: {}", enabled ? "enabled" : "disabled", participantName);
    }

    public boolean getSimulcastPreference(String participantName) {
        return simulcastPreferences.getOrDefault(participantName, true);
    }

    public void setQualityPreference(String participantName, String quality) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.setVideoQualityPreference(quality);
        BrowserPollingHelper.safeSleep(QUALITY_CHANGE_DELAY_MS);
    }

    public void setMaxReceiveBandwidth(String participantName, int kbps) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.setMaxReceiveBandwidth(kbps);
        BrowserPollingHelper.safeSleep(BANDWIDTH_CHANGE_DELAY_MS);
    }

    public void setVideoSubscribed(String subscriber, String publisher, boolean subscribed) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        meetInstance.setVideoSubscribed(publisher, subscribed);
    }

    public Long getReceivedVideoWidth(String subscriber, String publisher) {
        WebDriver driver = meetSessionStateManager.getWebDriver(subscriber);
        return pollForVideoWidth(driver, publisher, VIDEO_WIDTH_MAX_ATTEMPTS, POLL_DELAY_MS);
    }

    public Long pollForLowerQualityWidth(String subscriber, String publisher, int maxWidth) {
        WebDriver driver = meetSessionStateManager.getWebDriver(subscriber);
        return pollForLowerQualityLayer(driver, publisher, maxWidth, LOWER_LAYER_MAX_ATTEMPTS, POLL_DELAY_MS);
    }

    public boolean isDynacastEnabled(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.isDynacastEnabled();
    }

    public boolean waitForTrackStreamState(String subscriber, String publisher, String expectedState, int timeoutMs) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        return meetInstance.waitForTrackStreamState(publisher, expectedState, timeoutMs);
    }

    public String getTrackStreamState(String subscriber, String publisher) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        return meetInstance.getTrackStreamState(publisher);
    }

    public void measureBitrate(String participantName, int seconds) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        WebDriver driver = meetSessionStateManager.getWebDriver(participantName);
        Long bitrateKbps = (Long) ((JavascriptExecutor) driver).executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "window.LiveKitTestHelpers.measureVideoBitrateOverInterval(" + (seconds * 1000) + ")" +
            ".then(function(kbps) { callback(kbps); })" +
            ".catch(function() { callback(0); });"
        );
        meetInstance.setStoredBitrate(bitrateKbps != null ? bitrateKbps.intValue() : 0);
        log.info("Measured {} video publish bitrate: {} kbps", participantName, meetInstance.getStoredBitrate());
    }

    public int getStoredBitrate(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getStoredBitrate();
    }

    public int measureCurrentBitrate(String participantName) {
        WebDriver driver = meetSessionStateManager.getWebDriver(participantName);
        Long currentBitrateKbps = (Long) ((JavascriptExecutor) driver).executeAsyncScript(
            "var callback = arguments[arguments.length - 1];" +
            "window.LiveKitTestHelpers.measureVideoBitrateOverInterval(" + BITRATE_MEASUREMENT_MS + ")" +
            ".then(function(kbps) { callback(kbps); })" +
            ".catch(function() { callback(0); });"
        );
        return currentBitrateKbps != null ? currentBitrateKbps.intValue() : 0;
    }

    public void clearAll() {
        log.info("Clearing video quality state");
        simulcastPreferences.clear();
    }

    private Long pollForVideoWidth(WebDriver driver, String publisherIdentity, int maxAttempts, long delayMs) {
        return BrowserPollingHelper.pollUntil(
            () -> executeVideoWidthScript(driver, publisherIdentity),
            width -> width != null && width > 0,
            maxAttempts,
            delayMs
        );
    }

    private Long pollForLowerQualityLayer(WebDriver driver, String publisherIdentity, int maxWidth, int maxAttempts, long delayMs) {
        return BrowserPollingHelper.pollUntil(
            () -> executeVideoWidthScript(driver, publisherIdentity),
            width -> width != null && width > 0 && width <= maxWidth,
            maxAttempts,
            delayMs
        );
    }

    private Long executeVideoWidthScript(WebDriver driver, String publisherIdentity) {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.getRemoteVideoTrackWidthByPublisher(arguments[0]);",
                publisherIdentity
            );
            return result == null ? 0L : ((Number) result).longValue();
        } catch (Exception e) {
            log.debug("Failed to get video width for {}: {}", publisherIdentity, e.getMessage());
            return 0L;
        }
    }
}
