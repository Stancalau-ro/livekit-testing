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

# Override LiveKit version for tests
./gradlew test -Plivekit_docker_version=v1.8.5

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
- `LiveKitAccessTokenSteps`: Step definitions with lifecycle management
- `RunCucumberTests`: JUnit Platform Suite for Cucumber execution

## Key Integration Points

**Version Management:**
- LiveKit versions configured via `gradle.properties` (`livekit_docker_version`) 
- Can be overridden with system properties or command line arguments
- Test containers automatically use the specified version

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

## Best Practices

- **Code Writing:**
  - Never write code before explaining what your approach will be

## Enterprise-Level Project Considerations

- The project is enterprise-level so the code should be:
  - Configurable
  - Thoroughly tested
  - Respect SOLID principles