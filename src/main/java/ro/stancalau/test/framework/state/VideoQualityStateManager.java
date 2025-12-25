package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;
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
        meetInstance.getSimulcast().setVideoQualityPreference(quality);
        BrowserPollingHelper.safeSleep(QUALITY_CHANGE_DELAY_MS);
    }

    public void setMaxReceiveBandwidth(String participantName, int kbps) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.getSimulcast().setMaxReceiveBandwidth(kbps);
        BrowserPollingHelper.safeSleep(BANDWIDTH_CHANGE_DELAY_MS);
    }

    public void setVideoSubscribed(String subscriber, String publisher, boolean subscribed) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        meetInstance.getSimulcast().setVideoSubscribed(publisher, subscribed);
    }

    public Long getReceivedVideoWidth(String subscriber, String publisher) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        Long width = pollForVideoWidth(meetInstance, publisher, VIDEO_WIDTH_MAX_ATTEMPTS, POLL_DELAY_MS);
        return width != null ? width : 0L;
    }

    public Long pollForLowerQualityWidth(String subscriber, String publisher, int maxWidth) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        Long width = pollForLowerQualityLayer(meetInstance, publisher, maxWidth, LOWER_LAYER_MAX_ATTEMPTS, POLL_DELAY_MS);
        return width != null ? width : 0L;
    }

    public boolean isDynacastEnabled(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getSimulcast().isDynacastEnabled();
    }

    public boolean waitForTrackStreamState(String subscriber, String publisher, String expectedState, int timeoutMs) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        return meetInstance.waitForTrackStreamState(publisher, expectedState, timeoutMs);
    }

    public String getTrackStreamState(String subscriber, String publisher) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(subscriber);
        return meetInstance.getSimulcast().getTrackStreamState(publisher);
    }

    public void measureBitrate(String participantName, int seconds) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        long bitrateKbps = meetInstance.getSimulcast().measureVideoBitrateOverInterval(seconds * 1000);
        meetInstance.setStoredBitrate((int) bitrateKbps);
        log.info("Measured {} video publish bitrate: {} kbps", participantName, meetInstance.getStoredBitrate());
    }

    public int getStoredBitrate(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getStoredBitrate();
    }

    public int measureCurrentBitrate(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        long currentBitrateKbps = meetInstance.getSimulcast().measureVideoBitrateOverInterval(BITRATE_MEASUREMENT_MS);
        return (int) currentBitrateKbps;
    }

    public void clearAll() {
        log.info("Clearing video quality state");
        simulcastPreferences.clear();
    }

    private Long pollForVideoWidth(LiveKitMeet meetInstance, String publisherIdentity, int maxAttempts, long delayMs) {
        return BrowserPollingHelper.pollUntil(
            () -> meetInstance.getSimulcast().getRemoteVideoTrackWidthByPublisher(publisherIdentity),
            width -> width != null && width > 0,
            maxAttempts,
            delayMs
        );
    }

    private Long pollForLowerQualityLayer(LiveKitMeet meetInstance, String publisherIdentity, int maxWidth, int maxAttempts, long delayMs) {
        return BrowserPollingHelper.pollUntil(
            () -> meetInstance.getSimulcast().getRemoteVideoTrackWidthByPublisher(publisherIdentity),
            width -> width != null && width > 0 && width <= maxWidth,
            maxAttempts,
            delayMs
        );
    }
}
