package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.VncRecordingContainer;
import org.testcontainers.lifecycle.TestDescription;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.selenium.SeleniumConfig;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class WebDriverStateManager {
    
    private final Map<String, WebDriver> webDrivers = new HashMap<>();
    private final Map<String, BrowserWebDriverContainer<?>> browserContainers = new HashMap<>();
    private final Map<String, TestDescription> testDescriptions = new HashMap<>();
    private final Map<String, Boolean> testResults = new HashMap<>();
    private final ContainerStateManager containerStateManager;
    private String currentScenarioRecordingPath;
    
    public WebDriverStateManager(ContainerStateManager containerStateManager) {
        this.containerStateManager = containerStateManager;
    }
    
    /**
     * Set the scenario-specific recording path for all subsequent WebDriver creations
     */
    public void setScenarioRecordingPath(String scenarioPath) {
        this.currentScenarioRecordingPath = scenarioPath + "/vnc-recordings";
        log.info("Set scenario recording path to: {}", this.currentScenarioRecordingPath);
    }
    
    /**
     * Get the current recording directory (scenario-specific), creating it lazily if needed
     */
    private File getCurrentRecordingDir() {
        if (currentScenarioRecordingPath == null) {
            throw new IllegalStateException("Scenario recording path must be set before creating WebDrivers");
        }
        File recDir = new File(currentScenarioRecordingPath);
        if (!recDir.exists()) {
            recDir.mkdirs();
            log.debug("Created recording directory: {}", recDir.getAbsolutePath());
        }
        return recDir;
    }
    
    
    /**
     * Create and register a WebDriver instance using BrowserWebDriverContainer
     * @param purpose The purpose of the WebDriver (e.g., "publish", "playback", "meet")
     * @param actor The actor/participant name (e.g., "Publisher", "Jack")
     * @param browser The browser type (e.g., "chrome", "firefox")
     * @return The created WebDriver instance
     */
    public WebDriver createWebDriver(String purpose, String actor, String browser) {
        String key = generateKey(purpose, actor);
        
        if (webDrivers.containsKey(key)) {
            log.warn("WebDriver already exists for key: {}, returning existing instance", key);
            return webDrivers.get(key);
        }
        
        log.info("Creating containerized WebDriver for purpose: {}, actor: {}, browser: {}", purpose, actor, browser);
        
        BrowserWebDriverContainer<?> browserContainer = createBrowserContainer(key, browser);
        WebDriver driver = browserContainer.getWebDriver();
        
        TestDescription testDescription = createTestDescription(key);
        testDescriptions.put(key, testDescription);
        
        if (TestConfig.isRecordingEnabled()) {
            browserContainer.beforeTest(testDescription);
            log.info("Started recording for WebDriver key: {}", key);
        }
        
        testResults.put(key, true);
        
        webDrivers.put(key, driver);
        browserContainers.put(key, browserContainer);
        log.debug("Registered containerized WebDriver with key: {}", key);

        grantDisplayCapturePermission(driver);

        return driver;
    }

    private void grantDisplayCapturePermission(WebDriver driver) {
        try {
            if (driver instanceof RemoteWebDriver remoteDriver) {
                WebDriver augmentedDriver = new Augmenter().augment(remoteDriver);
                if (augmentedDriver instanceof HasCdp cdpDriver) {
                    Map<String, Object> permission = new HashMap<>();
                    Map<String, Object> permissionDescriptor = new HashMap<>();
                    permissionDescriptor.put("name", "display-capture");

                    permission.put("permission", permissionDescriptor);
                    permission.put("setting", "granted");

                    cdpDriver.executeCdpCommand("Browser.setPermission", permission);
                    log.info("Granted display-capture permission via CDP");
                } else {
                    log.warn("WebDriver does not support CDP after augmentation");
                }
            }
        } catch (Exception e) {
            log.warn("Could not grant display-capture permission via CDP: {}", e.getMessage());
        }
    }
    
    /**
     * Create a new browser container based on the pattern provided
     */
    private BrowserWebDriverContainer<?> createBrowserContainer(String name, String browser) {
        Network network = containerStateManager.getOrCreateNetwork();
        String insecureUrl = "http://host.docker.internal:*,http://webserver";
        
        BrowserWebDriverContainer<?> browserContainer = switch (browser.toLowerCase()) {
            case "firefox" -> new BrowserWebDriverContainer<>()
                    .withCapabilities(SeleniumConfig.getFirefoxOptions())
                    .withNetwork(network)
                    .withEnv("MOZ_FAKE_MEDIA_STREAMS", "1");
            case "edge" -> new BrowserWebDriverContainer<>()
                    .withCapabilities(SeleniumConfig.getEdgeOptions(insecureUrl))
                    .withNetwork(network);
            default -> new BrowserWebDriverContainer<>()
                    .withCapabilities(SeleniumConfig.getChromeOptions(insecureUrl))
                    .withNetwork(network);
        };

        String recordingMode = TestConfig.getRecordingMode();
        if (TestConfig.isRecordingEnabled()) {
            log.info("Creating browser container with VNC recording enabled (mode: {})", recordingMode);
            File currentRecDir = getCurrentRecordingDir();
            log.info("Recordings will be saved to: {}", currentRecDir.getAbsolutePath());
            
            BrowserWebDriverContainer.VncRecordingMode vncMode;
            if (TestConfig.isRecordOnlyFailed()) {
                vncMode = BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING;
            } else {
                vncMode = BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;
            }
            
            browserContainer = browserContainer.withRecordingMode(vncMode, getCurrentRecordingDir(), VncRecordingContainer.VncRecordingFormat.MP4);
        } else {
            log.info("Creating browser container with VNC recording disabled (mode: {})", recordingMode);
        }
        
        browserContainer.start();
        browserContainer.setNetworkAliases(List.of(name));
        
        log.info("Started browser container with alias: {} - VNC URL: {}", name, browserContainer.getVncAddress());
        if (TestConfig.isRecordingEnabled()) {
            File currentRecDir = getCurrentRecordingDir();
            log.info("Recording directory permissions: readable={}, writable={}", 
                    currentRecDir.canRead(), currentRecDir.canWrite());
        }
        
        return browserContainer;
    }
    
    /**
     * Create a TestDescription for recording lifecycle
     */
    private TestDescription createTestDescription(String key) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return "webdriver-" + key;
            }

            @Override
            public String getFilesystemFriendlyName() {
                return "webdriver-" + key.replace(":", "-");
            }
        };
    }
    
    /**
     * Get an existing WebDriver instance
     * @param purpose The purpose of the WebDriver
     * @param actor The actor/participant name
     * @return The WebDriver instance or null if not found
     */
    public WebDriver getWebDriver(String purpose, String actor) {
        String key = generateKey(purpose, actor);
        WebDriver driver = webDrivers.get(key);
        
        if (driver == null) {
            log.warn("No WebDriver found for key: {}", key);
        }
        
        return driver;
    }
    
    /**
     * Get or create a WebDriver instance
     * @param purpose The purpose of the WebDriver
     * @param actor The actor/participant name
     * @param browser The browser type (used only if creating new)
     * @return The WebDriver instance
     */
    public WebDriver getOrCreateWebDriver(String purpose, String actor, String browser) {
        WebDriver driver = getWebDriver(purpose, actor);
        if (driver == null) {
            driver = createWebDriver(purpose, actor, browser);
        }
        return driver;
    }
    
    /**
     * Check if a WebDriver exists for the given parameters
     * @param purpose The purpose of the WebDriver
     * @param actor The actor/participant name
     * @return true if WebDriver exists, false otherwise
     */
    public boolean hasWebDriver(String purpose, String actor) {
        String key = generateKey(purpose, actor);
        return webDrivers.containsKey(key);
    }
    
    /**
     * Mark a WebDriver test as failed for proper recording naming
     * @param purpose The purpose of the WebDriver
     * @param actor The actor/participant name
     */
    public void markTestFailed(String purpose, String actor) {
        String key = generateKey(purpose, actor);
        testResults.put(key, false);
        log.debug("Marked test as FAILED for WebDriver key: {}", key);
    }
    
    /**
     * Mark a WebDriver test as passed
     * @param purpose The purpose of the WebDriver  
     * @param actor The actor/participant name
     */
    public void markTestPassed(String purpose, String actor) {
        String key = generateKey(purpose, actor);
        testResults.put(key, true);
        log.debug("Marked test as PASSED for WebDriver key: {}", key);
    }
    
    /**
     * Close and remove a specific WebDriver and its container
     * @param purpose The purpose of the WebDriver
     * @param actor The actor/participant name
     */
    public void closeWebDriver(String purpose, String actor) {
        String key = generateKey(purpose, actor);
        WebDriver driver = webDrivers.remove(key);
        BrowserWebDriverContainer<?> container = browserContainers.remove(key);
        TestDescription testDescription = testDescriptions.remove(key);
        Boolean testPassed = testResults.remove(key);
        
        if (driver != null || container != null) {
            log.info("Closing WebDriver and container for purpose: {}, actor: {}", purpose, actor);
            
            if (driver != null) {
                try {
                    driver.quit();
                    log.debug("Successfully closed WebDriver with key: {}", key);
                } catch (Exception e) {
                    log.warn("Error closing WebDriver with key: {}: {}", key, e.getMessage());
                }
            }
            
            if (container != null) {
                try {
                    if (testDescription != null && TestConfig.isRecordingEnabled()) {
                        Optional<Throwable> testFailure = Optional.empty();
                        if (testPassed != null && !testPassed) {
                            testFailure = Optional.of(new AssertionError("BDD test step failed"));
                        }
                        
                        container.afterTest(testDescription, testFailure);
                        String result = testPassed != null && testPassed ? "PASSED" : "FAILED";
                        log.info("Finalized recording for WebDriver key: {} (result: {})", key, result);
                        
                        waitForRecordingFile(testDescription, testPassed);
                    }
                    
                    container.stop();
                    log.debug("Successfully stopped browser container with key: {}", key);
                } catch (Exception e) {
                    log.warn("Error stopping browser container with key: {}: {}", key, e.getMessage());
                }
            }
        } else {
            log.warn("No WebDriver or container found to close for key: {}", key);
        }
    }
    
    /**
     * Close all WebDrivers for a specific purpose
     * @param purpose The purpose
     */
    public void closeWebDriversForPurpose(String purpose) {
        log.info("Closing all WebDrivers for purpose: {}", purpose);
        
        webDrivers.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (key.startsWith(purpose + ":")) {
                WebDriver driver = entry.getValue();
                try {
                    driver.quit();
                    log.debug("Closed WebDriver with key: {}", key);
                    return true;
                } catch (Exception e) {
                    log.warn("Error closing WebDriver with key: {}: {}", key, e.getMessage());
                    return true;
                }
            }
            return false;
        });
        
        browserContainers.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (key.startsWith(purpose + ":")) {
                BrowserWebDriverContainer<?> container = entry.getValue();
                try {
                    Thread.sleep(1000);
                    container.stop();
                    log.debug("Stopped browser container with key: {}", key);
                    return true;
                } catch (Exception e) {
                    log.warn("Error stopping browser container with key: {}: {}", key, e.getMessage());
                    return true;
                }
            }
            return false;
        });
    }
    
    /**
     * Close all WebDrivers for a specific actor
     * @param actor The actor/participant name
     */
    public void closeWebDriversForActor(String actor) {
        log.info("Closing all WebDrivers for actor: {}", actor);
        
        // Close WebDrivers
        webDrivers.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            String[] parts = key.split(":");
            if (parts.length >= 2 && parts[1].equals(actor)) {
                WebDriver driver = entry.getValue();
                try {
                    driver.quit();
                    log.debug("Closed WebDriver with key: {}", key);
                    return true;
                } catch (Exception e) {
                    log.warn("Error closing WebDriver with key: {}: {}", key, e.getMessage());
                    return true;
                }
            }
            return false;
        });
        
        browserContainers.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            String[] parts = key.split(":");
            if (parts.length >= 2 && parts[1].equals(actor)) {
                BrowserWebDriverContainer<?> container = entry.getValue();
                try {
                    Thread.sleep(1000);
                    container.stop();
                    log.debug("Stopped browser container with key: {}", key);
                    return true;
                } catch (Exception e) {
                    log.warn("Error stopping browser container with key: {}: {}", key, e.getMessage());
                    return true;
                }
            }
            return false;
        });
    }
    
    /**
     * Close all WebDrivers and browser containers
     */
    public void closeAllWebDrivers() {
        log.info("Closing all WebDrivers and containers, count: {}", webDrivers.size());
        
        for (Map.Entry<String, WebDriver> entry : webDrivers.entrySet()) {
            String key = entry.getKey();
            WebDriver driver = entry.getValue();
            try {
                driver.quit();
                log.debug("Closed WebDriver with key: {}", key);
            } catch (Exception e) {
                log.warn("Error closing WebDriver with key: {}: {}", key, e.getMessage());
            }
        }
        webDrivers.clear();
        
        for (Map.Entry<String, BrowserWebDriverContainer<?>> entry : browserContainers.entrySet()) {
            String key = entry.getKey();
            BrowserWebDriverContainer<?> container = entry.getValue();
            TestDescription testDescription = testDescriptions.get(key);
            
            try {
                if (testDescription != null && TestConfig.isRecordingEnabled()) {
                    Boolean testPassed = testResults.get(key);
                    Optional<Throwable> testFailure = Optional.empty();
                    if (testPassed != null && !testPassed) {
                        testFailure = Optional.of(new AssertionError("BDD test step failed"));
                    }
                    
                    container.afterTest(testDescription, testFailure);
                    String result = testPassed != null && testPassed ? "PASSED" : "FAILED";
                    log.info("Finalized recording for WebDriver key: {} (result: {})", key, result);
                    
                    waitForRecordingFile(testDescription, testPassed);
                }
                container.stop();
                log.debug("Stopped browser container with key: {}", key);
            } catch (Exception e) {
                log.warn("Error stopping browser container with key: {}: {}", key, e.getMessage());
            }
        }
        browserContainers.clear();
        testDescriptions.clear();
        testResults.clear();
    }
    
    /**
     * Get the count of active WebDrivers
     * @return The number of active WebDrivers
     */
    public int getActiveWebDriverCount() {
        return webDrivers.size();
    }
    
    /**
     * Get all active WebDriver keys
     * @return Map of all active WebDrivers with their keys
     */
    public Map<String, WebDriver> getAllWebDrivers() {
        return new HashMap<>(webDrivers);
    }
    
    /**
     * Wait for recording file to be written to disk after finalization
     * Uses polling with exponential backoff to avoid unnecessary waits
     */
    private void waitForRecordingFile(TestDescription testDescription, Boolean testPassed) {
        String status = (testPassed != null && testPassed) ? "PASSED" : "FAILED";
        String expectedFilePrefix = status + "-" + testDescription.getFilesystemFriendlyName();
        int maxAttempts = 20;
        int attemptMs = 100;
        
        log.debug("Waiting for recording file with prefix: {}", expectedFilePrefix);
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            File currentRecDir = getCurrentRecordingDir();
            File[] recordings = currentRecDir.listFiles((dir, name) -> 
                name.startsWith(expectedFilePrefix) && name.endsWith(".mp4")
            );
            
            if (recordings != null) {
                for (File recording : recordings) {
                    if (recording.length() > 0) {
                        log.debug("Recording file found after {}ms: {} (size: {} bytes)", 
                                attempt * attemptMs, recording.getName(), recording.length());
                        return;
                    }
                }
            }
            
            try {
                Thread.sleep(attemptMs);
                attemptMs = Math.min(500, (int)(attemptMs * 1.5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for recording file");
                return;
            }
        }
        
        int totalTimeMs = 0;
        int tempMs = 100;
        for (int i = 1; i <= maxAttempts; i++) {
            totalTimeMs += tempMs;
            tempMs = Math.min(500, (int)(tempMs * 1.5));
        }
        
        log.warn("Recording file with prefix '{}' not found after {} attempts ({}ms total)", 
                expectedFilePrefix, maxAttempts, totalTimeMs);
    }
    
    /**
     * Wait for recording file - fallback method for backward compatibility
     */
    private void waitForRecordingFile(TestDescription testDescription) {
        waitForRecordingFile(testDescription, true); // Default to PASSED for backward compatibility
    }
    
    /**
     * Generate a unique key for WebDriver identification
     * Format: purpose:actor
     */
    private String generateKey(String purpose, String actor) {
        return purpose + ":" + actor;
    }
    
    /**
     * Parse a key back into its components
     * @param key The key to parse
     * @return Array containing [purpose, actor] or null if invalid
     */
    public String[] parseKey(String key) {
        String[] parts = key.split(":");
        if (parts.length == 2) {
            return parts;
        }
        log.warn("Invalid key format: {}, expected format: purpose:actor", key);
        return null;
    }
}