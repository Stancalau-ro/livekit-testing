package ro.stancalau.test.framework.capabilities;

import java.util.List;
import java.util.Map;

public interface DataChannelCapability {

    boolean sendDataMessage(String message, boolean reliable);

    boolean sendDataMessageTo(String message, String recipientIdentity, boolean reliable);

    boolean sendDataMessageOfSize(int sizeBytes, boolean reliable);

    boolean sendTimestampedDataMessage(String message, boolean reliable);

    boolean hasReceivedDataMessage(String expectedContent, String fromIdentity);

    int getReceivedDataMessageCount();

    List<Map<String, Object>> getReceivedDataMessages();

    boolean isDataPublishingBlocked();

    String getLastDataChannelError();

    Map<String, Object> getDataChannelLatencyStats();

    void clearDataChannelState();
}
