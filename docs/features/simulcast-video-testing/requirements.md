# Simulcast Video Testing - Requirements Document

## Epic Description

**Epic:** Test Simulcast Video Publishing
**Story ID:** 1.1.2
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test simulcast video publishing
**So that** I can verify multiple quality layers are published

---

## Story Breakdown

The original story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.2.1: Enable Simulcast in Browser Video Publish

**Size:** S (Small)

**As a** test developer
**I want** to enable simulcast when publishing video from a browser
**So that** multiple quality layers are generated for subscribers

**Acceptance Criteria:**

**Given** a participant joins a room with video publish permission
**When** they publish video with simulcast enabled
**Then** the video track should be published with simulcast active

**Given** simulcast is enabled in the browser client
**When** the participant's track info is inspected via server API
**Then** the track should indicate simulcast is enabled

- [x] Add browser-side simulcast enable/disable capability
- [x] Verify simulcast flag is accessible via LiveKitMeet page object
- [x] Test default simulcast behavior (enabled/disabled)
- [x] Verify simulcast works across Chrome, Firefox browsers

**Dependencies:** None (builds on existing WebRTC publishing)

---

### Story 1.1.2.2: Verify Multiple Video Layers Exist

**Size:** M (Medium)

**As a** test developer
**I want** to verify that multiple video layers are published
**So that** I can confirm simulcast is functioning correctly

**Acceptance Criteria:**

**Given** simulcast is enabled for a video track
**When** the track layers are inspected via server API
**Then** multiple layers should exist (typically 3: low, medium, high)

**Given** simulcast is disabled for a video track
**When** the track layers are inspected
**Then** only a single layer should exist

**Given** multiple quality layers exist
**When** inspecting each layer
**Then** each layer should have distinct width, height, and bitrate values

- [x] Add step definitions for layer count verification
- [x] Implement layer inspection via RoomServiceClient/TrackInfo
- [x] Verify layer count changes based on simulcast setting
- [x] Handle edge cases where fewer layers are published (low resolution source)

**Dependencies:** Story 1.1.2.1

---

### Story 1.1.2.3: Measure Quality Differences Between Layers

**Size:** S (Small)

**As a** test developer
**I want** to measure the quality differences between simulcast layers
**So that** I can verify layers represent different quality levels

**Acceptance Criteria:**

**Given** a track has multiple simulcast layers
**When** layer properties are compared
**Then** higher quality layers should have greater width and height

**Given** a track has multiple simulcast layers
**When** bitrate values are compared
**Then** higher quality layers should have greater bitrate allocation

**Given** layer quality values are retrieved
**When** compared against expected ratios
**Then** layers should follow expected scaling (e.g., high = 4x low resolution)

- [x] Add step definitions for layer property comparison
- [x] Verify resolution scaling between layers
- [x] Verify bitrate differences between layers
- [ ] Document expected layer ratios for different source resolutions

**Dependencies:** Story 1.1.2.2

---

### Story 1.1.2.4: Test Layer Selection via SDK

**Size:** M (Medium)

**As a** test developer
**I want** to test subscriber layer selection
**So that** I can verify subscribers can request specific quality levels

**Acceptance Criteria:**

**Given** a publisher is streaming with simulcast enabled
**When** a subscriber sets video quality preference to LOW
**Then** the subscriber should receive the low quality layer

**Given** a subscriber is receiving a specific layer
**When** they change quality preference to HIGH
**Then** the subscriber should switch to the high quality layer

**Given** a subscriber requests a layer that does not exist
**When** the request is processed
**Then** the closest available layer should be selected

- [x] Add browser-side layer preference setting
- [x] Implement subscriber quality preference step definitions
- [ ] Verify layer changes via server API subscription info
- [ ] Test layer switching latency

**Dependencies:** Story 1.1.2.2

---

### Story 1.1.2.5: Test Simulcast with CLI Publisher

**Size:** S (Small)

**As a** test developer
**I want** to test simulcast using the CLI publisher
**So that** I can verify simulcast in load testing scenarios

**Acceptance Criteria:**

