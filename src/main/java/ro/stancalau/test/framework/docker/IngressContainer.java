package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.util.PathUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;

@Slf4j
public class IngressContainer extends GenericContainer<IngressContainer> {

    public static final int RTMP_PORT = 1935;
    public static final int WHIP_PORT = 8080;
    public static final int HEALTH_PORT = 8081;

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
        container = configureIngressConfig(container, apiKey, apiSecret, livekitWsUrl, redisUrl);
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

    private static IngressContainer configureIngressConfig(
            IngressContainer container,
            String apiKey,
            String apiSecret,
            String wsUrl,
            String redisUrl) {
        String config = createIngressConfigBody(apiKey, apiSecret, wsUrl, redisUrl);
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
                .withStartupTimeout(Duration.ofSeconds(120)));
    }

    private static String createIngressConfigBody(String apiKey, String apiSecret, String wsUrl, String redisUrl) {
        return String.format(
            """
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
            cpu_cost:
              rtmp_cpu_cost: 0.1
              whip_cpu_cost: 0.1
              whip_bypass_transcoding_cpu_cost: 0.01
              url_cpu_cost: 0.1
            """,
            apiKey, apiSecret, wsUrl, redisUrl,
            RTMP_PORT, WHIP_PORT, HEALTH_PORT);
    }

    public String getAlias() {
        return alias != null ? alias : getNetworkAliases().getFirst();
    }

    public String getRtmpUrl() {
        return String.format("rtmp://localhost:%d/live", getMappedPort(RTMP_PORT));
    }

    public String getWhipUrl() {
        return String.format("http://localhost:%d/whip", getMappedPort(WHIP_PORT));
    }

    public String getNetworkRtmpUrl() {
        return String.format("rtmp://%s:%d/live", getAlias(), RTMP_PORT);
    }

    public String getNetworkWhipUrl() {
        return String.format("http://%s:%d/whip", getAlias(), WHIP_PORT);
    }
}
