package ro.stancalau.test.framework.factory;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.util.TestConfig;

import javax.annotation.Nullable;

/**
 * Factory for creating LiveKit containers with configurable versions.
 * This factory is responsible for version selection and delegates 
 * container creation to LiveKitContainer which handles the container lifecycle.
 */
@Slf4j
@UtilityClass
public class LiveKitContainerFactory {

    /**
     * Creates a LiveKit container with the configured version
     */
    public static LiveKitContainer createContainer(String alias, Network network, String livekitVersion, @Nullable String configFilePath) {
        log.debug("Creating LiveKit container '{}' with version '{}' and config '{}'", alias, livekitVersion, configFilePath);
        return LiveKitContainer.createContainer(alias, network, livekitVersion, configFilePath);
    }

    /**
     * Creates a LiveKit container using the version from TestConfig
     */
    public static LiveKitContainer createContainer(String alias, Network network, @Nullable String configFilePath) {
        String version = TestConfig.getLiveKitVersion();
        log.info("Creating LiveKit container '{}' with configured version '{}'", alias, version);
        return createContainer(alias, network, version, configFilePath);
    }

    /**
     * Creates a LiveKit container using the version from TestConfig
     */
    public static LiveKitContainer createContainer(String alias, Network network) {
        return createContainer(alias, network, null);
    }

    /**
     * Creates a LiveKit container with default alias using the version from TestConfig
     */
    public static LiveKitContainer createContainer(Network network) {
        String version = TestConfig.getLiveKitVersion();
        log.info("Creating default LiveKit container with configured version '{}'", version);
        return LiveKitContainer.createContainer(network, null, version);
    }

    /**
     * Creates a LiveKit container with default alias and config using the version from TestConfig
     */
    public static LiveKitContainer createContainer(Network network, @Nullable String configFilePath) {
        String version = TestConfig.getLiveKitVersion();
        log.info("Creating default LiveKit container with configured version '{}' and config '{}'", version, configFilePath);
        return LiveKitContainer.createContainer(network, configFilePath, version);
    }

    /**
     * Creates a LiveKit container for BDD tests with explicit version and configuration
     */
    public static LiveKitContainer createBddContainer(String alias, Network network, @Nullable String configFilePath) {
        String version = TestConfig.getLiveKitVersion();
        log.info("Creating BDD LiveKit container '{}' with version '{}' and config '{}'", alias, version, configFilePath);
        return createContainer(alias, network, version, configFilePath);
    }

    /**
     * Creates a LiveKit container for integration tests with explicit version
     */
    public static LiveKitContainer createIntegrationTestContainer(String alias, Network network) {
        String version = TestConfig.getLiveKitVersion();
        String configPath = "src/test/resources/livekit/config/config.yaml";
        log.info("Creating integration test LiveKit container '{}' with version '{}' and config '{}'", alias, version, configPath);
        return createContainer(alias, network, version, configPath);
    }

    /**
     * Creates a LiveKit container for integration tests without config file
     */
    public static LiveKitContainer createIntegrationTestContainerWithoutConfig(String alias, Network network) {
        String version = TestConfig.getLiveKitVersion();
        log.info("Creating integration test LiveKit container '{}' with version '{}' (no config)", alias, version);
        return createContainer(alias, network, version, null);
    }
}