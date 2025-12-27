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
    When an RTMP ingress "test-ingress" is created for room "IngressRoom" with identity "Streamer"
    Then the ingress "test-ingress" should have input type "RTMP_INPUT"
    And the ingress "test-ingress" should have a valid RTMP URL
    And the ingress "test-ingress" should have a stream key
    And the ingress "test-ingress" should be in state "ENDPOINT_INACTIVE"

  Scenario: Create RTMP ingress with custom participant name
    Given room "CustomNameRoom" is created using service "livekit1"
    When an RTMP ingress "named-ingress" is created for room "CustomNameRoom" with identity "Broadcaster" and name "Live Broadcast"
    Then the ingress "named-ingress" should have participant name "Live Broadcast"

  Scenario: RTMP stream appears as participant in room
    Given room "StreamRoom" is created using service "livekit1"
    And an RTMP ingress "stream-ingress" is created for room "StreamRoom" with identity "RTMPStreamer"
    When an RTMP stream is sent to ingress "stream-ingress"
    Then the ingress "stream-ingress" should be in state "ENDPOINT_PUBLISHING" within 30 seconds
    And participant "RTMPStreamer" should appear in room "StreamRoom" using service "livekit1"
    When the RTMP stream is stopped
    Then the ingress "stream-ingress" should be in state "ENDPOINT_INACTIVE" within 15 seconds

  Scenario: Delete ingress removes participant
    Given room "DeleteRoom" is created using service "livekit1"
    And an RTMP ingress "delete-ingress" is created for room "DeleteRoom" with identity "ToDelete"
    And an RTMP stream is sent to ingress "delete-ingress"
    And the ingress "delete-ingress" is in state "ENDPOINT_PUBLISHING"
    And participant "ToDelete" should appear in room "DeleteRoom" using service "livekit1"
    When ingress "delete-ingress" is deleted
    Then the ingress "delete-ingress" should not exist

  Scenario: List ingresses returns created ingresses
    Given room "ListRoom" is created using service "livekit1"
    And an RTMP ingress "ingress-a" is created for room "ListRoom" with identity "StreamerA"
    And an RTMP ingress "ingress-b" is created for room "ListRoom" with identity "StreamerB"
    When ingresses are listed for room "ListRoom"
    Then the ingress list should contain "ingress-a"
    And the ingress list should contain "ingress-b"
    And the ingress list should have 2 items

  Scenario: Ingress creation for non-existent room succeeds
    When an RTMP ingress "room-ingress" is created for room "NonExistentRoom" with identity "Orphan"
    Then the ingress "room-ingress" should be created successfully
    And the ingress "room-ingress" should have room name "NonExistentRoom"
