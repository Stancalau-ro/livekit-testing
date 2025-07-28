Feature: LiveKit WebRTC Video Publishing
  As a developer
  I want to test WebRTC video publishing functionality  
  So that I can verify clients can publish video streams to LiveKit rooms

  Background:
    Given a LiveKit server is running in a container with service name "livekit1"

  Scenario: Multiple participants can join the same room
    Given an access token is created with identity "Jack" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Jill" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "MultiRoom" using service "livekit1"

    When I open a Chrome browser with LiveKit Meet page as "Jack"
    And I connect to room "MultiRoom" as "Jack" using the access token
    And I wait for successful connection for "Jack"

    Then the room "MultiRoom" should have 1 active participants in service "livekit1"

    When I open a Chrome browser with LiveKit Meet page as "Jill"
    And I connect to room "MultiRoom" as "Jill" using the access token
    And I wait for successful connection for "Jill"

    Then the room "MultiRoom" should have 2 active participants in service "livekit1"

  Scenario: Leave meeting and verify disconnection
    Given an access token is created with identity "TemporaryUser" and room "TempRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "TempRoom" using service "livekit1"
    And I open a Chrome browser with LiveKit Meet page as "TemporaryUser"
    And I connect to room "TempRoom" as "TemporaryUser" using the access token
    And I wait for successful connection for "TemporaryUser"

    When "TemporaryUser" leaves the meeting

    Then "TemporaryUser" should be disconnected from the room
    And "TemporaryUser" should see the join form again
    And the room "TemporaryUser" should have 0 active participants in service "livekit1"