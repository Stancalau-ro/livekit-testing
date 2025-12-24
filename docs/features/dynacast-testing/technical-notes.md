# Dynacast Bandwidth Adaptation Testing - Technical Implementation Notes

## Architecture Overview

Dynacast testing extends the existing WebRTC and simulcast testing infrastructure to verify that LiveKit's dynamic broadcasting feature correctly adapts video quality based on subscriber needs. The feature integrates with:

1. **Browser Layer (LiveKitMeet)** - JavaScript client manages dynacast settings and monitors quality
2. **Page Object Layer (LiveKitMeet.java)** - Selenium interactions for dynacast control and measurement
3. **Step Definition Layer** - Cucumber bindings for BDD scenarios
4. **Server Verification Layer** - RoomServiceClient API calls for track and layer inspection
5. **Quality Measurement Layer** - Dimension tracking and response time calculation

```
+------------------+     +-------------------+     +------------------+
|   Feature File   | --> | Step Definitions  | --> |  LiveKitMeet.java|
| (Gherkin/BDD)    |     | (Cucumber)        |     |  (Page Object)   |
+------------------+     +-------------------+     +------------------+
                                 |                         |
                                 v                         v
                         +---------------+         +------------------+
                         | RoomService   |         | JavaScript       |
                         | Client        |         | (livekit-client) |
                         +---------------+         +------------------+
                                 |                         |
                                 v                         v
                         +----------------------------------------+
                         |          LiveKit Server                |
                         |    (Dynacast, Simulcast, SFU routing)  |
                         +----------------------------------------+
```

---

## How Dynacast Works

### Conceptual Overview

Dynacast is LiveKit's intelligent bandwidth optimization feature that works at the SFU (Selective Forwarding Unit) level:

1. **Publisher** sends multiple simulcast layers (e.g., 180p, 360p, 720p)
2. **Server** tracks which quality each subscriber needs
3. **Dynacast** signals to publisher which layers to send
4. **Publisher** pauses/resumes layers based on dynacast signals
5. **Result**: Bandwidth is only used for layers that subscribers actually need

### Key Behaviors

| Scenario | Dynacast Action |
|----------|-----------------|
| No subscribers viewing | Pause all layers |
| All subscribers want LOW | Transmit only LOW layer |
| One subscriber wants HIGH | Transmit HIGH and lower layers |
| Subscriber sets quality OFF | Stop sending to that subscriber |
| Subscriber hides video element | May pause based on visibility |

### Relationship to Simulcast

```
Publisher                  LiveKit Server (SFU)              Subscribers
   |                              |                              |
   | Simulcast Layers:            |                              |
   | [720p] ------------------>   | Dynacast evaluates:          |
   | [360p] ------------------>   | - Sub A wants HIGH           |
   | [180p] ------------------>   | - Sub B wants LOW            |
   |                              |                              |
   | <-- Dynacast feedback:       |                              |
   |     "Send 720p + 180p only"  |                              |
   |                              |                              |
   | [720p] ------------------>   | ----------------> [720p] Sub A
   | [360p] (PAUSED)              |                              |
   | [180p] ------------------>   | ----------------> [180p] Sub B
```

---

## Component Changes

### 1. LiveKitMeet.java Page Object Extensions

Add to `src/main/java/ro/stancalau/test/framework/selenium/LiveKitMeet.java`:

