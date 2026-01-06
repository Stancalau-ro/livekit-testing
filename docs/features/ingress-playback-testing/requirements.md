# Ingress Stream Playback Testing - Requirements Document

## Epic Description

**Epic:** Verify Ingress Stream Playback
**Story ID:** 1.1.9
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to verify that browser participants can view ingress streams
**So that** I can confirm ingress produces playable media in the room

---

## Story Breakdown

The feature is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.9.1: Single Subscriber Receives Ingress Video Track

**Size:** M (Medium)

**As a** test developer
**I want** to verify a browser subscriber receives video from an ingress stream
**So that** I can confirm ingress video is playable

**Acceptance Criteria:**

**Given** an RTMP ingress is streaming to a room
**When** a browser participant joins and subscribes
**Then** the participant receives the ingress video track

**Given** a browser is subscribed to ingress video
**When** the track info is inspected via server API
**Then** the track shows as subscribed for the browser participant

**Given** the browser is receiving ingress video
**When** remote tracks are counted
**Then** at least one remote video track is available

- [ ] Add step: `Then "{subscriber}" should receive video track from ingress "{identity}"`
- [ ] Implement polling for remote track availability (timeout: 45 seconds)
- [ ] Verify via RoomServiceClient that subscriber has subscribed tracks
- [ ] Test with Chrome browser
- [ ] Test with Firefox browser

**Dependencies:** Story 1.1.8 (Ingress Infrastructure)

---

### Story 1.1.9.2: Single Subscriber Receives Ingress Audio Track

**Size:** S (Small)

**As a** test developer
**I want** to verify a browser subscriber receives audio from an ingress stream
**So that** I can confirm ingress audio is playable

**Acceptance Criteria:**

**Given** an RTMP ingress is streaming audio to a room
**When** a browser participant subscribes
**Then** the participant receives the ingress audio track

**Given** a browser is subscribed to ingress audio
**When** the track count is checked
**Then** at least one remote audio track is available

**Given** the ingress stream includes audio
**When** the ingress participant's tracks are inspected
**Then** an audio track with MICROPHONE source is published

- [ ] Add step: `Then "{subscriber}" should receive audio track from ingress "{identity}"`
- [ ] Verify audio track exists via server API
- [ ] Test audio track subscription in browser

**Dependencies:** Story 1.1.9.1

---

### Story 1.1.9.3: Verify Ingress Video Dimensions Match Configuration

**Size:** M (Medium)

**As a** test developer
**I want** to verify ingress video dimensions match the transcoding configuration
**So that** I can confirm transcoding is working correctly

**Acceptance Criteria:**

**Given** an RTMP ingress is created with video preset "H264_720P_30FPS_3_LAYERS"
**When** video dimensions are checked via server API
**Then** the video track should have width 1280 and height 720

**Given** an RTMP stream is sent with resolution "1920x1080"
**When** using preset "H264_1080P_30FPS_3_LAYERS"
**Then** the output video dimensions should be 1920x1080

**Given** an ingress uses default configuration (no preset)
**When** video dimensions are checked
**Then** the dimensions should match the source stream resolution

- [ ] Add step: `Then ingress "{identity}" video track should have width {width} and height {height}`
- [ ] Retrieve video layer information from TrackInfo
- [ ] Handle cases where layer info is not immediately available (polling)
- [ ] Document expected dimensions for each preset

**Dependencies:** Story 1.1.9.1

---

### Story 1.1.9.4: Multiple Subscribers Receive Ingress Stream

**Size:** M (Medium)

**As a** test developer
**I want** to verify multiple browsers can simultaneously receive ingress streams
**So that** I can confirm broadcast scenarios work correctly

**Acceptance Criteria:**

**Given** an RTMP ingress is streaming to a room
**When** two browser participants join and subscribe
**Then** both participants receive the ingress video track

**Given** multiple subscribers are receiving the ingress stream
**When** room participant count is checked
**Then** the count should include the ingress participant plus all browser subscribers

**Given** subscribers are using different browsers (Chrome, Firefox)
**When** track reception is verified
**Then** both browsers successfully receive video

