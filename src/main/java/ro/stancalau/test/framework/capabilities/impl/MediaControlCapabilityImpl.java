package ro.stancalau.test.framework.capabilities.impl;

import ro.stancalau.test.framework.capabilities.MediaControlCapability;
import ro.stancalau.test.framework.js.JsExecutor;

public class MediaControlCapabilityImpl implements MediaControlCapability {

    private final JsExecutor jsExecutor;

    public MediaControlCapabilityImpl(JsExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    @Override
    public void muteAudio() {
        jsExecutor.executeVoid("muteAudio");
    }

    @Override
    public void unmuteAudio() {
        jsExecutor.executeVoid("unmuteAudio");
    }

    @Override
    public void muteVideo() {
        jsExecutor.executeVoid("muteVideo");
    }

    @Override
    public void unmuteVideo() {
        jsExecutor.executeVoid("unmuteVideo");
    }

    @Override
    public boolean isAudioMuted() {
        return jsExecutor.execute("isAudioMuted", Boolean.class).orElse(false);
    }

    @Override
    public boolean isVideoMuted() {
        return jsExecutor.execute("isVideoMuted", Boolean.class).orElse(false);
    }

    @Override
    public boolean isScreenSharing() {
        return jsExecutor.execute("isScreenSharing", Boolean.class).orElse(false);
    }

    @Override
    public boolean isScreenShareBlocked() {
        return jsExecutor
                .execute("isScreenSharePermissionDenied", Boolean.class)
                .orElse(false);
    }

    @Override
    public String getLastScreenShareError() {
        return jsExecutor.execute("getLastScreenShareError", String.class).orElse("");
    }
}
