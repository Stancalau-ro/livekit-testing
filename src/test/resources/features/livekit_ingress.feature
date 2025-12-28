Feature: LiveKit Ingress Stream Input
  As a test developer
  I want to test ingress functionality
  So that I can verify external streams can be brought into LiveKit rooms

  Background:
    Given the LiveKit config is set to "with_ingress"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit1"
    And an Ingress service is running with service name "ingress1"

  Scenario: Create RTMP ingress for a room
    Given room "IngressRoom" is created using service "livekit1"
    When an RTMP ingress "studio" is created for room "IngressRoom" with identity "Alice"
    Then the ingress "studio" should have input type "RTMP_INPUT"
    And the ingress "studio" should have a valid RTMP URL
    And the ingress "studio" should have a stream key
    And the ingress "studio" should be in state inactive

  Scenario: Create RTMP ingress with custom participant name
    Given room "CustomNameRoom" is created using service "livekit1"
    When an RTMP ingress "broadcast" is created for room "CustomNameRoom" with identity "Bob" and name "Live Broadcast"
    Then the ingress "broadcast" should have participant name "Live Broadcast"

  Scenario: RTMP stream appears as participant in room
    Given room "StreamRoom" is created using service "livekit1"
    And an RTMP ingress "primary" is created for room "StreamRoom" with identity "Charlie"
    When an RTMP stream is sent to ingress "primary"
    Then the ingress "primary" should be in state publishing within 30 seconds
    And participant "Charlie" should appear in room "StreamRoom" using service "livekit1"
    When the RTMP stream is stopped
    Then the ingress "primary" should be in state inactive within 15 seconds

  Scenario: Delete ingress removes participant
    Given room "DeleteRoom" is created using service "livekit1"
    And an RTMP ingress "camera" is created for room "DeleteRoom" with identity "David"
    And an RTMP stream is sent to ingress "camera"
    And the ingress "camera" is in state publishing
    And participant "David" should appear in room "DeleteRoom" using service "livekit1"
    When ingress "camera" is deleted
    Then the ingress "camera" should not exist

  Scenario: List ingresses returns created ingresses
    Given room "ListRoom" is created using service "livekit1"
    And an RTMP ingress "alpha" is created for room "ListRoom" with identity "Emily"
    And an RTMP ingress "beta" is created for room "ListRoom" with identity "Frank"
    When ingresses are listed for room "ListRoom"
    Then the ingress list should contain "alpha"
    And the ingress list should contain "beta"
    And the ingress list should have 2 items

  Scenario: Ingress can be configured before room creation
    When an RTMP ingress "early" is created for room "FutureRoom" with identity "Grace"
    Then the ingress "early" should be created successfully
    And the ingress "early" should have room name "FutureRoom"
