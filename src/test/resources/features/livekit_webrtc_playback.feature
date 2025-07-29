Feature: LiveKit WebRTC Video Playback
  As a developer
  I want to test WebRTC video playback functionality
  So that I can verify participants can subscribe to and view other participants' video streams

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Publisher and subscriber can share video in a room
    Given an access token is created with identity "Publisher" and room "VideoRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Subscriber" and room "VideoRoom" with grants "canPublish:false,canSubscribe:true"
    And room "VideoRoom" is created using service "livekit1"

    When "Publisher" opens a Chrome browser with LiveKit Meet page
    And "Publisher" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Publisher"

    Then room "VideoRoom" should have 1 active participants in service "livekit1"
    And participant "Publisher" should be publishing video in room "VideoRoom" using service "livekit1"

    When "Subscriber" opens a Chrome browser with LiveKit Meet page
    And "Subscriber" connects to room "VideoRoom" using the access token
    And connection is established successfully for "Subscriber"

    Then room "VideoRoom" should have 2 active participants in service "livekit1"
    And participant "Subscriber" should see 1 remote video tracks in room "VideoRoom" using service "livekit1"