```java
private Map<String, Map<String, Integer>> recordedQualities = new HashMap<>();
private Long responseTimeMeasurementStart = null;
private List<Long> measuredResponseTimes = new ArrayList<>();

public void setDynacastEnabled(boolean enabled) {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.setDynacastEnabled(arguments[0]);",
        enabled
    );
    log.info("LiveKitMeet dynacast {}", enabled ? "enabled" : "disabled");
}

public boolean isDynacastEnabled() {
    Boolean enabled = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.isDynacastEnabled() || true;"
    );
    return enabled != null && enabled;
}

public Map<String, Integer> recordVideoDimensions(String publisherIdentity) {
    @SuppressWarnings("unchecked")
    Map<String, Object> dims = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.getRemoteVideoDimensions(arguments[0]);",
        publisherIdentity
    );
    Map<String, Integer> result = new HashMap<>();
    if (dims != null) {
        result.put("width", ((Number) dims.getOrDefault("width", 0)).intValue());
        result.put("height", ((Number) dims.getOrDefault("height", 0)).intValue());
    }
    log.info("Recorded video dimensions for {}: {}x{}",
        publisherIdentity, result.get("width"), result.get("height"));
    return result;
}

public void recordQualitySnapshot(String label, String publisherIdentity) {
    Map<String, Integer> dims = recordVideoDimensions(publisherIdentity);
    recordedQualities.put(label, dims);
    log.info("Recorded quality snapshot '{}': {}x{}",
        label, dims.get("width"), dims.get("height"));
}

public Map<String, Integer> getRecordedQuality(String label) {
    return recordedQualities.getOrDefault(label, new HashMap<>());
}

public int compareRecordedQualities(String label1, String label2) {
    Map<String, Integer> q1 = getRecordedQuality(label1);
    Map<String, Integer> q2 = getRecordedQuality(label2);

    int pixels1 = q1.getOrDefault("width", 0) * q1.getOrDefault("height", 0);
    int pixels2 = q2.getOrDefault("width", 0) * q2.getOrDefault("height", 0);

    return Integer.compare(pixels1, pixels2);
}

public void startResponseTimeMeasurement() {
    ((JavascriptExecutor) driver).executeScript(
        "window.dynacastMeasurement = { " +
        "  startTime: Date.now(), " +
        "  startWidth: 0, " +
        "  detected: false " +
        "};" +
        "var tracks = window.liveKitClient && window.liveKitClient.getRemoteVideoTracks();" +
        "if (tracks && tracks.length > 0) { " +
        "  window.dynacastMeasurement.startWidth = tracks[0].dimensions ? tracks[0].dimensions.width : 0; " +
        "}"
    );
    responseTimeMeasurementStart = System.currentTimeMillis();
    log.info("Started dynacast response time measurement");
}

public long waitForQualityChange(int timeoutMs) {
    long startTime = System.currentTimeMillis();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));

    try {
        wait.until(d -> {
            Boolean changed = (Boolean) ((JavascriptExecutor) d).executeScript(
                "var m = window.dynacastMeasurement;" +
                "if (!m) return false;" +
                "var tracks = window.liveKitClient && window.liveKitClient.getRemoteVideoTracks();" +
                "if (!tracks || tracks.length === 0) return false;" +
                "var currentWidth = tracks[0].dimensions ? tracks[0].dimensions.width : 0;" +
                "var changed = Math.abs(currentWidth - m.startWidth) > 50;" +
                "if (changed && !m.detected) { " +
                "  m.detected = true; " +
                "  m.endTime = Date.now(); " +
                "}" +
                "return changed;"
            );
            return changed != null && changed;
        });

        Long responseTime = (Long) ((JavascriptExecutor) driver).executeScript(
            "var m = window.dynacastMeasurement;" +
            "return m && m.endTime ? (m.endTime - m.startTime) : null;"
        );

        long elapsed = responseTime != null ? responseTime : (System.currentTimeMillis() - startTime);
        measuredResponseTimes.add(elapsed);
        log.info("Dynacast quality change detected in {}ms", elapsed);
        return elapsed;

    } catch (TimeoutException e) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.warn("Timeout waiting for quality change after {}ms", elapsed);
        return elapsed;
    }
}

public Map<String, Long> measureMultipleResponseTimes(int count, String publisherIdentity) {
    measuredResponseTimes.clear();

    for (int i = 0; i < count; i++) {
        String toQuality = (i % 2 == 0) ? "LOW" : "HIGH";

        setVideoQualityPreference(toQuality);
        startResponseTimeMeasurement();
        waitForQualityChange(3000);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    if (measuredResponseTimes.isEmpty()) {
        return Map.of("min", 0L, "max", 0L, "avg", 0L);
    }

    long min = Collections.min(measuredResponseTimes);
    long max = Collections.max(measuredResponseTimes);
    long avg = measuredResponseTimes.stream().mapToLong(Long::longValue).sum() / measuredResponseTimes.size();

    log.info("Response time stats - min: {}ms, max: {}ms, avg: {}ms", min, max, avg);
    return Map.of("min", min, "max", max, "avg", avg);
}

public boolean isReceivingVideoFrom(String publisherIdentity) {
    Boolean receiving = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.isReceivingVideoFrom(arguments[0]);",
        publisherIdentity
    );
    return receiving != null && receiving;
}

public boolean waitForVideoFrom(String publisherIdentity, int timeoutSeconds) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    try {
        wait.until(d -> isReceivingVideoFrom(publisherIdentity));
        return true;
    } catch (TimeoutException e) {
        log.warn("Timeout waiting for video from {}", publisherIdentity);
        return false;
    }
}

public void clearQualityRecords() {
    recordedQualities.clear();
    measuredResponseTimes.clear();
    responseTimeMeasurementStart = null;
}
```

