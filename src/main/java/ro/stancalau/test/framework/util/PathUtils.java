package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Paths;

@UtilityClass
public final class PathUtils {

    private static final String OUTPUT_BASE = "out";
    private static final String BDD_BASE = join(OUTPUT_BASE, "bdd");
    private static final String SCENARIOS_BASE = join(BDD_BASE, "scenarios");
    private static final String CONFIG_BASE = join("src", "test", "resources", "livekit", "config");

    public static String join(String first, String... more) {
        return Paths.get(first, more).toString();
    }

    public static File file(String first, String... more) {
        return Paths.get(first, more).toFile();
    }

    public static String scenarioPath(String featureName, String scenarioName, String timestamp) {
        return join(SCENARIOS_BASE, featureName, scenarioName, timestamp);
    }

    public static String currentScenarioPath() {
        return join(SCENARIOS_BASE, "current");
    }

    public static String containerLogPath(String basePath, String containerType, String alias) {
        return join(basePath, containerType, alias);
    }

    public static String livekitConfigPath(String version, String profile, String filename) {
        return join(CONFIG_BASE, version, profile, filename);
    }

    public static String egressConfigPath(String version, String profile, String filename) {
        return join(CONFIG_BASE, version, profile, filename);
    }

    public static String containerPath(String first, String... more) {
        StringBuilder path = new StringBuilder(first);
        for (String segment : more) {
            if (!path.toString().endsWith("/") && !segment.startsWith("/")) {
                path.append("/");
            }
            path.append(segment);
        }
        return path.toString();
    }
}