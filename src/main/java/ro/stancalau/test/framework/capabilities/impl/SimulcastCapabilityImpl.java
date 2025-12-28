package ro.stancalau.test.framework.capabilities.impl;

import ro.stancalau.test.framework.capabilities.SimulcastCapability;
import ro.stancalau.test.framework.js.JsExecutor;

public class SimulcastCapabilityImpl implements SimulcastCapability {

  private static final int DEFAULT_BITRATE_MEASUREMENT_MS = 3000;

  private final JsExecutor jsExecutor;

  public SimulcastCapabilityImpl(JsExecutor jsExecutor) {
    this.jsExecutor = jsExecutor;
  }

  @Override
  public void enableSimulcast() {
    jsExecutor.executeVoid("enableSimulcast");
  }

  @Override
  public void disableSimulcast() {
    jsExecutor.executeVoid("disableSimulcast");
  }

  @Override
  public boolean isSimulcastEnabled() {
    return jsExecutor.execute("isSimulcastEnabled", Boolean.class).orElse(false);
  }

  @Override
  public void setVideoQualityPreference(String quality) {
    jsExecutor.executeVoid("setVideoQualityPreference", quality);
  }

  @Override
  public String getVideoQualityPreference() {
    return jsExecutor.execute("getVideoQualityPreference", String.class).orElse("HIGH");
  }

  @Override
  public void setMaxReceiveBandwidth(int kbps) {
    jsExecutor.executeVoid("setMaxReceiveBandwidth", kbps);
  }

  @Override
  public boolean isDynacastEnabled() {
    return jsExecutor.execute("isDynacastEnabled", Boolean.class).orElse(false);
  }

  @Override
  public String getTrackStreamState(String publisherIdentity) {
    return jsExecutor.execute("getTrackStreamState", String.class, publisherIdentity).orElse(null);
  }

  @Override
  public boolean setVideoSubscribed(String publisherIdentity, boolean subscribed) {
    return jsExecutor
        .execute("setVideoSubscribed", Boolean.class, publisherIdentity, subscribed)
        .orElse(false);
  }

  @Override
  public Long getRemoteVideoTrackWidthByPublisher(String publisherIdentity) {
    return jsExecutor
        .execute("getRemoteVideoTrackWidthByPublisher", Long.class, publisherIdentity)
        .orElse(0L);
  }

  @Override
  public long measureVideoBitrateOverInterval(int milliseconds) {
    long scriptTimeout = milliseconds + 5000;
    return jsExecutor
        .executeAsync("measureVideoBitrateOverInterval", Long.class, scriptTimeout, milliseconds)
        .orElse(0L);
  }

  @Override
  public void clearDynacastState() {
    jsExecutor.executeVoid("clearDynacastState");
  }
}
