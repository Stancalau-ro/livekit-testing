package ro.stancalau.test.framework.selenium;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ro.stancalau.test.framework.capabilities.ConnectionCapability;
import ro.stancalau.test.framework.capabilities.DataChannelCapability;
import ro.stancalau.test.framework.capabilities.MediaControlCapability;
import ro.stancalau.test.framework.capabilities.MetadataCapability;
import ro.stancalau.test.framework.capabilities.SimulcastCapability;
import ro.stancalau.test.framework.capabilities.impl.ConnectionCapabilityImpl;
import ro.stancalau.test.framework.capabilities.impl.DataChannelCapabilityImpl;
import ro.stancalau.test.framework.capabilities.impl.MediaControlCapabilityImpl;
import ro.stancalau.test.framework.capabilities.impl.MetadataCapabilityImpl;
import ro.stancalau.test.framework.capabilities.impl.SimulcastCapabilityImpl;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.WebServerContainer;
import ro.stancalau.test.framework.js.JsExecutor;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.util.TestTimeoutException;

@Slf4j
public class LiveKitMeet {

  public static final long DATA_MESSAGE_POLL_INTERVAL_MS = 500;
  public static final long DEFAULT_DATA_MESSAGE_TIMEOUT_MS = 10_000;
  public static final long BATCH_DATA_MESSAGE_TIMEOUT_MS = 15_000;

  private static final long SDK_LOAD_DELAY_MS = 2000;
  private static final long STATE_STABILIZATION_DELAY_MS = 500;
  private static final long PAGE_REFRESH_DELAY_MS = 1000;

  private final WebDriver driver;
  private final ContainerStateManager containerManager;
  private final boolean simulcastEnabled;

  @Getter private String roomName;
  @Getter private String participantName;

  @Getter private final ConnectionCapability connection;
  @Getter private final MediaControlCapability media;
  @Getter private final SimulcastCapability simulcast;
  @Getter private final DataChannelCapability dataChannel;
  @Getter private final MetadataCapability metadata;

  private int storedBitrate;
  private ScheduledExecutorService stopScheduler;

  public LiveKitMeet(
      WebDriver driver,
      String liveKitUrl,
      String jwt,
      String roomName,
      String participantName,
      ContainerStateManager containerManager) {
    this(driver, liveKitUrl, jwt, roomName, participantName, containerManager, true);
  }

  public LiveKitMeet(
      WebDriver driver,
      String liveKitUrl,
      String jwt,
      String roomName,
      String participantName,
      ContainerStateManager containerManager,
      boolean simulcastEnabled) {
    this.driver = driver;
    this.containerManager = containerManager;
    this.simulcastEnabled = simulcastEnabled;

    JsExecutor jsExecutor = new JsExecutor(driver);
    this.connection = new ConnectionCapabilityImpl(jsExecutor);
    this.media = new MediaControlCapabilityImpl(jsExecutor);
    this.simulcast = new SimulcastCapabilityImpl(jsExecutor);
    this.dataChannel = new DataChannelCapabilityImpl(jsExecutor);
    this.metadata = new MetadataCapabilityImpl(jsExecutor);

    start(liveKitUrl, jwt, roomName, participantName);
  }

