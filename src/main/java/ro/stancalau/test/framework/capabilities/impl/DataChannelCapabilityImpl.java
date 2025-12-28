package ro.stancalau.test.framework.capabilities.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import ro.stancalau.test.framework.capabilities.DataChannelCapability;
import ro.stancalau.test.framework.js.JsExecutor;

public class DataChannelCapabilityImpl implements DataChannelCapability {

  private final JsExecutor jsExecutor;

  public DataChannelCapabilityImpl(JsExecutor jsExecutor) {
    this.jsExecutor = jsExecutor;
  }

  @Override
  public boolean sendDataMessage(String message, boolean reliable) {
    return jsExecutor
        .execute("sendDataMessage", Boolean.class, message, reliable, null)
        .orElse(false);
  }

  @Override
  public boolean sendDataMessageTo(String message, String recipientIdentity, boolean reliable) {
    Object[] recipients = new Object[] {recipientIdentity};
    return jsExecutor
        .execute("sendDataMessage", Boolean.class, message, reliable, recipients)
        .orElse(false);
  }

  @Override
  public boolean sendDataMessageOfSize(int sizeBytes, boolean reliable) {
    return jsExecutor
        .execute("sendDataMessageOfSize", Boolean.class, sizeBytes, reliable)
        .orElse(false);
  }

  @Override
  public boolean sendTimestampedDataMessage(String message, boolean reliable) {
    return jsExecutor
        .execute("sendTimestampedDataMessage", Boolean.class, message, reliable)
        .orElse(false);
  }

  @Override
  public boolean hasReceivedDataMessage(String expectedContent, String fromIdentity) {
    return jsExecutor
        .execute("hasReceivedDataMessage", Boolean.class, expectedContent, fromIdentity)
        .orElse(false);
  }

  @Override
  public int getReceivedDataMessageCount() {
    return jsExecutor.execute("getReceivedDataMessageCount", Long.class).orElse(0L).intValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getReceivedDataMessages() {
    return jsExecutor
        .execute("getReceivedDataMessages", List.class)
        .orElse(Collections.emptyList());
  }

  @Override
  public boolean isDataPublishingBlocked() {
    return jsExecutor.execute("isDataPublishingBlocked", Boolean.class).orElse(false);
  }

  @Override
  public String getLastDataChannelError() {
    return jsExecutor.execute("getLastDataChannelError", String.class).orElse("");
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> getDataChannelLatencyStats() {
    return jsExecutor
        .execute("getDataChannelLatencyStats", Map.class)
        .orElse(Collections.emptyMap());
  }

  @Override
  public void clearDataChannelState() {
    jsExecutor.executeVoid("clearDataChannelState");
  }
}