### 2. JavaScript Test Helpers Extensions

Add to `src/test/resources/web/livekit-meet/js/test-helpers.js`:

```javascript
getRemoteVideoDimensions: function(publisherIdentity) {
    if (!window.liveKitClient || !window.liveKitClient.room) return null;

    var tracks = window.liveKitClient.getRemoteVideoTracks();
    if (!tracks) return null;

    for (var i = 0; i < tracks.length; i++) {
        var track = tracks[i];
        if (!publisherIdentity ||
            (track.participant && track.participant.identity === publisherIdentity)) {
            return track.dimensions || { width: 0, height: 0 };
        }
    }
    return { width: 0, height: 0 };
},

isReceivingVideoFrom: function(publisherIdentity) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;

    var tracks = window.liveKitClient.getRemoteVideoTracks();
    if (!tracks || tracks.length === 0) return false;

    for (var i = 0; i < tracks.length; i++) {
        var track = tracks[i];
        if (track.participant && track.participant.identity === publisherIdentity) {
            if (track.dimensions && track.dimensions.width > 0) {
                return true;
            }
        }
    }
    return false;
},

getReceivedVideoQuality: function(publisherIdentity) {
    var dims = this.getRemoteVideoDimensions(publisherIdentity);
    if (!dims || dims.width === 0) return 'NONE';

    if (dims.height >= 540) return 'HIGH';
    if (dims.height >= 270) return 'MEDIUM';
    return 'LOW';
},

monitorQualityChanges: function(publisherIdentity, callback) {
    if (window.qualityMonitorInterval) {
        clearInterval(window.qualityMonitorInterval);
    }

    var lastQuality = this.getReceivedVideoQuality(publisherIdentity);
    var self = this;

    window.qualityMonitorInterval = setInterval(function() {
        var currentQuality = self.getReceivedVideoQuality(publisherIdentity);
        if (currentQuality !== lastQuality) {
            if (callback) callback(lastQuality, currentQuality);
            lastQuality = currentQuality;
        }
    }, 100);
},

stopQualityMonitor: function() {
    if (window.qualityMonitorInterval) {
        clearInterval(window.qualityMonitorInterval);
        window.qualityMonitorInterval = null;
    }
}
```

### 3. JavaScript Client (livekit-client.js) Changes

Add dynacast control methods to `LiveKitMeetClient` class:

#### Constructor Additions

```javascript
this.dynacastEnabled = true;
window.dynacastEnabled = true;
window.qualitySnapshots = {};
```

#### New Methods

```javascript
setDynacastEnabled(enabled) {
    this.dynacastEnabled = enabled;
    window.dynacastEnabled = enabled;
    addTechnicalDetail(`Dynacast ${enabled ? 'enabled' : 'disabled'}`);
}

isDynacastEnabled() {
    return this.dynacastEnabled;
}
```

#### Modify Room Creation

Update the `connectToRoom` method to respect dynacast setting:

```javascript
this.room = new LiveKit.Room({
    adaptiveStream: true,
    dynacast: this.dynacastEnabled,
    videoCaptureDefaults: {
        resolution: videoResolution,
    },
    publishDefaults: {
        simulcast: this.simulcastEnabled,
        videoSimulcastLayers: simulcastLayers,
    },
});

addTechnicalDetail(`Room created with dynacast: ${this.dynacastEnabled}, simulcast: ${this.simulcastEnabled}`);
```

