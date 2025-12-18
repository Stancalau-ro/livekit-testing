Feature: LiveKit Room Management
  As a developer
  I want to manage LiveKit rooms through the RoomServiceClient
  So that I can create and list rooms on the LiveKit server

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Verify no rooms exist on a fresh LiveKit container
    When the system fetches all rooms from service "livekit1"
    Then the room count should be 0

  Scenario: Verify room count is 1 after creating a room
    When the system creates room "TestRoom" using service "livekit1"
    And the system fetches all rooms from service "livekit1"
    Then the room count should be 1