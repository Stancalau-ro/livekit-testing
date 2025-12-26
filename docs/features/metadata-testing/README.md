# Feature: Metadata Operations Testing

## Status
**Complete** | Stories 1.1.5 & 1.1.6 | Size: M (Medium)

## Implementation
- Feature file: `src/test/resources/features/livekit_metadata.feature`
- 11 Gherkin scenarios covering room and participant metadata operations
- Tests room metadata CRUD, events, late joiners, JSON metadata, and size limits
- Tests participant metadata via tokens, API updates, event propagation, and webhooks

## Problem Statement
The LiveKit testing framework currently lacks test coverage for metadata operations at both the room and participant levels. Metadata is a critical feature that enables applications to store and propagate custom information associated with rooms and participants in real-time. Without proper testing:
- There is no verification that room metadata can be set, updated, and retrieved via the server API
- Participant metadata propagation to other participants is untested
- Webhook events containing metadata changes are not validated
- Edge cases like metadata size limits and special characters are not verified
- The interaction between token-based metadata and API-based updates is not tested

## User Value
Test developers need to verify metadata operations to ensure their LiveKit integrations properly handle custom application data. This testing capability enables:
- Confidence that room metadata persists and propagates correctly
- Verification that participant metadata set via tokens is accessible to others
- Validation of real-time metadata update events reaching all participants
- Detection of regressions in metadata handling across LiveKit versions
- Quality assurance for applications using metadata (user profiles, session state, room settings)

## Success Metrics
- All metadata scenarios pass consistently across supported LiveKit versions
- Room metadata set via RoomServiceClient is retrievable and matches expected values
- Participant metadata is visible to other participants after join
- Metadata update events are received by all connected participants
- Metadata size limits are documented and enforced
- Special characters and escape sequences in metadata are handled correctly
- Webhook events contain accurate metadata information

## Scope

### In Scope
- Setting room metadata via RoomServiceClient
- Retrieving room metadata via server API
- Room metadata update event propagation to participants
- Setting participant metadata via access token attributes
- Updating participant metadata via RoomServiceClient
- Participant metadata update event propagation
- Metadata visibility in ListParticipants and ListRooms responses
- Webhook event verification for metadata changes
- Metadata size limit testing
- Special characters and JSON in metadata
- Empty metadata and null handling

### Out of Scope
- Encrypted metadata (encryption is separate feature)
- Metadata synchronization across multiple LiveKit clusters
- Metadata persistence across server restarts (LiveKit does not persist rooms)
- Custom metadata serialization formats (framework tests string values)
- Real-time metadata conflict resolution
- Metadata access control (permissions are handled via grants)
- Metadata compression or optimization

## Dependencies

### Existing Infrastructure (Required)
- `LiveKitContainer` - Docker-based LiveKit server management
- `RoomServiceClient` - Server API for room and participant operations
- `AccessTokenStateManager` - Token creation with custom attributes
- `ContainerStateManager` - Container lifecycle management
- `LiveKitMeet` page object - Browser-based room interaction
- `WebhookReceiver` - Webhook event capture and validation
- `LiveKitRoomSteps` - Server-side room/participant verification

### New Components (To Be Created)
- Metadata step definitions for room metadata operations
- Metadata step definitions for participant metadata operations
- Extended JavaScript helpers for metadata event listening
- New feature file `livekit_metadata.feature`
- Metadata verification utilities

### Technical Dependencies
- LiveKit RoomServiceClient metadata API
- LiveKit access token metadata/attributes support
- Browser SDK metadata event listeners
- Webhook metadata event payloads

## Related Documentation
- [LiveKit Room Service API](https://docs.livekit.io/realtime/server/managing-rooms/)
- [LiveKit Participant Management](https://docs.livekit.io/realtime/server/managing-participants/)
- [LiveKit Access Tokens](https://docs.livekit.io/realtime/server/generating-tokens/)
- [LiveKit Webhooks](https://docs.livekit.io/realtime/server/receiving-webhooks/)

## Open Questions

### Technical Investigation Required

1. **Metadata Size Limits**: What is the maximum size for room and participant metadata in LiveKit? Need to document limits per version.

2. **Update Frequency**: Is there rate limiting on metadata updates? What is the maximum update frequency?

3. **Event Ordering**: Are metadata update events guaranteed to arrive in order? How are concurrent updates handled?

4. **Webhook Payload**: Does the webhook payload include the full metadata or just the changed portion?

5. **Token Refresh**: When participant metadata is updated via API after join, does it override token-based metadata completely or merge?

### Business Questions

6. **Acceptable Latency**: What is the maximum acceptable latency for metadata propagation to other participants in test scenarios?

7. **Size Thresholds**: What metadata sizes should we test (small, medium, large)?

## Relationship to Token Attributes

Participant metadata can be set in two ways:

| Method | Timing | Use Case |
|--------|--------|----------|
| **Token Attributes** | At join time | Static profile info, user ID, display name |
| **API Update** | After join | Dynamic state, status changes, custom data |

### Testing Interaction
- Initial metadata from token should be visible to other participants
- API updates should replace/update the initial metadata
- Events should fire for API-triggered updates
- Other participants should see the updated metadata in real-time

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Metadata events lost in high-load scenarios | Medium | Low | Test with reasonable update frequency |
| Size limits vary across LiveKit versions | Medium | Medium | Test against current version; document limits |
| JSON metadata parsing issues | Low | Low | Test with valid JSON structures |
| Webhook delivery timing | Medium | Medium | Use polling with timeout for webhook verification |
| Browser SDK event listener differences | Low | Low | Test on multiple browsers |

### Mitigation Strategies

1. **Event Verification**: Use polling with reasonable timeouts for event verification
2. **Size Testing**: Start with small metadata and progressively increase
3. **JSON Handling**: Test both simple strings and complex JSON structures
4. **Webhook Timing**: Allow sufficient time for webhook delivery in containerized environment
5. **Browser Compatibility**: Test metadata events on Chrome, Firefox, and Edge

## Implementation Considerations

### Suggested Approach

1. **Room Metadata First**: Start with server-side room metadata (simplest case)
2. **Token Metadata**: Add participant metadata via token attributes
3. **API Updates**: Add participant metadata updates via RoomServiceClient
4. **Event Propagation**: Verify events reach connected participants
5. **Webhook Integration**: Add webhook verification for metadata events
6. **Edge Cases**: Test size limits, special characters, concurrent updates

### Metadata Types to Test

| Type | Example | Purpose |
|------|---------|---------|
| Simple string | `"active"` | Status flags |
| JSON object | `{"theme":"dark","lang":"en"}` | Structured settings |
| Long string | 1KB+ of text | Size limit testing |
| Special chars | `"line1\nline2"` | Escape handling |
| Empty string | `""` | Null/empty handling |
| Unicode | `"user: \u4e2d\u6587"` | International characters |

## File Structure

```
docs/features/metadata-testing/
    README.md           # This file - overview and status
    requirements.md     # Detailed user stories with acceptance criteria
    technical-notes.md  # Implementation considerations
```

## Related Stories

- **Story 1.1.4** (Data Channel) - Completed; similar real-time event patterns
- **Story 1.1.7** (Track Mute) - Completed; similar state propagation patterns
- **Story 1.1.3** (Dynacast) - Completed; similar multi-participant verification
- **Story 2.2.2** (Container Logs) - Future; could help debug metadata issues
