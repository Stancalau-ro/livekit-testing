# Track Mute and Unmute Testing - Requirements Document

## Epic Description

**Epic:** Test Track Mute and Unmute Operations
**Story ID:** 1.1.7
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test track mute/unmute behavior
**So that** I can verify mute state is correctly propagated

---

## Story Breakdown

The original story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.7.1: Client-Side Audio Mute Operations

**Size:** S (Small)

**As a** test developer
**I want** to test client-side audio mute/unmute
**So that** I can verify audio track state changes work correctly

**Acceptance Criteria:**

**Given** a participant is publishing audio
**When** they mute their microphone
**Then** the audio track should be muted

**Given** a participant has muted audio
**When** they unmute their microphone
**Then** audio transmission should resume

**Given** a participant mutes audio
**When** inspected via server API
**Then** the track muted state should be true

- [x] Add explicit audio mute/unmute methods to LiveKitMeet
- [x] Verify mute state via JavaScript client
- [x] Verify mute state via server API TrackInfo
- [ ] Test rapid mute/unmute toggling

**Dependencies:** None (builds on existing WebRTC publishing)

---

### Story 1.1.7.2: Client-Side Video Mute Operations

**Size:** S (Small)

**As a** test developer
**I want** to test client-side video mute/unmute (camera disable)
**So that** I can verify video track state changes work correctly

**Acceptance Criteria:**

**Given** a participant is publishing video
**When** they disable their camera
**Then** the video track should be muted

**Given** a participant has disabled video
**When** they enable their camera
**Then** video transmission should resume

**Given** a participant disables video
**When** inspected via server API
**Then** the video track muted state should be true

- [x] Add explicit video mute/unmute methods to LiveKitMeet
- [x] Verify video mute state via JavaScript client
- [x] Verify video mute state via server API TrackInfo
- [ ] Differentiate between camera off and video muted

**Dependencies:** None (builds on existing WebRTC publishing)

---

### Story 1.1.7.3: Mute State Propagation to Other Participants

**Size:** M (Medium)

**As a** test developer
**I want** to verify mute state is visible to other participants
**So that** I can confirm mute state propagates correctly

**Acceptance Criteria:**

**Given** participant A is publishing audio/video
**When** participant A mutes their track
**Then** participant B should see the track as muted

**Given** participant A has muted their track
**When** participant A unmutes
**Then** participant B should see the track as unmuted

**Given** multiple participants are in a room
**When** one participant mutes
**Then** all other participants receive the mute state update

- [x] Add step definitions for verifying remote participant mute state
- [x] Implement mute state verification via remote track events
- [x] Test mute visibility with 2+ participants
- [ ] Verify mute state indicator in subscriber UI

**Dependencies:** Story 1.1.7.1, Story 1.1.7.2

---

### Story 1.1.7.4: Server-Initiated Mute Operations

**Size:** M (Medium)

**As a** test developer
**I want** to test server-initiated mute operations
**So that** I can verify admin mute functionality works

**Acceptance Criteria:**

**Given** a participant is publishing audio
**When** the server mutes their track via API
**Then** the participant's track should be muted

**Given** a server has muted a participant
**When** the participant attempts to unmute client-side
**Then** the behavior should be documented (allowed or blocked)

**Given** a server mutes a participant
**When** other participants are subscribed
**Then** they should see the muted state

- [ ] Implement server-initiated mute via RoomServiceClient.mutePublishedTrack()
- [ ] Verify client receives mute event
- [ ] Test server unmute operation
- [ ] Document client-side unmute behavior after server mute

Note: Server-initiated mute scenarios are defined in requirements but not yet implemented in the feature file.

**Dependencies:** Story 1.1.7.1

---

### Story 1.1.7.5: Cross-Browser Mute Verification

**Size:** S (Small)

**As a** test developer
**I want** to verify mute works across different browsers
**So that** I can ensure cross-browser compatibility

**Acceptance Criteria:**

**Given** Chrome participant mutes their track
**When** Firefox participant is subscribed
**Then** Firefox participant sees the muted state

**Given** mute operations work on Chrome
**When** the same operations are tested on Firefox and Edge
**Then** behavior should be consistent

- [x] Test audio mute on Chrome, Firefox
- [x] Test video mute on Chrome, Firefox
- [x] Verify cross-browser mute state propagation
- [ ] Document any browser-specific differences

**Dependencies:** Story 1.1.7.3

---

### Story 1.1.7.6: Verify Media Flow Resumes After Unmute

