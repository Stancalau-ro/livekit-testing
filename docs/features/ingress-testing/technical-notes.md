# Ingress Stream Input Testing - Technical Implementation Notes

## Architecture Overview

Ingress testing extends the existing test infrastructure to verify that external media streams (RTMP, WHIP, URL) can be brought into LiveKit rooms. The implementation follows established project patterns:

1. **Ingress Container Layer** - `IngressContainer` in `ro.stancalau.test.framework.docker`
2. **API Client Layer** - Use SDK's `IngressServiceClient` directly (no wrapper needed)
3. **Stream Simulation Layer** - `FFmpegContainer` in `ro.stancalau.test.framework.docker`
4. **Step Definition Layer** - `LiveKitIngressSteps` in `ro.stancalau.test.bdd.steps`
5. **State Management Layer** - `IngressStateManager` in `ro.stancalau.test.framework.state`

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
                                 |                         ^
                    +------------+------------+            |
                    v            v            v            |
             +---------+  +-----------+  +--------+        |
             | Ingress |  | Redis     |  | FFmpeg |        |
             | Container| | Container |  | Container|      |
             +---------+  +-----------+  +--------+        |
                    |            |                         |
                    +------------+------> Ingress joins -->+
                                          room as participant
```

---

## Resolved Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Stream Simulation | **FFmpeg in Docker** | Use `linuxserver/ffmpeg` container for isolation and consistency |
| WHIP Testing | **Essential** | Required for complete ingress coverage; not optional |
| Stream Duration | **15-30 seconds** | Sufficient to verify stable streaming without excessive test runtime |
| Version Management | **Configurable** | Follow existing patterns with `-Pingress_docker_version` gradle property |
| API Client | **SDK Direct** | Use `IngressServiceClient` from SDK directly, no wrapper needed |

---

## File Locations

Following existing project structure:

| Component | Location |
|-----------|----------|
| `IngressContainer.java` | `src/main/java/ro/stancalau/test/framework/docker/` |
| `FFmpegContainer.java` | `src/main/java/ro/stancalau/test/framework/docker/` |
| `IngressStateManager.java` | `src/main/java/ro/stancalau/test/framework/state/` |
| `LiveKitIngressSteps.java` | `src/test/java/ro/stancalau/test/bdd/steps/` |
| `livekit_ingress.feature` | `src/test/resources/features/` |
| Config profile | `src/test/resources/livekit/config/v1.8.4/with_ingress/` |

---

## Configuration Requirements

### gradle.properties

```properties
ingress_docker_version=v1.8.4
```

### build.gradle Addition

```groovy
systemProperty 'ingress.version', project.findProperty('ingress_docker_version') ?: 'v1.8.4'
```

### TestConfig.java Addition

```java
private static final String DEFAULT_INGRESS_VERSION = "v1.8.4";
private static final String INGRESS_VERSION_PROPERTY = "ingress.version";
private static final String INGRESS_VERSION_ENV = "INGRESS_VERSION";

public static String getIngressVersion() {
    String version = System.getProperty(INGRESS_VERSION_PROPERTY);
    if (version != null && !version.isEmpty()) {
        return version;
    }
    version = System.getenv(INGRESS_VERSION_ENV);
    if (version != null && !version.isEmpty()) {
        return version;
    }
    return DEFAULT_INGRESS_VERSION;
}
```

### LiveKit Server Config Profile

Create `src/test/resources/livekit/config/v1.8.4/with_ingress/config.yaml`:

```yaml
port: 7880
rtc:
  port_range_start: 50000
  port_range_end: 50100
  use_external_ip: false
keys:
  devkey: secret
ingress:
  rtmp_base_url: 'rtmp://ingress:1935/live'
  whip_base_url: 'http://ingress:8080/whip'
```

---

## Component Implementation

### 1. IngressContainer

Follow `EgressContainer` pattern with static factory method:

```java
package ro.stancalau.test.framework.docker;

