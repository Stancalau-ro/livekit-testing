Feature: LiveKit WebRTC Video Playback
  As a developer
  I want to test WebRTC video playback functionality
  So that I can verify participants can subscribe to and view other participants' video streams

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit"

  Scenario Outline: Publisher and subscriber can share video in a room
    Given an access token is created with identity "Michael" and room "VideoRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Lisa" and room "VideoRoom" with grants "canPublish:false,canSubscribe:true"
    And room "VideoRoom" is created using service "livekit"

    When "Michael" opens a <browser> browser with LiveKit Meet page
    And "Michael" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Michael"

    Then room "VideoRoom" should have 1 active participants in service "livekit"
    And participant "Michael" should be publishing video in room "VideoRoom" using service "livekit"

    When "Lisa" opens a <browser> browser with LiveKit Meet page
    And "Lisa" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Lisa"

    Then room "VideoRoom" should have 2 active participants in service "livekit"
    And participant "Lisa" should have 1 remote video tracks available in room "VideoRoom" using service "livekit"
    And "Lisa" should be receiving video from "Michael"

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
      | "Edge"    |

  Scenario: Participant without subscribe permission cannot view other participants' video
    Given an access token is created with identity "Robert" and room "NoViewRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Karen" and room "NoViewRoom" with grants "canPublish:false,canSubscribe:false"
    And room "NoViewRoom" is created using service "livekit"

    When "Robert" opens a "Chrome" browser with LiveKit Meet page
    And "Robert" connects to room "NoViewRoom" using the access token
    And connection is established successfully for "Robert"

    Then room "NoViewRoom" should have 1 active participants in service "livekit"
    And participant "Robert" should be publishing video in room "NoViewRoom" using service "livekit"

    When "Karen" opens a "Chrome" browser with LiveKit Meet page
    And "Karen" connects to room "NoViewRoom" using the access token
    And connection is established successfully for "Karen"

    Then room "NoViewRoom" should have 2 active participants in service "livekit"
    And participant "Karen" should not be able to subscribe to video due to permissions