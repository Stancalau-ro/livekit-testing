package ro.stancalau.test.framework.capabilities;

public interface MediaControlCapability {

    void muteAudio();

    void unmuteAudio();

    void muteVideo();

    void unmuteVideo();

    boolean isAudioMuted();

    boolean isVideoMuted();

    boolean isScreenSharing();

    boolean isScreenShareBlocked();

    String getLastScreenShareError();
}
