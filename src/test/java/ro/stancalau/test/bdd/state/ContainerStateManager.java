package ro.stancalau.test.bdd.state;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ContainerStateManager {
    
    private static ContainerStateManager instance;
    private Network network;
    private final Map<String, GenericContainer<?>> containers = new HashMap<>();
    
    private ContainerStateManager() {
        // Private constructor for singleton
    }
    
    public static ContainerStateManager getInstance() {
        if (instance == null) {
            instance = new ContainerStateManager();
        }
        return instance;
    }
    
    public Network getOrCreateNetwork() {
        if (network == null) {
            log.info("Creating new Docker network for BDD tests");
            network = Network.newNetwork();
        }
        return network;
    }
    
    public void registerContainer(String serviceName, GenericContainer<?> container) {
        log.info("Registering container for service: {}", serviceName);
        containers.put(serviceName, container);
    }
    
    public GenericContainer<?> getContainer(String serviceName) {
        return containers.get(serviceName);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends GenericContainer<T>> T getContainer(String serviceName, Class<T> containerType) {
        GenericContainer<?> container = containers.get(serviceName);
        if (container != null && containerType.isInstance(container)) {
            return (T) container;
        }
        return null;
    }
    
    public boolean hasContainer(String serviceName) {
        return containers.containsKey(serviceName);
    }
    
    public boolean isContainerRunning(String serviceName) {
        GenericContainer<?> container = containers.get(serviceName);
        return container != null && container.isRunning();
    }
    
    public void stopContainer(String serviceName) {
        GenericContainer<?> container = containers.get(serviceName);
        if (container != null && container.isRunning()) {
            log.info("Stopping container for service: {}", serviceName);
            container.stop();
        }
    }
    
    public void stopAllContainers() {
        log.info("Stopping all containers");
        for (Map.Entry<String, GenericContainer<?>> entry : containers.entrySet()) {
            String serviceName = entry.getKey();
            GenericContainer<?> container = entry.getValue();
            if (container.isRunning()) {
                log.info("Stopping container for service: {}", serviceName);
                container.stop();
            }
        }
        containers.clear();
    }
    
    public void closeNetwork() {
        if (network != null) {
            log.info("Closing Docker network");
            network.close();
            network = null;
        }
    }
    
    public void cleanup() {
        log.info("Cleaning up all containers and network");
        stopAllContainers();
        closeNetwork();
    }
    
    public static void reset() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
}