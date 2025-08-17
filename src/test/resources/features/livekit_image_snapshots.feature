Feature: LiveKit Image Snapshots
  As a LiveKit user
  I want to capture on-demand image snapshots from video tracks and rooms
  So that I can generate thumbnails and validate video content

  Background:
    Given the LiveKit config is set to "with_egress_hook"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit1"

  # Room snapshots capture the entire room composite view (vs individual participant track snapshots)
  Scenario: Capture on-demand room snapshots to both S3 and local filesystem
    Given a MinIO server is running in a container with service name "minio1"
    And a bucket "snapshots" is created in MinIO service "minio1"
    And a LiveKit S3 snapshot egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"
    And room "SnapshotRoom" is created using service "livekit1"
    And an access token is created with identity "Alice" and room "SnapshotRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Charlie" and room "SnapshotRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "SnapshotRoom" using the access token
    And connection is established successfully for "Alice"
    And "Charlie" opens a "Firefox" browser with LiveKit Meet page
    And "Charlie" connects to room "SnapshotRoom" using the access token
    And connection is established successfully for "Charlie"
    
    Then participant "Alice" should be publishing video in room "SnapshotRoom" using service "livekit1"
    And participant "Charlie" should be publishing video in room "SnapshotRoom" using service "livekit1"
    
    When an on-demand snapshot is captured to S3 for room "SnapshotRoom" using LiveKit service "livekit1" and MinIO service "minio1"
    And an on-demand snapshot is captured to local filesystem for room "SnapshotRoom" using LiveKit service "livekit1"
    
    Then the S3 snapshot image file exists for room "SnapshotRoom" using MinIO service "minio1"
    And the S3 snapshot image is valid and contains actual image data
    And the local snapshot image file exists for room "SnapshotRoom"
    And the local snapshot image is valid and contains actual image data

  # Participant track snapshots capture individual video tracks (vs room composite snapshots)
  Scenario: Capture on-demand participant track snapshots to both S3 and local filesystem
    Given a MinIO server is running in a container with service name "minio1"
    And a bucket "snapshots" is created in MinIO service "minio1"
    And a LiveKit S3 snapshot egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"
    And room "ParticipantSnapshotRoom" is created using service "livekit1"
    And an access token is created with identity "Bob" and room "ParticipantSnapshotRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "ParticipantSnapshotRoom" using the access token
    And connection is established successfully for "Bob"
    
    Then participant "Bob" should be publishing video in room "ParticipantSnapshotRoom" using service "livekit1"
    
    When an on-demand snapshot is captured to S3 for participant "Bob" video track in room "ParticipantSnapshotRoom" using LiveKit service "livekit1" and MinIO service "minio1"
    And an on-demand snapshot is captured to local filesystem for participant "Bob" video track in room "ParticipantSnapshotRoom" using LiveKit service "livekit1"
    
    Then the S3 snapshot image file exists for participant "Bob" using MinIO service "minio1"
    And the S3 snapshot image is valid and contains actual image data
    And the local snapshot image file exists for participant "Bob"
    And the local snapshot image is valid and contains actual image data