@Slf4j
public class IngressContainer extends GenericContainer<IngressContainer> {

    private static final int RTMP_PORT = 1935;
    private static final int WHIP_PORT = 8080;
    private static final int HEALTH_PORT = 8081;

    @Getter
    private final Network network;

    private String alias;

    private IngressContainer(String imageName, Network network) {
        super(imageName);
        this.network = network;
    }

    public static IngressContainer createContainer(
            String alias,
            Network network,
            String ingressVersion,
            String livekitWsUrl,
            String apiKey,
            String apiSecret,
            String redisUrl,
            @Nullable String logDestinationPath) {

        String logDirPath = (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        String ingressImage = "livekit/ingress:" + ingressVersion;

        IngressContainer container = new IngressContainer(ingressImage, network)
            .withExposedPorts(RTMP_PORT, WHIP_PORT, HEALTH_PORT);

        container = ContainerLogUtils.withLogCapture(container, logDirRoot, "ingress.log");
        container = configureIngress(container, apiKey, apiSecret, livekitWsUrl, redisUrl);
        container = configureNetworkAndWaiting(container, network, alias);

        container.alias = alias;

        return container;
    }

    public static IngressContainer createContainer(
            String alias,
            Network network,
            String livekitWsUrl,
            String redisUrl) {
        return createContainer(
            alias,
            network,
            TestConfig.getIngressVersion(),
            livekitWsUrl,
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET,
            redisUrl,
            null
        );
    }

    private static IngressContainer configureIngress(
            IngressContainer container,
            String apiKey,
            String apiSecret,
            String wsUrl,
            String redisUrl) {
        String config = String.format("""
            api_key: %s
            api_secret: %s
            ws_url: %s
            redis:
              address: %s
            rtmp_port: %d
            whip_port: %d
            health_port: %d
            logging:
              level: debug
            """, apiKey, apiSecret, wsUrl, redisUrl,
            RTMP_PORT, WHIP_PORT, HEALTH_PORT);
        return container.withEnv("INGRESS_CONFIG_BODY", config);
    }

    private static IngressContainer configureNetworkAndWaiting(
            IngressContainer container,
            Network network,
            String alias) {
        return container
            .withNetwork(network)
            .withNetworkAliases(alias)
            .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT)
                .withStartupTimeout(Duration.ofSeconds(60)));
    }

    public String getRtmpUrl() {
        return String.format("rtmp://%s:%d/live", alias, RTMP_PORT);
    }

    public String getWhipUrl() {
        return String.format("http://%s:%d/whip", alias, WHIP_PORT);
    }
}
```

### 2. FFmpegContainer

```java
package ro.stancalau.test.framework.docker;

@Slf4j
public class FFmpegContainer extends GenericContainer<FFmpegContainer> {

    @Getter
    private final Network network;

    private FFmpegContainer(String imageName, Network network) {
        super(imageName);
        this.network = network;
    }

    public static FFmpegContainer createRtmpStream(
            String alias,
            Network network,
            String rtmpUrl,
            String streamKey,
            int durationSeconds,
            @Nullable String logDestinationPath) {

        String logDirPath = (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        String fullRtmpUrl = rtmpUrl + "/" + streamKey;

        FFmpegContainer container = new FFmpegContainer("linuxserver/ffmpeg:latest", network);

        container = ContainerLogUtils.withLogCapture(container, logDirRoot, "ffmpeg.log");

        container = container
            .withNetwork(network)
            .withNetworkAliases(alias)
            .withCommand(
                "-re",
                "-f", "lavfi",
                "-i", "testsrc=size=1280x720:rate=30",
                "-f", "lavfi",
                "-i", "sine=frequency=1000:sample_rate=48000",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "flv",
                "-t", String.valueOf(durationSeconds),
                fullRtmpUrl
            );

        return container;
    }
}
```

### 3. IngressStateManager

```java
package ro.stancalau.test.framework.state;

@Slf4j
public class IngressStateManager {

