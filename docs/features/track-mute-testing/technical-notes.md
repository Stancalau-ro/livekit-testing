# Track Mute Testing - Technical Implementation Notes

## Architecture Overview

Track mute testing extends the existing WebRTC testing infrastructure to verify that publishers can mute/unmute their tracks and that state changes are propagated to subscribers and visible via server API. The feature integrates with:

1. **Browser Layer (LiveKitMeet)** - JavaScript client handles track enable/disable
2. **Page Object Layer (LiveKitMeet.java)** - Selenium interactions for mute controls
3. **Step Definition Layer** - Cucumber bindings for BDD scenarios
4. **Server Verification Layer** - RoomServiceClient API calls for mute state inspection
5. **Server Control Layer** - RoomServiceClient.mutePublishedTrack() for server-initiated mute

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
                         |    (TrackInfo.muted, mutePublishedTrack)|
                         +----------------------------------------+
```

---

## Component Changes

### 1. LiveKitMeet.java Extensions

The page object requires new methods for explicit audio and video mute control. The existing `toggleMute()` method is generic; we need specific methods for better control and verification.

Add to `src/main/java/ro/stancalau/test/framework/selenium/LiveKitMeet.java`:

```java
public void muteAudio() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.muteAudio();"
    );
    log.info("LiveKitMeet audio muted");
}

public void unmuteAudio() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.unmuteAudio();"
    );
    log.info("LiveKitMeet audio unmuted");
}

public boolean isAudioMuted() {
    Boolean muted = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.isAudioMuted() || false;"
    );
    return muted != null && muted;
}

public void muteVideo() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.muteVideo();"
    );
    log.info("LiveKitMeet video muted");
}

public void unmuteVideo() {
    ((JavascriptExecutor) driver).executeScript(
        "window.liveKitClient && window.liveKitClient.unmuteVideo();"
    );
    log.info("LiveKitMeet video unmuted");
}

public boolean isVideoMuted() {
    Boolean muted = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.liveKitClient && window.liveKitClient.isVideoMuted() || false;"
    );
    return muted != null && muted;
}

public boolean isRemoteTrackMuted(String identity, String trackType) {
    String script = String.format(
        "return window.liveKitClient && window.liveKitClient.isRemoteTrackMuted('%s', '%s') || false;",
        identity, trackType
    );
    Boolean muted = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
    return muted != null && muted;
}
```

### 2. JavaScript Client (livekit-client.js) Changes

Add mute-related methods to the `LiveKitMeetClient` class in `src/test/resources/web/livekit-meet/js/livekit-client.js`:

```javascript
muteAudio() {
    if (this.room && this.room.localParticipant) {
        this.room.localParticipant.setMicrophoneEnabled(false);
        addTechnicalDetail('Audio muted');
        this.updateMuteUI('audio', true);
    }
}

unmuteAudio() {
    if (this.room && this.room.localParticipant) {
        this.room.localParticipant.setMicrophoneEnabled(true);
        addTechnicalDetail('Audio unmuted');
        this.updateMuteUI('audio', false);
    }
}

isAudioMuted() {
    if (this.room && this.room.localParticipant) {
        const audioTrack = this.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Microphone);
        return audioTrack ? audioTrack.isMuted : true;
    }
    return true;
}

muteVideo() {
    if (this.room && this.room.localParticipant) {
        this.room.localParticipant.setCameraEnabled(false);
        addTechnicalDetail('Video muted');
        this.updateMuteUI('video', true);
    }
}

unmuteVideo() {
    if (this.room && this.room.localParticipant) {
        this.room.localParticipant.setCameraEnabled(true);
        addTechnicalDetail('Video unmuted');
        this.updateMuteUI('video', false);
    }
}

isVideoMuted() {
    if (this.room && this.room.localParticipant) {
        const videoTrack = this.room.localParticipant.getTrackPublication(LiveKit.Track.Source.Camera);
        return videoTrack ? videoTrack.isMuted : true;
    }
    return true;
}