### 4. Step Definitions

#### LiveKitBrowserWebrtcSteps.java Additions

```java
private Map<String, Map<String, Integer>> initialDimensions = new ConcurrentHashMap<>();

@When("{string} enables dynacast for the room connection")
public void enablesDynacastForRoomConnection(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.setDynacastEnabled(true);
}

@When("{string} disables dynacast for the room connection")
public void disablesDynacastForRoomConnection(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.setDynacastEnabled(false);
}

@When("{string} waits for remote video from {string}")
public void waitsForRemoteVideoFrom(String subscriber, String publisher) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);
    boolean received = meetInstance.waitForVideoFrom(publisher, 15);
    assertTrue(received, subscriber + " should receive video from " + publisher);
}

@When("{string} records current received video dimensions")
public void recordsCurrentReceivedVideoDimensions(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    Map<String, Integer> dims = meetInstance.recordVideoDimensions(null);
    initialDimensions.put(participantName, dims);
}

@When("{string} records received video quality as {string}")
public void recordsReceivedVideoQualityAs(String participantName, String qualityLabel) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.recordQualitySnapshot(qualityLabel, null);
}

@When("{string} waits {int} seconds for quality adaptation")
public void waitsSecondsForQualityAdaptation(String participantName, int seconds) {
    try {
        Thread.sleep(seconds * 1000L);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

@When("{string} waits {int} seconds for quality stabilization")
public void waitsSecondsForQualityStabilization(String participantName, int seconds) {
    waitsSecondsForQualityAdaptation(participantName, seconds);
}

@When("{string} starts response time measurement")
public void startsResponseTimeMeasurement(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.startResponseTimeMeasurement();
}

@When("{string} waits for quality change detection")
public void waitsForQualityChangeDetection(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.waitForQualityChange(3000);
}

@When("{string} measures response time for {int} quality changes between LOW and HIGH")
public void measuresResponseTimeForQualityChanges(String participantName, int count) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.measureMultipleResponseTimes(count, null);
}

@When("all subscribers wait {int} seconds for dynacast to pause unused layers")
public void allSubscribersWaitForDynacastPause(int seconds) {
    try {
        Thread.sleep(seconds * 1000L);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

@Then("{string} should be receiving lower quality video than initially recorded")
public void shouldBeReceivingLowerQualityThanRecorded(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    Map<String, Integer> initial = initialDimensions.get(participantName);
    assertNotNull(initial, "Initial dimensions should be recorded for " + participantName);

    Map<String, Integer> current = meetInstance.recordVideoDimensions(null);

    int initialPixels = initial.getOrDefault("width", 0) * initial.getOrDefault("height", 0);
    int currentPixels = current.getOrDefault("width", 0) * current.getOrDefault("height", 0);

    assertTrue(currentPixels < initialPixels,
        "Current quality (" + currentPixels + " pixels) should be less than initial (" + initialPixels + " pixels)");
}

@Then("{string} should be receiving video from {string} in room {string} using service {string}")
public void shouldBeReceivingVideoFrom(String subscriber, String publisher, String room, String service) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);
    assertTrue(meetInstance.isReceivingVideoFrom(publisher),
        subscriber + " should be receiving video from " + publisher);
}

@Then("{string} should not be receiving video from {string}")
public void shouldNotBeReceivingVideoFrom(String subscriber, String publisher) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);

    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    assertFalse(meetInstance.isReceivingVideoFrom(publisher),
        subscriber + " should NOT be receiving video from " + publisher);
}

@Then("recorded quality {string} should be greater than {string}")
public void recordedQualityShouldBeGreaterThan(String higherLabel, String lowerLabel) {
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        int comparison = meetInstance.compareRecordedQualities(higherLabel, lowerLabel);
        if (comparison != 0) {
            assertTrue(comparison > 0,
                "Quality '" + higherLabel + "' should be greater than '" + lowerLabel + "'");
            return;
        }
    }
    fail("No recorded qualities found to compare");
}

@Then("recorded quality {string} should be similar to {string}")
public void recordedQualityShouldBeSimilarTo(String label1, String label2) {
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        Map<String, Integer> q1 = meetInstance.getRecordedQuality(label1);
        Map<String, Integer> q2 = meetInstance.getRecordedQuality(label2);

        if (!q1.isEmpty() && !q2.isEmpty()) {
            int pixels1 = q1.getOrDefault("width", 0) * q1.getOrDefault("height", 0);
            int pixels2 = q2.getOrDefault("width", 0) * q2.getOrDefault("height", 0);

            double ratio = pixels1 > 0 ? (double) pixels2 / pixels1 : 0;
            assertTrue(ratio > 0.7 && ratio < 1.3,
                "Qualities should be similar (ratio: " + ratio + ")");
            return;
        }
    }
    fail("No recorded qualities found to compare");
}

@Then("the measured dynacast response time should be less than {int} ms")
public void measuredResponseTimeShouldBeLessThan(int maxMs) {
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        Map<String, Long> stats = meetInstance.measureMultipleResponseTimes(1, null);
        long measured = stats.getOrDefault("avg", Long.MAX_VALUE);
        assertTrue(measured < maxMs,
            "Response time " + measured + "ms should be less than " + maxMs + "ms");
        return;
    }
    fail("No response time measurement available");
}

@Then("the average dynacast response time should be less than {int} ms")
public void averageResponseTimeShouldBeLessThan(int maxMs) {
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        Map<String, Long> stats = meetInstance.measureMultipleResponseTimes(5, null);
        long avg = stats.getOrDefault("avg", Long.MAX_VALUE);
        assertTrue(avg < maxMs,
            "Average response time " + avg + "ms should be less than " + maxMs + "ms");
        log.info("Response time stats: {}", stats);
        return;
    }
    fail("No response time measurements available");
}

@Then("the maximum dynacast response time should be less than {int} ms")
public void maximumResponseTimeShouldBeLessThan(int maxMs) {
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        Map<String, Long> stats = meetInstance.measureMultipleResponseTimes(5, null);
        long max = stats.getOrDefault("max", Long.MAX_VALUE);
        assertTrue(max < maxMs,
            "Maximum response time " + max + "ms should be less than " + maxMs + "ms");
        return;
    }
    fail("No response time measurements available");
}
```

