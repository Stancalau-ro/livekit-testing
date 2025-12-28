package ro.stancalau.test.framework.selenium;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

@Slf4j
public class WebrtcPublish {

  private static final long PUBLISH_CONNECTION_TIMEOUT_MS = 30_000;
  private static final long POLL_INTERVAL_MS = 100;

  private final WebDriver driver;

  private String alertError;

  public WebrtcPublish(String browser, String publisherUrl, String streamName) {
    SeleniumConfig config = new SeleniumConfig(browser, null);
    driver = config.getDriver();
    start(publisherUrl, streamName);
  }

  public WebrtcPublish(WebDriver driver, String publisherUrl, String streamId) {
    this.driver = driver;
    start(publisherUrl, streamId);
  }

  private void start(String publisherUrl, String streamId) {
    driver.get(publisherUrl);
    log.info("Creating publisher for {} streamId {}", publisherUrl, streamId);
    driver.manage().window().setSize(new Dimension(1376, 1026));

    WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(10));
    w.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("webrtc-publish-frame"));

    WebElement streamIdElement = driver.findElement(By.id("streamId"));
    streamIdElement.clear();
    streamIdElement.sendKeys(streamId);
  }

  public void publish() {
    WebElement streamButton = driver.findElement(By.id("start_publish_button"));
    log.info("Clicking publish button");
    streamButton.click();
    log.info("Clicked publish button");
  }

  public void waitForWebrtcPublishConnection() {
    BrowserPollingHelper.pollForConditionOrThrow(
        this::checkPublishConnectionStatus,
        PUBLISH_CONNECTION_TIMEOUT_MS,
        POLL_INTERVAL_MS,
        "waitForWebrtcPublishConnection",
        "stats_panel not displayed");
  }

  private Boolean checkPublishConnectionStatus() {
    try {
      WebElement status = driver.findElement(By.id("stats_panel"));
      if (status.isDisplayed()) {
        log.info("Successfully publishing.");
        return true;
      }

      List<WebElement> elements = driver.findElements(By.xpath("//span[@data-notify-text]"));
      String notificationText = elements.isEmpty() ? "" : elements.getFirst().getText();
      if (notificationText.contains("Warning")) {
        alertError = notificationText;
        return true;
      }
    } catch (UnhandledAlertException e) {
      alertError = e.getAlertText();
      return true;
    }
    return false;
  }

  public void stopPublishAfter(int publishTimeSeconds) {
    Executors.newScheduledThreadPool(1)
        .schedule(this::stopPublish, publishTimeSeconds, TimeUnit.SECONDS);
  }

  public void stopPublish() {
    log.info("Stopping webrtc publish.");
    try {
      WebElement streamButton = driver.findElement(By.id("stop_publish_button"));
      streamButton.click();
    } catch (Exception e) {
    }
  }

  public String getWebrtcPublishErrors() {
    return alertError;
  }

  public void closeWindow() {
    driver.quit();
  }
}
