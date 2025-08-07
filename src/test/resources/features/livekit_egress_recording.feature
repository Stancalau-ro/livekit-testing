Feature: LiveKit Egress Recording
  As a LiveKit user
  I want to record video sessions to local storage using the egress service
  So that I can capture and archive actual video content

  Background:
    Given the LiveKit config is set to "with_egress"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit1"
    And a LiveKit egress service is running in a container with service name "egress1" connected to LiveKit service "livekit1"

  Scenario: Record video from a publisher to local file using egress
    Given room "EgressRecordingRoom" is created using service "livekit1"
    And an access token is created with identity "David" and room "EgressRecordingRoom" with grants "canPublish:true,canSubscribe:true,roomRecord:true" that expires in 5 minutes
    And an access token is created with identity "Rachel" and room "EgressRecordingRoom" with grants "canSubscribe:true" that expires in 5 minutes
    
    When "David" opens a "Chrome" browser with LiveKit Meet page
    And "David" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "David"
    
    When "Rachel" opens a "Chrome" browser with LiveKit Meet page
    And "Rachel" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "Rachel"
    
    Then participant "David" should be publishing video in room "EgressRecordingRoom" using service "livekit1"
    And participant "Rachel" should see 1 remote video tracks in room "EgressRecordingRoom" using service "livekit1"
    
    When room composite recording is started for room "EgressRecordingRoom" using LiveKit service "livekit1"
    And the recording runs for 6 seconds
    And room composite recording is stopped for room "EgressRecordingRoom" using LiveKit service "livekit1"
    
    Then the recording file exists in the output directory for room "EgressRecordingRoom"
    And the recording file contains actual video content

  Scenario: Record specific participant tracks using track composite egress
    Given room "TrackCompositeRoom" is created using service "livekit1"
    And an access token is created with identity "Thomas" and room "TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Jennifer" and room "TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Thomas" opens a "Chrome" browser with LiveKit Meet page
    And "Thomas" connects to room "TrackCompositeRoom" using the access token
    And connection is established successfully for "Thomas"
    
    When "Jennifer" opens a "Chrome" browser with LiveKit Meet page
    And "Jennifer" connects to room "TrackCompositeRoom" using the access token
    And connection is established successfully for "Jennifer"
    
    Then participant "Thomas" should be publishing video in room "TrackCompositeRoom" using service "livekit1"
    And participant "Jennifer" should be publishing video in room "TrackCompositeRoom" using service "livekit1"
    
    When track IDs are captured for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit1"
    And track composite recording is started for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit1"
    And the recording runs for 6 seconds
    And track composite recording is stopped for participant "Thomas" using LiveKit service "livekit1"
    
    Then the track composite recording file exists for participant "Thomas"
    And the recording file contains actual video content

  Scenario: Record multiple participants in the same room using egress
    Given room "MultiParticipantRecording" is created using service "livekit1"
    And an access token is created with identity "Steve" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Amanda" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Steve" opens a "Chrome" browser with LiveKit Meet page
    And "Steve" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Steve"
    
    When "Amanda" opens a "Chrome" browser with LiveKit Meet page  
    And "Amanda" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Amanda"
    
    Then room "MultiParticipantRecording" should have 2 active participants in service "livekit1"
    
    When room composite recording is started for room "MultiParticipantRecording" using LiveKit service "livekit1"
    And the recording runs for 6 seconds
    And room composite recording is stopped for room "MultiParticipantRecording" using LiveKit service "livekit1"
    
    Then the recording file exists in the output directory for room "MultiParticipantRecording"
    And the recording file contains actual video content from multiple participants

  Scenario: Record individual tracks from multiple participants simultaneously
    Given room "MultiTrackCompositeRoom" is created using service "livekit1"
    And an access token is created with identity "Alice" and room "MultiTrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Bob" and room "MultiTrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "MultiTrackCompositeRoom" using the access token
    And connection is established successfully for "Alice"
    
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "MultiTrackCompositeRoom" using the access token
    And connection is established successfully for "Bob"
    
    Then room "MultiTrackCompositeRoom" should have 2 active participants in service "livekit1"
    
    When track IDs are captured for participant "Alice" in room "MultiTrackCompositeRoom" using LiveKit service "livekit1"
    And track IDs are captured for participant "Bob" in room "MultiTrackCompositeRoom" using LiveKit service "livekit1"
    And track composite recording is started for participant "Alice" in room "MultiTrackCompositeRoom" using LiveKit service "livekit1"
    And track composite recording is started for participant "Bob" in room "MultiTrackCompositeRoom" using LiveKit service "livekit1"
    And the recording runs for 6 seconds
    And track composite recording is stopped for participant "Alice" using LiveKit service "livekit1"
    And track composite recording is stopped for participant "Bob" using LiveKit service "livekit1"
    
    Then the track composite recording file exists for participant "Alice"
    And the track composite recording file exists for participant "Bob"
    And the recording file contains actual video content