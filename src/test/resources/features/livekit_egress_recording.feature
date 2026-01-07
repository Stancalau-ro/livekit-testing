Feature: LiveKit Egress Recording
  As a LiveKit user
  I want to record video sessions to local storage using the egress service
  So that I can capture and archive actual video content

  Background:
    Given the LiveKit config is set to "with_egress_hook"
    And a mock HTTP server is running in a container with service name "mockserver"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit"
    And a LiveKit egress service is running in a container with service name "egress" connected to LiveKit service "livekit"

  Scenario: Record video from a publisher to local file using egress
    Given the system creates room "EgressRecordingRoom" using service "livekit"
    And an access token is created with identity "David" and room "EgressRecordingRoom" with grants "canPublish:true,canSubscribe:true,roomRecord:true" that expires in 5 minutes
    And an access token is created with identity "Rachel" and room "EgressRecordingRoom" with grants "canSubscribe:true" that expires in 5 minutes
    
    When "David" opens a "Chrome" browser with LiveKit Meet page
    And "David" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "David"
    
    When "Rachel" opens a "Chrome" browser with LiveKit Meet page
    And "Rachel" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "Rachel"
    
    Then participant "David" should be publishing video in room "EgressRecordingRoom" using service "livekit"
    And participant "Rachel" should have 1 remote video tracks available in room "EgressRecordingRoom" using service "livekit"
    
    When the system starts room composite recording for room "EgressRecordingRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "EgressRecordingRoom"
    
    When the recording runs for 6 seconds
    And the system stops room composite recording for room "EgressRecordingRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "EgressRecordingRoom"
    
    And the recording file should exist in the output directory for room "EgressRecordingRoom"
    And the recording file should contain actual video content

  Scenario: Record specific participant tracks using track composite egress
    Given the system creates room "TrackCompositeRoom" using service "livekit"
    And an access token is created with identity "Thomas" and room "TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Jennifer" and room "TrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Thomas" opens a "Chrome" browser with LiveKit Meet page
    And "Thomas" connects to room "TrackCompositeRoom" using the access token
    And connection is established successfully for "Thomas"
    
    When "Jennifer" opens a "Chrome" browser with LiveKit Meet page
    And "Jennifer" connects to room "TrackCompositeRoom" using the access token
    And connection is established successfully for "Jennifer"
    
    Then participant "Thomas" should be publishing video in room "TrackCompositeRoom" using service "livekit"
    And participant "Jennifer" should be publishing video in room "TrackCompositeRoom" using service "livekit"
    
    When track IDs are captured for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit"
    And the system starts track composite recording for participant "Thomas" in room "TrackCompositeRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "TrackCompositeRoom"
    
    When the recording runs for 6 seconds
    And the system stops track composite recording for participant "Thomas" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "TrackCompositeRoom"
    
    And the track composite recording file should exist for participant "Thomas"
    And the recording file should contain actual video content

  Scenario: Record multiple participants in the same room using egress
    Given the system creates room "MultiParticipantRecording" using service "livekit"
    And an access token is created with identity "Steve" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Amanda" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Steve" opens a "Chrome" browser with LiveKit Meet page
    And "Steve" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Steve"
    
    When "Amanda" opens a "Chrome" browser with LiveKit Meet page  
    And "Amanda" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Amanda"
    
    Then room "MultiParticipantRecording" should have 2 active participants in service "livekit"
    
    When the system starts room composite recording for room "MultiParticipantRecording" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "MultiParticipantRecording"
    
    When the recording runs for 6 seconds
    And the system stops room composite recording for room "MultiParticipantRecording" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_ended" event for room "MultiParticipantRecording"
    
    And the recording file should exist in the output directory for room "MultiParticipantRecording"
    And the recording file should contain actual video content from multiple participants

  Scenario: Record individual tracks from multiple participants simultaneously
    Given the system creates room "MultiTrackCompositeRoom" using service "livekit"
    And an access token is created with identity "Alice" and room "MultiTrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Bob" and room "MultiTrackCompositeRoom" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "MultiTrackCompositeRoom" using the access token
    And connection is established successfully for "Alice"
    
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "MultiTrackCompositeRoom" using the access token
    And connection is established successfully for "Bob"
    
    Then room "MultiTrackCompositeRoom" should have 2 active participants in service "livekit"
    
    When track IDs are captured for participant "Alice" in room "MultiTrackCompositeRoom" using LiveKit service "livekit"
    And track IDs are captured for participant "Bob" in room "MultiTrackCompositeRoom" using LiveKit service "livekit"
    And the system starts track composite recording for participant "Alice" in room "MultiTrackCompositeRoom" using LiveKit service "livekit"
    Then "mockserver" should have received an "egress_started" event for room "MultiTrackCompositeRoom"
    
    When the system starts track composite recording for participant "Bob" in room "MultiTrackCompositeRoom" using LiveKit service "livekit"
    Then "mockserver" should have received 2 "egress_started" events for room "MultiTrackCompositeRoom"
    
    When the recording runs for 6 seconds
    And the system stops track composite recording for participant "Alice" using LiveKit service "livekit"
    And the system stops track composite recording for participant "Bob" using LiveKit service "livekit"
    
    Then "mockserver" should have received an "egress_ended" event for room "MultiTrackCompositeRoom"
    
    And the track composite recording file should exist for participant "Alice"
    And the track composite recording file should exist for participant "Bob"
    And the recording file should contain actual video content