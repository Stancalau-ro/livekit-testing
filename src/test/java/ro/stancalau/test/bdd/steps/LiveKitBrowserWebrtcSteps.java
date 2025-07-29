package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.livekit.server.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.state.AccessTokenStateManager;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.state.RoomClientStateManager;
import ro.stancalau.test.framework.state.WebDriverStateManager;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.StringParsingUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class LiveKitBrowserWebrtcSteps {

    private ContainerStateManager containerManager;
    private RoomClientStateManager roomClientManager;
    private AccessTokenStateManager accessTokenManager;
    private WebDriverStateManager webDriverManager;
    
    private final Map<String, LiveKitMeet> meetInstances = new ConcurrentHashMap<>();

    @Before
    public void setUpLiveKitWebrtcSteps() {
        containerManager = ContainerStateManager.getInstance();
        roomClientManager = RoomClientStateManager.getInstance();
        accessTokenManager = AccessTokenStateManager.getInstance();
        webDriverManager = WebDriverStateManager.getInstance();
    }

    @After
    public void tearDownLiveKitWebrtcSteps(Scenario scenario) {
        if (scenario.isFailed() && webDriverManager != null) {
            // Mark all active WebDrivers for this scenario as failed
            webDriverManager.getAllWebDrivers().keySet().forEach(key -> {
                String[] parts = webDriverManager.parseKey(key);
                if (parts != null && parts.length == 2) {
                    webDriverManager.markTestFailed(parts[0], parts[1]);
                    log.info("Marked WebDriver test as FAILED due to scenario failure: {}", key);
                }
            });
        }
        
        meetInstances.values().forEach(meet -> {
            try {
                meet.closeWindow();
            } catch (Exception e) {
                log.warn("Error closing meet instance: {}", e.getMessage());
            }
        });
        meetInstances.clear();
        
        if (webDriverManager != null) {
            webDriverManager.closeAllWebDrivers();
        }
        
        if (roomClientManager != null) {
            roomClientManager.clearAll();
        }
        if (accessTokenManager != null) {
            accessTokenManager.clearAll();
        }
    }

    @When("{string} opens a {string} browser with LiveKit Meet page")
    public void opensABrowserWithLiveKitMeetPage(String participantId, String browser) {
        log.info("Opening {} browser for participant: {}", browser, participantId);
        WebDriver driver = webDriverManager.createWebDriver("meet", participantId, browser.toLowerCase());
        assertNotNull(driver, browser + " browser should be initialized for " + participantId);
    }

    @When("{string} connects to room {string} using the access token")
    public void connectsToRoomUsingTheAccessToken(String participantName, String roomName) {
        AccessToken token = accessTokenManager.getLastToken(participantName, roomName);
        assertNotNull(token, "Access token should exist for " + participantName + " in room " + roomName);
        
        WebDriver driver = webDriverManager.getWebDriver("meet", participantName);
        assertNotNull(driver, "WebDriver should exist for participant: " + participantName);
        
        String liveKitUrl = getLiveKitServerUrl();
        String tokenString = token.toJwt();
        
        log.info("Connecting {} to room {} using LiveKit URL: {}", participantName, roomName, liveKitUrl);
        
        LiveKitMeet meetInstance = new LiveKitMeet(driver, liveKitUrl, tokenString, roomName, participantName);
        meetInstances.put(participantName, meetInstance);
        assertNotNull(meetInstance, "LiveKitMeet instance should be created");
    }

    @And("connection is established successfully for {string}")
    public void connectionIsEstablishedSuccessfullyFor(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        
        try {
            boolean connected = meetInstance.waitForConnection();
            if (!connected) {
                String errorDetails = meetInstance.getPageErrorDetails();
                String failureMessage = participantName + " should successfully connect to the meeting";
                if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                    failureMessage += ". Browser error: " + errorDetails;
                }
                fail(failureMessage);
            }
            log.info("{} successfully connected to meeting", participantName);
        } catch (Exception e) {
            String errorDetails = meetInstance.getPageErrorDetails();
            String failureMessage = "Connection failed for " + participantName + " with exception: " + e.getMessage();
            if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                failureMessage += ". Browser error: " + errorDetails;
            }
            fail(failureMessage);
        }
    }

    @Then("the connection should be successful for {string}")
    public void theConnectionShouldBeSuccessfulFor(String participantName) {
        connectionIsEstablishedSuccessfullyFor(participantName);
    }

    @And("{string} can toggle camera controls")
    public void participantCanToggleCameraControls(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        meetInstance.toggleCamera();
        meetInstance.toggleCamera();
        log.info("{} successfully toggled camera controls", participantName);
    }

    @And("{string} can toggle mute controls")
    public void participantCanToggleMuteControls(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        meetInstance.toggleMute();
        meetInstance.toggleMute();
        log.info("{} successfully toggled mute controls", participantName);
    }


    @Then("participants {string} should see room name {string}")
    public void participantsShouldSeeRoomName(String participantList, String expectedRoomName) {
        String[] participants = StringParsingUtils.parseCommaSeparatedList(participantList).toArray(new String[0]);

        for (String participantName : participants) {
            LiveKitMeet meetInstance = meetInstances.get(participantName);
            assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

            String actualRoomName = meetInstance.getCurrentRoomName();
            assertEquals(expectedRoomName, actualRoomName,
                "Participant " + participantName + " should see correct room name");

            log.info("Verified participant {} sees room name: {}", participantName, actualRoomName);
        }
    }

    @And("{string} leaves the meeting")
    public void participantLeavesTheMeeting(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        meetInstance.stop();
        log.info("{} left the meeting", participantName);
    }

    @Then("{string} should be disconnected from the room")
    public void participantShouldBeDisconnectedFromTheRoom(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        boolean disconnected = meetInstance.disconnected();
        assertTrue(disconnected, participantName + " should be disconnected from the room");
        log.info("Verified {} is disconnected from room", participantName);
    }

    @Then("{string} should see the join form again")
    public void participantShouldSeeTheJoinFormAgain(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
        assertTrue(meetInstance.isJoinFormVisible(), "Join form should be visible for " + participantName);
        log.info("Verified {} sees join form again", participantName);
    }

    @And("{string} closes the browser")
    public void participantClosesTheBrowser(String participantName) {
        webDriverManager.closeWebDriver("meet", participantName);
        meetInstances.remove(participantName);
        log.info("{} closed browser", participantName);
    }
    
    @Then("participant {string} should have video subscription blocked due to permissions")
    public void participantShouldHaveVideoSubscriptionBlockedDueToPermissions(String participantName) {
        LiveKitMeet meetInstance = meetInstances.get(participantName);
        assertNotNull(meetInstance, participantName + " should have an active LiveKit Meet instance");
        
        WebDriver driver = webDriverManager.getWebDriver("meet", participantName);
        assertNotNull(driver, "WebDriver should exist for " + participantName);
        
        try {
            // Wait a bit for subscription processing
            Thread.sleep(3000);
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Check if any subscription failures were captured
            Long subscriptionFailedCount = (Long) js.executeScript(
                "return window.subscriptionFailedEvents ? window.subscriptionFailedEvents.length : 0;"
            );
            
            // Check if permission denied flag was set
            Boolean permissionDenied = (Boolean) js.executeScript(
                "return window.subscriptionPermissionDenied || false;"
            );
            
            // Get the error message if available
            String errorMessage = (String) js.executeScript(
                "return window.lastSubscriptionError || '';"
            );
            
            // Check the number of video elements that are actually playing video (fallback check)
            Long playingVideoElements = (Long) js.executeScript(
                "try { return Array.from(document.querySelectorAll('video')).filter(v => " +
                "v.videoWidth > 0 && v.videoHeight > 0 && !v.paused && !v.ended && v.readyState >= 2).length; } catch(e) { return 0; }"
            );
            
            // Check if client has any subscribed tracks (fallback check)
            Long subscribedTracks = (Long) js.executeScript(
                "try { return window.liveKitClient && window.liveKitClient.room ? " +
                "Array.from(window.liveKitClient.room.tracks.values()).filter(t => t.kind === 'video' && t.isSubscribed).length : 0; } catch(e) { return 0; }"
            );
            
            log.info("Video subscription check for {}: subscriptionFailedCount={}, permissionDenied={}, errorMessage='{}', playingVideoElements={}, subscribedTracks={}", 
                     participantName, subscriptionFailedCount, permissionDenied, errorMessage, playingVideoElements, subscribedTracks);
            
            // A participant without subscribe permission should either have subscription failures OR no playing/subscribed tracks
            boolean hasSubscriptionFailures = subscriptionFailedCount > 0 || permissionDenied;
            boolean hasNoVideoPlayback = playingVideoElements == 0 && subscribedTracks == 0;
            
            assertTrue(hasSubscriptionFailures || hasNoVideoPlayback, 
                participantName + " should have video subscription blocked (failedEvents: " + subscriptionFailedCount + 
                ", permissionDenied: " + permissionDenied + ", playingVideos: " + playingVideoElements + 
                ", subscribedTracks: " + subscribedTracks + ", error: '" + errorMessage + "')");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while checking subscription status");
        }
    }

    private String getLiveKitServerUrl() {
        LiveKitContainer container = containerManager.getContainer("livekit1", LiveKitContainer.class);
        assertNotNull(container, "LiveKit container should be running");
        assertTrue(container.isRunning(), "LiveKit container should be running");
        
        // Return network WebSocket URL accessible from other containers in the same network
        return container.getNetworkWs();
    }
}