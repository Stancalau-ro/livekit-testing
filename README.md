# LiveKit Testing Framework

A comprehensive Java-based testing framework for LiveKit applications providing Docker-based integration testing, BDD testing capabilities, and WebRTC automation using Selenium and TestContainers.

## Features

- **🐳 Docker Integration**: Automated LiveKit, Redis, MinIO, and Egress container orchestration
- **🧪 BDD Testing**: Cucumber-based behavior-driven development with Gherkin feature files  
- **🌐 WebRTC Testing**: Real browser automation with video/audio streaming capabilities
- **📹 Recording Support**: Automatic test session recording with VNC for debugging
- **🔐 Token Management**: Comprehensive LiveKit access token generation and validation
- **☁️ S3 Integration**: MinIO S3 testing for egress recording storage
- **📊 State Management**: Centralized state management for complex test scenarios
- **🚀 CI/CD Ready**: Designed for continuous integration environments

## Quick Start

### Prerequisites

- Java 21 or higher
- Docker client

### Running Tests

```bash
# Run all tests (unit + BDD)
./gradlew test

# Run only unit tests
./gradlew test --tests "ro.stancalau.test.framework.*"

# Run only BDD/Cucumber tests
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests"

# Run specific BDD scenario
./gradlew test --tests "ro.stancalau.test.bdd.RunCucumberTests" -Dcucumber.filter.name="Successfully publish video to a room with Chrome browser"

# Override LiveKit version
./gradlew test -Plivekit_docker_version=v1.8.5

# Control VNC recording
./gradlew test -Drecording.mode=skip     # No recordings
./gradlew test -Drecording.mode=failed   # Record only failed tests
./gradlew test -Drecording.mode=all      # Record all tests (default)
```

## Architecture

### Infrastructure Layer (`src/main/java/ro/stancalau/test/framework/`)

**Docker Container Management:**
- `LiveKitContainer`: TestContainers integration for LiveKit server instances
- `LiveKitContainerFactory`: Factory for different container configurations
- `EgressContainer`: Egress service container for recording functionality
- `MinIOContainer`: S3-compatible storage for egress recordings
- `RedisContainer`: Redis backend for LiveKit state management

**WebRTC Browser Testing:**
- `SeleniumConfig`: Cross-platform WebDriver configuration with media permissions
- `LiveKitMeet`: Custom web interface for real WebRTC testing
- `WebrtcPublish/WebrtcPlayback`: Page objects for video streaming automation

**Utilities:**
- `StringParsingUtils`: Advanced parsing for BDD feature file parameters
- `S3ClientUtils`: MinIO S3 client management and file operations
- `DateUtils`: Timestamp utilities for test data organization
- `FileUtils`: File system operations and test artifact management

### State Management Layer (`src/main/java/ro/stancalau/test/framework/state/`)

**ContainerStateManager:** Centralized Docker infrastructure management:
- Shared Docker network for container communication
- Container registry with type-safe retrieval
- Automatic cleanup and resource management

**RoomClientStateManager:** LiveKit client state tracking:
- Access token management by identity and room
- VideoGrant parsing for all 17 permission types
- Custom attribute support with escape sequence handling

**WebDriverStateManager:** Browser session management:
- WebDriver instance lifecycle management
- VNC recording integration
- Cross-browser compatibility

### Testing Layer

**Unit Tests** (`src/test/java/ro/stancalau/test/framework/`):
- Component validation and infrastructure testing
- Utility function verification
- Integration testing for Docker containers

**BDD Tests** (`src/test/java/ro/stancalau/test/bdd/`):
- Cucumber integration with comprehensive step definitions
- Feature files covering all LiveKit functionality
- Real WebRTC testing scenarios

## Test Scenarios

The framework includes comprehensive test coverage for:

### WebRTC Functionality
- Video publishing and playback
- Multiple participant scenarios
- Permission-based access control
- Connection lifecycle management

### Recording & Egress
- Local file recording
- S3/MinIO cloud storage
- Track composite recording
- Multi-participant recording scenarios

### LiveKit Features
- Room management and lifecycle
- Participant removal and reconnection
- Webhook event validation
- Access token security

### Infrastructure
- Container orchestration
- Network connectivity
- Service integration
- Error handling and recovery

## Technology Stack

- **Java 21+**: Core language with modern features
- **JUnit 5**: Unit testing framework
- **Cucumber**: BDD testing with Gherkin syntax~~~~
- **TestContainers**: Docker integration testing
- **Selenium WebDriver**: Browser automation
- **LiveKit SDK**: Official Java server SDK
- **Docker**: Container orchestration
- **MinIO**: S3-compatible object storage
- **Lombok**: Boilerplate code reduction

## Dependencies

| Component | Version | Purpose |
|-----------|---------|---------|
| LiveKit Server SDK | 0.8.5 | Server-side token generation |
| Selenium | 4.27.0 | WebRTC browser automation |
| TestContainers | 1.20.4 | Docker integration |
| Cucumber | 7.18.0 | BDD testing framework |
| JUnit | 5.10.0 | Unit testing |
| AWS SDK | 2.20.68 | S3 integration |

## Configuration

### Version Management
Configure versions in `gradle.properties`:
```properties
livekit_docker_version=v1.8.4
egress_docker_version=v1.8.4
```

Override at runtime:
```bash
./gradlew test -Plivekit_docker_version=v1.8.5
```

### Recording Options
Control test session recording:
```bash
-Drecording.mode=skip     # Disable recording
-Drecording.mode=failed   # Record only failures  
-Drecording.mode=all      # Record all tests
```

## Development

### Adding New Tests

1. **Unit Tests**: Add to `src/test/java/ro/stancalau/test/framework/`
2. **BDD Features**: Create `.feature` files in `src/test/resources/features/`
3. **Step Definitions**: Implement in `src/test/java/ro/stancalau/test/bdd/steps/`

### Code Guidelines

- Use Lombok for boilerplate reduction
- Follow existing state management patterns
- Ensure proper resource cleanup in tests
- Add comprehensive logging for debugging

### Architecture Principles

- **Injectable Dependencies**: All state managers are injectable, not singletons
- **Resource Management**: Automatic cleanup of containers and WebDriver instances
- **Type Safety**: Generic methods for container and state retrieval
- **Test Isolation**: Independent state for each test scenario

## CI/CD Integration

The framework is designed for continuous integration:

- **Headless Execution**: Chrome runs in headless mode in CI environments
- **VNC Recording**: Automatic failure recording for debugging
- **Container Cleanup**: Automatic resource cleanup prevents CI resource leaks
- **Parallel Execution**: Tests can run in parallel with proper isolation
- **Artifact Collection**: Test recordings and logs are preserved for analysis

## Troubleshooting

### Common Issues

**Docker Connectivity:**
```bash
# Verify Docker is running
docker ps

# Check container logs
docker logs <container_id>
```

**WebRTC Testing:**
```bash
# Ensure Chrome allows media access
# Check VNC recordings in build output
# Verify container network connectivity
```

**Test Failures:**
```bash
# Run with verbose logging
./gradlew test --info

# Check specific scenario
./gradlew test -Dcucumber.filter.name="scenario name"
```

## Contributing

1. Follow existing code patterns and architecture
2. Add unit tests for new framework components  
3. Include BDD scenarios for new LiveKit features
4. Ensure proper resource cleanup in all tests
5. Update documentation for new functionality

## License

Apache-2.0 License
