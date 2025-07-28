package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@UtilityClass
public class TestConfig {
    
    private static final String DEFAULT_LIVEKIT_VERSION = "v1.8.4";
    private static final String LIVEKIT_VERSION_PROPERTY = "livekit.version";
    private static final String LIVEKIT_VERSION_ENV = "LIVEKIT_VERSION";
    
    private static final String RECORDING_MODE_PROPERTY = "recording.mode";
    private static final String RECORDING_MODE_ENV = "RECORDING_MODE";
    private static final String DEFAULT_RECORDING_MODE = "all";
    
    private static final List<String> VALID_RECORDING_MODES = Arrays.asList("skip", "all", "failed");
    
    /**
     * Gets the LiveKit version to use for tests.
     * Priority: System Property > Environment Variable > Default
     * 
     * Usage:
     * - System Property: -Dlivekit.version=v1.8.5
     * - Environment Variable: LIVEKIT_VERSION=v1.8.5
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
        
        return DEFAULT_LIVEKIT_VERSION;
    }
    
    /**
     * Gets the default LiveKit version
     */
    public static String getDefaultLiveKitVersion() {
        return DEFAULT_LIVEKIT_VERSION;
    }
    
    /**
     * Gets the VNC recording mode for browser tests.
     * Priority: System Property > Environment Variable > Default
     * 
     * Valid values:
     * - "skip": No recordings
     * - "all": Record all tests (passed and failed)
     * - "failed": Record only failed tests
     * 
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
}