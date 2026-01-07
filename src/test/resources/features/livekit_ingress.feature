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

  # Note: RTMP ingress uses H264 codec. Firefox in Linux Selenium containers lacks H264 support
  # due to licensing, so this test only runs on Chrome. Firefox can be tested with VP8-based streams.
  Scenario: Browser subscriber receives media from RTMP ingress
    Given room "PlaybackRoom" is created using service "livekit"
    And "Henry" creates an RTMP ingress to room "PlaybackRoom" on "livekit"
    And "Henry" starts streaming via RTMP to "ingress" with duration 120 seconds
    And the ingress for "Henry" on "livekit" is publishing within 30 seconds
    And participant "Henry" should appear in room "PlaybackRoom" using service "livekit"
    And an access token is created with identity "Ivy" and room "PlaybackRoom" with grants "canSubscribe:true"
    When "Ivy" opens a "Chrome" browser with LiveKit Meet page
    And "Ivy" connects to room "PlaybackRoom" using the access token
    And connection is established successfully for "Ivy"
    Then room "PlaybackRoom" should have 2 active participants in service "livekit"
    And participant "Ivy" should have 1 remote video tracks available in room "PlaybackRoom" using service "livekit"
    And participant "Henry" should have published audio track in room "PlaybackRoom" using service "livekit"
    And "Ivy" should be receiving video from "Henry"

  Scenario: Ingress with 720p preset produces correct video dimensions and subscriber can receive
    Given room "DimensionRoom" is created using service "livekit"
    And "Jack" creates an RTMP ingress to room "DimensionRoom" on "livekit" with video preset "H264_720P_30FPS_3_LAYERS"
    And "Jack" starts streaming via RTMP to "ingress" with resolution "1280x720" for 120 seconds
    And the ingress for "Jack" on "livekit" is publishing within 30 seconds
    And participant "Jack" video track should have maximum width 1280 in room "DimensionRoom" using service "livekit"
    And an access token is created with identity "Kate" and room "DimensionRoom" with grants "canSubscribe:true"
    When "Kate" opens a "Chrome" browser with LiveKit Meet page
    And "Kate" connects to room "DimensionRoom" using the access token
    And connection is established successfully for "Kate"
    Then participant "Kate" should have 1 remote video tracks available in room "DimensionRoom" using service "livekit"
    And "Kate" should be receiving video from "Jack"

  Scenario: Multiple browser subscribers receive ingress stream simultaneously
    Given room "MultiViewerRoom" is created using service "livekit"
    And "Leo" creates an RTMP ingress to room "MultiViewerRoom" on "livekit"
    And "Leo" starts streaming via RTMP to "ingress" with duration 120 seconds
    And the ingress for "Leo" on "livekit" is publishing within 30 seconds
    And an access token is created with identity "Mia" and room "MultiViewerRoom" with grants "canSubscribe:true,canPublish:false"
    And an access token is created with identity "Noah" and room "MultiViewerRoom" with grants "canSubscribe:true,canPublish:false"
    When "Mia" opens a "Chrome" browser with LiveKit Meet page
    And "Mia" connects to room "MultiViewerRoom" using the access token
    And connection is established successfully for "Mia"
    And participant "Mia" should appear in room "MultiViewerRoom" using service "livekit"
    When "Noah" opens a "Chrome" browser with LiveKit Meet page
    And "Noah" connects to room "MultiViewerRoom" using the access token
    And connection is established successfully for "Noah"
    And participant "Noah" should appear in room "MultiViewerRoom" using service "livekit"
    Then room "MultiViewerRoom" should have 3 active participants in service "livekit"
    And participant "Mia" should have 1 remote video tracks available in room "MultiViewerRoom" using service "livekit"
    And participant "Noah" should have 1 remote video tracks available in room "MultiViewerRoom" using service "livekit"
    And "Mia" should be receiving video from "Leo"
    And "Noah" should be receiving video from "Leo"

  Scenario: Ingress video track has correct source type
    Given room "MetadataRoom" is created using service "livekit"
    And "Olivia" creates an RTMP ingress to room "MetadataRoom" on "livekit"
    And "Olivia" starts streaming via RTMP to "ingress"
    And the ingress for "Olivia" on "livekit" is publishing within 30 seconds
    Then participant "Olivia" should be publishing video in room "MetadataRoom" using service "livekit"
    And participant "Olivia" video track should have source "CAMERA" in room "MetadataRoom" using service "livekit"
    And participant "Olivia" should have published audio track in room "MetadataRoom" using service "livekit"

  Scenario: Subscriber detects when ingress stream stops
    Given room "RemovalRoom" is created using service "livekit"
    And "Peter" creates an RTMP ingress to room "RemovalRoom" on "livekit"
    And "Peter" starts streaming via RTMP to "ingress" with duration 120 seconds
    And the ingress for "Peter" on "livekit" is publishing within 30 seconds
    And an access token is created with identity "Quinn" and room "RemovalRoom" with grants "canSubscribe:true"
    When "Quinn" opens a "Chrome" browser with LiveKit Meet page
    And "Quinn" connects to room "RemovalRoom" using the access token
    And connection is established successfully for "Quinn"
    And participant "Quinn" should have 1 remote video tracks available in room "RemovalRoom" using service "livekit"
    And "Quinn" should be receiving video from "Peter"
    When "Peter" stops streaming to "ingress"
    Then the ingress for "Peter" on "livekit" should be inactive within 15 seconds
    And participant "Peter" should not exist in room "RemovalRoom" using service "livekit"

  Scenario: Ingress with simulcast preset has multiple layers available to subscriber
    Given room "SimulcastIngressRoom" is created using service "livekit"
    And "Ryan" creates an RTMP ingress to room "SimulcastIngressRoom" on "livekit" with video preset "H264_720P_30FPS_3_LAYERS"
    And "Ryan" starts streaming via RTMP to "ingress" with resolution "1280x720" for 120 seconds
    And the ingress for "Ryan" on "livekit" is publishing within 30 seconds
    And participant "Ryan" video track should have ">=1" layers in room "SimulcastIngressRoom" using service "livekit"
    And an access token is created with identity "Sara" and room "SimulcastIngressRoom" with grants "canSubscribe:true"
    When "Sara" opens a "Chrome" browser with LiveKit Meet page
    And "Sara" connects to room "SimulcastIngressRoom" using the access token
    And connection is established successfully for "Sara"
    Then participant "Sara" should have 1 remote video tracks available in room "SimulcastIngressRoom" using service "livekit"
    And "Sara" should be receiving video from "Ryan"