**Given** CLI publisher is configured with simulcast enabled
**When** it publishes video to a room
**Then** multiple quality layers should be available

**Given** CLI publisher is configured with simulcast disabled
**When** it publishes video to a room
**Then** only a single quality layer should exist

**Given** multiple CLI publishers with simulcast are in a room
**When** a browser subscriber joins
**Then** they should be able to see simulcast tracks from CLI publishers

- [ ] Verify CLI publisher simulcast config works correctly
- [ ] Compare layer counts between simulcast:true and simulcast:false
- [ ] Test browser-CLI interoperability for simulcast

Note: CLI publisher simulcast scenarios are defined in requirements but not yet implemented in the feature file.

**Dependencies:** Story 1.1.2.2

---

### Story 1.1.2.6: Bandwidth Constraint Layer Selection

**Size:** M (Medium)

**As a** test developer
**I want** to verify layer selection under bandwidth constraints
**So that** I can ensure adaptive streaming works correctly

**Acceptance Criteria:**

**Given** simulcast is enabled and bandwidth is constrained
**When** adaptive streaming activates
**Then** a lower quality layer should be selected

**Given** a subscriber has limited bandwidth
**When** they subscribe to a simulcast track
**Then** the server should send an appropriate layer

**Given** bandwidth recovers after constraint
**When** the subscriber is still connected
**Then** higher quality layers should become available

Note: Full network simulation is out of scope. This story focuses on verifiable layer selection behavior that can be tested without external traffic control tools.

- [x] Test layer selection with explicit subscriber bandwidth hints
- [x] Verify layer preference is respected by server
- [ ] Document limitations of container-based bandwidth testing

**Dependencies:** Story 1.1.2.4

---

## Gherkin Scenarios

### Feature File: `livekit_simulcast.feature`

