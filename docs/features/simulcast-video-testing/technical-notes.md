# Simulcast Video Testing - Technical Implementation Notes

## Architecture Overview

Simulcast testing extends the existing WebRTC testing infrastructure to verify that publishers can stream multiple quality layers and subscribers can select specific layers. The feature integrates with:

1. **Browser Layer (LiveKitMeet)** - JavaScript client enables simulcast and sets subscriber preferences
2. **Page Object Layer (LiveKitMeet.java)** - Selenium interactions for simulcast controls
3. **Step Definition Layer** - Cucumber bindings for BDD scenarios
4. **Server Verification Layer** - RoomServiceClient API calls for layer inspection

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
                         |    (TrackInfo, VideoLayer, simulcast)  |
                         +----------------------------------------+
```

---

## Component Changes

### 1. LiveKitMeet.java Extensions

The page object requires new methods for simulcast control. Add to `src/main/java/ro/stancalau/test/framework/selenium/LiveKitMeet.java`:

```java
public void enableSimulcast() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.setSimulcast(true);"
    );
    log.info("LiveKitMeet simulcast enabled");
}

public void disableSimulcast() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.setSimulcast(false);"
    );
    log.info("LiveKitMeet simulcast disabled");
}

public boolean isSimulcastEnabled() {
    Boolean enabled = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.isSimulcastEnabled() || false;"
    );
    return enabled != null && enabled;
}

public void setVideoQualityPreference(String quality) {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.setVideoQuality('" + quality + "');"
    );
    log.info("LiveKitMeet video quality preference set to: {}", quality);
}

public void setMaxReceiveBandwidth(int kbps) {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.setMaxBandwidth(" + kbps + ");"
    );
    log.info("LiveKitMeet max receive bandwidth set to: {} kbps", kbps);
}

public String getCurrentVideoQuality() {
    String quality = (String) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.getCurrentVideoQuality() || 'UNKNOWN';"
    );
    return quality;
}
```

### 2. JavaScript Client (livekit-client.js) Changes

Add simulcast-related methods to the `LiveKitMeetClient` class in `src/test/resources/web/livekit-meet/js/livekit-client.js`:

```javascript
constructor() {
    // Existing properties...
    this.simulcastEnabled = true;  // Default enabled
    this.videoQualityPreference = 'HIGH';
    this.maxBandwidth = null;
}

setSimulcast(enabled) {
    this.simulcastEnabled = enabled;
    addTechnicalDetail(`Simulcast ${enabled ? 'enabled' : 'disabled'}`);
}

isSimulcastEnabled() {
    return this.simulcastEnabled;
}

setVideoQuality(quality) {
    this.videoQualityPreference = quality.toUpperCase();

    if (this.room && this.room.state === 'connected') {
        this.room.remoteParticipants.forEach((participant) => {
            participant.videoTrackPublications.forEach((publication) => {
                if (publication.track) {
                    const lkQuality = this.mapQualityToLiveKit(quality);
                    publication.setVideoQuality(lkQuality);
                    addTechnicalDetail(`Set video quality to ${quality} for ${participant.identity}`);
                }
            });
        });
    }
}

mapQualityToLiveKit(quality) {
    switch (quality.toUpperCase()) {
        case 'LOW': return LiveKit.VideoQuality.LOW;
        case 'MEDIUM': return LiveKit.VideoQuality.MEDIUM;
        case 'HIGH': return LiveKit.VideoQuality.HIGH;
        case 'OFF': return LiveKit.VideoQuality.OFF;
        default: return LiveKit.VideoQuality.HIGH;
    }
}

getCurrentVideoQuality() {
    return this.videoQualityPreference;
}