- [ ] Add scenario with 2 browser subscribers
- [ ] Test cross-browser compatibility (Chrome + Firefox)
- [ ] Verify room participant count includes ingress + subscribers
- [ ] Test with 3 subscribers as stretch goal

**Dependencies:** Story 1.1.9.1

---

### Story 1.1.9.5: Verify Ingress Track Metadata

**Size:** S (Small)

**As a** test developer
**I want** to verify ingress track metadata is correct
**So that** I can confirm track properties are properly set

**Acceptance Criteria:**

**Given** an RTMP ingress is publishing
**When** track metadata is inspected via server API
**Then** video track type should be VIDEO
**And** video track source should be CAMERA

**Given** an RTMP ingress is publishing audio
**When** audio track metadata is inspected
**Then** audio track type should be AUDIO
**And** audio track source should be MICROPHONE

**Given** ingress track codec is inspected
**When** using H264 preset
**Then** video codec should be H264

- [ ] Add step: `Then ingress "{identity}" video track should have source "CAMERA"`
- [ ] Add step: `Then ingress "{identity}" audio track should have source "MICROPHONE"`
- [ ] Verify track types via TrackInfo protobuf
- [ ] Verify track sources match expected values

**Dependencies:** Story 1.1.9.1

---

### Story 1.1.9.6: Subscriber Detects Ingress Track Removal

**Size:** S (Small)

**As a** test developer
**I want** to verify subscribers detect when ingress tracks are removed
**So that** I can confirm proper cleanup behavior

**Acceptance Criteria:**

**Given** a browser is subscribed to an ingress stream
**When** the RTMP stream stops
**Then** the subscriber should detect the track is no longer available

**Given** the ingress participant leaves the room
**When** the subscriber checks remote tracks
**Then** no remote video tracks from the ingress identity should exist

**Given** the ingress is deleted while a subscriber is connected
**When** the subscriber checks the room
**Then** the ingress participant should no longer be present

- [ ] Add step: `Then "{subscriber}" should detect ingress "{identity}" video track removal`
- [ ] Implement polling for track removal detection
- [ ] Test stream stop scenario
- [ ] Test ingress deletion scenario

**Dependencies:** Story 1.1.9.1, Story 1.1.8.7 (Ingress Lifecycle)

---

### Story 1.1.9.7: Ingress Simulcast Layers Available to Subscribers

**Size:** M (Medium)

**As a** test developer
**I want** to verify ingress simulcast layers are available to subscribers
**So that** I can confirm adaptive bitrate streaming from ingress works

**Acceptance Criteria:**

**Given** an RTMP ingress is created with preset "H264_720P_30FPS_3_LAYERS"
**When** video track layers are inspected
**Then** the track should have 3 simulcast layers

**Given** ingress video has multiple layers
**When** layer resolutions are compared
**Then** layers should have different dimensions (low, medium, high)

**Given** a subscriber sets quality preference to LOW
**When** receiving ingress video
**Then** the subscriber should receive a lower resolution layer

- [ ] Add step: `Then ingress "{identity}" video track should have {count} simulcast layers`
- [ ] Retrieve layer count from TrackInfo
- [ ] Verify layer dimensions differ
- [ ] Test subscriber quality preference with ingress (optional, complex)

**Dependencies:** Story 1.1.9.3, Story 1.1.2 (Simulcast Testing)

---

## Gherkin Scenarios

**IMPORTANT**: These scenarios are added to the existing `livekit_ingress.feature` file, NOT a separate feature file. This consolidates all ingress testing (infrastructure + playback) in one place, sharing the same Background setup.

### Add to Existing Feature File: `livekit_ingress.feature`

The existing `livekit_ingress.feature` already has:
- Background with Redis, LiveKit, and Ingress containers
- CRUD scenarios for ingress creation/deletion/listing
- Basic participant appearance verification

The following playback scenarios should be **appended** to the existing feature file:

