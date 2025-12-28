package ro.stancalau.test.framework.capabilities;

import java.util.List;
import java.util.Map;

public interface MetadataCapability {

  void startListeningForRoomMetadataEvents();

  void startListeningForParticipantMetadataEvents();

  List<Map<String, Object>> getRoomMetadataEvents();

  List<Map<String, Object>> getParticipantMetadataEvents();

  String getCurrentRoomMetadata();

  String getParticipantMetadata(String identity);

  String getLocalParticipantMetadata();

  boolean waitForRoomMetadataEvent(String expectedValue, int timeoutSeconds);

  boolean waitForParticipantMetadataEvent(
      String identity, String expectedValue, int timeoutSeconds);

  int getRoomMetadataEventCount();

  int getParticipantMetadataEventCount();

  void clearMetadataEvents();
}
