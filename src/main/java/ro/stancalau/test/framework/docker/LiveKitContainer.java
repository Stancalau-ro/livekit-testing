package ro.stancalau.testframework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

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

    public static LiveKitContainer createContainer(String alias, Network network) {
        String logDirPath = "out/bdd/docker/livekit/" + alias;
        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        LiveKitContainer container = new LiveKitContainer("stancalau/livekit:v1.8.4", network)
                .withExposedPorts(HTTP_PORT)
                .withFileSystemBind(logDirRoot.getAbsolutePath(),
                        "/var/log", BindMode.READ_WRITE)
                .withEnv("TZ", ZoneId.systemDefault().toString())
                .withExtraHost("host.docker.internal", "host-gateway")
                .withNetwork(network)
                .withNetworkAliases(alias);

        container.alias = alias;

        return container;
    }

    public static LiveKitContainer createContainer(Network network) {
        return createContainer(DEFAULT_SERVER_ALIAS, network);
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