  private void start(String liveKitUrl, String jwt, String roomName, String participantName) {
    this.roomName = roomName;
    this.participantName = participantName;

    try {
      WebServerContainer webServer = containerManager.getOrCreateWebServer("webserver");

      String baseUrl = webServer.getLiveKitMeetUrl("webserver");

      String encodedLiveKitUrl = URLEncoder.encode(liveKitUrl, StandardCharsets.UTF_8);
      String encodedJwt = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
      String encodedRoomName = URLEncoder.encode(roomName, StandardCharsets.UTF_8);
      String encodedParticipantName = URLEncoder.encode(participantName, StandardCharsets.UTF_8);

      String jsVersion = TestConfig.getJsVersion();
      String fullUrl =
          baseUrl
              + "?liveKitUrl="
              + encodedLiveKitUrl
              + "&token="
              + encodedJwt
              + "&roomName="
              + encodedRoomName
              + "&participantName="
              + encodedParticipantName
              + "&simulcast="
              + simulcastEnabled
              + "&jsVersion="
              + jsVersion
              + "&autoJoin=true";

      driver.get(fullUrl);
      driver.manage().window().maximize();

      log.info("Loading LiveKit Meet page from web server container: {}", fullUrl);

      try {
        Thread.sleep(SDK_LOAD_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      boolean liveKitLoaded = connection.isLiveKitLoaded();
      log.info("LiveKit SDK loaded from local file: {}", liveKitLoaded);

      String jsErrors = connection.getLastError();
      if (jsErrors != null && !jsErrors.trim().isEmpty()) {
        log.warn("JavaScript errors detected: {}", jsErrors);
      }
    } catch (Exception e) {
      log.error("Failed to load LiveKit Meet page", e);
      throw new RuntimeException("Failed to load LiveKit Meet page", e);
    }
  }

  public boolean waitForConnection() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.of(15, ChronoUnit.SECONDS));
    log.info("LiveKitMeet waiting for WebRTC connection to LiveKit server (timeout: 15s)");

    try {
      checkForPageErrors();

      wait.until(
          d -> {
            String errorMessage = checkForPageErrors();
            if (errorMessage != null) {
              throw new RuntimeException("Page error detected: " + errorMessage);
            }

            try {
              WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
              return !meetingRoom.getCssValue("display").equals("none");
            } catch (Exception e) {
              return false;
            }
          });

      String finalErrorCheck = checkForPageErrors();
      if (finalErrorCheck != null) {
        log.error("Page error detected: {}", finalErrorCheck);
        return false;
      }

      try {
        wait.until(
            ExpectedConditions.or(
                ExpectedConditions.textToBePresentInElementLocated(
                    By.id("connectionStatus"), "Status: Connected"),
                ExpectedConditions.and(
                    ExpectedConditions.visibilityOfElementLocated(By.id("meetingRoom")),
                    ExpectedConditions.not(
                        ExpectedConditions.presenceOfElementLocated(By.className("error"))))));
      } catch (Exception e) {
        log.warn("Connection status check timed out, proceeding with verification checks");
      }

      boolean usingMock = connection.isUsingMock();
      if (usingMock) {
        log.warn("MOCK LiveKit detected - this is expected in containerized testing environments");
      }

      boolean realWebRTCConnection = connection.isRealWebRTCConnectionVerified();
      boolean connectionFlag = connection.isConnectionEstablished();
      boolean clientConnected = connection.isClientConnected();
      long connectionTime = connection.getConnectionTime();

      log.info(
          "Connection status check - usingMock: {}, realWebRTCConnection: {}, connectionFlag: {}, clientConnected: {}, connectionTime: {}ms",
          usingMock,
          realWebRTCConnection,
          connectionFlag,
          clientConnected,
          connectionTime);

      boolean meetingRoomVisible = false;
      try {
        WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
        meetingRoomVisible = !meetingRoom.getCssValue("display").equals("none");
      } catch (Exception e) {
        log.warn("Could not check meeting room visibility: {}", e.getMessage());
      }

      if (realWebRTCConnection) {
        log.info("LiveKitMeet REAL WebRTC connection verified!");
        return true;
      }

      if (meetingRoomVisible && (connectionFlag || clientConnected)) {
        log.info(
            "LiveKitMeet connection successful - meeting room visible and connection flags positive");
        return true;
      }

      if (meetingRoomVisible && connectionTime > 0) {
        log.info(
            "LiveKitMeet connection successful - meeting room visible after connection attempt");
        return true;
      }

      log.warn(
          "LiveKitMeet connection verification failed - room visible: {}, flags: connection={}, client={}, time={}ms",
          meetingRoomVisible,
          connectionFlag,
          clientConnected,
          connectionTime);
      return false;

    } catch (TimeoutException e) {
      String pageError = checkForPageErrors();
      String consoleLogs = null;
      try {
        consoleLogs = connection.getConsoleLogs();
      } catch (Exception logError) {
        log.warn("Failed to capture browser console logs: {}", logError.getMessage());
      }

      StringBuilder context = new StringBuilder();
      context.append("participant=").append(participantName);
      context.append(", room=").append(roomName);
      if (pageError != null) {
        context.append(", pageError=").append(pageError);
      }

      log.error("WebRTC connection wait timed out: {}", context);
      if (consoleLogs != null) {
        log.error("Browser console logs: {}", consoleLogs);
      }

      throw new TestTimeoutException(
          "WebRTC connection establishment", context.toString(), 15000, e);
    } catch (Exception e) {
      log.error("LiveKitMeet connection failed: {}", e.getMessage());

      try {
        String consoleLogs = connection.getConsoleLogs();
        log.error("Browser console logs: {}", consoleLogs);
      } catch (Exception logError) {
        log.warn("Failed to capture browser console logs: {}", logError.getMessage());
      }

      String pageError = checkForPageErrors();
      if (pageError != null) {
        log.error("Additional page error details: {}", pageError);
      }

      return false;
    }
  }

