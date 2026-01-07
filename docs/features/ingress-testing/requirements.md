# Ingress Stream Input Testing - Requirements Document

## Epic Description

**Epic:** Test Ingress Stream Input
**Story ID:** 1.1.8
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test ingress functionality
**So that** I can verify external streams can be brought into LiveKit rooms

---

## Story Breakdown

The original L-sized story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.8.1: Ingress Container Infrastructure Setup

**Size:** M (Medium)

**As a** test infrastructure developer
**I want** an Ingress container integrated with the test framework
**So that** ingress functionality can be tested alongside LiveKit server

**Acceptance Criteria:**

**Given** a test requires ingress functionality
**When** the ingress container is requested
**Then** the container starts with proper configuration

**Given** the ingress container is starting
**When** it connects to Redis
**Then** it registers with the LiveKit server successfully

**Given** the ingress container is running
**When** health check is performed
**Then** the container reports healthy status

- [ ] Create `IngressContainer` class extending TestContainers
- [ ] Configure ingress container with API key, secret, WebSocket URL
- [ ] Connect ingress container to shared Docker network
- [ ] Configure Redis connection for ingress
- [ ] Implement health check verification
- [ ] Add ingress container to ContainerStateManager
- [ ] Create BDD step: `Given an Ingress service is running with service name "{name}"`

**Dependencies:** LiveKitContainer, RedisContainer, ContainerStateManager

---

### Story 1.1.8.2: Ingress Service Client Implementation

**Size:** S (Small)

**As a** test developer
**I want** a Java client for the Ingress API
**So that** I can create and manage ingresses programmatically

**Acceptance Criteria:**

**Given** the Ingress API client is initialized
**When** API methods are called
**Then** they communicate with LiveKit server correctly

**Given** an ingress is created via API
**When** the response is returned
**Then** it contains IngressInfo with URL and stream key

**Given** an ingress exists
**When** list ingress is called
**Then** the ingress appears in the list

- [ ] Implement `IngressServiceClient` wrapper using livekit-server SDK
- [ ] Support CreateIngress with all input types (RTMP, WHIP, URL)
- [ ] Support ListIngress with room name filter
- [ ] Support UpdateIngress for configuration changes
- [ ] Support DeleteIngress for cleanup
- [ ] Add IngressAdmin grant to AccessToken when needed
- [ ] Create state manager for tracking created ingresses

**Dependencies:** io.livekit:livekit-server SDK

---

### Story 1.1.8.3: RTMP Stream Simulator Implementation

**Size:** M (Medium)

**As a** test developer
**I want** to simulate RTMP streams using FFmpeg
**So that** I can test RTMP ingress without external encoder software

**Acceptance Criteria:**

**Given** an ingress with RTMP endpoint exists
**When** the RTMP simulator connects
**Then** audio and video are streamed to the endpoint

**Given** the RTMP stream is active
**When** stream parameters are inspected
**Then** codec and bitrate match configuration

**Given** the RTMP stream should stop
**When** the simulator is terminated
**Then** the stream disconnects cleanly

- [ ] Create `RtmpStreamSimulator` class
- [ ] Use FFmpeg with test pattern sources (testsrc, sine wave)
- [ ] Support configurable stream duration
- [ ] Support configurable video resolution and framerate
- [ ] Support configurable audio codec and bitrate
- [ ] Run FFmpeg in Docker container with network access
- [ ] Implement stream start, status check, and stop operations
- [ ] Create BDD step: `When an RTMP stream is sent to ingress "{name}"`

**Dependencies:** FFmpeg container, Docker network

---

### Story 1.1.8.4: Create RTMP Ingress via API

**Size:** S (Small)

**As a** test developer
**I want** to create RTMP ingresses via the API
**So that** I can set up test scenarios for RTMP streaming

**Acceptance Criteria:**

**Given** a room exists
**When** an RTMP ingress is created for the room
**Then** the ingress has a valid RTMP URL and stream key

**Given** an RTMP ingress is created
**When** the ingress info is inspected
**Then** it shows RTMP_INPUT type and ENDPOINT_INACTIVE state

**Given** ingress creation parameters include participant identity
**When** the ingress is created
**Then** the ingress uses the specified identity

