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

# Override Egress version for tests
./gradlew test -Pegress_docker_version=v1.8.5

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

## Best Practices and Guidelines

- **Code Writing:**
  - Use Lombok instead of boilerplate
  - Create an overview of the changes and ask for approval before coding
  - Never add comments to the code
  - Do not add javadoc blocks

## Architecture Principles

- **State Management:**
  - All state management classes need to be injectable, never singletons
- The project must run on both windows and linux so local paths must always be handled appropriately