**Size:** S (Small)

**As a** test developer
**I want** to verify that media flow actually resumes after unmute
**So that** I can confirm unmute restores full functionality

**Acceptance Criteria:**

**Given** a participant has muted audio
**When** they unmute
**Then** audio bytes should be transmitted again

**Given** a participant has disabled video
**When** they enable video
**Then** video frames should be transmitted again

**Given** media was muted for extended period
**When** unmuted
**Then** media flow should resume without reconnection

- [ ] Verify audio transmission after unmute (via stats or server metrics)
- [ ] Verify video transmission after unmute (via stats or server metrics)
- [ ] Test unmute after extended mute period
- [ ] Verify no track recreation needed on unmute

Note: Story 1.1.7.6 scenarios are defined in requirements but not yet implemented in the feature file.

**Dependencies:** Story 1.1.7.1, Story 1.1.7.2

---

## Gherkin Scenarios

### Feature File: `livekit_track_mute.feature`

```gherkin
Feature: LiveKit Track Mute and Unmute Operations
  As a test developer
  I want to test track mute/unmute behavior
  So that I can verify mute state is correctly propagated

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.7.1: Client-Side Audio Mute Operations
  Scenario: Participant mutes audio track
    Given an access token is created with identity "AudioMuter" and room "MuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "MuteRoom" is created using service "livekit1"

    When "AudioMuter" opens a "Chrome" browser with LiveKit Meet page
    And "AudioMuter" connects to room "MuteRoom" using the access token
    And connection is established successfully for "AudioMuter"

    Then participant "AudioMuter" should be publishing audio in room "MuteRoom" using service "livekit1"
    And participant "AudioMuter" audio track should not be muted in room "MuteRoom" using service "livekit1"

    When "AudioMuter" mutes their audio

    Then participant "AudioMuter" audio track should be muted in room "MuteRoom" using service "livekit1"
    And "AudioMuter" closes the browser

  Scenario: Participant unmutes audio track
    Given an access token is created with identity "AudioUnmuter" and room "UnmuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "UnmuteRoom" is created using service "livekit1"

    When "AudioUnmuter" opens a "Chrome" browser with LiveKit Meet page
    And "AudioUnmuter" connects to room "UnmuteRoom" using the access token
    And connection is established successfully for "AudioUnmuter"

    Then participant "AudioUnmuter" should be publishing audio in room "UnmuteRoom" using service "livekit1"

    When "AudioUnmuter" mutes their audio
    Then participant "AudioUnmuter" audio track should be muted in room "UnmuteRoom" using service "livekit1"

    When "AudioUnmuter" unmutes their audio
    Then participant "AudioUnmuter" audio track should not be muted in room "UnmuteRoom" using service "livekit1"
    And "AudioUnmuter" closes the browser

  # Story 1.1.7.2: Client-Side Video Mute Operations
  Scenario: Participant mutes video track
    Given an access token is created with identity "VideoMuter" and room "VideoMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "VideoMuteRoom" is created using service "livekit1"

    When "VideoMuter" opens a "Chrome" browser with LiveKit Meet page
    And "VideoMuter" connects to room "VideoMuteRoom" using the access token
    And connection is established successfully for "VideoMuter"

    Then participant "VideoMuter" should be publishing video in room "VideoMuteRoom" using service "livekit1"
    And participant "VideoMuter" video track should not be muted in room "VideoMuteRoom" using service "livekit1"

    When "VideoMuter" mutes their video

    Then participant "VideoMuter" video track should be muted in room "VideoMuteRoom" using service "livekit1"
    And "VideoMuter" closes the browser

  Scenario: Participant unmutes video track
    Given an access token is created with identity "VideoUnmuter" and room "VideoUnmuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "VideoUnmuteRoom" is created using service "livekit1"

    When "VideoUnmuter" opens a "Chrome" browser with LiveKit Meet page
    And "VideoUnmuter" connects to room "VideoUnmuteRoom" using the access token
    And connection is established successfully for "VideoUnmuter"

    Then participant "VideoUnmuter" should be publishing video in room "VideoUnmuteRoom" using service "livekit1"

    When "VideoUnmuter" mutes their video
    Then participant "VideoUnmuter" video track should be muted in room "VideoUnmuteRoom" using service "livekit1"

    When "VideoUnmuter" unmutes their video
    Then participant "VideoUnmuter" video track should not be muted in room "VideoUnmuteRoom" using service "livekit1"
    And "VideoUnmuter" closes the browser

  # Story 1.1.7.3: Mute State Propagation to Other Participants
  Scenario: Other participant sees audio mute state
    Given an access token is created with identity "Publisher" and room "PropagateRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Subscriber" and room "PropagateRoom" with grants "canPublish:false,canSubscribe:true"
    And room "PropagateRoom" is created using service "livekit1"

    When "Publisher" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher" connects to room "PropagateRoom" using the access token
    And connection is established successfully for "Publisher"

    Then participant "Publisher" should be publishing audio in room "PropagateRoom" using service "livekit1"

    When "Subscriber" opens a "Chrome" browser with LiveKit Meet page
    And "Subscriber" connects to room "PropagateRoom" using the access token
    And connection is established successfully for "Subscriber"

    Then room "PropagateRoom" should have 2 active participants in service "livekit1"
    And "Subscriber" should see "Publisher" audio track as not muted

    When "Publisher" mutes their audio

    Then "Subscriber" should see "Publisher" audio track as muted
    And "Publisher" closes the browser
    And "Subscriber" closes the browser

  Scenario: Other participant sees video mute state
    Given an access token is created with identity "VideoPublisher" and room "VideoPropRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "VideoSubscriber" and room "VideoPropRoom" with grants "canPublish:false,canSubscribe:true"
    And room "VideoPropRoom" is created using service "livekit1"

    When "VideoPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "VideoPublisher" connects to room "VideoPropRoom" using the access token
    And connection is established successfully for "VideoPublisher"

    Then participant "VideoPublisher" should be publishing video in room "VideoPropRoom" using service "livekit1"

    When "VideoSubscriber" opens a "Chrome" browser with LiveKit Meet page
    And "VideoSubscriber" connects to room "VideoPropRoom" using the access token
    And connection is established successfully for "VideoSubscriber"

    Then room "VideoPropRoom" should have 2 active participants in service "livekit1"
    And "VideoSubscriber" should see "VideoPublisher" video track as not muted

    When "VideoPublisher" mutes their video

    Then "VideoSubscriber" should see "VideoPublisher" video track as muted
    And "VideoPublisher" closes the browser
    And "VideoSubscriber" closes the browser

  # Story 1.1.7.4: Server-Initiated Mute Operations
  Scenario: Server mutes participant audio track
    Given an access token is created with identity "ServerMuteTarget" and room "ServerMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "ServerMuteRoom" is created using service "livekit1"

    When "ServerMuteTarget" opens a "Chrome" browser with LiveKit Meet page
    And "ServerMuteTarget" connects to room "ServerMuteRoom" using the access token
    And connection is established successfully for "ServerMuteTarget"

    Then participant "ServerMuteTarget" should be publishing audio in room "ServerMuteRoom" using service "livekit1"
    And participant "ServerMuteTarget" audio track should not be muted in room "ServerMuteRoom" using service "livekit1"

    When the server mutes "ServerMuteTarget" audio track in room "ServerMuteRoom" using service "livekit1"

    Then participant "ServerMuteTarget" audio track should be muted in room "ServerMuteRoom" using service "livekit1"
    And "ServerMuteTarget" local audio should show as muted
    And "ServerMuteTarget" closes the browser

  Scenario: Server mutes participant video track
    Given an access token is created with identity "ServerVideoMute" and room "ServerVideoRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "ServerVideoRoom" is created using service "livekit1"

    When "ServerVideoMute" opens a "Chrome" browser with LiveKit Meet page
    And "ServerVideoMute" connects to room "ServerVideoRoom" using the access token
    And connection is established successfully for "ServerVideoMute"

    Then participant "ServerVideoMute" should be publishing video in room "ServerVideoRoom" using service "livekit1"
    And participant "ServerVideoMute" video track should not be muted in room "ServerVideoRoom" using service "livekit1"

    When the server mutes "ServerVideoMute" video track in room "ServerVideoRoom" using service "livekit1"

    Then participant "ServerVideoMute" video track should be muted in room "ServerVideoRoom" using service "livekit1"
    And "ServerVideoMute" closes the browser

  # Story 1.1.7.5: Cross-Browser Mute Verification
  Scenario Outline: Participant can mute audio with different browsers
    Given an access token is created with identity "BrowserMuter" and room "BrowserMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "BrowserMuteRoom" is created using service "livekit1"

    When "BrowserMuter" opens a <browser> browser with LiveKit Meet page
    And "BrowserMuter" connects to room "BrowserMuteRoom" using the access token
    And connection is established successfully for "BrowserMuter"

    Then participant "BrowserMuter" should be publishing audio in room "BrowserMuteRoom" using service "livekit1"

    When "BrowserMuter" mutes their audio

    Then participant "BrowserMuter" audio track should be muted in room "BrowserMuteRoom" using service "livekit1"
    And "BrowserMuter" closes the browser

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |

  Scenario: Cross-browser mute state propagation
    Given an access token is created with identity "ChromePublisher" and room "CrossBrowserRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "FirefoxSubscriber" and room "CrossBrowserRoom" with grants "canPublish:false,canSubscribe:true"
    And room "CrossBrowserRoom" is created using service "livekit1"

    When "ChromePublisher" opens a "Chrome" browser with LiveKit Meet page
    And "ChromePublisher" connects to room "CrossBrowserRoom" using the access token
    And connection is established successfully for "ChromePublisher"

    When "FirefoxSubscriber" opens a "Firefox" browser with LiveKit Meet page
    And "FirefoxSubscriber" connects to room "CrossBrowserRoom" using the access token
    And connection is established successfully for "FirefoxSubscriber"

    Then room "CrossBrowserRoom" should have 2 active participants in service "livekit1"

    When "ChromePublisher" mutes their audio

    Then "FirefoxSubscriber" should see "ChromePublisher" audio track as muted
    And "ChromePublisher" closes the browser
    And "FirefoxSubscriber" closes the browser

  # Story 1.1.7.6: Verify Media Flow Resumes After Unmute
  Scenario: Audio flow resumes after unmute
    Given an access token is created with identity "AudioResumeTest" and room "ResumeRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "ResumeRoom" is created using service "livekit1"

    When "AudioResumeTest" opens a "Chrome" browser with LiveKit Meet page
    And "AudioResumeTest" connects to room "ResumeRoom" using the access token
    And connection is established successfully for "AudioResumeTest"

    Then participant "AudioResumeTest" should be publishing audio in room "ResumeRoom" using service "livekit1"

    When "AudioResumeTest" mutes their audio
    Then participant "AudioResumeTest" audio track should be muted in room "ResumeRoom" using service "livekit1"

    When "AudioResumeTest" waits for 2 seconds
    And "AudioResumeTest" unmutes their audio

    Then participant "AudioResumeTest" audio track should not be muted in room "ResumeRoom" using service "livekit1"
    And participant "AudioResumeTest" should be publishing audio in room "ResumeRoom" using service "livekit1"
    And "AudioResumeTest" closes the browser

  Scenario: Video flow resumes after unmute
    Given an access token is created with identity "VideoResumeTest" and room "VideoResumeRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "VideoResumeRoom" is created using service "livekit1"

    When "VideoResumeTest" opens a "Chrome" browser with LiveKit Meet page
    And "VideoResumeTest" connects to room "VideoResumeRoom" using the access token
    And connection is established successfully for "VideoResumeTest"

    Then participant "VideoResumeTest" should be publishing video in room "VideoResumeRoom" using service "livekit1"

    When "VideoResumeTest" mutes their video
    Then participant "VideoResumeTest" video track should be muted in room "VideoResumeRoom" using service "livekit1"

    When "VideoResumeTest" waits for 2 seconds
    And "VideoResumeTest" unmutes their video

    Then participant "VideoResumeTest" video track should not be muted in room "VideoResumeRoom" using service "livekit1"
    And participant "VideoResumeTest" should be publishing video in room "VideoResumeRoom" using service "livekit1"
    And "VideoResumeTest" closes the browser
```

