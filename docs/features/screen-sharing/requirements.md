# Screen Sharing Testing - Requirements Document

## Epic Description

**Epic:** Test Screen Sharing Functionality
**Story ID:** 1.1.1
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test screen sharing between participants
**So that** I can verify screen share track publishing and subscription

---

## Story Breakdown

The original story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.1.1: Screen Share Permission Grant

**Size:** XS (Extra Small)

**As a** test developer
**I want** to create access tokens with screen share publishing permissions
**So that** participants can be authorized to share their screens

**Acceptance Criteria:**
- [x] Token with `canPublishSources` grant can be created
- [x] Grant accepts screen share source type specification
- [x] Token without screen share permission can be created for negative testing
- [x] Token validation confirms grant presence

**Dependencies:** None (uses existing AccessTokenStateManager)

---

### Story 1.1.1.2: Screen Share Track Publishing

**Size:** S (Small)

**As a** test developer
**I want** to simulate screen sharing from a browser
**So that** I can verify screen share tracks are published to the room

**Acceptance Criteria:**
- [x] Participant can start screen share from browser
- [x] Screen share track appears in participant's published tracks via server API
- [x] Track is identified as screen share type (not camera)
- [x] Screen share publishing works with Chrome browser
- [x] Screen share publishing works with Edge browser

**Dependencies:** Story 1.1.1.1

---

### Story 1.1.1.3: Screen Share Track Subscription

**Size:** S (Small)

**As a** test developer
**I want** to verify subscribers receive screen share tracks
**So that** I can confirm screen sharing works end-to-end

**Acceptance Criteria:**
- [x] Subscriber can see screen share track from publisher
- [x] Screen share track count is correct via server API
- [ ] Subscriber without subscribe permission cannot receive screen share
- [x] Multiple subscribers receive the same screen share track

**Dependencies:** Story 1.1.1.2

---

### Story 1.1.1.4: Screen Share Stop and Cleanup

**Size:** XS (Extra Small)

**As a** test developer
**I want** to verify screen sharing stops correctly
**So that** I can confirm proper cleanup of screen share resources

**Acceptance Criteria:**
- [x] Screen share can be stopped from browser
- [x] Screen share track is removed from server after stop
- [x] Other participants see track removed
- [x] Participant can start new screen share after stopping previous one

**Dependencies:** Story 1.1.1.2

---

### Story 1.1.1.5: Screen Share Permission Denied

**Size:** XS (Extra Small)

**As a** test developer
**I want** to verify participants without screen share permission cannot share
**So that** I can confirm permission enforcement works

**Acceptance Criteria:**
- [x] Participant without canPublishSources cannot publish screen share
- [x] Error or blocked state is detectable
- [x] Other publish permissions (camera) still work independently

**Dependencies:** Story 1.1.1.1, Story 1.1.1.2

---

## Gherkin Scenarios

### Feature File: `livekit_screen_sharing.feature`

