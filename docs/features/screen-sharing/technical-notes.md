# Screen Sharing Testing - Technical Notes

## Implementation Considerations

### Browser Screen Share Simulation

#### Chrome Configuration
Chrome's containerized environment requires specific flags for screen share simulation:

```java
chromeOptions.addArguments("--use-fake-device-for-media-stream");
chromeOptions.addArguments("--use-fake-ui-for-media-stream");
chromeOptions.addArguments("--auto-select-desktop-capture-source=Entire screen");
chromeOptions.addArguments("--enable-features=AutoScreenCapture");
```

The existing `SeleniumConfig.java` already includes `use-fake-device-for-media-stream` but may need extension for screen share.

#### Firefox Configuration
Firefox preferences for screen share:

```java
firefoxProfile.setPreference("media.navigator.permission.disabled", true);
firefoxProfile.setPreference("media.getusermedia.screensharing.enabled", true);
firefoxProfile.setPreference("media.getusermedia.screensharing.allowed_domains", "localhost,webserver");
```

### LiveKitMeet Page Object Extensions

#### New Methods Required

```java
public void startScreenShare() {
    WebElement screenShareBtn = driver.findElement(By.id("screenShareBtn"));
    screenShareBtn.click();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(driver -> {
        Boolean isSharing = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.liveKitClient && window.liveKitClient.isScreenSharing();"
        );
        return isSharing != null && isSharing;
    });
}

public void stopScreenShare() {
    WebElement screenShareBtn = driver.findElement(By.id("screenShareBtn"));
    if (isScreenSharing()) {
        screenShareBtn.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(driver -> !isScreenSharing());
    }
}

public boolean isScreenSharing() {
    Boolean isSharing = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.isScreenSharing() || false;"
    );
    return isSharing != null && isSharing;
}

public boolean isScreenShareBlocked() {
    Boolean blocked = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.screenSharePermissionDenied || false;"
    );
    return blocked != null && blocked;
}
```

### Web Application (meet.html) Updates

#### JavaScript Screen Share Handler

```javascript
let screenShareTrack = null;

async function toggleScreenShare() {
    if (screenShareTrack) {
        await stopScreenShare();
    } else {
        await startScreenShare();
    }
}

async function startScreenShare() {
    try {
        screenShareTrack = await room.localParticipant.setScreenShareEnabled(true);
        window.screenShareActive = true;
        window.screenSharePermissionDenied = false;
        updateScreenShareButton(true);
    } catch (error) {
        console.error('Screen share failed:', error);
        if (error.message.includes('permission') || error.message.includes('denied')) {
            window.screenSharePermissionDenied = true;
        }
        window.lastError = 'Screen share error: ' + error.message;
    }
}

async function stopScreenShare() {
    try {
        await room.localParticipant.setScreenShareEnabled(false);
        screenShareTrack = null;
        window.screenShareActive = false;
        updateScreenShareButton(false);
    } catch (error) {
        console.error('Stop screen share failed:', error);
    }
}

function isScreenSharing() {
    return screenShareTrack !== null && window.screenShareActive;
}

window.liveKitClient = window.liveKitClient || {};
window.liveKitClient.isScreenSharing = isScreenSharing;
```

### Server-Side Track Verification

#### Track Type Detection

LiveKit's `TrackInfo` protobuf includes a `source` field that identifies track type:

```java
public boolean isScreenShareTrack(LivekitModels.TrackInfo track) {
    return track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE ||
           track.getSource() == LivekitModels.TrackSource.SCREEN_SHARE_AUDIO;
}

public long countScreenShareTracks(List<LivekitModels.ParticipantInfo> participants, String identity) {
    return participants.stream()
        .filter(p -> identity.equals(p.getIdentity()))
        .flatMap(p -> p.getTracksList().stream())
        .filter(this::isScreenShareTrack)
        .count();
}
```

### Permission Grant Handling

#### canPublishSources Grant Format

The `canPublishSources` grant restricts which track sources a participant can publish. In the LiveKit Java SDK:

```java
VideoGrant videoGrant = new VideoGrant();
videoGrant.setCanPublish(true);
videoGrant.setCanPublishSources(Arrays.asList(
    TrackSource.SCREEN_SHARE,
    TrackSource.CAMERA,
    TrackSource.MICROPHONE
));
```

#### AccessTokenStateManager Extension

May need to add support for `canPublishSources` if not already present:

