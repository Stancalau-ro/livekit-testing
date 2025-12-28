# LiveKit Testing Framework Roadmap

**Vision:** Become the go-to reference for online a/v streaming integration.

**Mission:** Livekit-testing automates feature, version and configuration testing in a consistent and extendable way.

---

## Current Coverage Summary

The framework currently tests:

- Access tokens (all 17 grant types, custom attributes, expiration)
- Room management (create, list, delete)
- WebRTC publishing and playback (Chrome, Firefox, Edge)
- Participant removal and reconnection
- Webhook event validation
- Egress recording (local and S3/MinIO)
- Image snapshots (room and participant tracks)
- CLI publisher integration (load testing)
- Screen sharing (publish, subscribe, quality settings)
- Simulcast video (multi-layer publishing, layer selection)
- Dynacast bandwidth adaptation (quality preference, bitrate monitoring)
- Data channel messaging (reliable/unreliable, broadcast, targeted)
- Track mute/unmute (local mute, remote visibility)
- Room and participant metadata (CRUD, events, propagation)

---

## Phase 1: Feature Coverage

*Focus: Add comprehensive tests for not-yet-tested LiveKit features*

### Epic 1.1: Expanded LiveKit Feature Coverage

**Goal:** Test all major LiveKit features including those not yet covered.

**Success Metrics:**

- Comprehensive coverage of LiveKit server features
- All new scenarios pass across supported versions
- Documentation exists for each tested feature

#### Story 1.1.1: Test Screen Sharing Functionality [DONE]

**As a** test developer
**I want** to test screen sharing between participants
**So that** I can verify screen share track publishing and subscription

**Acceptance Criteria:**

- Given a participant with screen share permissions
- When they share their screen
- Then the screen track is published to the room
- Given another participant subscribes
- When the screen share is active
- Then they receive the screen share track

- [x] Add permission grant for screen sharing (canPublishSources)
- [x] Simulate screen share from browser
- [x] Verify screen share track appears in room
- [x] Test subscriber receives screen share track
- [x] Test screen share quality settings
- [x] Verify screen share stops correctly

**Size:** M

**Implementation:** `src/test/resources/features/livekit_screen_sharing.feature`

---

#### Story 1.1.2: Test Simulcast Video Publishing [DONE]

**As a** test developer
**I want** to test simulcast video publishing
**So that** I can verify multiple quality layers are published

**Acceptance Criteria:**

- Given simulcast is enabled
- When a participant publishes video
- Then multiple quality layers are available
- Given bandwidth is constrained
- When adaptive streaming activates
- Then lower quality layer is selected

- [x] Enable simulcast in video publish settings
- [x] Verify multiple video layers exist
- [x] Test layer selection via SDK
- [x] Measure quality differences between layers
- [x] Test layer switching under bandwidth constraints

**Size:** M

**Implementation:** `src/test/resources/features/livekit_simulcast.feature`

---

#### Story 1.1.3: Test Dynacast Bandwidth Adaptation [DONE]

**As a** test developer
**I want** to test dynacast functionality
**So that** I can verify automatic quality adaptation

**Acceptance Criteria:**

- Given dynacast is enabled
- When bandwidth decreases
- Then video quality adapts automatically
- Given bandwidth recovers
- When sufficient capacity is available
- Then quality increases appropriately

- [x] Enable dynacast in room configuration
- [x] Simulate bandwidth constraints via quality preference API
- [x] Verify quality adaptation occurs
- [x] Measure adaptation response time via bitrate monitoring
- [x] Test recovery when bandwidth increases

**Size:** L

**Implementation:** `src/test/resources/features/livekit_dynacast.feature`

---

#### Story 1.1.4: Test Data Channel Communication [DONE]

**As a** test developer
**I want** to test data channel messaging
**So that** I can verify reliable and unreliable data delivery

**Acceptance Criteria:**

- Given two participants in a room
- When one sends a data message
- Then the other receives it
- Given reliable data channel is used
- When message is sent
- Then delivery is guaranteed

- [x] Add permission grant for data publishing (canPublishData)
- [x] Send message via reliable data channel
- [x] Verify message receipt by other participant
- [x] Test unreliable data channel
- [x] Measure data channel latency
- [x] Test large message handling (up to 15 KiB)

**Size:** M

**Implementation:** `docs/features/data-channel-testing/` - 9 sub-stories, 16 Gherkin scenarios

---

#### Story 1.1.5: Test Room Metadata Operations [DONE]

**As a** test developer
**I want** to test room metadata updates
**So that** I can verify metadata is correctly stored and propagated

**Acceptance Criteria:**

- Given a room exists
- When metadata is updated via API
- Then the metadata change is persisted
- Given a participant is connected
- When metadata changes
- Then the participant receives the update event

- [x] Set room metadata via RoomServiceClient
- [x] Verify metadata is retrievable
- [x] Test metadata update events
- [x] Verify participants receive metadata changes
- [x] Test metadata size limits

**Size:** S

**Implementation:** `src/test/resources/features/livekit_metadata.feature`

---

#### Story 1.1.6: Test Participant Metadata Operations [DONE]

**As a** test developer
**I want** to test participant metadata updates
**So that** I can verify participant-level metadata works correctly

**Acceptance Criteria:**

- Given a participant is connected
- When their metadata is updated
- Then other participants receive the update
- Given metadata is set at connection time
- When the participant joins
- Then the initial metadata is visible

- [x] Set participant metadata via token attributes
- [x] Update participant metadata via API
- [x] Verify metadata update events
- [x] Test other participants receive updates
- [x] Verify metadata in webhook events

**Size:** S

**Implementation:** `src/test/resources/features/livekit_metadata.feature`

---

#### Story 1.1.7: Test Track Mute and Unmute Operations [DONE]

**As a** test developer
**I want** to test track mute/unmute behavior
**So that** I can verify mute state is correctly propagated

**Acceptance Criteria:**

- Given a participant is publishing audio/video
- When they mute their track
- Then other participants see the mute state
- Given a track is muted
- When it is unmuted
- Then media flow resumes

- [x] Implement mute operation from browser
- [x] Verify mute state via server API
- [x] Test mute state visible to other participants
- [x] Verify unmute restores media flow
- [ ] Test server-initiated mute

**Size:** M

**Implementation:** `src/test/resources/features/livekit_track_mute.feature`

---

#### Story 1.1.8: Test Ingress Stream Input

**As a** test developer
**I want** to test ingress functionality
**So that** I can verify external streams can be brought into LiveKit rooms

**Acceptance Criteria:**

- Given an ingress is configured
- When an external stream connects
- Then participants see the ingress stream
- Given ingress is stopped
- When the stream ends
- Then the track is unpublished

- [ ] Add Ingress container to infrastructure
- [ ] Create ingress configuration via API
- [ ] Simulate external RTMP/WHIP stream
- [ ] Verify ingress track appears in room
- [ ] Test ingress stop/cleanup

**Size:** L

---

#### Story 1.1.9: Verify Ingress Stream Playback

**As a** test developer
**I want** to verify that browser participants can view ingress streams
**So that** I can confirm ingress produces playable media in the room

**Acceptance Criteria:**

- Given an RTMP ingress is streaming to a room
- When a browser participant joins and subscribes
- Then the participant receives video from the ingress
- And the video dimensions match the ingress configuration
- Given the ingress stream stops
- When the participant checks subscriptions
- Then the ingress track is no longer available

