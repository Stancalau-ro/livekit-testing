package ro.stancalau.test.framework.state;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.MinIOContainer;
import ro.stancalau.test.framework.docker.WebServerContainer;

@Slf4j
public class ContainerStateManager {

  private Network network;
  private final Map<String, GenericContainer<?>> containers = new HashMap<>();

  public ContainerStateManager() {
    // Public constructor for dependency injection
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
  public <T extends GenericContainer<T>> T getContainer(
      String serviceName, Class<T> containerType) {
    GenericContainer<?> container = containers.get(serviceName);
    if (containerType.isInstance(container)) {
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

  @SuppressWarnings("unchecked")
  public <T extends GenericContainer<T>> Map.Entry<String, T> getFirstContainerOfType(
      Class<T> containerType) {
    for (Map.Entry<String, GenericContainer<?>> entry : containers.entrySet()) {
      if (containerType.isInstance(entry.getValue())) {
        return Map.entry(entry.getKey(), (T) entry.getValue());
      }
    }
    return null;
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

  public void cleanup(Class<? extends GenericContainer> clazz) {
    log.info("Cleaning up all containers of type: {}", clazz.getName());

    // First pass: stop and collect services to remove
    Map<String, GenericContainer<?>> toRemove = new HashMap<>();
    for (Map.Entry<String, GenericContainer<?>> entry : containers.entrySet()) {
      String serviceName = entry.getKey();
      GenericContainer<?> container = entry.getValue();
      if (clazz.isInstance(container)) {
        if (container.isRunning()) {
          log.info("Stopping container for service: {}", serviceName);
          container.stop();
        }
        toRemove.put(serviceName, container);
      }
    }

    // Second pass: remove only the stopped containers of the specified type
    for (String serviceName : toRemove.keySet()) {
      containers.remove(serviceName);
      log.info("Removed container from registry: {}", serviceName);
    }
  }

  /**
   * Get or create a web server container for serving static files
   *
   * @param serviceName The service name for the web server
   * @return The web server container
   */
  public WebServerContainer getOrCreateWebServer(String serviceName) {
    WebServerContainer webServer = getContainer(serviceName, WebServerContainer.class);

    if (webServer == null) {
      log.info("Creating new WebServerContainer for service: {}", serviceName);
      webServer =
          new WebServerContainer()
              .withLiveKitMeetFiles()
              .withNetworkAliasAndStart(getOrCreateNetwork(), serviceName);

      registerContainer(serviceName, webServer);
    }

    return webServer;
  }

  /**
   * Get or create a MinIO container for S3-compatible object storage
   *
   * @param serviceName The service name for the MinIO server
   * @return The MinIO container
   */
  public MinIOContainer getOrCreateMinIO(String serviceName) {
    MinIOContainer minio = getContainer(serviceName, MinIOContainer.class);

    if (minio == null) {
      log.info("Creating new MinIOContainer for service: {}", serviceName);
      minio = MinIOContainer.createContainer(serviceName, getOrCreateNetwork());
      minio.start();

      registerContainer(serviceName, minio);
    }

    return minio;
  }

  /**
   * Get or create a MinIO container with custom credentials
   *
   * @param serviceName The service name for the MinIO server
   * @param accessKey The access key for MinIO
   * @param secretKey The secret key for MinIO
   * @return The MinIO container
   */
  public MinIOContainer getOrCreateMinIO(String serviceName, String accessKey, String secretKey) {
    MinIOContainer minio = getContainer(serviceName, MinIOContainer.class);

    if (minio == null) {
      log.info("Creating new MinIOContainer for service: {} with custom credentials", serviceName);
      minio =
          MinIOContainer.createContainer(serviceName, getOrCreateNetwork(), accessKey, secretKey);
      minio.start();

      registerContainer(serviceName, minio);
    }

    return minio;
  }
}
