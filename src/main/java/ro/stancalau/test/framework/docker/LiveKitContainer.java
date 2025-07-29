package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import javax.annotation.Nullable;
import java.io.File;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
public class LiveKitContainer extends GenericContainer<LiveKitContainer> {

    private static final String DEFAULT_SERVER_ALIAS = "livekitServer";

    public static final int HTTP_PORT = 7880;
    public static final String API_KEY = "devkey";
    public static final String SECRET = "secret";

    @Getter
    private final Network network;

    private String alias;

    private LiveKitContainer(String source, Network network) {
        super(source);
        this.network = network;
    }

    public static LiveKitContainer createContainer(String alias, Network network, String livekitVersion, @Nullable String configFilePath) {
        return createContainer(alias, network, livekitVersion, configFilePath, null);
    }

    public static LiveKitContainer createContainer(String alias, Network network, String livekitVersion, @Nullable String configFilePath, @Nullable String logDestinationPath) {
        String logDirPath = (logDestinationPath != null) 
            ? logDestinationPath 
            : "out/bdd/docker/livekit/" + alias;
        
        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        String liveKitImage = "local/livekit:" + livekitVersion;

        DockerImageUtils.ensureDockerImageExists(liveKitImage, "Docker/livekit");

        LiveKitContainer container = new LiveKitContainer(liveKitImage, network)
                .withExposedPorts(HTTP_PORT)
                .withFileSystemBind(logDirRoot.getAbsolutePath(),
                        "/var/log", BindMode.READ_WRITE);

        if (configFilePath != null && !configFilePath.isEmpty()) {
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                container = container.withFileSystemBind(configFile.getAbsolutePath(),
                        "/config.yaml", BindMode.READ_ONLY);
                log.info("Binding config file {} to container", configFilePath);
            } else {
                log.warn("Config file not found: {}", configFilePath);
            }
        }

        container = container.withEnv("TZ", ZoneId.systemDefault().toString())
                .withExtraHost("host.docker.internal", "host-gateway")
                .withNetwork(network)
                .withNetworkAliases(alias);

        container.alias = alias;

        return container;
    }

    public static LiveKitContainer createContainer(String alias, Network network, String configFilePath) {
        return createContainer(alias, network, "v1.8.4", configFilePath);
    }

    public static LiveKitContainer createContainer(String alias, Network network) {
        return createContainer(alias, network, "v1.8.4", null);
    }

    public static LiveKitContainer createContainer(Network network) {
        return createContainer(DEFAULT_SERVER_ALIAS, network, "v1.8.4", null);
    }

    public static LiveKitContainer createContainer(Network network, String configFilePath) {
        return createContainer(DEFAULT_SERVER_ALIAS, network, "v1.8.4", configFilePath);
    }

    public static LiveKitContainer createContainer(Network network, String configFilePath, String livekitVersion) {
        return createContainer(DEFAULT_SERVER_ALIAS, network, livekitVersion, configFilePath);
    }

    public String getHttpLink() {
        return "http://" + getContainerIpAddress() + ":" + getMappedPort(HTTP_PORT);
    }

    public String getNetworkWs() {
        return "ws://" + getAlias() + ":" + HTTP_PORT;
    }

    public String getlocalWs() {
        return "ws://" + getContainerIpAddress() + ":" + getMappedPort(HTTP_PORT);
    }

    public String getAlias() {
        if (Objects.nonNull(alias)) {
            return alias;
        }
        return getNetworkAliases().get(0);
    }
}