```gherkin
Feature: LiveKit Simulcast Video Publishing
  As a test developer
  I want to test simulcast video publishing
  So that I can verify multiple quality layers are published

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.2.1: Enable Simulcast in Browser Video Publish
  Scenario Outline: Participant publishes video with simulcast enabled
    Given an access token is created with identity "SimPublisher" and room "SimRoom" with grants "canPublish:true,canSubscribe:true"
    And room "SimRoom" is created using service "livekit1"

    When "SimPublisher" opens a <browser> browser with LiveKit Meet page
    And "SimPublisher" enables simulcast for video publishing
    And "SimPublisher" connects to room "SimRoom" using the access token
    And connection is established successfully for "SimPublisher"

    Then participant "SimPublisher" should be publishing video in room "SimRoom" using service "livekit1"
    And participant "SimPublisher" should have simulcast enabled for video in room "SimRoom" using service "livekit1"

    Examples:
      | browser  |
      | "Chrome" |
      | "Edge"   |

  Scenario: Participant publishes video with simulcast disabled
    Given an access token is created with identity "NoSimPublisher" and room "NoSimRoom" with grants "canPublish:true,canSubscribe:true"
    And room "NoSimRoom" is created using service "livekit1"

    When "NoSimPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "NoSimPublisher" disables simulcast for video publishing
    And "NoSimPublisher" connects to room "NoSimRoom" using the access token
    And connection is established successfully for "NoSimPublisher"

    Then participant "NoSimPublisher" should be publishing video in room "NoSimRoom" using service "livekit1"
    And participant "NoSimPublisher" should have simulcast disabled for video in room "NoSimRoom" using service "livekit1"

  # Story 1.1.2.2: Verify Multiple Video Layers Exist
  Scenario: Simulcast track has multiple video layers
    Given an access token is created with identity "LayerPublisher" and room "LayerRoom" with grants "canPublish:true,canSubscribe:true"
    And room "LayerRoom" is created using service "livekit1"

    When "LayerPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "LayerPublisher" enables simulcast for video publishing
    And "LayerPublisher" connects to room "LayerRoom" using the access token
    And connection is established successfully for "LayerPublisher"

    Then participant "LayerPublisher" should be publishing video in room "LayerRoom" using service "livekit1"
    And participant "LayerPublisher" video track should have at least 2 layers in room "LayerRoom" using service "livekit1"

  Scenario: Non-simulcast track has single video layer
    Given an access token is created with identity "SingleLayerPub" and room "SingleLayerRoom" with grants "canPublish:true,canSubscribe:true"
    And room "SingleLayerRoom" is created using service "livekit1"

    When "SingleLayerPub" opens a "Chrome" browser with LiveKit Meet page
    And "SingleLayerPub" disables simulcast for video publishing
    And "SingleLayerPub" connects to room "SingleLayerRoom" using the access token
    And connection is established successfully for "SingleLayerPub"

    Then participant "SingleLayerPub" should be publishing video in room "SingleLayerRoom" using service "livekit1"
    And participant "SingleLayerPub" video track should have exactly 1 layer in room "SingleLayerRoom" using service "livekit1"

  # Story 1.1.2.3: Measure Quality Differences Between Layers
  Scenario: Simulcast layers have different resolutions
    Given an access token is created with identity "QualityPub" and room "QualityRoom" with grants "canPublish:true,canSubscribe:true"
    And room "QualityRoom" is created using service "livekit1"

    When "QualityPub" opens a "Chrome" browser with LiveKit Meet page
    And "QualityPub" enables simulcast for video publishing
    And "QualityPub" connects to room "QualityRoom" using the access token
    And connection is established successfully for "QualityPub"

    Then participant "QualityPub" video track should have at least 2 layers in room "QualityRoom" using service "livekit1"
    And participant "QualityPub" video layers should have different resolutions in room "QualityRoom" using service "livekit1"
    And participant "QualityPub" highest video layer should have greater resolution than lowest layer in room "QualityRoom" using service "livekit1"

  Scenario: Simulcast layers have different bitrates
    Given an access token is created with identity "BitratePub" and room "BitrateRoom" with grants "canPublish:true,canSubscribe:true"
    And room "BitrateRoom" is created using service "livekit1"

    When "BitratePub" opens a "Chrome" browser with LiveKit Meet page
    And "BitratePub" enables simulcast for video publishing
    And "BitratePub" connects to room "BitrateRoom" using the access token
    And connection is established successfully for "BitratePub"

    Then participant "BitratePub" video track should have at least 2 layers in room "BitrateRoom" using service "livekit1"
    And participant "BitratePub" video layers should have different bitrates in room "BitrateRoom" using service "livekit1"

  # Story 1.1.2.4: Test Layer Selection via SDK
  Scenario: Subscriber can set video quality preference to low
    Given an access token is created with identity "HighResPub" and room "PrefRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "LowBandwidthSub" and room "PrefRoom" with grants "canPublish:false,canSubscribe:true"
    And room "PrefRoom" is created using service "livekit1"

    When "HighResPub" opens a "Chrome" browser with LiveKit Meet page
    And "HighResPub" enables simulcast for video publishing
    And "HighResPub" connects to room "PrefRoom" using the access token
    And connection is established successfully for "HighResPub"

    Then participant "HighResPub" video track should have at least 2 layers in room "PrefRoom" using service "livekit1"

    When "LowBandwidthSub" opens a "Chrome" browser with LiveKit Meet page
    And "LowBandwidthSub" connects to room "PrefRoom" using the access token
    And connection is established successfully for "LowBandwidthSub"
    And "LowBandwidthSub" sets video quality preference to "LOW"

    Then "LowBandwidthSub" should be receiving low quality video from "HighResPub" in room "PrefRoom" using service "livekit1"

  Scenario: Subscriber can switch video quality preference
    Given an access token is created with identity "Publisher" and room "SwitchRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Subscriber" and room "SwitchRoom" with grants "canPublish:false,canSubscribe:true"
    And room "SwitchRoom" is created using service "livekit1"

    When "Publisher" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher" enables simulcast for video publishing
    And "Publisher" connects to room "SwitchRoom" using the access token
    And connection is established successfully for "Publisher"

    When "Subscriber" opens a "Chrome" browser with LiveKit Meet page
    And "Subscriber" connects to room "SwitchRoom" using the access token
    And connection is established successfully for "Subscriber"
    And "Subscriber" sets video quality preference to "LOW"

    Then "Subscriber" should be receiving low quality video from "Publisher" in room "SwitchRoom" using service "livekit1"

    When "Subscriber" sets video quality preference to "HIGH"

    Then "Subscriber" should be receiving high quality video from "Publisher" in room "SwitchRoom" using service "livekit1"

  # Story 1.1.2.5: Test Simulcast with CLI Publisher
  Scenario: CLI publisher with simulcast enabled has multiple layers
    Given room "CLISimRoom" is created using service "livekit1"
    When a CLI load test with config "videoPublishers:1,simulcast:true" connects to room "CLISimRoom" using service "livekit1"
    Then room "CLISimRoom" should have 1 active participants in service "livekit1"
    And the CLI publisher should have simulcast video layers in room "CLISimRoom" using service "livekit1"

  Scenario: CLI publisher with simulcast disabled has single layer
    Given room "CLINoSimRoom" is created using service "livekit1"
    When a CLI load test with config "videoPublishers:1,simulcast:false" connects to room "CLINoSimRoom" using service "livekit1"
    Then room "CLINoSimRoom" should have 1 active participants in service "livekit1"
    And the CLI publisher should have exactly 1 video layer in room "CLINoSimRoom" using service "livekit1"

  Scenario: Browser subscriber views CLI publisher simulcast stream
    Given an access token is created with identity "BrowserViewer" and room "InteropRoom" with grants "canPublish:false,canSubscribe:true"
    And room "InteropRoom" is created using service "livekit1"
    When a CLI load test with config "videoPublishers:1,simulcast:true" connects to room "InteropRoom" using service "livekit1"

    Then room "InteropRoom" should have 1 active participants in service "livekit1"
    And the CLI publisher should have simulcast video layers in room "InteropRoom" using service "livekit1"

    When "BrowserViewer" opens a "Chrome" browser with LiveKit Meet page
    And "BrowserViewer" connects to room "InteropRoom" using the access token
    And connection is established successfully for "BrowserViewer"

    Then room "InteropRoom" should have 2 active participants in service "livekit1"
    And participant "BrowserViewer" should see 1 remote video tracks in room "InteropRoom" using service "livekit1"

  # Story 1.1.2.6: Bandwidth Constraint Layer Selection
  Scenario: Subscriber with bandwidth hint receives appropriate layer
    Given an access token is created with identity "FullResPub" and room "BWRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "ConstrainedSub" and room "BWRoom" with grants "canPublish:false,canSubscribe:true"
    And room "BWRoom" is created using service "livekit1"

    When "FullResPub" opens a "Chrome" browser with LiveKit Meet page
    And "FullResPub" enables simulcast for video publishing
    And "FullResPub" connects to room "BWRoom" using the access token
    And connection is established successfully for "FullResPub"

    Then participant "FullResPub" video track should have at least 2 layers in room "BWRoom" using service "livekit1"

    When "ConstrainedSub" opens a "Chrome" browser with LiveKit Meet page
    And "ConstrainedSub" connects to room "BWRoom" using the access token
    And connection is established successfully for "ConstrainedSub"
    And "ConstrainedSub" sets maximum receive bandwidth to 100 kbps

    Then "ConstrainedSub" should be receiving a lower quality layer from "FullResPub" in room "BWRoom" using service "livekit1"

  Scenario: Multiple subscribers with different quality preferences
    Given an access token is created with identity "MultiPub" and room "MultiSubRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "HighQualitySub" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "LowQualitySub" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiSubRoom" is created using service "livekit1"

    When "MultiPub" opens a "Chrome" browser with LiveKit Meet page
    And "MultiPub" enables simulcast for video publishing
    And "MultiPub" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "MultiPub"

    When "HighQualitySub" opens a "Chrome" browser with LiveKit Meet page
    And "HighQualitySub" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "HighQualitySub"
    And "HighQualitySub" sets video quality preference to "HIGH"

    When "LowQualitySub" opens a "Firefox" browser with LiveKit Meet page
    And "LowQualitySub" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "LowQualitySub"
    And "LowQualitySub" sets video quality preference to "LOW"

    Then "HighQualitySub" should be receiving high quality video from "MultiPub" in room "MultiSubRoom" using service "livekit1"
    And "LowQualitySub" should be receiving low quality video from "MultiPub" in room "MultiSubRoom" using service "livekit1"
```

