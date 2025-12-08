# LiveKit Testing Framework Architecture

This document provides a comprehensive overview of the LiveKit Testing Framework architecture, detailing the component relationships, design patterns, and integration points that enable Docker-based integration testing for LiveKit applications.

## Table of Contents

- [High-Level Architecture](#high-level-architecture)
- [Layer Overview](#layer-overview)
- [Infrastructure Layer](#infrastructure-layer)
- [State Management Layer](#state-management-layer)
- [Testing Layer](#testing-layer)
- [Component Relationships](#component-relationships)
- [Docker Container Orchestration](#docker-container-orchestration)
- [Token Management and Authentication](#token-management-and-authentication)
- [Configuration System](#configuration-system)

---

## High-Level Architecture

The framework follows a three-layer architecture designed for modularity, testability, and extensibility:

```
+------------------------------------------------------------------+
|                        TESTING LAYER                              |
|  +------------------------------------------------------------+  |
|  |  BDD/Cucumber Tests          |    Unit Tests                |  |
|  |  - Feature Files             |    - JUnit 5                 |  |
|  |  - Step Definitions          |    - Component Tests         |  |
|  |  - RunCucumberTests          |    - Utility Tests           |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                    STATE MANAGEMENT LAYER                         |
|  +------------------------------------------------------------+  |
|  |  ManagerFactory  -->  ManagerSet (per scenario)             |  |
|  |    |                                                        |  |
|  |    +-> ContainerStateManager    (Docker containers)         |  |
|  |    +-> WebDriverStateManager    (Browser instances)         |  |
|  |    +-> AccessTokenStateManager  (LiveKit tokens)            |  |
|  |    +-> RoomClientStateManager   (Room API clients)          |  |
|  |    +-> EgressStateManager       (Recording state)           |  |
|  |    +-> ImageSnapshotStateManager (Snapshot state)           |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                     INFRASTRUCTURE LAYER                          |
|  +------------------------------------------------------------+  |
|  |  Docker Containers           |    Selenium/WebDriver        |  |
|  |  - LiveKitContainer          |    - SeleniumConfig          |  |
|  |  - EgressContainer           |    - BrowserWebDriverContainer|  |
|  |  - RedisContainer            |    - VNC Recording           |  |
|  |  - MinIOContainer            |                              |  |
|  |  - CLIPublisherContainer     |    Utilities                 |  |
|  |  - WebServerContainer        |    - PathUtils               |  |
|  |  - MockHttpServerContainer   |    - StringParsingUtils      |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
```

---

## Layer Overview

### 1. Infrastructure Layer
**Location**: `src/main/java/ro/stancalau/test/framework/`

Provides the foundational components for Docker container management, browser automation, and utility functions. This layer encapsulates all low-level implementation details.

### 2. State Management Layer
**Location**: `src/main/java/ro/stancalau/test/framework/state/`

Manages test state across scenarios, ensuring proper isolation and cleanup. Each BDD scenario receives its own isolated set of managers through the ManagerFactory.

### 3. Testing Layer
**Location**: `src/test/java/ro/stancalau/test/bdd/` and `src/test/java/ro/stancalau/test/framework/`

Contains both BDD/Cucumber tests with Gherkin feature files and traditional JUnit 5 unit tests.

---

## Infrastructure Layer

### Docker Container Management

All container classes extend TestContainers' `GenericContainer` and share common patterns:

#### LiveKitContainer
Primary container for the LiveKit media server.

**Key Features**:
- Configurable version via `livekit/livekit-server:{version}` image
- HTTP port exposure (7880) for API access
- WebSocket URL generation for client connections
- Network alias support for inter-container communication
- Log capture to filesystem
- Optional configuration file binding

**Configuration Options**:
```
Container Alias: User-defined or "livekitServer"
Ports: 7880 (HTTP/WebSocket)
API Key: "devkey" (default)
Secret: "secret" (default)
```

#### EgressContainer
Container for LiveKit's egress service (recording/streaming).

**Key Features**:
- GRPC port exposure (7980)
- S3 configuration support for MinIO integration
- Redis dependency for job coordination
- Dynamic configuration via environment variables
- Chrome flags for headless rendering
- Video recordings and snapshots output directories

**Required Services**:
- LiveKit server (for WebSocket connection)
- Redis (for job queue)
- MinIO (optional, for S3 storage)

#### RedisContainer
Container for Redis cache/message broker.

**Key Features**:
- Redis 7 Alpine image
- Port 6379 exposure
- Network URL generation for other containers
- Log message wait strategy

#### MinIOContainer
S3-compatible object storage container.

**Key Features**:
- Dual port exposure (9000 API, 9001 Console)
- Configurable access/secret keys
- Health check on `/minio/health/live`
- Bucket management support

#### CLIPublisherContainer
Container for LiveKit CLI tool supporting load testing.

**Key Features**:
- `livekit/livekit-cli:latest` image
- Load test mode with configurable publishers
- Join mode for individual participant simulation
- Configurable video resolution and simulcast
- Speaker simulation support

**Configuration Builder**:
```java
PublisherConfig.builder()
    .type(PublisherType.LOAD_TEST)
    .videoPublishers(5)
    .audioPublishers(3)
    .subscribers(10)
    .videoResolution("high")
    .simulcast(true)
    .duration(60)
    .build();
```

#### WebServerContainer
Container for serving static web content.

**Key Features**:
- Nginx-based static file server
- LiveKit Meet page serving
- Custom HTML page support

#### MockHttpServerContainer
MockServer-based container for webhook testing.

**Key Features**:
- HTTP request recording
- Webhook event capture
- Request matching and verification

### Browser Automation

#### SeleniumConfig
Cross-platform WebDriver configuration with WebRTC-specific settings.

**Supported Browsers**:
- Chrome (with fake media streams)
- Firefox (with WebRTC preferences)
- Edge (with Chrome-like options)

**Key WebRTC Settings**:
- Fake device streams for testing
- Disabled mDNS for local IP visibility
- Insecure origin handling
- Autoplay policies

### Utilities

#### PathUtils
Cross-platform path handling utility.

**Key Methods**:
- `join(String first, String... more)` - Platform-safe path joining
- `scenarioPath(...)` - BDD scenario output paths
- `containerLogPath(...)` - Container log destinations
- `livekitConfigPath(...)` - Version-aware config resolution

#### StringParsingUtils
Parsing utility for BDD feature file values.

**Key Features**:
- Comma-separated value parsing with escape support
- Key-value pair extraction
- Grant string parsing

---

## State Management Layer

### ManagerFactory
Factory for creating isolated manager sets per test scenario.

**Design Pattern**: Factory Pattern with composition

**Purpose**:
- Creates complete, wired manager sets
- Ensures proper dependency injection
- Enables parallel test execution

```java
public static ManagerSet createManagerSet() {
    ContainerStateManager containerManager = new ContainerStateManager();
    WebDriverStateManager webDriverManager = new WebDriverStateManager(containerManager);
    RoomClientStateManager roomClientManager = new RoomClientStateManager(containerManager);
    AccessTokenStateManager accessTokenManager = new AccessTokenStateManager();
    EgressStateManager egressStateManager = new EgressStateManager();
    ImageSnapshotStateManager imageSnapshotStateManager = new ImageSnapshotStateManager();

    return new ManagerSet(...);
}
```

### ManagerSet (Record)
Immutable container holding all scenario managers.

**Features**:
- Type-safe access to all managers
- Coordinated cleanup in reverse dependency order
- Exception handling during cleanup

### ContainerStateManager
Central registry for Docker containers and networks.

**Responsibilities**:
- Docker network lifecycle management
- Container registration and retrieval
- Type-safe container access via generics
- Coordinated container cleanup
- Web server and MinIO factory methods

**Key Data Structures**:
```
Network network
Map<String, GenericContainer<?>> containers
```

### WebDriverStateManager
Manages browser instances and VNC recordings.

**Responsibilities**:
- Browser container creation and lifecycle
- VNC recording with test result awareness
- Purpose/actor-based WebDriver identification
- Recording path management per scenario

**Key Data Structures**:
```
Map<String, WebDriver> webDrivers
Map<String, BrowserWebDriverContainer<?>> browserContainers
Map<String, TestDescription> testDescriptions
Map<String, Boolean> testResults
```

### AccessTokenStateManager
Manages LiveKit access token generation and storage.

**Responsibilities**:
- Token creation with various grant types
- Support for all 17 VideoGrant types
- Custom attribute handling with escape sequences
- Token TTL/expiration configuration
- Token retrieval by identity and room

**Supported Grants**:
- RoomJoin, RoomName
- CanPublish, CanSubscribe, CanPublishData
- CanUpdateOwnMetadata
- RoomCreate, RoomList, RoomRecord, RoomAdmin
- Hidden, Recorder
- IngressAdmin, Agent

**Key Data Structure**:
```
Map<String, Map<String, AccessToken>> tokens
  (identity -> (roomName -> token))
```

### RoomClientStateManager
Manages LiveKit Room Service API clients.

**Responsibilities**:
- RoomServiceClient creation per LiveKit instance
- Client caching by service name
- Server initialization verification

### EgressStateManager
Tracks egress recording state.

**Responsibilities**:
- Participant track ID storage
- Active recording ID management
- Recording cleanup coordination

### ImageSnapshotStateManager
Manages image snapshot state.

**Responsibilities**:
- Snapshot file path tracking
- S3 and local snapshot management

---

## Testing Layer

### BDD Test Structure

#### Feature Files
Location: `src/test/resources/features/`

| Feature File | Coverage |
|--------------|----------|
| `livekit_access_token.feature` | Token generation with grants and attributes |
| `livekit_rooms.feature` | Room CRUD operations |
| `livekit_webrtc_publish.feature` | Browser-based video publishing |
| `livekit_webrtc_playback.feature` | Video playback and subscription |
| `livekit_webhooks.feature` | Webhook event validation |
| `livekit_egress_recording.feature` | Video recording to local/S3 |
| `livekit_image_snapshots.feature` | On-demand image capture |
| `livekit_cli_publisher.feature` | CLI-based load testing |
| `livekit_minio_recording.feature` | S3 storage integration |
| `livekit_participant_removal.feature` | Participant management |

#### Step Definitions
Location: `src/test/java/ro/stancalau/test/bdd/steps/`

| Step Definition Class | Responsibility |
|-----------------------|----------------|
| `BaseSteps` | Manager lifecycle (@Before/@After hooks) |
| `ManagerProvider` | Thread-local manager access |
| `LiveKitLifecycleSteps` | Container and room lifecycle |
| `LiveKitAccessTokenSteps` | Token creation and validation |
| `LiveKitBrowserWebrtcSteps` | Browser automation for WebRTC |
| `LiveKitCLIPublisherSteps` | CLI publisher management |
| `LiveKitEgressSteps` | Recording start/stop |
| `LiveKitImageSnapshotSteps` | Snapshot capture |
| `LiveKitRoomSteps` | Room operations |
| `LiveKitWebhookSteps` | Webhook event verification |
| `LiveKitMinIORecordingSteps` | S3 storage operations |

#### Test Runner
```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("ro.stancalau.test.bdd")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ro.stancalau.test.bdd.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class RunCucumberTests {
}
```

### Unit Test Structure
Location: `src/test/java/ro/stancalau/test/framework/`

**Test Classes**:
- `LiveKitContainerTest` - Container lifecycle and connectivity
- `StringParsingUtilsTest` - Parsing utility validation
- `DateUtilsTest` - Date utility tests
- `FileUtilsTest` - File utility tests
- `ImageValidationUtilsTest` - Image validation tests
- `ScenarioNamingUtilsTest` - Naming utility tests

---

## Component Relationships

### Dependency Graph

```
                     ManagerFactory
                          |
                          v
                      ManagerSet
                          |
          +---------------+---------------+
          |               |               |
          v               v               v
   ContainerStateManager  AccessTokenStateManager
          |                      |
   +------+------+               |
   |             |               |
   v             v               v
WebDriverSM   RoomClientSM   EgressSM / ImageSnapshotSM
   |             |
   |             +---> LiveKitContainer
   |
   +---> BrowserWebDriverContainer
```

### Manager Dependencies

| Manager | Dependencies |
|---------|--------------|
| ContainerStateManager | None (root manager) |
| WebDriverStateManager | ContainerStateManager |
| RoomClientStateManager | ContainerStateManager |
| AccessTokenStateManager | None (configuration only) |
| EgressStateManager | None (state storage only) |
| ImageSnapshotStateManager | None (state storage only) |

---

## Docker Container Orchestration

### Network Configuration
All containers share a common Docker network created by ContainerStateManager:

```
Docker Network (Bridge Mode)
     |
     +-- livekit1 (LiveKitContainer)
     |      Port: 7880
     |
     +-- egress1 (EgressContainer)
     |      Port: 7980
     |
     +-- redis (RedisContainer)
     |      Port: 6379
     |
     +-- minio1 (MinIOContainer)
     |      Ports: 9000, 9001
     |
     +-- webserver (WebServerContainer)
     |      Port: 80
     |
     +-- mockserver1 (MockHttpServerContainer)
     |      Port: 1080
     |
     +-- browser-* (BrowserWebDriverContainer)
            VNC accessible
```

### Container Start Order
Services must start in dependency order:

1. **Network** - Created first by ContainerStateManager
2. **Redis** - Required by Egress for job coordination
3. **MinIO** - Optional S3 storage
4. **MockServer** - Optional webhook receiver
5. **LiveKit** - Core media server
6. **Egress** - Recording service (requires LiveKit + Redis)
7. **WebServer** - Static content server
8. **CLI Publisher** - Load testing participants
9. **Browser Containers** - WebRTC clients

### Container Configuration Patterns

All containers follow consistent configuration:
- Network alias for DNS resolution
- Host gateway for host.docker.internal
- Log capture to scenario-specific directory
- Wait strategies for startup verification

---

## Token Management and Authentication

### Token Creation Flow

```
1. BDD Step receives identity, room, grants
                    |
                    v
2. AccessTokenStateManager.createTokenWithDynamicGrants()
                    |
                    v
3. Parse grant strings (e.g., "canPublish:true,roomAdmin:true")
                    |
                    v
4. Create LiveKit AccessToken with API key/secret
                    |
                    v
5. Add VideoGrant objects based on parsed grants
                    |
                    v
6. Store token in Map<identity, Map<roomName, token>>
                    |
                    v
7. Return JWT string via token.toJwt()
```

### Grant Type Mapping

| Grant String | VideoGrant Class | Description |
|--------------|------------------|-------------|
| `roomJoin` | RoomJoin | Join room permission |
| `canPublish` | CanPublish | Publish tracks permission |
| `canSubscribe` | CanSubscribe | Subscribe to tracks permission |
| `canPublishData` | CanPublishData | Publish data messages |
| `canUpdateOwnMetadata` | CanUpdateOwnMetadata | Update participant metadata |
| `roomCreate` | RoomCreate | Create rooms permission |
| `roomList` | RoomList | List rooms permission |
| `roomRecord` | RoomRecord | Record rooms permission |
| `roomAdmin` | RoomAdmin | Room administration |
| `hidden` | Hidden | Hidden participant |
| `recorder` | Recorder | Recorder participant |
| `ingressAdmin` | IngressAdmin | Ingress administration |
| `agent` | Agent | AI agent participant |

---

## Configuration System

### TestConfig
Centralized configuration management with priority resolution:

**Priority Order**:
1. System Property (`-Dlivekit.version=v1.8.5`)
2. Environment Variable (`LIVEKIT_VERSION=v1.8.5`)
3. Gradle Property (`-Plivekit_docker_version=v1.8.5`)
4. Default Value

### Version-Aware Configuration

Configuration files are organized by version:
```
src/test/resources/livekit/config/
    v1.8.4/
        basic/
            config.yaml
        basic_hook/
            config.yaml
        with_egress/
            config.yaml
            egress.yaml
        with_egress_hook/
            config.yaml
            egress.yaml
```

**Fallback Mechanism**:
If a configuration for the requested version doesn't exist, TestConfig searches for the latest available version directory.

### Recording Mode Configuration

| Mode | Behavior |
|------|----------|
| `skip` | No VNC recordings |
| `all` | Record all tests (default) |
| `failed` | Record only failed tests |

---

## Design Principles

### 1. Test Isolation
Each BDD scenario receives its own isolated ManagerSet through ThreadLocal storage, enabling safe parallel execution.

### 2. Dependency Injection
All managers are injectable (never singletons), allowing for proper testing and mock substitution.

### 3. Cross-Platform Compatibility
PathUtils ensures the framework runs on both Windows and Linux by using Java's Paths API.

### 4. Version Flexibility
The configuration system supports testing against any LiveKit version with automatic fallback.

### 5. Resource Cleanup
ManagerSet.cleanup() ensures proper resource release in reverse dependency order with exception handling.

### 6. Logging and Observability
All containers capture logs to scenario-specific directories, and VNC recordings capture browser test sessions.
