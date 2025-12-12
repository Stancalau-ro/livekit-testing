Feature: LiveKit WebRTC Video Publishing
  As a developer
  I want to test WebRTC video publishing functionality  
  So that I can verify clients can publish video streams to LiveKit rooms

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Multiple participants can join the same room and publish video
    Given an access token is created with identity "Jack" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Jill" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And room "MultiRoom" is created using service "livekit1"

    When "Jack" opens a "Chrome" browser with LiveKit Meet page
    And "Jack" connects to room "MultiRoom" using the access token
    And connection is established successfully for "Jack"

    Then room "MultiRoom" should have 1 active participants in service "livekit1"
    And participant "Jack" should be publishing video in room "MultiRoom" using service "livekit1"

    When "Jill" opens a "Chrome" browser with LiveKit Meet page
    And "Jill" connects to room "MultiRoom" using the access token
    And connection is established successfully for "Jill"

    Then room "MultiRoom" should have 2 active participants in service "livekit1"
    And participant "Jill" should be publishing video in room "MultiRoom" using service "livekit1"
    And participant "Jack" should have 1 remote video tracks available in room "MultiRoom" using service "livekit1"
    And participant "Jill" should have 1 remote video tracks available in room "MultiRoom" using service "livekit1"

  Scenario: Leave meeting and verify disconnection
    Given an access token is created with identity "Sarah" and room "TempRoom" with grants "canPublish:true,canSubscribe:true"
    And room "TempRoom" is created using service "livekit1"
    And "Sarah" opens a "Chrome" browser with LiveKit Meet page
    And "Sarah" connects to room "TempRoom" using the access token
    And connection is established successfully for "Sarah"

    When "Sarah" leaves the meeting

    Then "Sarah" should be disconnected from the room
    And "Sarah" should see the join form again
    And room "Sarah" should have 0 active participants in service "livekit1"

  Scenario: Participant without publish permission can join but cannot publish video
    Given an access token is created with identity "Emily" and room "RestrictedRoom" with grants "canPublish:false,canSubscribe:true"
    And room "RestrictedRoom" is created using service "livekit1"

    When "Emily" opens a "Chrome" browser with LiveKit Meet page
    And "Emily" connects to room "RestrictedRoom" using the access token
    And connection is established successfully for "Emily"

    Then room "RestrictedRoom" should have 1 active participants in service "livekit1"
    And participant "Emily" should not be publishing video in room "RestrictedRoom" using service "livekit1"