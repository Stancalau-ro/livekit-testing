package ro.stancalau.test.bdd.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ScreenShareSteps {

    private static final long PERMISSION_CHECK_DELAY_MS = 2000;
    private static final long SUBSCRIPTION_CHECK_DELAY_MS = 3000;

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
        BrowserPollingHelper.safeSleep(PERMISSION_CHECK_DELAY_MS);
        boolean isBlocked = meetInstance.getMedia().isScreenShareBlocked();
        boolean isSharing = meetInstance.getMedia().isScreenSharing();
        assertTrue(isBlocked || !isSharing,
            participantName + " should not be able to share screen (blocked: " + isBlocked + ", sharing: " + isSharing + ")");
    }

    @Then("participant {string} should not be able to subscribe to video due to permissions")
    public void participantShouldNotBeAbleToSubscribeToVideoDueToPermissions(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        BrowserPollingHelper.safeSleep(SUBSCRIPTION_CHECK_DELAY_MS);

        long subscriptionFailedCount = meetInstance.getConnection().getSubscriptionFailedEventCount();
        boolean permissionDenied = meetInstance.getConnection().isSubscriptionPermissionDenied();
        String errorMessage = meetInstance.getConnection().getLastSubscriptionError();
        long playingVideoElements = meetInstance.getConnection().getPlayingVideoElementCount();
        long subscribedTracks = meetInstance.getConnection().getSubscribedVideoTrackCount();

        boolean hasSubscriptionFailures = subscriptionFailedCount > 0 || permissionDenied;
        boolean hasNoVideoPlayback = playingVideoElements == 0 && subscribedTracks == 0;

        assertTrue(hasSubscriptionFailures || hasNoVideoPlayback,
            participantName + " should not be able to subscribe to video (failedEvents: " + subscriptionFailedCount +
            ", permissionDenied: " + permissionDenied + ", playingVideos: " + playingVideoElements +
            ", subscribedTracks: " + subscribedTracks + ", error: '" + errorMessage + "')");
    }
}
