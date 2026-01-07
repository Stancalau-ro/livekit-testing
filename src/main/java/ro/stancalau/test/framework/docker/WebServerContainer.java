package ro.stancalau.test.framework.docker;

import java.io.File;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * TestContainers wrapper for a simple web server to serve static files Uses nginx to serve files
 * from the local filesystem to containerized browsers
 */
@Slf4j
public class WebServerContainer extends GenericContainer<WebServerContainer> {

    private static final String NGINX_IMAGE = "nginx:alpine";
    private static final int HTTP_PORT = 80;
    private static final String CONTAINER_WEB_ROOT = "/usr/share/nginx/html";

    public WebServerContainer() {
        super(DockerImageName.parse(NGINX_IMAGE));

        // Expose HTTP port for testing
        addExposedPort(HTTP_PORT);

        waitingFor(new HttpWaitStrategy().forPath("/").forPort(HTTP_PORT).withStartupTimeout(Duration.ofSeconds(30)));

        log.info("Created WebServerContainer with nginx:alpine and HTTP support");
    }

    /**
     * Add static files from a local directory to be served by the web server
     *
     * @param localPath Local directory or file path
     * @param containerPath Path in the web server where files should be accessible
     * @return this container for method chaining
     */
    public WebServerContainer withStaticFiles(String localPath, String containerPath) {
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            throw new RuntimeException("Local path does not exist: " + localPath);
        }

        String targetPath = CONTAINER_WEB_ROOT + containerPath;
        withCopyFileToContainer(MountableFile.forHostPath(localPath), targetPath);

        log.info("Added static files: {} -> {}", localPath, targetPath);
        return this;
    }

    public WebServerContainer withLiveKitMeetFiles() {
        String meetHtmlPath = "src/main/resources/web/livekit-meet";
        File meetDir = new File(meetHtmlPath);

        if (!meetDir.exists()) {
            throw new RuntimeException("LiveKit Meet directory not found: " + meetHtmlPath);
        }

        withCopyFileToContainer(MountableFile.forHostPath(meetHtmlPath), CONTAINER_WEB_ROOT);

        log.info("Added LiveKit Meet files from: {}", meetHtmlPath);
        return this;
    }

    /**
     * Get the URL to access the web server from other containers in the same network
     *
     * @param networkAlias The network alias for this container
     * @return The base URL for accessing the web server
     */
    public String getNetworkUrl(String networkAlias) {
        return "http://" + networkAlias + ":" + HTTP_PORT;
    }

    /**
     * Get the URL to access the web server from the host machine
     *
     * @return The base URL for accessing the web server from host
     */
    public String getHostUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    /**
     * Get the URL for the LiveKit Meet page
     *
     * @param networkAlias The network alias for this container
     * @return The URL to the LiveKit Meet page
     */
    public String getLiveKitMeetUrl(String networkAlias) {
        return getNetworkUrl(networkAlias) + "/index.html";
    }

    /**
     * Set the network alias and start the container
     *
     * @param network The Docker network to join
     * @param alias The network alias for this container
     * @return this container for method chaining
     */
    public WebServerContainer withNetworkAliasAndStart(Network network, String alias) {
        withNetwork(network);
        withNetworkAliases(alias);
        start();

        log.info("Started WebServerContainer with network alias: {} - URL: {}", alias, getNetworkUrl(alias));
        return this;
    }
}