```gherkin
  # ============================================================
  # PLAYBACK VERIFICATION SCENARIOS (Story 1.1.9)
  # ============================================================

  # Story 1.1.9.1 + 1.1.9.2: Subscriber receives video and audio from ingress
  Scenario Outline: Browser subscriber receives media from RTMP ingress
    Given room "PlaybackRoom" is created using service "livekit1"
    And "Streamer" creates an RTMP ingress to room "PlaybackRoom"
    And "Streamer" starts streaming via RTMP
    And the ingress for "Streamer" is publishing within 30 seconds
    And participant "Streamer" should appear in room "PlaybackRoom" using service "livekit1"
    And an access token is created with identity "Viewer" and room "PlaybackRoom" with grants "canPublish:false,canSubscribe:true"
    When "Viewer" opens a "<browser>" browser with LiveKit Meet page
    And "Viewer" connects to room "PlaybackRoom" using the access token
    And connection is established successfully for "Viewer"
    Then room "PlaybackRoom" should have 2 active participants in service "livekit1"
    And participant "Viewer" should have 1 remote video tracks available in room "PlaybackRoom" using service "livekit1"
    And participant "Streamer" should have published audio track in room "PlaybackRoom" using service "livekit1"

    Examples:
      | browser |
      | Chrome  |
      | Firefox |

  # Story 1.1.9.3: Verify ingress video dimensions
  Scenario: Ingress with 720p preset produces correct video dimensions and subscriber can receive
    Given room "DimensionRoom" is created using service "livekit1"
    And "DimStreamer" creates an RTMP ingress to room "DimensionRoom" with video preset "H264_720P_30FPS_3_LAYERS"
    And "DimStreamer" starts streaming via RTMP with resolution "1280x720"
    And the ingress for "DimStreamer" is publishing within 30 seconds
    And participant "DimStreamer" video track should have maximum width 1280 in room "DimensionRoom" using service "livekit1"
    And an access token is created with identity "DimViewer" and room "DimensionRoom" with grants "canPublish:false,canSubscribe:true"
    When "DimViewer" opens a "Chrome" browser with LiveKit Meet page
    And "DimViewer" connects to room "DimensionRoom" using the access token
    And connection is established successfully for "DimViewer"
    Then participant "DimViewer" should have 1 remote video tracks available in room "DimensionRoom" using service "livekit1"

  # Story 1.1.9.4: Multiple subscribers receive ingress stream
  Scenario: Multiple browser subscribers receive ingress stream simultaneously
    Given room "MultiViewerRoom" is created using service "livekit1"
    And "Broadcaster" creates an RTMP ingress to room "MultiViewerRoom"
    And "Broadcaster" starts streaming via RTMP
    And the ingress for "Broadcaster" is publishing within 30 seconds
    And an access token is created with identity "Viewer1" and room "MultiViewerRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "Viewer2" and room "MultiViewerRoom" with grants "canPublish:false,canSubscribe:true"
    When "Viewer1" opens a "Chrome" browser with LiveKit Meet page
    And "Viewer1" connects to room "MultiViewerRoom" using the access token
    And connection is established successfully for "Viewer1"
    When "Viewer2" opens a "Firefox" browser with LiveKit Meet page
    And "Viewer2" connects to room "MultiViewerRoom" using the access token
    And connection is established successfully for "Viewer2"
    Then room "MultiViewerRoom" should have 3 active participants in service "livekit1"
    And participant "Viewer1" should have 1 remote video tracks available in room "MultiViewerRoom" using service "livekit1"
    And participant "Viewer2" should have 1 remote video tracks available in room "MultiViewerRoom" using service "livekit1"

  # Story 1.1.9.5: Verify ingress track metadata
  Scenario: Ingress video track has correct source type
    Given room "MetadataRoom" is created using service "livekit1"
    And "MetaStreamer" creates an RTMP ingress to room "MetadataRoom"
    And "MetaStreamer" starts streaming via RTMP
    And the ingress for "MetaStreamer" is publishing within 30 seconds
    Then participant "MetaStreamer" should be publishing video in room "MetadataRoom" using service "livekit1"
    And participant "MetaStreamer" video track should have source "CAMERA" in room "MetadataRoom" using service "livekit1"
    And participant "MetaStreamer" should have published audio track in room "MetadataRoom" using service "livekit1"

  # Story 1.1.9.6: Extended version of existing "Delete ingress removes participant"
  # This adds subscriber perspective to the existing delete scenario
  Scenario: Subscriber detects when ingress stream stops
    Given room "RemovalRoom" is created using service "livekit1"
    And "RemovalStreamer" creates an RTMP ingress to room "RemovalRoom"
    And "RemovalStreamer" starts streaming via RTMP
    And the ingress for "RemovalStreamer" is publishing within 30 seconds
    And an access token is created with identity "RemovalViewer" and room "RemovalRoom" with grants "canPublish:false,canSubscribe:true"
    When "RemovalViewer" opens a "Chrome" browser with LiveKit Meet page
    And "RemovalViewer" connects to room "RemovalRoom" using the access token
    And connection is established successfully for "RemovalViewer"
    And participant "RemovalViewer" should have 1 remote video tracks available in room "RemovalRoom" using service "livekit1"
    When "RemovalStreamer" stops streaming
    Then the ingress for "RemovalStreamer" should be inactive within 15 seconds
    And participant "RemovalStreamer" should not exist in room "RemovalRoom" using service "livekit1"

  # Story 1.1.9.7: Ingress simulcast verification with subscriber
  Scenario: Ingress with simulcast preset has multiple layers available to subscriber
    Given room "SimulcastIngressRoom" is created using service "livekit1"
    And "SimStreamer" creates an RTMP ingress to room "SimulcastIngressRoom" with video preset "H264_720P_30FPS_3_LAYERS"
    And "SimStreamer" starts streaming via RTMP with resolution "1280x720"
    And the ingress for "SimStreamer" is publishing within 30 seconds
    And participant "SimStreamer" video track should have ">=2" layers in room "SimulcastIngressRoom" using service "livekit1"
    And participant "SimStreamer" video layers should have different resolutions in room "SimulcastIngressRoom" using service "livekit1"
    And an access token is created with identity "SimViewer" and room "SimulcastIngressRoom" with grants "canPublish:false,canSubscribe:true"
    When "SimViewer" opens a "Chrome" browser with LiveKit Meet page
    And "SimViewer" connects to room "SimulcastIngressRoom" using the access token
    And connection is established successfully for "SimViewer"
    Then participant "SimViewer" should have 1 remote video tracks available in room "SimulcastIngressRoom" using service "livekit1"
```