**Implementation Tasks:**
- [ ] Create browser participant that joins room with ingress
- [ ] Subscribe to ingress participant's video track
- [ ] Verify video frames are being received
- [ ] Validate video dimensions match expected resolution
- [ ] Test subscription cleanup when ingress stops

**Size:** M

---

#### Story 1.1.10: Record Ingress Stream via Egress

**As a** test developer
**I want** to record ingress streams to video files
**So that** I can verify ingress output quality and egress integration

**Acceptance Criteria:**

- Given an RTMP ingress is streaming to a room
- When a room composite egress is started
- Then the recording captures the ingress video
- Given the egress completes
- When the output file is validated
- Then it contains video matching the ingress duration
- And the video is playable and not corrupted

**Implementation Tasks:**
- [ ] Start ingress stream to room
- [ ] Configure and start room composite egress
- [ ] Wait for egress to complete
- [ ] Download recording from S3/MinIO
- [ ] Validate video file integrity and duration
- [ ] Verify video contains ingress content (not blank)

**Size:** M

---

#### Story 1.1.11: Test SIP Integration

**As a** test developer
**I want** to test SIP bridge functionality
**So that** I can verify phone participants can join rooms

**Acceptance Criteria:**

- Given SIP is configured
- When a SIP call connects
- Then the caller appears as participant
- Given a participant is on SIP
- When they speak
- Then audio is delivered to other participants

- [ ] Add SIP container to infrastructure
- [ ] Configure SIP trunk
- [ ] Simulate SIP call into room
- [ ] Verify SIP participant appears
- [ ] Test audio flow from SIP participant

**Size:** L

---

#### Story 1.1.12: Test Agents API Integration

**As a** test developer
**I want** to test the Agents API
**So that** I can verify AI agents can join and interact in rooms

**Acceptance Criteria:**

- Given an agent is registered
- When the agent joins a room
- Then it appears as a participant
- Given an agent publishes audio
- When other participants subscribe
- Then they receive the agent audio

- [ ] Research Agents API requirements
- [ ] Implement agent registration
- [ ] Create agent join scenario
- [ ] Test agent track publishing
- [ ] Verify agent events in webhooks

**Size:** L

---

---

## Phase 2: Foundation

*Focus: Stabilize the core, improve reliability, and establish infrastructure for growth*

### Epic 2.1: Test Reliability and Stability

**Goal:** Reduce test flakiness on stable infrastructure.

**Success Metrics:**

- Consistent test pass rate on stable environment
- Reduced average retry count per test suite
- Faster time to detect test failures

#### Story 2.1.1: Implement Container Health Checks

**As a** test framework user
**I want** containers to report health status before tests begin
**So that** tests do not fail due to container startup timing issues

**Acceptance Criteria:**

- Given a LiveKit container is starting
- When the container reports healthy
- Then tests can proceed
- Given a container fails health check within timeout
- When the timeout expires
- Then a clear error message is logged and the test fails fast

- [ ] Add health check endpoints for LiveKit container
- [ ] Add health check endpoints for Redis container
- [ ] Add health check endpoints for MinIO container
- [ ] Add health check endpoints for Egress container
- [ ] Implement configurable health check timeout
- [ ] Log health check status with timestamps

**Size:** M

---

#### Story 2.1.2: Implement Retry Mechanism for Transient Failures

**As a** CI/CD operator
**I want** tests to automatically retry on transient failures
**So that** network glitches do not cause false negatives

**Acceptance Criteria:**

- Given a test fails with a retryable exception
- When the retry policy allows another attempt
- Then the test is re-executed with fresh state
- Given a test fails with a non-retryable exception
- When the failure is analyzed
- Then no retry is attempted

- [ ] Define retryable exception types (network timeout, connection refused)
- [ ] Implement retry interceptor for JUnit 5
- [ ] Configure maximum retry attempts (default: 2)
- [ ] Log retry attempts with failure reasons
- [ ] Report final status after all retries exhausted
- [ ] Support retry configuration via system properties

**Size:** M

---

#### Story 2.1.3: Add WebRTC Connection Stability Monitoring

**As a** test developer
**I want** to monitor WebRTC connection state during tests
**So that** I can identify intermittent connection issues

**Acceptance Criteria:**

- Given a browser establishes WebRTC connection
- When connection state changes
- Then the state change is logged with timestamp
- Given connection drops unexpectedly during test
- When the drop is detected
- Then detailed diagnostics are captured

- [ ] Capture ICE connection state changes
- [ ] Log peer connection statistics during test
- [ ] Detect and report connection drops
- [ ] Store connection quality metrics per test
- [ ] Generate connection stability summary in test report

**Size:** S

---

#### Story 2.1.4: Implement Graceful Container Cleanup

**As a** test framework maintainer
**I want** containers to be cleaned up gracefully after each test
**So that** resource leaks do not affect subsequent tests

**Acceptance Criteria:**

- Given a test completes (pass or fail)
- When cleanup runs
- Then all containers are stopped in correct order
- Given a container fails to stop gracefully
- When the timeout expires
- Then the container is forcefully terminated

- [ ] Define container shutdown order (browser, egress, livekit, redis, minio)
- [ ] Implement graceful shutdown with timeout per container
- [ ] Force kill containers that exceed shutdown timeout
- [ ] Verify all container networks are removed
- [ ] Log cleanup status for debugging
- [ ] Handle orphaned containers from previous failed runs

**Size:** M

---

#### Story 2.1.5: Add Test Timeout Configuration

**As a** test developer
**I want** configurable timeouts for different test phases
**So that** slow tests fail fast with clear timeout messages

**Acceptance Criteria:**

- Given a test phase exceeds its timeout
- When the timeout is reached
- Then the test fails with a specific timeout exception
- Given different test types have different durations
- When configuring timeouts
- Then each phase can be configured independently

- [ ] Define timeout phases (container startup, connection, recording, cleanup)
- [ ] Implement per-phase timeout configuration
- [ ] Support timeout configuration via properties file
- [ ] Log timeout warnings at 75% threshold
- [ ] Include timeout diagnostics in failure messages

**Size:** S

---

#### Story 2.1.6: Pre-Download Docker Images Before Test Execution

**As a** test developer
**I want** all required Docker images pre-pulled before tests start
**So that** test execution is not delayed by image downloads

**Acceptance Criteria:**

- Given a test suite is starting
- When the pre-download phase runs
- Then all required Docker images are pulled before any test executes
- Given an image fails to download
- When the failure is detected
- Then a clear error message identifies the missing image and tests fail fast
- Given all images are already present locally
- When pre-download runs
- Then the phase completes quickly with no network activity

**Required Images:**
- LiveKit server (livekit/livekit-server)
- Egress (livekit/egress)
- Ingress (livekit/ingress)
- MinIO (minio/minio)
- Redis (redis)
- Selenium Chrome (selenium/standalone-chrome)
- Selenium Firefox (selenium/standalone-firefox)
- Selenium Edge (selenium/standalone-edge)

**Implementation Tasks:**
- [ ] Create DockerImagePreloader utility class
- [ ] Implement image existence check using Docker client
- [ ] Add parallel image pulling with progress logging
- [ ] Create Gradle task `preloadImages` for manual execution
- [ ] Integrate into test lifecycle via JUnit 5 extension or @BeforeAll hook
- [ ] Add timeout and retry for image pulls (configurable)
- [ ] Log image pull duration and sizes
- [ ] Support version-specific image tags from TestConfig
- [ ] Skip pre-download when all images present (fast path)
- [ ] Add `--skip-preload` flag for CI environments with pre-warmed caches