- [ ] Add BDD step: `When an RTMP ingress "{name}" is created for room "{room}" with identity "{identity}"`
- [ ] Store ingress info in state manager for later reference
- [ ] Verify ingress URL format matches configuration
- [ ] Verify stream key is generated
- [ ] Test ingress with custom participant name
- [ ] Test ingress with preset video/audio options

**Dependencies:** Story 1.1.8.2

---

### Story 1.1.8.5: Verify Ingress Participant Appears in Room

**Size:** M (Medium)

**As a** test developer
**I want** to verify the ingress participant joins the room
**So that** I can confirm ingress is publishing to the correct room

**Acceptance Criteria:**

**Given** an RTMP ingress is created for a room
**When** an RTMP stream connects to the ingress
**Then** a participant with the ingress identity appears in the room

**Given** the ingress participant is in the room
**When** participant info is retrieved
**Then** the identity and name match ingress configuration

**Given** the ingress stream disconnects
**When** the room participants are checked
**Then** the ingress participant leaves the room

- [ ] Add BDD step: `Then participant "{identity}" should appear in room "{room}" via ingress`
- [ ] Implement polling for participant appearance with timeout
- [ ] Verify participant identity matches ingress configuration
- [ ] Verify participant name matches ingress configuration
- [ ] Test participant removal when stream disconnects
- [ ] Log ingress participant join/leave events

**Dependencies:** Story 1.1.8.3, Story 1.1.8.4, RoomServiceClient

---

### Story 1.1.8.6: Verify Ingress Track Subscription

**Size:** M (Medium)

**As a** test developer
**I want** to verify subscribers can receive ingress tracks
**So that** I can confirm end-to-end media flow from ingress

**Acceptance Criteria:**

**Given** an ingress is actively publishing
**When** a browser participant subscribes to the room
**Then** they receive the ingress audio and video tracks

**Given** a subscriber is receiving ingress tracks
**When** the track info is inspected
**Then** tracks have expected source types (camera, microphone)

**Given** a subscriber is receiving video
**When** video dimensions are checked
**Then** they match expected resolution from transcoding

- [ ] Add BDD step: `Then "{participant}" should receive tracks from ingress "{identity}"`
- [ ] Use existing browser infrastructure to join as subscriber
- [ ] Verify audio track is received
- [ ] Verify video track is received
- [ ] Check video resolution matches expected (from preset)
- [ ] Optionally verify simulcast layers if enabled

**Dependencies:** Story 1.1.8.5, LiveKitMeet browser infrastructure

---

### Story 1.1.8.7: Test Ingress Lifecycle (Delete/Cleanup)

**Size:** S (Small)

**As a** test developer
**I want** to test ingress deletion and cleanup
**So that** I can verify ingresses are properly removed

**Acceptance Criteria:**

**Given** an active ingress exists
**When** DeleteIngress is called
**Then** the ingress is removed from the list

**Given** an ingress is deleted
**When** the ingress participant is checked
**Then** the participant has left the room

**Given** ingresses were created during test
**When** test cleanup runs
**Then** all ingresses are deleted

- [ ] Add BDD step: `When ingress "{name}" is deleted`
- [ ] Verify ingress removal from ListIngress
- [ ] Verify participant removal from room
- [ ] Implement automatic cleanup in @After hook
- [ ] Handle deletion of ingress in different states
- [ ] Log cleanup actions for debugging

**Dependencies:** Story 1.1.8.2, Story 1.1.8.5

---

### Story 1.1.8.8: Test URL Input Ingress

**Size:** S (Small)

**As a** test developer
**I want** to test URL-based ingress
**So that** I can verify media file ingestion works

**Acceptance Criteria:**

**Given** a media file URL is accessible
**When** a URL ingress is created with that URL
**Then** the ingress starts fetching and transcoding immediately

**Given** a URL ingress is actively publishing
**When** the ingress state is checked
**Then** it shows ENDPOINT_PUBLISHING state

**Given** the media file ends
**When** the ingress finishes
**Then** the ingress participant leaves the room

- [ ] Host test media files (MP4, HLS) in web container or use public URL
- [ ] Add BDD step: `When a URL ingress "{name}" is created for room "{room}" with URL "{url}"`
- [ ] Verify ingress starts immediately (no stream key needed)
- [ ] Verify participant appears in room
- [ ] Test with different media formats (MP4, MKV, HLS)
- [ ] Test with short media file (verify end behavior)

