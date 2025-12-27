# Feature: Ingress Stream Input Testing

## Status
**Planned** | Story 1.1.8 | Size: L (Large)

**Implementation Status:** Not started - requires infrastructure setup and API integration

## Problem Statement

The LiveKit testing framework currently lacks test coverage for ingress functionality - the ability to bring external media streams (RTMP, WHIP, URL) into LiveKit rooms. Ingress is a critical feature for:

- Broadcasting from OBS Studio, streaming hardware, or FFmpeg
- Restreaming from HTTP/HLS sources
- Integrating WHIP-compatible WebRTC encoders
- SRT server media ingestion

Without proper testing:
- There is no verification that external streams are correctly transcoded and published
- Ingress lifecycle (create, connect, disconnect, delete) is untested
- Stream quality and simulcast layer generation cannot be validated
- Ingress participant visibility to room subscribers is unverified
- Cleanup behavior when streams disconnect is not tested

## User Value

Test developers need to verify ingress functionality to ensure their LiveKit integrations properly support external media ingestion. This testing capability enables:

- Confidence that RTMP streams from OBS/FFmpeg are correctly transcoded
- Verification that WHIP streams connect with low latency
- Validation of ingress participant appearing in room with correct identity
- Testing of simulcast layer generation from ingress
- Detection of regressions in ingress behavior across LiveKit versions
- Quality assurance for broadcast and streaming applications

## Success Metrics

- Ingress container starts and registers with LiveKit server
- Ingress can be created via API with RTMP/WHIP/URL input types
- Simulated RTMP stream successfully connects to ingress endpoint
- Ingress participant appears in room with expected identity and name
- Other participants can subscribe to ingress tracks
- Ingress tracks have expected audio/video codecs
- Ingress deletion properly cleans up participant
- Stream reconnection works after disconnect

## Scope

### In Scope

- Ingress container infrastructure (Docker, TestContainers)
- Redis integration for ingress-server communication
- RTMP ingress testing with FFmpeg stream simulation
- WHIP ingress testing with GStreamer/WebRTC simulation
- URL/HLS ingress testing for file-based sources
- Ingress API operations (create, list, update, delete)
- Ingress participant verification in room
- Track subscription and media verification
- Ingress lifecycle events and cleanup
- Basic transcoding configuration (presets)

### Out of Scope

- Native OBS integration testing (simulated via FFmpeg)
- SRT ingress (requires additional infrastructure)
- Advanced transcoding customization (custom layers)
- Ingress recording to egress (separate epic)
- Multi-ingress scenarios (single ingress per room for MVP)
- Ingress failover and high availability testing
- WHIP with ICE candidate filtering (network complexity)
- Production-grade RTMPS with valid TLS certificates

## Dependencies

### Existing Infrastructure (Required)

- `LiveKitContainer` - Docker-based LiveKit server management
- `RedisContainer` - Required for ingress-server communication
- `ContainerStateManager` - Container lifecycle management
- `RoomServiceClient` - Room and participant verification
- `AccessToken` - Token generation for ingress (IngressAdmin grant)
- Docker network setup for container communication

### New Components (To Be Created)

| Component | Location | Description |
|-----------|----------|-------------|
| `IngressContainer` | `src/main/java/.../docker/` | TestContainers wrapper for livekit/ingress |
| `FFmpegContainer` | `src/main/java/.../docker/` | FFmpeg-based RTMP stream generator |
| `IngressStateManager` | `src/main/java/.../state/` | Tracks ingresses for cleanup |
| `LiveKitIngressSteps` | `src/test/java/.../steps/` | Cucumber step definitions |
| `livekit_ingress.feature` | `src/test/resources/features/` | BDD scenarios |
| `with_ingress/config.yaml` | `src/test/resources/livekit/config/v1.8.4/` | Server config profile |

**Note:** Use SDK's `IngressServiceClient` directly (no wrapper needed).

### External Dependencies

- Docker image: `livekit/ingress:latest` (version configurable)
- Docker image: `linuxserver/ffmpeg:latest` (for RTMP streaming)
- Docker image: `livekit/gstreamer:1.22.8-prod-rs` (for WHIP)
- Redis for ingress coordination

## Architecture Overview

```
+------------------+     +-------------------+     +------------------+
|   Feature File   | --> | Step Definitions  | --> | IngressService   |
| (Gherkin/BDD)    |     | (Cucumber)        |     | Client (SDK)     |
+------------------+     +-------------------+     +------------------+
                                 |                         |
                                 v                         v
                         +---------------+         +------------------+
                         | ManagerProvider|        | LiveKit Server   |
                         | .ingress()    |         | (Ingress API)    |
                         +---------------+         +------------------+
                                 |                         |
                    +------------+------------+            |
                    v            v            v            v
             +---------+  +-----------+  +--------+  +---------+
             | Ingress |  | Redis     |  | FFmpeg |  | LiveKit |
             | Container| | Container |  | Stream |  | Room    |
             +---------+  +-----------+  +--------+  +---------+
                    |                                      |
                    +------------> Ingress joins room <----+
                                   as participant
```

### Container Network Topology

