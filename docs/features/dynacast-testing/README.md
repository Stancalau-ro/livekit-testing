# Feature: Dynacast Bandwidth Adaptation Testing

## Status
**Draft** | Story 1.1.3 | Size: L (Large)

## Problem Statement
The LiveKit testing framework currently has dynacast enabled by default in the client configuration (`dynacast: true`), but lacks comprehensive test coverage for verifying dynacast's automatic bandwidth adaptation behavior. Dynacast is LiveKit's intelligent bandwidth optimization feature that dynamically pauses and resumes video layer transmission based on subscriber viewing state and network conditions. Without proper testing:
- There is no verification that dynacast correctly pauses unused video layers
- Bandwidth adaptation behavior under network constraints is unverified
- Quality recovery when bandwidth increases is untested
- Response time for quality adaptation cannot be measured
- Multi-subscriber scenarios with different bandwidth needs are not validated
- The relationship between dynacast and simulcast is not tested

## User Value
Test developers need to verify dynacast bandwidth adaptation to ensure their LiveKit integrations properly handle dynamic network conditions. This testing capability enables:
- Confidence that dynacast automatically adapts video quality based on subscriber needs
- Verification that bandwidth is conserved when subscribers do not need high-quality video
- Validation of quality recovery when network conditions improve
- Measurement of adaptation response time for real-time applications
- Detection of regressions in dynacast behavior across LiveKit versions
- Quality assurance for bandwidth-constrained scenarios (mobile, poor network)

## Success Metrics
- All dynacast scenarios pass consistently across supported LiveKit versions
- Video layer pausing is detectable when subscribers are not viewing
- Quality adaptation under bandwidth constraints is measurable
- Adaptation response time is within acceptable bounds (configurable threshold)
- Quality recovery after bandwidth increase is verified
- Multi-subscriber scenarios with different quality preferences work correctly
- Tests handle dynacast-disabled scenarios gracefully

## Scope

### In Scope
- Enabling/disabling dynacast in room configuration via browser client
- Verifying dynacast activation via server API (TrackInfo)
- Testing video quality adaptation when subscriber sets quality preference
- Measuring adaptation response time between quality changes
- Testing quality recovery when subscriber requests higher quality
- Verifying dynacast behavior with multiple subscribers (different quality preferences)
- Testing dynacast interaction with simulcast layers
- Subscriber visibility state changes (video element visible/hidden)

### Out of Scope
- Real network bandwidth throttling via traffic control tools (tc, netem)
- Container network simulation (pumba, toxiproxy) - future epic
- Mobile network condition emulation (3G, 4G profiles)
- Publisher-side bandwidth detection (uplink adaptation)
- Audio track adaptation (dynacast is video-specific)
- Latency-based adaptation (focus is on bandwidth)
- Cross-region network simulation

## Dependencies

### Existing Infrastructure (Required)
- `LiveKitContainer` - Docker-based LiveKit server management
- `SeleniumConfig` - Cross-platform WebDriver configuration
- `AccessTokenStateManager` - Token creation with grants
- `ContainerStateManager` - Container lifecycle management
- `LiveKitMeet` page object - Browser-based room interaction with dynacast setting
- `LiveKitRoomSteps` - Server-side room/participant verification
- Simulcast implementation (Story 1.1.2) - Dynacast works with simulcast layers

### New Components (To Be Created)
- Dynacast step definitions in `LiveKitBrowserWebrtcSteps`
- Extended `LiveKitMeet.java` with dynacast control methods
- JavaScript helpers for dynacast state monitoring
- New feature file `livekit_dynacast.feature`
- Bandwidth constraint simulation via subscriber quality preferences

### Technical Dependencies
- LiveKit server dynacast support (enabled by default)
- Browser WebRTC with simulcast capability
- Server API access to track layer information via RoomServiceClient
- LiveKit SDK VideoQuality enum for quality preference control
- Subscriber-side layer selection API