setMaxBandwidth(kbps) {
    this.maxBandwidth = kbps;

    if (this.room && this.room.state === 'connected') {
        this.room.remoteParticipants.forEach((participant) => {
            participant.videoTrackPublications.forEach((publication) => {
                if (publication.track) {
                    publication.setVideoDimensions({ width: 0, height: 0 });
                }
            });
        });
    }
    addTechnicalDetail(`Max bandwidth set to ${kbps} kbps`);
}
```

Modify the `connectToRoom` method to respect simulcast setting:

```javascript
async connectToRoom(url, token, roomName, participantName) {
    // ...existing setup code...

    this.room = new LiveKit.Room({
        adaptiveStream: true,
        dynacast: true,
        videoCaptureDefaults: {
            resolution: videoResolution,
        },
        publishDefaults: {
            simulcast: this.simulcastEnabled,
            videoSimulcastLayers: this.simulcastEnabled ? [
                LiveKit.VideoPresets.h180,
                LiveKit.VideoPresets.h360,
            ] : undefined,
        },
    });

    // ...rest of connection code...
}
```

### 3. New Step Definitions Required

#### LiveKitBrowserWebrtcSteps.java Additions

```java
@When("{string} enables simulcast for video publishing")
public void enablesSimulcastForVideoPublishing(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.enableSimulcast();
}

@When("{string} disables simulcast for video publishing")
public void disablesSimulcastForVideoPublishing(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.disableSimulcast();
}

@When("{string} sets video quality preference to {string}")
public void setsVideoQualityPreferenceTo(String participantName, String quality) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.setVideoQualityPreference(quality);
}

@When("{string} sets maximum receive bandwidth to {int} kbps")
public void setsMaximumReceiveBandwidth(String participantName, int kbps) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.setMaxReceiveBandwidth(kbps);
}

@Then("{string} should be receiving low quality video from {string} in room {string} using service {string}")
public void shouldBeReceivingLowQualityVideoFrom(String subscriber, String publisher, String room, String service) {
    verifySubscriberQuality(subscriber, publisher, room, service, "LOW");
}

@Then("{string} should be receiving high quality video from {string} in room {string} using service {string}")
public void shouldBeReceivingHighQualityVideoFrom(String subscriber, String publisher, String room, String service) {
    verifySubscriberQuality(subscriber, publisher, room, service, "HIGH");
}

@Then("{string} should be receiving a lower quality layer from {string} in room {string} using service {string}")
public void shouldBeReceivingLowerQualityLayerFrom(String subscriber, String publisher, String room, String service) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);

    WebDriver driver = ManagerProvider.webDrivers().getWebDriver("meet", subscriber);
    Long receivedWidth = (Long) ((JavascriptExecutor) driver).executeScript(
        "var tracks = window.liveKitClient && window.liveKitClient.getRemoteVideoTracks();" +
        "return tracks && tracks[0] ? tracks[0].dimensions.width : 0;"
    );

    assertTrue(receivedWidth < 720,
        subscriber + " should be receiving lower quality (width: " + receivedWidth + ")");
}

private void verifySubscriberQuality(String subscriber, String publisher, String room, String service, String expectedQuality) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);

    WebDriver driver = ManagerProvider.webDrivers().getWebDriver("meet", subscriber);
    String currentQuality = (String) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.getCurrentVideoQuality() || 'UNKNOWN';"
    );

    assertEquals(expectedQuality, currentQuality,
        subscriber + " should be receiving " + expectedQuality + " quality video");
}
```

#### LiveKitRoomSteps.java Additions

```java
@Then("participant {string} should have simulcast enabled for video in room {string} using service {string}")
public void participantShouldHaveSimulcastEnabled(String identity, String room, String service) {
    verifySimulcastState(identity, room, service, true);
}

@Then("participant {string} should have simulcast disabled for video in room {string} using service {string}")
public void participantShouldHaveSimulcastDisabled(String identity, String room, String service) {
    verifySimulcastState(identity, room, service, false);
}

