package ro.stancalau.test.bdd.steps;

import ro.stancalau.test.framework.state.AccessTokenStateManager;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.state.DataChannelStateManager;
import ro.stancalau.test.framework.state.EgressStateManager;
import ro.stancalau.test.framework.state.ImageSnapshotStateManager;
import ro.stancalau.test.framework.state.IngressStateManager;
import ro.stancalau.test.framework.state.ManagerFactory;
import ro.stancalau.test.framework.state.MeetSessionStateManager;
import ro.stancalau.test.framework.state.MetadataStateManager;
import ro.stancalau.test.framework.state.RoomClientStateManager;
import ro.stancalau.test.framework.state.VideoQualityStateManager;
import ro.stancalau.test.framework.state.WebDriverStateManager;

/**
 * Utility class to provide clean access to scenario-specific managers. Provides both direct access
 * methods and convenient helper aliases.
 */
public class ManagerProvider {

  private static final ThreadLocal<ManagerFactory.ManagerSet> managerSet = new ThreadLocal<>();

  /** Initialize managers for the current scenario thread */
  public static void initializeManagers() {
    if (managerSet.get() == null) {
      ManagerFactory.ManagerSet managers = ManagerFactory.createManagerSet();
      managerSet.set(managers);
    }
  }

  /** Clean up managers for the current scenario thread */
  public static void cleanupManagers() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers != null) {
      managers.cleanup();
      managerSet.remove();
    }
  }

  /** Get the container manager for the current scenario */
  public static ContainerStateManager getContainerManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.containerManager();
  }

  /** Get the WebDriver manager for the current scenario */
  public static WebDriverStateManager getWebDriverManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.webDriverManager();
  }

  /** Get the room client manager for the current scenario */
  public static RoomClientStateManager getRoomClientManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.roomClientManager();
  }

  /** Get the access token manager for the current scenario */
  public static AccessTokenStateManager getAccessTokenManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.accessTokenManager();
  }

  /** Get the egress state manager for the current scenario */
  public static EgressStateManager getEgressStateManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.egressStateManager();
  }

  /** Get the image snapshot state manager for the current scenario */
  public static ImageSnapshotStateManager getImageSnapshotStateManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.imageSnapshotStateManager();
  }

  // Convenient shorter aliases for common operations
  public static ContainerStateManager containers() {
    return getContainerManager();
  }

  public static WebDriverStateManager webDrivers() {
    return getWebDriverManager();
  }

  public static RoomClientStateManager rooms() {
    return getRoomClientManager();
  }

  public static AccessTokenStateManager tokens() {
    return getAccessTokenManager();
  }

  public static EgressStateManager egress() {
    return getEgressStateManager();
  }

  public static IngressStateManager getIngressStateManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.ingressStateManager();
  }

  public static IngressStateManager ingress() {
    return getIngressStateManager();
  }

  public static ImageSnapshotStateManager snapshots() {
    return getImageSnapshotStateManager();
  }

  public static MeetSessionStateManager getMeetSessionManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.meetSessionStateManager();
  }

  public static VideoQualityStateManager getVideoQualityManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.videoQualityStateManager();
  }

  public static DataChannelStateManager getDataChannelManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.dataChannelStateManager();
  }

  public static MeetSessionStateManager meetSessions() {
    return getMeetSessionManager();
  }

  public static VideoQualityStateManager videoQuality() {
    return getVideoQualityManager();
  }

  public static DataChannelStateManager dataChannel() {
    return getDataChannelManager();
  }

  public static MetadataStateManager getMetadataManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
      throw new IllegalStateException(
          "Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.metadataStateManager();
  }

  public static MetadataStateManager metadata() {
    return getMetadataManager();
  }
}
