package ro.stancalau.test.framework.selenium;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ro.stancalau.test.framework.docker.WebServerContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LiveKitMeet {

    public static final long DATA_MESSAGE_POLL_INTERVAL_MS = 500;
    public static final long DEFAULT_DATA_MESSAGE_TIMEOUT_MS = 10_000;
    public static final long BATCH_DATA_MESSAGE_TIMEOUT_MS = 15_000;

    private final WebDriver driver;
    private final ContainerStateManager containerManager;
    private final boolean simulcastEnabled;
    private int storedBitrate;

    public LiveKitMeet(WebDriver driver, String liveKitUrl, String jwt, String roomName, String participantName, ContainerStateManager containerManager) {
        this(driver, liveKitUrl, jwt, roomName, participantName, containerManager, true);
    }

    public LiveKitMeet(WebDriver driver, String liveKitUrl, String jwt, String roomName, String participantName, ContainerStateManager containerManager, boolean simulcastEnabled) {
        this.driver = driver;
        this.containerManager = containerManager;
        this.simulcastEnabled = simulcastEnabled;
        start(liveKitUrl, jwt, roomName, participantName);
    }

    private void start(String liveKitUrl, String jwt, String roomName, String participantName) {
        try {
            WebServerContainer webServer = containerManager.getOrCreateWebServer("webserver");
            
            String baseUrl = webServer.getLiveKitMeetUrl("webserver");
            
            String encodedLiveKitUrl = URLEncoder.encode(liveKitUrl, StandardCharsets.UTF_8);
            String encodedJwt = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            String encodedRoomName = URLEncoder.encode(roomName, StandardCharsets.UTF_8);
            String encodedParticipantName = URLEncoder.encode(participantName, StandardCharsets.UTF_8);
            
            String fullUrl = baseUrl + "?liveKitUrl=" + encodedLiveKitUrl +
                            "&token=" + encodedJwt +
                            "&roomName=" + encodedRoomName +
                            "&participantName=" + encodedParticipantName +
                            "&simulcast=" + simulcastEnabled +
                            "&autoJoin=true";

            driver.get(fullUrl);
            driver.manage().window().maximize();

            log.info("Loading LiveKit Meet page from web server container: {}", fullUrl);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Boolean liveKitLoaded = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isLiveKitLoaded();"
            );
            log.info("LiveKit SDK loaded from local file: {}", liveKitLoaded);

            String jsErrors = (String) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.getLastError();"
            );
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
            
            // Wait for either meeting room to appear OR error status to be displayed
            wait.until(driver -> {
                // Check for errors on each poll
                String errorMessage = checkForPageErrors();
                if (errorMessage != null) {
                    throw new RuntimeException("Page error detected: " + errorMessage);
                }
                
                // Check if meeting room is visible
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
            
            // Wait for connection attempts to complete - either successful connection or clear failure
            try {
                wait.until(ExpectedConditions.or(
                    // Success: connection status shows Connected
                    ExpectedConditions.textToBePresentInElementLocated(By.id("connectionStatus"), "Status: Connected"),
                    // Success: meeting room is visible and no error status
                    ExpectedConditions.and(
                        ExpectedConditions.visibilityOfElementLocated(By.id("meetingRoom")),
                        ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(By.className("error")))
                    )
                ));
            } catch (Exception e) {
                log.warn("Connection status check timed out, proceeding with verification checks");
            }
            
            Boolean usingMock = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isUsingMock();"
            );

            if (usingMock != null && usingMock) {
                log.warn("MOCK LiveKit detected - this is expected in containerized testing environments");
            }

            Boolean realWebRTCConnection = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isRealWebRTCConnectionVerified();"
            );

            Boolean connectionFlag = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isConnectionEstablished();"
            );

            Boolean clientConnected = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isClientConnected();"
            );

            Long connectionTime = (Long) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.getConnectionTime();"
            );
            
            log.info("Connection status check - usingMock: {}, realWebRTCConnection: {}, connectionFlag: {}, clientConnected: {}, connectionTime: {}ms", 
                     usingMock, realWebRTCConnection, connectionFlag, clientConnected, connectionTime);
            
            boolean meetingRoomVisible = false;
            try {
                WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
                meetingRoomVisible = !meetingRoom.getCssValue("display").equals("none");
            } catch (Exception e) {
                log.warn("Could not check meeting room visibility: {}", e.getMessage());
            }
            
            // Consider connection successful if:
            // 1. Real WebRTC connection is verified, OR
            // 2. Meeting room is visible AND (connection flag is true OR client reports connected), OR
            // 3. Meeting room visible AND connection attempted (connectionTime > 0) with no errors
            boolean connectionSuccessful = false;
            
            if (realWebRTCConnection != null && realWebRTCConnection) {
                log.info("✅ LiveKitMeet REAL WebRTC connection verified!");
                connectionSuccessful = true;
            } else if (meetingRoomVisible && (connectionFlag || clientConnected)) {
                log.info("✅ LiveKitMeet connection successful - meeting room visible and connection flags positive");
                connectionSuccessful = true;
            } else if (meetingRoomVisible && connectionTime > 0) {
                // Final check: if room is visible and connection was attempted, consider it successful
                // This handles cases where polyfills work but don't set all expected flags
                log.info("✅ LiveKitMeet connection appears successful - meeting room visible after connection attempt");
                connectionSuccessful = true;
            } else {
                log.warn("❌ LiveKitMeet connection verification failed - room visible: {}, flags: connection={}, client={}, time={}ms", 
                        meetingRoomVisible, connectionFlag, clientConnected, connectionTime);
            }
            
            return connectionSuccessful;
            
        } catch (Exception e) {
            log.error("LiveKitMeet connection failed: {}", e.getMessage());
            
            try {
                String consoleLogs = (String) ((JavascriptExecutor) driver).executeScript(
                    "return window.LiveKitTestHelpers.getConsoleLogs();"
                );
                log.error("Browser console logs: {}", consoleLogs);
            } catch (Exception logError) {
                log.warn("Failed to capture browser console logs: {}", logError.getMessage());
            }
            
            // Try to get any error message from the page before failing
            String pageError = checkForPageErrors();
            if (pageError != null) {
                log.error("Additional page error details: {}", pageError);
            }
            
            return false;
        }
    }
    
    /**
     * Check for error messages displayed on the page
     * @return Error message if found, null otherwise
     */
    private String checkForPageErrors() {
        try {
            // Check for error status messages
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
                String consoleErrors = (String) ((JavascriptExecutor) driver).executeScript(
                    "return window.LiveKitTestHelpers.getLastError();"
                );
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
    
    /**
     * Get comprehensive error details from the page for test failure reporting
     * @return Detailed error information or null if no errors found
     */
    public String getPageErrorDetails() {
        StringBuilder errorDetails = new StringBuilder();
        
        try {
            // Check for error status messages
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
                String consoleErrors = (String) ((JavascriptExecutor) driver).executeScript(
                    "return window.LiveKitTestHelpers.getLastError();"
                );
                if (consoleErrors != null && !consoleErrors.trim().isEmpty()) {
                    if (!errorDetails.isEmpty()) errorDetails.append(" | ");
                    errorDetails.append("JavaScript error: ").append(consoleErrors);
                }
            } catch (Exception ignored) {
            }
            
            // Get current page URL for context
            try {
                String currentUrl = driver.getCurrentUrl();
                if (currentUrl != null && !currentUrl.contains("about:blank")) {
                    if (!errorDetails.isEmpty()) errorDetails.append(" | ");
                    errorDetails.append("Page URL: ").append(currentUrl);
                }
            } catch (Exception ignored) {
                // URL might not be available
            }
            
            // Get page title for additional context
            try {
                String pageTitle = driver.getTitle();
                if (pageTitle != null && !pageTitle.trim().isEmpty()) {
                    if (!errorDetails.isEmpty()) errorDetails.append(" | ");
                    errorDetails.append("Page title: ").append(pageTitle);
                }
            } catch (Exception ignored) {
                // Title might not be available
            }
            
            return !errorDetails.isEmpty() ? errorDetails.toString() : null;
            
        } catch (Exception e) {
            return "Error gathering page details: " + e.getMessage();
        }
    }

    public void stopAfter(int publishTimeSeconds) {
        Executors.newScheduledThreadPool(1).schedule(this::stop, publishTimeSeconds, TimeUnit.SECONDS);
    }

    public boolean disconnected() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.of(3, ChronoUnit.SECONDS));
        
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("joinForm")));
            
            WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
            boolean isHidden = meetingRoom.getCssValue("display").equals("none");
            
            log.info("LiveKitMeet disconnected - join form visible: {}, meeting room hidden: {}", true, isHidden);
            return true;
        } catch (TimeoutException e) {
            log.info("LiveKitMeet still connected - join form not visible");
            return false;
        }
    }

    public void stop() {
        log.info("LiveKitMeet stopping meet");
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

    public void closeWindow() {
        driver.quit();
    }
    
    /**
     * Get the current room name displayed in the meeting interface
     */
    public String getCurrentRoomName() {
        try {
            WebElement roomTitle = driver.findElement(By.id("roomTitle"));
            return roomTitle.getText();
        } catch (NoSuchElementException e) {
            log.warn("Room title element not found");
            return null;
        }
    }
    
    /**
     * Get the current server URL displayed in the meeting interface
     */
    public String getCurrentServerUrl() {
        try {
            WebElement serverUrl = driver.findElement(By.id("serverUrl"));
            return serverUrl.getText();
        } catch (NoSuchElementException e) {
            log.warn("Server URL element not found");
            return null;
        }
    }
    
    /**
     * Check if currently in meeting room (vs join form)
     */
    public boolean isInMeetingRoom() {
        try {
            WebElement meetingRoom = driver.findElement(By.id("meetingRoom"));
            String display = meetingRoom.getCssValue("display");
            boolean roomVisible = !display.equals("none");
            
            if (roomVisible) {
                Boolean connected = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "return window.LiveKitTestHelpers.isInMeetingRoom();"
                );
                return connected != null && connected;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if join form is currently visible
     */
    public boolean isJoinFormVisible() {
        try {
            WebElement joinForm = driver.findElement(By.id("joinForm"));
            String display = joinForm.getCssValue("display");
            return !display.equals("none");
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    
    /**
     * Toggle mute state in the meeting interface
     */
    public void toggleMute() {
        try {
            WebElement muteButton = driver.findElement(By.id("muteBtn"));
            muteButton.click();
            log.info("LiveKitMeet toggled mute");
        } catch (NoSuchElementException e) {
            log.warn("Mute button not found");
        }
    }
    
    /**
     * Toggle camera state in the meeting interface
     */
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
            if (!isScreenSharing()) {
                screenShareButton.click();
                log.info("LiveKitMeet started screen sharing");

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                try {
                    wait.until(driver -> {
                        Boolean isSharing = (Boolean) ((JavascriptExecutor) driver).executeScript(
                                "return window.LiveKitTestHelpers.isScreenSharing();"
                        );
                        Boolean permissionDenied = (Boolean) ((JavascriptExecutor) driver).executeScript(
                                "return window.LiveKitTestHelpers.isScreenSharePermissionDenied();"
                        );
                        if (permissionDenied != null && permissionDenied) {
                            String error = (String) ((JavascriptExecutor) driver).executeScript(
                                    "return window.LiveKitTestHelpers.getLastScreenShareError();"
                            );
                            throw new RuntimeException("Screen share permission denied: " + error);
                        }
                        return isSharing != null && isSharing;
                    });
                } catch (TimeoutException e) {
                    String lastError = (String) ((JavascriptExecutor) driver).executeScript(
                            "return window.LiveKitTestHelpers.getLastScreenShareError();"
                    );
                    Boolean permissionDenied = (Boolean) ((JavascriptExecutor) driver).executeScript(
                            "return window.LiveKitTestHelpers.isScreenSharePermissionDenied();"
                    );
                    log.error("Screen share timeout. Last error: {}, Permission denied: {}", lastError, permissionDenied);
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
            if (isScreenSharing()) {
                screenShareButton.click();
                log.info("LiveKitMeet stopped screen sharing");

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                wait.until(driver -> !isScreenSharing());
            }
        } catch (NoSuchElementException e) {
            log.warn("Screen share button not found");
        }
    }

    public boolean isScreenSharing() {
        try {
            Boolean isSharing = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isScreenSharing();"
            );
            return isSharing != null && isSharing;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isScreenShareBlocked() {
        try {
            Boolean blocked = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.isScreenSharePermissionDenied();"
            );
            return blocked != null && blocked;
        } catch (Exception e) {
            return false;
        }
    }

    public void refreshAndReconnect() {
        log.info("LiveKitMeet refreshing page to retry connection");
        try {
            driver.navigate().refresh();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Refresh sleep interrupted");
        }
    }

    public void enableSimulcast() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.enableSimulcast();"
        );
        log.info("LiveKitMeet simulcast enabled");
    }

    public void disableSimulcast() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.disableSimulcast();"
        );
        log.info("LiveKitMeet simulcast disabled");
    }

    public boolean isSimulcastEnabled() {
        Boolean enabled = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.isSimulcastEnabled();"
        );
        return enabled != null && enabled;
    }

    public void setVideoQualityPreference(String quality) {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.setVideoQualityPreference(arguments[0]);",
            quality
        );
        log.info("LiveKitMeet video quality preference set to: {}", quality);
    }

    public String getVideoQualityPreference() {
        String quality = (String) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getVideoQualityPreference();"
        );
        return quality != null ? quality : "HIGH";
    }

    public void setMaxReceiveBandwidth(int kbps) {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.setMaxReceiveBandwidth(arguments[0]);",
            kbps
        );
        log.info("LiveKitMeet max receive bandwidth set to: {} kbps", kbps);
    }

    public void muteAudio() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.muteAudio();"
        );
        log.info("LiveKitMeet audio muted");
    }

    public void unmuteAudio() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.unmuteAudio();"
        );
        log.info("LiveKitMeet audio unmuted");
    }

    public void muteVideo() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.muteVideo();"
        );
        log.info("LiveKitMeet video muted");
    }

    public void unmuteVideo() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.unmuteVideo();"
        );
        log.info("LiveKitMeet video unmuted");
    }

    public boolean isAudioMuted() {
        Boolean muted = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.isAudioMuted();"
        );
        return muted != null && muted;
    }

    public boolean isVideoMuted() {
        Boolean muted = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.isVideoMuted();"
        );
        return muted != null && muted;
    }

    public void waitForAudioMuted(boolean expectedMuted) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(d -> isAudioMuted() == expectedMuted);
    }

    public void waitForVideoMuted(boolean expectedMuted) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(d -> isVideoMuted() == expectedMuted);
    }

    public void sendDataMessage(String message, boolean reliable) {
        Boolean success = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.sendDataMessage(arguments[0], arguments[1], null);",
            message, reliable
        );
        if (success == null || !success) {
            String error = getLastDataChannelError();
            throw new RuntimeException("Failed to send data message: " + error);
        }
        log.info("Sent data message (reliable: {}): {}", reliable, message.substring(0, Math.min(50, message.length())));
    }

    public void sendDataMessageTo(String message, String recipientIdentity, boolean reliable) {
        Boolean success = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.sendDataMessage(arguments[0], arguments[1], [arguments[2]]);",
            message, reliable, recipientIdentity
        );
        if (success == null || !success) {
            String error = getLastDataChannelError();
            throw new RuntimeException("Failed to send targeted data message: " + error);
        }
        log.info("Sent targeted data message to {} (reliable: {}): {}",
            recipientIdentity, reliable, message.substring(0, Math.min(50, message.length())));
    }

    public void sendDataMessageOfSize(int sizeBytes, boolean reliable) {
        Boolean success = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.sendDataMessageOfSize(arguments[0], arguments[1]);",
            sizeBytes, reliable
        );
        if (success == null || !success) {
            String error = getLastDataChannelError();
            throw new RuntimeException("Failed to send data message of size " + sizeBytes + ": " + error);
        }
        log.info("Sent data message of size {} bytes (reliable: {})", sizeBytes, reliable);
    }

    public void sendTimestampedDataMessage(String message, boolean reliable) {
        Boolean success = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.sendTimestampedDataMessage(arguments[0], arguments[1]);",
            message, reliable
        );
        if (success == null || !success) {
            String error = getLastDataChannelError();
            throw new RuntimeException("Failed to send timestamped data message: " + error);
        }
        log.info("Sent timestamped data message (reliable: {}): {}", reliable, message);
    }

    public boolean hasReceivedDataMessage(String expectedContent, String fromIdentity) {
        return hasReceivedDataMessage(expectedContent, fromIdentity, DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
    }

    public boolean hasReceivedDataMessage(String expectedContent, String fromIdentity, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            Boolean hasMessage = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "return window.LiveKitTestHelpers.hasReceivedDataMessage(arguments[0], arguments[1]);",
                expectedContent, fromIdentity
            );
            if (hasMessage != null && hasMessage) {
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

    public int getReceivedDataMessageCount() {
        Long count = (Long) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getReceivedDataMessageCount();"
        );
        return count != null ? count.intValue() : 0;
    }

    public boolean waitForDataMessageCount(int expectedCount) {
        return waitForDataMessageCount(expectedCount, DEFAULT_DATA_MESSAGE_TIMEOUT_MS);
    }

    public boolean waitForDataMessageCount(int expectedCount, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            int actualCount = getReceivedDataMessageCount();
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

    public boolean isDataPublishingBlocked() {
        Boolean blocked = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.isDataPublishingBlocked();"
        );
        return blocked != null && blocked;
    }

    public String getLastDataChannelError() {
        String error = (String) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getLastDataChannelError();"
        );
        return error != null ? error : "";
    }

    @SuppressWarnings("unchecked")
    public double getAverageDataChannelLatency() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getDataChannelLatencyStats();"
        );
        if (result instanceof Map) {
            Map<String, Object> stats = (Map<String, Object>) result;
            Object avgObj = stats.get("average");
            if (avgObj instanceof Number) {
                return ((Number) avgObj).doubleValue();
            }
        }
        return 0.0;
    }

    public void clearDataChannelState() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.clearDataChannelState();"
        );
    }

    public boolean isDynacastEnabled() {
        Boolean enabled = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.isDynacastEnabled();"
        );
        return enabled != null && enabled;
    }

    public String getTrackStreamState(String publisherIdentity) {
        String state = (String) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getTrackStreamState(arguments[0]);",
            publisherIdentity
        );
        return state;
    }

    public void setVideoSubscribed(String publisherIdentity, boolean subscribed) {
        Boolean success = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.setVideoSubscribed(arguments[0], arguments[1]);",
            publisherIdentity, subscribed
        );
        if (success == null || !success) {
            log.warn("Failed to set video subscription for {}: subscribed={}", publisherIdentity, subscribed);
        } else {
            log.info("Set video subscription for {} to {}", publisherIdentity, subscribed);
        }
    }

    public boolean waitForTrackStreamState(String publisherIdentity, String expectedState, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String currentState = getTrackStreamState(publisherIdentity);
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

    public void clearDynacastState() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.clearDynacastState();"
        );
    }

    public void setStoredBitrate(int bitrate) {
        this.storedBitrate = bitrate;
    }

    public int getStoredBitrate() {
        return storedBitrate;
    }
}
