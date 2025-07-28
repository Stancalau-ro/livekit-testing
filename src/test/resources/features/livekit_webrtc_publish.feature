Feature: LiveKit WebRTC Video Publishing
  As a developer
  I want to test WebRTC video publishing functionality  
  So that I can verify clients can publish video streams to LiveKit rooms

  Background:
    Given a LiveKit server is running in a container with service name "livekit1"

  Scenario: Successfully publish video to a room with Chrome browser
    Given an access token is created with identity "Publisher" and room "VideoRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "VideoRoom" using service "livekit1"
    When I open a Chrome browser with LiveKit Meet page as "Publisher"
    And I connect to room "VideoRoom" as "Publisher" using the access token
    Then the connection should be successful for "Publisher"
    And "Publisher" should be in the meeting room
    And "Publisher" should see room name "VideoRoom"

  Scenario: Publish video and verify room has participants
    Given an access token is created with identity "StreamPublisher" and room "StreamRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "StreamRoom" using service "livekit1"
    When I open a Chrome browser with LiveKit Meet page as "StreamPublisher"
    And I connect to room "StreamRoom" as "StreamPublisher" using the access token
    And I wait for successful connection for "StreamPublisher"
    Then "StreamPublisher" should be in the meeting room
    And "StreamPublisher" can toggle camera controls
    And "StreamPublisher" can toggle mute controls

  Scenario: Multiple participants can join the same room
    Given an access token is created with identity "Participant1" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Participant2" and room "MultiRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "MultiRoom" using service "livekit1"
    When I open a Chrome browser with LiveKit Meet page as "Participant1"
    And I connect to room "MultiRoom" as "Participant1" using the access token
    And I wait for successful connection for "Participant1"
    And I open a Chrome browser with LiveKit Meet page as "Participant2"  
    And I connect to room "MultiRoom" as "Participant2" using the access token
    Then participants "Participant1,Participant2" should be connected to the meeting room
    And participants "Participant1,Participant2" should see room name "MultiRoom"

  Scenario: Leave meeting and verify disconnection
    Given an access token is created with identity "TemporaryUser" and room "TempRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "TempRoom" using service "livekit1"
    When I open a Chrome browser with LiveKit Meet page as "TemporaryUser"
    And I connect to room "TempRoom" as "TemporaryUser" using the access token
    And I wait for successful connection for "TemporaryUser"
    And "TemporaryUser" leaves the meeting
    Then "TemporaryUser" should be disconnected from the room
    And "TemporaryUser" should see the join form again

  Scenario: Verify room cleanup after all participants leave
    Given an access token is created with identity "LastUser" and room "CleanupRoom" with grants "canPublish:true,canSubscribe:true"
    And I create a room "CleanupRoom" using service "livekit1"
    When I open a Chrome browser with LiveKit Meet page as "LastUser"
    And I connect to room "CleanupRoom" as "LastUser" using the access token
    And I wait for successful connection for "LastUser"
    And "LastUser" leaves the meeting
    And "LastUser" closes the browser
    Then the room should still exist in the LiveKit server
    And the room should have 0 active participants