---

## Definition of Done

### Code Implementation
- [x] New step definitions added to `LiveKitBrowserWebrtcSteps.java` for mute control
- [x] New step definitions added to `LiveKitRoomSteps.java` for mute state verification
- [x] `LiveKitMeet.java` extended with explicit audio/video mute methods
- [x] Feature file `livekit_track_mute.feature` created and passing
- [x] All scenarios pass on Chrome browser
- [x] All scenarios pass on Firefox browser

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass
- [x] Tests pass against default LiveKit version
- [ ] Server-initiated mute tested via RoomServiceClient
- [x] Cross-browser mute propagation verified

### Documentation
- [x] Feature documentation complete in `docs/features/track-mute-testing/`
- [ ] Step definitions documented in `docs/features.md`
- [ ] Technical notes added for mute implementation

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
@When("{string} mutes their audio")
public void mutesTheirAudio(String participantName)

@When("{string} unmutes their audio")
public void unmutesTheirAudio(String participantName)

@When("{string} mutes their video")
public void mutesTheirVideo(String participantName)

@When("{string} unmutes their video")
public void unmutesTheirVideo(String participantName)

@Then("{string} should see {string} audio track as muted")
public void shouldSeeAudioTrackAsMuted(String subscriber, String publisher)

@Then("{string} should see {string} audio track as not muted")
public void shouldSeeAudioTrackAsNotMuted(String subscriber, String publisher)

