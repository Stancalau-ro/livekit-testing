package ro.stancalau.test.framework.state;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.selenium.LiveKitMeet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class MeetSessionStateManager {

    private final WebDriverStateManager webDriverStateManager;
    private final Map<String, LiveKitMeet> meetInstances = new HashMap<>();

    public MeetSessionStateManager(WebDriverStateManager webDriverStateManager) {
        this.webDriverStateManager = webDriverStateManager;
    }

    public LiveKitMeet getMeetInstance(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        return Objects.requireNonNull(meetInstance, "Meet instance should exist for " + participantName);
    }

    public LiveKitMeet getMeetInstanceOrNull(String participantName) {
        return meetInstances.get(participantName);
    }

    public void putMeetInstance(String participantName, LiveKitMeet meetInstance) {
        meetInstances.put(participantName, meetInstance);
        log.debug("Registered meet instance for participant: {}", participantName);
    }

    public void removeMeetInstance(String participantName) {
        meetInstances.remove(participantName);
        log.debug("Removed meet instance for participant: {}", participantName);
    }

    public WebDriver getWebDriver(String participantName) {
        WebDriver driver = webDriverStateManager.getWebDriver("meet", participantName);
        return Objects.requireNonNull(driver, "WebDriver should exist for participant: " + participantName);
    }

    public JavascriptExecutor getJsExecutor(String participantName) {
        return (JavascriptExecutor) getWebDriver(participantName);
    }

    public boolean hasMeetInstance(String participantName) {
        return meetInstances.containsKey(participantName);
    }

    public Map<String, LiveKitMeet> getAllMeetInstances() {
        return meetInstances;
    }

    public void clearAll() {
        log.info("Clearing all meet session state");
        meetInstances.values().forEach(meet -> {
            try {
                meet.clearDynacastState();
            } catch (Exception e) {
                log.debug("Error clearing dynacast state: {}", e.getMessage());
            }
            try {
                meet.closeWindow();
            } catch (Exception e) {
                log.warn("Error closing meet instance: {}", e.getMessage());
            }
        });
        meetInstances.clear();
    }
}
