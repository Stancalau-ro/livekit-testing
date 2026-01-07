package ro.stancalau.test.framework.state;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ImageSnapshotStateManager {

    private final Map<String, Map<String, String>> participantTrackIds = new HashMap<>();
    private final Map<String, String> capturedSnapshots = new HashMap<>();

    public ImageSnapshotStateManager() {}

    public void storeTrackIds(String participantIdentity, Map<String, String> trackIds) {
        participantTrackIds.put(participantIdentity, trackIds);
        log.debug("Stored track IDs for participant {}: {}", participantIdentity, trackIds);
    }

    public Map<String, String> getTrackIds(String participantIdentity) {
        return participantTrackIds.get(participantIdentity);
    }

    public void storeCapturedSnapshot(String key, String imagePath) {
        capturedSnapshots.put(key, imagePath);
        log.debug("Stored captured snapshot for {}: {}", key, imagePath);
    }

    public String getCapturedSnapshot(String key) {
        return capturedSnapshots.get(key);
    }

    public void clearAll() {
        participantTrackIds.clear();
        capturedSnapshots.clear();
        log.debug("Cleared all image snapshot state");
    }
}
