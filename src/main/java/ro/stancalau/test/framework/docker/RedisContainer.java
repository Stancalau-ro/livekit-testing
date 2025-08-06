package ro.stancalau.test.framework.docker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
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
    
    public static RedisContainer createContainer(String alias, Network network, String logDestinationPath) {
        String logDirPath = logDestinationPath != null 
            ? logDestinationPath
            : "out/bdd/scenarios/current/docker/" + alias;
        
        File logDirRoot = new File(logDirPath);
        logDirRoot.mkdirs();
        
        RedisContainer container = new RedisContainer("redis:7-alpine", network)
                .withExposedPorts(REDIS_PORT)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(30)));

        container = ContainerLogUtils.withLogCapture(container, logDirRoot, "redis.log");
        
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