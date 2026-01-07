package ro.stancalau.test.framework.capabilities.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import ro.stancalau.test.framework.capabilities.MetadataCapability;
import ro.stancalau.test.framework.js.JsExecutor;

public class MetadataCapabilityImpl implements MetadataCapability {

    private final JsExecutor jsExecutor;

    public MetadataCapabilityImpl(JsExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    @Override
    public void startListeningForRoomMetadataEvents() {
        jsExecutor.executeVoid("startListeningForRoomMetadataEvents");
    }

    @Override
    public void startListeningForParticipantMetadataEvents() {
        jsExecutor.executeVoid("startListeningForParticipantMetadataEvents");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoomMetadataEvents() {
        return jsExecutor.execute("getRoomMetadataEvents", List.class).orElse(Collections.emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getParticipantMetadataEvents() {
        return jsExecutor.execute("getParticipantMetadataEvents", List.class).orElse(Collections.emptyList());
    }

    @Override
    public String getCurrentRoomMetadata() {
        return jsExecutor.execute("getCurrentRoomMetadata", String.class).orElse("");
    }

    @Override
    public String getParticipantMetadata(String identity) {
        return jsExecutor
                .execute("getParticipantMetadata", String.class, identity)
                .orElse("");
    }

    @Override
    public String getLocalParticipantMetadata() {
        return jsExecutor.execute("getLocalParticipantMetadata", String.class).orElse("");
    }

    @Override
    public boolean waitForRoomMetadataEvent(String expectedValue, int timeoutSeconds) {
        return jsExecutor
                .execute("waitForRoomMetadataEvent", Boolean.class, expectedValue, timeoutSeconds)
                .orElse(false);
    }

    @Override
    public boolean waitForParticipantMetadataEvent(String identity, String expectedValue, int timeoutSeconds) {
        return jsExecutor
                .execute("waitForParticipantMetadataEvent", Boolean.class, identity, expectedValue, timeoutSeconds)
                .orElse(false);
    }

    @Override
    public int getRoomMetadataEventCount() {
        return jsExecutor
                .execute("getRoomMetadataEventCount", Long.class)
                .orElse(0L)
                .intValue();
    }

    @Override
    public int getParticipantMetadataEventCount() {
        return jsExecutor
                .execute("getParticipantMetadataEventCount", Long.class)
                .orElse(0L)
                .intValue();
    }

    @Override
    public void clearMetadataEvents() {
        jsExecutor.executeVoid("clearMetadataEvents");
    }
}
