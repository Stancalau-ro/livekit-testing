package ro.stancalau.test.framework.config;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.util.PathUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
@UtilityClass
public class TestConfig {
    
    private static final String DEFAULT_LIVEKIT_VERSION = "v1.8.4";
    private static final String LIVEKIT_VERSION_PROPERTY = "livekit.version";
    private static final String LIVEKIT_VERSION_ENV = "LIVEKIT_VERSION";
    
    private static final String DEFAULT_EGRESS_VERSION = "v1.8.4";
    private static final String EGRESS_VERSION_PROPERTY = "egress.version";
    private static final String EGRESS_VERSION_ENV = "EGRESS_VERSION";

    private static final String DEFAULT_JS_VERSION = "2.6.4";
    private static final String JS_VERSION_PROPERTY = "livekit.js.version";
    private static final String JS_VERSION_ENV = "LIVEKIT_JS_VERSION";
    
    private static final String RECORDING_MODE_PROPERTY = "recording.mode";
    private static final String RECORDING_MODE_ENV = "RECORDING_MODE";
    private static final String DEFAULT_RECORDING_MODE = "all";
    
    private static final List<String> VALID_RECORDING_MODES = Arrays.asList("skip", "all", "failed");
    
    private static final String CONFIG_BASE_PATH = "src/test/resources/livekit/config";
    
    /**
     * Gets the LiveKit version to use for tests.
     * Priority: System Property > Environment Variable > Gradle Property > Default
     * <p>
     * Usage:
     * - System Property: -Dlivekit.version=v1.8.5
     * - Environment Variable: LIVEKIT_VERSION=v1.8.5
     * - Gradle Property: -Plivekit_docker_version=v1.8.5
     * - Default: v1.8.4
     */
    public static String getLiveKitVersion() {
        String version = System.getProperty(LIVEKIT_VERSION_PROPERTY);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        version = System.getenv(LIVEKIT_VERSION_ENV);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        version = System.getProperty("livekit_docker_version");
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        return DEFAULT_LIVEKIT_VERSION;
    }
    
    /**
     * Gets the default LiveKit version
     */
    public static String getDefaultLiveKitVersion() {
        return DEFAULT_LIVEKIT_VERSION;
    }
    
    /**
     * Gets the egress version to use for tests.
     * Priority: System Property > Environment Variable > Gradle Property > Default
     * <p>
     * Usage:
     * - System Property: -Degress.version=v1.8.5
     * - Environment Variable: EGRESS_VERSION=v1.8.5
     * - Gradle Property: -Pegress_docker_version=v1.8.5
     * - Default: v1.8.4
     */
    public static String getEgressVersion() {
        String version = System.getProperty(EGRESS_VERSION_PROPERTY);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        version = System.getenv(EGRESS_VERSION_ENV);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        version = System.getProperty("egress_docker_version");
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        return DEFAULT_EGRESS_VERSION;
    }
    
    public static String getDefaultEgressVersion() {
        return DEFAULT_EGRESS_VERSION;
    }

    public static String getJsVersion() {
        String version = System.getProperty(JS_VERSION_PROPERTY);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }

