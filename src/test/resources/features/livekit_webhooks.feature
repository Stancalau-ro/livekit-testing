Feature: LiveKit Webhook Integration
  As a developer
  I want to test LiveKit webhook functionality
  So that I can verify webhook events are properly sent by the LiveKit server

  Background:
    Given the LiveKit config is set to "basic_hook"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Create room and verify webhook events are configured
    When room "WebhookTestRoom" is created using service "livekit1"
    And all rooms are fetched from service "livekit1"
    Then the room count should be 1