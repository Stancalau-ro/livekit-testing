package ro.stancalau.test.framework.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class EgressStateManager {
    
    private final Map<String, Map<String, String>> participantTrackIds = new HashMap<>();
    private final Map<String, String> activeRecordings = new HashMap<>();
    
    public EgressStateManager() {
    }
    
    public void storeTrackIds(String participantIdentity, Map<String, String> trackIds) {
        participantTrackIds.put(participantIdentity, trackIds);
        log.debug("Stored track IDs for participant {}: {}", participantIdentity, trackIds);
    }
    
    public Map<String, String> getTrackIds(String participantIdentity) {
        return participantTrackIds.get(participantIdentity);
    }
    
    public void storeActiveRecording(String key, String egressId) {
        activeRecordings.put(key, egressId);
        log.debug("Stored active recording for {}: {}", key, egressId);
    }
    
    public String getActiveRecording(String key) {
        return activeRecordings.get(key);
    }
    
    public void removeActiveRecording(String key) {
        activeRecordings.remove(key);
        log.debug("Removed active recording for {}", key);
    }
    
    public void clearAll() {
        participantTrackIds.clear();
        activeRecordings.clear();
        log.debug("Cleared all egress state");
    }
}