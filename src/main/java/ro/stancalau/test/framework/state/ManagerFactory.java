package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating manager instances for BDD test scenarios. Each scenario gets its own
 * isolated set of managers to enable concurrent execution.
 */
@Slf4j
public class ManagerFactory {

  /**
   * Creates a complete set of managers for a test scenario. All managers are wired together with
   * proper dependencies.
   *
   * @return A new ManagerSet with all dependencies configured
   */
  public static ManagerSet createManagerSet() {
    log.debug("Creating new manager set for test scenario");

    ContainerStateManager containerManager = new ContainerStateManager();

    WebDriverStateManager webDriverManager = new WebDriverStateManager(containerManager);
    RoomClientStateManager roomClientManager = new RoomClientStateManager(containerManager);

    AccessTokenStateManager accessTokenManager = new AccessTokenStateManager();
    EgressStateManager egressStateManager = new EgressStateManager();
    IngressStateManager ingressStateManager = new IngressStateManager();
    ImageSnapshotStateManager imageSnapshotStateManager = new ImageSnapshotStateManager();

    MeetSessionStateManager meetSessionStateManager = new MeetSessionStateManager(webDriverManager);
    VideoQualityStateManager videoQualityStateManager =
        new VideoQualityStateManager(meetSessionStateManager);
    DataChannelStateManager dataChannelStateManager =
        new DataChannelStateManager(meetSessionStateManager);
    MetadataStateManager metadataStateManager =
        new MetadataStateManager(meetSessionStateManager, roomClientManager);

    return new ManagerSet(
        containerManager,
        webDriverManager,
        roomClientManager,
        accessTokenManager,
        egressStateManager,
        ingressStateManager,
        imageSnapshotStateManager,
        meetSessionStateManager,
        videoQualityStateManager,
        dataChannelStateManager,
        metadataStateManager);
  }

  public record ManagerSet(
      ContainerStateManager containerManager,
      WebDriverStateManager webDriverManager,
      RoomClientStateManager roomClientManager,
      AccessTokenStateManager accessTokenManager,
      EgressStateManager egressStateManager,
      IngressStateManager ingressStateManager,
      ImageSnapshotStateManager imageSnapshotStateManager,
      MeetSessionStateManager meetSessionStateManager,
      VideoQualityStateManager videoQualityStateManager,
      DataChannelStateManager dataChannelStateManager,
      MetadataStateManager metadataStateManager) {

    public void cleanup() {
      log.debug("Cleaning up manager set");

      try {
        dataChannelStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up DataChannelStateManager", e);
      }

      try {
        metadataStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up MetadataStateManager", e);
      }

      try {
        videoQualityStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up VideoQualityStateManager", e);
      }

      try {
        meetSessionStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up MeetSessionStateManager", e);
      }

      try {
        webDriverManager.closeAllWebDrivers();
      } catch (Exception e) {
        log.warn("Error cleaning up WebDriverManager", e);
      }

      try {
        roomClientManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up RoomClientManager", e);
      }

      try {
        accessTokenManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up AccessTokenManager", e);
      }

      try {
        egressStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up EgressStateManager", e);
      }

      try {
        ingressStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up IngressStateManager", e);
      }

      try {
        imageSnapshotStateManager.clearAll();
      } catch (Exception e) {
        log.warn("Error cleaning up ImageSnapshotStateManager", e);
      }

      try {
        containerManager.stopAllContainers();
        containerManager.closeNetwork();
      } catch (Exception e) {
        log.warn("Error cleaning up ContainerManager", e);
      }
    }
  }
}
