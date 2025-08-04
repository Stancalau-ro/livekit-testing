Feature: LiveKit Egress Recording
  As a LiveKit user
  I want to record video sessions to local storage using the egress service
  So that I can capture and archive actual video content

  Background:
    Given the LiveKit config is set to "with_egress"
    And a Redis server is running in a container with service name "redis"
    And a LiveKit server is running in a container with service name "livekit1"
    And a LiveKit egress service is running in a container with service name "egress1"

  Scenario: Record video from a publisher to local file using egress
    Given room "EgressRecordingRoom" is created using service "livekit1"
    And an access token is created with identity "VideoPublisher" and room "EgressRecordingRoom" with grants "canPublish:true,canSubscribe:true,roomRecord:true" that expires in 5 minutes
    And an access token is created with identity "VideoViewer" and room "EgressRecordingRoom" with grants "canSubscribe:true" that expires in 5 minutes
    
    When "VideoPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "VideoPublisher" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "VideoPublisher"
    
    When "VideoViewer" opens a "Chrome" browser with LiveKit Meet page
    And "VideoViewer" connects to room "EgressRecordingRoom" using the access token
    And connection is established successfully for "VideoViewer"
    
    Then participant "VideoPublisher" should be publishing video in room "EgressRecordingRoom" using service "livekit1"
    And participant "VideoViewer" should see 1 remote video tracks in room "EgressRecordingRoom" using service "livekit1"
    
    When room composite recording is started for room "EgressRecordingRoom" using egress service "egress1"
    And the recording runs for 15 seconds
    And room composite recording is stopped for room "EgressRecordingRoom" using egress service "egress1"
    
    Then the recording file exists in the output directory for room "EgressRecordingRoom"
    And the recording file contains actual video content

  Scenario: Record multiple participants in the same room using egress
    Given room "MultiParticipantRecording" is created using service "livekit1"
    And an access token is created with identity "Publisher1" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    And an access token is created with identity "Publisher2" and room "MultiParticipantRecording" with grants "canPublish:true,canSubscribe:true" that expires in 5 minutes
    
    When "Publisher1" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher1" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Publisher1"
    
    When "Publisher2" opens a "Chrome" browser with LiveKit Meet page  
    And "Publisher2" connects to room "MultiParticipantRecording" using the access token
    And connection is established successfully for "Publisher2"
    
    Then room "MultiParticipantRecording" should have 2 active participants in service "livekit1"
    
    When room composite recording is started for room "MultiParticipantRecording" using egress service "egress1"
    And the recording runs for 15 seconds
    And room composite recording is stopped for room "MultiParticipantRecording" using egress service "egress1"
    
    Then the recording file exists in the output directory for room "MultiParticipantRecording"
    And the recording file contains actual video content from multiple participants