        version = System.getenv(JS_VERSION_ENV);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }

        version = System.getProperty("livekit_js_version");
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }

        return DEFAULT_JS_VERSION;
    }

    public static String getDefaultJsVersion() {
        return DEFAULT_JS_VERSION;
    }

    /**
     * Gets the VNC recording mode for browser tests.
     * Priority: System Property > Environment Variable > Default
     * <p>
     * Valid values:
     * - "skip": No recordings
     * - "all": Record all tests (passed and failed)
     * - "failed": Record only failed tests
     * <p>
     * Usage:
     * - System Property: -Drecording.mode=failed
     * - Environment Variable: RECORDING_MODE=skip
     * - Default: all
     */
    public static String getRecordingMode() {
        String mode = System.getProperty(RECORDING_MODE_PROPERTY);
        if (mode != null && !mode.trim().isEmpty()) {
            return validateAndNormalizeRecordingMode(mode.trim());
        }
        
        mode = System.getenv(RECORDING_MODE_ENV);
        if (mode != null && !mode.trim().isEmpty()) {
            return validateAndNormalizeRecordingMode(mode.trim());
        }
        
        return DEFAULT_RECORDING_MODE;
    }
    
    /**
     * Validates and normalizes the recording mode value
     * @param mode The raw recording mode value
     * @return The validated and normalized recording mode, or default if invalid
     */
    private static String validateAndNormalizeRecordingMode(String mode) {
        String normalizedMode = mode.toLowerCase();
        
        if (VALID_RECORDING_MODES.contains(normalizedMode)) {
            return normalizedMode;
        }
        
        log.warn("Invalid recording mode '{}'. Valid options are: {}. Falling back to default: '{}'", 
                mode, VALID_RECORDING_MODES, DEFAULT_RECORDING_MODE);
        return DEFAULT_RECORDING_MODE;
    }
    
    /**
     * Check if VNC recording is enabled for the current configuration
     */
    public static boolean isRecordingEnabled() {
        return !"skip".equals(getRecordingMode());
    }
    
    /**
     * Check if recording should only happen for failed tests
     */
    public static boolean isRecordOnlyFailed() {
        return "failed".equals(getRecordingMode());
    }
    
    /**
     * Get the list of valid recording modes
     * @return List of valid recording mode values
     */
    public static List<String> getValidRecordingModes() {
        return VALID_RECORDING_MODES;
    }
    
    /**
     * Resolves the configuration path for a given profile using the current LiveKit version.
     * Falls back to the latest available version if the current version is not found.
     * 
     * @param profileName The profile name (e.g., "basic", "basic_hook")
     * @return The full path to the configuration file
     */
    public static String resolveConfigPath(String profileName) {
        String currentVersion = getLiveKitVersion();
        String versionedPath = PathUtils.livekitConfigPath(currentVersion, profileName, "config.yaml");
        
        File versionedConfig = new File(versionedPath);
        if (versionedConfig.exists()) {
            log.debug("Using configuration for version {} and profile {}: {}", currentVersion, profileName, versionedPath);
            return versionedPath;
        }
        
        log.warn("Configuration not found for version {} and profile {}. Searching for latest available version...", 
                currentVersion, profileName);
        
        String latestVersionPath = findLatestVersionConfig(profileName);
        if (latestVersionPath != null) {
            return latestVersionPath;
        }
        
        throw new IllegalStateException(String.format(
                "No configuration found for profile '%s'. Searched in %s and all available version directories.",
                profileName, versionedPath));
    }
    
    /**
     * Finds the latest available configuration for a given profile by scanning version directories.
     * 
     * @param profileName The profile name to search for
     * @return The path to the latest available config, or null if none found
     */
    private static String findLatestVersionConfig(String profileName) {
        File configBaseDir = new File(CONFIG_BASE_PATH);
        if (!configBaseDir.exists() || !configBaseDir.isDirectory()) {
            log.error("Configuration base directory does not exist: {}", CONFIG_BASE_PATH);
            return null;
        }
        
        File[] versionDirs = configBaseDir.listFiles(File::isDirectory);
        if (versionDirs == null || versionDirs.length == 0) {
            log.error("No version directories found in: {}", CONFIG_BASE_PATH);
            return null;
        }
        
        Arrays.sort(versionDirs, (a, b) -> b.getName().compareTo(a.getName()));
        
        for (File versionDir : versionDirs) {
            String candidatePath = PathUtils.livekitConfigPath(versionDir.getName(), profileName, "config.yaml");
            File candidateConfig = new File(candidatePath);
            
            if (candidateConfig.exists()) {
                log.warn("Using fallback configuration for profile {} from version {}: {}", 
                        profileName, versionDir.getName(), candidatePath);
                return candidatePath;
            }
        }
        
        return null;
    }
}