package ro.stancalau.test.framework.capabilities;

public interface ConnectionCapability {

    boolean isLiveKitLoaded();

    boolean isConnectionEstablished();

    boolean isClientConnected();

    boolean isInMeetingRoom();

    boolean isRealWebRTCConnectionVerified();

    boolean isUsingMock();

    long getConnectionTime();

    String getLastError();

    String getConsoleLogs();

    long getSubscriptionFailedEventCount();

    boolean isSubscriptionPermissionDenied();

    String getLastSubscriptionError();

    long getPlayingVideoElementCount();

    long getSubscribedVideoTrackCount();

    boolean isReceivingVideoFrom(String publisherIdentity);

    VideoReceptionStats getSubscriberVideoStats(String publisherIdentity);

    VideoReceptionRate measureVideoReceptionRate(String publisherIdentity, long intervalMs);

    record VideoReceptionStats(
            boolean isSubscribed, boolean hasTrack, int frameWidth, int frameHeight, boolean isPlaying) {}

    record VideoReceptionRate(long bytesPerSecond, double framesPerSecond) {}
}