    private final Map<String, IngressInfo> ingresses = new ConcurrentHashMap<>();

    public void registerIngress(String name, IngressInfo info) {
        ingresses.put(name, info);
        log.debug("Registered ingress: {} with id: {}", name, info.getIngressId());
    }

    public IngressInfo getIngress(String name) {
        return ingresses.get(name);
    }

    public Map<String, IngressInfo> getAllIngresses() {
        return new HashMap<>(ingresses);
    }

    public void clearAll() {
        ingresses.clear();
        log.debug("Cleared all ingress state");
    }
}
```

### 4. ManagerFactory Integration

Add to `ManagerFactory.java`:

```java
public static ManagerSet createManagerSet() {
    ContainerStateManager containerManager = new ContainerStateManager();
    WebDriverStateManager webDriverManager = new WebDriverStateManager(containerManager);
    AccessTokenStateManager tokenManager = new AccessTokenStateManager();
    EgressStateManager egressManager = new EgressStateManager();
    IngressStateManager ingressManager = new IngressStateManager();  // ADD THIS
    // ... other managers

    return new ManagerSet(
        containerManager,
        webDriverManager,
        tokenManager,
        egressManager,
        ingressManager,  // ADD THIS
        // ... other managers
    );
}
```

### 5. ManagerProvider Integration

Add to `ManagerProvider.java`:

```java
public static IngressStateManager getIngressStateManager() {
    return getCurrentManagerSet().ingressStateManager();
}

public static IngressStateManager ingress() {
    return getIngressStateManager();
}
```

### 6. ManagerSet Record Update

```java
public record ManagerSet(
    ContainerStateManager containerStateManager,
    WebDriverStateManager webDriverStateManager,
    AccessTokenStateManager accessTokenStateManager,
    EgressStateManager egressStateManager,
    IngressStateManager ingressStateManager,
) {
    public void cleanup() {
        ingressStateManager.clearAll();
        egressStateManager.clearAll();
    }
}
```

---

## Step Definitions

### LiveKitIngressSteps.java

```java
package ro.stancalau.test.bdd.steps;

@Slf4j
public class LiveKitIngressSteps {

    @Given("an Ingress service is running with service name {string}")
    public void ingressServiceRunning(String serviceName) {
        ContainerStateManager containers = ManagerProvider.containers();
        LiveKitContainer livekit = containers.getLiveKitContainer("livekit1");
        RedisContainer redis = containers.getContainer("redis", RedisContainer.class);
        Network network = containers.getOrCreateNetwork();

        IngressContainer ingress = IngressContainer.createContainer(
            serviceName,
            network,
            livekit.getWsUrl(),
            redis.getNetworkRedisUrl()
        );
        ingress.start();
        containers.registerContainer(serviceName, ingress);

        log.info("Ingress service {} started with RTMP URL: {}", serviceName, ingress.getRtmpUrl());
    }

    @When("an RTMP ingress {string} is created for room {string} with identity {string}")
    public void createRtmpIngress(String ingressName, String roomName, String identity) {
        ContainerStateManager containers = ManagerProvider.containers();
        LiveKitContainer livekit = containers.getLiveKitContainer("livekit1");

        IngressServiceClient client = IngressServiceClient.createClient(
            livekit.getHttpUrl(),
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET
        );

        CreateIngressRequest request = CreateIngressRequest.newBuilder()
            .setInputType(IngressInput.RTMP_INPUT)
            .setName(ingressName)
            .setRoomName(roomName)
            .setParticipantIdentity(identity)
            .setParticipantName(identity)
            .build();

        IngressInfo info = client.createIngress(request).execute().body();
        ManagerProvider.ingress().registerIngress(ingressName, info);

        log.info("Created RTMP ingress {} for room {} with URL: {}",
            ingressName, roomName, info.getUrl());
    }

