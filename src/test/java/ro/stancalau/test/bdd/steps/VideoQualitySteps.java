package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoQualitySteps {

    private static final int LOW_QUALITY_MAX_WIDTH = 400;
    private static final int HIGH_QUALITY_MIN_WIDTH = 500;

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
        assertTrue(
                receivedWidth > 0 && receivedWidth <= LOW_QUALITY_MAX_WIDTH,
                subscriber
                        + " should be receiving low quality video from "
                        + publisher
                        + " (width: "
                        + receivedWidth
                        + ", expected <= "
                        + LOW_QUALITY_MAX_WIDTH
                        + ")");
    }

    @Then("{string} should be receiving high quality video from {string}")
    public void shouldBeReceivingHighQualityVideoFrom(String subscriber, String publisher) {
        Long receivedWidth = ManagerProvider.videoQuality().getReceivedVideoWidth(subscriber, publisher);
        assertTrue(
                receivedWidth >= HIGH_QUALITY_MIN_WIDTH,
                subscriber
                        + " should be receiving high quality video from "
                        + publisher
                        + " (width: "
                        + receivedWidth
                        + ", expected >= "
                        + HIGH_QUALITY_MIN_WIDTH
                        + ")");
    }

    @Then("{string} should be receiving a lower quality layer from {string}")
    public void shouldBeReceivingLowerQualityLayerFrom(String subscriber, String publisher) {
        Long receivedWidth =
                ManagerProvider.videoQuality().pollForLowerQualityWidth(subscriber, publisher, LOW_QUALITY_MAX_WIDTH);
        assertTrue(
                receivedWidth > 0 && receivedWidth <= LOW_QUALITY_MAX_WIDTH,
                subscriber
                        + " should be receiving lower quality from "
                        + publisher
                        + " (width: "
                        + receivedWidth
                        + ", expected <= "
                        + LOW_QUALITY_MAX_WIDTH
                        + ")");
    }

    @Then("dynacast should be enabled in the room for {string}")
    public void dynacastShouldBeEnabledInTheRoomFor(String participantName) {
        assertTrue(
                ManagerProvider.videoQuality().isDynacastEnabled(participantName),
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
        boolean isUnsubscribed =
                ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "unsubscribed", 15000);
        assertTrue(
                isUnsubscribed,
                publisher
                        + "'s video track should be unsubscribed for "
                        + subscriber
                        + " (current state: "
                        + ManagerProvider.videoQuality().getTrackStreamState(subscriber, publisher)
                        + ")");
    }

    @Then("{string}'s video track should be active for {string}")
    public void videoTrackShouldBeActiveFor(String publisher, String subscriber) {
        boolean isActive =
                ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "active", 10000);
        if (!isActive) {
            boolean isPending =
                    ManagerProvider.videoQuality().waitForTrackStreamState(subscriber, publisher, "pending", 2000);
            String state = ManagerProvider.videoQuality().getTrackStreamState(subscriber, publisher);
            assertTrue(
                    isPending || "active".equalsIgnoreCase(state),
                    publisher
                            + "'s video track should be active for "
                            + subscriber
                            + " (current state: "
                            + state
                            + ")");
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

        var result =
                ManagerProvider.videoQuality().pollForBitrateDrop(participantName, baselineBitrate, minDropPercent);

        assertTrue(
                result.dropAchieved(),
                participantName
                        + "'s video publish bitrate should have dropped by at least "
                        + minDropPercent
                        + "% but only dropped by "
                        + result.actualDropPercent()
                        + "% (baseline: "
                        + baselineBitrate
                        + " kbps, current: "
                        + result.currentBitrate()
                        + " kbps)");
    }
}