---

## Definition of Done

### Code Implementation
- [x] New step definitions added to `LiveKitBrowserWebrtcSteps.java` for simulcast control
- [x] New step definitions added to `LiveKitRoomSteps.java` for layer verification
- [x] `LiveKitMeet.java` extended with simulcast enable/disable and quality preference methods
- [x] Feature file `livekit_simulcast.feature` created and passing
- [x] All scenarios pass on Chrome browser
- [x] All scenarios pass on Firefox browser
- [ ] All scenarios pass on Edge browser

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass
- [x] Tests pass against default LiveKit version
- [x] Manual verification of simulcast layers in containerized browser
- [ ] CLI publisher simulcast scenarios verified

### Documentation
- [x] Feature documentation complete in `docs/features/simulcast-video-testing/`
- [ ] Step definitions documented in `docs/features.md`
- [ ] Technical notes added for simulcast configuration

### Code Quality
- [x] No new Lombok violations
- [x] No code comments added (per project guidelines)
- [x] Cross-platform path handling maintained
- [x] Proper cleanup in After hooks

---

## Technical Components

### New Step Definitions Required

#### In `LiveKitBrowserWebrtcSteps.java`:
```java
@When("{string} enables simulcast for video publishing")
public void enablesSimulcastForVideoPublishing(String participantName)

@When("{string} disables simulcast for video publishing")
public void disablesSimulcastForVideoPublishing(String participantName)

@When("{string} sets video quality preference to {string}")
public void setsVideoQualityPreferenceTo(String participantName, String quality)

@When("{string} sets maximum receive bandwidth to {int} kbps")
public void setsMaximumReceiveBandwidth(String participantName, int kbps)

@Then("{string} should be receiving low quality video from {string} in room {string} using service {string}")
public void shouldBeReceivingLowQualityVideoFrom(String subscriber, String publisher, String room, String service)

@Then("{string} should be receiving high quality video from {string} in room {string} using service {string}")
public void shouldBeReceivingHighQualityVideoFrom(String subscriber, String publisher, String room, String service)

@Then("{string} should be receiving a lower quality layer from {string} in room {string} using service {string}")
public void shouldBeReceivingLowerQualityLayerFrom(String subscriber, String publisher, String room, String service)
```

