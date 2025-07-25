package ro.stancalau.test.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestConfig {
    
    private static final String DEFAULT_LIVEKIT_VERSION = "v1.8.4";
    private static final String LIVEKIT_VERSION_PROPERTY = "livekit.version";
    private static final String LIVEKIT_VERSION_ENV = "LIVEKIT_VERSION";
    
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
}