**Size:** M

---

#### Story 2.1.7: Pre-Flight Docker Environment Health Check

**As a** test developer
**I want** the test suite to check Docker environment health before running tests
**So that** I can detect and address environment issues before tests fail with obscure errors

**Acceptance Criteria:**

- Given the test suite is starting
- When pre-flight checks run
- Then Docker daemon connectivity is verified
- Given orphaned Docker networks exist
- When the threshold is exceeded
- Then a warning is issued with cleanup suggestion
- Given containers from previous runs are still running
- When detected
- Then a warning lists the stale containers
- Given Docker resource usage is high
- When limits are approached
- Then a warning indicates potential resource constraints

**Background:**
This story addresses the "all predefined address pools have been fully subnetted" error caused by orphaned Docker networks accumulating from interrupted test runs or improper cleanup.

**Implementation Tasks:**
- [ ] Create DockerEnvironmentChecker utility class
- [ ] Check Docker daemon is running and responsive
- [ ] Count existing Docker networks and warn if > 20 (configurable threshold)
- [ ] Detect orphaned networks by naming pattern (TestContainers GUID format)
- [ ] List running containers that match test container patterns
- [ ] Check available disk space on Docker volumes
- [ ] Check available memory for container allocation
- [ ] Log warnings with actionable remediation commands (e.g., `docker network prune -f`)
- [ ] Add configurable check levels: `fail`, `warn`, `skip`
- [ ] Integrate as JUnit 5 extension or Cucumber @BeforeAll hook
- [ ] Add system property to skip checks: `-Ddocker.preflight.skip=true`

**Size:** M

---

### Epic 2.2: Enhanced Error Handling and Diagnostics

**Goal:** Provide actionable error information for every test failure.

**Success Metrics:**

- All test failures include diagnostic information
- Reduced time to diagnose failures
- Clear root cause identification for test failures

#### Story 2.2.1: Implement Structured Logging for Test Execution

**As a** test developer
**I want** structured logs with consistent format
**So that** I can easily search and correlate log entries

**Acceptance Criteria:**

- Given a test is executing
- When any log entry is created
- Then it includes test name, step, timestamp, and context
- Given logs are exported
- When parsed by log aggregation tools
- Then they can be indexed by all standard fields

- [ ] Define structured log format (JSON or key-value)
- [ ] Add test context to all log entries (scenario, step)
- [ ] Include container names in container-related logs
- [ ] Add correlation ID for tracking across components
- [ ] Support log level configuration per component

**Size:** M

---

#### Story 2.2.2: Capture Container Logs on Test Failure

**As a** test developer
**I want** container logs captured when tests fail
**So that** I can diagnose container-side issues

**Acceptance Criteria:**

- Given a test fails
- When failure handling runs
- Then logs from all active containers are captured
- Given container logs are captured
- When stored as artifacts
- Then they are named with test identifier and timestamp

- [ ] Capture LiveKit server logs on failure
- [ ] Capture Egress container logs on failure
- [ ] Capture Redis logs on failure
- [ ] Capture browser console logs on failure
- [ ] Store logs with test identifier and timestamp
- [ ] Include last 1000 lines of each container log

**Size:** M

---

#### Story 2.2.3: Implement Browser Screenshot on Failure

**As a** test developer
**I want** browser screenshots captured on test failure
**So that** I can see the visual state at failure time

**Acceptance Criteria:**

- Given a browser test fails
- When failure handling runs
- Then a screenshot is captured from each active browser
- Given multiple browsers are active
- When screenshots are stored
- Then each is named with browser identifier and test name

- [ ] Capture screenshot from all active WebDriver instances
- [ ] Name screenshots with test name and browser type
- [ ] Store screenshots in standard artifacts directory
- [ ] Capture screenshot before browser cleanup
- [ ] Handle screenshot failure gracefully (log warning)

**Size:** S

---

#### Story 2.2.4: Add Network Diagnostics on Connection Failures

**As a** test developer
**I want** network diagnostics when connections fail
**So that** I can distinguish network from application issues

**Acceptance Criteria:**

- Given a connection failure occurs
- When diagnostics run
- Then container network status is logged
- Given network diagnostics complete
- When reviewing failure
- Then I can see port accessibility and DNS resolution status

- [ ] Check container network connectivity on failure
- [ ] Verify port accessibility between containers
- [ ] Log DNS resolution status
- [ ] Capture network interface status
- [ ] Include diagnostics in failure report

**Size:** S

---

#### Story 2.2.5: Create Failure Analysis Summary Report

**As a** test developer
**I want** a summary report for failed tests
**So that** I can quickly understand failure patterns

**Acceptance Criteria:**

- Given test execution completes
- When failures occurred
- Then a summary report is generated
- Given the summary is generated
- When reviewed
- Then it groups failures by type and includes frequency

- [ ] Generate failure summary after test suite completion
- [ ] Group failures by exception type
- [ ] Include failure frequency per test
- [ ] List most common failure causes
- [ ] Output summary to console and file

**Size:** M

---

### Epic 2.3: Version Testing Infrastructure

**Goal:** Enable testing against any LiveKit ecosystem version combination with full component configurability, protocol-aware compatibility checking, and automatic scenario filtering.

**Success Metrics:**

- All component versions (Server, Egress, JS Client, Java SDK) configurable via config/env vars
- Dynamic fetching of Docker images and JS libraries at runtime
- Protocol compatibility validated before test execution with early failure on incompatibility
- Protocol abstraction layer isolates version-specific behavior
- BDD scenarios with version annotations are automatically excluded when incompatible

**Research Context:**
- No official LiveKit compatibility matrix exists
- Protocol version is the primary compatibility indicator (currently at Protocol 15)
- Known issues: Server v1.5.x + Egress v1.7.x fails; Server v1.7.x → v1.8.x upgrade has protocol issues (fixed in v1.8.4)
- JS Client v2.x is compatible with Server v1.7.x and newer
- LiveKit follows "generally backward compatible" approach

---

#### Story 2.3.1: Complete Version Configuration for All Components [PARTIAL]

**As a** test developer
**I want** all ecosystem component versions configurable via config or environment variables
**So that** I can test any version combination without code changes

**Acceptance Criteria:**

- Given any component (Server, Egress, JS Client, Java SDK)
- When I set its version via system property, env var, or gradle property
- Then tests use that specific version
- Given no version is specified
- When tests run
- Then the default version is used

**Current State:**
- [x] LiveKit Server version: configurable via `-Dlivekit.version`, `LIVEKIT_VERSION`, `-Plivekit_docker_version`
- [x] Egress version: configurable via `-Degress.version`, `EGRESS_VERSION`, `-Pegress_docker_version`
- [x] JS Client version: configurable via `-Dlivekit.js.version`, `LIVEKIT_JS_VERSION`, `-Plivekit_js_version` (newly added)
- [x] Web resources moved to main resources with dynamic JS SDK version URL parameter
- [ ] Java Server SDK version: currently fixed at compile-time in build.gradle (0.8.5)
- [ ] Redis version: add configuration support
- [ ] Selenium container version: add configuration support

**Remaining Tasks:**
- [ ] Add Java SDK version to TestConfig (for documentation/logging purposes)
- [ ] Add Redis version configuration
- [ ] Add Selenium version configuration
- [ ] Log all component versions at test suite startup
- [ ] Create version summary report after test execution

