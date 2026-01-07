package ro.stancalau.test.framework.selenium;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;

@Slf4j
public class WebrtcPlayback {

    public static final String START_PLAY_BUTTON_ID = "start_play_button";
    public static final String STOP_PLAY_BUTTON_ID = "stop_play_button";
    public static final String CUSTOM_PARAM = "customParam";
    public static final String CUSTOM_VALUE = "customValue";

    private final WebDriver driver;

    private String alertError;

    public WebrtcPlayback(String browser, String playerUrl, String streamName) {
        SeleniumConfig config = new SeleniumConfig(browser, null);
        driver = config.getDriver();
        driver.get(playerUrl);
        start(streamName);
    }

    public WebrtcPlayback(WebDriver driver, String playerUrl, String streamId) {
        this.driver = driver;
        driver.get(playerUrl);
        start(streamId);
    }

    private void start(String streamName) {
        driver.manage().window().setSize(new Dimension(1376, 1026));

        WebElement streamIdElement = driver.findElement(By.id("streamName"));
        streamIdElement.clear();
        streamIdElement.sendKeys(streamName);
    }

    public void play() {
        alertError = null;
        WebElement streamButton = driver.findElement(By.id(START_PLAY_BUTTON_ID));
        log.info("Clicking play button");
        streamButton.click();
        log.info("Clicked play button");
    }

    public void waitForWebrtcPlaybackConnection() throws InterruptedException {
        try {
            WebElement status = driver.findElement(By.id("stats_panel"));
            if (!status.isDisplayed()) {
                Thread.sleep(100);

                List<WebElement> elements = driver.findElements(By.xpath("//span[@data-notify-text]"));
                String notificationText =
                        elements.isEmpty() ? "" : elements.getFirst().getText();
                if (notificationText.contains("Warning")) {
                    alertError = notificationText;
                } else {
                    waitForWebrtcPlaybackConnection();
                }
            } else {
                log.info("Successfully playing.");
            }
        } catch (UnhandledAlertException e) {
            alertError = e.getAlertText();
        }
    }

    public void stopPlayAfter(int publishTimeSeconds) {
        Executors.newScheduledThreadPool(1).schedule(this::stopPlayback, publishTimeSeconds, TimeUnit.SECONDS);
    }

    public void stopPlayback() {
        log.info("Stopping webrtc playing.");
        try {
            WebElement streamButton = driver.findElement(By.id(STOP_PLAY_BUTTON_ID));
            streamButton.click();
        } catch (Exception e) {
            log.error("Stopping webrtc playing error {}", e.getMessage());
        }
    }

    public void assertPlayStopped() {
        WebElement streamButtonStart = driver.findElement(By.id(START_PLAY_BUTTON_ID));
        assertTrue(streamButtonStart.isEnabled());
        WebElement streamButtonStop = driver.findElement(By.id(STOP_PLAY_BUTTON_ID));
        assertFalse(streamButtonStop.isEnabled());
    }

    public String getWebrtcPlayErrors() {
        return alertError;
    }

    public void closeWindow() {
        driver.quit();
    }
}
