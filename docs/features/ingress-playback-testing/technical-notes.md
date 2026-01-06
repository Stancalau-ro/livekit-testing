# Ingress Stream Playback Testing - Technical Notes

## Implementation Strategy

### Integration with Existing Infrastructure

This feature builds on the ingress infrastructure from Story 1.1.8 and reuses patterns from WebRTC playback testing (Story 1.1.1). The key integration points are:

1. **Ingress Steps** (`LiveKitIngressSteps.java`): Reuse existing steps for ingress creation, streaming, and lifecycle
2. **Room Steps** (`LiveKitRoomSteps.java`): Reuse existing steps for participant and track verification
3. **Browser Steps** (`BrowserLifecycleSteps.java`, `RoomConnectionSteps.java`): Reuse existing browser management
4. **New Steps**: Minimal new steps required for ingress-specific track verification

### Step Reuse Analysis

| Required Capability | Existing Step | Status |
|---------------------|---------------|--------|
| Create ingress | `"{identity}" creates an RTMP ingress to room "{room}"` | Exists |
| Start streaming | `"{identity}" starts streaming via RTMP` | Exists |
| Check publishing | `the ingress for "{identity}" is publishing within {N} seconds` | Exists |
| Open browser | `"{name}" opens a "{browser}" browser with LiveKit Meet page` | Exists |
| Connect to room | `"{name}" connects to room "{room}" using the access token` | Exists |
| Check participant count | `room "{room}" should have {N} active participants` | Exists |
| Check remote tracks | `participant "{id}" should have {N} remote video tracks available` | Exists |
| Check video publishing | `participant "{id}" should be publishing video` | Exists |
| Check simulcast layers | `participant "{id}" video track should have "{expr}" layers` | Exists |
| Check layer resolutions | `participant "{id}" video layers should have different resolutions` | Exists |
| Stop streaming | `"{identity}" stops streaming` | Exists |
| Close browser | `"{name}" closes the browser` | Exists |

### New Step Definitions Required

Only a few new steps are needed:

```java
// In LiveKitRoomSteps.java

@Then("participant {string} should have published audio track in room {string} using service {string}")
public void participantShouldHavePublishedAudioTrackInRoomUsingService(
    String participantIdentity, String roomName, String serviceName) {
  boolean success = BrowserPollingHelper.pollForCondition(() -> {
    LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
    return p.getTracksList().stream()
        .anyMatch(t -> t.getType() == LivekitModels.TrackType.AUDIO);
  });
  assertTrue(success, "Participant '" + participantIdentity + "' should have published audio track");
}

@Then("participant {string} video track should have source {string} in room {string} using service {string}")
public void participantVideoTrackShouldHaveSourceInRoomUsingService(
    String participantIdentity, String source, String roomName, String serviceName) {
  LivekitModels.TrackSource expectedSource = LivekitModels.TrackSource.valueOf(source);
  boolean success = BrowserPollingHelper.pollForCondition(() -> {
    LivekitModels.ParticipantInfo p = findParticipantOrFail(serviceName, roomName, participantIdentity);
    return p.getTracksList().stream()
        .filter(t -> t.getType() == LivekitModels.TrackType.VIDEO)
        .anyMatch(t -> t.getSource() == expectedSource);
  });
  assertTrue(success, "Video track should have source " + source);
}

@Then("participant {string} video track should have maximum width {int} in room {string} using service {string}")
public void participantVideoTrackShouldHaveMaximumWidthInRoomUsingService(
    String participantIdentity, int maxWidth, String roomName, String serviceName) {
  boolean success = BrowserPollingHelper.pollForCondition(() -> {
    List<LivekitModels.VideoLayer> layers = getVideoLayersForParticipant(serviceName, roomName, participantIdentity);
    if (layers.isEmpty()) return false;
    int actualMaxWidth = layers.stream()
        .mapToInt(LivekitModels.VideoLayer::getWidth)
        .max()
        .orElse(0);
    return actualMaxWidth <= maxWidth && actualMaxWidth > 0;
  });
  assertTrue(success, "Video track should have maximum width " + maxWidth);
}
```

## Timing and Polling Considerations

### Ingress Transcoding Latency

Ingress streams have additional latency due to:
1. RTMP connection establishment (1-2 seconds)
2. Transcoding startup (3-5 seconds)
3. Buffer filling (2-3 seconds)
4. WebRTC publishing setup (1-2 seconds)

**Total expected latency: 7-12 seconds**

Recommendation: Use 30-45 second timeouts for ingress publishing verification.

### Subscriber Track Reception

After ingress publishes, subscribers need time to:
1. Receive track announcement (~100ms)
2. Establish subscription (~500ms)
3. Receive first frames (~200ms)

**Total expected latency: ~1 second** after ingress publishes

Recommendation: Add 2-3 second buffer after ingress publishing before verifying subscriber tracks.