**Dependencies:** Story 1.1.8.2, WebServerContainer or public URL

---

### Story 1.1.8.9: Test Ingress Transcoding Presets

**Size:** S (Small)

**As a** test developer
**I want** to test ingress with different transcoding presets
**So that** I can verify quality settings are applied correctly

**Acceptance Criteria:**

**Given** an ingress is created with a specific video preset
**When** the ingress publishes
**Then** the video matches the preset parameters

**Given** an ingress uses H264_720P_30FPS_3_LAYERS preset
**When** simulcast layers are checked
**Then** three quality layers are available

**Given** an ingress uses audio preset OPUS_STEREO_96KBPS
**When** audio track is received
**Then** audio codec is OPUS

- [ ] Test with different video presets (720p, 1080p, etc.)
- [ ] Test with different audio presets (mono, stereo)
- [ ] Verify video resolution matches preset
- [ ] Verify simulcast layers count matches preset
- [ ] Document available presets and their parameters

**Dependencies:** Story 1.1.8.6

---

### Story 1.1.8.10: Test WHIP Ingress

**Size:** L (Large)

**As a** test developer
**I want** to test WHIP ingress
**So that** I can verify low-latency WebRTC ingestion

**Acceptance Criteria:**

**Given** a WHIP ingress is created
**When** the ingress info is returned
**Then** it has a valid WHIP endpoint URL

**Given** a WHIP stream connects
**When** the connection is established
**Then** the ingress participant appears in the room

**Given** WHIP transcoding bypass is enabled
**When** media is published
**Then** media is forwarded without transcoding

- [ ] Create `WhipStreamSimulator` using GStreamer or WHIP client
- [ ] Test WHIP ingress creation with endpoint URL
- [ ] Test WHIP stream connection (requires ICE handling)
- [ ] Test transcoding bypass option
- [ ] Test enable_transcoding option
- [ ] Document WHIP-specific requirements

**Dependencies:** GStreamer container with WHIP plugins, Story 1.1.8.2

**Note:** WHIP is essential for complete ingress coverage, providing low-latency WebRTC-based ingestion.

---

## Gherkin Scenarios

### Feature File: `livekit_ingress.feature`

