package ro.stancalau.test.framework.capabilities.impl;

import java.util.Map;
import ro.stancalau.test.framework.capabilities.ConnectionCapability;
import ro.stancalau.test.framework.js.JsExecutor;

public class ConnectionCapabilityImpl implements ConnectionCapability {

    private final JsExecutor jsExecutor;

    public ConnectionCapabilityImpl(JsExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    @Override
    public boolean isLiveKitLoaded() {
        return jsExecutor.execute("isLiveKitLoaded", Boolean.class).orElse(false);
    }

    @Override
    public boolean isConnectionEstablished() {
        return jsExecutor.execute("isConnectionEstablished", Boolean.class).orElse(false);
    }

    @Override
    public boolean isClientConnected() {
        return jsExecutor.execute("isClientConnected", Boolean.class).orElse(false);
    }

    @Override
    public boolean isInMeetingRoom() {
        return jsExecutor.execute("isInMeetingRoom", Boolean.class).orElse(false);
    }

    @Override
    public boolean isRealWebRTCConnectionVerified() {
        return jsExecutor
                .execute("isRealWebRTCConnectionVerified", Boolean.class)
                .orElse(false);
    }

    @Override
    public boolean isUsingMock() {
        return jsExecutor.execute("isUsingMock", Boolean.class).orElse(false);
    }

    @Override
    public long getConnectionTime() {
        return jsExecutor.execute("getConnectionTime", Long.class).orElse(0L);
    }

    @Override
    public String getLastError() {
        return jsExecutor.execute("getLastError", String.class).orElse("");
    }

    @Override
    public String getConsoleLogs() {
        return jsExecutor.execute("getConsoleLogs", String.class).orElse("");
    }

    @Override
    public long getSubscriptionFailedEventCount() {
        return jsExecutor.execute("getSubscriptionFailedEventCount", Long.class).orElse(0L);
    }

    @Override
    public boolean isSubscriptionPermissionDenied() {
        return jsExecutor
                .execute("isSubscriptionPermissionDenied", Boolean.class)
                .orElse(false);
    }

    @Override
    public String getLastSubscriptionError() {
        return jsExecutor.execute("getLastSubscriptionError", String.class).orElse("");
    }

    @Override
    public long getPlayingVideoElementCount() {
        return jsExecutor.execute("getPlayingVideoElementCount", Long.class).orElse(0L);
    }

    @Override
    public long getSubscribedVideoTrackCount() {
        return jsExecutor.execute("getSubscribedVideoTrackCount", Long.class).orElse(0L);
    }

    @Override
    public boolean isReceivingVideoFrom(String publisherIdentity) {
        return jsExecutor
                .executeAsync("isReceivingVideoFrom", Boolean.class, 10000, publisherIdentity)
                .orElse(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public VideoReceptionStats getSubscriberVideoStats(String publisherIdentity) {
        Map<String, Object> result = jsExecutor
                .executeAsync("getSubscriberVideoStats", Map.class, 10000, publisherIdentity)
                .orElse(null);
        if (result == null) {
            return null;
        }
        return new VideoReceptionStats(
                toBoolean(result.get("isSubscribed")),
                toBoolean(result.get("hasTrack")),
                toInt(result.get("frameWidth")),
                toInt(result.get("frameHeight")),
                toBoolean(result.get("isPlaying")));
    }

    @Override
    @SuppressWarnings("unchecked")
    public VideoReceptionRate measureVideoReceptionRate(String publisherIdentity, long intervalMs) {
        Map<String, Object> result = jsExecutor
                .executeAsync("measureVideoReceptionRate", Map.class, intervalMs + 5000, publisherIdentity)
                .orElse(null);
        if (result == null) {
            return new VideoReceptionRate(0, 0.0);
        }
        return new VideoReceptionRate(toLong(result.get("bytesPerSecond")), toDouble(result.get("framesPerSecond")));
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
}
