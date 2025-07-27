# LiveKit Meet - Local Test Clone

This is a simplified, offline clone of the LiveKit Meet interface designed for testing LiveKit containers in Docker environments without requiring internet access.

## Features

- ✅ Offline operation (no external dependencies)
- ✅ URL parameter support for automation
- ✅ Mock WebRTC interface for UI testing
- ✅ Responsive design matching LiveKit Meet aesthetics
- ✅ Form validation and error handling
- ✅ Connection simulation for testing flows

## Usage

### Direct Browser Access

1. Open `index.html` in any modern web browser
2. Fill in the connection details:
   - **LiveKit Server URL**: Your local LiveKit server (e.g., `ws://localhost:7880`)
   - **Access Token**: Valid LiveKit access token
   - **Room Name**: Target room name
   - **Participant Name**: Display name for the participant

### URL Parameters (for Automation)

You can pre-populate **all form fields** using URL parameters:

```
index.html?liveKitUrl=ws://localhost:7880&token=YOUR_TOKEN&roomName=TestRoom&participantName=TestUser&autoJoin=true
```

**All Supported Parameters:**
- `liveKitUrl` - LiveKit server WebSocket URL (default: `ws://localhost:7880`)
- `token` - Access token for authentication (default: `test`)
- `roomName` - Room to join (default: `TestRoom`)
- `participantName` - Participant display name (default: `Test User`)
- `autoJoin` - Auto-submit form on page load (set to `true` to enable)

**Parameter Behavior:**
- ✅ All fields can be populated via query string
- ✅ Query parameters override default values
- ✅ Default values are used if no parameter provided
- ✅ Empty fields get default values automatically
- ✅ `autoJoin=true` automatically submits the form after page load

### Integration with Selenium Tests

This page is designed to work with Selenium WebDriver for automated testing:

```java
// Basic usage - manual form submission
String pageUrl = "file://" + new File("src/test/resources/web/livekit-meet/index.html").getAbsolutePath();
String fullUrl = pageUrl + "?liveKitUrl=ws://localhost:7880&token=" + accessToken + 
                "&roomName=TestRoom&participantName=TestUser";

driver.get(fullUrl);
driver.findElement(By.tagName("form")).submit();

// Verify connection simulation
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("meetingRoom")));
```

```java
// Advanced usage - automatic form submission
String pageUrl = "file://" + new File("src/test/resources/web/livekit-meet/index.html").getAbsolutePath();
String autoJoinUrl = pageUrl + "?liveKitUrl=" + containerUrl + 
                    "&token=" + URLEncoder.encode(accessToken, "UTF-8") + 
                    "&roomName=" + URLEncoder.encode(roomName, "UTF-8") + 
                    "&participantName=" + URLEncoder.encode(participantName, "UTF-8") + 
                    "&autoJoin=true";

driver.get(autoJoinUrl);

// Form automatically submits, just wait for meeting room
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("meetingRoom")));

// Verify connection details
String displayedRoom = driver.findElement(By.id("roomTitle")).getText();
String displayedUrl = driver.findElement(By.id("serverUrl")).getText();
assertEquals(roomName, displayedRoom);
assertEquals(containerUrl, displayedUrl);
```

## Mock Functionality

This clone provides a **simulated** meeting interface that:

- ✅ Validates form inputs
- ✅ Simulates connection delays
- ✅ Provides visual feedback for user actions
- ✅ Maintains state for mute/camera toggles
- ✅ Updates URL with current parameters
- ❌ Does **NOT** establish real WebRTC connections
- ❌ Does **NOT** communicate with actual LiveKit servers

## File Structure

```
livekit-meet/
├── index.html          # Complete standalone HTML page
└── README.md           # This documentation
```

## Design Notes

- **Self-contained**: All CSS and JavaScript is inline for portability
- **No external dependencies**: Works completely offline
- **Framework-agnostic**: Pure HTML/CSS/JS for maximum compatibility
- **Test-friendly**: Clear element IDs and predictable behavior for automation

## Customization

To modify the interface:

1. **Styling**: Edit the `<style>` section in `index.html`
2. **Behavior**: Modify the JavaScript section for different mock responses
3. **Layout**: Adjust the HTML structure as needed

## Integration with LiveKit Testing Framework

This clone is specifically designed to work with the BDD testing framework in this project:

1. **Container Integration**: Use with `LiveKitContainer` instances
2. **Token Generation**: Combine with `AccessTokenStateManager` for valid tokens
3. **Selenium Testing**: Navigate to this page in `SeleniumConfig` WebDriver instances
4. **Room Validation**: Test room creation/listing flows before UI testing

## Example Test Scenario

```gherkin
Given a LiveKit server is running in a container with service name "livekit1"
And an access token is created with identity "TestUser" and room "TestRoom"
When I navigate to the local meet page with the generated token
And I submit the connection form
Then I should see the meeting room interface
And the connection status should show "Connected"
```

## Limitations

- This is a **mock interface** for testing UI flows
- Real video/audio functionality requires the actual LiveKit Meet application
- For production use, deploy the full LiveKit Meet from: https://github.com/livekit-examples/meet

## License

This test clone follows the same Apache-2.0 license as the original LiveKit Meet project.