```gherkin
Feature: LiveKit Ingress Stream Input
  As a test developer
  I want to test ingress functionality
  So that I can verify external streams can be brought into LiveKit rooms

  Background:
    Given the LiveKit config is set to "basic-with-ingress"
    And a LiveKit server is running in a container with service name "livekit1"
    And a Redis server is running in a container with service name "redis1"
    And an Ingress service is running with service name "ingress1"

  # Story 1.1.8.4: Create RTMP Ingress via API
  Scenario: Create RTMP ingress for a room
    Given room "IngressRoom" is created using service "livekit1"
    When an RTMP ingress "test-ingress" is created for room "IngressRoom" with identity "Streamer"
    Then the ingress "test-ingress" should have input type "RTMP_INPUT"
    And the ingress "test-ingress" should have a valid RTMP URL
    And the ingress "test-ingress" should have a stream key
    And the ingress "test-ingress" should be in state "ENDPOINT_INACTIVE"

  Scenario: Create RTMP ingress with custom participant name
    Given room "CustomNameRoom" is created using service "livekit1"
    When an RTMP ingress "named-ingress" is created for room "CustomNameRoom" with identity "Broadcaster" and name "Live Broadcast"
    Then the ingress "named-ingress" should have participant name "Live Broadcast"

  # Story 1.1.8.5: Verify Ingress Participant Appears in Room
  Scenario: RTMP stream appears as participant in room
    Given room "StreamRoom" is created using service "livekit1"
    And an RTMP ingress "stream-ingress" is created for room "StreamRoom" with identity "RTMPStreamer"
    When an RTMP stream is sent to ingress "stream-ingress"
    Then the ingress "stream-ingress" should be in state "ENDPOINT_PUBLISHING" within 30 seconds
    And participant "RTMPStreamer" should appear in room "StreamRoom" using service "livekit1"
    When the RTMP stream is stopped
    Then the ingress "stream-ingress" should be in state "ENDPOINT_INACTIVE" within 15 seconds
    And participant "RTMPStreamer" should not be in room "StreamRoom" using service "livekit1"

  # Story 1.1.8.6: Verify Ingress Track Subscription
  Scenario: Subscriber receives ingress tracks
    Given room "SubscribeRoom" is created using service "livekit1"
    And an RTMP ingress "sub-ingress" is created for room "SubscribeRoom" with identity "IngressPublisher"
    And an RTMP stream is sent to ingress "sub-ingress"
    And the ingress "sub-ingress" is in state "ENDPOINT_PUBLISHING"
    And an access token is created with identity "Viewer" and room "SubscribeRoom" with grants "canSubscribe:true"
    When "Viewer" opens a "Chrome" browser with LiveKit Meet page
    And "Viewer" connects to room "SubscribeRoom" using the access token
    And connection is established successfully for "Viewer"
    Then "Viewer" should receive remote video track from "IngressPublisher"
    And "Viewer" should receive remote audio track from "IngressPublisher"
    And "Viewer" closes the browser
    And the RTMP stream is stopped

  Scenario: Multiple subscribers receive ingress tracks
    Given room "MultiSubRoom" is created using service "livekit1"
    And an RTMP ingress "multi-ingress" is created for room "MultiSubRoom" with identity "Broadcaster"
    And an RTMP stream is sent to ingress "multi-ingress"
    And the ingress "multi-ingress" is in state "ENDPOINT_PUBLISHING"
    And an access token is created with identity "Viewer1" and room "MultiSubRoom" with grants "canSubscribe:true"
    And an access token is created with identity "Viewer2" and room "MultiSubRoom" with grants "canSubscribe:true"
    When "Viewer1" opens a "Chrome" browser with LiveKit Meet page
    And "Viewer1" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Viewer1"
    When "Viewer2" opens a "Firefox" browser with LiveKit Meet page
    And "Viewer2" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Viewer2"
    Then "Viewer1" should receive remote video track from "Broadcaster"
    And "Viewer2" should receive remote video track from "Broadcaster"
    And room "MultiSubRoom" should have 3 active participants in service "livekit1"
    And "Viewer1" closes the browser
    And "Viewer2" closes the browser
    And the RTMP stream is stopped

  # Story 1.1.8.7: Test Ingress Lifecycle
  Scenario: Delete ingress removes participant
    Given room "DeleteRoom" is created using service "livekit1"
    And an RTMP ingress "delete-ingress" is created for room "DeleteRoom" with identity "ToDelete"
    And an RTMP stream is sent to ingress "delete-ingress"
    And the ingress "delete-ingress" is in state "ENDPOINT_PUBLISHING"
    And participant "ToDelete" should appear in room "DeleteRoom" using service "livekit1"
    When ingress "delete-ingress" is deleted
    Then the ingress "delete-ingress" should not exist
    And participant "ToDelete" should not be in room "DeleteRoom" using service "livekit1"

  Scenario: List ingresses returns created ingresses
    Given room "ListRoom" is created using service "livekit1"
    And an RTMP ingress "ingress-a" is created for room "ListRoom" with identity "StreamerA"
    And an RTMP ingress "ingress-b" is created for room "ListRoom" with identity "StreamerB"
    When ingresses are listed for room "ListRoom"
    Then the ingress list should contain "ingress-a"
    And the ingress list should contain "ingress-b"
    And the ingress list should have 2 items

  # Story 1.1.8.8: Test URL Input Ingress
  Scenario: URL ingress publishes media file to room
    Given room "UrlRoom" is created using service "livekit1"
    And a test media file is available at URL "http://webserver1:8080/test-video.mp4"
    When a URL ingress "url-ingress" is created for room "UrlRoom" with URL "http://webserver1:8080/test-video.mp4" and identity "FileStream"
    Then the ingress "url-ingress" should be in state "ENDPOINT_PUBLISHING" within 30 seconds
    And participant "FileStream" should appear in room "UrlRoom" using service "livekit1"

  # Story 1.1.8.9: Test Transcoding Presets
  Scenario: Ingress with 720p preset publishes correct resolution
    Given room "PresetRoom" is created using service "livekit1"
    And an RTMP ingress "preset-ingress" is created for room "PresetRoom" with identity "PresetStreamer" and video preset "H264_720P_30FPS_3_LAYERS"
    And an RTMP stream is sent to ingress "preset-ingress" with resolution "1280x720"
    And the ingress "preset-ingress" is in state "ENDPOINT_PUBLISHING"
    And an access token is created with identity "PresetViewer" and room "PresetRoom" with grants "canSubscribe:true"
    When "PresetViewer" opens a "Chrome" browser with LiveKit Meet page
    And "PresetViewer" connects to room "PresetRoom" using the access token
    And connection is established successfully for "PresetViewer"
    Then "PresetViewer" should receive video track with maximum width 1280 from "PresetStreamer"
    And "PresetViewer" closes the browser
    And the RTMP stream is stopped

  # Negative Scenarios
  Scenario: Ingress creation fails for non-existent room
    When an RTMP ingress "fail-ingress" is created for room "NonExistentRoom" with identity "Orphan"
    Then the ingress "fail-ingress" should be created successfully
    And the ingress "fail-ingress" should have room name "NonExistentRoom"
    # Note: LiveKit allows creating ingress for non-existent rooms - room is created on first connection

  Scenario: Stream to ingress with wrong stream key fails
    Given room "AuthRoom" is created using service "livekit1"
    And an RTMP ingress "auth-ingress" is created for room "AuthRoom" with identity "AuthStreamer"
    When an RTMP stream is sent to ingress "auth-ingress" with wrong stream key
    Then the RTMP stream should fail to connect
    And the ingress "auth-ingress" should remain in state "ENDPOINT_INACTIVE"
```

