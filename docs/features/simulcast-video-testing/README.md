# Feature: Simulcast Video Testing

## Status
**Complete** | Story 1.1.2 | Size: M (Medium)

## Problem Statement
The LiveKit testing framework currently has basic simulcast support through the CLI publisher configuration (`simulcast:true`), but lacks comprehensive test coverage for verifying simulcast video behavior. Simulcast is a critical WebRTC optimization that publishes multiple quality layers (low, medium, high) of a video stream, allowing subscribers to receive the most appropriate quality based on their bandwidth and viewing requirements. Without proper testing:
- There is no verification that multiple video layers are actually published
- Layer selection behavior cannot be validated
- Quality degradation under bandwidth constraints is untested
- Adaptive streaming functionality is not exercised

## User Value
Test developers need to verify simulcast video publishing to ensure their LiveKit integrations properly support adaptive quality streaming. This testing capability enables:
- Confidence that simulcast is correctly configured and working
- Verification that multiple quality layers exist and differ in resolution/bitrate
- Validation of subscriber layer selection via SDK
- Detection of regressions in simulcast behavior across LiveKit versions
- Quality assurance for bandwidth-constrained scenarios

## Success Metrics
- All simulcast scenarios pass consistently across supported LiveKit versions
- Multiple video layers (low, medium, high) are detectable via server API
- Layer quality differences are measurable (resolution, bitrate)
- Subscriber layer selection changes are verifiable
- Tests handle simulcast-disabled scenarios gracefully

## Scope

### In Scope
- Enabling simulcast in video publish settings via browser
- Verifying multiple video layers exist via server API
- Comparing quality metrics between layers
- Testing layer selection via SDK preference settings
- Verifying layer count differences between simulcast on/off
- Basic bandwidth constraint scenarios using CLI publisher

### Out of Scope
- Dynacast/automatic bandwidth adaptation (covered by Story 1.1.3)
- Network condition simulation via traffic control (future epic)
- Real bandwidth throttling in container environment
- Audio simulcast (not applicable - audio uses single track)
- Screen share simulcast (separate consideration)
- Client-side quality measurement (server API only)

## Dependencies

### Existing Infrastructure (Required)
- `LiveKitContainer` - Docker-based LiveKit server management
- `SeleniumConfig` - Cross-platform WebDriver configuration
- `AccessTokenStateManager` - Token creation with grants
- `ContainerStateManager` - Container lifecycle management
- `LiveKitMeet` page object - Browser-based room interaction
- `LiveKitRoomSteps` - Server-side room/participant verification
- `CLIPublisherContainer` - CLI-based video publishing with simulcast support
- `Layer.java` - Existing webhook model for video layer data

### New Components (To Be Created)
- Simulcast step definitions in `LiveKitBrowserWebrtcSteps`
- Extended `LiveKitRoomSteps` for layer verification
- LiveKitMeet page object extensions for simulcast settings
- New feature file `livekit_simulcast.feature`

### Technical Dependencies
- LiveKit server simulcast support (enabled by default in modern versions)
- Browser WebRTC simulcast capability
- Server API access to track layer information via RoomServiceClient
- LiveKit protobuf models for VideoLayer inspection

## Related Documentation
- [LiveKit Simulcast Documentation](https://docs.livekit.io/realtime/client/publish/#simulcast)
- [WebRTC Simulcast Specification](https://www.w3.org/TR/webrtc/#simulcast-functionality)
- [LiveKit Video Quality Settings](https://docs.livekit.io/client-sdk-js/interfaces/VideoCaptureOptions.html)

## Open Questions
1. What is the minimum resolution threshold to generate all three simulcast layers?
2. Does the containerized Chrome browser's fake video stream produce sufficient resolution for simulcast?
3. How long does it take for all simulcast layers to stabilize after track publication?
4. Can we access layer information directly via gRPC/protobuf or only through webhooks?
5. What is the behavior when simulcast is disabled at the room level vs track level?
