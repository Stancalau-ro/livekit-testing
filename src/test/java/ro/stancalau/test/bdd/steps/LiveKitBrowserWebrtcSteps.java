package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.livekit.server.AccessToken;
import lombok.extern.slf4j.Slf4j;
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
    public void tearDownLiveKitWebrtcSteps(io.cucumber.java.Scenario scenario) {
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


    @When("I open a Chrome browser with LiveKit Meet page as {string}")
    public void iOpenAChromeBrowserWithLiveKitMeetPageAs(String participantId) {
        log.info("Opening Chrome browser for participant: {}", participantId);
        WebDriver driver = webDriverManager.createWebDriver("meet", participantId, "chrome");
        assertNotNull(driver, "Chrome browser should be initialized for " + participantId);
    }


    @When("I connect to room {string} as {string} using the access token")
    public void iConnectToRoomAsUsingTheAccessToken(String roomName, String participantName) {
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

    @And("I wait for successful connection for {string}")
    public void iWaitForSuccessfulConnectionFor(String participantName) {
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
        iWaitForSuccessfulConnectionFor(participantName);
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


    private String getLiveKitServerUrl() {
        LiveKitContainer container = containerManager.getContainer("livekit1", LiveKitContainer.class);
        assertNotNull(container, "LiveKit container should be running");
        assertTrue(container.isRunning(), "LiveKit container should be running");
        
        // Return network WebSocket URL accessible from other containers in the same network
        return container.getNetworkWs();
    }
    
}