---

## Definition of Done

### Infrastructure Implementation
- [x] `IngressContainer` class created and tested
- [x] Ingress container integrates with Docker network
- [x] Redis connection configured correctly
- [x] Health checks implemented and working
- [x] Version configuration supported via system properties

### API Implementation
- [x] `IngressServiceClient` wrapper implemented
- [x] All CRUD operations (create, list, update, delete) working
- [x] State manager tracks created ingresses
- [x] Proper cleanup in test hooks

### Stream Simulation
- [x] FFmpeg-based RTMP stream simulator working
- [x] Stream can be started and stopped programmatically
- [x] Stream uses configurable test patterns
- [x] Stream runs in Docker container with network access

### BDD Scenarios
- [x] Feature file `livekit_ingress.feature` created
- [x] All step definitions implemented
- [x] All scenarios pass on Chrome browser
- [x] Cleanup hooks properly remove ingresses

### Documentation
- [x] Feature documentation complete in `docs/features/ingress-testing/`
- [x] Step definitions documented
- [x] Technical notes cover implementation details
- [x] Configuration requirements documented

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass (Chrome only for H264 ingress)
- [x] Tests pass against default LiveKit/Ingress version
- [x] Resource cleanup verified (no orphaned containers)

---

## Technical Components

### New Step Definitions Required

#### In `LiveKitIngressSteps.java` (new file):

```java
@Given("an Ingress service is running with service name {string}")
public void ingressServiceRunning(String serviceName)

@When("an RTMP ingress {string} is created for room {string} with identity {string}")
public void createRtmpIngress(String ingressName, String roomName, String identity)

@When("an RTMP ingress {string} is created for room {string} with identity {string} and name {string}")
public void createRtmpIngressWithName(String ingressName, String roomName, String identity, String name)

@When("an RTMP ingress {string} is created for room {string} with identity {string} and video preset {string}")
public void createRtmpIngressWithPreset(String ingressName, String roomName, String identity, String preset)

@When("a URL ingress {string} is created for room {string} with URL {string} and identity {string}")
public void createUrlIngress(String ingressName, String roomName, String url, String identity)

@When("an RTMP stream is sent to ingress {string}")
public void sendRtmpStream(String ingressName)

@When("an RTMP stream is sent to ingress {string} with resolution {string}")
public void sendRtmpStreamWithResolution(String ingressName, String resolution)

@When("an RTMP stream is sent to ingress {string} with wrong stream key")
public void sendRtmpStreamWrongKey(String ingressName)

@When("the RTMP stream is stopped")
public void stopRtmpStream()

@When("ingress {string} is deleted")
public void deleteIngress(String ingressName)

@When("ingresses are listed for room {string}")
public void listIngresses(String roomName)

@Then("the ingress {string} should have input type {string}")
public void verifyIngressInputType(String ingressName, String inputType)

@Then("the ingress {string} should have a valid RTMP URL")
public void verifyIngressRtmpUrl(String ingressName)

@Then("the ingress {string} should have a stream key")
public void verifyIngressStreamKey(String ingressName)

@Then("the ingress {string} should be in state {string}")
public void verifyIngressState(String ingressName, String state)

@Then("the ingress {string} should be in state {string} within {int} seconds")
public void verifyIngressStateWithTimeout(String ingressName, String state, int timeoutSeconds)

@Then("the ingress {string} should have participant name {string}")
public void verifyIngressParticipantName(String ingressName, String name)

@Then("the ingress {string} should not exist")
public void verifyIngressNotExist(String ingressName)

@Then("the ingress list should contain {string}")
public void verifyIngressListContains(String ingressName)

@Then("the ingress list should have {int} items")
public void verifyIngressListSize(int count)

@Then("the RTMP stream should fail to connect")
public void verifyRtmpStreamFailed()
```