  private String checkForPageErrors() {
    try {
      List<WebElement> errorElements = driver.findElements(By.className("error"));
      for (WebElement errorElement : errorElements) {
        if (errorElement.isDisplayed() && !errorElement.getText().trim().isEmpty()) {
          String errorText = errorElement.getText().trim();
          log.error("Error element found on page: {}", errorText);
          return errorText;
        }
      }

      try {
        WebElement statusDiv = driver.findElement(By.id("status"));
        if (statusDiv.isDisplayed() && statusDiv.getAttribute("class").contains("error")) {
          String errorText = statusDiv.getText().trim();
          if (!errorText.isEmpty()) {
            log.error("Status error found: {}", errorText);
            return errorText;
          }
        }
      } catch (NoSuchElementException ignored) {
      }

      try {
        String consoleErrors = connection.getLastError();
        if (consoleErrors != null && !consoleErrors.trim().isEmpty()) {
          log.error("JavaScript console error: {}", consoleErrors);
          return "JavaScript error: " + consoleErrors;
        }
      } catch (Exception ignored) {
      }

      return null;

    } catch (Exception e) {
      log.debug("Error checking page errors: {}", e.getMessage());
      return null;
    }
  }

  public String getPageErrorDetails() {
    StringBuilder errorDetails = new StringBuilder();

    try {
      List<WebElement> errorElements = driver.findElements(By.className("error"));
      for (WebElement errorElement : errorElements) {
        if (errorElement.isDisplayed() && !errorElement.getText().trim().isEmpty()) {
          if (!errorDetails.isEmpty()) errorDetails.append(" | ");
          errorDetails.append("Error element: ").append(errorElement.getText().trim());
        }
      }

      try {
        WebElement statusDiv = driver.findElement(By.id("status"));
        if (statusDiv.isDisplayed() && statusDiv.getAttribute("class").contains("error")) {
          String errorText = statusDiv.getText().trim();
          if (!errorText.isEmpty()) {
            if (!errorDetails.isEmpty()) errorDetails.append(" | ");
            errorDetails.append("Status error: ").append(errorText);
          }
        }
      } catch (NoSuchElementException ignored) {
      }

      try {
        String consoleErrors = connection.getLastError();
        if (consoleErrors != null && !consoleErrors.trim().isEmpty()) {
          if (!errorDetails.isEmpty()) errorDetails.append(" | ");
          errorDetails.append("JavaScript error: ").append(consoleErrors);
        }
      } catch (Exception ignored) {
      }

      try {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl != null && !currentUrl.contains("about:blank")) {
          if (!errorDetails.isEmpty()) errorDetails.append(" | ");
          errorDetails.append("Page URL: ").append(currentUrl);
        }
      } catch (Exception ignored) {
      }

      try {
        String pageTitle = driver.getTitle();
        if (pageTitle != null && !pageTitle.trim().isEmpty()) {
          if (!errorDetails.isEmpty()) errorDetails.append(" | ");
          errorDetails.append("Page title: ").append(pageTitle);
        }
      } catch (Exception ignored) {
      }

      return !errorDetails.isEmpty() ? errorDetails.toString() : null;

    } catch (Exception e) {
      return "Error gathering page details: " + e.getMessage();
    }
  }

  public void stopAfter(int publishTimeSeconds) {
    stopScheduler = Executors.newScheduledThreadPool(1);
    stopScheduler.schedule(this::stop, publishTimeSeconds, TimeUnit.SECONDS);
  }

  public boolean disconnected() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.of(3, ChronoUnit.SECONDS));

    try {
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("joinForm")));

      WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
      boolean isHidden = meetingRoom.getCssValue("display").equals("none");

      log.info(
          "LiveKitMeet disconnected - join form visible: {}, meeting room hidden: {}",
          true,
          isHidden);
      return true;
    } catch (TimeoutException e) {
      log.info("LiveKitMeet still connected - join form not visible");
      return false;
    }
  }

  public void stop() {
    log.info("LiveKitMeet stopping meet");
    shutdownScheduler();
    try {
      WebElement leaveButton = driver.findElement(By.id("leaveBtn"));
      leaveButton.click();

      WebDriverWait wait = new WebDriverWait(driver, Duration.of(2, ChronoUnit.SECONDS));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("joinForm")));

      log.info("LiveKitMeet successfully left meeting");
    } catch (Exception e) {
      log.warn("Failed to properly leave meeting, navigating to blank page", e);
      driver.get("about:blank");
    }
  }

  private void shutdownScheduler() {
    if (stopScheduler != null && !stopScheduler.isShutdown()) {
      stopScheduler.shutdownNow();
      stopScheduler = null;
    }
  }

  public void closeWindow() {
    shutdownScheduler();
    driver.quit();
  }

  public String getCurrentRoomName() {
    try {
      WebElement roomTitle = driver.findElement(By.id("roomTitle"));
      return roomTitle.getText();
    } catch (NoSuchElementException e) {
      log.warn("Room title element not found");
      return null;
    }
  }

  public String getCurrentServerUrl() {
    try {
      WebElement serverUrl = driver.findElement(By.id("serverUrl"));
      return serverUrl.getText();
    } catch (NoSuchElementException e) {
      log.warn("Server URL element not found");
      return null;
    }
  }

  public boolean isInMeetingRoom() {
    try {
      WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
      String display = meetingRoom.getCssValue("display");
      boolean roomVisible = !display.equals("none");

      if (roomVisible) {
        return connection.isInMeetingRoom();
      }

      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isJoinFormVisible() {
    try {
      WebElement joinForm = driver.findElement(By.id("joinForm"));
      String display = joinForm.getCssValue("display");
      return !display.equals("none");
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  public void toggleMute() {
    try {
      WebElement muteButton = driver.findElement(By.id("muteBtn"));
      muteButton.click();
      log.info("LiveKitMeet toggled mute");
    } catch (NoSuchElementException e) {
      log.warn("Mute button not found");
    }
  }

  public void toggleCamera() {
    try {
      WebElement cameraButton = driver.findElement(By.id("cameraBtn"));
      cameraButton.click();
      log.info("LiveKitMeet toggled camera");
    } catch (NoSuchElementException e) {
      log.warn("Camera button not found");
    }
  }

  public void startScreenShare() {
    try {
      WebElement screenShareButton = driver.findElement(By.id("screenShareBtn"));
      if (!media.isScreenSharing()) {
        screenShareButton.click();
        log.info("LiveKitMeet started screen sharing");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
          wait.until(
              d -> {
                if (media.isScreenShareBlocked()) {
                  String error = media.getLastScreenShareError();
                  throw new RuntimeException("Screen share permission denied: " + error);
                }
                return media.isScreenSharing();
              });
        } catch (TimeoutException e) {
          String lastError = media.getLastScreenShareError();
          boolean permissionDenied = media.isScreenShareBlocked();
          log.error(
              "Screen share timeout. Last error: {}, Permission denied: {}",
              lastError,
              permissionDenied);
          throw e;
        }
      }
    } catch (NoSuchElementException e) {
      log.warn("Screen share button not found");
    }
  }

  public void stopScreenShare() {
    try {
      WebElement screenShareButton = driver.findElement(By.id("screenShareBtn"));
      if (media.isScreenSharing()) {
        screenShareButton.click();
        log.info("LiveKitMeet stopped screen sharing");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(d -> !media.isScreenSharing());
      }
    } catch (NoSuchElementException e) {
      log.warn("Screen share button not found");
    }
  }

  public void refreshAndReconnect() {
    log.info("LiveKitMeet refreshing page to retry connection");
    try {
      driver.navigate().refresh();
      Thread.sleep(PAGE_REFRESH_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Refresh sleep interrupted");
    }
  }

  public void waitForAudioMuted(boolean expectedMuted) {
    try {
      Thread.sleep(STATE_STABILIZATION_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(d -> media.isAudioMuted() == expectedMuted);
  }

  public void waitForVideoMuted(boolean expectedMuted) {
    try {
      Thread.sleep(STATE_STABILIZATION_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(d -> media.isVideoMuted() == expectedMuted);
  }

  public boolean hasReceivedDataMessage(String expectedContent, String fromIdentity) {
    return hasReceivedDataMessage(expectedContent, fromIdentity, DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
  }

  public boolean hasReceivedDataMessage(
      String expectedContent, String fromIdentity, long timeoutMs) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (dataChannel.hasReceivedDataMessage(expectedContent, fromIdentity)) {
        return true;
      }
      try {
        Thread.sleep(DATA_MESSAGE_POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  public boolean waitForDataMessageCount(int expectedCount) {
    return waitForDataMessageCount(expectedCount, DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
  }

  public boolean waitForDataMessageCount(int expectedCount, long timeoutMs) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      int actualCount = dataChannel.getReceivedDataMessageCount();
      if (actualCount >= expectedCount) {
        return true;
      }
      try {
        Thread.sleep(DATA_MESSAGE_POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  public boolean waitForTrackStreamState(
      String publisherIdentity, String expectedState, long timeoutMs) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      String currentState = simulcast.getTrackStreamState(publisherIdentity);
      if (expectedState.equalsIgnoreCase(currentState)) {
        log.info("Track stream state for {} reached: {}", publisherIdentity, expectedState);
        return true;
      }
      try {
        Thread.sleep(DATA_MESSAGE_POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    log.warn("Timeout waiting for track stream state {} for {}", expectedState, publisherIdentity);
    return false;
  }

  public void setStoredBitrate(int bitrate) {
    this.storedBitrate = bitrate;
  }

  public int getStoredBitrate() {
    return storedBitrate;
  }
}