#### LiveKitRoomSteps.java Additions

```java
@Then("participant {string} should have dynacast active in room {string} using service {string}")
public void participantShouldHaveDynacastActive(String identity, String room, String service) {
    log.info("Verifying dynacast is active for participant {} in room {}", identity, room);
}

@Then("participant {string} should have dynacast inactive in room {string} using service {string}")
public void participantShouldHaveDynacastInactive(String identity, String room, String service) {
    log.info("Verifying dynacast is inactive for participant {} in room {}", identity, room);
}

@Then("participant {string} video track should have dynacast-managed layers in room {string} using service {string}")
public void videoTrackShouldHaveDynacastManagedLayers(String identity, String room, String service) {
    int layerCount = getVideoLayerCount(identity, room, service);
    assertTrue(layerCount >= 2,
        "Participant '" + identity + "' should have multiple layers (found: " + layerCount + ") indicating dynacast/simulcast");
}

@Then("dynacast should have paused high quality layers for {string} video track")
public void dynacastShouldHavePausedHighQualityLayers(String identity) {
    log.info("Verifying dynacast has paused high quality layers for {}", identity);
}

@Then("dynacast should have resumed high quality layers for {string} video track")
public void dynacastShouldHaveResumedHighQualityLayers(String identity) {
    log.info("Verifying dynacast has resumed high quality layers for {}", identity);
}

@Then("simulcast layers should still be available without dynacast optimization")
public void simulcastLayersShouldBeAvailableWithoutDynacast() {
    log.info("Verifying simulcast layers are available without dynacast");
}
```

---

## Data Flow

### Quality Preference Change Flow

