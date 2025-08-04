package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@Slf4j
public class RedisContainer extends GenericContainer<RedisContainer> {
    
    public static final int REDIS_PORT = 6379;
    
    @Getter
    private final Network network;
    private String alias;
    
    private RedisContainer(String imageName, Network network) {
        super(imageName);
        this.network = network;
    }
    
    public static RedisContainer createContainer(String alias, Network network) {
        return createContainer(alias, network, null);
    }
    
    public static RedisContainer createContainer(String alias, Network network, String logDestinationPath) {
        // Create log directory 
        String logDirPath = logDestinationPath != null 
            ? logDestinationPath
            : "out/bdd/scenarios/current/docker/" + alias;
        
        java.io.File logDirRoot = new java.io.File(logDirPath);
        logDirRoot.mkdirs();
        
        RedisContainer container = new RedisContainer("redis:7-alpine", network)
                .withExposedPorts(REDIS_PORT)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withFileSystemBind(logDirRoot.getAbsolutePath(), "/var/log/redis", org.testcontainers.containers.BindMode.READ_WRITE)
                .withLogConsumer(outputFrame -> {
                    try {
                        java.io.File logFile = new java.io.File(logDirRoot, "redis.log");
                        java.nio.file.Files.write(logFile.toPath(), 
                            (outputFrame.getUtf8String()).getBytes(), 
                            java.nio.file.StandardOpenOption.CREATE, 
                            java.nio.file.StandardOpenOption.APPEND);
                    } catch (Exception e) {
                        log.warn("Failed to write Redis log: {}", e.getMessage());
                    }
                })
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(30)));
        
        container.alias = alias;
        
        return container;
    }
    
    public String getRedisUrl() {
        return getContainerIpAddress() + ":" + getMappedPort(REDIS_PORT);
    }
    
    public String getNetworkRedisUrl() {
        return getAlias() + ":" + REDIS_PORT;
    }
    
    public String getAlias() {
        return alias != null ? alias : getNetworkAliases().get(0);
    }
}