**Size:** S

---

#### Story 2.3.2: Dynamic Docker Image Fetching

**As a** test developer
**I want** Docker images fetched dynamically based on version configuration
**So that** I can test any released version without pre-pulling images

**Acceptance Criteria:**

- Given a version is specified that isn't locally available
- When container starts
- Then the image is pulled automatically
- Given a version doesn't exist on Docker Hub
- When container attempts to start
- Then a clear error message identifies the invalid version

- [x] LiveKitContainer fetches image dynamically (already implemented)
- [x] EgressContainer fetches image dynamically (already implemented)
- [ ] Add image existence validation before test execution
- [ ] Implement image pull with progress logging
- [ ] Add timeout and retry for image pulls
- [ ] Cache pulled image list to avoid repeated checks

**Size:** S

---

#### Story 2.3.3: Dynamic JavaScript Client Library Fetching

**As a** test developer
**I want** the JS client library fetched dynamically based on version configuration
**So that** I can test any JS client version without manually downloading files

**Acceptance Criteria:**

- Given a JS client version is specified
- When tests start
- Then the correct version is available for browser tests
- Given the version isn't cached locally
- When requested
- Then it is downloaded from npm/unpkg CDN automatically

**Current State:**
- JS client v2.6.4 is bundled statically at `src/test/resources/web/livekit-meet/lib/livekit-client/v2.6.4/`

**Implementation Tasks:**
- [ ] Create JsClientLibraryManager class for version management
- [ ] Implement download from unpkg.com CDN: `https://unpkg.com/livekit-client@{version}/dist/livekit-client.umd.min.js`
- [ ] Cache downloaded versions in `lib/livekit-client/{version}/` directory
- [ ] Modify WebServerContainer to serve the configured version
- [ ] Update HTML template to use dynamic script path or modify at runtime
- [ ] Add version validation before download
- [ ] Handle download failures gracefully with clear error messages
- [ ] Add BDD step: `Given the JS client version is set to "{version}"`

**Size:** M

---

#### Story 2.3.4: Protocol Version Detection and Extraction

**As a** test developer
**I want** to detect and extract protocol versions from all components
**So that** compatibility can be validated programmatically

**Acceptance Criteria:**

- Given a LiveKit server container is running
- When protocol version is queried
- Then the server's protocol version is returned
- Given a JS client is loaded in browser
- When protocol version is queried
- Then the client's supported protocol version is returned

**Implementation Tasks:**
- [ ] Create ProtocolVersionExtractor interface
- [ ] Implement ServerProtocolExtractor using server health endpoint or WebSocket connection
- [ ] Implement JsClientProtocolExtractor using browser JavaScript execution
- [ ] Create ProtocolVersion record with major, minor, features
- [ ] Add protocol version to container logs on startup
- [ ] Store extracted versions in state manager for later use
- [ ] Research: Check if `room.serverInfo.protocol` is accessible via Selenium

**Size:** M

---

#### Story 2.3.5: Protocol Compatibility Rules Engine

**As a** test developer
**I want** a rules engine that defines protocol compatibility between components
**So that** incompatible combinations are detected before tests run

**Acceptance Criteria:**

- Given a compatibility rule exists
- When component versions violate the rule
- Then the violation is identified with reason
- Given known bad combinations exist
- When those versions are specified
- Then tests fail early with clear explanation

**Known Compatibility Issues to Encode:**
- Server v1.5.x + Egress v1.7.x: Update fails
- Server v1.7.x → v1.8.x: Protocol issues (fixed in v1.8.4)
- Dynacast with Server v0.15.1: Known bug, client auto-disables
- Server < v1.5.2: Missing identity info in disconnect updates

**Implementation Tasks:**
- [ ] Create CompatibilityRule interface with `isCompatible(versions)` and `getViolationMessage()`
- [ ] Create ProtocolCompatibilityRules class with rule registry
- [ ] Define rules in YAML configuration: `src/test/resources/compatibility-rules.yaml`
- [ ] Support rule types: `version-range`, `exact-match`, `protocol-minimum`, `known-bad`
- [ ] Implement rule evaluation with detailed violation reporting
- [ ] Add configurable strictness levels: `fail`, `warn`, `ignore`

**Size:** M

---

#### Story 2.3.6: Pre-Test Protocol Compatibility Validation

**As a** CI/CD operator
**I want** protocol compatibility validated before any tests run
**So that** incompatible configurations fail fast with actionable error messages

**Acceptance Criteria:**

- Given tests are starting
- When configured versions are incompatible
- Then tests fail immediately with compatibility error
- Given compatibility check passes
- When tests proceed
- Then no compatibility-related failures occur mid-test

**Implementation Tasks:**
- [ ] Create CompatibilityValidator class as JUnit 5 extension
- [ ] Hook into test lifecycle: validate before @BeforeAll
- [ ] Extract protocol versions from running containers
- [ ] Evaluate all compatibility rules against current versions
- [ ] Fail with detailed report: which rule failed, what versions, what's recommended
- [ ] Add `@SkipCompatibilityCheck` annotation for override
- [ ] Log compatibility validation results with timestamps
- [ ] Add Cucumber @Before hook for BDD tests

**Size:** M

**Dependencies:** Story 2.3.4, Story 2.3.5

---

#### Story 2.3.7: Protocol Abstraction Layer

**As a** framework maintainer
**I want** protocol-specific code isolated in an abstraction layer
**So that** new protocol versions only require changes in one place

**Acceptance Criteria:**

- Given a new protocol version is released
- When support is added
- Then only the abstraction layer changes
- Given the abstraction layer is updated
- When tests run
- Then all existing tests work without modification

**Design Principles:**
- Protocol-specific behavior behind interfaces
- Version detection at startup, then use appropriate implementation
- No version checks scattered through test code
- Factory pattern for protocol-aware components

**Implementation Tasks:**
- [ ] Create `ProtocolAdapter` interface for protocol-specific operations
- [ ] Create `ProtocolAdapterFactory` that selects adapter by protocol version
- [ ] Implement `Protocol15Adapter` for current protocol
- [ ] Define adapter methods: `createVideoGrant()`, `parseParticipantInfo()`, `getTrackPublishOptions()`
- [ ] Move version-specific token grant handling to adapter
- [ ] Move version-specific event parsing to adapter
- [ ] Add adapter registration mechanism for new protocol versions
- [ ] Document how to add support for new protocol versions

**Size:** L

---

#### Story 2.3.8: BDD Scenario Version Annotations

**As a** test developer
**I want** to annotate BDD scenarios with min/max version requirements
**So that** incompatible scenarios are automatically excluded from test runs

**Acceptance Criteria:**

- Given a scenario has `@MinServerVersion(v1.9.0)` annotation
- When tests run with server v1.8.4
- Then the scenario is skipped with reason
- Given a scenario has no version annotation
- When tests run
- Then the scenario runs regardless of version

**Annotation Types:**
- `@MinServerVersion("v1.9.0")` - Minimum server version required
- `@MaxServerVersion("v1.8.4")` - Maximum server version supported
- `@MinJsClientVersion("2.10.0")` - Minimum JS client version
- `@MinProtocolVersion(15)` - Minimum protocol version
- `@RequiresFeature("dynacast")` - Requires specific feature
- `@SkipOnVersion(server="v1.5.2", reason="Known egress bug")` - Skip specific versions

