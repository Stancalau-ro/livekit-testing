package ro.stancalau.test.framework.capabilities;

public interface SimulcastCapability {

    void enableSimulcast();

    void disableSimulcast();

    boolean isSimulcastEnabled();

    void setVideoQualityPreference(String quality);

    String getVideoQualityPreference();

    void setMaxReceiveBandwidth(int kbps);

    boolean isDynacastEnabled();

    String getTrackStreamState(String publisherIdentity);

    boolean setVideoSubscribed(String publisherIdentity, boolean subscribed);

    Long getRemoteVideoTrackWidthByPublisher(String publisherIdentity);

    long measureVideoBitrateOverInterval(int milliseconds);

    void clearDynacastState();
}