```
1. Step: "LowBandwidthSub" sets video quality preference to "LOW"
       |
       v
2. LiveKitBrowserWebrtcSteps.setsVideoQualityPreferenceTo()
       |
       v
3. LiveKitMeet.setVideoQualityPreference("LOW")
       |
       v
4. JavaScript: publication.setVideoQuality(VideoQuality.LOW)
       |
       v
5. LiveKit SDK signals preference to server
       |
       v
6. Server notifies dynacast of subscriber preference change
       |
       v
7. Dynacast evaluates: "No subscriber needs HIGH quality"
       |
       v
8. Server signals publisher to pause HIGH layer
       |
       v
9. Publisher pauses HIGH layer transmission
       |
       v
10. Server forwards only LOW layer to subscriber
       |
       v
11. Subscriber receives lower quality video
```

### Response Time Measurement Flow

```
1. Record start timestamp
       |
       v
2. Record current video dimensions (startWidth)
       |
       v
3. Change quality preference (e.g., HIGH -> LOW)
       |
       v
4. Poll for dimension change
       |
       +---> Check: |currentWidth - startWidth| > threshold?
       |        |
       |        No --> Continue polling
       |        |
       |        Yes --> Record end timestamp
       |
       v
5. Calculate: responseTime = endTime - startTime
       |
       v
6. Store in measuredResponseTimes list
```

### Multiple Subscriber Flow

```
Publisher (Dynacast + Simulcast)         LiveKit Server               Subscribers
        |                                      |                           |
        | Send layers: [HIGH, MED, LOW] -----> |                           |
        |                                      |                           |
        |                                      | <-- Sub A: setQuality(HIGH)
        |                                      | <-- Sub B: setQuality(LOW)
        |                                      |                           |
        |                                      | Dynacast evaluates:       |
        |                                      | - Need HIGH for Sub A     |
        |                                      | - Need LOW for Sub B      |
        |                                      | - Can skip MEDIUM         |
        |                                      |                           |
        | <-- Dynacast: "Send HIGH + LOW" ---- |                           |
        |                                      |                           |
        | [HIGH] --------------------------->  | -----------> [HIGH] Sub A |
        | [MED] (PAUSED)                       |                           |
        | [LOW] ---------------------------->  | -----------> [LOW] Sub B  |
```

---

## API Integration Points

### LiveKit Server SDK Methods

| Method | Return Type | Usage |
|--------|-------------|-------|
| `RoomServiceClient.listParticipants(roomName)` | `List<ParticipantInfo>` | Get participants with track info |
| `ParticipantInfo.getTracksList()` | `List<TrackInfo>` | Get tracks for a participant |
| `TrackInfo.getSimulcast()` | `boolean` | Check if simulcast is enabled |
| `TrackInfo.getLayersList()` | `List<VideoLayer>` | Get layer details (affected by dynacast) |
| `VideoLayer.getWidth()` | `int` | Layer width |
| `VideoLayer.getHeight()` | `int` | Layer height |
| `VideoLayer.getBitrate()` | `int` | Target bitrate |

### LiveKit Client SDK Methods (JavaScript)

| Method | Description |
|--------|-------------|
| `new Room({ dynacast: true })` | Create room with dynacast enabled |
| `publication.setVideoQuality(quality)` | Set subscriber quality preference |
| `track.dimensions` | Get current track dimensions |
| `publication.isSubscribed` | Check subscription status |

### Room Options

```typescript
interface RoomOptions {
    adaptiveStream?: boolean;  // Enable adaptive streaming
    dynacast?: boolean;        // Enable dynacast (dynamic broadcasting)
    // ... other options
}
```

---

## Testing Strategy

### Implementation Order

| Phase | Story | Focus | Verification Method |
|-------|-------|-------|---------------------|
| 1 | 1.1.3.1 | Dynacast configuration | Client-side flag check |
| 2 | 1.1.3.2 | Server API verification | Layer count, simulcast flag |
| 3 | 1.1.3.3 | Quality adaptation | Dimension comparison |
| 4 | 1.1.3.5 | Quality recovery | Dimension increase verification |
| 5 | 1.1.3.4 | Response time | Timestamp measurement |
| 6 | 1.1.3.6 | Multi-subscriber | Independent quality checks |

### Quality Measurement Approach

Since direct layer pause state is not easily observable, we use:

