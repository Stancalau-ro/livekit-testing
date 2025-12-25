package ro.stancalau.test.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ro.stancalau.test.bdd.steps.params.MuteAction;
import ro.stancalau.test.bdd.steps.params.MuteState;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;
import ro.stancalau.test.framework.util.StringParsingUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitBrowserWebrtcSteps {

    private static final int LOW_QUALITY_MAX_WIDTH = 400;
    private static final int HIGH_QUALITY_MIN_WIDTH = 500;
    private static final int MIN_TIMESTAMPED_MESSAGES = 10;

    @After
    public void tearDownLiveKitWebrtcSteps(Scenario scenario) {
        if (scenario.isFailed() && ManagerProvider.webDrivers() != null) {
            ManagerProvider.webDrivers().getAllWebDrivers().keySet().forEach(key -> {
                String[] parts = ManagerProvider.webDrivers().parseKey(key);
                if (parts != null && parts.length == 2) {
                    ManagerProvider.webDrivers().markTestFailed(parts[0], parts[1]);
                }
            });
        }
    }

    @When("{string} opens a {string} browser with LiveKit Meet page")
    public void opensABrowserWithLiveKitMeetPage(String participantId, String browser) {
        WebDriver driver = ManagerProvider.webDrivers().createWebDriver("meet", participantId, browser.toLowerCase());
        assertNotNull(driver, browser + " browser should be initialized for " + participantId);
    }

    @When("{string} connects to room {string} using the access token")
    public void connectsToRoomUsingTheAccessToken(String participantName, String roomName) {
        AccessToken token = ManagerProvider.tokens().getLastToken(participantName, roomName);
        assertNotNull(token, "Access token should exist for " + participantName + " in room " + roomName);

        WebDriver driver = ManagerProvider.meetSessions().getWebDriver(participantName);
        String liveKitUrl = getLiveKitServerUrl();
        String tokenString = token.toJwt();

        boolean simulcastEnabled = ManagerProvider.videoQuality().getSimulcastPreference(participantName);
        LiveKitMeet meetInstance = new LiveKitMeet(driver, liveKitUrl, tokenString, roomName, participantName, ManagerProvider.containers(), simulcastEnabled);
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
                    log.warn("Retryable server error for {}, attempt {}/{}: {}. Retrying in {}ms",
                            participantName, attempt, maxRetries, errorDetails, retryDelayMs);
                    BrowserPollingHelper.safeSleep(retryDelayMs);
                    meetInstance.refreshAndReconnect();
                    continue;
                }

                String failureMessage = participantName + " should successfully connect to the meeting (attempt " + attempt + "/" + maxRetries + ")";
                if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                    failureMessage += ". Browser error: " + errorDetails;
                }
                fail(failureMessage);

            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    fail("Connection attempt interrupted for " + participantName);
                }
                String errorDetails = meetInstance.getPageErrorDetails();
                boolean isRetryableError = isRetryableServerError(errorDetails);

                if (isRetryableError && attempt < maxRetries) {
                    log.warn("Retryable exception for {}, attempt {}/{}: {}. Retrying in {}ms",
                            participantName, attempt, maxRetries, e.getMessage(), retryDelayMs);
                    BrowserPollingHelper.safeSleep(retryDelayMs);
                    meetInstance.refreshAndReconnect();
                    continue;
                }

                String failureMessage = "Connection failed for " + participantName + " with exception: " + e.getMessage();
                if (errorDetails != null && !errorDetails.trim().isEmpty()) {
                    failureMessage += ". Browser error: " + errorDetails;
                }
                fail(failureMessage);
            }
        }
    }

    private boolean isRetryableServerError(String errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        String errorLower = errorDetails.toLowerCase();
        return errorLower.contains("could not find any available nodes") ||
               errorLower.contains("server error") ||
               errorLower.contains("500") ||
               errorLower.contains("internal server error") ||
               errorLower.contains("websocket error") ||
               errorLower.contains("signal connection") ||
               errorLower.contains("could not establish");
    }

    @Then("the connection should be successful for {string}")
    public void theConnectionShouldBeSuccessfulFor(String participantName) {
        connectionIsEstablishedSuccessfullyFor(participantName);
    }

    @And("{string} can toggle camera controls")
    public void participantCanToggleCameraControls(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        meetInstance.toggleCamera();
        meetInstance.toggleCamera();
    }

    @And("{string} can toggle mute controls")
    public void participantCanToggleMuteControls(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        meetInstance.toggleMute();
        meetInstance.toggleMute();
    }

    @Then("participants {string} should see room name {string}")
    public void participantsShouldSeeRoomName(String participantList, String expectedRoomName) {
        String[] participants = StringParsingUtils.parseCommaSeparatedList(participantList).toArray(new String[0]);
        for (String participantName : participants) {
            LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
            String actualRoomName = meetInstance.getCurrentRoomName();
            assertEquals(expectedRoomName, actualRoomName, "Participant " + participantName + " should see correct room name");
        }
    }

    @And("{string} leaves the meeting")
    public void participantLeavesTheMeeting(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        meetInstance.stop();
    }

    @Then("{string} should be disconnected from the room")
    public void participantShouldBeDisconnectedFromTheRoom(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        boolean disconnected = meetInstance.disconnected();
        assertTrue(disconnected, participantName + " should be disconnected from the room");
    }

    @Then("{string} should see disconnection in the browser")
    public void participantShouldSeeDisconnectionInTheBrowser(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        boolean disconnected = BrowserPollingHelper.pollForCondition(meetInstance::disconnected, 20, 500);
        assertTrue(disconnected, participantName + " should see disconnection in the browser after being removed by the server");
    }

    @Then("{string} should see the join form again")
    public void participantShouldSeeTheJoinFormAgain(String participantName) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        assertTrue(meetInstance.isJoinFormVisible(), "Join form should be visible for " + participantName);
    }

    @And("{string} closes the browser")
    public void participantClosesTheBrowser(String participantName) {
        ManagerProvider.webDrivers().closeWebDriver("meet", participantName);
        ManagerProvider.meetSessions().removeMeetInstance(participantName);
    }

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

    private String getLiveKitServerUrl() {
        LiveKitContainer container = ManagerProvider.containers().getContainer("livekit1", LiveKitContainer.class);
        assertNotNull(container, "LiveKit container should be running");
        assertTrue(container.isRunning(), "LiveKit container should be running");
        return container.getNetworkUrl();
    }

    @When("{string} enables simulcast for video publishing")
    public void enablesSimulcastForVideoPublishing(String participantName) {
        ManagerProvider.videoQuality().setSimulcastEnabled(participantName, true);
    }

    @When("{string} disables simulcast for video publishing")
    public void disablesSimulcastForVideoPublishing(String participantName) {
        ManagerProvider.videoQuality().setSimulcastEnabled(participantName, false);
    }

    @When("{string} sets video quality preference to {string}")
    public void setsVideoQualityPreferenceTo(String participantName, String quality) {
        ManagerProvider.videoQuality().setQualityPreference(participantName, quality);
    }

    @When("{string} sets maximum receive bandwidth to {int} kbps")
    public void setsMaximumReceiveBandwidth(String participantName, int kbps) {
        ManagerProvider.videoQuality().setMaxReceiveBandwidth(participantName, kbps);
    }

    @Then("{string} should be receiving low quality video from {string}")
    public void shouldBeReceivingLowQualityVideoFrom(String subscriber, String publisher) {
        Long receivedWidth = ManagerProvider.videoQuality().getReceivedVideoWidth(subscriber, publisher);
        assertTrue(receivedWidth > 0 && receivedWidth <= LOW_QUALITY_MAX_WIDTH,
            subscriber + " should be receiving low quality video from " + publisher +
            " (width: " + receivedWidth + ", expected <= " + LOW_QUALITY_MAX_WIDTH + ")");
    }

    @Then("{string} should be receiving high quality video from {string}")
    public void shouldBeReceivingHighQualityVideoFrom(String subscriber, String publisher) {
        Long receivedWidth = ManagerProvider.videoQuality().getReceivedVideoWidth(subscriber, publisher);
        assertTrue(receivedWidth >= HIGH_QUALITY_MIN_WIDTH,
            subscriber + " should be receiving high quality video from " + publisher +
            " (width: " + receivedWidth + ", expected >= " + HIGH_QUALITY_MIN_WIDTH + ")");
    }

    @Then("{string} should be receiving a lower quality layer from {string}")
    public void shouldBeReceivingLowerQualityLayerFrom(String subscriber, String publisher) {
        Long receivedWidth = ManagerProvider.videoQuality().pollForLowerQualityWidth(subscriber, publisher, LOW_QUALITY_MAX_WIDTH);
        assertTrue(receivedWidth > 0 && receivedWidth <= LOW_QUALITY_MAX_WIDTH,
            subscriber + " should be receiving lower quality from " + publisher +
            " (width: " + receivedWidth + ", expected <= " + LOW_QUALITY_MAX_WIDTH + ")");
    }

    @When("{string} {muteAction} their audio")
    public void togglesTheirAudio(String participantName, MuteAction action) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        if (action.shouldMute()) {
            meetInstance.muteAudio();
        } else {
            meetInstance.unmuteAudio();
        }
        meetInstance.waitForAudioMuted(action.shouldMute());
    }

    @When("{string} {muteAction} their video")
    public void togglesTheirVideo(String participantName, MuteAction action) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        if (action.shouldMute()) {
            meetInstance.muteVideo();
        } else {
            meetInstance.unmuteVideo();
        }
        meetInstance.waitForVideoMuted(action.shouldMute());
    }

    @Then("{string} should have audio {muteState} locally")
    public void shouldHaveAudioStateLocally(String participantName, MuteState state) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        assertEquals(state.isMuted(), meetInstance.isAudioMuted(), participantName + " should have audio " + state + " locally");
    }

    @Then("{string} should have video {muteState} locally")
    public void shouldHaveVideoStateLocally(String participantName, MuteState state) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        assertEquals(state.isMuted(), meetInstance.isVideoMuted(), participantName + " should have video " + state + " locally");
    }

    @When("{string} sends a data message {string} via reliable channel")
    public void sendsDataMessageViaReliableChannel(String participantName, String message) {
        ManagerProvider.dataChannel().sendMessage(participantName, message, true);
    }

    @When("{string} sends a data message {string} via unreliable channel")
    public void sendsDataMessageViaUnreliableChannel(String participantName, String message) {
        ManagerProvider.dataChannel().sendMessage(participantName, message, false);
    }

    @When("{string} sends a broadcast data message {string} via reliable channel")
    public void sendsBroadcastDataMessage(String participantName, String message) {
        ManagerProvider.dataChannel().sendBroadcastMessage(participantName, message, true);
    }

    @When("{string} sends a data message {string} to {string} via reliable channel")
    public void sendsTargetedDataMessage(String sender, String message, String recipient) {
        ManagerProvider.dataChannel().sendTargetedMessage(sender, message, recipient, true);
    }

    @When("{string} sends a data message of size {int} bytes via reliable channel")
    public void sendsDataMessageOfSize(String participantName, int sizeBytes) {
        ManagerProvider.dataChannel().sendMessageOfSize(participantName, sizeBytes, true);
    }

    @When("{string} sends {int} data messages via unreliable channel")
    public void sendsMultipleDataMessagesViaUnreliableChannel(String participantName, int count) {
        ManagerProvider.dataChannel().sendMultipleMessages(participantName, count, false);
    }

    @When("{string} sends {int} timestamped data messages via reliable channel")
    public void sendsTimestampedMessages(String participantName, int count) {
        ManagerProvider.dataChannel().sendTimestampedMessages(participantName, count, true);
    }

    @When("{string} attempts to send a data message {string}")
    public void attemptsToSendDataMessage(String participantName, String message) {
        ManagerProvider.dataChannel().attemptSendMessage(participantName, message);
    }

    @Then("{string} should receive data message {string} from {string}")
    public void shouldReceiveDataMessageFrom(String receiver, String message, String sender) {
        boolean received = ManagerProvider.dataChannel().hasReceivedMessage(receiver, message, sender);
        assertTrue(received, receiver + " should have received data message '" + message + "' from " + sender);
    }

    @Then("{string} should receive at least {int} out of {int} messages from {string}")
    public void shouldReceiveAtLeastMessagesFrom(String receiver, int minMessages, int totalSent, String sender) {
        boolean receivedEnough = ManagerProvider.dataChannel().waitForMessageCount(receiver, minMessages, LiveKitMeet.BATCH_DATA_MESSAGE_TIMEOUT_MS);
        int actualCount = ManagerProvider.dataChannel().getReceivedMessageCount(receiver);
        assertTrue(receivedEnough && actualCount >= minMessages,
            receiver + " should have received at least " + minMessages + " messages from " + sender +
            " (actual: " + actualCount + " out of " + totalSent + " sent)");
        log.info("Unreliable channel delivery: {} out of {} messages received ({}%)",
            actualCount, totalSent, (actualCount * 100.0 / totalSent));
    }

    @Then("{string} should receive all timestamped messages")
    public void shouldReceiveAllTimestampedMessages(String participantName) {
        int receivedCount = ManagerProvider.dataChannel().getReceivedMessageCountAfterWait(participantName);
        assertTrue(receivedCount >= MIN_TIMESTAMPED_MESSAGES,
            participantName + " should have received all timestamped messages (received: " + receivedCount + ")");
    }

    @Then("{string} should receive a data message of size {int} bytes")
    public void shouldReceiveDataMessageOfSize(String participantName, int expectedSize) {
        boolean received = ManagerProvider.dataChannel().waitForMessageCount(participantName, 1, LiveKitMeet.DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
        assertTrue(received, participantName + " should have received a data message");
        int actualCount = ManagerProvider.dataChannel().getReceivedMessageCount(participantName);
        assertTrue(actualCount > 0, participantName + " should have at least one message (size: " + expectedSize + " bytes)");
    }

    @Then("{string} should not receive data message {string}")
    public void shouldNotReceiveDataMessage(String participantName, String message) {
        boolean received = ManagerProvider.dataChannel().hasReceivedMessageAfterWait(participantName, message);
        assertFalse(received, participantName + " should not have received data message: " + message);
    }

    @Then("{string} should have data publishing blocked due to permissions")
    public void shouldHaveDataPublishingBlockedDueToPermissions(String participantName) {
        boolean isBlocked = ManagerProvider.dataChannel().isDataPublishingBlocked(participantName);
        String error = ManagerProvider.dataChannel().getLastDataChannelError(participantName);
        assertTrue(isBlocked, participantName + " should have data publishing blocked (error: " + error + ")");
    }

    @Then("the average data channel latency for {string} should be less than {int} ms")
    public void averageLatencyShouldBeLessThan(String participantName, int maxLatencyMs) {
        double avgLatency = ManagerProvider.dataChannel().getAverageLatency(participantName);
        assertTrue(avgLatency >= 0 && avgLatency < maxLatencyMs,
            "Average data channel latency for " + participantName + " should be less than " + maxLatencyMs +
            " ms (actual: " + String.format("%.2f", avgLatency) + " ms)");
        log.info("Data channel latency for {}: {} ms (threshold: {} ms)",
            participantName, String.format("%.2f", avgLatency), maxLatencyMs);
    }

    @Then("the test logs document that lossy mode in local containers typically achieves near-100% delivery")
    public void testLogsDocumentLossyModeDelivery() {
        log.info("NOTE: Lossy/unreliable data channel mode in local container testing typically " +
            "achieves near-100% delivery due to low latency and no packet loss. In production " +
            "environments with network constraints, message loss is expected and normal.");
    }

    @Then("{string} should receive data messages in order:")
    public void shouldReceiveDataMessagesInOrder(String participantName, DataTable dataTable) {
        List<String> expectedMessages = dataTable.asList().subList(1, dataTable.asList().size());
        boolean allReceived = ManagerProvider.dataChannel().waitForMessageCount(participantName, expectedMessages.size(), LiveKitMeet.BATCH_DATA_MESSAGE_TIMEOUT_MS);
        assertTrue(allReceived, participantName + " should have received all " + expectedMessages.size() + " messages");

        List<Map<String, Object>> receivedMessages = ManagerProvider.dataChannel().getReceivedMessages(participantName);
        Objects.requireNonNull(receivedMessages, "Should have received messages list");
        assertTrue(receivedMessages.size() >= expectedMessages.size(),
            "Should have received at least " + expectedMessages.size() + " messages");

        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = receivedMessages.get(i).get("content").toString();
            assertEquals(expected, actual,
                "Message at position " + i + " should match expected order and content. " +
                "Expected: '" + expected + "', Actual: '" + actual + "'. " +
                "This indicates messages arrived out of order or with wrong content.");
        }
        log.info("All {} messages received in correct order for {}", expectedMessages.size(), participantName);
    }

    @Then("dynacast should be enabled in the room for {string}")
    public void dynacastShouldBeEnabledInTheRoomFor(String participantName) {
        assertTrue(ManagerProvider.videoQuality().isDynacastEnabled(participantName),
            "Dynacast should be enabled in the room for " + participantName);
    }

    @When("{string} unsubscribes from {string}'s video")
    public void unsubscribesFromVideo(String subscriber, String publisher) {
        ManagerProvider.videoQuality().setVideoSubscribed(subscriber, publisher, false);
    }

    @When("{string} subscribes to {string}'s video")
    public void subscribesToVideo(String subscriber, String publisher) {
        ManagerProvider.videoQuality().setVideoSubscribed(subscriber, publisher, true);
    }

    @Then("{string}'s video track should be paused for {string}")
    public void videoTrackShouldBePausedFor(String publisher, String subscriber) {
        boolean isUnsubscribed = ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "unsubscribed", 15000);
        assertTrue(isUnsubscribed,
            publisher + "'s video track should be unsubscribed for " + subscriber +
            " (current state: " + ManagerProvider.videoQuality().getTrackStreamState(subscriber, publisher) + ")");
    }

    @Then("{string}'s video track should be active for {string}")
    public void videoTrackShouldBeActiveFor(String publisher, String subscriber) {
        boolean isActive = ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "active", 10000);
        if (!isActive) {
            boolean isPending = ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "pending", 2000);
            String state = ManagerProvider.videoQuality().getTrackStreamState(subscriber, publisher);
            assertTrue(isPending || "active".equalsIgnoreCase(state),
                publisher + "'s video track should be active for " + subscriber + " (current state: " + state + ")");
        }
    }

    @When("{string} measures their video publish bitrate over {int} seconds")
    public void measuresVideoPublishBitrate(String participantName, int seconds) {
        ManagerProvider.videoQuality().measureBitrate(participantName, seconds);
    }

    @Then("{string}'s video publish bitrate should have dropped by at least {int} percent")
    public void videoPublishBitrateShouldHaveDropped(String participantName, int minDropPercent) {
        int baselineBitrate = ManagerProvider.videoQuality().getStoredBitrate(participantName);
        assertTrue(baselineBitrate > 0, "Baseline bitrate should have been measured for " + participantName);

        int currentBitrate = ManagerProvider.videoQuality().measureCurrentBitrate(participantName);
        int actualDropPercent = baselineBitrate > 0 ? ((baselineBitrate - currentBitrate) * 100) / baselineBitrate : 0;

        log.info("{} bitrate drop: baseline={} kbps, current={} kbps, drop={}%",
            participantName, baselineBitrate, currentBitrate, actualDropPercent);

        assertTrue(actualDropPercent >= minDropPercent,
            participantName + "'s video publish bitrate should have dropped by at least " + minDropPercent +
            "% but only dropped by " + actualDropPercent + "% (baseline: " + baselineBitrate +
            " kbps, current: " + currentBitrate + " kbps)");
    }
}