### Consolidation Notes

The playback scenarios are designed to **extend** the existing ingress feature rather than duplicate:

| Existing Scenario | New Playback Extension |
|-------------------|------------------------|
| "RTMP stream appears as participant in room" | Tests server-side participant appearance |
| "Browser subscriber receives media from RTMP ingress" | Tests client-side track reception |
| "Delete ingress removes participant" | Tests server-side cleanup |
| "Subscriber detects when ingress stream stops" | Tests client-side cleanup detection |

### Scenario Outline Consolidation

Instead of separate Chrome and Firefox scenarios, we use `Scenario Outline` with `Examples` to test both browsers with the same logic, reducing duplication from 13 scenarios down to 7.

---

## Definition of Done

### Code Implementation
- [ ] All new step definitions implemented in appropriate step classes
- [ ] Playback scenarios appended to existing `livekit_ingress.feature`
- [ ] All scenarios pass on Chrome browser
- [ ] All scenarios pass on Firefox browser
- [ ] Proper cleanup in @After hooks

### Testing
- [ ] All unit tests pass
- [ ] All BDD scenarios pass against default LiveKit/Ingress version
- [ ] Multi-subscriber scenarios verified (2-3 browsers)
- [ ] Track removal detection verified

### Documentation
- [ ] Feature documentation complete in `docs/features/ingress-playback-testing/`
- [ ] New step definitions documented
- [ ] Technical notes cover subscriber integration

### Code Quality
- [ ] No code comments added (per project guidelines)
- [ ] Proper Lombok usage
- [ ] Cross-platform path handling maintained

---

## Technical Components

### New Step Definitions Required

#### In `LiveKitRoomSteps.java` (extensions):

```java
@Then("participant {string} should have published audio track in room {string} using service {string}")
public void participantShouldHavePublishedAudioTrackInRoomUsingService(
    String participantIdentity, String roomName, String serviceName)

@Then("participant {string} video track should have source {string} in room {string} using service {string}")
public void participantVideoTrackShouldHaveSourceInRoomUsingService(
    String participantIdentity, String source, String roomName, String serviceName)

@Then("participant {string} video track should have maximum width {int} in room {string} using service {string}")
public void participantVideoTrackShouldHaveMaximumWidthInRoomUsingService(
    String participantIdentity, int maxWidth, String roomName, String serviceName)
```