isRemoteTrackMuted(identity, trackType) {
    if (!this.room) return true;

    const participant = this.room.remoteParticipants.get(identity) ||
        Array.from(this.room.remoteParticipants.values()).find(p => p.identity === identity);

    if (!participant) return true;

    const source = trackType.toLowerCase() === 'audio'
        ? LiveKit.Track.Source.Microphone
        : LiveKit.Track.Source.Camera;

    const publication = participant.getTrackPublication(source);
    return publication ? publication.isMuted : true;
}

updateMuteUI(type, muted) {
    const statusElement = document.getElementById('mute-status');
    if (statusElement) {
        statusElement.textContent = `${type} ${muted ? 'muted' : 'unmuted'}`;
    }
}
```

Also, add event listeners for remote track mute changes:

```javascript
setupRoomEventListeners() {
    // ... existing listeners ...

    this.room.on(LiveKit.RoomEvent.TrackMuted, (publication, participant) => {
        addTechnicalDetail(`Track muted: ${participant.identity} - ${publication.kind}`);
        this.onRemoteTrackMuteChanged(participant.identity, publication.kind, true);
    });

    this.room.on(LiveKit.RoomEvent.TrackUnmuted, (publication, participant) => {
        addTechnicalDetail(`Track unmuted: ${participant.identity} - ${publication.kind}`);
        this.onRemoteTrackMuteChanged(participant.identity, publication.kind, false);
    });

    this.room.on(LiveKit.RoomEvent.LocalTrackMuted, (publication) => {
        addTechnicalDetail(`Local track muted: ${publication.kind}`);
        this.updateMuteUI(publication.kind, true);
    });

    this.room.on(LiveKit.RoomEvent.LocalTrackUnmuted, (publication) => {
        addTechnicalDetail(`Local track unmuted: ${publication.kind}`);
        this.updateMuteUI(publication.kind, false);
    });
}

onRemoteTrackMuteChanged(identity, trackType, muted) {
    const key = `${identity}_${trackType}_muted`;
    this.remoteMuteStates = this.remoteMuteStates || {};
    this.remoteMuteStates[key] = muted;
}
```

### 3. New Step Definitions Required

#### LiveKitBrowserWebrtcSteps.java Additions

```java
@When("{string} mutes their audio")
public void mutesTheirAudio(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.muteAudio();
}

@When("{string} unmutes their audio")
public void unmutesTheirAudio(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.unmuteAudio();
}

@When("{string} mutes their video")
public void mutesTheirVideo(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.muteVideo();
}

@When("{string} unmutes their video")
public void unmutesTheirVideo(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.unmuteVideo();
}

@Then("{string} should see {string} audio track as muted")
public void shouldSeeAudioTrackAsMuted(String subscriber, String publisher) {
    verifyRemoteTrackMuteState(subscriber, publisher, "audio", true);
}

@Then("{string} should see {string} audio track as not muted")
public void shouldSeeAudioTrackAsNotMuted(String subscriber, String publisher) {
    verifyRemoteTrackMuteState(subscriber, publisher, "audio", false);
}

@Then("{string} should see {string} video track as muted")
public void shouldSeeVideoTrackAsMuted(String subscriber, String publisher) {
    verifyRemoteTrackMuteState(subscriber, publisher, "video", true);
}

@Then("{string} should see {string} video track as not muted")
public void shouldSeeVideoTrackAsNotMuted(String subscriber, String publisher) {
    verifyRemoteTrackMuteState(subscriber, publisher, "video", false);
}

