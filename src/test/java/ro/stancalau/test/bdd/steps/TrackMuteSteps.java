package ro.stancalau.test.bdd.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.steps.params.MuteAction;
import ro.stancalau.test.bdd.steps.params.MuteState;
import ro.stancalau.test.framework.selenium.LiveKitMeet;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TrackMuteSteps {

    @When("{string} {muteAction} their audio")
    public void togglesTheirAudio(String participantName, MuteAction action) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        if (action.shouldMute()) {
            meetInstance.getMedia().muteAudio();
        } else {
            meetInstance.getMedia().unmuteAudio();
        }
        meetInstance.waitForAudioMuted(action.shouldMute());
    }

    @When("{string} {muteAction} their video")
    public void togglesTheirVideo(String participantName, MuteAction action) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        if (action.shouldMute()) {
            meetInstance.getMedia().muteVideo();
        } else {
            meetInstance.getMedia().unmuteVideo();
        }
        meetInstance.waitForVideoMuted(action.shouldMute());
    }

    @Then("{string} should have audio {muteState} locally")
    public void shouldHaveAudioStateLocally(String participantName, MuteState state) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        assertEquals(state.isMuted(), meetInstance.getMedia().isAudioMuted(), participantName + " should have audio " + state + " locally");
    }

    @Then("{string} should have video {muteState} locally")
    public void shouldHaveVideoStateLocally(String participantName, MuteState state) {
        LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
        assertEquals(state.isMuted(), meetInstance.getMedia().isVideoMuted(), participantName + " should have video " + state + " locally");
    }
}
