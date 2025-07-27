package ro.stancalau.test.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestConfig {
    
    private static final String DEFAULT_LIVEKIT_VERSION = "v1.8.4";
    private static final String LIVEKIT_VERSION_PROPERTY = "livekit.version";
    private static final String LIVEKIT_VERSION_ENV = "LIVEKIT_VERSION";
    
    // VNC Recording Configuration
    private static final String RECORDING_MODE_PROPERTY = "recording.mode";
    private static final String RECORDING_MODE_ENV = "RECORDING_MODE";
    private static final String DEFAULT_RECORDING_MODE = "all";
    
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
        // 1. Check system property first (highest priority)
        String version = System.getProperty(LIVEKIT_VERSION_PROPERTY);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        // 2. Check environment variable
        version = System.getenv(LIVEKIT_VERSION_ENV);
        if (version != null && !version.trim().isEmpty()) {
            return version.trim();
        }
        
        // 3. Fall back to default
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
        // 1. Check system property first (highest priority)
        String mode = System.getProperty(RECORDING_MODE_PROPERTY);
        if (mode != null && !mode.trim().isEmpty()) {
            return mode.trim().toLowerCase();
        }
        
        // 2. Check environment variable
        mode = System.getenv(RECORDING_MODE_ENV);
        if (mode != null && !mode.trim().isEmpty()) {
            return mode.trim().toLowerCase();
        }
        
        // 3. Fall back to default
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
}