**Implementation Tasks:**
- [ ] Define Cucumber tag format: `@MinServerVersion:v1.9.0`
- [ ] Create VersionTagFilter class implementing Cucumber TagPredicate
- [ ] Parse version tags from scenario tags
- [ ] Compare against current TestConfig versions
- [ ] Skip scenarios with clear skip message in report
- [ ] Add version requirement to scenario documentation
- [ ] Support tag combinations: `@MinServerVersion:v1.9.0 @MinJsClient:2.10.0`
- [ ] Log skipped scenarios with reasons at test start

**Size:** M

**Dependencies:** Story 2.3.1

---

#### Story 2.3.9: Version Compatibility Documentation Generator

**As a** LiveKit integrator
**I want** auto-generated documentation of tested version combinations
**So that** I know which versions work together

**Acceptance Criteria:**

- Given tests have run against version combinations
- When documentation is generated
- Then compatibility matrix shows pass/fail per combination
- Given a combination hasn't been tested
- When viewing documentation
- Then it's marked as "untested" with recommendation

**Implementation Tasks:**
- [ ] Create TestResultCollector that tracks version combinations
- [ ] Store results in `docs/version-compatibility.md`
- [ ] Auto-update after test runs (via Gradle task)
- [ ] Include test date, pass/fail count, known issues
- [ ] Generate matrix format: Server versions × Client versions
- [ ] Add links to detailed test reports per combination
- [ ] Create badge generator for README

**Size:** M

---

#### Story 2.3.10: Dynamic Configuration by Version [ENHANCED]

**As a** test developer
**I want** configuration to adapt automatically to the target version
**So that** version-specific settings are applied without manual intervention

**Acceptance Criteria:**

- Given a specific LiveKit version is targeted
- When configuration is loaded
- Then version-appropriate settings are applied
- Given a version has deprecated settings
- When those settings are present
- Then they are ignored with a warning

- [x] Version-specific config directories exist (v1.8.4/)
- [x] Fallback mechanism finds latest config when current version config missing
- [ ] Add version-specific capability detection
- [ ] Auto-generate config for new versions from template
- [ ] Detect and warn about deprecated configuration options
- [ ] Log configuration differences between versions
- [ ] Support configuration override for testing

**Size:** M

---

#### Story 2.3.11: Version Downgrade Testing Support

**As a** test developer
**I want** to test downgrade scenarios
**So that** I can verify backward compatibility

**Acceptance Criteria:**

- Given a room exists on version X
- When LiveKit is restarted with version Y (older)
- Then the downgrade behavior is validated
- Given downgrade fails
- When the failure is detected
- Then specific incompatibility is reported

- [ ] Support container restart with different version
- [ ] Preserve room state across version changes (where applicable)
- [ ] Detect version-specific behavior differences
- [ ] Log version transition events
- [ ] Document expected downgrade behavior per version

**Size:** L

**Dependencies:** Story 2.3.10

---

---

## Phase 3: Growth

*Focus: Expand automation capabilities and public reporting*

### Epic 3.1: Release Automation and CI/CD Integration

**Goal:** Automatically test against new LiveKit releases.

**Success Metrics:**

- Tests run automatically after new releases
- GitHub Actions workflow triggers reliably
- Test results available promptly after trigger

#### Story 3.1.1: Create GitHub Actions Workflow for Manual Testing

**As a** project maintainer
**I want** to trigger test runs via GitHub Actions
**So that** I can run tests against any version on demand

**Acceptance Criteria:**

- Given a workflow dispatch is triggered
- When version parameters are provided
- Then tests run against specified versions
- Given tests complete
- When results are available
- Then they are visible in GitHub Actions UI

- [ ] Create workflow_dispatch workflow
- [ ] Accept LiveKit version as input
- [ ] Accept Egress version as input
- [ ] Configure Docker-in-Docker for TestContainers
- [ ] Store test results as artifacts
- [ ] Report test summary in workflow output

**Size:** M

---

#### Story 3.1.2: Implement Scheduled Test Runs

**As a** project maintainer
**I want** tests to run on a schedule
**So that** regressions are detected automatically

**Acceptance Criteria:**

- Given the schedule time is reached
- When the workflow triggers
- Then tests run against configured versions
- Given a scheduled run fails
- When failure is detected
- Then notification is sent

- [ ] Configure cron schedule for daily runs
- [ ] Test against latest stable version
- [ ] Test against pinned baseline version
- [ ] Generate daily test report
- [ ] Configure failure notifications

**Size:** S

**Dependencies:** Story 3.1.1

---

#### Story 3.1.3: Detect New LiveKit Releases Automatically

**As a** project maintainer
**I want** new releases detected automatically
**So that** tests run against new versions promptly

**Acceptance Criteria:**

- Given a new LiveKit version is released
- When the detection mechanism runs
- Then the new version is identified
- Given a new version is detected
- When trigger conditions are met
- Then test workflow is started

- [ ] Monitor Docker Hub for new livekit/livekit-server tags
- [ ] Monitor Docker Hub for new livekit/egress tags
- [ ] Filter out pre-release/RC versions (configurable)
- [ ] Trigger workflow on new release detection
- [ ] Log version detection events

**Size:** M

---

#### Story 3.1.4: Implement Version Matrix Testing

**As a** project maintainer
**I want** tests to run against a matrix of versions provided as input configuration
**So that** I can validate compatibility across explicitly specified version combinations

**Acceptance Criteria:**

- Given a version matrix configuration file exists
- When the matrix workflow is triggered
- Then tests run against each specified version combination
- Given version combinations are provided as workflow input
- When the workflow runs
- Then only the provided combinations are tested
- Given matrix results are available
- When reviewing
- Then each combination result is clearly identified

- [ ] Define version matrix configuration format (YAML/JSON)
- [ ] Accept version matrix as workflow input parameter
- [ ] Validate provided version combinations before execution
- [ ] Generate matrix combinations from provided config
- [ ] Run tests in parallel where possible
- [ ] Aggregate results by version combination
- [ ] Generate compatibility report from matrix

**Size:** L

**Dependencies:** Story 3.1.1, Story 2.3.1

---

#### Story 3.1.5: Create Pull Request Testing Workflow

**As a** contributor
**I want** tests to run on pull requests
**So that** I know if my changes break existing functionality

**Acceptance Criteria:**

- Given a pull request is opened
- When tests trigger
- Then tests run against PR branch
- Given tests complete
- When results are available
- Then status is reported on PR

- [ ] Configure pull_request trigger
- [ ] Run tests against PR branch
- [ ] Report test status as PR check
- [ ] Comment test summary on PR
- [ ] Block merge on test failure (optional)

**Size:** M

---

### Epic 3.2: Public Test Reports and Dashboards

**Goal:** Make test results publicly accessible with historical tracking.

**Success Metrics:**

- Public dashboard available continuously
- Historical data retained for configurable period
- Badge reflects current test status

#### Story 3.2.1: Generate HTML Test Reports

**As a** stakeholder
**I want** HTML test reports generated after each run
**So that** I can view results in a browser

**Acceptance Criteria:**

- Given tests complete
- When report generation runs
- Then HTML report is created
- Given the HTML report is opened
- When viewing
- Then it shows pass/fail status, duration, and details

- [ ] Configure Cucumber HTML reporter
- [ ] Include scenario outcomes with details
- [ ] Add execution time per scenario
- [ ] Include VNC recording links for failures
- [ ] Style report for readability

**Size:** S

---

#### Story 3.2.2: Publish Test Reports to GitHub Pages