@Then("{string} should see {string} video track as muted")
public void shouldSeeVideoTrackAsMuted(String subscriber, String publisher)

@Then("{string} should see {string} video track as not muted")
public void shouldSeeVideoTrackAsNotMuted(String subscriber, String publisher)

@Then("{string} local audio should show as muted")
public void localAudioShouldShowAsMuted(String participantName)

@When("{string} waits for {int} seconds")
public void waitsForSeconds(String participantName, int seconds)
```

#### In `LiveKitRoomSteps.java`:
```java
@Then("participant {string} audio track should be muted in room {string} using service {string}")
public void audioTrackShouldBeMuted(String identity, String room, String service)

@Then("participant {string} audio track should not be muted in room {string} using service {string}")
public void audioTrackShouldNotBeMuted(String identity, String room, String service)

@Then("participant {string} video track should be muted in room {string} using service {string}")
public void videoTrackShouldBeMuted(String identity, String room, String service)

@Then("participant {string} video track should not be muted in room {string} using service {string}")
public void videoTrackShouldNotBeMuted(String identity, String room, String service)

@When("the server mutes {string} audio track in room {string} using service {string}")
public void serverMutesAudioTrack(String identity, String room, String service)

@When("the server mutes {string} video track in room {string} using service {string}")
public void serverMutesVideoTrack(String identity, String room, String service)
```

### LiveKitMeet Page Object Extensions

```java
public void muteAudio()
public void unmuteAudio()
public boolean isAudioMuted()

