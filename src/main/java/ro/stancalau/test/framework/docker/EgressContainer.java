package ro.stancalau.test.framework.docker;

import com.github.dockerjava.api.model.Capability;
import java.io.File;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import ro.stancalau.test.framework.config.S3Config;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.util.PathUtils;

@Slf4j
public class EgressContainer extends GenericContainer<EgressContainer> {

  public static final int GRPC_PORT = 7980;

  private static final String DEFAULT_EGRESS_ALIAS = "egress";

  @Getter private final Network network;

  private String alias;

  private EgressContainer(
      String imageName, Network network, String apiKey, String apiSecret, String wsUrl) {
    super(imageName);
    this.network = network;
  }

  public static EgressContainer createContainer(
      String alias,
      Network network,
      String egressVersion,
      String livekitWsUrl,
      String apiKey,
      String apiSecret,
      @Nullable String configFilePath,
      @Nullable String logDestinationPath,
      @Nullable String redisUrl) {

    String logDirPath =
        (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

    File logDirRoot = new File(logDirPath);
    logDirRoot.mkdirs();

    String egressImage = "livekit/egress:" + egressVersion;

    EgressContainer container =
        new EgressContainer(egressImage, network, apiKey, apiSecret, livekitWsUrl)
            .withExposedPorts(GRPC_PORT);

    container = configureOutputDirectories(container, alias, logDestinationPath);

    container = configureContainerCapabilities(container);

    container = ContainerLogUtils.withLogCapture(container, logDirRoot, "egress.log");
    container =
        configureEgressConfig(
            container, apiKey, apiSecret, livekitWsUrl, redisUrl, alias, configFilePath);

    container = configureNetworkAndWaiting(container, network, alias);

    container.alias = alias;

    return container;
  }

  public static EgressContainer createContainer(
      String alias, Network network, String livekitWsUrl, String redisUrl) {
    String egressVersion = TestConfig.getEgressVersion();
    String defaultConfigPath =
        PathUtils.egressConfigPath(egressVersion, "with_egress", "egress.yaml");
    return createContainer(
        alias,
        network,
        egressVersion,
        livekitWsUrl,
        LiveKitContainer.API_KEY,
        LiveKitContainer.SECRET,
        defaultConfigPath,
        null,
        redisUrl);
  }

  public static EgressContainer createContainer(
      String alias, Network network, String livekitWsUrl) {
    return createContainer(alias, network, livekitWsUrl, null);
  }

  public static EgressContainer createContainerWithS3(
      String alias,
      Network network,
      String egressVersion,
      String livekitWsUrl,
      String apiKey,
      String apiSecret,
      @Nullable String logDestinationPath,
      @Nullable String redisUrl,
      S3Config s3Config) {

    String logDirPath =
        (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

    File logDirRoot = new File(logDirPath);
    logDirRoot.mkdirs();

    String egressImage = "livekit/egress:" + egressVersion;

    EgressContainer container =
        new EgressContainer(egressImage, network, apiKey, apiSecret, livekitWsUrl)
            .withExposedPorts(GRPC_PORT)
            .withCreateContainerCmdModifier(
                cmd ->
                    cmd.getHostConfig()
                        .withCapAdd(Capability.SYS_ADMIN)
                        .withSecurityOpts(List.of("apparmor:unconfined")));

    container = ContainerLogUtils.withLogCapture(container, logDirRoot, "egress.log");

    if (redisUrl != null) {
      String configBody =
          createEgressConfigBodyWithS3(apiKey, apiSecret, livekitWsUrl, redisUrl, alias, s3Config);
      container = container.withEnv("EGRESS_CONFIG_BODY", configBody);
      log.info("Created dynamic egress config with Redis URL and S3 config: {}", redisUrl);
    }

    container =
        container
            .withExtraHost("host.docker.internal", "host-gateway")
            .withNetwork(network)
            .withNetworkAliases(alias)
            .waitingFor(
                Wait.forLogMessage(".*service ready.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    container.alias = alias;

    return container;
  }

  public String getGrpcUrl() {
    return getContainerIpAddress() + ":" + getMappedPort(GRPC_PORT);
  }

  public String getNetworkGrpcUrl() {
    return getAlias() + ":" + GRPC_PORT;
  }

  public String getAlias() {
    return alias != null ? alias : getNetworkAliases().getFirst();
  }

  private static EgressContainer configureOutputDirectories(
      EgressContainer container, String alias, String logDestinationPath) {
    container =
        bindOutputDirectory(
            container, alias, logDestinationPath, "video-recordings", "/out/video-recordings");
    container =
        bindOutputDirectory(container, alias, logDestinationPath, "snapshots", "/out/snapshots");
    return container;
  }

  private static EgressContainer bindOutputDirectory(
      EgressContainer container,
      String alias,
      String logDestinationPath,
      String dirType,
      String containerPath) {
    String hostPath =
        logDestinationPath != null
            ? logDestinationPath.replace(PathUtils.join("docker", alias), dirType)
            : PathUtils.join(PathUtils.currentScenarioPath(), dirType);

    File hostDir = new File(hostPath);
    hostDir.mkdirs();

    try {
      hostDir.setWritable(true, false);
      hostDir.setReadable(true, false);
      hostDir.setExecutable(true, false);
    } catch (Exception e) {
      log.warn("Failed to set permissions on {} directory: {}", dirType, e.getMessage());
    }

    container =
        container.withFileSystemBind(hostDir.getAbsolutePath(), containerPath, BindMode.READ_WRITE);
    log.debug("Bound {} directory: {}", dirType, hostDir.getAbsolutePath());
    return container;
  }

  private static EgressContainer configureContainerCapabilities(EgressContainer container) {
    return container.withCreateContainerCmdModifier(
        cmd ->
            cmd.getHostConfig()
                .withCapAdd(Capability.SYS_ADMIN)
                .withSecurityOpts(List.of("apparmor:unconfined")));
  }

  private static EgressContainer configureEgressConfig(
      EgressContainer container,
      String apiKey,
      String apiSecret,
      String livekitWsUrl,
      String redisUrl,
      String alias,
      String configFilePath) {
    if (redisUrl != null) {
      String configBody = createEgressConfigBody(apiKey, apiSecret, livekitWsUrl, redisUrl, alias);
      container = container.withEnv("EGRESS_CONFIG_BODY", configBody);
      log.info("Created dynamic egress config with Redis URL: {}", redisUrl);
    } else if (configFilePath != null && !configFilePath.isEmpty()) {
      File configFile = new File(configFilePath);
      if (configFile.exists()) {
        container =
            container
                .withFileSystemBind(
                    configFile.getAbsolutePath(), "/egress.yaml", BindMode.READ_ONLY)
                .withEnv("EGRESS_CONFIG_FILE", "/egress.yaml");
        log.info("Binding egress config file {} to container", configFilePath);
      } else {
        log.warn("Egress config file not found: {}", configFilePath);
      }
    }
    return container;
  }

  private static EgressContainer configureNetworkAndWaiting(
      EgressContainer container, Network network, String alias) {
    return container
        .withExtraHost("host.docker.internal", "host-gateway")
        .withNetwork(network)
        .withNetworkAliases(alias)
        .waitingFor(
            Wait.forLogMessage(".*service ready.*", 1).withStartupTimeout(Duration.ofSeconds(60)));
  }

  private static String createEgressConfigBody(
      String apiKey, String apiSecret, String wsUrl, String redisUrl, String alias) {
    return String.format(
        """
            api_key: %s
            api_secret: %s
            ws_url: %s
            redis:
              address: %s
            log_level: debug
            insecure: true
            disable_https: true
            template_base: "http://127.0.0.1:7980"
            template_port: 7980
            template_address: "127.0.0.1"
            # WebRTC configuration for SDK connections
            health_port: 9999
            prometheus_port: 9998
            # ICE configuration for container networking
            ice:
              # Use internal STUN servers to avoid external dependencies
              stun_servers: []
              # Allow connections to container network addresses
              tcp_fallback: true
              # Set connection timeout
              timeout: 10s
            chrome:
              extra_flags:
                - "--use-fake-ui-for-media-stream"
                - "--autoplay-policy=no-user-gesture-required"
                - "--disable-web-security"
                - "--disable-features=WebRtcHideLocalIpsWithMdns"
                - "--disable-dev-shm-usage"
                - "--no-sandbox"
                - "--allow-running-insecure-content"
                - "--unsafely-treat-insecure-origin-as-secure=ws://%s:7880"
                - "--unsafely-treat-insecure-origin-as-secure=http://%s:7880"
""",
        apiKey, apiSecret, wsUrl, redisUrl, alias, alias);
  }

  private static String createEgressConfigBodyWithS3(
      String apiKey,
      String apiSecret,
      String wsUrl,
      String redisUrl,
      String alias,
      S3Config s3Config) {
    return String.format(
        """
            api_key: %s
            api_secret: %s
            ws_url: %s
            redis:
              address: %s
            log_level: debug
            insecure: true
            disable_https: true
            template_base: "http://127.0.0.1:7980"
            template_port: 7980
            template_address: "127.0.0.1"
            health_port: 9999
            prometheus_port: 9998
            ice:
              stun_servers: []
              tcp_fallback: true
              timeout: 10s
            chrome:
              extra_flags:
                - "--use-fake-ui-for-media-stream"
                - "--autoplay-policy=no-user-gesture-required"
                - "--disable-web-security"
                - "--disable-features=WebRtcHideLocalIpsWithMdns"
                - "--disable-dev-shm-usage"
                - "--no-sandbox"
                - "--allow-running-insecure-content"
                - "--unsafely-treat-insecure-origin-as-secure=ws://livekit1:7880"
                - "--unsafely-treat-insecure-origin-as-secure=http://livekit1:7880"
            s3:
              access_key: %s
              secret_key: %s
              bucket: %s
              endpoint: %s
              region: %s
              force_path_style: true
            """,
        apiKey,
        apiSecret,
        wsUrl,
        redisUrl,
        s3Config.accessKey,
        s3Config.secretKey,
        s3Config.bucket,
        s3Config.endpoint,
        s3Config.region);
  }
}
