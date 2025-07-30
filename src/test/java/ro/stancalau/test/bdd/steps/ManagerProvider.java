package ro.stancalau.test.bdd.steps;

import ro.stancalau.test.framework.state.AccessTokenStateManager;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.state.ManagerFactory;
import ro.stancalau.test.framework.state.RoomClientStateManager;
import ro.stancalau.test.framework.state.WebDriverStateManager;

/**
 * Utility class to provide clean access to scenario-specific managers.
 * Provides both direct access methods and convenient helper aliases.
 */
public class ManagerProvider {
    
    private static final ThreadLocal<ManagerFactory.ManagerSet> managerSet = new ThreadLocal<>();
    
    /**
     * Initialize managers for the current scenario thread
     */
    public static void initializeManagers() {
        if (managerSet.get() == null) {
            ManagerFactory.ManagerSet managers = ManagerFactory.createManagerSet();
            managerSet.set(managers);
        }
    }
    
    /**
     * Clean up managers for the current scenario thread
     */
    public static void cleanupManagers() {
        ManagerFactory.ManagerSet managers = managerSet.get();
        if (managers != null) {
            managers.cleanup();
            managerSet.remove();
        }
    }
    
    /**
     * Get the container manager for the current scenario
     */
    public static ContainerStateManager getContainerManager() {
        ManagerFactory.ManagerSet managers = managerSet.get();
        if (managers == null) {
            throw new IllegalStateException("Managers not initialized. Ensure BaseSteps @Before hook ran.");
        }
        return managers.getContainerManager();
    }
    
    /**
     * Get the WebDriver manager for the current scenario  
     */
    public static WebDriverStateManager getWebDriverManager() {
        ManagerFactory.ManagerSet managers = managerSet.get();
        if (managers == null) {
            throw new IllegalStateException("Managers not initialized. Ensure BaseSteps @Before hook ran.");
        }
        return managers.getWebDriverManager();
    }
    
    /**
     * Get the room client manager for the current scenario
     */
    public static RoomClientStateManager getRoomClientManager() {
        ManagerFactory.ManagerSet managers = managerSet.get();
        if (managers == null) {
            throw new IllegalStateException("Managers not initialized. Ensure BaseSteps @Before hook ran.");
        }
        return managers.getRoomClientManager();
    }
    
    /**
     * Get the access token manager for the current scenario
     */
    public static AccessTokenStateManager getAccessTokenManager() {
        ManagerFactory.ManagerSet managers = managerSet.get();
        if (managers == null) {
            throw new IllegalStateException("Managers not initialized. Ensure BaseSteps @Before hook ran.");
        }
        return managers.getAccessTokenManager();
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
}