public void muteVideo()
public void unmuteVideo()
public boolean isVideoMuted()

public boolean isRemoteTrackMuted(String identity, String trackType)
```

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Mute state API delay | Medium | Medium | Implement polling with reasonable timeout |
| Server mute behavior differs from client mute | Medium | Low | Document differences and test both paths |
| Browser-specific mute behavior | Low | Low | Test on all target browsers |
| Fake media stream mute detection | Low | Medium | Verify via server API, not just client state |

### Mitigation Strategies

1. **API Delay**: Use established polling pattern (20 attempts, 500ms interval)
2. **Server vs Client**: Test both paths independently with separate scenarios
3. **Browser Differences**: Focus on Chrome as primary, document Firefox/Edge differences
4. **Detection**: Rely on server API TrackInfo.getMuted() as source of truth

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.7.1 - Audio Mute | S | Foundation for all mute tests |
| 2 | 1.1.7.2 - Video Mute | S | Similar pattern to audio |
| 3 | 1.1.7.3 - Mute Propagation | M | Core verification capability |
| 4 | 1.1.7.4 - Server Mute | M | Admin functionality |
| 5 | 1.1.7.5 - Cross-Browser | S | Compatibility verification |
| 6 | 1.1.7.6 - Unmute Verification | S | Edge case validation |

**Total Estimated Effort:** M (Medium) - approximately 3-5 days

---

## Appendix: LiveKit Mute Reference

### TrackInfo Protobuf Mute Field

```protobuf
message TrackInfo {
  // ... other fields ...
  bool muted = 7;
  string name = 8;
  TrackType type = 1;
  TrackSource source = 2;
}
```

### LiveKit Client SDK Mute Methods

```typescript
// Mute local audio
room.localParticipant.setMicrophoneEnabled(false);

// Mute local video
room.localParticipant.setCameraEnabled(false);

// Check mute state
localParticipant.audioTrackPublications.forEach(pub => {
  console.log('Audio muted:', pub.isMuted);
});
```

### Server-Side Mute API

```java
RoomServiceClient.mutePublishedTrack(
  room,
  identity,
  trackSid,
  muted  // true to mute, false to unmute
);
```

---

## Related Stories

- **Story 1.1.1** (Screen Sharing) - Completed; similar browser interaction patterns
- **Story 1.1.2** (Simulcast) - Completed; related track state management
- **Story 1.1.4** (Data Channel) - Future; unrelated but same priority level
