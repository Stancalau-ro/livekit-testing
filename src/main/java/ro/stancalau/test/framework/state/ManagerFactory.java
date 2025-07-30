package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating manager instances for BDD test scenarios.
 * Each scenario gets its own isolated set of managers to enable concurrent execution.
 */
@Slf4j
public class ManagerFactory {
    
    /**
     * Creates a complete set of managers for a test scenario.
     * All managers are wired together with proper dependencies.
     * 
     * @return A new ManagerSet with all dependencies configured
     */
    public static ManagerSet createManagerSet() {
        log.debug("Creating new manager set for test scenario");
        
        // Create the container manager first (base dependency)
        ContainerStateManager containerManager = new ContainerStateManager();
        
        // Create managers that depend on container manager
        WebDriverStateManager webDriverManager = new WebDriverStateManager(containerManager);
        RoomClientStateManager roomClientManager = new RoomClientStateManager(containerManager);
        
        // Create independent managers
        AccessTokenStateManager accessTokenManager = new AccessTokenStateManager();
        
        return new ManagerSet(
            containerManager,
            webDriverManager, 
            roomClientManager,
            accessTokenManager
        );
    }
    
    /**
     * Container class that holds all manager instances for a test scenario.
     * Provides easy access to all managers with proper type safety.
     */
    public static class ManagerSet {
        private final ContainerStateManager containerManager;
        private final WebDriverStateManager webDriverManager;
        private final RoomClientStateManager roomClientManager;
        private final AccessTokenStateManager accessTokenManager;
        
        public ManagerSet(ContainerStateManager containerManager,
                         WebDriverStateManager webDriverManager,
                         RoomClientStateManager roomClientManager,
                         AccessTokenStateManager accessTokenManager) {
            this.containerManager = containerManager;
            this.webDriverManager = webDriverManager;
            this.roomClientManager = roomClientManager;
            this.accessTokenManager = accessTokenManager;
        }
        
        public ContainerStateManager getContainerManager() {
            return containerManager;
        }
        
        public WebDriverStateManager getWebDriverManager() {
            return webDriverManager;
        }
        
        public RoomClientStateManager getRoomClientManager() {
            return roomClientManager;
        }
        
        public AccessTokenStateManager getAccessTokenManager() {
            return accessTokenManager;
        }
        
        /**
         * Cleanup all managers in proper order to release resources.
         * Call this at the end of each test scenario.
         */
        public void cleanup() {
            log.debug("Cleaning up manager set");
            
            // Cleanup in reverse dependency order
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
                containerManager.stopAllContainers();
                containerManager.closeNetwork();
            } catch (Exception e) {
                log.warn("Error cleaning up ContainerManager", e);
            }
        }
    }
}