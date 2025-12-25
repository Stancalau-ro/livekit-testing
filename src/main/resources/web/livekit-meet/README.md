# LiveKit Meet - WebRTC Testing Interface

A fully functional LiveKit Meet web application for testing real WebRTC video publishing and playback with LiveKit servers in Docker environments. This interface supports actual video/audio streaming, not just UI mocking.

## Features

- ✅ Real WebRTC video/audio publishing and playback
- ✅ LiveKit SDK integration for actual room connections
- ✅ URL parameter support for test automation
- ✅ Responsive design matching LiveKit Meet aesthetics
- ✅ Form validation and error handling
- ✅ Real-time connection status monitoring
- ✅ Video track management and display
- ✅ WebRTC polyfills for cross-browser compatibility
- ✅ Technical details panel with auto-scrolling logs
- ✅ Comprehensive event handling for LiveKit SDK

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

## File Structure

```
livekit-meet/
├── index.html              # Main application entry point
├── css/
│   └── styles.css          # Application styling
├── js/
│   ├── app-init.js         # Application initialization and URL parameter handling
│   ├── livekit-client.js   # LiveKit SDK wrapper and room management
│   ├── sdk-loader.js       # Dynamic SDK loading and initialization
│   ├── status-manager.js   # Connection status and UI state management
│   └── webrtc-polyfills.js # WebRTC compatibility layer
├── lib/
│   └── livekit-client/     # LiveKit client SDK versions
│       └── v2.6.4/         # Version 2.6.4
│           └── livekit-client.umd.js
└── README.md               # This documentation
```

## Design Notes

- **Modular architecture**: Separated concerns for better maintainability
- **Real WebRTC support**: Full video/audio streaming capabilities
- **LiveKit SDK integration**: Uses official LiveKit client SDK
- **Test-friendly**: Clear element IDs and comprehensive event logging
- **Production-ready**: Handles connection failures, retries, and edge cases
- **Version management**: SDK versions stored in lib/livekit-client/vX.Y.Z for easy upgrades

## SDK Version Management

The LiveKit client SDK is stored in a versioned folder structure to support:
- Easy SDK upgrades without removing old versions
- Testing against multiple SDK versions
- Clear version tracking for debugging

To add a new SDK version:
1. Download the new livekit-client.umd.js from the official release
2. Create a new folder: `lib/livekit-client/vX.Y.Z/`
3. Place the SDK file in the new folder
4. Update the script src in index.html to point to the desired version

## Customization

To modify the interface:

1. **Styling**: Edit `css/styles.css` for visual changes
2. **Behavior**: Modify JavaScript modules in `js/` directory:
   - `livekit-client.js` for room connection logic
   - `status-manager.js` for UI state management
   - `app-init.js` for initialization behavior
3. **Layout**: Adjust the HTML structure in `index.html`

## Integration with LiveKit Testing Framework

This application is specifically designed to work with the BDD testing framework in this project:

1. **Container Integration**: Use with `LiveKitContainer` instances for real server connections
2. **Token Generation**: Combine with `AccessTokenStateManager` for valid tokens with proper grants
3. **Selenium Testing**: Full WebRTC testing support with `SeleniumConfig` WebDriver instances
4. **Browser Support**: Works with containerized Chrome/Firefox browsers with media permissions
5. **VNC Recording**: Test sessions can be recorded for debugging WebRTC issues

## Example Test Scenarios

### Publishing Video
```gherkin
Given a LiveKit server is running in a container with service name "livekit1"
And an access token is created with identity "Publisher" and room "VideoRoom" with grants "canPublish:true"
When "Publisher" opens a Chrome browser with LiveKit Meet page
And "Publisher" connects to room "VideoRoom" using the access token
Then participant "Publisher" should be publishing video in room "VideoRoom"
```

### Video Playback
```gherkin
Given an access token is created with identity "Subscriber" and room "VideoRoom" with grants "canSubscribe:true"
When "Subscriber" connects to room "VideoRoom" using the access token
Then participant "Subscriber" should see remote video tracks in the room
```

## Key Differences from Mock Version

- **Real WebRTC**: Actual video/audio streaming, not simulation
- **LiveKit SDK**: Full SDK integration with event handling
- **Track Management**: Real video track publishing and subscription
- **Connection States**: Actual WebSocket connections and ICE negotiation
- **Error Handling**: Production-grade error recovery and retry logic

## Technical Details

### WebRTC Implementation
- Uses getUserMedia API for camera/microphone access
- Supports both publishing and subscribing to video tracks
- Handles ICE candidates and connection state changes
- Implements automatic reconnection on connection failures

### LiveKit SDK Events
The application handles all major LiveKit events:
- `RoomEvent.Connected` - Room connection established
- `RoomEvent.Disconnected` - Connection lost
- `RoomEvent.ParticipantConnected` - New participant joined
- `RoomEvent.TrackPublished` - New track available
- `RoomEvent.TrackSubscribed` - Successfully subscribed to track
- `RoomEvent.TrackUnsubscribed` - Track removed
- `RoomEvent.ConnectionQualityChanged` - Network quality updates

### Browser Compatibility
- Chrome/Chromium (recommended for testing)
- Firefox (with WebRTC support)
- Edge (Chromium-based)
- Safari (limited testing)

## License

This testing application follows the same Apache-2.0 license as the main project.