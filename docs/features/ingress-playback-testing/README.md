# Feature: Ingress Stream Playback Testing

## Status
**COMPLETE** | Story 1.1.9 | Size: M (Medium)

**Implementation Status:** Fully implemented in `livekit_ingress.feature` (scenarios 55-144)

### What Was Implemented:
- **`isReceivingVideoFrom` assertion**: Verifies subscription status, track availability, frame dimensions, and stream state
- **Browser subscriber scenarios**: Chrome can subscribe to ingress video tracks
- **Dimension validation**: Tests confirm video dimensions match ingress presets
- **Multi-subscriber testing**: Multiple browsers can receive the same ingress stream
- **Track lifecycle testing**: Subscription cleanup verified when ingress stops

### Browser Support Note:
Firefox is excluded from RTMP ingress tests due to H264 codec licensing limitations in Linux Selenium containers. Chrome has built-in H264 support and is the primary testing browser for ingress scenarios.

## Implementation Approach

> **IMPORTANT:** These scenarios are **additions to the existing `livekit_ingress.feature`** file, NOT a separate feature file.
>
> **Why consolidate?**
> - Shares the same Background setup (Redis, LiveKit, Ingress containers)
> - Avoids duplicate container orchestration
> - Tests the same ingress flow end-to-end in one place
> - Reduces test execution time by reusing infrastructure

## Problem Statement

The current ingress testing scenarios (Story 1.1.8) verify CRUD operations and state transitions for ingress streams, but there is a critical gap: **no scenarios verify that browser participants can actually subscribe to and receive media from ingress streams**.

Without playback testing:
- There is no verification that transcoded ingress media is playable by WebRTC subscribers
- Video dimensions and quality from ingress cannot be validated against configuration
- Multi-subscriber scenarios for ingress streams are untested
- Track metadata (codec, dimensions, simulcast layers) from ingress is unverified
- End-to-end media flow from RTMP source to browser subscriber is not validated

## User Value

Test developers need to verify that ingress streams produce playable media in rooms to ensure:

- Confidence that RTMP/FFmpeg streams are correctly transcoded and delivered to browsers
- Verification that video dimensions match ingress transcoding configuration
- Validation of audio/video track availability for subscribers
- Testing of multi-subscriber scenarios common in broadcast applications
- Detection of regressions in ingress-to-WebRTC transcoding across LiveKit versions
- Quality assurance for OBS/encoder integrations

## Success Metrics

- Browser subscriber can join room and receive ingress video track
- Browser subscriber can receive ingress audio track
- Video dimensions match expected values from transcoding preset
- Multiple browsers can simultaneously receive ingress stream
- Track metadata (codec, dimensions) is accessible and correct
- Ingress track disappearance is correctly detected when stream stops

## Scope

### In Scope

- Browser subscriber connecting to room with active ingress
- Video track subscription and verification from ingress participant
- Audio track subscription and verification from ingress participant
- Video dimensions validation against ingress configuration
- Multi-subscriber scenarios (2-3 browsers)
- Track metadata verification via server API
- Track disappearance detection when ingress stops
- Cross-browser testing (Chrome, Firefox)

### Out of Scope

- Simulcast layer selection from ingress (separate story if needed)
- Frame-level video quality analysis (e.g., PSNR/SSIM)
- Audio quality measurement (waveform analysis)
- Network condition testing during playback
- Playback latency measurement
- Recording ingress streams via egress (Story 1.1.10)

## Dependencies

### Existing Infrastructure (Required)

- `IngressContainer` - Ingress service container (from Story 1.1.8)
- `FFmpegContainer` - RTMP stream generation (from Story 1.1.8)
- `IngressStateManager` - Ingress tracking and cleanup
- `LiveKitMeet` - Browser interaction and track verification
- `RoomServiceClient` - Room and participant API access
- `WebDriverStateManager` - Browser session management
- `VideoQualityStateManager` - Track quality verification

### From Story 1.1.8 (Ingress Infrastructure)

- Step: `Given an Ingress service is running with service name "{name}"`
- Step: `When "{identity}" creates an RTMP ingress to room "{room}"`
- Step: `When "{identity}" starts streaming via RTMP`
- Step: `Then the ingress for "{identity}" should be publishing`
- Step: `Then participant "{identity}" should appear in room "{room}"`

### New Components (To Be Created)

| Component | Location | Description |
|-----------|----------|-------------|
| New step definitions | `LiveKitIngressSteps.java` | Subscriber-specific ingress steps |
| Track verification logic | `LiveKitRoomSteps.java` | Ingress track metadata verification |

## Architecture Overview

```
+------------------+     +-------------------+     +------------------+
|   Feature File   | --> | Step Definitions  | --> | State Managers   |
| (Gherkin/BDD)    |     | (Cucumber)        |     | & Page Objects   |
+------------------+     +-------------------+     +------------------+
                                 |                         |
                                 v                         v
                    +-------------------------+    +------------------+
                    | LiveKitMeet (Browser)   |    | RoomServiceClient|
                    | - Track subscription    |    | - Track metadata |
                    | - Quality verification  |    | - Participant info|
                    +-------------------------+    +------------------+
                                 |                         |
                    +------------+------------+            |
                    v            v            v            v
             +---------+  +-----------+  +--------+  +---------+
             | Browser |  | Ingress   |  | FFmpeg |  | LiveKit |
             | (Sub)   |  | Container |  | Stream |  | Room    |
             +---------+  +-----------+  +--------+  +---------+
                    |           |             |            |
                    +<----------+-------------+------------+
                        Subscriber receives ingress media
```

## Related Documentation

- [LiveKit Ingress Overview](https://docs.livekit.io/transport/media/ingress-egress/ingress/)
- [Track Subscription](https://docs.livekit.io/realtime/client/receive/)
- [Video Quality](https://docs.livekit.io/realtime/client/video-quality/)
- Existing documentation: `docs/features/ingress-testing/README.md`

## Key Implementation Considerations

### Track Verification Strategy

1. **Server-side verification** via `RoomServiceClient.listParticipants()`:
   - Verify ingress participant has published tracks
   - Check track types (VIDEO, AUDIO)
   - Inspect track metadata (dimensions, codec, simulcast)

2. **Browser-side verification** via `LiveKitMeet`:
   - Verify remote tracks are received
   - Check video dimensions match expected
   - Confirm audio track is playing

### Timing Considerations

- Ingress transcoding introduces latency (5-15 seconds typical)
- Browser subscription requires track publication to complete
- Use polling with appropriate timeouts (30-45 seconds for ingress scenarios)

### Resolution Mapping

| Ingress Preset | Expected Width | Expected Height |
|----------------|----------------|-----------------|
| H264_720P_30FPS_3_LAYERS | 1280 | 720 |
| H264_1080P_30FPS_3_LAYERS | 1920 | 1080 |
| H264_540P_25FPS_2_LAYERS | 960 | 540 |
| Default (no preset) | Varies | Varies |

## Version Compatibility

Ingress playback testing requires:
- LiveKit Server: v1.8.x or later (recommended)
- Ingress Service: v1.4.x or later
- JS Client: v2.x (for proper track subscription)

## Open Questions

1. **Track Naming**: How do we identify ingress tracks vs other participant tracks?
   - Answer: Use participant identity from ingress configuration

2. **Quality Verification Depth**: Do we need frame-level analysis or is dimension/codec sufficient?
   - Recommendation: Start with dimension/codec verification; frame analysis can be added later

3. **Browser Focus**: Which browsers are priority for ingress playback testing?
   - Recommendation: Chrome primary, Firefox secondary
