package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;
import ro.stancalau.test.framework.util.StringParsingUtils;

@Slf4j
public class MeetingControlSteps {

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
    String[] participants =
        StringParsingUtils.parseCommaSeparatedList(participantList).toArray(new String[0]);
    for (String participantName : participants) {
      LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
      String actualRoomName = meetInstance.getCurrentRoomName();
      assertEquals(
          expectedRoomName,
          actualRoomName,
          "Participant " + participantName + " should see correct room name");
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
    boolean disconnected = BrowserPollingHelper.pollForCondition(meetInstance::disconnected);
    assertTrue(
        disconnected,
        participantName
            + " should see disconnection in the browser after being removed by the server");
  }

  @Then("{string} should see the join form again")
  public void participantShouldSeeTheJoinFormAgain(String participantName) {
    LiveKitMeet meetInstance = ManagerProvider.meetSessions().getMeetInstance(participantName);
    assertTrue(
        meetInstance.isJoinFormVisible(), "Join form should be visible for " + participantName);
  }
}