**As a** stakeholder
**I want** test reports published to a public URL
**So that** anyone can view the latest results

**Acceptance Criteria:**

- Given tests complete in CI
- When publish step runs
- Then report is uploaded to GitHub Pages
- Given the public URL is accessed
- When loading
- Then the latest report is displayed

- [ ] Configure GitHub Pages for repository
- [ ] Add workflow step to deploy report
- [ ] Organize reports by date/version
- [ ] Create index page listing all reports
- [ ] Add navigation between reports

**Size:** M

**Dependencies:** Story 3.2.1

---

#### Story 3.2.3: Create Test Status Badges

**As a** project visitor
**I want** test status badges in README
**So that** I can see current test status at a glance

**Acceptance Criteria:**

- Given tests have run
- When badge URL is accessed
- Then current status is shown
- Given tests are failing
- When badge is viewed
- Then it shows failing status with appropriate color

- [ ] Generate badge after each test run
- [ ] Include pass/fail/skip counts
- [ ] Support version-specific badges
- [ ] Add badges to README
- [ ] Cache badges appropriately

**Size:** S

**Dependencies:** Story 3.1.1

---

#### Story 3.2.4: Implement Historical Trend Dashboard

**As a** project maintainer
**I want** to see test result trends over time
**So that** I can identify stability patterns

**Acceptance Criteria:**

- Given multiple test runs have completed
- When dashboard loads
- Then historical trends are visualized
- Given a date range is selected
- When filtering
- Then only that range is displayed

- [ ] Store test results in persistent storage
- [ ] Create pass rate trend chart
- [ ] Show execution time trends
- [ ] Display failure patterns over time
- [ ] Support filtering by version/date

**Size:** L

**Dependencies:** Story 3.2.2

---

#### Story 3.2.5: Add Version Compatibility Dashboard

**As a** LiveKit integrator
**I want** to see which version combinations are tested and passing
**So that** I can choose compatible versions for my project

**Acceptance Criteria:**

- Given matrix tests have run
- When compatibility dashboard loads
- Then version grid shows test status
- Given a cell is clicked
- When detail view opens
- Then test results for that combination are shown

- [ ] Create version matrix grid visualization
- [ ] Color-code cells by pass/fail status
- [ ] Show last test date per combination
- [ ] Link to detailed report per combination
- [ ] Update automatically after matrix runs

**Size:** M

**Dependencies:** Story 3.1.4, Story 3.2.4

---

---

## Phase 4: Scale

*Focus: Build knowledge base and optimize for production use*

### Epic 4.1: Knowledge Collection and Documentation

**Goal:** Systematically capture testing knowledge and compatibility data.

**Success Metrics:**

- Compatibility matrix covers all tested version combinations
- Known issues database captures 100% of recurring failures
- Documentation auto-updates with each release

#### Story 4.1.1: Create Compatibility Matrix Documentation

**As a** LiveKit integrator
**I want** a documented compatibility matrix
**So that** I know which versions work together

**Acceptance Criteria:**

- Given tests have run against version combinations
- When documentation is generated
- Then compatibility matrix is updated
- Given the matrix is viewed
- When checking a combination
- Then test status and notes are visible

- [ ] Define compatibility matrix markdown format
- [ ] Auto-generate from test results
- [ ] Include test date and pass/fail status
- [ ] Add notes for known issues per combination
- [ ] Version the matrix with repository

**Size:** M

**Dependencies:** Story 3.1.4

---

#### Story 4.1.2: Implement Known Issues Database

**As a** test developer
**I want** a database of known issues
**So that** recurring failures are documented and tracked

**Acceptance Criteria:**

- Given a test failure matches a known pattern
- When the failure is analyzed
- Then it is linked to the known issue
- Given a new failure occurs
- When it does not match known patterns
- Then it is flagged for investigation

- [ ] Define known issues format (YAML/JSON)
- [ ] Match failure patterns to known issues
- [ ] Link failures to GitHub issues when available
- [ ] Track issue resolution status
- [ ] Exclude known issues from failure counts (configurable)

**Size:** M

---

#### Story 4.1.3: Auto-Generate Feature Documentation

**As a** documentation reader
**I want** feature documentation generated from tests
**So that** documentation stays in sync with tested behavior

**Acceptance Criteria:**

- Given feature files exist
- When documentation generation runs
- Then human-readable docs are created
- Given a feature file changes
- When docs regenerate
- Then changes are reflected

- [ ] Parse Gherkin feature files
- [ ] Generate markdown documentation per feature
- [ ] Include scenario descriptions and steps
- [ ] Cross-reference related features
- [ ] Generate documentation index

**Size:** M

---

#### Story 4.1.4: Capture and Index Container Logs

**As a** test analyst
**I want** container logs indexed and searchable
**So that** I can analyze patterns across test runs

**Acceptance Criteria:**

- Given tests complete
- When logs are processed
- Then they are stored with metadata
- Given a search query is run
- When matching logs exist
- Then relevant entries are returned

- [ ] Store container logs per test run
- [ ] Index logs with test metadata
- [ ] Enable text search across logs
- [ ] Filter by date, test, container
- [ ] Retain logs for configurable period

**Size:** L

---

#### Story 4.1.5: Create Version Release Notes Integration

**As a** project maintainer
**I want** test results linked to release notes
**So that** I know what changed between versions

**Acceptance Criteria:**

- Given a new version is tested
- When results are recorded
- Then release notes URL is included
- Given viewing results
- When clicking release notes link
- Then official release notes open

- [ ] Fetch release notes from GitHub API
- [ ] Link test runs to release versions
- [ ] Summarize changes relevant to testing
- [ ] Highlight breaking changes
- [ ] Display in dashboard and reports

**Size:** S

---

### Epic 4.2: Performance and Scalability Testing

**Goal:** Add load testing capabilities to the framework.

**Success Metrics:**

- Support for high concurrent participant counts in tests
- Performance regression detection with configurable threshold
- Resource utilization metrics captured for all tests

#### Story 4.2.1: Implement Participant Count Scalability Tests

**As a** test developer
**I want** to test rooms with many participants
**So that** I can verify scalability limits

**Acceptance Criteria:**

- Given a room is created
- When many CLI publishers join
- Then participant count reaches target
- Given maximum participants is exceeded
- When join fails
- Then the limit behavior is documented

- [ ] Create scenarios for 10, 25, 50, 100 participants
- [ ] Use CLI publisher for efficiency
- [ ] Measure join time as count increases
- [ ] Capture server resource usage
- [ ] Document observed limits per version

**Size:** L

---

#### Story 4.2.2: Add Resource Utilization Monitoring

**As a** performance analyst
**I want** resource metrics captured during tests
**So that** I can identify resource bottlenecks

**Acceptance Criteria:**

- Given a test is running
- When resource monitoring is active
- Then CPU/memory/network metrics are captured
- Given test completes
- When results are stored
- Then resource timeline is available

- [ ] Capture CPU usage per container
- [ ] Capture memory usage per container
- [ ] Monitor network I/O
- [ ] Store metrics with test results
- [ ] Generate resource utilization report

**Size:** M

---

#### Story 4.2.3: Implement Performance Regression Detection

**As a** project maintainer
**I want** performance regressions detected automatically
**So that** degradations are caught early

**Acceptance Criteria:**

- Given baseline performance metrics exist
- When new test results are compared
- Then regressions exceeding threshold are flagged
- Given a regression is detected
- When reported
- Then specific metric and deviation are shown

