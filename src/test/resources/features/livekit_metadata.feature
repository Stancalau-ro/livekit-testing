Feature: LiveKit Room and Participant Metadata
  As a LiveKit application developer
  I want to set and retrieve metadata on rooms and participants
  So that I can store custom information and have it propagate to all clients

  Background:
    Given the LiveKit config is set to "webhook"
    And a mock HTTP server is running in a container with service name "mockserver"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Room metadata CRUD operations
    Given an access token is created with identity "Alice" and room "MetadataRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "MetadataRoom" using the access token
    Then room "MetadataRoom" should have empty metadata in service "livekit1"
    When room metadata for "MetadataRoom" is set to "initial-metadata" using service "livekit1"
    Then room "MetadataRoom" should have metadata "initial-metadata" in service "livekit1"
    When room metadata for "MetadataRoom" is set to "updated-metadata" using service "livekit1"
    Then room "MetadataRoom" should have metadata "updated-metadata" in service "livekit1"
    When room info for "MetadataRoom" is retrieved using service "livekit1"
    Then the retrieved room info should have metadata "updated-metadata"

  Scenario: Room metadata events with multiple participants
    Given an access token is created with identity "Alice" and room "EventRoom" with grants "CanPublish, CanSubscribe"
    And an access token is created with identity "Bob" and room "EventRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "EventRoom" using the access token
    And "Bob" connects to room "EventRoom" using the access token
    And "Alice" starts listening for room metadata events
    And "Bob" starts listening for room metadata events
    When room metadata for "EventRoom" is set to "shared-data" using service "livekit1"
    Then "Alice" should receive a room metadata update event with value "shared-data"
    And "Bob" should receive a room metadata update event with value "shared-data"
    And "Alice" should see room metadata "shared-data"
    And "Bob" should see room metadata "shared-data"

  Scenario: Late joiner sees current room metadata
    Given an access token is created with identity "Alice" and room "LateJoinRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "LateJoinRoom" using the access token
    And room metadata for "LateJoinRoom" is set to "existing-metadata" using service "livekit1"
    Then room "LateJoinRoom" should have metadata "existing-metadata" in service "livekit1"
    Given an access token is created with identity "Bob" and room "LateJoinRoom" with grants "CanPublish, CanSubscribe"
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "LateJoinRoom" using the access token
    Then "Bob" should see room metadata "existing-metadata"

  Scenario: Participant token metadata visible to other participants and webhook
    Given an access token is created with identity "Alice" and room "TokenMetaRoom" with grants "CanPublish, CanSubscribe" and metadata "alice-token-meta"
    And an access token is created with identity "Bob" and room "TokenMetaRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "TokenMetaRoom" using the access token
    Then "mockserver" should have received a "participant_joined" event for participant "Alice" in room "TokenMetaRoom" with metadata "alice-token-meta"
    When "Bob" connects to room "TokenMetaRoom" using the access token
    Then "mockserver" should have received a "participant_joined" event for participant "Bob" in room "TokenMetaRoom" with empty metadata
    And participant "Alice" should have metadata "alice-token-meta" in room "TokenMetaRoom" using service "livekit1"
    And "Bob" should see participant "Alice" with metadata "alice-token-meta"

  Scenario: Update participant metadata via API
    Given an access token is created with identity "Alice" and room "ApiMetaRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "ApiMetaRoom" using the access token
    Then participant "Alice" should have empty metadata in room "ApiMetaRoom" using service "livekit1"
    When participant "Alice" metadata is updated to "api-set-metadata" in room "ApiMetaRoom" using service "livekit1"
    Then participant "Alice" should have metadata "api-set-metadata" in room "ApiMetaRoom" using service "livekit1"
    When participant "Alice" metadata is updated to "updated-again" in room "ApiMetaRoom" using service "livekit1"
    Then participant "Alice" should have metadata "updated-again" in room "ApiMetaRoom" using service "livekit1"

  Scenario: Participant metadata events with multiple observers
    Given an access token is created with identity "Alice" and room "ParticipantEventRoom" with grants "CanPublish, CanSubscribe"
    And an access token is created with identity "Bob" and room "ParticipantEventRoom" with grants "CanPublish, CanSubscribe"
    And an access token is created with identity "Charlie" and room "ParticipantEventRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Charlie" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "ParticipantEventRoom" using the access token
    And "Bob" connects to room "ParticipantEventRoom" using the access token
    And "Charlie" connects to room "ParticipantEventRoom" using the access token
    And "Alice" starts listening for participant metadata events
    And "Bob" starts listening for participant metadata events
    And "Charlie" starts listening for participant metadata events
    When participant "Alice" metadata is updated to "alice-updated" in room "ParticipantEventRoom" using service "livekit1"
    Then "Alice" should receive a participant metadata update event for "Alice" with value "alice-updated"
    And "Bob" should receive a participant metadata update event for "Alice" with value "alice-updated"
    And "Charlie" should receive a participant metadata update event for "Alice" with value "alice-updated"

  Scenario: Late joiner sees existing participant metadata
    Given an access token is created with identity "Alice" and room "LateParticipantRoom" with grants "CanPublish, CanSubscribe" and metadata "early-bird-meta"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "LateParticipantRoom" using the access token
    Then participant "Alice" should have metadata "early-bird-meta" in room "LateParticipantRoom" using service "livekit1"
    Given an access token is created with identity "Bob" and room "LateParticipantRoom" with grants "CanPublish, CanSubscribe"
    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "LateParticipantRoom" using the access token
    Then "Bob" should see participant "Alice" with metadata "early-bird-meta"

  Scenario: Room metadata with JSON structure
    Given an access token is created with identity "Alice" and room "JsonMetaRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "JsonMetaRoom" using the access token
    When room metadata for "JsonMetaRoom" is set to "{\"type\":\"meeting\",\"host\":\"Alice\",\"settings\":{\"recording\":true}}" using service "livekit1"
    Then room "JsonMetaRoom" should have metadata "{\"type\":\"meeting\",\"host\":\"Alice\",\"settings\":{\"recording\":true}}" in service "livekit1"

  Scenario: Participant metadata edge cases - special characters, JSON and Unicode
    Given an access token is created with identity "Alice" and room "EdgeCaseRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "EdgeCaseRoom" using the access token
    When participant "Alice" metadata is updated to "hello=world&foo=bar" in room "EdgeCaseRoom" using service "livekit1"
    Then participant "Alice" should have metadata containing "hello=world" in room "EdgeCaseRoom" using service "livekit1"
    When participant "Alice" metadata is updated to "{\"name\":\"Test\",\"value\":42}" in room "EdgeCaseRoom" using service "livekit1"
    Then participant "Alice" metadata should be valid JSON in room "EdgeCaseRoom" using service "livekit1"

  Scenario: Room metadata size limits
    Given an access token is created with identity "Alice" and room "SizeRoom" with grants "CanPublish, CanSubscribe"
    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "SizeRoom" using the access token
    When room metadata for "SizeRoom" is set to a string of 100 bytes using service "livekit1"
    Then room "SizeRoom" should have metadata of length 100 bytes in service "livekit1"
    When room metadata for "SizeRoom" is set to a string of 1000 bytes using service "livekit1"
    Then room "SizeRoom" should have metadata of length 1000 bytes in service "livekit1"