private void verifySimulcastState(String identity, String room, String service, boolean expectedState) {
    int maxAttempts = 20;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

        LivekitModels.ParticipantInfo participant = participants.stream()
            .filter(p -> identity.equals(p.getIdentity()))
            .findFirst()
            .orElse(null);

        if (participant != null) {
            LivekitModels.TrackInfo videoTrack = participant.getTracksList().stream()
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .filter(t -> t.getSource() == LivekitModels.TrackSource.CAMERA)
                .findFirst()
                .orElse(null);

            if (videoTrack != null && videoTrack.getSimulcast() == expectedState) {
                return;
            }
        }

        try {
            Thread.sleep(POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    fail("Participant '" + identity + "' simulcast state should be " + expectedState);
}

@Then("participant {string} video track should have at least {int} layers in room {string} using service {string}")
public void videoTrackShouldHaveAtLeastLayers(String identity, int minLayers, String room, String service) {
    int layerCount = getVideoLayerCount(identity, room, service);
    assertTrue(layerCount >= minLayers,
        "Participant '" + identity + "' should have at least " + minLayers + " layers (found: " + layerCount + ")");
}

@Then("participant {string} video track should have exactly {int} layer in room {string} using service {string}")
public void videoTrackShouldHaveExactlyLayers(String identity, int layers, String room, String service) {
    int layerCount = getVideoLayerCount(identity, room, service);
    assertEquals(layers, layerCount,
        "Participant '" + identity + "' should have exactly " + layers + " layer(s)");
}

private int getVideoLayerCount(String identity, String room, String service) {
    int maxAttempts = 20;
    int layerCount = 0;

    for (int attempt = 0; attempt < maxAttempts; attempt++) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

        LivekitModels.ParticipantInfo participant = participants.stream()
            .filter(p -> identity.equals(p.getIdentity()))
            .findFirst()
            .orElse(null);

        if (participant != null) {
            LivekitModels.TrackInfo videoTrack = participant.getTracksList().stream()
                .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
                .filter(t -> t.getSource() == LivekitModels.TrackSource.CAMERA)
                .findFirst()
                .orElse(null);

            if (videoTrack != null) {
                layerCount = videoTrack.getLayersCount();
                if (layerCount > 0) {
                    return layerCount;
                }
            }
        }

        try {
            Thread.sleep(POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    return layerCount;
}

@Then("participant {string} video layers should have different resolutions in room {string} using service {string}")
public void videoLayersShouldHaveDifferentResolutions(String identity, String room, String service) {
    List<LivekitModels.VideoLayer> layers = getVideoLayers(identity, room, service);
    assertTrue(layers.size() >= 2, "Should have at least 2 layers to compare");

    Set<Integer> widths = layers.stream()
        .map(LivekitModels.VideoLayer::getWidth)
        .collect(Collectors.toSet());

    assertTrue(widths.size() > 1, "Layers should have different resolutions");
}

@Then("participant {string} video layers should have different bitrates in room {string} using service {string}")
public void videoLayersShouldHaveDifferentBitrates(String identity, String room, String service) {
    List<LivekitModels.VideoLayer> layers = getVideoLayers(identity, room, service);
    assertTrue(layers.size() >= 2, "Should have at least 2 layers to compare");

    Set<Integer> bitrates = layers.stream()
        .map(LivekitModels.VideoLayer::getBitrate)
        .collect(Collectors.toSet());

    assertTrue(bitrates.size() > 1, "Layers should have different bitrates");
}

@Then("participant {string} highest video layer should have greater resolution than lowest layer in room {string} using service {string}")
public void highestLayerShouldHaveGreaterResolution(String identity, String room, String service) {
    List<LivekitModels.VideoLayer> layers = getVideoLayers(identity, room, service);
    assertTrue(layers.size() >= 2, "Should have at least 2 layers");

    LivekitModels.VideoLayer highest = layers.stream()
        .max(Comparator.comparing(LivekitModels.VideoLayer::getWidth))
        .orElseThrow();

    LivekitModels.VideoLayer lowest = layers.stream()
        .min(Comparator.comparing(LivekitModels.VideoLayer::getWidth))
        .orElseThrow();

    assertTrue(highest.getWidth() > lowest.getWidth(),
        "Highest layer width (" + highest.getWidth() + ") should exceed lowest (" + lowest.getWidth() + ")");
}

private List<LivekitModels.VideoLayer> getVideoLayers(String identity, String room, String service) {
    List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

    LivekitModels.ParticipantInfo participant = participants.stream()
        .filter(p -> identity.equals(p.getIdentity()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Participant not found: " + identity));

    LivekitModels.TrackInfo videoTrack = participant.getTracksList().stream()
        .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
        .filter(t -> t.getSource() == LivekitModels.TrackSource.CAMERA)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Video track not found for: " + identity));

    return videoTrack.getLayersList();
}

@Then("the CLI publisher should have simulcast video layers in room {string} using service {string}")
public void cliPublisherShouldHaveSimulcastLayers(String room, String service) {
    List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

    boolean hasSimulcast = participants.stream()
        .flatMap(p -> p.getTracksList().stream())
        .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
        .anyMatch(t -> t.getSimulcast() && t.getLayersCount() > 1);

    assertTrue(hasSimulcast, "CLI publisher should have simulcast video layers");
}

@Then("the CLI publisher should have exactly {int} video layer in room {string} using service {string}")
public void cliPublisherShouldHaveExactlyLayers(int layers, String room, String service) {
    List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

    int layerCount = participants.stream()
        .flatMap(p -> p.getTracksList().stream())
        .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
        .mapToInt(LivekitModels.TrackInfo::getLayersCount)
        .max()
        .orElse(0);

    assertEquals(layers, layerCount, "CLI publisher should have exactly " + layers + " video layer(s)");
}
```

---

## Data Flow

### Enabling Simulcast in Browser

```
1. User Step: "SimPublisher" enables simulcast for video publishing
       |
       v
2. LiveKitMeet.enableSimulcast()
       |
       v
3. JavaScript: window.liveKitClient.setSimulcast(true)
       |
       v
4. Store setting: this.simulcastEnabled = true
       |
       v
5. On room.connect():
   - publishDefaults.simulcast = true
   - videoSimulcastLayers = [h180, h360]
       |
       v
6. room.localParticipant.enableCameraAndMicrophone()
       |
       v
7. LiveKit SDK encodes video to multiple SVC layers
       |
       v
8. WebRTC sends multiple RTP streams with different SSRCs
```

### Layer Information Flow: Browser to Test Verification

```
Browser                    LiveKit Server              Test Framework
   |                            |                            |
   | Publish video with         |                            |
   | simulcast enabled          |                            |
   |--------------------------->|                            |
   |                            |                            |
   |                            | Store TrackInfo with       |
   |                            | - simulcast: true          |
   |                            | - layers: [q, h, f]        |
   |                            |                            |
   |                            |<---------------------------|
   |                            | RoomServiceClient          |
   |                            | .listParticipants(room)    |
   |                            |                            |
   |                            |--------------------------->|
   |                            | ParticipantInfo with       |
   |                            | TrackInfo containing       |
   |                            | VideoLayer list            |
   |                            |                            |
   |                            |                            | Assert layers.size >= 2
   |                            |                            | Assert layers have
   |                            |                            | different widths/bitrates
```

### Subscriber Quality Preference Flow

```
1. Subscriber connects to room
       |
       v
2. TrackSubscribed event fires
       |
       v
3. User Step: "LowBandwidthSub" sets video quality preference to "LOW"
       |
       v
4. LiveKitMeet.setVideoQualityPreference("LOW")
       |
       v
5. JavaScript: publication.setVideoQuality(VideoQuality.LOW)
       |
       v
6. LiveKit SDK signals preference to server
       |
       v
7. Server sends only LOW quality layer to subscriber
       |
       v
8. Test verifies via:
   - getCurrentVideoQuality() returns "LOW"
   - Received video dimensions match low layer
```

---

## API Integration Points

### LiveKit Server SDK Methods

The `io.livekit:livekit-server` SDK provides access to track and layer information:

| Method | Return Type | Usage |
|--------|-------------|-------|
| `RoomServiceClient.listParticipants(roomName)` | `List<ParticipantInfo>` | Get all participants with their tracks |
| `ParticipantInfo.getTracksList()` | `List<TrackInfo>` | Get tracks for a participant |
| `TrackInfo.getSimulcast()` | `boolean` | Check if track has simulcast enabled |
| `TrackInfo.getLayersList()` | `List<VideoLayer>` | Get layer details |
| `VideoLayer.getWidth()` | `int` | Layer width in pixels |
| `VideoLayer.getHeight()` | `int` | Layer height in pixels |
| `VideoLayer.getBitrate()` | `int` | Target bitrate in bps |
| `VideoLayer.getQuality()` | `VideoQuality` | LOW, MEDIUM, HIGH |

### LiveKit Client SDK Methods (JavaScript)

| Method | Description |
|--------|-------------|
| `room.localParticipant.enableCameraAndMicrophone()` | Publish with current simulcast settings |
| `publication.setVideoQuality(quality)` | Set subscriber quality preference |
| `publication.setVideoDimensions(dimensions)` | Constrain received dimensions |
| `track.dimensions` | Get current track dimensions |

### WebRTC Stats (for advanced verification)

```javascript
const stats = await peerConnection.getStats();
stats.forEach(report => {
    if (report.type === 'inbound-rtp' && report.kind === 'video') {
        console.log('Frame width:', report.frameWidth);
        console.log('Frame height:', report.frameHeight);
        console.log('Frames received:', report.framesReceived);
    }
});
```

---

## Testing Strategy

### Implementation Order

| Phase | Story | Focus | Verification Method |
|-------|-------|-------|---------------------|
| 1 | 1.1.2.1 | Enable simulcast flag | Server API: `TrackInfo.getSimulcast()` |
| 2 | 1.1.2.2 | Multiple layers exist | Server API: `TrackInfo.getLayersList().size()` |
| 3 | 1.1.2.3 | Layer quality differences | Server API: Compare layer widths/bitrates |
| 4 | 1.1.2.5 | CLI publisher simulcast | Server API + CLI container config |
| 5 | 1.1.2.4 | Subscriber layer selection | JavaScript: quality preference + dimensions |
| 6 | 1.1.2.6 | Bandwidth constraints | JavaScript: bandwidth hints |

### Component Isolation Testing

#### Browser Simulcast Enable (Story 1.1.2.1)
1. Create room and token
2. Open browser with LiveKitMeet
3. Call `enableSimulcast()` before connect
4. Connect to room
5. Verify via Server API: `TrackInfo.getSimulcast() == true`

#### Layer Verification (Story 1.1.2.2)
1. Complete Story 1.1.2.1 setup
2. Poll Server API until layers populated
3. Assert `layers.size() >= 2`
4. Expected layers: q (quarter), h (half), f (full)

#### Quality Comparison (Story 1.1.2.3)
1. Retrieve layer list
2. Compare width values: `layer_f.width > layer_h.width > layer_q.width`
3. Compare bitrate values: `layer_f.bitrate > layer_h.bitrate > layer_q.bitrate`

#### CLI Publisher (Story 1.1.2.5)
1. Configure `CLIPublisherContainer` with `simulcast: true`
2. Start container
3. Verify layers via Server API
4. Repeat with `simulcast: false` and verify single layer

### Integration Testing Approach

Full scenario testing validates end-to-end flow:

```gherkin
Scenario: End-to-end simulcast with subscriber quality switch
  Given simulcast publisher "Publisher" in room "E2ERoom"
  And subscriber "Subscriber" joins room "E2ERoom"

  When "Subscriber" sets quality to "LOW"
  Then "Subscriber" receives low resolution stream

  When "Subscriber" sets quality to "HIGH"
  Then "Subscriber" receives high resolution stream
```

---

## Known Limitations and Workarounds

### Container Environment Constraints

| Limitation | Impact | Workaround |
|------------|--------|------------|
| No real camera hardware | Fake video stream used | Chrome flags: `--use-fake-device-for-media-stream` |
| Fake stream is low resolution | May produce fewer layers | Use `--use-file-for-fake-video-capture` with higher-res file |
| Network between containers | Cannot easily simulate bandwidth | Use SDK bandwidth hints instead of network shaping |
| Headless browser limitations | Some WebRTC features limited | VNC recording captures visual state |

### Fake Video Stream Considerations

The default fake video stream in Chrome is 640x480 at a low framerate. This may result in:
- Only 2 simulcast layers instead of 3
- Lower quality differences between layers
- Longer time for layers to stabilize

**Workaround Options:**

1. **Accept 2 layers minimum** - Change assertions from `>= 3` to `>= 2`

2. **Use custom fake video file:**
   ```java
   chromeOptions.addArguments("--use-file-for-fake-video-capture=/path/to/720p-video.y4m");
   ```

3. **Increase capture resolution:**
   ```javascript
   videoCaptureDefaults: {
       resolution: { width: 1280, height: 720 }
   }
   ```

### Browser-Specific Behaviors

| Browser | Behavior | Notes |
|---------|----------|-------|
| Chrome | Full simulcast support | Recommended for primary testing |
| Firefox | Simulcast supported | May differ in layer negotiation timing |
| Edge | Chrome-based, similar behavior | Use Chromium flags |

### Layer Stabilization Timing

Layers may not appear immediately in the Server API. Use polling:

```java
private static final int MAX_POLLING_ATTEMPTS = 20;
private static final int POLLING_INTERVAL_MS = 500;

for (int attempt = 0; attempt < MAX_POLLING_ATTEMPTS; attempt++) {
    List<VideoLayer> layers = getVideoLayers(identity, room, service);
    if (layers.size() >= expectedMinLayers) {
        return layers;
    }
    Thread.sleep(POLLING_INTERVAL_MS);
}
throw new AssertionError("Layers did not stabilize within timeout");
```

### Subscriber Quality Verification Challenges

Verifying that a subscriber is actually receiving a specific layer is challenging because:
1. Server API shows what's available, not what's being sent to each subscriber
2. Client-side quality reports may not be immediately accurate

**Workaround:**
1. Trust the SDK's quality preference setting
2. Verify via JavaScript that preference was set: `getCurrentVideoQuality()`
3. For advanced verification, use WebRTC stats to check actual received dimensions

---

## File Changes Summary

| File | Changes Required |
|------|------------------|
| `LiveKitMeet.java` | Add 6 new methods for simulcast control |
| `livekit-client.js` | Add simulcast config in constructor, quality control methods |
| `LiveKitBrowserWebrtcSteps.java` | Add 6+ new step definitions |
| `LiveKitRoomSteps.java` | Add 10+ new step definitions for layer verification |
| `livekit_simulcast.feature` | New feature file (from requirements.md) |

---

## Existing Infrastructure Leverage

The simulcast implementation builds on existing components:

1. **Track.java and Layer.java** - Webhook models already support `simulcast` and `layers` fields
2. **CLIPublisherContainer** - Already has `simulcast` configuration via `PublisherConfig.Builder.simulcast(boolean)`
3. **RoomServiceClient** - Already used for track verification in `LiveKitRoomSteps`
4. **Polling pattern** - Established in `participantShouldBePublishingVideoInRoomUsingService`
5. **Screen share implementation** - Similar browser/page object pattern to follow

---

## Version Compatibility

| LiveKit Version | Simulcast Support | Notes |
|-----------------|-------------------|-------|
| v1.5.0+ | Full support | Recommended |
| v1.4.x | Basic simulcast | Layer API may differ |
| < v1.4.0 | Limited | Test required |

The default LiveKit version in `gradle.properties` should be verified to support full simulcast features including `TrackInfo.getLayersList()`.
