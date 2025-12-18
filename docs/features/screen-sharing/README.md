# Feature: Screen Sharing Testing

## Status
**Complete** | Story 1.1.1 | Size: M (Medium)

## Problem Statement
The LiveKit testing framework currently lacks comprehensive test coverage for screen sharing functionality. Screen sharing is a critical feature in video conferencing applications, and without proper testing, there is no automated verification that:
- Participants can share their screens with appropriate permissions
- Screen share tracks are correctly published and appear in rooms
- Subscribers can receive and view screen share tracks
- Screen sharing can be properly stopped and cleaned up

## User Value
Test developers need to verify screen sharing behavior between participants to ensure their LiveKit integrations work correctly. This testing capability enables:
- Confidence in screen sharing feature deployments
- Regression detection when LiveKit versions change
- Validation of permission models for screen share publishing
- Quality verification for screen share tracks

## Success Metrics
- All screen sharing scenarios pass consistently across supported LiveKit versions
- Permission-based access control for screen sharing is verified
- Screen share track lifecycle (publish/unpublish) is validated
- Subscriber track reception is confirmed via server API

## Scope

### In Scope
- Permission grant for screen sharing (canPublishSources)
- Screen share track publishing from browser
- Server-side verification of screen share tracks
- Subscriber track visibility verification
- Screen share stop/cleanup verification
- Basic track type identification (camera vs screen share)

### Out of Scope
- Screen share quality/resolution assertions (future Story 1.1.3 - Dynacast)
- Screen share with audio capture (OS-level audio sharing)
- Multiple simultaneous screen shares from same participant
- Screen share recording via Egress (covered by existing egress tests)
- Application-specific window sharing (browser simulates generic screen)

## Dependencies

### Existing Infrastructure (Required)
- `LiveKitContainer` - Docker-based LiveKit server management
- `SeleniumConfig` - Cross-platform WebDriver configuration
- `AccessTokenStateManager` - Token creation with grants
- `ContainerStateManager` - Container lifecycle management
- `LiveKitMeet` page object - Browser-based room interaction
- `LiveKitRoomSteps` - Server-side room/participant verification

### New Components (To Be Created)
- Screen share step definitions in `LiveKitBrowserWebrtcSteps`
- LiveKitMeet page object extensions for screen share controls
- New feature file `livekit_screen_sharing.feature`

### Technical Dependencies
- LiveKit SDK `canPublishSources` grant support
- Browser fake media stream for screen share simulation
- Selenium WebDriver screen share permission handling

## Related Documentation
- [LiveKit SDK Track Sources](https://docs.livekit.io/client-sdk-js/enums/Track.Source.html)
- [LiveKit Access Tokens](https://docs.livekit.io/realtime/concepts/authentication/)
- [LiveKit Track Types](https://docs.livekit.io/realtime/concepts/tracks/)

## Open Questions
1. Does the containerized Chrome browser support fake screen share media streams?
2. Should we test both `getDisplayMedia` (user-initiated) and `getDisplayMedia` with fake stream?
3. What is the minimum LiveKit version that supports `canPublishSources` grant?
