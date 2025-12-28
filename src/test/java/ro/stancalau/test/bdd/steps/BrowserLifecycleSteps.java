package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

@Slf4j
public class BrowserLifecycleSteps {

  @After(order = 2000)
  public void tearDownBrowserLifecycleSteps(Scenario scenario) {
    if (scenario.isFailed() && ManagerProvider.webDrivers() != null) {
      log.info("Scenario failed, marking all WebDrivers as failed for proper VNC recording");
      ManagerProvider.webDrivers()
          .getAllWebDrivers()
          .keySet()
          .forEach(
              key -> {
                String[] parts = ManagerProvider.webDrivers().parseKey(key);
                if (parts != null && parts.length == 2) {
                  ManagerProvider.webDrivers().markTestFailed(parts[0], parts[1]);
                }
              });
    }
  }

  @When("{string} opens a {string} browser with LiveKit Meet page")
  public void opensABrowserWithLiveKitMeetPage(String participantId, String browser) {
    WebDriver driver =
        ManagerProvider.webDrivers().createWebDriver("meet", participantId, browser.toLowerCase());
    assertNotNull(driver, browser + " browser should be initialized for " + participantId);
  }

  @And("{string} closes the browser")
  public void participantClosesTheBrowser(String participantName) {
    ManagerProvider.webDrivers().closeWebDriver("meet", participantName);
    ManagerProvider.meetSessions().removeMeetInstance(participantName);
  }
}
