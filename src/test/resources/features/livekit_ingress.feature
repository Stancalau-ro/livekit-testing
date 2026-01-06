Feature: LiveKit Ingress Stream Input
  As a test developer
  I want to test ingress functionality
  So that I can verify external streams can be brought into LiveKit rooms

  Background:
    Given the LiveKit config is set to "with_ingress"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit"
    And an Ingress service "ingress" is running connected to "livekit" and "redis"

  Scenario: Create RTMP ingress for a room
    Given room "IngressRoom" is created using service "livekit"
    When "Alice" creates an RTMP ingress to room "IngressRoom" on "livekit"
    Then the ingress for "Alice" on "livekit" should have input type "RTMP_INPUT"
    And the ingress for "Alice" on "livekit" should have a valid RTMP URL
    And the ingress for "Alice" on "livekit" should have a stream key
    And the ingress for "Alice" on "livekit" should be inactive

  Scenario: Create RTMP ingress with custom display name
    Given room "CustomNameRoom" is created using service "livekit"
    When "Bob" creates an RTMP ingress to room "CustomNameRoom" on "livekit" with display name "Live Broadcast"
    Then the ingress for "Bob" on "livekit" should have participant name "Live Broadcast"

  Scenario: RTMP stream appears as participant in room
    Given room "StreamRoom" is created using service "livekit"
    And "Charlie" creates an RTMP ingress to room "StreamRoom" on "livekit"
    When "Charlie" starts streaming via RTMP to "ingress"
    Then the ingress for "Charlie" on "livekit" should be publishing within 30 seconds
    And participant "Charlie" should appear in room "StreamRoom" using service "livekit"
    When "Charlie" stops streaming to "ingress"
    Then the ingress for "Charlie" on "livekit" should be inactive within 15 seconds

  Scenario: Delete ingress removes participant
    Given room "DeleteRoom" is created using service "livekit"
    And "David" creates an RTMP ingress to room "DeleteRoom" on "livekit"
    And "David" starts streaming via RTMP to "ingress"
    And the ingress for "David" on "livekit" is publishing
    And participant "David" should appear in room "DeleteRoom" using service "livekit"
    When the ingress for "David" on "livekit" is deleted
    Then the ingress for "David" on "livekit" should not exist

  Scenario: List ingresses returns created ingresses
    Given room "ListRoom" is created using service "livekit"
    And "Emily" creates an RTMP ingress to room "ListRoom" on "livekit"
    And "Frank" creates an RTMP ingress to room "ListRoom" on "livekit"
    Then room "ListRoom" on "livekit" should have 2 ingresses
    And room "ListRoom" on "livekit" should have ingresses for "Emily, Frank"

  Scenario: Ingress can be created before room exists
    When "Grace" creates an RTMP ingress to room "FutureRoom" on "livekit"
    Then the ingress for "Grace" on "livekit" should be created successfully
    And the ingress for "Grace" on "livekit" should have room name "FutureRoom"