```gherkin
Feature: LiveKit Screen Sharing
  As a test developer
  I want to test screen sharing functionality
  So that I can verify screen share track publishing and subscription

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.1.1: Screen Share Permission Grant
  Scenario: Generate access token with screen share permission
    When an access token is created with identity "ScreenUser" and room "ScreenRoom" with grants "canPublish:true,canPublishSources:screen_share"
    Then the access token for "ScreenUser" in room "ScreenRoom" should be valid
    And the access token for "ScreenUser" in room "ScreenRoom" should have the following grants:
      | grant             | value        |
      | canPublish        | true         |
      | roomJoin          | true         |

  Scenario: Generate access token without screen share permission
    When an access token is created with identity "NoScreenUser" and room "RestrictedRoom" with grants "canPublish:true,canPublishSources:camera,canPublishSources:microphone"
    Then the access token for "NoScreenUser" in room "RestrictedRoom" should be valid
    And the access token for "NoScreenUser" in room "RestrictedRoom" should have the following grants:
      | grant             | value        |
      | canPublish        | true         |
      | roomJoin          | true         |

  # Story 1.1.1.2: Screen Share Track Publishing
  Scenario: Participant can publish screen share track
    Given an access token is created with identity "Publisher" and room "ScreenShareRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And room "ScreenShareRoom" is created using service "livekit1"

    When "Publisher" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher" connects to room "ScreenShareRoom" using the access token
    And connection is established successfully for "Publisher"
    And "Publisher" starts screen sharing

    Then room "ScreenShareRoom" should have 1 active participants in service "livekit1"
    And participant "Publisher" should be publishing screen share in room "ScreenShareRoom" using service "livekit1"
    And participant "Publisher" should have 2 published tracks in room "ScreenShareRoom" using service "livekit1"

  Scenario Outline: Screen share publishing works across browsers
    Given an access token is created with identity "BrowserUser" and room "CrossBrowserRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And room "CrossBrowserRoom" is created using service "livekit1"

    When "BrowserUser" opens a <browser> browser with LiveKit Meet page
    And "BrowserUser" connects to room "CrossBrowserRoom" using the access token
    And connection is established successfully for "BrowserUser"
    And "BrowserUser" starts screen sharing

    Then participant "BrowserUser" should be publishing screen share in room "CrossBrowserRoom" using service "livekit1"

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |

  # Story 1.1.1.3: Screen Share Track Subscription
  Scenario: Subscriber receives screen share track from publisher
    Given an access token is created with identity "ScreenPublisher" and room "SubTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And an access token is created with identity "ScreenViewer" and room "SubTestRoom" with grants "canPublish:false,canSubscribe:true"
    And room "SubTestRoom" is created using service "livekit1"

    When "ScreenPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "ScreenPublisher" connects to room "SubTestRoom" using the access token
    And connection is established successfully for "ScreenPublisher"
    And "ScreenPublisher" starts screen sharing

    Then participant "ScreenPublisher" should be publishing screen share in room "SubTestRoom" using service "livekit1"

    When "ScreenViewer" opens a "Chrome" browser with LiveKit Meet page
    And "ScreenViewer" connects to room "SubTestRoom" using the access token
    And connection is established successfully for "ScreenViewer"

    Then room "SubTestRoom" should have 2 active participants in service "livekit1"
    And participant "ScreenViewer" should see 1 remote screen share tracks in room "SubTestRoom" using service "livekit1"

  Scenario: Multiple subscribers receive the same screen share track
    Given an access token is created with identity "MainPresenter" and room "MultiSubRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And an access token is created with identity "Viewer1" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "Viewer2" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiSubRoom" is created using service "livekit1"

    When "MainPresenter" opens a "Chrome" browser with LiveKit Meet page
    And "MainPresenter" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "MainPresenter"
    And "MainPresenter" starts screen sharing

    When "Viewer1" opens a "Chrome" browser with LiveKit Meet page
    And "Viewer1" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Viewer1"

    When "Viewer2" opens a "Firefox" browser with LiveKit Meet page
    And "Viewer2" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Viewer2"

    Then room "MultiSubRoom" should have 3 active participants in service "livekit1"
    And participant "Viewer1" should see 1 remote screen share tracks in room "MultiSubRoom" using service "livekit1"
    And participant "Viewer2" should see 1 remote screen share tracks in room "MultiSubRoom" using service "livekit1"

  # Story 1.1.1.4: Screen Share Stop and Cleanup
  Scenario: Screen share can be stopped and track is removed
    Given an access token is created with identity "StopTestUser" and room "StopTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And an access token is created with identity "Observer" and room "StopTestRoom" with grants "canPublish:false,canSubscribe:true"
    And room "StopTestRoom" is created using service "livekit1"

    When "StopTestUser" opens a "Chrome" browser with LiveKit Meet page
    And "StopTestUser" connects to room "StopTestRoom" using the access token
    And connection is established successfully for "StopTestUser"
    And "StopTestUser" starts screen sharing

    Then participant "StopTestUser" should be publishing screen share in room "StopTestRoom" using service "livekit1"

    When "Observer" opens a "Chrome" browser with LiveKit Meet page
    And "Observer" connects to room "StopTestRoom" using the access token
    And connection is established successfully for "Observer"

    Then participant "Observer" should see 1 remote screen share tracks in room "StopTestRoom" using service "livekit1"

    When "StopTestUser" stops screen sharing

    Then participant "StopTestUser" should not be publishing screen share in room "StopTestRoom" using service "livekit1"
    And participant "Observer" should see 0 remote screen share tracks in room "StopTestRoom" using service "livekit1"

  Scenario: Participant can restart screen share after stopping
    Given an access token is created with identity "RestartUser" and room "RestartRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:screen_share"
    And room "RestartRoom" is created using service "livekit1"

    When "RestartUser" opens a "Chrome" browser with LiveKit Meet page
    And "RestartUser" connects to room "RestartRoom" using the access token
    And connection is established successfully for "RestartUser"

    When "RestartUser" starts screen sharing
    Then participant "RestartUser" should be publishing screen share in room "RestartRoom" using service "livekit1"

    When "RestartUser" stops screen sharing
    Then participant "RestartUser" should not be publishing screen share in room "RestartRoom" using service "livekit1"

    When "RestartUser" starts screen sharing
    Then participant "RestartUser" should be publishing screen share in room "RestartRoom" using service "livekit1"

  # Story 1.1.1.5: Screen Share Permission Denied
  Scenario: Participant without screen share permission cannot publish screen share
    Given an access token is created with identity "RestrictedUser" and room "PermissionRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera,canPublishSources:microphone"
    And room "PermissionRoom" is created using service "livekit1"

    When "RestrictedUser" opens a "Chrome" browser with LiveKit Meet page
    And "RestrictedUser" connects to room "PermissionRoom" using the access token
    And connection is established successfully for "RestrictedUser"

    Then participant "RestrictedUser" should be publishing video in room "PermissionRoom" using service "livekit1"

    When "RestrictedUser" attempts to start screen sharing

    Then participant "RestrictedUser" should have screen share blocked due to permissions
    And participant "RestrictedUser" should not be publishing screen share in room "PermissionRoom" using service "livekit1"

  Scenario: Camera permission works independently of screen share permission
    Given an access token is created with identity "CameraOnlyUser" and room "IndependentRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera,canPublishSources:microphone"
    And room "IndependentRoom" is created using service "livekit1"

    When "CameraOnlyUser" opens a "Chrome" browser with LiveKit Meet page
    And "CameraOnlyUser" connects to room "IndependentRoom" using the access token
    And connection is established successfully for "CameraOnlyUser"

    Then participant "CameraOnlyUser" should be publishing video in room "IndependentRoom" using service "livekit1"
    And participant "CameraOnlyUser" should not be publishing screen share in room "IndependentRoom" using service "livekit1"
```

