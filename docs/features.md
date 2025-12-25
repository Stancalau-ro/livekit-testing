# LiveKit Testing Framework Features

This document provides comprehensive documentation of all implemented features in the LiveKit Testing Framework, including usage examples, configuration options, and integration patterns.

## Table of Contents

- [LiveKit Server Container Management](#livekit-server-container-management)
- [Egress Container Support](#egress-container-support)
- [Browser Automation with Selenium](#browser-automation-with-selenium)
- [BDD/Cucumber Testing](#bddcucumber-testing)
- [Access Token Generation and Management](#access-token-generation-and-management)
- [VNC Recording for Browser Tests](#vnc-recording-for-browser-tests)
- [Cross-Platform Path Handling](#cross-platform-path-handling)
- [CLI Publisher Container](#cli-publisher-container)
- [Webhook Event Testing](#webhook-event-testing)
- [Image Snapshot Capture](#image-snapshot-capture)
- [MinIO S3 Storage Integration](#minio-s3-storage-integration)
- [Room Management](#room-management)

---

## LiveKit Server Container Management

### Overview
The framework provides Docker-based LiveKit server management through the `LiveKitContainer` class, enabling automated deployment and testing against any LiveKit version.

### Features
- Automatic container lifecycle management
- Version-configurable LiveKit server deployment
- Network alias support for inter-container communication
- Configuration file binding for custom server settings
- Log capture to filesystem

### Usage

#### Basic Container Creation
```gherkin
Given the LiveKit config is set to "basic"
And a LiveKit server is running in a container with service name "livekit1"
```

#### Creating Rooms
```gherkin
When room "TestRoom" is created using service "livekit1"
Then the room count should be 1
```

#### Verifying Room State
```gherkin
Then room "TestRoom" should have 2 active participants in service "livekit1"
```

### Configuration

**Default Ports**:
- HTTP/WebSocket: 7880

**Default Credentials**:
- API Key: `devkey`
- Secret: `secret`

**Configuration Profiles**:
| Profile | Description |
|---------|-------------|
| `basic` | Simple standalone server |
| `basic_hook` | Server with webhook endpoint |
| `with_egress` | Server configured for egress service |
| `with_egress_hook` | Server with egress and webhook support |

### Version Override
```powershell
# Command line
./gradlew test -Plivekit_docker_version=v1.8.5

# System property
./gradlew test -Dlivekit.version=v1.8.5
```

---

## Egress Container Support

### Overview
The Egress container enables video recording and streaming capabilities through LiveKit's egress service.

### Features
- Room composite recording (entire room view)
- Track composite recording (individual participant tracks)
- Local filesystem output
- S3-compatible storage output (MinIO)
- Webhook event notification for egress lifecycle
- Configurable Chrome rendering flags

### Usage

#### Setup Egress Infrastructure
```gherkin
Given the LiveKit config is set to "with_egress_hook"
And a mock HTTP server is running in a container with service name "mockserver1"
And a Redis server is running in a container with service name "redis"
And a LiveKit server is running in a container with service name "livekit1"
And a LiveKit egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"
```

#### Room Composite Recording
```gherkin
When room composite recording is started for room "EgressRecordingRoom" using LiveKit service "livekit1"
# ... recording runs ...
When the recording runs for 6 seconds
And room composite recording is stopped for room "EgressRecordingRoom" using LiveKit service "livekit1"
Then the recording file exists in the output directory for room "EgressRecordingRoom"
And the recording file contains actual video content
```

#### Track Composite Recording
```gherkin
When track IDs are captured for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit1"
And track composite recording is started for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit1"
# ... recording runs ...
And track composite recording is stopped for participant "Thomas" using LiveKit service "livekit1"
Then the track composite recording file exists for participant "Thomas"
```

### Dependencies
- Redis container (required for job coordination)
- MinIO container (optional, for S3 storage)

### Version Override
```powershell
./gradlew test -Pegress_docker_version=v1.8.5
```

---

## Browser Automation with Selenium

### Overview
The framework provides cross-browser WebRTC testing through containerized Selenium WebDriver instances with fake media streams.

### Supported Browsers
- Chrome
- Firefox
- Edge

### Features
- Fake media stream injection for testing
- WebRTC-specific browser configurations
- Automatic permission handling
- Insecure origin support for container networking
- VNC session recording
- Multi-browser parallel testing

### Usage

#### Opening Browser with LiveKit Meet
```gherkin
When "Alice" opens a "Chrome" browser with LiveKit Meet page
And "Alice" connects to room "TestRoom" using the access token
And connection is established successfully for "Alice"
```

#### Multi-Browser Testing
```gherkin
When "Alice" opens a "Chrome" browser with LiveKit Meet page
And "Charlie" opens a "Firefox" browser with LiveKit Meet page
```

#### Verifying Publishing State
```gherkin
Then participant "Alice" should be publishing video in room "TestRoom" using service "livekit1"
And participant "Alice" should see 1 remote video tracks in room "TestRoom" using service "livekit1"
```

#### Leaving Meeting
```gherkin
When "Sarah" leaves the meeting
Then "Sarah" should be disconnected from the room
And "Sarah" should see the join form again
```

### Chrome Configuration
```
Arguments:
- use-fake-device-for-media-stream
- use-fake-ui-for-media-stream
- --disable-field-trial-config
- --disable-features=WebRtcHideLocalIpsWithMdns
- --disable-dev-shm-usage
- --reduce-security-for-testing
- --allow-running-insecure-content
- --unsafely-treat-insecure-origin-as-secure={url}
```

### Firefox Configuration
```
Preferences:
- permissions.default.microphone: 1
- permissions.default.camera: 1
- media.navigator.streams.fake: true
- media.navigator.permission.disabled: true
- media.peerconnection.enabled: true
- media.devices.insecure.enabled: true
- media.getusermedia.insecure.enabled: true
```

---

## BDD/Cucumber Testing

### Overview
The framework uses Cucumber for behavior-driven development, enabling readable test scenarios with Gherkin syntax.

### Features
- Gherkin feature files for test scenarios
- Reusable step definitions
- Scenario-level isolation through ManagerProvider
- Before/After hooks for setup and cleanup
- Data tables for complex assertions

### Feature Files

| Feature | Scenarios | Description |
|---------|-----------|-------------|
| `livekit_access_token.feature` | 13 | Token generation with all grant types |
| `livekit_rooms.feature` | 2 | Room creation and listing |
| `livekit_webrtc_publish.feature` | 3 | Browser video publishing |
| `livekit_webrtc_playback.feature` | - | Video playback testing |
| `livekit_webhooks.feature` | 3 | Webhook event validation |
| `livekit_egress_recording.feature` | 4 | Video recording scenarios |
| `livekit_image_snapshots.feature` | 2 | Image capture scenarios |
| `livekit_cli_publisher.feature` | 9 | CLI-based load testing |
| `livekit_minio_recording.feature` | - | S3 storage integration |
| `livekit_participant_removal.feature` | - | Participant management |
| `livekit_screen_sharing.feature` | 4 | Screen sharing functionality |
| `livekit_simulcast.feature` | 5 | Multi-layer video publishing |
| `livekit_track_mute.feature` | 4 | Track mute/unmute operations |
| `livekit_data_channel.feature` | 16 | Data channel messaging |
| `livekit_dynacast.feature` | 4 | Bandwidth adaptation testing |

### Running Tests

```powershell
# Run all BDD tests
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests"

# Run specific scenario by name
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Generate access token with admin permissions for Dave"

# Run with specific LiveKit version
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Plivekit_docker_version=v1.8.5
```

### Step Definition Organization

| Step Class | Purpose |
|------------|---------|
| `BaseSteps` | Manager lifecycle hooks |
| `ManagerProvider` | Thread-safe manager access |
| `LiveKitLifecycleSteps` | Container/room lifecycle |
| `LiveKitAccessTokenSteps` | Token operations |
| `LiveKitBrowserWebrtcSteps` | Browser automation |
| `LiveKitCLIPublisherSteps` | CLI publisher management |
| `LiveKitEgressSteps` | Recording operations |
| `LiveKitImageSnapshotSteps` | Snapshot capture |
| `LiveKitRoomSteps` | Room API operations |
| `LiveKitWebhookSteps` | Webhook verification |

---

## Access Token Generation and Management

### Overview
Comprehensive LiveKit access token management supporting all 17 VideoGrant types, custom attributes, and token expiration.

### Features
- All VideoGrant types supported
- Custom token attributes with escape sequence handling
- Configurable TTL/expiration
- Identity and room-based token storage
- Grant validation in assertions

### Supported Grant Types

| Grant | Description | Usage |
|-------|-------------|-------|
| `roomJoin` | Join room permission | Always true when room specified |
| `canPublish` | Publish audio/video tracks | Participant publishing |
| `canSubscribe` | Subscribe to other tracks | Participant subscription |
| `canPublishData` | Send data messages | Data channel communication |
| `canUpdateOwnMetadata` | Update own metadata | Self-service metadata |
| `roomCreate` | Create new rooms | Admin operations |
| `roomList` | List existing rooms | Admin operations |
| `roomRecord` | Record rooms | Recording initiation |
| `roomAdmin` | Full room administration | Admin operations |
| `hidden` | Hidden participant | Observer/bot participants |
| `recorder` | Recorder participant | Recording service |
| `ingressAdmin` | Ingress administration | Ingress management |
| `agent` | AI agent participant | Agent integration |

### Usage

#### Basic Token with Permissions
```gherkin
When an access token is created with identity "Bob" and room "BobsRoom" with publish permissions
Then the access token for "Bob" in room "BobsRoom" should be valid
```

#### Token with Dynamic Grants
```gherkin
When an access token is created with identity "Charlie" and room "MeetingRoom" with grants "canPublish:true,canSubscribe:true"
Then the access token for "Charlie" in room "MeetingRoom" should have the following grants:
  | grant        | value |
  | canPublish   | true  |
  | canSubscribe | true  |
  | roomJoin     | true  |
```

#### Token with Custom Attributes
```gherkin
When an access token is created with identity "Frank" and room "CustomRoom" with grants "canPublish:true" and attributes "role=moderator,department=engineering,level=senior"
Then the access token for "Frank" in room "CustomRoom" should have the following attributes:
  | attribute  | value       |
  | role       | moderator   |
  | department | engineering |
  | level      | senior      |
```

#### Token with Escaped Commas in Attributes
```gherkin
When an access token is created with identity "Grace" and room "TestRoom" with grants "canPublish:true,canSubscribe:true" and attributes "description=A room for testing\, debugging\, and development,tags=test\,debug\,dev"
Then the access token for "Grace" in room "TestRoom" should have the following attributes:
  | attribute   | value                                          |
  | description | A room for testing, debugging, and development |
  | tags        | test,debug,dev                                 |
```

#### Token with Expiration
```gherkin
When an access token is created with identity "Jack" and room "TempRoom" with grants "canPublish:true,canSubscribe:true" that expires in 30 seconds
When an access token is created with identity "Kate" and room "LongTermRoom" with grants "roomAdmin:true" that expires in 60 minutes
```

### Grant Assertions

#### Positive Assertion
```gherkin
Then the access token for "Dave" in room "AdminRoom" should have the following grants:
  | grant      | value |
  | roomAdmin  | true  |
  | roomCreate | true  |
  | roomList   | true  |
  | roomJoin   | true  |
```

#### Negative Assertion
```gherkin
And the access token for "Dave" in room "AdminRoom" should not have the following grants:
  | grant      |
  | canPublish |
```

---

## VNC Recording for Browser Tests

### Overview
Automatic VNC session recording for browser tests, capturing visual state for debugging and test evidence.

### Features
- Three recording modes: all, failed, skip
- Scenario-specific recording directories
- MP4 output format
- Automatic recording start/stop
- Test result-aware file naming

### Recording Modes

| Mode | Description | Output |
|------|-------------|--------|
| `skip` | No recordings | None |
| `all` | Record all tests | All browser sessions |
| `failed` | Only failed tests | Only failing scenarios |

### Configuration

```powershell
# Record all tests (default)
./gradlew test -Drecording.mode=all

# Record only failed tests
./gradlew test -Drecording.mode=failed

# Skip recordings
./gradlew test -Drecording.mode=skip
```

### Output Structure
```
out/bdd/scenarios/{feature}/{scenario}/{timestamp}/
    vnc-recordings/
        PASSED-webdriver-publish-Alice.mp4
        FAILED-webdriver-meet-Bob.mp4
```

### Recording Lifecycle
1. Browser container starts with VNC enabled
2. `beforeTest()` called to start recording
3. Test executes with VNC capture
4. `afterTest()` called with pass/fail status
5. Recording saved with status prefix
6. Polling waits for file write completion

---

## Cross-Platform Path Handling

### Overview
PathUtils provides cross-platform path operations ensuring the framework runs on both Windows and Linux.

### Features
- Platform-independent path joining
- Scenario-specific output directories
- Container log path generation
- Configuration file path resolution

### Key Methods

| Method | Purpose |
|--------|---------|
| `join(first, more...)` | Join path segments using platform separator |
| `file(first, more...)` | Create File from path segments |
| `scenarioPath(...)` | Generate scenario output path |
| `currentScenarioPath()` | Get current scenario directory |
| `containerLogPath(...)` | Generate container log path |
| `livekitConfigPath(...)` | Get version-aware config path |
| `egressConfigPath(...)` | Get egress config path |
| `containerPath(...)` | Unix-style path for containers |

### Directory Structure
```
out/
    bdd/
        scenarios/
            current/
                docker/
                    livekit/
                    egress/
                video-recordings/
                snapshots/
                vnc-recordings/
            {feature}/
                {scenario}/
                    {timestamp}/
                        ...
```

---

## CLI Publisher Container

### Overview
The CLI Publisher container uses LiveKit's official CLI tool to simulate participants for load testing and multi-participant scenarios.

### Features
- Load test mode with configurable publishers
- Individual participant join mode
- Video resolution configuration
- Simulcast support
- Speaker simulation
- Progressive participant joining

### Usage

#### Load Test with Video Publishers
```gherkin
When a CLI load test publisher with 5 video publishers connects to room "LoadTestRoom" using service "livekit1"
Then room "LoadTestRoom" should have 5 active participants in service "livekit1"
```

#### Load Test with Audio Publishers
```gherkin
When a CLI load test publisher with 3 audio publishers connects to room "AudioTestRoom" using service "livekit1"
Then room "AudioTestRoom" should have 3 active participants in service "livekit1"
```

#### Mixed Publishers and Subscribers
```gherkin
When a CLI load test with 2 video publishers and 10 subscribers connects to room "MixedLoadRoom" using service "livekit1"
Then room "MixedLoadRoom" should have 12 active participants in service "livekit1"
```

#### Custom Configuration
```gherkin
When a CLI load test with config "videoPublishers:4,audioPublishers:2,videoResolution:medium,simulcast:true" connects to room "CustomConfigRoom" using service "livekit1"
```

#### Individual Publisher Join
```gherkin
When "John" starts a CLI publisher with "video" to room "MultiCLIRoom" using service "livekit1"
And "Amy" starts a CLI publisher with "audio" to room "MultiCLIRoom" using service "livekit1"
And "Collin" starts a CLI publisher with "a/v" to room "MultiCLIRoom" using service "livekit1"
```

#### Progressive Joining
```gherkin
When a CLI load test with config "videoPublishers:10,numPerSecond:2,duration:5" connects to room "ProgressiveRoom" using service "livekit1"
Then the CLI load test for room "ProgressiveRoom" should complete successfully within 10 seconds
```

#### Speaker Simulation
```gherkin
When a CLI load test with config "audioPublishers:5,simulateSpeakers:true,duration:10" connects to room "SpeakerRoom" using service "livekit1"
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `videoPublishers` | Number of video publishers | 0 |
| `audioPublishers` | Number of audio publishers | 0 |
| `subscribers` | Number of subscribers | 0 |
| `videoResolution` | Resolution (low, medium, high) | high |
| `simulcast` | Enable simulcast | true |
| `duration` | Test duration in seconds | none |
| `numPerSecond` | Participants joining per second | none |
| `layout` | Room layout | none |
| `simulateSpeakers` | Simulate speaking | false |

---

## Webhook Event Testing

### Overview
Webhook testing validates that LiveKit properly sends HTTP webhook notifications for room and participant lifecycle events.

### Features
- MockServer-based webhook capture
- Event type filtering
- Room-specific event isolation
- Participant attribute verification
- Event clearing for test isolation

### Supported Events

| Event Type | Trigger |
|------------|---------|
| `room_started` | Room created |
| `room_finished` | Room closed/deleted |
| `participant_joined` | Participant connects |
| `participant_left` | Participant disconnects |
| `track_published` | Track starts publishing |
| `track_unpublished` | Track stops publishing |
| `egress_started` | Recording begins |
| `egress_ended` | Recording completes |

### Usage

#### Setup Webhook Infrastructure
```gherkin
Given the LiveKit config is set to "basic_hook"
And a mock HTTP server is running in a container with service name "mockserver1"
And a LiveKit server is running in a container with service name "livekit1"
```

#### Verifying Room Events
```gherkin
When room "WebhookTestRoom" is created using service "livekit1"
Then "mockserver1" should have received a "room_started" event for room "WebhookTestRoom"

When room "WebhookTestRoom" is deleted using service "livekit1"
Then "mockserver1" should have received a "room_finished" event for room "WebhookTestRoom"
```

#### Verifying Participant Events
```gherkin
When "George" connects to room "WebhookTestRoom" using the access token
And connection is established successfully for "George"
Then "mockserver1" should have received a "participant_joined" event for participant "George" in room "WebhookTestRoom"
```

#### Verifying Track Events
```gherkin
Then "mockserver1" should have received a "track_published" event for track type "VIDEO" in room "WebhookTestRoom"
```

#### Verifying Participant Attributes in Events
```gherkin
Then "mockserver1" should have received a "participant_joined" event for participant "Elizabeth" in room "AttributesTestRoom" with attributes "role=admin,department=engineering,project=livekit-testing"
```

#### Event Isolation (Clearing)
```gherkin
When "mockserver1" webhook events are cleared

When room "RoomB" is created using service "livekit1"
Then "mockserver1" should have received a "room_started" event for room "RoomB"
And "mockserver1" should have received exactly 1 webhook event
And "mockserver1" should not have received a "room_started" event for room "RoomA"
```

#### Event Count Verification
```gherkin
And "mockserver1" should have received exactly 8 webhook events
And "mockserver1" should have received 2 "egress_started" events for room "MultiTrackCompositeRoom"
```

---

## Image Snapshot Capture

### Overview
On-demand image snapshot capture from video tracks and room composite views for thumbnails and content validation.

### Features
- Room composite snapshots
- Individual participant track snapshots
- S3 (MinIO) storage output
- Local filesystem output
- Image validation

### Usage

#### Setup Snapshot Infrastructure
```gherkin
Given the LiveKit config is set to "with_egress_hook"
And a Redis server is running in a container with service name "redis"
And a LiveKit server is running in a container with service name "livekit1"
And a MinIO server is running in a container with service name "minio1"
And a bucket "snapshots" is created in MinIO service "minio1"
And a LiveKit S3 snapshot egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"
```

#### Room Composite Snapshot to S3
```gherkin
When an on-demand snapshot is captured to S3 for room "SnapshotRoom" using LiveKit service "livekit1" and MinIO service "minio1"
Then the S3 snapshot image file exists for room "SnapshotRoom" using MinIO service "minio1"
And the S3 snapshot image is valid and contains actual image data
```

#### Room Composite Snapshot to Local
```gherkin
When an on-demand snapshot is captured to local filesystem for room "SnapshotRoom" using LiveKit service "livekit1"
Then the local snapshot image file exists for room "SnapshotRoom"
And the local snapshot image is valid and contains actual image data
```

#### Participant Track Snapshot to S3
```gherkin
When an on-demand snapshot is captured to S3 for participant "Bob" video track in room "ParticipantSnapshotRoom" using LiveKit service "livekit1" and MinIO service "minio1"
Then the S3 snapshot image file exists for participant "Bob" using MinIO service "minio1"
```

#### Participant Track Snapshot to Local
```gherkin
When an on-demand snapshot is captured to local filesystem for participant "Bob" video track in room "ParticipantSnapshotRoom" using LiveKit service "livekit1"
Then the local snapshot image file exists for participant "Bob"
And the local snapshot image is valid and contains actual image data
```

---

## MinIO S3 Storage Integration

### Overview
MinIO provides S3-compatible object storage for testing egress recording and snapshot storage functionality.

### Features
- S3-compatible API
- Bucket creation and management
- Configurable credentials
- Network accessibility from containers
- Health check monitoring

### Usage

#### Setup MinIO
```gherkin
Given a MinIO server is running in a container with service name "minio1"
And a bucket "recordings" is created in MinIO service "minio1"
```

#### Configure Egress with S3
```gherkin
And a LiveKit S3 snapshot egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"
```

#### Verify S3 Storage
```gherkin
Then the S3 snapshot image file exists for room "SnapshotRoom" using MinIO service "minio1"
And the S3 snapshot image is valid and contains actual image data
```

### Default Credentials
- Access Key: `minioadmin`
- Secret Key: `minioadmin`
- API Port: 9000
- Console Port: 9001

---

## Room Management

### Overview
Room management through LiveKit's RoomServiceClient API for creating, listing, and deleting rooms.

### Features
- Room creation
- Room listing
- Room deletion
- Participant counting
- Participant track verification

### Usage

#### Create Room
```gherkin
When room "TestRoom" is created using service "livekit1"
```

#### Verify Room Count
```gherkin
When all rooms are fetched from service "livekit1"
Then the room count should be 1
```

#### Verify No Rooms Exist
```gherkin
When all rooms are fetched from service "livekit1"
Then the room count should be 0
```

#### Verify Participant Count
```gherkin
Then room "MultiRoom" should have 2 active participants in service "livekit1"
```

#### Verify Publishing State
```gherkin
Then participant "Jack" should be publishing video in room "MultiRoom" using service "livekit1"
And participant "Emily" should not be publishing video in room "RestrictedRoom" using service "livekit1"
```

#### Verify Remote Tracks
```gherkin
Then participant "Jack" should see 1 remote video tracks in room "MultiRoom" using service "livekit1"
```

#### Delete Room
```gherkin
When room "WebhookTestRoom" is deleted using service "livekit1"
```

---

## Quick Reference

### Test Execution Commands

```powershell
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "ro.stancalau.test.framework.*"

# BDD tests only
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests"

# Specific scenario
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Generate access token with admin permissions for Dave"

# With version override
./gradlew test -Plivekit_docker_version=v1.8.5 -Pegress_docker_version=v1.8.5

# With recording mode
./gradlew test -Drecording.mode=failed

# Clean build
./gradlew clean test
```

### Configuration Quick Reference

| Setting | System Property | Env Variable | Gradle Property | Default |
|---------|----------------|--------------|-----------------|---------|
| LiveKit Version | `-Dlivekit.version` | `LIVEKIT_VERSION` | `-Plivekit_docker_version` | v1.8.4 |
| Egress Version | `-Degress.version` | `EGRESS_VERSION` | `-Pegress_docker_version` | v1.8.4 |
| JS SDK Version | `-Dlivekit.js.version` | `LIVEKIT_JS_VERSION` | `-Plivekit_js_version` | 2.6.4 |
| Recording Mode | `-Drecording.mode` | `RECORDING_MODE` | - | all |
