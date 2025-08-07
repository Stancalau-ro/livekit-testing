Feature: LiveKit MinIO Recording
  As a LiveKit user
  I want to record video sessions directly to MinIO S3 storage
  So that I can store recordings in object storage instead of local filesystem

  Background:
    Given the LiveKit config is set to "with_egress"
    And a Redis server is running in a container with service name "redis"
    And a MinIO server is running in a container with service name "minio" with access key "livekit" and secret key "livekitsecret"
    And a bucket "recordings" is created in MinIO service "minio"
    And a LiveKit server is running in a container with service name "livekit1"
    And a LiveKit egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1" with S3 output to MinIO service "minio"

  Scenario: Record video from a publisher to MinIO S3 bucket using egress
    Given room "S3RecordingRoom" is created using service "livekit1"
    And an access token is created with identity "Oliver" and room "S3RecordingRoom" with grants "canPublish:true,canSubscribe:true,roomRecord:true" that expires in 5 minutes
    And an access token is created with identity "Sophia" and room "S3RecordingRoom" with grants "canSubscribe:true" that expires in 5 minutes
    
    When "Oliver" opens a "Chrome" browser with LiveKit Meet page
    And "Oliver" connects to room "S3RecordingRoom" using the access token
    And connection is established successfully for "Oliver"
    
    When "Sophia" opens a "Chrome" browser with LiveKit Meet page
    And "Sophia" connects to room "S3RecordingRoom" using the access token
    And connection is established successfully for "Sophia"
    
    Then participant "Oliver" should be publishing video in room "S3RecordingRoom" using service "livekit1"
    And participant "Sophia" should see 1 remote video tracks in room "S3RecordingRoom" using service "livekit1"
    
    When room composite recording is started for room "S3RecordingRoom" using LiveKit service "livekit1" with S3 output to bucket "recordings"
    And the recording runs for 6 seconds
    And room composite recording is stopped for room "S3RecordingRoom" using LiveKit service "livekit1"
    
    Then the recording file exists in MinIO bucket "recordings" for room "S3RecordingRoom"
    And the recording file in MinIO contains actual video content
    And no recording file exists in the local output directory for room "S3RecordingRoom"

  Scenario: Record specific participant tracks to MinIO S3 using track composite egress
    Given room "S3TrackCompositeRoom" is created using service "livekit1"
    And an access token is created with identity "James" and room "S3TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Emma" and room "S3TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "James" opens a "Chrome" browser with LiveKit Meet page
    And "James" connects to room "S3TrackCompositeRoom" using the access token
    And connection is established successfully for "James"
    
    When "Emma" opens a "Chrome" browser with LiveKit Meet page
    And "Emma" connects to room "S3TrackCompositeRoom" using the access token
    And connection is established successfully for "Emma"
    
    Then participant "James" should be publishing video in room "S3TrackCompositeRoom" using service "livekit1"
    And participant "Emma" should be publishing video in room "S3TrackCompositeRoom" using service "livekit1"
    
    When track IDs are captured for participant "James" in room "S3TrackCompositeRoom" using LiveKit service "livekit1"
    And track composite recording is started for participant "James" in room "S3TrackCompositeRoom" using LiveKit service "livekit1" with S3 output to bucket "recordings"
    And the recording runs for 6 seconds
    And track composite recording is stopped for participant "James" using LiveKit service "livekit1"
    
    Then the track composite recording file exists in MinIO bucket "recordings" for participant "James"
    And the recording file in MinIO contains actual video content
    And no track composite recording file exists in the local output directory for participant "James"

  Scenario: Record multiple participants to separate S3 prefixes
    Given room "S3MultiPrefixRoom" is created using service "livekit1"
    And an access token is created with identity "Alice" and room "S3MultiPrefixRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Bob" and room "S3MultiPrefixRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "S3MultiPrefixRoom" using the access token
    And connection is established successfully for "Alice"
    
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "S3MultiPrefixRoom" using the access token
    And connection is established successfully for "Bob"
    
    Then room "S3MultiPrefixRoom" should have 2 active participants in service "livekit1"
    
    When track IDs are captured for participant "Alice" in room "S3MultiPrefixRoom" using LiveKit service "livekit1"
    And track IDs are captured for participant "Bob" in room "S3MultiPrefixRoom" using LiveKit service "livekit1"
    And track composite recording is started for participant "Alice" in room "S3MultiPrefixRoom" using LiveKit service "livekit1" with S3 output to bucket "recordings" with prefix "alice/"
    And track composite recording is started for participant "Bob" in room "S3MultiPrefixRoom" using LiveKit service "livekit1" with S3 output to bucket "recordings" with prefix "bob/"
    And the recording runs for 6 seconds
    And track composite recording is stopped for participant "Alice" using LiveKit service "livekit1"
    And track composite recording is stopped for participant "Bob" using LiveKit service "livekit1"
    
    Then the track composite recording file exists in MinIO bucket "recordings" with prefix "alice/" for participant "Alice"
    And the track composite recording file exists in MinIO bucket "recordings" with prefix "bob/" for participant "Bob"
    And the recording files in MinIO contain actual video content