### IngressStateManager

Location: `src/main/java/ro/stancalau/test/framework/state/IngressStateManager.java`

```java
@Slf4j
public class IngressStateManager {
    private final Map<String, IngressInfo> ingresses = new ConcurrentHashMap<>();
    private final Map<String, IngressServiceClient> clients = new ConcurrentHashMap<>();

    public void registerIngress(String name, IngressInfo info);
    public IngressInfo getIngress(String name);
    public void registerClient(String serviceName, IngressServiceClient client);
    public IngressServiceClient getClient(String serviceName);
    public void clearAll();
}
```

Integrated via `ManagerFactory` and accessed via `ManagerProvider.ingress()`.

### FFmpegContainer

Location: `src/main/java/ro/stancalau/test/framework/docker/FFmpegContainer.java`

```java
@Slf4j
public class FFmpegContainer extends GenericContainer<FFmpegContainer> {
    @Getter
    private final Network network;

    public static FFmpegContainer createRtmpStream(
        String alias,
        Network network,
        String rtmpUrl,
        String streamKey,
        int durationSeconds,
        @Nullable String logDestinationPath);
}
```

### IngressContainer

Location: `src/main/java/ro/stancalau/test/framework/docker/IngressContainer.java`

```java
@Slf4j
public class IngressContainer extends GenericContainer<IngressContainer> {
    @Getter
    private final Network network;

    public static IngressContainer createContainer(
        String alias,
        Network network,
        String livekitWsUrl,
        String redisUrl);

    public String getRtmpUrl();
    public String getWhipUrl();
}
```

### API Client

Use `IngressServiceClient` from SDK directly (no wrapper needed):

```java
IngressServiceClient client = IngressServiceClient.createClient(
    livekit.getHttpUrl(),
    LiveKitContainer.API_KEY,
    LiveKitContainer.SECRET
);
```

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Ingress container resource requirements exceed CI limits | High | Medium | Document requirements, use smaller presets |
| FFmpeg container networking issues | Medium | Medium | Test thoroughly in Docker network |
| Transcoding timing unpredictable | Medium | High | Use generous timeouts, polling approach |
| WHIP requires complex ICE handling | High | High | Defer WHIP to optional story |
| Ingress version compatibility issues | Medium | Medium | Align versions with LiveKit server |

### Mitigation Strategies

1. **Resource Constraints**: Use lower resolution presets (480p) for basic tests, reserve 720p/1080p for specific scenarios

2. **Networking**: Ensure all containers share the same Docker network, use container hostnames

3. **Timing**: Implement robust polling with configurable timeouts, log state transitions

4. **Complexity**: Start with RTMP which is simpler, defer WHIP to later phase

5. **Compatibility**: Use matching ingress and server versions, document tested combinations

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.8.1 - Ingress Container | M | Foundation for all other stories |
| 2 | 1.1.8.2 - Ingress Service Client | S | API access needed for all operations |
| 3 | 1.1.8.3 - RTMP Stream Simulator | M | Enables stream testing |
| 4 | 1.1.8.4 - Create RTMP Ingress | S | Core functionality |
| 5 | 1.1.8.5 - Verify Participant | M | Validates ingress joins room |
| 6 | 1.1.8.6 - Track Subscription | M | End-to-end media verification |
| 7 | 1.1.8.7 - Lifecycle/Cleanup | S | Essential for test hygiene |
| 8 | 1.1.8.8 - URL Input | S | Alternative input type |
| 9 | 1.1.8.9 - Transcoding Presets | S | Quality verification |
| 10 | 1.1.8.10 - WHIP Ingress | L | Essential for low-latency ingestion |

