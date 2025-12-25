package ro.stancalau.test.bdd.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ScreenShareSteps {

    @When("{string} starts screen sharing")
    public void startsScreenSharing(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        meetInstance.startScreenShare();
    }

    @When("{string} stops screen sharing")
    public void stopsScreenSharing(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        meetInstance.stopScreenShare();
    }

    @When("{string} attempts to start screen sharing")
    public void attemptsToStartScreenSharing(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        try {
            meetInstance.startScreenShare();
        } catch (Exception e) {
            log.info("Screen share attempt failed as expected: {}", e.getMessage());
        }
    }

    @Then("participant {string} should not be able to share screen due to permissions")
    public void participantShouldNotBeAbleToShareScreenDueToPermissions(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        ManagerProvider.meetSessions().getWebDriver(participantName);
        BrowserPollingHelper.safeSleep(2000);
        boolean isBlocked = meetInstance.isScreenShareBlocked();
        boolean isSharing = meetInstance.isScreenSharing();
        assertTrue(isBlocked || !isSharing,
            participantName + " should not be able to share screen (blocked: " + isBlocked + ", sharing: " + isSharing + ")");
    }

    @Then("participant {string} should not be able to subscribe to video due to permissions")
    public void participantShouldNotBeAbleToSubscribeToVideoDueToPermissions(String participantName) {
        ManagerProvider.meetSessions().getMeetInstance(participantName);
        WebDriver driver = ManagerProvider.meetSessions().getWebDriver(participantName);
        BrowserPollingHelper.safeSleep(3000);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        Long subscriptionFailedCount = (Long) js.executeScript("return window.LiveKitTestHelpers.getSubscriptionFailedEventCount();");
        Boolean permissionDenied = (Boolean) js.executeScript("return window.LiveKitTestHelpers.isSubscriptionPermissionDenied();");
        String errorMessage = (String) js.executeScript("return window.LiveKitTestHelpers.getLastSubscriptionError();");
        Long playingVideoElements = (Long) js.executeScript("return window.LiveKitTestHelpers.getPlayingVideoElementCount();");
        Long subscribedTracks = (Long) js.executeScript("return window.LiveKitTestHelpers.getSubscribedVideoTrackCount();");

        boolean hasSubscriptionFailures = subscriptionFailedCount > 0 || permissionDenied;
        boolean hasNoVideoPlayback = playingVideoElements == 0 && subscribedTracks == 0;

        assertTrue(hasSubscriptionFailures || hasNoVideoPlayback,
            participantName + " should not be able to subscribe to video (failedEvents: " + subscriptionFailedCount +
            ", permissionDenied: " + permissionDenied + ", playingVideos: " + playingVideoElements +
            ", subscribedTracks: " + subscribedTracks + ", error: '" + errorMessage + "')");
    }
}