    @When("an RTMP stream is started for ingress {string} with duration {int} seconds")
    public void startRtmpStream(String ingressName, int durationSeconds) {
        IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
        ContainerStateManager containers = ManagerProvider.containers();

        FFmpegContainer ffmpeg = FFmpegContainer.createRtmpStream(
            "ffmpeg-" + ingressName,
            containers.getOrCreateNetwork(),
            info.getUrl(),
            info.getStreamKey(),
            durationSeconds,
            null
        );
        ffmpeg.start();
        containers.registerContainer("ffmpeg-" + ingressName, ffmpeg);

        log.info("Started RTMP stream to {} for {} seconds", info.getUrl(), durationSeconds);
    }

    @Then("ingress {string} should have state {string}")
    public void verifyIngressState(String ingressName, String expectedState) {
        IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
        ContainerStateManager containers = ManagerProvider.containers();
        LiveKitContainer livekit = containers.getLiveKitContainer("livekit1");

        IngressServiceClient client = IngressServiceClient.createClient(
            livekit.getHttpUrl(),
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET
        );

        List<IngressInfo> ingresses = client.listIngress(
            ListIngressRequest.newBuilder()
                .setIngressId(info.getIngressId())
                .build()
        ).execute().body();

        assertThat(ingresses).isNotEmpty();
        assertThat(ingresses.get(0).getState().getStatus().name()).isEqualTo(expectedState);
    }
}
```

---

## Container Network Setup

All containers must share the same Docker network:

```java
Network network = ManagerProvider.containers().getOrCreateNetwork();

LiveKitContainer livekit = LiveKitContainer.createContainer(
    "livekit1", network, configPath, "with_ingress");

RedisContainer redis = RedisContainer.createContainer("redis", network);

IngressContainer ingress = IngressContainer.createContainer(
    "ingress", network, livekit.getWsUrl(), redis.getNetworkRedisUrl());
```

---

## Data Flow

### RTMP Ingress Stream Flow

```
1. Test creates room via RoomServiceClient
2. Test creates RTMP ingress via IngressServiceClient
3. LiveKit server returns IngressInfo with URL and stream key
4. Test starts FFmpegContainer with URL and stream key
5. FFmpeg connects to Ingress container via RTMP
6. Ingress container transcodes stream (GStreamer pipeline)
7. Ingress container joins room as participant
8. Ingress container publishes transcoded tracks to room
9. Subscriber browser receives tracks via WebRTC
```

### Ingress State Transitions

```
ENDPOINT_INACTIVE --> ENDPOINT_BUFFERING --> ENDPOINT_PUBLISHING
        ^                     |                      |
        |                     v                      v
        +<-- Stream disconnects <--------------------+
        |
        +<-- DeleteIngress <-------------------------+
```

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Container networking | Medium | Use shared Docker network, container aliases |
| RTMP stream reliability | Medium | Robust retry logic, container log capture |
| Transcoding performance | Low | Use lower resolution presets, short test streams |
| SDK compatibility | Low | Verify IngressServiceClient in SDK before implementation |

---

## Implementation Phases

### Phase 1: Infrastructure

1. Add `ingress_docker_version` to `gradle.properties`
2. Add `getIngressVersion()` to `TestConfig.java`
3. Create `with_ingress/` config profile
4. Create `IngressContainer.java`

### Phase 2: State Management

1. Create `IngressStateManager.java`
2. Update `ManagerFactory.java`
3. Update `ManagerProvider.java`
4. Update `ManagerSet` record

### Phase 3: RTMP Streaming

1. Create `FFmpegContainer.java`
2. Create `LiveKitIngressSteps.java`
3. Create `livekit_ingress.feature`
4. Implement RTMP scenarios

### Phase 4: WHIP Support

1. Add GStreamer container or WHIP client
2. Implement WHIP step definitions
3. Add WHIP scenarios to feature file
