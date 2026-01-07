package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.AccessToken;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

@Slf4j
public class RoomConnectionSteps {

    @When("{string} connects to room {string} using the access token")
    public void connectsToRoomUsingTheAccessToken(String participantName, String roomName) {
        Map.Entry<String, LiveKitContainer> containerEntry =
                ManagerProvider.containers().getFirstContainerOfType(LiveKitContainer.class);
        assertNotNull(containerEntry, "At least one LiveKit container should be running");
        connectsToRoomUsingTheAccessTokenForService(participantName, roomName, containerEntry.getKey());
    }

    @When("{string} connects to room {string} using the access token for service {string}")
    public void connectsToRoomUsingTheAccessTokenForService(
            String participantName, String roomName, String serviceName) {
        AccessToken token = ManagerProvider.tokens().getLastToken(participantName, roomName);
        assertNotNull(token, "Access token should exist for " + participantName + " in room " + roomName);

        WebDriver driver = ManagerProvider.meetSessions().getWebDriver(participantName);
        String liveKitUrl = getLiveKitServerUrl(serviceName);
        String tokenString = token.toJwt();

        boolean simulcastEnabled = ManagerProvider.videoQuality().getSimulcastPreference(participantName);
        LiveKitMeet meetInstance = new LiveKitMeet(
                driver,
                liveKitUrl,
                tokenString,
                roomName,
                participantName,
                ManagerProvider.containers(),
                simulcastEnabled);
        ManagerProvider.meetSessions().putMeetInstance(participantName, meetInstance);
        assertNotNull(meetInstance, "LiveKitMeet instance should be created");
    }

    @And("connection is established successfully for {string}")
    public void connectionIsEstablishedSuccessfullyFor(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        int maxRetries = 3;
        int retryDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                boolean connected = meetInstance.waitForConnection();
                if (connected) {
                    if (attempt > 1) {
                        log.info("Connection succeeded for {} on attempt {}", participantName, attempt);
                    }
                    return;
                }

                String errorDetails = meetInstance.getPageErrorDetails();
                boolean isRetryableError = isRetryableServerError(errorDetails);

                if (isRetryableError && attempt < maxRetries) {
                    log.warn(
                            "Retryable server error for {}, attempt {}/{}: {}. Retrying in {}ms",
                            participantName,
                            attempt,
                            maxRetries,
                            errorDetails,
                            retryDelayMs);
                    BrowserPollingHelper.safeSleep(retryDelayMs);
                    meetInstance.refreshAndReconnect();
                    continue;
                }

                String failureMessage = participantName
                        + " should successfully connect to the meeting (attempt "
                        + attempt
                        + "/"
                        + maxRetries
                        + ")";
                if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                    failureMessage += ". Browser error: " + errorDetails;
                }
                fail(failureMessage);

            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    fail("Connection attempt interrupted for " + participantName);
                }
                if (e instanceof ro.stancalau.test.framework.util.WebDriverSessionDeadException) {
                    throw e;
                }
                String errorDetails = meetInstance.getPageErrorDetails();
                boolean isRetryableError = isRetryableServerError(errorDetails);

                if (isRetryableError && attempt < maxRetries) {
                    log.warn(
                            "Retryable exception for {}, attempt {}/{}: {}. Retrying in {}ms",
                            participantName,
                            attempt,
                            maxRetries,
                            e.getMessage(),
                            retryDelayMs);
                    BrowserPollingHelper.safeSleep(retryDelayMs);
                    meetInstance.refreshAndReconnect();
                    continue;
                }

                String failureMessage =
                        "Connection failed for " + participantName + " with exception: " + e.getMessage();
                if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                    failureMessage += ". Browser error: " + errorDetails;
                }
                fail(failureMessage);
            }
        }
    }

    @Then("the connection should be successful for {string}")
    public void theConnectionShouldBeSuccessfulFor(String participantName) {
        connectionIsEstablishedSuccessfullyFor(participantName);
    }

    private boolean isRetryableServerError(String errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        String errorLower = errorDetails.toLowerCase();
        return errorLower.contains("could not find any available nodes")
                || errorLower.contains("server error")
                || errorLower.contains("500")
                || errorLower.contains("internal server error")
                || errorLower.contains("websocket error")
                || errorLower.contains("signal connection")
                || errorLower.contains("could not establish");
    }

    private String getLiveKitServerUrl(String serviceName) {
        LiveKitContainer container = ManagerProvider.containers().getContainer(serviceName, LiveKitContainer.class);
        assertNotNull(container, "LiveKit container '" + serviceName + "' should be running");
        assertTrue(container.isRunning(), "LiveKit container '" + serviceName + "' should be running");
        return container.getNetworkUrl();
    }

    @Then("{string} should be receiving video from {string}")
    public void shouldBeReceivingVideoFrom(String subscriberName, String publisherName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(subscriberName);
        assertNotNull(meetInstance, "Meet instance should exist for " + subscriberName);

        boolean isReceiving = BrowserPollingHelper.pollForCondition(
                () -> meetInstance.getConnection().isReceivingVideoFrom(publisherName), 30_000, 1000);
        assertTrue(
                isReceiving,
                subscriberName
                        + " should be receiving video from "
                        + publisherName
                        + " (WebRTC bytes/frames received)");
    }

    @Then("{string} should be receiving video from {string} at more than {int} bytes per second")
    public void shouldBeReceivingVideoAtBitrate(String subscriberName, String publisherName, int minBytesPerSecond) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(subscriberName);
        assertNotNull(meetInstance, "Meet instance should exist for " + subscriberName);

        var rate = meetInstance.getConnection().measureVideoReceptionRate(publisherName, 2000);
        assertTrue(
                rate.bytesPerSecond() > minBytesPerSecond,
                subscriberName
                        + " should be receiving video from "
                        + publisherName
                        + " at more than "
                        + minBytesPerSecond
                        + " bytes/sec (actual: "
                        + rate.bytesPerSecond()
                        + " bytes/sec, "
                        + rate.framesPerSecond()
                        + " fps)");
    }

    @Then("{string} should be receiving video frames from {string}")
    public void shouldBeReceivingVideoFramesFrom(String subscriberName, String publisherName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(subscriberName);
        assertNotNull(meetInstance, "Meet instance should exist for " + subscriberName);

        var stats = meetInstance.getConnection().getSubscriberVideoStats(publisherName);
        assertNotNull(stats, subscriberName + " should have subscriber video stats for " + publisherName);
        boolean isReceiving = stats.isSubscribed() && stats.hasTrack() && (stats.frameWidth() > 0 || stats.isPlaying());
        assertTrue(
                isReceiving,
                subscriberName
                        + " should have received video frames from "
                        + publisherName
                        + " (isSubscribed: "
                        + stats.isSubscribed()
                        + ", hasTrack: "
                        + stats.hasTrack()
                        + ", frameWidth: "
                        + stats.frameWidth()
                        + ", isPlaying: "
                        + stats.isPlaying()
                        + ")");
    }
}
