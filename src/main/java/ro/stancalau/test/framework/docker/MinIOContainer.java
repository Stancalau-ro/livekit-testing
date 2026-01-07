package ro.stancalau.test.framework.docker;

import java.io.File;
import java.time.Duration;
import java.time.ZoneId;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

@Getter
@Slf4j
public class MinIOContainer extends GenericContainer<MinIOContainer> {

    public static final String DEFAULT_ACCESS_KEY = "minioadmin";
    public static final String DEFAULT_SECRET_KEY = "minioadmin";

    private static final String MINIO_IMAGE = "minio/minio:latest";
    private static final int API_PORT = 9000;
    private static final int CONSOLE_PORT = 9001;

    private final Network network;

    private final String alias;

    private final String accessKey;
    private final String secretKey;

    private MinIOContainer(
            Network network, String alias, String accessKey, String secretKey, @Nullable String logDestinationPath) {
        super(DockerImageName.parse(MINIO_IMAGE));
        this.network = network;
        this.alias = alias;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        String logDirPath = (logDestinationPath != null) ? logDestinationPath : "out/bdd/docker/minio/" + alias;

        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();

        addExposedPorts(API_PORT, CONSOLE_PORT);

        withCommand("server", "/data", "--console-address", ":" + CONSOLE_PORT);

        withEnv("MINIO_ROOT_USER", accessKey);
        withEnv("MINIO_ROOT_PASSWORD", secretKey);
        withEnv("TZ", ZoneId.systemDefault().toString());

        withNetwork(network);
        withNetworkAliases(alias);
        withExtraHost("host.docker.internal", "host-gateway");

        waitingFor(new HttpWaitStrategy()
                .forPath("/minio/health/live")
                .forPort(API_PORT)
                .withStartupTimeout(Duration.ofSeconds(60)));

        ContainerLogUtils.withLogCapture(this, logDirRoot, "minio.log");

        log.info("Created MinIOContainer with alias: {}, access key: {}", alias, accessKey);
    }

    public static MinIOContainer createContainer(String alias, Network network, String accessKey, String secretKey) {
        return createContainer(alias, network, accessKey, secretKey, null);
    }

    public static MinIOContainer createContainer(
            String alias, Network network, String accessKey, String secretKey, @Nullable String logDestinationPath) {
        return new MinIOContainer(network, alias, accessKey, secretKey, logDestinationPath);
    }

    public static MinIOContainer createContainer(String alias, Network network) {
        return createContainer(alias, network, DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY);
    }

    public String getS3EndpointUrl() {
        return "http://" + getHost() + ":" + getMappedPort(API_PORT);
    }

    public String getNetworkS3EndpointUrl() {
        return "http://" + alias + ":" + API_PORT;
    }
}
