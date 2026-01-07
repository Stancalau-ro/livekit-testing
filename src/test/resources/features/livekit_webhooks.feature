Feature: LiveKit Webhook Integration
  As a developer
  I want to test LiveKit webhook functionality
  So that I can verify webhook events are properly sent by the LiveKit server

  Background:
    Given the LiveKit config is set to "basic_hook"
    And a mock HTTP server is running in a container with service name "mockserver"
    And a LiveKit server is running in a container with service name "livekit"

  Scenario: LiveKit sends webhook events for room and participant lifecycle
    When the system creates room "WebhookTestRoom" using service "livekit"
    Then "mockserver" should have received a "room_started" event for room "WebhookTestRoom"
    
    Given an access token is created with identity "George" and room "WebhookTestRoom" with grants "canPublish:true,canSubscribe:true"
    When "George" opens a "Chrome" browser with LiveKit Meet page
    And "George" connects to room "WebhookTestRoom" using the access token
    And connection is established successfully for "George"
    Then "mockserver" should have received a "participant_joined" event for participant "George" in room "WebhookTestRoom"
    And "mockserver" should have received a "track_published" event for "VIDEO" track from "CAMERA" in room "WebhookTestRoom"
    And "mockserver" should have received a "track_published" event for "AUDIO" track from "MICROPHONE" in room "WebhookTestRoom"
    
    When the system deletes room "WebhookTestRoom" using service "livekit"
    Then "mockserver" should have received a "room_finished" event for room "WebhookTestRoom"

  Scenario: Multiple rooms with isolated webhook event tracking
    # First room lifecycle
    When the system creates room "RoomA" using service "livekit"
    Then "mockserver" should have received a "room_started" event for room "RoomA"
    
    # Create participant in RoomA
    Given an access token is created with identity "Matthew" and room "RoomA" with grants "canPublish:true"
    When "Matthew" opens a "Chrome" browser with LiveKit Meet page
    And "Matthew" connects to room "RoomA" using the access token
    And connection is established successfully for "Matthew"
    Then "mockserver" should have received a "participant_joined" event for participant "Matthew" in room "RoomA"
    
    # Clear events to isolate next room test
    When the system clears "mockserver" webhook events

    # Second room lifecycle - only these events should be tracked
    When the system creates room "RoomB" using service "livekit"
    Then "mockserver" should have received a "room_started" event for room "RoomB"
    And "mockserver" should have received exactly 1 webhook event
    And "mockserver" should not have received a "room_started" event for room "RoomA"
    
    # Create participant in RoomB
    Given an access token is created with identity "Samantha" and room "RoomB" with grants "canPublish:true,canSubscribe:true"
    When "Samantha" opens a "Chrome" browser with LiveKit Meet page
    And "Samantha" connects to room "RoomB" using the access token
    And connection is established successfully for "Samantha"
    Then "mockserver" should have received a "participant_joined" event for participant "Samantha" in room "RoomB"
    And "mockserver" should not have received a "participant_joined" event for participant "Matthew" in room "RoomA"
    
    # Verify we have exactly the expected events after clearing
    When the system deletes room "RoomB" using service "livekit"
    Then "mockserver" should have received a "room_finished" event for room "RoomB"
    # Room deletion triggers: track_unpublished (video), track_unpublished (audio), participant_left, room_finished
    # Total events: room_started, participant_joined, track_published (video), track_published (audio), 
    #               track_unpublished (video), track_unpublished (audio), participant_left, room_finished
    And "mockserver" should have received exactly 8 webhook events

  Scenario: LiveKit sends webhook events with participant attributes
    When the system creates room "AttributesTestRoom" using service "livekit"
    Then "mockserver" should have received a "room_started" event for room "AttributesTestRoom"
    
    Given an access token is created with identity "Elizabeth" and room "AttributesTestRoom" with grants "canPublish:true,canSubscribe:true" and attributes "role=admin,department=engineering,project=livekit-testing"
    When "Elizabeth" opens a "Chrome" browser with LiveKit Meet page
    And "Elizabeth" connects to room "AttributesTestRoom" using the access token
    And connection is established successfully for "Elizabeth"
    Then "mockserver" should have received a "participant_joined" event for participant "Elizabeth" in room "AttributesTestRoom" with attributes "role=admin,department=engineering,project=livekit-testing"
    
    When the system deletes room "AttributesTestRoom" using service "livekit"
    Then "mockserver" should have received a "room_finished" event for room "AttributesTestRoom"
    And "mockserver" should have received a "participant_left" event for participant "Elizabeth" in room "AttributesTestRoom" with attributes "role=admin,department=engineering,project=livekit-testing"

  Scenario: LiveKit sends webhook events when screen share is published and unpublished
    When the system creates room "ScreenShareWebhookRoom" using service "livekit"
    Then "mockserver" should have received a "room_started" event for room "ScreenShareWebhookRoom"

    Given an access token is created with identity "Yolanda" and room "ScreenShareWebhookRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone\,screen_share"
    When "Yolanda" opens a "Chrome" browser with LiveKit Meet page
    And "Yolanda" connects to room "ScreenShareWebhookRoom" using the access token
    And connection is established successfully for "Yolanda"
    Then "mockserver" should have received a "participant_joined" event for participant "Yolanda" in room "ScreenShareWebhookRoom"
    And "mockserver" should have received a "track_published" event for "VIDEO" track from "CAMERA" in room "ScreenShareWebhookRoom"
    And "mockserver" should have received a "track_published" event for "AUDIO" track from "MICROPHONE" in room "ScreenShareWebhookRoom"

    When the system clears "mockserver" webhook events
    And "Yolanda" starts screen sharing

    Then "mockserver" should have received a "track_published" event for "VIDEO" track from "SCREEN_SHARE" in room "ScreenShareWebhookRoom"

    When the system clears "mockserver" webhook events
    And "Yolanda" stops screen sharing

    Then "mockserver" should have received a "track_unpublished" event for "VIDEO" track from "SCREEN_SHARE" in room "ScreenShareWebhookRoom"

    When the system deletes room "ScreenShareWebhookRoom" using service "livekit"
    Then "mockserver" should have received a "room_finished" event for room "ScreenShareWebhookRoom"
