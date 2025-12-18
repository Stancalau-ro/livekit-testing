# Feature: Data Channel Communication Testing

## Status
**Ready for Implementation** | Story 1.1.4 | Size: M (Medium)

**Planning Complete:** Research validated, critical issues fixed, patterns documented

## Problem Statement
The LiveKit testing framework currently lacks test coverage for data channel communication between participants. Data channels are a critical WebRTC feature that enables real-time messaging and data transfer between participants without going through a media server for the actual data. Without proper testing:
- There is no verification that participants can send and receive data messages
- Reliable vs unreliable data channel behavior is untested
- Message delivery guarantees cannot be validated
- Large message handling and potential fragmentation is not verified
- Data channel latency characteristics are undocumented

## User Value
Test developers need to verify data channel communication to ensure their LiveKit integrations properly support real-time messaging between participants. This testing capability enables:
- Confidence that data messages are delivered correctly between participants
- Verification of reliable data channel guarantees (ordered, guaranteed delivery)
- Validation of unreliable data channel behavior (best-effort delivery)
- Testing of message size limits and large message handling
- Detection of regressions in data channel behavior across LiveKit versions
- Quality assurance for applications using data channels (chat, file transfer, game state sync)

## Success Metrics
- All data channel scenarios pass consistently across supported LiveKit versions
- Messages sent via reliable channel are confirmed received by all subscribers
- Unreliable channel behavior is documented and tested
- Large message handling (near the 15KB reliable mode limit) is verified
- Data channel latency is measurable (under normal conditions)
- Permission model (canPublishData) is properly enforced

## Scope

### In Scope
- Permission grant for data publishing (canPublishData)
- Sending messages via reliable data channel
- Verifying message receipt by other participants
- Testing unreliable data channel delivery
- Measuring basic data channel latency
- Testing large message handling (up to LiveKit limits)
- Cross-browser data channel testing (Chrome, Firefox, Edge)
- Broadcast vs targeted message delivery

### Out of Scope
- Binary data transfer (focus on string/JSON messages)
- File transfer over data channels (complex multi-part transfer)
- Data channel encryption verification (handled by DTLS)
- Network partition simulation (requires traffic control)
- Data channel reconnection after network interruption
- Custom message acknowledgment protocols
- Real-time collaborative editing scenarios

## Dependencies

### Existing Infrastructure (Required)
- `LiveKitContainer` - Docker-based LiveKit server management
- `SeleniumConfig` - Cross-platform WebDriver configuration
- `AccessTokenStateManager` - Token creation with grants
- `ContainerStateManager` - Container lifecycle management
- `LiveKitMeet` page object - Browser-based room interaction
- `LiveKitRoomSteps` - Server-side room/participant verification

### New Components (To Be Created)
- Data channel step definitions in `LiveKitBrowserWebrtcSteps`
- Extended `LiveKitMeet.java` with data channel methods
- JavaScript helpers in meet.html for data channel operations
- New feature file `livekit_data_channel.feature`

### Technical Dependencies
- LiveKit SDK data message API
- Browser WebRTC data channel support
- LiveKit server data channel routing
- `canPublishData` grant in VideoGrant

## Related Documentation
- [LiveKit Data Messages](https://docs.livekit.io/realtime/client/data-messages/)
- [LiveKit DataPacket Kind](https://docs.livekit.io/realtime/server/receiving-webhooks/#data-packet-kind)
- [WebRTC Data Channels](https://developer.mozilla.org/en-US/docs/Web/API/RTCDataChannel)

## Research Findings

### Message Size Limits (Confirmed)
| Mode | Limit | Notes |
|------|-------|-------|
| Reliable | 15 KiB (15,360 bytes) | 16 KiB protocol minus headers |
| Lossy | 1,300 bytes | Stay within 1,400-byte MTU |

### DataPublishOptions API
- `reliable`: boolean - ordered delivery with retransmission
- `destinationIdentities`: string[] - specific recipients (empty = broadcast)
- `topic`: string - custom packet type identifier

### Existing Infrastructure
- `canPublishData` grant already supported in AccessTokenStateManager
- LiveKitTestHelpers pattern established for test automation
- Event array pattern: `window.eventArray = []` → `.push()`

### Open Questions (Remaining)
1. ~~What is the maximum message size?~~ **Answered: 15 KiB reliable, 1,300 bytes lossy**
2. ~~How does message ordering work with unreliable channels?~~ **Answered: No ordering guarantee**
3. Can we verify server-side data channel events via webhooks? **(Needs testing)**
4. What is the expected latency range in containerized testing? **(Needs benchmarking)**
5. ~~Does LiveKit support targeted messages?~~ **Answered: Yes, via destinationIdentities**