```java
public AccessToken createTokenWithPublishSources(
    String identity,
    String roomName,
    List<String> sources
) {
    VideoGrant grant = new VideoGrant();
    grant.setRoomJoin(true);
    grant.setRoom(roomName);
    grant.setCanPublish(true);

    List<TrackSource> trackSources = sources.stream()
        .map(this::parseTrackSource)
        .collect(Collectors.toList());
    grant.setCanPublishSources(trackSources);

    return new AccessToken(apiKey, apiSecret)
        .setIdentity(identity)
        .addGrant(grant);
}

private TrackSource parseTrackSource(String source) {
    return switch (source.toLowerCase()) {
        case "camera" -> TrackSource.CAMERA;
        case "microphone" -> TrackSource.MICROPHONE;
        case "screen_share" -> TrackSource.SCREEN_SHARE;
        case "screen_share_audio" -> TrackSource.SCREEN_SHARE_AUDIO;
        default -> throw new IllegalArgumentException("Unknown track source: " + source);
    };
}
```

### Test Data Considerations

#### Participant Naming Convention
Use descriptive names that indicate role in test:
- `ScreenPublisher` - participant who will share screen
- `ScreenViewer` - subscriber to screen share
- `RestrictedUser` - participant without screen share permission

#### Room Naming Convention
Use test-specific room names:
- `ScreenShareRoom` - basic screen share tests
- `SubTestRoom` - subscription tests
- `PermissionRoom` - permission enforcement tests

### Polling and Timing

#### Track Publication Timing
Screen share track publication may take 1-3 seconds. Use polling pattern:

```java
private static final int MAX_ATTEMPTS = 20;
private static final int POLLING_INTERVAL_MS = 500;

public void waitForScreenShareTrack(String identity, String roomName, String serviceName) {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(serviceName, roomName);

        long screenShareCount = participants.stream()
            .filter(p -> identity.equals(p.getIdentity()))
            .flatMap(p -> p.getTracksList().stream())
            .filter(t -> t.getSource() == LivekitModels.TrackSource.SCREEN_SHARE)
            .count();

        if (screenShareCount >= 1) {
            return;
        }

        try {
            Thread.sleep(POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    fail("Screen share track not published within timeout");
}
```

### Error Handling

#### Common Failure Scenarios

| Scenario | Detection | Handling |
|----------|-----------|----------|
| Permission denied | `window.screenSharePermissionDenied` | Assert blocked state |
| Browser not supported | `getDisplayMedia` undefined | Skip test or fail gracefully |
| Track publish timeout | Polling exhausted | Fail with descriptive message |
| Server-side rejection | Track count remains 0 | Check permission grant |

#### Browser Console Error Capture

Extend error capture in `LiveKitMeet.getPageErrorDetails()`:

```java
String screenShareErrors = (String) ((JavascriptExecutor) driver).executeScript(
    "return window.screenShareError || ''"
);
if (screenShareErrors != null && !screenShareErrors.trim().isEmpty()) {
    errorDetails.append("Screen share error: ").append(screenShareErrors);
}
```

### VNC Recording Considerations

Screen share tests should have VNC recording enabled to capture:
- Screen share button interactions
- Visual confirmation of screen share track
- Permission dialog handling (if any)

Recording mode `all` is recommended for initial development and debugging.

---

## File Changes Summary

| File | Changes |
|------|---------|
| `LiveKitBrowserWebrtcSteps.java` | Add 4 new step definitions for screen share |
| `LiveKitRoomSteps.java` | Add 4 new step definitions for track verification |
| `LiveKitMeet.java` | Add screen share control methods |
| `AccessTokenStateManager.java` | May need `canPublishSources` support |
| `SeleniumConfig.java` | Add screen share browser flags if needed |
| `meet.html` (web resources) | Add screen share UI and handlers |
| `livekit_screen_sharing.feature` | New feature file |
| `features.md` | Document new steps |

---

## Testing Strategy

### Unit Tests
- Test `canPublishSources` grant parsing in `StringParsingUtilsTest`
- Test track source detection logic

### Integration Tests (BDD)
- Run full feature file against default LiveKit version
- Run cross-browser scenarios (Chrome, Firefox)

### Manual Verification
- Verify fake screen share media in containerized browser
- Confirm VNC recording captures screen share visually

---

## Version Compatibility Notes

| LiveKit Version | canPublishSources Support | Notes |
|-----------------|---------------------------|-------|
| v1.5.0+ | Full support | Recommended minimum |
| v1.4.x | Limited | May not enforce all sources |
| < v1.4.0 | Unknown | Test required |

Check release notes for specific version capabilities before setting minimum supported version.
