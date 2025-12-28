package ro.stancalau.test.framework.capabilities.impl;

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
    return jsExecutor.execute("isRealWebRTCConnectionVerified", Boolean.class).orElse(false);
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
    return jsExecutor.execute("isSubscriptionPermissionDenied", Boolean.class).orElse(false);
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
}