- [ ] Define performance baseline per scenario
- [ ] Compare current results to baseline
- [ ] Configure regression threshold
- [ ] Flag regressions in test report
- [ ] Track performance trends over time

**Size:** M

**Dependencies:** Story 4.2.2

---

#### Story 4.2.4: Test Recording Under Load

**As a** test developer
**I want** to test egress recording under load
**So that** I can verify recording stability with many participants

**Acceptance Criteria:**

- Given many participants are publishing
- When recording is active
- Then recording captures all participants
- Given recording completes
- When output is verified
- Then all participants are visible in recording

- [ ] Create multi-participant recording scenarios
- [ ] Test room composite with 5, 10, 20 participants
- [ ] Verify recording file quality
- [ ] Measure recording latency under load
- [ ] Document recording limits per version

**Size:** L

**Dependencies:** Story 4.2.1

---

---

## Phase 5: Innovation

*Focus: Create tools that extend the project's value beyond testing*

### Epic 5.1: LiveKit MCP Server Development

**Goal:** Create an MCP server that helps developers integrate LiveKit using LLMs.

**Success Metrics:**

- MCP server functional with Claude and other MCP clients
- Covers common LiveKit integration questions
- Positive developer feedback on accuracy

#### Story 5.1.1: Define MCP Server Architecture

**As a** architect
**I want** a clear MCP server architecture
**So that** implementation follows best practices

**Acceptance Criteria:**

- Given the architecture is defined
- When reviewed
- Then all components and their interactions are clear
- Given the architecture is approved
- When implementation starts
- Then developers have clear guidance

- [ ] Research MCP protocol specification
- [ ] Define server components (tools, resources, prompts)
- [ ] Plan knowledge base integration
- [ ] Define deployment model
- [ ] Document architecture decisions

**Size:** M

---

#### Story 5.1.2: Implement LiveKit Documentation Resource

**As a** LLM user
**I want** access to LiveKit documentation via MCP
**So that** I can get accurate integration guidance

**Acceptance Criteria:**

- Given the MCP server is running
- When documentation resource is requested
- Then relevant documentation is returned
- Given a specific topic is queried
- When documentation is fetched
- Then topic-specific content is provided

- [ ] Create MCP resource for LiveKit docs
- [ ] Index official documentation
- [ ] Include version-specific documentation
- [ ] Support search within documentation
- [ ] Update documentation periodically

**Size:** L

**Dependencies:** Story 5.1.1

---

#### Story 5.1.3: Implement Code Example Resource

**As a** developer using LLM
**I want** access to working code examples
**So that** I can see how to implement specific features

**Acceptance Criteria:**

- Given a feature is requested
- When code examples resource is queried
- Then relevant examples are returned
- Given examples exist from test framework
- When included
- Then they are adapted for general use

- [ ] Extract code examples from test framework
- [ ] Create example index by feature
- [ ] Include examples for all major languages
- [ ] Version examples for API compatibility
- [ ] Validate examples compile/run

**Size:** M

**Dependencies:** Story 5.1.1

---

#### Story 5.1.4: Implement Compatibility Query Tool

**As a** developer using LLM
**I want** to query version compatibility
**So that** I can choose the right versions for my project

**Acceptance Criteria:**

- Given version compatibility data exists
- When queried via MCP tool
- Then compatibility status is returned
- Given an incompatible combination is requested
- When queried
- Then alternative compatible versions are suggested

- [ ] Create MCP tool for compatibility queries
- [ ] Integrate with compatibility matrix
- [ ] Return pass/fail status for combinations
- [ ] Suggest alternatives for failing combinations
- [ ] Include test evidence links

**Size:** M

**Dependencies:** Story 5.1.1, Story 4.1.1

---

#### Story 5.1.5: Implement Troubleshooting Guide Tool

**As a** developer using LLM
**I want** troubleshooting guidance for common issues
**So that** I can resolve problems quickly

**Acceptance Criteria:**

- Given an error description is provided
- When troubleshooting tool is invoked
- Then relevant solutions are suggested
- Given a known issue matches
- When results are returned
- Then the known issue is referenced

- [ ] Create troubleshooting knowledge base
- [ ] Index common errors and solutions
- [ ] Integrate with known issues database
- [ ] Provide step-by-step resolution guides
- [ ] Link to relevant documentation

**Size:** M

**Dependencies:** Story 5.1.1, Story 4.1.2

---

#### Story 5.1.6: Implement Configuration Generator Tool

**As a** developer using LLM
**I want** help generating LiveKit configuration
**So that** I can set up LiveKit correctly

**Acceptance Criteria:**

- Given requirements are described
- When configuration tool is invoked
- Then appropriate configuration is generated
- Given generated configuration is used
- When validated
- Then it works for the described use case

- [ ] Create configuration template library
- [ ] Generate config based on requirements
- [ ] Include Docker Compose configurations
- [ ] Generate token configuration
- [ ] Validate generated configurations

**Size:** L

**Dependencies:** Story 5.1.1

---

#### Story 5.1.7: Implement Test Scenario Generator Prompt

**As a** developer using LLM
**I want** help writing test scenarios
**So that** I can test my LiveKit integration

**Acceptance Criteria:**

- Given a feature to test is described
- When scenario generator is invoked
- Then Gherkin scenarios are generated
- Given generated scenarios are used
- When run with the framework
- Then they execute correctly

- [ ] Create MCP prompt for test generation
- [ ] Include framework step definitions
- [ ] Generate scenarios following best practices
- [ ] Provide multiple scenarios per feature
- [ ] Include positive and negative cases

**Size:** M

**Dependencies:** Story 5.1.1

---

#### Story 5.1.8: Package and Distribute MCP Server

**As a** developer
**I want** to easily install the MCP server
**So that** I can use it with my preferred LLM client

**Acceptance Criteria:**

- Given the MCP server is complete
- When distributed
- Then it can be installed via standard package managers
- Given installation completes
- When configured
- Then it connects to MCP clients

- [ ] Package MCP server for distribution
- [ ] Create installation documentation
- [ ] Support npm/pip distribution (as appropriate)
- [ ] Provide Docker image option
- [ ] Create quickstart guide

**Size:** M

**Dependencies:** Stories 5.1.2-5.1.7

---

### Epic 5.2: Advanced Test Capabilities

**Goal:** Add cutting-edge testing capabilities.

**Success Metrics:**

- AI-assisted test generation produces valid scenarios
- Network simulation enables realistic testing
- Cross-region testing validates geo-distributed deployments

#### Story 5.2.1: Implement Network Condition Simulation

**As a** test developer
**I want** to simulate various network conditions
**So that** I can test behavior under poor network

**Acceptance Criteria:**

- Given a network condition is configured
- When test runs
- Then the condition is applied
- Given high latency is simulated
- When WebRTC connects
- Then connection behavior under latency is observed

- [ ] Integrate network simulation tool (tc/pumba)
- [ ] Support latency simulation
- [ ] Support packet loss simulation
- [ ] Support bandwidth limiting
- [ ] Create predefined network profiles (3G, 4G, wifi)

**Size:** L

---

#### Story 5.2.2: Implement Audio/Video Quality Metrics

**As a** test developer
**I want** to measure media quality objectively
**So that** I can verify quality meets requirements

**Acceptance Criteria:**

- Given video is published
- When quality metrics are captured
- Then resolution, framerate, bitrate are measured
- Given audio is published
- When quality metrics are captured
- Then codec, bitrate, loss are measured

