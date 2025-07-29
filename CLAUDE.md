# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build and Test
```bash
# Run all tests (unit + BDD)
./gradlew test

# Run only unit tests (excludes Cucumber)
./gradlew test --tests "ro.stancalau.test.framework.*"

# Run specific unit test class
./gradlew test --tests "ro.stancalau.test.framework.util.StringParsingUtilsTest"

# Run only BDD/Cucumber tests
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests"

# Run specific BDD scenario by name (recommended for development and debugging)
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="<scenario name>"

# Examples of running specific scenarios:
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Successfully publish video to a room with Chrome browser"
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Generate access token with admin permissions for Dave"

# Override LiveKit version for tests
./gradlew test -Plivekit_docker_version=v1.8.5

# Control VNC recording for browser tests
./gradlew test -Drecording.mode=skip     # No recordings
./gradlew test -Drecording.mode=all      # Record all tests (default)
./gradlew test -Drecording.mode=failed   # Record only failed tests

# Clean build artifacts
./gradlew clean
```

### Project Setup
```bash
# Build project
./gradlew build

# Generate IntelliJ project files (if needed)
./gradlew idea
```

## Architecture Overview

This is a **LiveKit testing framework** that provides Docker-based integration testing and BDD testing capabilities for LiveKit applications. The architecture consists of three main layers:

### 1. Infrastructure Layer (`src/main/java/ro/stancalau/test/framework/`)

**Docker Container Management:**
- `LiveKitContainer`: Extends TestContainers to manage LiveKit server instances with configurable versions
- `LiveKitContainerFactory`: Factory for creating containers with different configurations  
- `DockerImageUtils`: Handles automated Docker image building and version management

**Browser Testing:**
- `SeleniumConfig`: Cross-platform WebDriver configuration for WebRTC testing with media permissions

**Utilities:**
- `StringParsingUtils`: Handles parsing of comma-separated values and key-value pairs with escape sequences (used in BDD feature files)

### 2. State Management Layer (`src/test/java/ro/stancalau/test/bdd/state/`)

**ContainerStateManager:** Singleton for centralized Docker container and network management:
- Manages shared Docker network accessible across all step definition classes
- Registers and tracks multiple service containers by name
- Provides type-safe container retrieval with generic methods
- Handles cleanup of all containers and network resources

**AccessTokenStateManager:** Central state manager for BDD tests that:
- Manages LiveKit access tokens by identity and room using `Map<Identity, Map<RoomName, AccessToken>>`
- Supports dynamic VideoGrant parsing for all 17 grant types (CanPublish, RoomAdmin, etc.)
- Handles custom token attributes with escape sequence support
- Provides immutable token creation where all permissions are set at creation time

### 3. Testing Layer

**Unit Tests** (`src/test/java/ro/stancalau/test/framework/`):
- Traditional JUnit 5 tests for component validation
- Infrastructure testing (container lifecycle, connectivity)
- Utility function validation

**BDD Tests** (`src/test/java/ro/stancalau/test/bdd/`):
- Cucumber integration with Gherkin feature files
- `LiveKitLifecycleSteps`: Container lifecycle and infrastructure management
- `LiveKitAccessTokenSteps`: Access token creation and validation steps
- `RunCucumberTests`: JUnit Platform Suite for Cucumber execution

## Key Integration Points

**Version Management:**
- LiveKit versions configured via `gradle.properties` (`livekit_docker_version`) 
- Can be overridden with system properties or command line arguments
- Test containers automatically use the specified version
- Configuration profiles are version-aware, falling back to latest available version if current version config is not found

**Test Isolation:**
- Unit tests and Cucumber tests are properly isolated
- Cucumber configuration in `junit-platform.properties` is commented out to prevent interference
- Each BDD scenario gets independent state via `@Before`/`@After` hooks

**Token Management:**
- Uses `io.livekit:livekit-server` SDK for server-side token generation
- Supports all VideoGrant types with string-based parsing from feature files
- Custom attributes support comma escaping: `"key=value1\, value2"`

## Project Structure Notes

- **Main sources**: Framework utilities and Docker infrastructure
- **Test sources**: Both unit tests and BDD test infrastructure  
- **Feature files**: Located in `src/test/resources/features/`
- **Configuration**: Lombok annotations used throughout for reducing boilerplate
- **Logging**: SLF4J with Logback, structured logging for test execution

## Dynamic Grant System

The framework supports dynamic VideoGrant specification in feature files using string syntax:
```gherkin
When an access token is created with identity "User" and room "Room" with grants "canPublish:true,roomAdmin:false"
```

All 17 VideoGrant types are supported: RoomJoin, CanPublish, CanSubscribe, CanPublishData, CanUpdateOwnMetadata, RoomCreate, RoomList, RoomRecord, RoomAdmin, Hidden, Recorder, IngressAdmin, Agent, etc.

Custom attributes with escaped commas are also supported:
```gherkin
And attributes "description=Testing\, debugging\, development,role=admin"
```

## Repository Guidelines

- **Commit Messages:**
  - Never add Co-Authored-By and Claude references to commit messages
  - Commit messages need to only specify most important changes and focus on the improvement not the implementation details

## Best Practices

- **Code Writing:**
  - Create an overview of the changes and ask for approval before coding
  - Avoid adding inline comments unless they describe the reason behind a code decision or something surprising
  - Do not add javadoc blocks

## Enterprise-Level Project Considerations