#### In `LiveKitRoomSteps.java`:
```java
@Then("participant {string} should have simulcast enabled for video in room {string} using service {string}")
public void participantShouldHaveSimulcastEnabled(String identity, String room, String service)

@Then("participant {string} should have simulcast disabled for video in room {string} using service {string}")
public void participantShouldHaveSimulcastDisabled(String identity, String room, String service)

@Then("participant {string} video track should have at least {int} layers in room {string} using service {string}")
public void videoTrackShouldHaveAtLeastLayers(String identity, int minLayers, String room, String service)

@Then("participant {string} video track should have exactly {int} layer in room {string} using service {string}")
public void videoTrackShouldHaveExactlyLayers(String identity, int layers, String room, String service)

@Then("participant {string} video layers should have different resolutions in room {string} using service {string}")
public void videoLayersShouldHaveDifferentResolutions(String identity, String room, String service)

@Then("participant {string} video layers should have different bitrates in room {string} using service {string}")
public void videoLayersShouldHaveDifferentBitrates(String identity, String room, String service)

@Then("participant {string} highest video layer should have greater resolution than lowest layer in room {string} using service {string}")
public void highestLayerShouldHaveGreaterResolution(String identity, String room, String service)

@Then("the CLI publisher should have simulcast video layers in room {string} using service {string}")
public void cliPublisherShouldHaveSimulcastLayers(String room, String service)

@Then("the CLI publisher should have exactly {int} video layer in room {string} using service {string}")
public void cliPublisherShouldHaveExactlyLayers(int layers, String room, String service)
```