- [ ] Capture video resolution and framerate
- [ ] Measure video bitrate
- [ ] Capture audio codec and bitrate
- [ ] Calculate packet loss percentage
- [ ] Generate quality score summary

**Size:** M

---

#### Story 5.2.3: Add Multi-Region Testing Support

**As a** test developer
**I want** to test across regions
**So that** I can verify geo-distributed deployments

**Acceptance Criteria:**

- Given multiple LiveKit instances are running
- When participants connect to different regions
- Then cross-region communication works
- Given regional failover occurs
- When primary region fails
- Then failover behavior is validated

- [ ] Support multiple LiveKit container instances
- [ ] Configure separate regions per instance
- [ ] Test cross-region participant communication
- [ ] Measure cross-region latency
- [ ] Test regional failover scenarios

**Size:** L

---

#### Story 5.2.4: Implement AI-Assisted Test Generation

**As a** test developer
**I want** AI to suggest test scenarios
**So that** I can improve test coverage efficiently

**Acceptance Criteria:**

- Given existing test scenarios
- When AI analysis runs
- Then coverage gaps are identified
- Given gaps are identified
- When suggestions are generated
- Then new scenarios address the gaps

- [ ] Analyze existing feature file coverage
- [ ] Identify untested feature combinations
- [ ] Generate scenario suggestions
- [ ] Validate generated scenarios are syntactically correct
- [ ] Prioritize suggestions by risk/impact

**Size:** L

---

---

## Dependencies Summary

```
Phase 1 (Feature Coverage)
    |
    +-- Epic 1.1 (Expanded LiveKit Features)
        Stories are largely independent
        (Ingress, SIP, Agents are larger efforts)

Phase 2 (Foundation)
    |
    +-- Epic 2.1 (Reliability)
    |   Stories are largely independent
    |
    +-- Epic 2.2 (Error Handling)
    |   Stories are largely independent
    |
    +-- Epic 2.3 (Version Infrastructure) [EXPANDED]
        2.3.1 (Config) - partial, foundation for all
        2.3.2 (Docker Images) - independent
        2.3.3 (JS Library Fetching) - independent
        2.3.4 (Protocol Detection) - foundation for compatibility
        2.3.5 (Rules Engine) - foundation for validation
        2.3.4, 2.3.5 --> 2.3.6 (Pre-Test Validation)
        2.3.7 (Abstraction Layer) - independent, high value
        2.3.1 --> 2.3.8 (BDD Version Annotations)
        2.3.9 (Doc Generator) - independent
        2.3.10 --> 2.3.11 (Downgrade Testing)

Phase 3 (Growth)
    |
    +-- Epic 3.1 (Release Automation)
    |   3.1.1 --> 3.1.2, 3.1.3 --> 3.1.4
    |   3.1.1 --> 3.1.5
    |   3.1.4 depends on 2.3.1
    |
    +-- Epic 3.2 (Public Reports)
        3.2.1 --> 3.2.2 --> 3.2.4
        3.1.1 --> 3.2.3
        3.1.4, 3.2.4 --> 3.2.5

Phase 4 (Scale)
    |
    +-- Epic 4.1 (Knowledge)
    |   3.1.4 --> 4.1.1
    |   Other stories largely independent
    |
    +-- Epic 4.2 (Performance)
        4.2.1 --> 4.2.4
        4.2.2 --> 4.2.3

Phase 5 (Innovation)
    |
    +-- Epic 5.1 (MCP Server)
    |   5.1.1 --> 5.1.2 through 5.1.7 --> 5.1.8
    |   5.1.4 depends on 4.1.1
    |   5.1.5 depends on 4.1.2
    |
    +-- Epic 5.2 (Advanced Testing)
        Stories are largely independent
```

---

## Priority Recommendations

### Immediate (Next 1-2 Sprints)

1. ~~Story 1.1.1 - Test Screen Sharing Functionality~~ [DONE]
2. ~~Story 1.1.2 - Test Simulcast Video Publishing~~ [DONE]
3. ~~Story 1.1.7 - Test Track Mute and Unmute Operations~~ [DONE]
4. ~~Story 1.1.4 - Test Data Channel Communication~~ [DONE]
5. ~~Story 1.1.3 - Test Dynacast Bandwidth Adaptation~~ [DONE]
6. ~~Story 1.1.5 - Test Room Metadata Operations~~ [DONE]
7. ~~Story 1.1.6 - Test Participant Metadata Operations~~ [DONE]
8. Story 1.1.8 - Test Ingress Stream Input
9. Story 1.1.9 - Verify Ingress Stream Playback
10. Story 1.1.10 - Record Ingress Stream via Egress
11. Story 1.1.11 - Test SIP Integration

### Short-term (Next Quarter)

1. Complete Epic 1.1 (Feature Coverage) - all untested LiveKit features
2. Story 2.1.1 - Container Health Checks
3. Story 2.1.2 - Retry Mechanism
4. Story 2.2.2 - Container Logs on Failure
5. **Story 2.3.1 - Complete Version Configuration** (finish remaining components)
6. **Story 2.3.3 - Dynamic JS Client Library Fetching** (enables client version testing)
7. **Story 2.3.6 - Pre-Test Protocol Compatibility Validation** (fail-fast on bad combinations)

### Medium-term

1. Complete Epic 2.1 (Reliability)
2. Complete Epic 2.2 (Error Handling)
3. **Epic 2.3 (Version Infrastructure) - High Priority Items:**
   - Story 2.3.4 - Protocol Version Detection
   - Story 2.3.5 - Protocol Compatibility Rules Engine
   - Story 2.3.7 - Protocol Abstraction Layer
   - Story 2.3.8 - BDD Scenario Version Annotations
4. Stories 3.1.1-3.1.5 (CI/CD)

### Long-term

1. Epic 3.2 (Public Reports and Dashboards)
2. Epic 4.1 (Knowledge Collection)
3. Epic 4.2 (Performance Testing)
4. Epic 5.1 (MCP Server)
5. Epic 5.2 (Advanced Capabilities)

---

## Open Questions

1. **Version Support Policy**: How many LiveKit versions should be actively tested?
2. **Browser Coverage**: Should Safari testing be prioritized?
3. **Cloud Testing**: Should tests run against managed LiveKit Cloud?
4. **MCP Distribution**: What is the preferred distribution channel for MCP server?
5. **Performance Baselines**: What performance metrics should be baselined first?
6. **Protocol Abstraction Scope**: Which operations need protocol-specific handling? (grants, events, track options)
7. **Version Matrix Depth**: Should we test N×M combinations (all servers × all clients) or focus on diagonal (matching releases)?
8. **Java SDK Runtime Switching**: Is compile-time SDK version acceptable, or do we need runtime switching?
9. **Compatibility Rule Sources**: Should rules be community-contributed or maintained internally?

---

## Success Criteria for Roadmap

The roadmap will be considered successful when:

1. Test reliability is high and consistent on stable infrastructure
2. Tests run automatically on each LiveKit release
3. Public dashboard shows test results for all supported versions
4. Compatibility matrix covers recent releases
5. MCP server helps developers integrate LiveKit effectively
6. Framework is adopted by LiveKit community for integration testing
7. **All component versions configurable without code changes**
8. **Incompatible version combinations fail fast before test execution**
9. **New protocol versions require changes only to abstraction layer**
10. **BDD scenarios automatically skip when version requirements not met**