- The project is enterprise-level so the code should be:
  - Configurable
  - Thoroughly tested
  - Respect SOLID principles

## BDD Guidelines

- **Step Definition Language:**
  - All BDD steps must use 3rd person subject-focused language

- **LiveKit Configuration Per Feature:**
  - Each feature file can specify its own LiveKit configuration profile using the Background section
  - Use the step: `Given the LiveKit config is set to "<profile-name>"`
  - Profile names automatically resolve to version-specific paths using the current LiveKit version
  - If the current version doesn't have the profile, the system falls back to the latest available version
  - Available profiles:
    - `basic` - Basic LiveKit configuration (default)
    - `basic_hook` - LiveKit with webhook support
  - Example Background section:
    ```gherkin
    Background:
      Given the LiveKit config is set to "basic_hook"
      And a LiveKit server is running in a container with service name "livekit1"
    ```

## Testing Tips

### Single Scenario Execution
- **Always run single scenarios during development** to avoid timeouts and speed up feedback
- Use the Cucumber filter by scenario name: `-Dcucumber.filter.name="<exact scenario name>"`
- This is much faster than running all tests and allows for focused debugging
- Example: `./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Successfully publish video to a room with Chrome browser"`

### BDD Test Development Workflow
1. Write or modify a single scenario in a `.feature` file
2. Run only that scenario using the name filter to test your changes
3. Once the scenario passes, run the full test suite to ensure no regressions
4. **Never run all scenarios** during active development unless necessary

### Test Isolation
- Each BDD scenario is completely isolated with fresh containers and state
- Browser recordings are saved for each test run for debugging WebRTC issues
- VNC recordings can be controlled via `-Drecording.mode=[skip|all|failed]`

## Test Artifact Preservation System

The framework automatically preserves logs and recordings from each BDD scenario for audit and debugging purposes:

### Unified Test Artifacts
- **BDD Scenario Artifacts**: Each scenario gets a timestamped directory in `out/bdd/scenarios/`
- **Integration Test Logs**: Unit/integration tests get separate logs in `out/integration-tests/logs/`
- **LiveKit Server Logs**: Complete container logs from all LiveKit services
- **VNC Recordings**: Browser test recordings with WebRTC interactions
- **Directory Structures**: 
  - BDD: `out/bdd/scenarios/{FeatureName}/{ScenarioName}/{Timestamp}/`
    - `docker/{ServiceName}/` - LiveKit container logs
    - `recordings/` - VNC browser recordings
  - Integration: `out/integration-tests/logs/{Alias}/{Timestamp}/`

### Artifact Types Preserved
- **LiveKit Server Logs**: JSON-formatted logs from LiveKit containers
  - Server startup and configuration
  - API requests (CreateRoom, DeleteRoom, ListParticipants, etc.)
  - WebRTC connection events and participant lifecycle
  - Room state management and cleanup
- **VNC Browser Recordings**: MP4 recordings of browser interactions
  - WebRTC connection establishment and testing
  - Video publishing and subscription flows
  - Named by test result: `PASSED-webdriver-meet-{participant}-{timestamp}.mp4`
- **Test Execution Logs**: Framework and BDD step execution logs (when generated)

### Usage Examples
```bash
# Test artifacts are automatically preserved after each scenario
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Your scenario name"

# Check preserved artifacts
ls out/bdd/scenarios/

# Example directory structures:

# BDD Tests - Unified Structure:
out/bdd/scenarios/
├── Livekit_Webrtc_Publish/
│   ├── Multiple_participants_can_join_the_same_room_and_publish_video/
│   │   └── 2025-07-29_18-14-54/
│   │       ├── docker/
│   │       │   └── livekit1/
│   │       │       └── livekit.log
│   │       └── recordings/
│   │           ├── PASSED-webdriver-meet-Jack-20250729-181522.mp4
│   │           └── PASSED-webdriver-meet-Jill-20250729-181520.mp4
│   └── Participant_without_publish_permission_can_join_but_cannot_publish_video/
│       └── 2025-07-29_18-15-49/
│           ├── docker/
│           │   └── livekit1/
│           │       └── livekit.log
│           └── recordings/
│               └── PASSED-webdriver-meet-NotAPublisher-20250729-181609.mp4
└── Livekit_Webrtc_Playback/
    └── Publisher_and_subscriber_can_share_video_in_a_room/
        └── 2025-07-29_18-20-15/
            ├── docker/
            │   └── livekit1/
            │       └── livekit.log
            └── recordings/
                ├── PASSED-webdriver-meet-Publisher-20250729-182035.mp4
                └── PASSED-webdriver-meet-Subscriber-20250729-182037.mp4

# Integration Tests:
out/integration-tests/logs/
└── test-livekit/
    └── 2025-07-29_18-15-46/
        └── livekit.log
```

### Implementation Details
- **Unified Storage**: Both logs and recordings are stored under the same scenario-specific path structure
- **Direct Log Binding**: Container logs are bound directly to scenario paths during container creation
- **Dynamic Recording Paths**: VNC recordings are directed to scenario-specific folders automatically
- **LiveKitLifecycleSteps**: Creates containers and sets WebDriver recording paths for each scenario
- **WebDriverStateManager**: Manages scenario-specific recording directories for browser tests
- **LogPreservationSteps**: Cucumber hooks for test artifact logging
- **Enhanced Logging**: Logback configuration with structured logging for framework components
- **Efficient Storage**: Only services and recordings from each scenario are preserved in that scenario's folder