### Polling Configuration

Use existing `BrowserPollingHelper` patterns:
- Default: 20 attempts, 500ms interval (10 seconds total)
- Extended: 40 attempts, 1000ms interval (40 seconds total) for ingress scenarios

## Test Resource Requirements

### Container Resources

Each ingress playback scenario requires:
- 1x LiveKit server container (~500MB RAM)
- 1x Redis container (~50MB RAM)
- 1x Ingress container (~1GB RAM, 2 CPU cores for transcoding)
- 1x FFmpeg container (~200MB RAM)
- 1-3x Selenium browser containers (~500MB RAM each)

**Minimum total: ~3GB RAM** for single-subscriber scenario
**Maximum total: ~4.5GB RAM** for triple-subscriber scenario

### Docker Network

All containers must share the same Docker network for container-to-container communication. The existing `ContainerStateManager.getOrCreateNetwork()` handles this.

### Test Duration

| Scenario Type | Estimated Duration |
|---------------|-------------------|
| Single subscriber | 45-60 seconds |
| Two subscribers | 60-90 seconds |
| Three subscribers | 90-120 seconds |
| With simulcast verification | +15-20 seconds |

## Error Handling

### Common Failure Modes

1. **Ingress fails to reach publishing state**
   - Check FFmpeg logs for connection errors
   - Verify RTMP URL and stream key
   - Check Ingress container health

2. **Subscriber does not receive tracks**
   - Verify ingress participant exists in room
   - Check subscriber has canSubscribe permission
   - Verify WebRTC connection established

3. **Track dimensions incorrect**
   - FFmpeg may not output exact requested resolution
   - Transcoding presets may adjust dimensions
   - Layer selection may affect received dimensions

### Diagnostic Logging

Add logging at key points:
```java
log.info("Ingress {} started streaming to room {}", identity, roomName);
log.info("Subscriber {} connected to room {}", subscriberName, roomName);
log.info("Subscriber {} sees {} remote video tracks", subscriberName, trackCount);
log.info("Ingress {} video track has {} layers, max resolution: {}x{}",
    identity, layerCount, maxWidth, maxHeight);
```

## FFmpeg Stream Configuration

### Default Test Pattern

```bash
ffmpeg -re -f lavfi -i testsrc2=size=1280x720:rate=30 \
       -f lavfi -i sine=frequency=440:sample_rate=48000 \
       -c:v libx264 -preset veryfast -tune zerolatency \
       -c:a aac -b:a 128k \
       -f flv rtmp://ingress:1935/live/STREAM_KEY
```

### Resolution Options

| Resolution | FFmpeg Parameter | Use Case |
|------------|------------------|----------|
| 720p | `size=1280x720` | Standard testing |
| 1080p | `size=1920x1080` | High-quality testing |
| 540p | `size=960x540` | Bandwidth testing |
| 480p | `size=854x480` | Low-bandwidth testing |

### Existing FFmpegContainer Methods

The `FFmpegContainer` class already supports resolution configuration:
```java
FFmpegContainer.createRtmpStream(
    containerAlias,
    network,
    rtmpUrl,
    streamKey,
    durationSeconds,
    resolution,  // e.g., "1280x720"
    framerate,   // e.g., 30
    logPath
);
```

## Test Isolation

### Scenario Independence

Each scenario should:
1. Create its own room with unique name
2. Use unique participant identities
3. Clean up all ingresses in @After hook
4. Clean up all FFmpeg containers in @After hook
5. Close all browser sessions in @After hook

### Cleanup Order

```
1. Stop FFmpeg streams
2. Delete ingresses
3. Close browser sessions
4. Stop containers (handled by framework)
```

The existing `LiveKitIngressSteps.tearDownLiveKitIngressSteps()` handles ingress and FFmpeg cleanup.

## Cross-Browser Considerations

### Chrome
- Primary test browser
- Best WebRTC compatibility
- Use Selenium `chrome` container

### Firefox
- Secondary test browser
- Slight differences in track reporting
- Use Selenium `firefox` container
- May have different simulcast behavior

### Edge
- Lower priority for ingress testing
- Similar to Chrome (Chromium-based)
- Use if Chrome/Firefox scenarios pass

## Integration with CI/CD

### Parallel Execution

Ingress scenarios are resource-intensive. Recommendations:
- Do not run ingress scenarios in parallel
- Tag ingress scenarios with `@Ingress` for filtering
- Consider separate CI job for ingress tests

### Container Preloading

Ensure these images are preloaded before test execution:
- `livekit/ingress:{version}`
- `linuxserver/ffmpeg:latest`
- `selenium/standalone-chrome`
- `selenium/standalone-firefox`

### Failure Artifacts

On failure, collect:
- FFmpeg container logs
- Ingress container logs
- LiveKit server logs
- Browser screenshots
- Browser console logs