## Related Documentation
- [LiveKit Dynacast Documentation](https://docs.livekit.io/realtime/client/receive/#adaptive-stream)
- [LiveKit Adaptive Streaming](https://docs.livekit.io/realtime/client/receive/#adaptive-stream)
- [LiveKit Room Options](https://docs.livekit.io/client-sdk-js/interfaces/RoomOptions.html)
- [Story 1.1.2: Simulcast Video Testing](../simulcast-video-testing/README.md)

## Open Questions

### Technical Investigation Required

1. **Layer Pause Detection**: Can we detect when dynacast pauses a video layer via the server API, or only through client-side events?

2. **Bandwidth Simulation Alternatives**: Since real network throttling is out of scope, what are the best alternatives?
   - Subscriber quality preference setting (implemented in simulcast)
   - Video dimension constraints
   - Client-side bandwidth hints

3. **Response Time Measurement**: What is the expected range for dynacast adaptation response time in containerized testing? Need to establish baseline.

4. **Visibility State Detection**: How reliably can we simulate subscriber "not viewing" state?
   - Hide video element via CSS
   - Set video quality to OFF
   - Minimize/hide browser window

5. **Server-Side Metrics**: Does LiveKit expose dynacast-specific metrics via the admin API or webhooks?

6. **Version Compatibility**: From which LiveKit version is dynacast fully supported? Need to document minimum version.

### Business Questions

7. **Acceptable Response Time**: What is the maximum acceptable response time for quality adaptation in the test framework? Suggest 2-5 seconds for containerized tests.

8. **Quality Thresholds**: What quality level differences should we expect and verify between layers?

## Relationship to Simulcast

Dynacast and simulcast are complementary features:

| Feature | Purpose | Scope |
|---------|---------|-------|
| **Simulcast** | Publisher sends multiple quality layers | Publisher-side |
| **Dynacast** | Server pauses/resumes layers based on subscriber needs | Server-side |

Dynacast requires simulcast to function effectively - it controls which of the simulcast layers are actively transmitted to subscribers.

### Testing Interaction
- When no subscriber wants high quality, dynacast should pause high-quality layer transmission
- When subscriber requests high quality, dynacast should resume high-quality layer
- Multiple subscribers with different needs should receive appropriate layers

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| No direct API for layer pause state | High | Medium | Use indirect metrics (bitrate, quality preference acknowledgment) |
| Bandwidth simulation limited in containers | High | High | Use quality preference API as proxy for bandwidth constraint |
| Response time measurement accuracy | Medium | Medium | Use multiple samples and statistical analysis |
| Containerized network too fast for realistic testing | Medium | High | Document limitations; focus on API behavior, not network simulation |
| Dynacast behavior differs across LiveKit versions | Medium | Low | Test against multiple versions; document version-specific behavior |

### Mitigation Strategies

1. **Layer State Detection**: Use combination of server API (TrackInfo layers), client events (quality change), and bitrate monitoring

2. **Bandwidth Simulation**: Rely on subscriber quality preference API which effectively tells the server "I only want low quality" - dynacast should respond accordingly

3. **Response Time**: Establish baseline in container environment; focus on relative timing rather than absolute values

4. **Multi-Subscriber Testing**: Use multiple browser instances with different quality preferences to verify dynacast delivers appropriate layers

5. **Version Documentation**: Test against default version first; document any version-specific behaviors discovered

## Implementation Considerations

### Suggested Approach

Since real bandwidth throttling is complex in containerized environments, the recommended approach is:

1. **Quality Preference as Bandwidth Proxy**: When a subscriber sets `VideoQuality.LOW`, dynacast should respond by reducing transmitted quality

2. **Visibility State Changes**: When a video element is hidden or quality is set to OFF, dynacast should pause transmission

3. **Multiple Subscribers**: Different subscribers with different quality preferences should all receive appropriate layers

4. **Response Time**: Measure time between quality preference change and actual quality change in received stream

### Alternative Approaches (Future Consideration)

For more realistic bandwidth testing, future epics could add:
- Container network shaping with `tc` (Linux traffic control)
- Toxiproxy for network condition simulation
- Chrome DevTools Protocol network throttling
- WebRTC internals monitoring for detailed metrics
