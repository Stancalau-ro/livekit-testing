package ro.stancalau.test.framework.selenium;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebrtcPublish {

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

    public void waitForWebrtcPublishConnection() throws InterruptedException {

        try {
            WebElement status = driver.findElement(By.id("stats_panel"));
            if (!status.isDisplayed()) {
                Thread.sleep(100);

                List<WebElement> elements = driver.findElements(By.xpath("//span[@data-notify-text]"));
                String notificationText = elements.isEmpty()? "" : elements.getFirst().getText();
                if (notificationText.contains("Warning")) {
                    alertError = notificationText;
                } else {
                    waitForWebrtcPublishConnection();
                }
            } else {
                log.info("Successfully publishing.");
            }
        } catch (UnhandledAlertException e) {
            alertError = e.getAlertText();
        }
    }

    public void stopPublishAfter(int publishTimeSeconds) {
        Executors.newScheduledThreadPool(1).schedule(this::stopPublish, publishTimeSeconds, TimeUnit.SECONDS);
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