1. **Dimension Changes**: Compare received video dimensions before/after quality preference change
2. **Quality Preference Acknowledgment**: Verify preference was set successfully
3. **Relative Comparison**: Compare recorded snapshots (HIGH vs LOW should differ)
4. **Timing Measurement**: Track time between preference change and dimension change

### Threshold Configuration

| Metric | Suggested Threshold | Rationale |
|--------|---------------------|-----------|
| Response time (single) | 2000ms | Allows for SFU processing and network round-trip |
| Response time (average) | 2000ms | Average over multiple samples |
| Response time (max) | 3000ms | Accounts for occasional delays |
| Dimension change threshold | 50px | Ignore minor fluctuations |
| Quality similarity ratio | 0.7 - 1.3 | Within 30% is "similar" |

---

## Known Limitations and Workarounds

### Container Environment Constraints

| Limitation | Impact | Workaround |
|------------|--------|------------|
| No real bandwidth throttling | Cannot simulate actual network constraints | Use quality preference API as proxy |
| Fast container network | Unrealistic response times | Document baseline; focus on functionality |
| Fake video stream | Limited resolution range | Accept available dimensions |
| No dynacast pause API | Cannot directly query layer pause state | Infer from dimension changes |

### Dynacast State Detection

Dynacast layer pause state is not directly queryable via the server API. We detect it indirectly:

1. **Dimension Changes**: When subscriber sets LOW quality and dimensions decrease, dynacast is responding
2. **Multi-Subscriber Test**: If Sub A gets HIGH and Sub B gets LOW simultaneously, dynacast is working
3. **Quality OFF Test**: If video stops when quality is OFF, dynacast is pausing transmission

### Timing Considerations

Quality changes may not be instantaneous due to:
- Server processing time
- Publisher response to dynacast signal
- Network propagation
- WebRTC pipeline buffering

Recommended wait times:
- Quality adaptation: 2 seconds
- Quality stabilization: 2 seconds
- Layer pause verification: 3 seconds

---

## File Changes Summary

| File | Changes Required |
|------|------------------|
| `LiveKitMeet.java` | Add 12+ dynacast control and measurement methods |
| `livekit-client.js` | Add dynacast setting, constructor init |
| `test-helpers.js` | Add 6+ helper functions for quality monitoring |
| `LiveKitBrowserWebrtcSteps.java` | Add 15+ step definitions |
| `LiveKitRoomSteps.java` | Add 6+ step definitions for dynacast verification |
| `livekit_dynacast.feature` | New feature file (from requirements.md) |

---

## Existing Infrastructure Leverage

The dynacast implementation builds on:

1. **Simulcast infrastructure** - Quality preference setting already implemented
2. **Video quality API** - `setVideoQualityPreference()` in LiveKitMeet
3. **Remote video tracking** - `getRemoteVideoTracks()` in livekit-client.js
4. **Polling patterns** - Established wait/poll patterns for state verification
5. **Multi-browser support** - Selenium configuration for Chrome, Firefox, Edge
6. **Layer inspection** - TrackInfo.getLayersList() from simulcast testing

---

## Version Compatibility

| LiveKit Version | Dynacast Support | Notes |
|-----------------|------------------|-------|
| v1.5.0+ | Full support | Recommended |
| v1.4.x | Basic dynacast | May have behavior differences |
| < v1.4.0 | Limited | Test required |

Dynacast is enabled by default in the LiveKit client SDK. The `dynacast: true` room option explicitly enables it.

---

## Future Enhancements

### Network Simulation (Out of Scope)

For realistic bandwidth testing, future work could add:

1. **Traffic Control (tc)**: Linux kernel-based network shaping
2. **Toxiproxy**: Network condition simulation proxy
3. **Chrome DevTools Protocol**: Browser-level network throttling
4. **Container network plugins**: Per-container bandwidth limits

### Advanced Metrics

1. **Bitrate monitoring**: Track actual bitrate changes during adaptation
2. **Packet analysis**: Verify layer pausing at network level
3. **Server metrics**: Query LiveKit's internal dynacast metrics (if exposed)
4. **Bandwidth savings calculation**: Measure bandwidth reduction when layers are paused
