package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogPreservationSteps {


    @After(order = 1000) // Run after other @After hooks to ensure logs are captured at the end
    public void preserveLogsAfterScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        String featureName = extractFeatureName(scenario.getUri().toString());
        
        log.info("Logs for scenario '{}' from feature '{}' are already preserved in scenario-specific directories", scenarioName, featureName);
    }

    private String extractFeatureName(String uri) {
        try {
            // Extract filename from URI (e.g., "file:///path/to/livekit_webrtc_publish.feature")
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            
            // Remove .feature extension
            if (fileName.endsWith(".feature")) {
                fileName = fileName.substring(0, fileName.length() - 8);
            }
            
            // Convert underscores to spaces and capitalize
            String[] words = fileName.replace('_', ' ').toLowerCase().split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(word.substring(0, 1).toUpperCase())
                          .append(word.substring(1));
                }
            }
            return result.toString();
            
        } catch (Exception e) {
            log.warn("Failed to extract feature name from URI: {}", uri, e);
            return "Unknown Feature";
        }
    }
}