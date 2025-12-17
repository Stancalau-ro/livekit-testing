Feature: Track Mute and Unmute Operations
  As a test developer
  I want to test audio and video mute/unmute operations
  So that I can verify participants can control their media tracks

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Participant can mute and unmute audio track with server verification
    Given an access token is created with identity "Alice" and room "AudioMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "AudioMuteRoom" is created using service "livekit1"

    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "AudioMuteRoom" using the access token
    And connection is established successfully for "Alice"

    Then room "AudioMuteRoom" should have 1 active participants in service "livekit1"
    And participant "Alice" should have audio track unmuted in room "AudioMuteRoom" using service "livekit1"
    And "Alice" should have audio unmuted locally

    When "Alice" mutes their audio

    Then "Alice" should have audio muted locally
    And participant "Alice" should have audio track muted in room "AudioMuteRoom" using service "livekit1"

    When "Alice" unmutes their audio

    Then "Alice" should have audio unmuted locally
    And participant "Alice" should have audio track unmuted in room "AudioMuteRoom" using service "livekit1"
    And "Alice" closes the browser

  Scenario: Participant can mute and unmute video track with server verification
    Given an access token is created with identity "Bob" and room "VideoMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "VideoMuteRoom" is created using service "livekit1"

    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "VideoMuteRoom" using the access token
    And connection is established successfully for "Bob"

    Then room "VideoMuteRoom" should have 1 active participants in service "livekit1"
    And participant "Bob" should be publishing video in room "VideoMuteRoom" using service "livekit1"
    And participant "Bob" should have video track unmuted in room "VideoMuteRoom" using service "livekit1"
    And "Bob" should have video unmuted locally

    When "Bob" mutes their video

    Then "Bob" should have video muted locally
    And participant "Bob" should have video track muted in room "VideoMuteRoom" using service "livekit1"

    When "Bob" unmutes their video

    Then "Bob" should have video unmuted locally
    And participant "Bob" should have video track unmuted in room "VideoMuteRoom" using service "livekit1"
    And "Bob" closes the browser

  Scenario: Mute state propagates to other participants
    Given an access token is created with identity "Charlie" and room "PropagationRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Diana" and room "PropagationRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "PropagationRoom" is created using service "livekit1"

    When "Charlie" opens a "Chrome" browser with LiveKit Meet page
    And "Charlie" connects to room "PropagationRoom" using the access token
    And connection is established successfully for "Charlie"

    Then room "PropagationRoom" should have 1 active participants in service "livekit1"
    And participant "Charlie" should be publishing video in room "PropagationRoom" using service "livekit1"

    When "Diana" opens a "Chrome" browser with LiveKit Meet page
    And "Diana" connects to room "PropagationRoom" using the access token
    And connection is established successfully for "Diana"

    Then room "PropagationRoom" should have 2 active participants in service "livekit1"
    And participant "Diana" should be publishing video in room "PropagationRoom" using service "livekit1"

    When "Charlie" mutes their audio
    And "Charlie" mutes their video

    Then participant "Charlie" should have audio track muted in room "PropagationRoom" using service "livekit1"
    And participant "Charlie" should have video track muted in room "PropagationRoom" using service "livekit1"

    When "Charlie" unmutes their audio
    And "Charlie" unmutes their video

    Then participant "Charlie" should have audio track unmuted in room "PropagationRoom" using service "livekit1"
    And participant "Charlie" should have video track unmuted in room "PropagationRoom" using service "livekit1"
    And "Charlie" closes the browser
    And "Diana" closes the browser

  Scenario Outline: Participant can mute and unmute audio across different browsers
    Given an access token is created with identity "Edward" and room "BrowserMuteRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "BrowserMuteRoom" is created using service "livekit1"

    When "Edward" opens a <browser> browser with LiveKit Meet page
    And "Edward" connects to room "BrowserMuteRoom" using the access token
    And connection is established successfully for "Edward"

    Then room "BrowserMuteRoom" should have 1 active participants in service "livekit1"
    And "Edward" should have audio unmuted locally

    When "Edward" mutes their audio

    Then "Edward" should have audio muted locally
    And participant "Edward" should have audio track muted in room "BrowserMuteRoom" using service "livekit1"

    When "Edward" unmutes their audio

    Then "Edward" should have audio unmuted locally
    And participant "Edward" should have audio track unmuted in room "BrowserMuteRoom" using service "livekit1"
    And "Edward" closes the browser

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