Note: Many required steps already exist from Stories 1.1.1-1.1.8:
- `participant should have N remote video tracks available`
- `participant should be publishing video`
- `participant video track should have N layers`
- `participant video layers should have different resolutions`

### Reusable Components from Existing Stories

| Step Pattern | Source Story | Reuse Status |
|--------------|--------------|--------------|
| `room should have N active participants` | 1.1.1 | Direct reuse |
| `participant should have N remote video tracks` | 1.1.1 | Direct reuse |
| `participant should be publishing video` | 1.1.1 | Direct reuse |
| `video track should have N layers` | 1.1.2 | Direct reuse |
| `video layers should have different resolutions` | 1.1.2 | Direct reuse |
| `participant should not exist in room` | 1.1.8 | Direct reuse |
| `ingress should be publishing within N seconds` | 1.1.8 | Direct reuse |

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Ingress transcoding delay causes timeouts | High | Medium | Use 45-60 second timeouts for playback scenarios |
| Browser track subscription timing varies | Medium | Medium | Implement robust polling with fallback |
| Multi-browser tests are resource-intensive | Medium | Low | Limit to 3 browsers max; run sequentially |
| Track metadata not immediately available | Medium | Medium | Add polling for track info stabilization |
| FFmpeg stream quality affects playback | Low | Low | Use known-good FFmpeg configurations |

### Mitigation Strategies

1. **Timeout Management**: Use generous timeouts (45+ seconds) for ingress scenarios due to transcoding latency

2. **Polling Strategy**: Implement polling similar to existing track verification (20 attempts, 500ms interval)

3. **Resource Management**: Run multi-browser tests sequentially; ensure proper browser cleanup

4. **Track Stabilization**: Wait for track metadata to stabilize before verification (additional 2-3 second buffer)

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.9.1 - Single Subscriber Video | M | Foundation for all playback tests |
| 2 | 1.1.9.2 - Single Subscriber Audio | S | Simple extension of video testing |
| 3 | 1.1.9.5 - Track Metadata | S | Validates track properties |
| 4 | 1.1.9.3 - Video Dimensions | M | Validates transcoding configuration |
| 5 | 1.1.9.4 - Multiple Subscribers | M | Validates broadcast scenarios |
| 6 | 1.1.9.6 - Track Removal | S | Tests cleanup behavior |
| 7 | 1.1.9.7 - Simulcast Layers | M | Advanced quality verification |

**Total Estimated Effort:** M (Medium) - approximately 3-5 days

---

## Appendix: Ingress Track Properties Reference

### TrackInfo for Ingress Participant

| Field | Expected Value | Notes |
|-------|----------------|-------|
| `type` | VIDEO or AUDIO | Standard track types |
| `source` | CAMERA or MICROPHONE | Ingress tracks use these sources |
| `simulcast` | true/false | Depends on preset |
| `layers` | 1-3 typically | Depends on preset |
| `muted` | false | Unless stream issue |
| `width` | Varies | From transcoding |
| `height` | Varies | From transcoding |

### Expected Layer Counts by Preset

| Preset | Layer Count | Typical Resolutions |
|--------|-------------|---------------------|
| H264_720P_30FPS_3_LAYERS | 3 | 1280x720, 640x360, 320x180 |
| H264_1080P_30FPS_3_LAYERS | 3 | 1920x1080, 960x540, 480x270 |
| H264_540P_25FPS_2_LAYERS | 2 | 960x540, 480x270 |
| H264_720P_30FPS_1_LAYER | 1 | 1280x720 |
| Default (no preset) | 1 | Source resolution |

---

## Related Stories

- **Story 1.1.8** (Ingress Infrastructure) - Prerequisite; provides ingress container and API
- **Story 1.1.2** (Simulcast Testing) - Related; similar layer verification patterns
- **Story 1.1.10** (Record Ingress via Egress) - Follow-up; records ingress streams
- **Story 1.1.1** (WebRTC Publishing/Playback) - Related; similar browser subscription patterns