**Total Estimated Effort:** L (Large) - approximately 2-3 weeks

---

## Appendix: LiveKit Ingress API Reference

### CreateIngressRequest Key Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `input_type` | enum | Yes | RTMP_INPUT, WHIP_INPUT, URL_INPUT |
| `name` | string | Yes | Human-readable name for the ingress |
| `room_name` | string | No | Target room (can be set later) |
| `participant_identity` | string | No | Identity for ingress participant |
| `participant_name` | string | No | Display name for ingress participant |
| `url` | string | Conditional | Required for URL_INPUT type |
| `audio` | IngressAudioOptions | No | Audio encoding configuration |
| `video` | IngressVideoOptions | No | Video encoding configuration |
| `enable_transcoding` | bool | No | Enable transcoding for WHIP |
| `bypass_transcoding` | bool | No | Deprecated, use enable_transcoding |

### IngressVideoEncodingPreset Values

| Preset | Resolution | FPS | Layers |
|--------|------------|-----|--------|
| H264_720P_30FPS_3_LAYERS | 1280x720 | 30 | 3 |
| H264_1080P_30FPS_3_LAYERS | 1920x1080 | 30 | 3 |
| H264_540P_25FPS_2_LAYERS | 960x540 | 25 | 2 |
| H264_720P_30FPS_1_LAYER | 1280x720 | 30 | 1 |

### IngressAudioEncodingPreset Values

| Preset | Bitrate | Channels |
|--------|---------|----------|
| OPUS_MONO_64KBS | 64 kbps | Mono |
| OPUS_STEREO_96KBPS | 96 kbps | Stereo |

### IngressState Enum

| Value | Meaning |
|-------|---------|
| ENDPOINT_INACTIVE | Created but no stream connected |
| ENDPOINT_BUFFERING | Stream connected, transcoding starting |
| ENDPOINT_PUBLISHING | Actively publishing to room |
| ENDPOINT_ERROR | Error occurred |

---

## Related Stories

- **Story 1.1.1** (Screen Sharing) - Completed; similar track verification patterns
- **Story 1.1.2** (Simulcast) - Completed; relevant for layer verification
- **Story 1.1.9** (Verify Ingress Stream Playback) - **Critical gap identified**; see [playback-verification-gap.md](./playback-verification-gap.md)
- **Story 1.1.10** (Record Ingress Stream via Egress) - Future; requires playback verification first
- **Story 1.1.11** (SIP Integration) - Future; similar external input pattern
- **Story 1.1.12** (Agents API) - Future; similar participant injection pattern

---

## Implementation Status

### Completed Stories (Story 1.1.8)

| Story | Description | Status |
|-------|-------------|--------|
| 1.1.8.1 | Ingress Container Infrastructure | DONE |
| 1.1.8.2 | Ingress Service Client | DONE |
| 1.1.8.3 | RTMP Stream Simulator (FFmpeg) | DONE |
| 1.1.8.4 | Create RTMP Ingress via API | DONE |
| 1.1.8.5 | Verify Ingress Participant Appears | DONE |
| 1.1.8.6 | Verify Ingress Track Subscription | DONE |
| 1.1.8.7 | Ingress Lifecycle (Delete/Cleanup) | DONE |
| 1.1.8.9 | Test Transcoding Presets | DONE |

### Story 1.1.9: Verify Ingress Stream Playback - COMPLETE

**Status:** DONE

Browser playback verification is now fully implemented using the `isReceivingVideoFrom` assertion which verifies:
- Track subscription status
- Track availability
- Frame dimensions or playing state
- Stream state (active/paused)

**Note:** Firefox is excluded from RTMP ingress tests due to H264 codec licensing limitations in Linux Selenium containers.

### Remaining Stories

| Story | Description | Status |
|-------|-------------|--------|
| 1.1.8.8 | Test URL Input Ingress | NOT STARTED |
| 1.1.8.10 | Test WHIP Ingress | NOT STARTED |
