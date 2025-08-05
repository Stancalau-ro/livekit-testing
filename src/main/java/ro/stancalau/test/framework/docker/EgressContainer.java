package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import com.github.dockerjava.api.model.Capability;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EgressContainer extends GenericContainer<EgressContainer> {

    private static final String DEFAULT_EGRESS_ALIAS = "egress";
    public static final int GRPC_PORT = 7980;
    
    @Getter
    private final Network network;
    
    private String alias;
    private final String apiKey;
    private final String apiSecret;
    private final String wsUrl;
    
    private EgressContainer(String imageName, Network network, String apiKey, String apiSecret, String wsUrl) {
        super(imageName);
        this.network = network;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.wsUrl = wsUrl;
    }
    
    public static EgressContainer createContainer(String alias, Network network, String egressVersion, 
                                                  String livekitWsUrl, String apiKey, String apiSecret,
                                                  @Nullable String configFilePath, @Nullable String logDestinationPath,
                                                  @Nullable String redisUrl) {
        
        String logDirPath = (logDestinationPath != null) 
            ? logDestinationPath 
            : "out/bdd/scenarios/current/docker/" + alias;
        
        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        String recordingsPath = logDestinationPath != null 
            ? logDestinationPath.replace("/docker/" + alias, "/recordings")
            : "out/bdd/scenarios/current/recordings";
        File recordingsDir = new File(recordingsPath);
        recordingsDir.mkdirs();

        try {
            recordingsDir.setWritable(true, false); // writable by all users
            recordingsDir.setReadable(true, false); // readable by all users  
            recordingsDir.setExecutable(true, false); // executable by all users
        } catch (Exception e) {
            log.warn("Failed to set permissions on recordings directory: {}", e.getMessage());
        }

        String egressImage = "livekit/egress:" + egressVersion;
        
        EgressContainer container = new EgressContainer(egressImage, network, apiKey, apiSecret, livekitWsUrl)
                .withExposedPorts(GRPC_PORT)
                .withFileSystemBind(recordingsDir.getAbsolutePath(), "/out/recordings", BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withCapAdd(Capability.SYS_ADMIN));

        // Add log capturing using unified approach
        container = ContainerLogUtils.withLogCapture(container, logDirRoot, "egress.log");
        
        // Create dynamic config with Redis URL
        if (redisUrl != null) {
            String configBody = createEgressConfigBody(apiKey, apiSecret, livekitWsUrl, redisUrl, alias);
            container = container.withEnv("EGRESS_CONFIG_BODY", configBody);
            log.info("Created dynamic egress config with Redis URL: {}", redisUrl);
        } else if (configFilePath != null && !configFilePath.isEmpty()) {
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                container = container.withFileSystemBind(configFile.getAbsolutePath(),
                        "/egress.yaml", BindMode.READ_ONLY)
                        .withEnv("EGRESS_CONFIG_FILE", "/egress.yaml");
                log.info("Binding egress config file {} to container", configFilePath);
            } else {
                log.warn("Egress config file not found: {}", configFilePath);
            }
        }
        
        container = container
                .withExtraHost("host.docker.internal", "host-gateway")
                .withNetwork(network)
                .withNetworkAliases(alias)
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.ofSeconds(60)));
        
        container.alias = alias;
        
        return container;
    }
    
    public static EgressContainer createContainer(String alias, Network network, String livekitWsUrl, String redisUrl) {
        String egressVersion = ro.stancalau.test.framework.util.TestConfig.getEgressVersion();
        String defaultConfigPath = "src/test/resources/livekit/config/" + egressVersion + "/with_egress/egress.yaml";
        return createContainer(alias, network, egressVersion, livekitWsUrl, 
                LiveKitContainer.API_KEY, LiveKitContainer.SECRET, defaultConfigPath, null, redisUrl);
    }
    
    public static EgressContainer createContainer(String alias, Network network, String livekitWsUrl) {
        return createContainer(alias, network, livekitWsUrl, null);
    }
    
    public String getGrpcUrl() {
        return getContainerIpAddress() + ":" + getMappedPort(GRPC_PORT);
    }
    
    public String getNetworkGrpcUrl() {
        return getAlias() + ":" + GRPC_PORT;
    }
    
    public String getAlias() {
        return alias != null ? alias : getNetworkAliases().get(0);
    }
    
    private static String createEgressConfigBody(String apiKey, String apiSecret, String wsUrl, String redisUrl, String alias) {
        return String.format("""
            api_key: %s
            api_secret: %s
            ws_url: %s
            redis:
              address: %s
            log_level: debug
            insecure: true
            disable_https: true
            template_base: "http://localhost:7980"
            template_port: 7980
            template_address: "0.0.0.0"
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
            """, apiKey, apiSecret, wsUrl, redisUrl, alias);
    }
}