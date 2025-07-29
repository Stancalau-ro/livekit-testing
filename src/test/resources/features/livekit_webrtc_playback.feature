Feature: LiveKit WebRTC Video Playback
  As a developer
  I want to test WebRTC video playback functionality
  So that I can verify participants can subscribe to and view other participants' video streams

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario Outline: Publisher and subscriber can share video in a room
    Given an access token is created with identity "Publisher" and room "VideoRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Subscriber" and room "VideoRoom" with grants "canPublish:false,canSubscribe:true"
    And room "VideoRoom" is created using service "livekit1"

    When "Publisher" opens a <browser> browser with LiveKit Meet page
    And "Publisher" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Publisher"

    Then room "VideoRoom" should have 1 active participants in service "livekit1"
    And participant "Publisher" should be publishing video in room "VideoRoom" using service "livekit1"

    When "Subscriber" opens a <browser> browser with LiveKit Meet page
    And "Subscriber" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Subscriber"

    Then room "VideoRoom" should have 2 active participants in service "livekit1"
    And participant "Subscriber" should see 1 remote video tracks in room "VideoRoom" using service "livekit1"

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
      | "Edge"    |

  Scenario: Participant without subscribe permission cannot view other participants' video
    Given an access token is created with identity "Publisher" and room "NoViewRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "NoSubscriber" and room "NoViewRoom" with grants "canPublish:false,canSubscribe:false"
    And room "NoViewRoom" is created using service "livekit1"

    When "Publisher" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher" connects to room "NoViewRoom" using the access token
    And connection is established successfully for "Publisher"

    Then room "NoViewRoom" should have 1 active participants in service "livekit1"
    And participant "Publisher" should be publishing video in room "NoViewRoom" using service "livekit1"

    When "NoSubscriber" opens a "Chrome" browser with LiveKit Meet page
    And "NoSubscriber" connects to room "NoViewRoom" using the access token
    And connection is established successfully for "NoSubscriber"

    Then room "NoViewRoom" should have 2 active participants in service "livekit1"
    And participant "NoSubscriber" should have video subscription blocked due to permissions