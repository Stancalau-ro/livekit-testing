Feature: LiveKit Ingress Stream Recording via Egress
  As a test developer
  I want to record ingress streams to video files using egress
  So that I can verify ingress-egress integration and archive external RTMP streams

  Background:
    Given the LiveKit config is set to "with_ingress_egress"
    And a mock HTTP server is running in a container with service name "mockserver"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit"
    And an Ingress service "ingress" is running connected to "livekit" and "redis"
    And a LiveKit egress service is running in a container with service name "egress" connected to LiveKit service "livekit"

  # Note: RTMP ingress uses H264 codec. Firefox in Linux Selenium containers lacks H264 support.
  Scenario: Record RTMP ingress stream to local file using room composite egress
    Given room "IngressRecordingRoom" is created using service "livekit"
    And "Streamer" creates an RTMP ingress to room "IngressRecordingRoom" on "livekit"
    When "Streamer" starts streaming via RTMP to "ingress" with duration 120 seconds
    Then the ingress for "Streamer" on "livekit" should be publishing within 30 seconds
    And participant "Streamer" should appear in room "IngressRecordingRoom" using service "livekit"
    When the system starts room composite recording for room "IngressRecordingRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "IngressRecordingRoom"
    When the recording runs for 6 seconds
    And the system stops room composite recording for room "IngressRecordingRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "IngressRecordingRoom"
    And the recording file should exist in the output directory for room "IngressRecordingRoom"
    And the recording file should contain actual video content

  Scenario: Record specific ingress participant tracks using track composite egress
    Given room "IngressTrackRoom" is created using service "livekit"
    And "Broadcaster" creates an RTMP ingress to room "IngressTrackRoom" on "livekit"
    When "Broadcaster" starts streaming via RTMP to "ingress" with duration 120 seconds
    Then the ingress for "Broadcaster" on "livekit" should be publishing within 30 seconds
    And participant "Broadcaster" should appear in room "IngressTrackRoom" using service "livekit"
    When track IDs are captured for participant "Broadcaster" in room "IngressTrackRoom" using LiveKit service "livekit"
    And the system starts track composite recording for participant "Broadcaster" in room "IngressTrackRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "IngressTrackRoom"
    When the recording runs for 6 seconds
    And the system stops track composite recording for participant "Broadcaster" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "IngressTrackRoom"
    And the track composite recording file should exist for participant "Broadcaster"
    And the recording file should contain actual video content

  Scenario: Record room with ingress and browser participant using room composite egress
    Given room "MixedParticipantsRoom" is created using service "livekit"
    And "LiveFeed" creates an RTMP ingress to room "MixedParticipantsRoom" on "livekit"
    And "LiveFeed" starts streaming via RTMP to "ingress" with duration 120 seconds
    And the ingress for "LiveFeed" on "livekit" is publishing within 30 seconds
    And participant "LiveFeed" should appear in room "MixedParticipantsRoom" using service "livekit"
    And an access token is created with identity "Viewer" and room "MixedParticipantsRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    When "Viewer" opens a "Chrome" browser with LiveKit Meet page
    And "Viewer" connects to room "MixedParticipantsRoom" using the access token
    And connection is established successfully for "Viewer"
    Then room "MixedParticipantsRoom" should have 2 active participants in service "livekit"
    And participant "Viewer" should be publishing video in room "MixedParticipantsRoom" using service "livekit"
    And "Viewer" should be receiving video from "LiveFeed"
    When the system starts room composite recording for room "MixedParticipantsRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "MixedParticipantsRoom"
    When the recording runs for 6 seconds
    And the system stops room composite recording for room "MixedParticipantsRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "MixedParticipantsRoom"
    And the recording file should exist in the output directory for room "MixedParticipantsRoom"
    And the recording file should contain actual video content from multiple participants

  Scenario: Record multiple RTMP ingress streams in the same room
    Given room "MultiIngressRoom" is created using service "livekit"
    And "Stream1" creates an RTMP ingress to room "MultiIngressRoom" on "livekit"
    And "Stream2" creates an RTMP ingress to room "MultiIngressRoom" on "livekit"
    When "Stream1" starts streaming via RTMP to "ingress" with duration 120 seconds
    And "Stream2" starts streaming via RTMP to "ingress" with duration 120 seconds
    Then the ingress for "Stream1" on "livekit" should be publishing within 30 seconds
    And the ingress for "Stream2" on "livekit" should be publishing within 30 seconds
    And room "MultiIngressRoom" should have 2 active participants in service "livekit"
    When the system starts room composite recording for room "MultiIngressRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "MultiIngressRoom"
    When the recording runs for 6 seconds
    And the system stops room composite recording for room "MultiIngressRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "MultiIngressRoom"
    And the recording file should exist in the output directory for room "MultiIngressRoom"
    And the recording file should contain actual video content from multiple participants