private void verifyRemoteTrackMuteState(String subscriber, String publisher, String trackType, boolean expectedMuted) {
    LiveKitMeet meetInstance = meetInstances.get(subscriber);
    assertNotNull(meetInstance, "Meet instance should exist for " + subscriber);

    int maxAttempts = 20;
    boolean actualMuted = false;

    for (int attempt = 0; attempt < maxAttempts; attempt++) {
        actualMuted = meetInstance.isRemoteTrackMuted(publisher, trackType);
        if (actualMuted == expectedMuted) {
            return;
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    assertEquals(expectedMuted, actualMuted,
        subscriber + " should see " + publisher + " " + trackType + " track as " +
        (expectedMuted ? "muted" : "not muted"));
}

@Then("{string} local audio should show as muted")
public void localAudioShouldShowAsMuted(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    assertTrue(meetInstance.isAudioMuted(),
        participantName + " local audio should show as muted");
}

@When("{string} waits for {int} seconds")
public void waitsForSeconds(String participantName, int seconds) {
    try {
        Thread.sleep(seconds * 1000L);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

#### LiveKitRoomSteps.java Additions

```java
@Then("participant {string} audio track should be muted in room {string} using service {string}")
public void audioTrackShouldBeMuted(String identity, String room, String service) {
    verifyTrackMuteState(identity, room, service, LivekitModels.TrackType.AUDIO, true);
}

@Then("participant {string} audio track should not be muted in room {string} using service {string}")
public void audioTrackShouldNotBeMuted(String identity, String room, String service) {
    verifyTrackMuteState(identity, room, service, LivekitModels.TrackType.AUDIO, false);
}

@Then("participant {string} video track should be muted in room {string} using service {string}")
public void videoTrackShouldBeMuted(String identity, String room, String service) {
    verifyTrackMuteState(identity, room, service, LivekitModels.TrackType.VIDEO, true);
}

@Then("participant {string} video track should not be muted in room {string} using service {string}")
public void videoTrackShouldNotBeMuted(String identity, String room, String service) {
    verifyTrackMuteState(identity, room, service, LivekitModels.TrackType.VIDEO, false);
}

private void verifyTrackMuteState(String identity, String room, String service,
        LivekitModels.TrackType trackType, boolean expectedMuted) {
    int maxAttempts = 20;

    for (int attempt = 0; attempt < maxAttempts; attempt++) {
        List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

        LivekitModels.ParticipantInfo participant = participants.stream()
            .filter(p -> identity.equals(p.getIdentity()))
            .findFirst()
            .orElse(null);

        if (participant != null) {
            LivekitModels.TrackInfo track = participant.getTracksList().stream()
                .filter(t -> t.getType() == trackType)
                .filter(t -> t.getSource() == getExpectedSource(trackType))
                .findFirst()
                .orElse(null);

            if (track != null && track.getMuted() == expectedMuted) {
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

    String trackName = trackType == LivekitModels.TrackType.AUDIO ? "audio" : "video";
    fail("Participant '" + identity + "' " + trackName + " track should be " +
         (expectedMuted ? "muted" : "not muted"));
}

private LivekitModels.TrackSource getExpectedSource(LivekitModels.TrackType trackType) {
    return trackType == LivekitModels.TrackType.AUDIO
        ? LivekitModels.TrackSource.MICROPHONE
        : LivekitModels.TrackSource.CAMERA;
}

@When("the server mutes {string} audio track in room {string} using service {string}")
public void serverMutesAudioTrack(String identity, String room, String service) {
    serverMuteTrack(identity, room, service, LivekitModels.TrackType.AUDIO, true);
}

@When("the server mutes {string} video track in room {string} using service {string}")
public void serverMutesVideoTrack(String identity, String room, String service) {
    serverMuteTrack(identity, room, service, LivekitModels.TrackType.VIDEO, true);
}

private void serverMuteTrack(String identity, String room, String service,
        LivekitModels.TrackType trackType, boolean mute) {
    LiveKitContainer container = containerStateManager.getContainer(service, LiveKitContainer.class);
    RoomServiceClient roomClient = container.getRoomServiceClient();

    List<LivekitModels.ParticipantInfo> participants = getParticipantInfo(service, room);

    LivekitModels.ParticipantInfo participant = participants.stream()
        .filter(p -> identity.equals(p.getIdentity()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Participant not found: " + identity));

    LivekitModels.TrackInfo track = participant.getTracksList().stream()
        .filter(t -> t.getType() == trackType)
        .filter(t -> t.getSource() == getExpectedSource(trackType))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Track not found for: " + identity));

    try {
        roomClient.mutePublishedTrack(room, identity, track.getSid(), mute).get();
        log.info("Server {} {} track for {} in room {}",
            mute ? "muted" : "unmuted", trackType, identity, room);
    } catch (Exception e) {
        throw new RuntimeException("Failed to mute track via server", e);
    }
}
```

---

## Data Flow

### Client-Side Mute Operation

```
1. User Step: "AudioMuter" mutes their audio
       |
       v
2. LiveKitMeet.muteAudio()
       |
       v
3. JavaScript: window.liveKitClient.muteAudio()
       |
       v
4. LiveKit SDK: room.localParticipant.setMicrophoneEnabled(false)
       |
       v
5. WebRTC: MediaStreamTrack.enabled = false
       |
       v
6. LiveKit Server: Receives mute signal, updates TrackInfo.muted = true
       |
       v
7. Server broadcasts mute event to all subscribers
       |
       v
8. Other participants receive TrackMuted event
```

### Server-Initiated Mute Operation

```
1. Step: "the server mutes 'Publisher' audio track in room 'Room' using service 'livekit1'"
       |
       v
2. LiveKitRoomSteps.serverMuteTrack()
       |
       v
3. RoomServiceClient.mutePublishedTrack(room, identity, trackSid, true)
       |
       v
4. LiveKit Server: Sets TrackInfo.muted = true
       |
       v
5. Server sends LocalTrackMuted event to publisher
       |
       v
6. Publisher's SDK updates local track state
       |
       v
7. Server broadcasts TrackMuted event to all subscribers
```

### Mute Verification Flow

```
Test Framework                    LiveKit Server              Browser Client
     |                                  |                            |
     | verifyTrackMuteState()           |                            |
     |--------------------------------->|                            |
     | RoomServiceClient                |                            |
     | .listParticipants(room)          |                            |
     |                                  |                            |
     |<---------------------------------|                            |
     | ParticipantInfo with TrackInfo   |                            |
     | containing muted = true/false    |                            |
     |                                  |                            |
     | Assert track.getMuted() matches  |                            |
     | expected value                   |                            |
```

---

## API Integration Points

### LiveKit Server SDK Methods

| Method | Return Type | Usage |
|--------|-------------|-------|
| `RoomServiceClient.listParticipants(roomName)` | `List<ParticipantInfo>` | Get all participants with their tracks |
| `RoomServiceClient.mutePublishedTrack(room, identity, trackSid, muted)` | `Void` | Server-initiated mute |
| `ParticipantInfo.getTracksList()` | `List<TrackInfo>` | Get tracks for a participant |
| `TrackInfo.getMuted()` | `boolean` | Check if track is muted |
| `TrackInfo.getSid()` | `String` | Get track SID for server operations |

### LiveKit Client SDK Methods (JavaScript)

| Method | Description |
|--------|-------------|
| `room.localParticipant.setMicrophoneEnabled(enabled)` | Mute/unmute audio |
| `room.localParticipant.setCameraEnabled(enabled)` | Mute/unmute video |
| `publication.isMuted` | Check mute state of a publication |
| `RoomEvent.TrackMuted` | Event fired when remote track is muted |
| `RoomEvent.TrackUnmuted` | Event fired when remote track is unmuted |
| `RoomEvent.LocalTrackMuted` | Event fired when local track is muted by server |
| `RoomEvent.LocalTrackUnmuted` | Event fired when local track is unmuted by server |

---

## Testing Strategy

### Implementation Order

| Phase | Story | Focus | Verification Method |
|-------|-------|-------|---------------------|
| 1 | 1.1.7.1 | Audio mute client-side | Server API: `TrackInfo.getMuted()` |
| 2 | 1.1.7.2 | Video mute client-side | Server API: `TrackInfo.getMuted()` |
| 3 | 1.1.7.3 | Mute propagation | JavaScript: `isRemoteTrackMuted()` |
| 4 | 1.1.7.4 | Server mute | Server API + Client event verification |
| 5 | 1.1.7.5 | Cross-browser | Same methods, different browser configs |
| 6 | 1.1.7.6 | Unmute verification | Server API: publishing state |

### Component Isolation Testing

#### Audio Mute (Story 1.1.7.1)
1. Create room and token with audio publish permission
2. Open browser with LiveKitMeet
3. Connect and verify audio is publishing
4. Call `muteAudio()`
5. Verify via Server API: `TrackInfo.getMuted() == true`
6. Call `unmuteAudio()`
7. Verify via Server API: `TrackInfo.getMuted() == false`

#### Server Mute (Story 1.1.7.4)
1. Complete audio publish setup
2. Call `RoomServiceClient.mutePublishedTrack(room, identity, trackSid, true)`
3. Poll Server API until `TrackInfo.getMuted() == true`
4. Verify client received `LocalTrackMuted` event
5. Verify subscriber received `TrackMuted` event

---

## Known Limitations and Workarounds

### Mute State Propagation Delay

Mute state changes may not appear immediately in the Server API due to network latency and server processing time.

**Workaround**: Use polling pattern with reasonable timeout:

```java
private static final int MAX_POLLING_ATTEMPTS = 20;
private static final int POLLING_INTERVAL_MS = 500;

for (int attempt = 0; attempt < MAX_POLLING_ATTEMPTS; attempt++) {
    TrackInfo track = getTrackInfo(identity, room, service, trackType);
    if (track != null && track.getMuted() == expectedMuted) {
        return;
    }
    Thread.sleep(POLLING_INTERVAL_MS);
}
throw new AssertionError("Mute state did not match expected within timeout");
```

### Client Unmute After Server Mute

When the server mutes a participant, the client's ability to unmute may depend on server configuration and SDK version. This behavior should be documented rather than assumed.

**Workaround**: Test both scenarios and document actual behavior per LiveKit version.

### Fake Media Stream Mute Detection

With fake media streams in containerized browsers, there may be no actual audio/video being produced. However, the mute state at the SDK level should still be accurate.

**Workaround**: Rely on SDK mute state via Server API, not actual media analysis.

---

## File Changes Summary

| File | Changes Required |
|------|------------------|
| `LiveKitMeet.java` | Add 7 new methods for mute control |
| `livekit-client.js` | Add mute methods and event handlers |
| `LiveKitBrowserWebrtcSteps.java` | Add 10+ new step definitions |
| `LiveKitRoomSteps.java` | Add 6+ new step definitions for mute verification |
| `livekit_track_mute.feature` | New feature file with 12+ scenarios |

---

## Existing Infrastructure Leverage

The mute implementation builds on existing components:

1. **toggleMute() method** - Already exists in LiveKitMeet.java, can be refactored
2. **Track.java** - Webhook model already has `muted` field
3. **RoomServiceClient** - Already used for track verification in `LiveKitRoomSteps`
4. **Polling pattern** - Established in existing step definitions
5. **Browser setup** - Chrome/Firefox configurations already work

---

## Version Compatibility

| LiveKit Version | Mute Support | Notes |
|-----------------|--------------|-------|
| v1.5.0+ | Full support | Server-initiated mute available |
| v1.4.x | Basic mute | Client-side mute only |
| < v1.4.0 | Limited | Test required |

The `mutePublishedTrack` API was introduced in LiveKit server v1.5.0.
