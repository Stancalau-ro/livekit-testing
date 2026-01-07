Feature: LiveKit CLI Publisher Integration
  As a developer
  I want to test LiveKit using the official CLI tool
  So that I can simulate publishers and load test scenarios

  Background:
    Given the LiveKit config is set to "with_egress_hook"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit"
    And a LiveKit snapshot egress service is running in a container with service name "egress" connected to LiveKit service "livekit"

  Scenario: CLI load test with video publishers
    Given room "LoadTestRoom" is created using service "livekit"
    When a CLI load test publisher with 5 video publishers connects to room "LoadTestRoom" using service "livekit"
    Then room "LoadTestRoom" should have 5 active participants in service "livekit"
    
    # Capture snapshot to see what the CLI video publishers are producing
    When an on-demand snapshot is captured to local filesystem for room "LoadTestRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "LoadTestRoom"
    And the local snapshot image is valid and contains actual image data

  Scenario: CLI load test with audio publishers
    Given room "AudioTestRoom" is created using service "livekit"
    When a CLI load test publisher with 3 audio publishers connects to room "AudioTestRoom" using service "livekit"
    Then room "AudioTestRoom" should have 3 active participants in service "livekit"

  Scenario: CLI load test with mixed publishers and subscribers
    Given room "MixedLoadRoom" is created using service "livekit"
    When a CLI load test with 2 video publishers and 10 subscribers connects to room "MixedLoadRoom" using service "livekit"
    Then room "MixedLoadRoom" should have 12 active participants in service "livekit"
    
    # Capture snapshot to see the mixed publisher/subscriber setup
    When an on-demand snapshot is captured to local filesystem for room "MixedLoadRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "MixedLoadRoom"
    And the local snapshot image is valid and contains actual image data

  Scenario: CLI publisher with custom configuration
    Given room "CustomConfigRoom" is created using service "livekit"
    When a CLI load test with config "videoPublishers:4,audioPublishers:2,videoResolution:medium,simulcast:true" connects to room "CustomConfigRoom" using service "livekit"
    Then room "CustomConfigRoom" should have 4 active participants in service "livekit"
    
    # Capture snapshot to see the custom configuration with medium resolution
    When an on-demand snapshot is captured to local filesystem for room "CustomConfigRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "CustomConfigRoom"
    And the local snapshot image is valid and contains actual image data

  Scenario: CLI publisher joins room with browser participant
    Given an access token is created with identity "Alice" and room "MixedRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "CLIBot" and room "MixedRoom" with grants "canPublish:true,canSubscribe:true"
    And room "MixedRoom" is created using service "livekit"
    
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "MixedRoom" using the access token
    And connection is established successfully for "Alice"
    And "CLIBot" starts a CLI publisher to room "MixedRoom" using service "livekit"
    
    Then room "MixedRoom" should have 2 active participants in service "livekit"
    And participant "Alice" should be publishing video in room "MixedRoom" using service "livekit"
    And the CLI publisher "CLIBot" should be connected to room "MixedRoom"
    And participant "Alice" should have 1 remote video tracks available in room "MixedRoom" using service "livekit"
    And "Alice" should be receiving video from "CLIBot"

    # Capture snapshot to see browser and CLI publisher together
    When an on-demand snapshot is captured to local filesystem for room "MixedRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "MixedRoom"
    And the local snapshot image is valid and contains actual image data

  Scenario: Multiple CLI publishers in same room with snapshots
    Given an access token is created with identity "John" and room "MultiCLIRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Amy" and room "MultiCLIRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Collin" and room "MultiCLIRoom" with grants "canPublish:true,canSubscribe:true"
    And room "MultiCLIRoom" is created using service "livekit"
    
    When "John" starts a CLI publisher with "video" to room "MultiCLIRoom" using service "livekit"
    And "Amy" starts a CLI publisher with "audio" to room "MultiCLIRoom" using service "livekit"
    And "Collin" starts a CLI publisher with "a/v" to room "MultiCLIRoom" using service "livekit"
    
    Then room "MultiCLIRoom" should have 3 active participants in service "livekit"
    And the CLI publisher "John" should be connected to room "MultiCLIRoom"
    And the CLI publisher "Amy" should be connected to room "MultiCLIRoom"
    And the CLI publisher "Collin" should be connected to room "MultiCLIRoom"
    
    # Capture room snapshot to see all CLI publishers together
    When an on-demand snapshot is captured to local filesystem for room "MultiCLIRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "MultiCLIRoom"
    And the local snapshot image is valid and contains actual image data

  Scenario: Load test with progressive participant joining
    Given room "ProgressiveRoom" is created using service "livekit"
    When a CLI load test with config "videoPublishers:10,numPerSecond:2,duration:5" connects to room "ProgressiveRoom" using service "livekit"
    Then the CLI load test for room "ProgressiveRoom" should complete successfully within 10 seconds

  Scenario: Load test with speaker simulation
    Given room "SpeakerRoom" is created using service "livekit"
    When a CLI load test with config "audioPublishers:5,simulateSpeakers:true,duration:10" connects to room "SpeakerRoom" using service "livekit"
    Then room "SpeakerRoom" should have 5 active participants in service "livekit"
    And the CLI load test for room "SpeakerRoom" should complete successfully within 15 seconds

  Scenario: CLI publisher with low resolution video
    Given room "LowResRoom" is created using service "livekit"
    When a CLI load test with config "videoPublishers:3,videoResolution:low,simulcast:false" connects to room "LowResRoom" using service "livekit"
    Then room "LowResRoom" should have 3 active participants in service "livekit"
    
    # Capture snapshot to see the low resolution video quality
    When an on-demand snapshot is captured to local filesystem for room "LowResRoom" using LiveKit service "livekit"
    Then the local snapshot image file exists for room "LowResRoom"
    And the local snapshot image is valid and contains actual image data
