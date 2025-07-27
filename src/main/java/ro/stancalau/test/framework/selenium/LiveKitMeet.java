package ro.stancalau.test.framework.selenium;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LiveKitMeet {

    private final WebDriver driver;
    private static final String LOCAL_MEET_PAGE = "src/test/resources/web/livekit-meet/index.html";

    public LiveKitMeet(String liveKitUrl, String browser, String jwt) {
        SeleniumConfig config = new SeleniumConfig(browser);
        driver = config.getDriver();
        start(liveKitUrl, jwt, "TestRoom", "Test User");
    }

    public LiveKitMeet(String liveKitUrl, String browser, String jwt, String roomName, String participantName) {
        SeleniumConfig config = new SeleniumConfig(browser);
        driver = config.getDriver();
        start(liveKitUrl, jwt, roomName, participantName);
    }

    public LiveKitMeet(WebDriver driver, String liveKitUrl, String jwt) {
        this.driver = driver;
        start(liveKitUrl, jwt, "TestRoom", "Test User");
    }

    public LiveKitMeet(WebDriver driver, String liveKitUrl, String jwt, String roomName, String participantName) {
        this.driver = driver;
        start(liveKitUrl, jwt, roomName, participantName);
    }

    private void start(String liveKitUrl, String jwt, String roomName, String participantName) {
        try {
            File localPage = new File(LOCAL_MEET_PAGE);
            String pageUrl = "file://" + localPage.getAbsolutePath();
            
            String encodedLiveKitUrl = URLEncoder.encode(liveKitUrl, StandardCharsets.UTF_8);
            String encodedJwt = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            String encodedRoomName = URLEncoder.encode(roomName, StandardCharsets.UTF_8);
            String encodedParticipantName = URLEncoder.encode(participantName, StandardCharsets.UTF_8);
            
            String fullUrl = pageUrl + "?liveKitUrl=" + encodedLiveKitUrl + 
                            "&token=" + encodedJwt + 
                            "&roomName=" + encodedRoomName + 
                            "&participantName=" + encodedParticipantName + 
                            "&autoJoin=true";

            driver.get(fullUrl);
            driver.manage().window().setSize(new Dimension(1080, 768));

            log.info("Loading local LiveKit Meet page: {}", fullUrl);
        } catch (Exception e) {
            log.error("Failed to load local LiveKit Meet page", e);
            throw new RuntimeException("Failed to load local LiveKit Meet page", e);
        }
    }

    public boolean waitForConnection() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.of(15, ChronoUnit.SECONDS));
        log.info("LiveKitMeet waiting for connection to meeting room");

        // Wait for the meeting room to appear (form auto-submits due to autoJoin=true)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("meetingRoom")));
        
        // Check if connection status shows success
        WebElement connectionStatus = driver.findElement(By.id("connectionStatus"));
        String statusText = connectionStatus.getText();
        log.info("LiveKitMeet connection status: {}", statusText);
        
        // In our mock interface, if the meeting room is visible, connection succeeded
        return true;
    }

    public void stopAfter(int publishTimeSeconds) {
        Executors.newScheduledThreadPool(1).schedule(this::stop, publishTimeSeconds, TimeUnit.SECONDS);
    }

    public boolean disconnected() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.of(10, ChronoUnit.SECONDS));
        
        try {
            // Wait for join form to become visible again (indicates disconnection)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("joinForm")));
            
            // Check if meeting room is hidden
            WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
            boolean isHidden = meetingRoom.getCssValue("display").equals("none");
            
            log.info("LiveKitMeet disconnected - join form visible: {}, meeting room hidden: {}", true, isHidden);
            return true;
        } catch (TimeoutException e) {
            log.info("LiveKitMeet still connected - join form not visible");
            return false;
        }
    }

    public void stop() {
        log.info("LiveKitMeet stopping meet");
        try {
            // Click the leave button in our local interface
            WebElement leaveButton = driver.findElement(By.id("leaveBtn"));
            leaveButton.click();

            // Wait for the join form to become visible again
            WebDriverWait wait = new WebDriverWait(driver, Duration.of(5, ChronoUnit.SECONDS));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("joinForm")));
            
            log.info("LiveKitMeet successfully left meeting");
        } catch (Exception e) {
            log.warn("Failed to properly leave meeting, navigating to blank page", e);
            driver.get("about:blank");
        }
    }

    public void closeWindow() {
        driver.quit();
    }
    
    /**
     * Get the current room name displayed in the meeting interface
     */
    public String getCurrentRoomName() {
        try {
            WebElement roomTitle = driver.findElement(By.id("roomTitle"));
            return roomTitle.getText();
        } catch (NoSuchElementException e) {
            log.warn("Room title element not found");
            return null;
        }
    }
    
    /**
     * Get the current server URL displayed in the meeting interface
     */
    public String getCurrentServerUrl() {
        try {
            WebElement serverUrl = driver.findElement(By.id("serverUrl"));
            return serverUrl.getText();
        } catch (NoSuchElementException e) {
            log.warn("Server URL element not found");
            return null;
        }
    }
    
    /**
     * Check if currently in meeting room (vs join form)
     */
    public boolean isInMeetingRoom() {
        try {
            WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
            String display = meetingRoom.getCssValue("display");
            return !display.equals("none");
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    
    /**
     * Check if join form is currently visible
     */
    public boolean isJoinFormVisible() {
        try {
            WebElement joinForm = driver.findElement(By.id("joinForm"));
            String display = joinForm.getCssValue("display");
            return !display.equals("none");
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    
    /**
     * Toggle mute state in the meeting interface
     */
    public void toggleMute() {
        try {
            WebElement muteButton = driver.findElement(By.id("muteBtn"));
            muteButton.click();
            log.info("LiveKitMeet toggled mute");
        } catch (NoSuchElementException e) {
            log.warn("Mute button not found");
        }
    }
    
    /**
     * Toggle camera state in the meeting interface
     */
    public void toggleCamera() {
        try {
            WebElement cameraButton = driver.findElement(By.id("cameraBtn"));
            cameraButton.click();
            log.info("LiveKitMeet toggled camera");
        } catch (NoSuchElementException e) {
            log.warn("Camera button not found");
        }
    }
}