```
Docker Network: livekit-test-network
+------------------------------------------------------------------+
|                                                                  |
|  +--------------+       +-------------+       +--------------+   |
|  | livekit1     |<----->| redis       |<----->| ingress1     |   |
|  | :7880 (WS)   |       | :6379       |       | :1935 (RTMP) |   |
|  | :7881 (API)  |       +-------------+       | :8080 (WHIP) |   |
|  +--------------+                              +--------------+   |
|         ^                                             ^          |
|         |                                             |          |
|         v                                             v          |
|  +--------------+                              +--------------+   |
|  | Subscriber   |                              | FFmpeg       |   |
|  | Browser      |                              | RTMP Stream  |   |
|  +--------------+                              +--------------+   |
|                                                                  |
+------------------------------------------------------------------+
```

## Related Documentation

- [LiveKit Ingress Overview](https://docs.livekit.io/transport/media/ingress-egress/ingress/)
- [Ingress Self-Hosting Guide](https://docs.livekit.io/transport/self-hosting/ingress/)
- [Encoder Configuration](https://docs.livekit.io/transport/media/ingress-egress/ingress/encoders/)
- [Transcoding Configuration](https://docs.livekit.io/transport/media/ingress-egress/ingress/transcode/)
- [LiveKit Kotlin Server SDK - IngressServiceClient](https://github.com/livekit/server-sdk-kotlin)

## Research Findings

### Ingress Input Types

| Input Type | Protocol | Description | Test Complexity |
|------------|----------|-------------|-----------------|
| RTMP_INPUT | RTMP/RTMPS | Push-based stream from OBS/FFmpeg | Medium |
| WHIP_INPUT | WHIP (WebRTC) | Low-latency WebRTC ingestion | High |
| URL_INPUT | HTTP/HLS | Pull-based from URL/file | Low |

### Ingress Container Requirements

- **CPU**: 4 cores minimum (for transcoding)
- **Memory**: 4 GB minimum
- **Ports**: 1935 (RTMP), 8080 (WHIP), 7885 (UDP for WHIP)
- **Dependencies**: Redis for coordination with LiveKit server

### IngressInfo Key Fields

| Field | Description |
|-------|-------------|
| `ingress_id` | Unique identifier for the ingress |
| `name` | Human-readable name |
| `stream_key` | Secret key for RTMP/WHIP authentication |
| `url` | Endpoint URL for encoder to connect |
| `input_type` | RTMP_INPUT, WHIP_INPUT, URL_INPUT |
| `room_name` | Target room for ingress participant |
| `participant_identity` | Identity of ingress participant |
| `participant_name` | Display name in room |
| `state` | Current ingress state (see below) |

### Ingress State Machine

```
ENDPOINT_INACTIVE --> ENDPOINT_BUFFERING --> ENDPOINT_PUBLISHING
        ^                     |                      |
        |                     v                      v
        +<-- ENDPOINT_ERROR <-+<---------------------+
        |
        +<-- (DeleteIngress) <-----------------------+
```

| State | Description |
|-------|-------------|
| `ENDPOINT_INACTIVE` | Created but not connected |
| `ENDPOINT_BUFFERING` | Stream connected, buffering/transcoding |
| `ENDPOINT_PUBLISHING` | Actively publishing to room |
| `ENDPOINT_ERROR` | Error occurred |

### LiveKit Server Configuration for Ingress

```yaml
ingress:
  rtmp_base_url: 'rtmp://ingress1:1935/live'
  whip_base_url: 'http://ingress1:8080/whip'
```

### Ingress Container Configuration

```yaml
api_key: devkey
api_secret: secret
ws_url: ws://livekit1:7880
redis:
  address: redis:6379
rtmp_port: 1935
whip_port: 8080
health_port: 8081
logging:
  level: debug
```

## Resolved Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| FFmpeg Availability | **Docker container** | Use `linuxserver/ffmpeg` container for isolation and consistency |
| WHIP Priority | **Essential** | WHIP is required for MVP, not optional |
| Stream Duration | **15-30 seconds** | Sufficient to verify stable streaming without excessive test time |
| Ingress Version | **Configurable** | Follow project pattern with `-Pingress_docker_version` gradle property |

## Open Questions

1. **Transcoding Verification**: How do we verify transcoding quality (visual inspection vs metrics)?
2. **Container Resources**: Will test runners have sufficient CPU/RAM for transcoding?
3. **URL Input Sources**: Should we host test media files or use public URLs?

## Version Configuration

Ingress version is configurable via multiple methods (following project patterns):

```powershell
# Gradle property (recommended)
./gradlew test -Pingress_docker_version=v1.8.4

# System property
./gradlew test -Dlivekit.ingress.version=v1.8.4

# Environment variable
$env:LIVEKIT_INGRESS_VERSION="v1.8.4"
./gradlew test
```

Default version will be defined in `gradle.properties`:
```properties
ingress_docker_version=v1.4.3
```

## Version Compatibility Notes

| LiveKit Version | Ingress Version | Notes |
|-----------------|-----------------|-------|
| v1.7.x | v1.2.x | Legacy support |
| v1.8.x | v1.3.x - v1.4.x | Recommended |

The Ingress service uses its own version numbering (v1.2, v1.3, v1.4) which is independent of LiveKit server versions. Check Docker Hub for available versions: `livekit/ingress`.
