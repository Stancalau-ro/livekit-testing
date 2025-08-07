Feature: LiveKit Participant Removal
  As a developer
  I want to test LiveKit's participant removal functionality via server API
  So that I can verify backend services can forcibly disconnect participants and all events are properly triggered

  Background:
    Given the LiveKit config is set to "basic_hook"
    And a mock HTTP server is running in a container with service name "mockserver1"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Backend service removes participant from room
    When room "RemovalTestRoom" is created using service "livekit1"
    Then "mockserver1" should have received a "room_started" event for room "RemovalTestRoom"
    
    Given an access token is created with identity "Daniel" and room "RemovalTestRoom" with grants "canPublish:true,canSubscribe:true"
    When "Daniel" opens a "Chrome" browser with LiveKit Meet page
    And "Daniel" connects to room "RemovalTestRoom" using the access token
    And connection is established successfully for "Daniel"
    Then "mockserver1" should have received a "participant_joined" event for participant "Daniel" in room "RemovalTestRoom"
    And "mockserver1" should have received a "track_published" event for track type "VIDEO" in room "RemovalTestRoom"
    
    When participant "Daniel" is removed from room "RemovalTestRoom" using service "livekit1"
    Then participant "Daniel" should not exist in room "RemovalTestRoom" using service "livekit1"
    And "Daniel" should see disconnection in the browser
    And "mockserver1" should have received a "track_unpublished" event for track type "VIDEO" in room "RemovalTestRoom"
    And "mockserver1" should have received a "track_unpublished" event for track type "AUDIO" in room "RemovalTestRoom"
    And "mockserver1" should have received a "participant_left" event for participant "Daniel" in room "RemovalTestRoom"

  Scenario: Backend service removes specific participant from room with multiple participants
    When room "MultiParticipantRoom" is created using service "livekit1"
    
    Given an access token is created with identity "Alice" and room "MultiParticipantRoom" with grants "canPublish:true,canSubscribe:true"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "MultiParticipantRoom" using the access token
    And connection is established successfully for "Alice"
    
    Given an access token is created with identity "Bob" and room "MultiParticipantRoom" with grants "canPublish:true,canSubscribe:true"
    When "Bob" opens a "Firefox" browser with LiveKit Meet page
    And "Bob" connects to room "MultiParticipantRoom" using the access token
    And connection is established successfully for "Bob"
    
    Given an access token is created with identity "Charlie" and room "MultiParticipantRoom" with grants "canPublish:true,canSubscribe:true"
    When "Charlie" opens a "Edge" browser with LiveKit Meet page
    And "Charlie" connects to room "MultiParticipantRoom" using the access token
    And connection is established successfully for "Charlie"
    
    Then room "MultiParticipantRoom" should have 3 active participants in service "livekit1"
    
    When participant "Bob" is removed from room "MultiParticipantRoom" using service "livekit1"
    Then participant "Bob" should not exist in room "MultiParticipantRoom" using service "livekit1"
    And "Bob" should see disconnection in the browser
    And room "MultiParticipantRoom" should have 2 active participants in service "livekit1"
    And participant "Alice" should be publishing video in room "MultiParticipantRoom" using service "livekit1"
    And participant "Charlie" should be publishing video in room "MultiParticipantRoom" using service "livekit1"
    And "mockserver1" should have received a "participant_left" event for participant "Bob" in room "MultiParticipantRoom"

  Scenario: Verify webhook events order for backend-initiated disconnection
    When room "WebhookOrderRoom" is created using service "livekit1"
    
    Given an access token is created with identity "Peter" and room "WebhookOrderRoom" with grants "canPublish:true,canSubscribe:true"
    When "Peter" opens a "Chrome" browser with LiveKit Meet page
    And "Peter" connects to room "WebhookOrderRoom" using the access token
    And connection is established successfully for "Peter"
    
    When "mockserver1" webhook events are cleared
    
    When participant "Peter" is removed from room "WebhookOrderRoom" using service "livekit1"
    Then "Peter" should see disconnection in the browser
    And "mockserver1" should have received exactly 3 webhook events
    And "mockserver1" should have received a "track_unpublished" event for track type "VIDEO" in room "WebhookOrderRoom"
    And "mockserver1" should have received a "track_unpublished" event for track type "AUDIO" in room "WebhookOrderRoom"
    And "mockserver1" should have received a "participant_left" event for participant "Peter" in room "WebhookOrderRoom"

  Scenario: Removed participant can rejoin with valid token after backend removal
    When room "RejoinRoom" is created using service "livekit1"
    
    Given an access token is created with identity "Marcus" and room "RejoinRoom" with grants "canPublish:true,canSubscribe:true"
    When "Marcus" opens a "Chrome" browser with LiveKit Meet page
    And "Marcus" connects to room "RejoinRoom" using the access token
    And connection is established successfully for "Marcus"
    Then room "RejoinRoom" should have 1 active participants in service "livekit1"
    
    When participant "Marcus" is removed from room "RejoinRoom" using service "livekit1"
    Then "Marcus" should see disconnection in the browser
    And room "RejoinRoom" should have 0 active participants in service "livekit1"
    
    When "mockserver1" webhook events are cleared
    
    When "Marcus" connects to room "RejoinRoom" using the access token
    And connection is established successfully for "Marcus"
    Then room "RejoinRoom" should have 1 active participants in service "livekit1"
    And "mockserver1" should have received a "participant_joined" event for participant "Marcus" in room "RejoinRoom"
    And "mockserver1" should have received a "track_published" event for track type "VIDEO" in room "RejoinRoom"

  Scenario: Backend service removes participant with custom attributes
    When room "AttributesRemovalRoom" is created using service "livekit1"
    
    Given an access token is created with identity "Christopher" and room "AttributesRemovalRoom" with grants "canPublish:true,canSubscribe:true" and attributes "role=presenter,department=sales,session=Q4-2024"
    When "Christopher" opens a "Chrome" browser with LiveKit Meet page
    And "Christopher" connects to room "AttributesRemovalRoom" using the access token
    And connection is established successfully for "Christopher"
    Then "mockserver1" should have received a "participant_joined" event for participant "Christopher" in room "AttributesRemovalRoom" with attributes "role=presenter,department=sales,session=Q4-2024"
    
    When participant "Christopher" is removed from room "AttributesRemovalRoom" using service "livekit1"
    Then "Christopher" should see disconnection in the browser
    And "mockserver1" should have received a "participant_left" event for participant "Christopher" in room "AttributesRemovalRoom" with attributes "role=presenter,department=sales,session=Q4-2024"

  Scenario: Backend removal during active video publishing
    When room "ActivePublishingRoom" is created using service "livekit1"
    
    Given an access token is created with identity "William" and room "ActivePublishingRoom" with grants "canPublish:true,canSubscribe:true"
    Given an access token is created with identity "Patricia" and room "ActivePublishingRoom" with grants "canPublish:false,canSubscribe:true"
    
    When "William" opens a "Chrome" browser with LiveKit Meet page
    And "William" connects to room "ActivePublishingRoom" using the access token
    And connection is established successfully for "William"
    
    When "Patricia" opens a "Firefox" browser with LiveKit Meet page
    And "Patricia" connects to room "ActivePublishingRoom" using the access token
    And connection is established successfully for "Patricia"
    
    Then participant "William" should be publishing video in room "ActivePublishingRoom" using service "livekit1"
    And participant "Patricia" should see 1 remote video tracks in room "ActivePublishingRoom" using service "livekit1"
    
    When participant "William" is removed from room "ActivePublishingRoom" using service "livekit1"
    Then "William" should see disconnection in the browser
    And participant "Patricia" should see 0 remote video tracks in room "ActivePublishingRoom" using service "livekit1"
    And "mockserver1" should have received a "track_unpublished" event for track type "VIDEO" in room "ActivePublishingRoom"
    And "mockserver1" should have received a "participant_left" event for participant "William" in room "ActivePublishingRoom"