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
        EgressStateManager egressStateManager = new EgressStateManager();
        
        return new ManagerSet(
            containerManager,
            webDriverManager, 
            roomClientManager,
            accessTokenManager,
            egressStateManager
        );
    }

    /**
         * Container class that holds all manager instances for a test scenario.
         * Provides easy access to all managers with proper type safety.
         */
        public record ManagerSet(ContainerStateManager containerManager, WebDriverStateManager webDriverManager,
                                 RoomClientStateManager roomClientManager, AccessTokenStateManager accessTokenManager,
                                 EgressStateManager egressStateManager) {

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
                    egressStateManager.clearAll();
                } catch (Exception e) {
                    log.warn("Error cleaning up EgressStateManager", e);
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