### LiveKitMeet Page Object Extensions

```java
public void enableSimulcast()
public void disableSimulcast()
public boolean isSimulcastEnabled()
public void setVideoQualityPreference(VideoQuality quality)
public void setMaxReceiveBandwidth(int kbps)
public VideoQuality getCurrentQualityPreference()
```

### Web Application Changes (meet.html)

The LiveKit Meet web application needs:
- Simulcast toggle in the settings/join form
- JavaScript to configure simulcast in publish options
- Quality preference selector for subscribers
- Bandwidth limit configuration
- State tracking for current simulcast/quality settings

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Fake video stream may not generate sufficient resolution for all 3 layers | High | Medium | Test with higher resolution fake stream; accept 2 layers as minimum |
| Layer information may not be immediately available via API | Medium | Medium | Implement polling with timeout for layer stabilization |
| Browser simulcast behavior differs across browsers | Medium | Low | Test on multiple browsers; document browser-specific behavior |
| Server API may not expose all layer details | Medium | Low | Use webhook events as fallback for layer information |
| Quality preference changes may not take effect immediately | Low | Medium | Add wait/polling for preference changes to propagate |

### Mitigation Strategies

1. **Resolution Requirements**: Configure fake video stream with 720p or higher to ensure simulcast generates multiple layers

2. **Layer Stabilization**: Add polling logic similar to existing track verification patterns (20 attempts, 500ms interval)

3. **Browser Compatibility**: Focus on Chrome as primary; document any browser-specific limitations

4. **API Fallback**: If track layer info is limited, use the existing `Layer.java` webhook model as alternative data source

5. **Timing Considerations**: Add configurable wait times for quality preference changes to take effect

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.2.1 - Enable Simulcast | S | Foundation for all simulcast tests |
| 2 | 1.1.2.2 - Verify Multiple Layers | M | Core verification needed by other stories |
| 3 | 1.1.2.3 - Quality Differences | S | Builds on layer verification |
| 4 | 1.1.2.5 - CLI Publisher Simulcast | S | Verifies existing CLI functionality |
| 5 | 1.1.2.4 - Layer Selection | M | Requires publisher with layers working |
| 6 | 1.1.2.6 - Bandwidth Constraints | M | Advanced scenario depending on layer selection |

**Total Estimated Effort:** M (Medium) - approximately 3-5 days

---

## Appendix: LiveKit Simulcast Reference

### Video Quality Layers

| Layer | Typical Name | Resolution Ratio | Bitrate Ratio |
|-------|--------------|------------------|---------------|
| q (Low) | Quarter | 1/4 | ~1/8 |
| h (Medium) | Half | 1/2 | ~1/4 |
| f (High/Full) | Full | 1 | 1 |

### VideoQuality Enum (LiveKit SDK)

```typescript
enum VideoQuality {
  LOW = 0,
  MEDIUM = 1,
  HIGH = 2,
  OFF = 3
}
```

### Simulcast Configuration (Client SDK)

```typescript
const options: VideoCaptureOptions = {
  resolution: VideoPresets.h720,
  // Simulcast is typically enabled by default in modern SDK versions
};

const publishOptions: TrackPublishOptions = {
  simulcast: true,  // Explicitly enable
  videoSimulcastLayers: [
    VideoPresets.h180,
    VideoPresets.h360,
  ],
};
```

### TrackInfo Protobuf Layer Fields

```protobuf
message TrackInfo {
  // ... other fields ...
  repeated VideoLayer layers = 14;
  bool simulcast = 11;
}

message VideoLayer {
  VideoQuality quality = 1;
  uint32 width = 2;
  uint32 height = 3;
  uint32 bitrate = 4;
  uint32 ssrc = 5;
}
```

---

## Related Stories

- **Story 1.1.1** (Screen Sharing) - Completed; similar browser interaction patterns
- **Story 1.1.3** (Dynacast) - Future; depends on simulcast infrastructure
- **Story 4.2.2** (Resource Utilization Monitoring) - Future; could measure simulcast overhead