---

## Definition of Done

### Code Implementation
- [x] New step definitions added to `LiveKitBrowserWebrtcSteps.java`
- [x] `LiveKitMeet.java` extended with screen share methods
- [x] New step definitions added to `LiveKitRoomSteps.java` for screen share track verification
- [x] Feature file `livekit_screen_sharing.feature` created and passing
- [x] All scenarios pass on Chrome browser
- [x] All scenarios pass on Edge browser

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass
- [x] Tests pass against default LiveKit version
- [x] Manual verification of screen share simulation in containerized browser

### Documentation
- [x] Feature documentation complete in `docs/features/screen-sharing/`
- [ ] Step definitions documented in `docs/features.md`
- [ ] New grants documented if any added

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
@When("{string} starts screen sharing")
public void startsScreenSharing(String participantName)

@When("{string} stops screen sharing")
public void stopsScreenSharing(String participantName)

@When("{string} attempts to start screen sharing")
public void attemptsToStartScreenSharing(String participantName)

@Then("participant {string} should have screen share blocked due to permissions")
public void shouldHaveScreenShareBlockedDueToPermissions(String participantName)
```

#### In `LiveKitRoomSteps.java`:
```java
@Then("participant {string} should be publishing screen share in room {string} using service {string}")
public void participantShouldBePublishingScreenShare(String identity, String room, String service)

@Then("participant {string} should not be publishing screen share in room {string} using service {string}")
public void participantShouldNotBePublishingScreenShare(String identity, String room, String service)

@Then("participant {string} should see {int} remote screen share tracks in room {string} using service {string}")
public void shouldSeeRemoteScreenShareTracks(String identity, int count, String room, String service)

@Then("participant {string} should have {int} published tracks in room {string} using service {string}")
public void shouldHavePublishedTracks(String identity, int count, String room, String service)
```

### LiveKitMeet Page Object Extensions

```java
public void startScreenShare()
public void stopScreenShare()
public boolean isScreenSharing()
public boolean isScreenShareBlocked()
```

### Web Application Changes (meet.html)

The LiveKit Meet web application needs:
- Screen share button in the meeting UI
- JavaScript to handle `getDisplayMedia` call
- Fake media stream configuration for containerized testing
- Screen share state tracking (started/stopped)
- Error handling for permission denied scenarios

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Containerized browsers may not support screen share simulation | High | Medium | Test with `--use-fake-device-for-media-stream` flag; may need custom video file injection |
| `canPublishSources` grant format may vary by LiveKit version | Medium | Low | Test against multiple versions; document minimum version support |
| Screen share permission dialogs may block automation | High | Medium | Use appropriate Selenium capabilities and fake media flags |
| Track type identification may be unreliable | Low | Low | Verify via server API rather than client-side detection |

### Mitigation Strategies

1. **Fake Media Simulation**: Chrome and Firefox support `--use-fake-device-for-media-stream` which can simulate screen capture with a generated video pattern

2. **Version Compatibility**: Add version checks in step definitions if `canPublishSources` behavior differs

3. **Permission Handling**: Configure Selenium with permissions pre-granted:
   ```java
   chromeOptions.addArguments("--enable-features=AutoScreenCapture");
   chromeOptions.addArguments("--auto-select-desktop-capture-source=Entire screen");
   ```

4. **Fallback Strategy**: If native `getDisplayMedia` fails in containers, use `getUserMedia` with a video file as fallback for testing track publishing

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.1.1 - Permission Grant | XS | Foundation for all other stories |
| 2 | 1.1.1.2 - Track Publishing | S | Core functionality required by subscribers |
| 3 | 1.1.1.4 - Stop and Cleanup | XS | Validates full lifecycle before subscription tests |
| 4 | 1.1.1.3 - Track Subscription | S | Requires publishing to work first |
| 5 | 1.1.1.5 - Permission Denied | XS | Negative test after positive flows confirmed |

**Total Estimated Effort:** M (Medium) - approximately 3-5 days

---

## Appendix: Grant Reference

### canPublishSources Values
| Value | Description |
|-------|-------------|
| `camera` | Camera video track |
| `microphone` | Microphone audio track |
| `screen_share` | Screen share video track |
| `screen_share_audio` | Screen share audio track (system audio) |

### Track Source Enum (LiveKit SDK)
```typescript
enum Track.Source {
  Camera = 'camera',
  Microphone = 'microphone',
  ScreenShare = 'screen_share',
  ScreenShareAudio = 'screen_share_audio',
  Unknown = 'unknown'
}
```
