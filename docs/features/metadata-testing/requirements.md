# Metadata Operations Testing - Requirements Document

## Epic Description

**Epic:** Test Metadata Operations
**Story IDs:** 1.1.5 (Room Metadata) & 1.1.6 (Participant Metadata)
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test room and participant metadata operations
**So that** I can verify metadata is correctly stored, updated, and propagated

---

## Story Breakdown

The original stories are broken down into smaller, independent stories following INVEST criteria.

---

## Part 1: Room Metadata (Story 1.1.5)

### Story 1.1.5.1: Set Room Metadata via RoomServiceClient

**Size:** S (Small)

**As a** test developer
**I want** to set room metadata via the server API
**So that** rooms can store custom application data

**Acceptance Criteria:**

**Given** a room exists in the LiveKit server
**When** metadata is set via RoomServiceClient
**Then** the operation should complete successfully

**Given** room metadata is set to a specific value
**When** the room is inspected
**Then** the metadata should match the value that was set

**Given** room metadata is empty initially
**When** metadata is set for the first time
**Then** the room should now contain the metadata

- [ ] Implement step definition to set room metadata via API
- [ ] Verify metadata is accepted without error
- [ ] Test setting metadata on newly created room
- [ ] Test setting metadata on room with existing participants

**Dependencies:** None (uses existing RoomServiceClient infrastructure)

---

### Story 1.1.5.2: Retrieve Room Metadata via Server API

**Size:** XS (Extra Small)

**As a** test developer
**I want** to retrieve room metadata via the server API
**So that** I can verify metadata persistence

**Acceptance Criteria:**

**Given** a room has metadata set
**When** the room is retrieved via ListRooms or other API
**Then** the metadata should be included in the response

**Given** metadata was recently updated
**When** retrieved immediately after update
**Then** the retrieved value should match the updated value

**Given** a room has no metadata
**When** retrieved
**Then** metadata should be empty or null without error

- [ ] Implement step definition to retrieve and verify room metadata
- [ ] Verify metadata in ListRooms response
- [ ] Handle empty metadata gracefully
- [ ] Document metadata field location in response

**Dependencies:** Story 1.1.5.1

---

### Story 1.1.5.3: Test Room Metadata Update Events

**Size:** M (Medium)

**As a** test developer
**I want** to verify room metadata update events are received by participants
**So that** connected clients can react to metadata changes

**Acceptance Criteria:**

**Given** participants are connected to a room
**When** room metadata is updated via API
**Then** all participants should receive a room metadata update event

**Given** a metadata update event is received
**When** the event payload is inspected
**Then** it should contain the new metadata value

**Given** multiple metadata updates occur in sequence
**When** events are received
**Then** each update should generate a corresponding event

- [ ] Add JavaScript listener for room metadata update events
- [ ] Implement step definitions for event verification
- [ ] Test event timing and delivery
- [ ] Verify event payload contains complete metadata

**Dependencies:** Story 1.1.5.1, Story 1.1.5.2

---

### Story 1.1.5.4: Verify Participants Receive Room Metadata Changes

**Size:** M (Medium)

**As a** test developer
**I want** to verify all connected participants receive room metadata changes
**So that** I can confirm multi-participant synchronization works

**Acceptance Criteria:**

**Given** three or more participants are in a room
**When** room metadata is updated
**Then** all participants should receive the update

**Given** a participant joins after metadata was set
**When** they connect
**Then** they should see the current room metadata

**Given** a participant joins before metadata is set
**When** metadata is updated after they join
**Then** they should receive the update event

- [ ] Create multi-participant test scenario
- [ ] Verify all participants receive metadata events
- [ ] Test late-joiner receives current metadata
- [ ] Document synchronization timing

**Dependencies:** Story 1.1.5.3

---

### Story 1.1.5.5: Test Room Metadata Size Limits

**Size:** S (Small)

**As a** test developer
**I want** to test room metadata size limits
**So that** I can document maximum metadata capacity

**Acceptance Criteria:**

**Given** small metadata (under 1KB)
**When** set on a room
**Then** it should be accepted without error

**Given** medium metadata (1-10KB)
**When** set on a room
**Then** behavior should be documented (accepted or rejected)

**Given** metadata exceeding the size limit
**When** set is attempted
**Then** an appropriate error should be returned

- [ ] Test metadata of various sizes (100B, 1KB, 5KB, 10KB, 50KB)
- [ ] Document accepted size range
- [ ] Capture and document error behavior for oversized metadata
- [ ] Define recommended maximum for test scenarios

**Dependencies:** Story 1.1.5.1

---

## Part 2: Participant Metadata (Story 1.1.6)

### Story 1.1.6.1: Set Participant Metadata via Token Attributes

**Size:** S (Small)

**As a** test developer
**I want** to set participant metadata via access token attributes
**So that** participants can have metadata from the moment they join

**Acceptance Criteria:**

**Given** an access token is created with custom metadata/attributes
**When** a participant joins using that token
**Then** their metadata should be set to the token values

**Given** a participant has token-based metadata
**When** other participants query participant info
**Then** the metadata should be visible

**Given** token attributes include structured data
**When** the participant joins
**Then** the structured data should be preserved

- [ ] Extend AccessTokenStateManager to support metadata attribute
- [ ] Create step definition for token with metadata
- [ ] Verify metadata appears in ParticipantInfo
- [ ] Test with various metadata formats

**Dependencies:** None (extends existing token infrastructure)

---

### Story 1.1.6.2: Update Participant Metadata via API

**Size:** S (Small)

**As a** test developer
**I want** to update participant metadata via RoomServiceClient
**So that** metadata can change after the participant joins

**Acceptance Criteria:**

**Given** a participant is connected to a room
**When** their metadata is updated via UpdateParticipant API
**Then** the metadata should change successfully

**Given** metadata was set via token
**When** updated via API
**Then** the new value should replace the token value

**Given** metadata is updated to empty string
**When** inspected
**Then** metadata should be empty (not the old value)

- [ ] Implement step definition for UpdateParticipant metadata
- [ ] Verify API call succeeds without error
- [ ] Test update replacing token metadata
- [ ] Test clearing metadata to empty

**Dependencies:** Story 1.1.6.1

---

### Story 1.1.6.3: Verify Participant Metadata Update Events

**Size:** M (Medium)

**As a** test developer
**I want** to verify participant metadata update events are received
**So that** other participants can react to changes

**Acceptance Criteria:**

**Given** multiple participants are in a room
**When** one participant's metadata is updated
**Then** all other participants should receive an update event

**Given** a participant metadata update event is received
**When** the event payload is inspected
**Then** it should contain the new metadata and participant identity

**Given** a participant updates their own metadata
**When** they inspect the event
**Then** they should also receive the update event

- [ ] Add JavaScript listener for participant metadata update events
- [ ] Implement step definitions for event verification
- [ ] Verify event contains participant identity and new metadata
- [ ] Test self-update event reception

**Dependencies:** Story 1.1.6.1, Story 1.1.6.2

---

### Story 1.1.6.4: Test Other Participants Receive Metadata Updates

**Size:** M (Medium)

**As a** test developer
**I want** to verify all other participants receive metadata update events
**So that** I can confirm multi-participant synchronization works

**Acceptance Criteria:**

**Given** three or more participants are in a room
**When** one participant's metadata is updated
**Then** all other participants should receive the update

**Given** a new participant joins
**When** they query existing participants
**Then** they should see current metadata for each participant

**Given** multiple participants have metadata
**When** any one is updated
**Then** only that participant's update event should fire

- [ ] Create multi-participant metadata test scenario
- [ ] Verify all participants receive correct events
- [ ] Test late-joiner sees existing participant metadata
- [ ] Verify event specificity (only updated participant triggers event)

**Dependencies:** Story 1.1.6.3

---

### Story 1.1.6.5: Verify Metadata in Webhook Events

**Size:** M (Medium)

**As a** test developer
**I want** to verify participant metadata appears in webhook events
**So that** server-side applications receive metadata changes

**Acceptance Criteria:**

**Given** webhooks are configured
**When** a participant joins with metadata
**Then** the participant_joined webhook should include metadata

**Given** participant metadata is updated
**When** the update occurs
**Then** a participant_metadata_updated webhook should fire

**Given** a participant leaves
**When** the participant_left webhook fires
**Then** it should include the participant's final metadata

- [ ] Capture webhook events for participant join
- [ ] Verify metadata in participant_joined payload
- [ ] Test participant_metadata_updated webhook event
- [ ] Verify metadata in participant_left payload

**Dependencies:** Story 1.1.6.2, existing webhook infrastructure

---

### Story 1.1.6.6: Test Participant Metadata Edge Cases

**Size:** S (Small)

**As a** test developer
**I want** to test edge cases for participant metadata
**So that** unusual scenarios are handled correctly

**Acceptance Criteria:**

**Given** metadata contains special characters
**When** set and retrieved
**Then** characters should be preserved exactly

**Given** metadata contains JSON structure
**When** set as string and retrieved
**Then** JSON should be parseable from retrieved value

**Given** metadata is very long (near size limit)
**When** set and propagated
**Then** full content should be preserved

**Given** metadata contains Unicode characters
**When** set and retrieved
**Then** Unicode should be preserved correctly

- [ ] Test special characters (newlines, quotes, backslashes)
- [ ] Test JSON as metadata value
- [ ] Test large metadata values
- [ ] Test Unicode and international characters
- [ ] Document any character restrictions

**Dependencies:** Story 1.1.6.1, Story 1.1.6.2

---

## Gherkin Scenarios

### Feature File: `livekit_metadata.feature`

```gherkin
Feature: LiveKit Metadata Operations
  As a test developer
  I want to test room and participant metadata operations
  So that I can verify metadata is correctly stored and propagated

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.5.1: Set Room Metadata via RoomServiceClient
  Scenario: Set room metadata via server API
    Given room "MetadataRoom" is created using service "livekit1"

    When room metadata for "MetadataRoom" is set to "room_status:active" using service "livekit1"

    Then room "MetadataRoom" should have metadata "room_status:active" in service "livekit1"

  Scenario: Set JSON metadata on room
    Given room "JsonMetadataRoom" is created using service "livekit1"

    When room metadata for "JsonMetadataRoom" is set to '{"theme":"dark","maxParticipants":10}' using service "livekit1"

    Then room "JsonMetadataRoom" should have metadata '{"theme":"dark","maxParticipants":10}' in service "livekit1"

  Scenario: Update existing room metadata
    Given room "UpdateRoom" is created using service "livekit1"
    And room metadata for "UpdateRoom" is set to "version:1" using service "livekit1"

    When room metadata for "UpdateRoom" is set to "version:2" using service "livekit1"

    Then room "UpdateRoom" should have metadata "version:2" in service "livekit1"

  # Story 1.1.5.2: Retrieve Room Metadata via Server API
  Scenario: Retrieve room metadata after setting
    Given room "RetrieveRoom" is created using service "livekit1"
    And room metadata for "RetrieveRoom" is set to "custom_data:test123" using service "livekit1"

    When room info for "RetrieveRoom" is retrieved using service "livekit1"

    Then the retrieved room info should have metadata "custom_data:test123"

  Scenario: Room with no metadata returns empty
    Given room "EmptyMetadataRoom" is created using service "livekit1"

    When room info for "EmptyMetadataRoom" is retrieved using service "livekit1"

    Then the retrieved room info should have empty metadata

  # Story 1.1.5.3: Test Room Metadata Update Events
  Scenario: Participant receives room metadata update event
    Given an access token is created with identity "MetadataWatcher" and room "EventRoom" with grants "canPublish:true,canSubscribe:true"
    And room "EventRoom" is created using service "livekit1"

    When "MetadataWatcher" opens a "Chrome" browser with LiveKit Meet page
    And "MetadataWatcher" connects to room "EventRoom" using the access token
    And connection is established successfully for "MetadataWatcher"
    And "MetadataWatcher" starts listening for room metadata events

    When room metadata for "EventRoom" is set to "broadcast:message1" using service "livekit1"

    Then "MetadataWatcher" should receive a room metadata update event with value "broadcast:message1"
    And "MetadataWatcher" closes the browser

  Scenario: Multiple metadata updates trigger multiple events
    Given an access token is created with identity "MultiEventWatcher" and room "MultiEventRoom" with grants "canPublish:true,canSubscribe:true"
    And room "MultiEventRoom" is created using service "livekit1"

    When "MultiEventWatcher" opens a "Chrome" browser with LiveKit Meet page
    And "MultiEventWatcher" connects to room "MultiEventRoom" using the access token
    And connection is established successfully for "MultiEventWatcher"
    And "MultiEventWatcher" starts listening for room metadata events

    When room metadata for "MultiEventRoom" is set to "update:1" using service "livekit1"
    And room metadata for "MultiEventRoom" is set to "update:2" using service "livekit1"
    And room metadata for "MultiEventRoom" is set to "update:3" using service "livekit1"

    Then "MultiEventWatcher" should have received 3 room metadata update events
    And "MultiEventWatcher" closes the browser

  # Story 1.1.5.4: Verify Participants Receive Room Metadata Changes
  Scenario: All participants receive room metadata update
    Given an access token is created with identity "Publisher1" and room "SharedRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "Subscriber1" and room "SharedRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "Subscriber2" and room "SharedRoom" with grants "canPublish:false,canSubscribe:true"
    And room "SharedRoom" is created using service "livekit1"

    When "Publisher1" opens a "Chrome" browser with LiveKit Meet page
    And "Publisher1" connects to room "SharedRoom" using the access token
    And connection is established successfully for "Publisher1"
    And "Publisher1" starts listening for room metadata events

    When "Subscriber1" opens a "Chrome" browser with LiveKit Meet page
    And "Subscriber1" connects to room "SharedRoom" using the access token
    And connection is established successfully for "Subscriber1"
    And "Subscriber1" starts listening for room metadata events

    When "Subscriber2" opens a "Firefox" browser with LiveKit Meet page
    And "Subscriber2" connects to room "SharedRoom" using the access token
    And connection is established successfully for "Subscriber2"
    And "Subscriber2" starts listening for room metadata events

    Then room "SharedRoom" should have 3 active participants in service "livekit1"

    When room metadata for "SharedRoom" is set to "announcement:hello_all" using service "livekit1"

    Then "Publisher1" should receive a room metadata update event with value "announcement:hello_all"
    And "Subscriber1" should receive a room metadata update event with value "announcement:hello_all"
    And "Subscriber2" should receive a room metadata update event with value "announcement:hello_all"
    And "Publisher1" closes the browser
    And "Subscriber1" closes the browser
    And "Subscriber2" closes the browser

  Scenario: Late joiner sees current room metadata
    Given an access token is created with identity "EarlyJoiner" and room "LateJoinRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "LateJoiner" and room "LateJoinRoom" with grants "canPublish:true,canSubscribe:true"
    And room "LateJoinRoom" is created using service "livekit1"
    And room metadata for "LateJoinRoom" is set to "preset:configuration_data" using service "livekit1"

    When "EarlyJoiner" opens a "Chrome" browser with LiveKit Meet page
    And "EarlyJoiner" connects to room "LateJoinRoom" using the access token
    And connection is established successfully for "EarlyJoiner"

    When "LateJoiner" opens a "Chrome" browser with LiveKit Meet page
    And "LateJoiner" connects to room "LateJoinRoom" using the access token
    And connection is established successfully for "LateJoiner"

    Then "LateJoiner" should see room metadata "preset:configuration_data"
    And "EarlyJoiner" closes the browser
    And "LateJoiner" closes the browser

  # Story 1.1.5.5: Test Room Metadata Size Limits
  Scenario: Small room metadata is accepted
    Given room "SmallMetadataRoom" is created using service "livekit1"

    When room metadata for "SmallMetadataRoom" is set to a string of 100 bytes using service "livekit1"

    Then room "SmallMetadataRoom" should have metadata of length 100 bytes in service "livekit1"

  Scenario: Medium room metadata is accepted
    Given room "MediumMetadataRoom" is created using service "livekit1"

    When room metadata for "MediumMetadataRoom" is set to a string of 1024 bytes using service "livekit1"

    Then room "MediumMetadataRoom" should have metadata of length 1024 bytes in service "livekit1"

  Scenario: Large room metadata behavior is documented
    Given room "LargeMetadataRoom" is created using service "livekit1"

    When room metadata for "LargeMetadataRoom" is set to a string of 10240 bytes using service "livekit1"

    Then the metadata operation result should be documented

  # Story 1.1.6.1: Set Participant Metadata via Token Attributes
  Scenario: Participant joins with metadata from token
    Given an access token is created with identity "UserWithMeta" and room "TokenMetaRoom" with grants "canPublish:true,canSubscribe:true" and metadata '{"displayName":"Alice","role":"moderator"}'
    And room "TokenMetaRoom" is created using service "livekit1"

    When "UserWithMeta" opens a "Chrome" browser with LiveKit Meet page
    And "UserWithMeta" connects to room "TokenMetaRoom" using the access token
    And connection is established successfully for "UserWithMeta"

    Then participant "UserWithMeta" should have metadata '{"displayName":"Alice","role":"moderator"}' in room "TokenMetaRoom" using service "livekit1"
    And "UserWithMeta" closes the browser

  Scenario: Token metadata visible to other participants
    Given an access token is created with identity "MetaPublisher" and room "VisibleMetaRoom" with grants "canPublish:true,canSubscribe:true" and metadata "user_type:premium"
    And an access token is created with identity "MetaObserver" and room "VisibleMetaRoom" with grants "canPublish:false,canSubscribe:true"
    And room "VisibleMetaRoom" is created using service "livekit1"

    When "MetaPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "MetaPublisher" connects to room "VisibleMetaRoom" using the access token
    And connection is established successfully for "MetaPublisher"

    When "MetaObserver" opens a "Chrome" browser with LiveKit Meet page
    And "MetaObserver" connects to room "VisibleMetaRoom" using the access token
    And connection is established successfully for "MetaObserver"

    Then "MetaObserver" should see participant "MetaPublisher" with metadata "user_type:premium"
    And "MetaPublisher" closes the browser
    And "MetaObserver" closes the browser

  # Story 1.1.6.2: Update Participant Metadata via API
  Scenario: Update participant metadata via server API
    Given an access token is created with identity "UpdatableUser" and room "UpdateUserRoom" with grants "canPublish:true,canSubscribe:true" and metadata "status:online"
    And room "UpdateUserRoom" is created using service "livekit1"

    When "UpdatableUser" opens a "Chrome" browser with LiveKit Meet page
    And "UpdatableUser" connects to room "UpdateUserRoom" using the access token
    And connection is established successfully for "UpdatableUser"

    Then participant "UpdatableUser" should have metadata "status:online" in room "UpdateUserRoom" using service "livekit1"

    When participant "UpdatableUser" metadata is updated to "status:away" in room "UpdateUserRoom" using service "livekit1"

    Then participant "UpdatableUser" should have metadata "status:away" in room "UpdateUserRoom" using service "livekit1"
    And "UpdatableUser" closes the browser

  Scenario: Clear participant metadata to empty
    Given an access token is created with identity "ClearableUser" and room "ClearMetaRoom" with grants "canPublish:true,canSubscribe:true" and metadata "initial:value"
    And room "ClearMetaRoom" is created using service "livekit1"

    When "ClearableUser" opens a "Chrome" browser with LiveKit Meet page
    And "ClearableUser" connects to room "ClearMetaRoom" using the access token
    And connection is established successfully for "ClearableUser"

    When participant "ClearableUser" metadata is updated to "" in room "ClearMetaRoom" using service "livekit1"

    Then participant "ClearableUser" should have empty metadata in room "ClearMetaRoom" using service "livekit1"
    And "ClearableUser" closes the browser

  # Story 1.1.6.3: Verify Participant Metadata Update Events
  Scenario: Other participant receives metadata update event
    Given an access token is created with identity "ChangingUser" and room "ParticipantEventRoom" with grants "canPublish:true,canSubscribe:true" and metadata "state:idle"
    And an access token is created with identity "Watcher" and room "ParticipantEventRoom" with grants "canPublish:false,canSubscribe:true"
    And room "ParticipantEventRoom" is created using service "livekit1"

    When "ChangingUser" opens a "Chrome" browser with LiveKit Meet page
    And "ChangingUser" connects to room "ParticipantEventRoom" using the access token
    And connection is established successfully for "ChangingUser"

    When "Watcher" opens a "Chrome" browser with LiveKit Meet page
    And "Watcher" connects to room "ParticipantEventRoom" using the access token
    And connection is established successfully for "Watcher"
    And "Watcher" starts listening for participant metadata events

    When participant "ChangingUser" metadata is updated to "state:active" in room "ParticipantEventRoom" using service "livekit1"

    Then "Watcher" should receive a participant metadata update event for "ChangingUser" with value "state:active"
    And "ChangingUser" closes the browser
    And "Watcher" closes the browser

  Scenario: Self receives own metadata update event
    Given an access token is created with identity "SelfUpdater" and room "SelfUpdateRoom" with grants "canPublish:true,canSubscribe:true" and metadata "initial:value"
    And room "SelfUpdateRoom" is created using service "livekit1"

    When "SelfUpdater" opens a "Chrome" browser with LiveKit Meet page
    And "SelfUpdater" connects to room "SelfUpdateRoom" using the access token
    And connection is established successfully for "SelfUpdater"
    And "SelfUpdater" starts listening for participant metadata events

    When participant "SelfUpdater" metadata is updated to "updated:value" in room "SelfUpdateRoom" using service "livekit1"

    Then "SelfUpdater" should receive a participant metadata update event for "SelfUpdater" with value "updated:value"
    And "SelfUpdater" closes the browser

  # Story 1.1.6.4: Test Other Participants Receive Metadata Updates
  Scenario: All participants receive metadata update for one participant
    Given an access token is created with identity "TargetUser" and room "MultiObserverRoom" with grants "canPublish:true,canSubscribe:true" and metadata "level:1"
    And an access token is created with identity "Observer1" and room "MultiObserverRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "Observer2" and room "MultiObserverRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiObserverRoom" is created using service "livekit1"

    When "TargetUser" opens a "Chrome" browser with LiveKit Meet page
    And "TargetUser" connects to room "MultiObserverRoom" using the access token
    And connection is established successfully for "TargetUser"

    When "Observer1" opens a "Chrome" browser with LiveKit Meet page
    And "Observer1" connects to room "MultiObserverRoom" using the access token
    And connection is established successfully for "Observer1"
    And "Observer1" starts listening for participant metadata events

    When "Observer2" opens a "Firefox" browser with LiveKit Meet page
    And "Observer2" connects to room "MultiObserverRoom" using the access token
    And connection is established successfully for "Observer2"
    And "Observer2" starts listening for participant metadata events

    Then room "MultiObserverRoom" should have 3 active participants in service "livekit1"

    When participant "TargetUser" metadata is updated to "level:10" in room "MultiObserverRoom" using service "livekit1"

    Then "Observer1" should receive a participant metadata update event for "TargetUser" with value "level:10"
    And "Observer2" should receive a participant metadata update event for "TargetUser" with value "level:10"
    And "TargetUser" closes the browser
    And "Observer1" closes the browser
    And "Observer2" closes the browser

  Scenario: Late joiner sees existing participant metadata
    Given an access token is created with identity "FirstUser" and room "LateJoinParticipantRoom" with grants "canPublish:true,canSubscribe:true" and metadata "established:user"
    And an access token is created with identity "SecondUser" and room "LateJoinParticipantRoom" with grants "canPublish:true,canSubscribe:true"
    And room "LateJoinParticipantRoom" is created using service "livekit1"

    When "FirstUser" opens a "Chrome" browser with LiveKit Meet page
    And "FirstUser" connects to room "LateJoinParticipantRoom" using the access token
    And connection is established successfully for "FirstUser"

    When "SecondUser" opens a "Chrome" browser with LiveKit Meet page
    And "SecondUser" connects to room "LateJoinParticipantRoom" using the access token
    And connection is established successfully for "SecondUser"

    Then "SecondUser" should see participant "FirstUser" with metadata "established:user"
    And "FirstUser" closes the browser
    And "SecondUser" closes the browser

  # Story 1.1.6.5: Verify Metadata in Webhook Events
  Scenario: Participant joined webhook includes metadata
    Given webhook receiver is configured for service "livekit1"
    And an access token is created with identity "WebhookUser" and room "WebhookRoom" with grants "canPublish:true,canSubscribe:true" and metadata "webhook_test:join_meta"
    And room "WebhookRoom" is created using service "livekit1"

    When "WebhookUser" opens a "Chrome" browser with LiveKit Meet page
    And "WebhookUser" connects to room "WebhookRoom" using the access token
    And connection is established successfully for "WebhookUser"

    Then a "participant_joined" webhook should have been received for room "WebhookRoom"
    And the webhook payload should include participant "WebhookUser" with metadata "webhook_test:join_meta"
    And "WebhookUser" closes the browser

  Scenario: Participant metadata update triggers webhook
    Given webhook receiver is configured for service "livekit1"
    And an access token is created with identity "WebhookUpdateUser" and room "WebhookUpdateRoom" with grants "canPublish:true,canSubscribe:true" and metadata "initial:meta"
    And room "WebhookUpdateRoom" is created using service "livekit1"

    When "WebhookUpdateUser" opens a "Chrome" browser with LiveKit Meet page
    And "WebhookUpdateUser" connects to room "WebhookUpdateRoom" using the access token
    And connection is established successfully for "WebhookUpdateUser"

    When participant "WebhookUpdateUser" metadata is updated to "updated:meta" in room "WebhookUpdateRoom" using service "livekit1"

    Then a "participant_metadata_updated" webhook should have been received for room "WebhookUpdateRoom"
    And the webhook payload should include participant "WebhookUpdateUser" with metadata "updated:meta"
    And "WebhookUpdateUser" closes the browser

  # Story 1.1.6.6: Test Participant Metadata Edge Cases
  Scenario: Metadata with special characters
    Given an access token is created with identity "SpecialCharUser" and room "SpecialCharRoom" with grants "canPublish:true,canSubscribe:true" and metadata 'text:line1\nline2\ttab'
    And room "SpecialCharRoom" is created using service "livekit1"

    When "SpecialCharUser" opens a "Chrome" browser with LiveKit Meet page
    And "SpecialCharUser" connects to room "SpecialCharRoom" using the access token
    And connection is established successfully for "SpecialCharUser"

    Then participant "SpecialCharUser" should have metadata containing "line1" in room "SpecialCharRoom" using service "livekit1"
    And "SpecialCharUser" closes the browser

  Scenario: Metadata with JSON structure
    Given an access token is created with identity "JsonUser" and room "JsonRoom" with grants "canPublish:true,canSubscribe:true" and metadata '{"user":{"id":123,"preferences":{"theme":"dark","notifications":true}}}'
    And room "JsonRoom" is created using service "livekit1"

    When "JsonUser" opens a "Chrome" browser with LiveKit Meet page
    And "JsonUser" connects to room "JsonRoom" using the access token
    And connection is established successfully for "JsonUser"

    Then participant "JsonUser" metadata should be valid JSON in room "JsonRoom" using service "livekit1"
    And the metadata JSON should have path "user.id" with value 123
    And "JsonUser" closes the browser

  Scenario: Metadata with Unicode characters
    Given an access token is created with identity "UnicodeUser" and room "UnicodeRoom" with grants "canPublish:true,canSubscribe:true" and metadata "name:Alice Wang"
    And room "UnicodeRoom" is created using service "livekit1"

    When "UnicodeUser" opens a "Chrome" browser with LiveKit Meet page
    And "UnicodeUser" connects to room "UnicodeRoom" using the access token
    And connection is established successfully for "UnicodeUser"

    Then participant "UnicodeUser" should have metadata containing "Alice" in room "UnicodeRoom" using service "livekit1"
    And "UnicodeUser" closes the browser

  Scenario: Large participant metadata near size limit
    Given an access token is created with identity "LargeMetaUser" and room "LargeMetaRoom" with grants "canPublish:true,canSubscribe:true" and metadata of size 5000 bytes
    And room "LargeMetaRoom" is created using service "livekit1"

    When "LargeMetaUser" opens a "Chrome" browser with LiveKit Meet page
    And "LargeMetaUser" connects to room "LargeMetaRoom" using the access token
    And connection is established successfully for "LargeMetaUser"

    Then participant "LargeMetaUser" should have metadata of approximately 5000 bytes in room "LargeMetaRoom" using service "livekit1"
    And "LargeMetaUser" closes the browser

  # Cross-browser Scenario
  Scenario Outline: Cross-browser participant metadata propagation
    Given an access token is created with identity "CrossBrowserSender" and room "CrossBrowserMetaRoom" with grants "canPublish:true,canSubscribe:true" and metadata "sender_browser:<pubBrowser>"
    And an access token is created with identity "CrossBrowserReceiver" and room "CrossBrowserMetaRoom" with grants "canPublish:false,canSubscribe:true"
    And room "CrossBrowserMetaRoom" is created using service "livekit1"

    When "CrossBrowserSender" opens a <pubBrowser> browser with LiveKit Meet page
    And "CrossBrowserSender" connects to room "CrossBrowserMetaRoom" using the access token
    And connection is established successfully for "CrossBrowserSender"

    When "CrossBrowserReceiver" opens a <subBrowser> browser with LiveKit Meet page
    And "CrossBrowserReceiver" connects to room "CrossBrowserMetaRoom" using the access token
    And connection is established successfully for "CrossBrowserReceiver"

    Then "CrossBrowserReceiver" should see participant "CrossBrowserSender" with metadata containing "<pubBrowser>"
    And "CrossBrowserSender" closes the browser
    And "CrossBrowserReceiver" closes the browser

    Examples:
      | pubBrowser | subBrowser |
      | "Chrome"   | "Firefox"  |
      | "Firefox"  | "Chrome"   |
      | "Chrome"   | "Edge"     |
```

---

## Definition of Done

### Code Implementation
- [x] New step definitions added for room metadata operations
- [x] New step definitions added for participant metadata operations
- [x] AccessTokenStateManager extended to support metadata attribute
- [x] `LiveKitMeet.java` extended with metadata event listener methods
- [x] JavaScript helpers added for metadata event capture
- [x] RoomServiceClient usage for UpdateParticipant metadata
- [x] Feature file `livekit_metadata.feature` created and passing
- [x] All scenarios pass on Chrome browser
- [x] All scenarios pass on Firefox browser
- [x] Webhook verification scenarios pass

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass
- [x] Tests pass against default LiveKit version
- [x] Room metadata operations verified via API
- [x] Participant metadata visible to other participants
- [x] Metadata events received by all participants
- [x] Webhook events contain metadata

### Documentation
- [x] Feature documentation complete in `docs/features/metadata-testing/`
- [x] Step definitions documented in `docs/features.md`
- [x] Technical notes added for metadata implementation
- [x] Size limits documented
- [x] Known limitations documented

### Code Quality
- [x] No new Lombok violations
- [x] No code comments added (per project guidelines)
- [x] Cross-platform path handling maintained
- [x] Proper cleanup in After hooks

---

## Technical Components

### New Step Definitions Required

#### Room Metadata Steps:
```java
@When("room metadata for {string} is set to {string} using service {string}")
public void setRoomMetadata(String roomName, String metadata, String service)

@When("room metadata for {string} is set to a string of {int} bytes using service {string}")
public void setRoomMetadataOfSize(String roomName, int size, String service)

@When("room info for {string} is retrieved using service {string}")
public void retrieveRoomInfo(String roomName, String service)

@Then("room {string} should have metadata {string} in service {string}")
public void verifyRoomMetadata(String roomName, String expectedMetadata, String service)

@Then("room {string} should have metadata of length {int} bytes in service {string}")
public void verifyRoomMetadataLength(String roomName, int expectedLength, String service)

@Then("the retrieved room info should have metadata {string}")
public void verifyRetrievedRoomMetadata(String expectedMetadata)

@Then("the retrieved room info should have empty metadata")
public void verifyRetrievedRoomMetadataEmpty()
```

#### Participant Metadata Steps:
```java
@Given("an access token is created with identity {string} and room {string} with grants {string} and metadata {string}")
public void createTokenWithMetadata(String identity, String room, String grants, String metadata)

@Given("an access token is created with identity {string} and room {string} with grants {string} and metadata of size {int} bytes")
public void createTokenWithMetadataOfSize(String identity, String room, String grants, int size)

@When("participant {string} metadata is updated to {string} in room {string} using service {string}")
public void updateParticipantMetadata(String identity, String metadata, String room, String service)

@Then("participant {string} should have metadata {string} in room {string} using service {string}")
public void verifyParticipantMetadata(String identity, String expectedMetadata, String room, String service)

@Then("participant {string} should have empty metadata in room {string} using service {string}")
public void verifyParticipantMetadataEmpty(String identity, String room, String service)

@Then("participant {string} metadata should be valid JSON in room {string} using service {string}")
public void verifyParticipantMetadataIsJson(String identity, String room, String service)
```

#### Event Listener Steps:
```java
@When("{string} starts listening for room metadata events")
public void startListeningForRoomMetadataEvents(String participantName)

@When("{string} starts listening for participant metadata events")
public void startListeningForParticipantMetadataEvents(String participantName)

@Then("{string} should receive a room metadata update event with value {string}")
public void shouldReceiveRoomMetadataEvent(String participantName, String expectedValue)

@Then("{string} should receive a participant metadata update event for {string} with value {string}")
public void shouldReceiveParticipantMetadataEvent(String watcher, String targetParticipant, String expectedValue)

@Then("{string} should have received {int} room metadata update events")
public void shouldHaveReceivedRoomMetadataEventCount(String participantName, int expectedCount)

@Then("{string} should see room metadata {string}")
public void shouldSeeRoomMetadata(String participantName, String expectedMetadata)

@Then("{string} should see participant {string} with metadata {string}")
public void shouldSeeParticipantMetadata(String observer, String participant, String expectedMetadata)

@Then("{string} should see participant {string} with metadata containing {string}")
public void shouldSeeParticipantMetadataContaining(String observer, String participant, String expectedSubstring)
```

#### Webhook Steps:
```java
@Then("a {string} webhook should have been received for room {string}")
public void webhookShouldHaveBeenReceived(String eventType, String roomName)

@Then("the webhook payload should include participant {string} with metadata {string}")
public void webhookPayloadShouldIncludeParticipantMetadata(String identity, String expectedMetadata)
```

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Metadata event timing variations | Medium | Medium | Use polling with generous timeout |
| Size limits vary across versions | Medium | Low | Document tested version limits |
| Webhook delivery delays in containers | Medium | Medium | Use configurable wait times |
| JSON parsing edge cases | Low | Low | Test standard JSON structures |
| Unicode handling differences | Low | Low | Test common Unicode scenarios |

### Mitigation Strategies

1. **Event Timing**: Use WebDriverWait with configurable timeout for event verification
2. **Size Testing**: Start with known-safe sizes; document limits for tested version
3. **Webhook Delays**: Allow 5-10 seconds for webhook delivery in container environment
4. **JSON Handling**: Use standard JSON library for parsing and comparison
5. **Unicode**: Test with common international characters; avoid exotic edge cases

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.5.1 - Set Room Metadata | S | Foundation for room metadata tests |
| 2 | 1.1.5.2 - Retrieve Room Metadata | XS | Verification capability |
| 3 | 1.1.6.1 - Token Metadata | S | Foundation for participant metadata |
| 4 | 1.1.6.2 - Update Participant Metadata | S | API update capability |
| 5 | 1.1.5.3 - Room Metadata Events | M | Event infrastructure |
| 6 | 1.1.6.3 - Participant Metadata Events | M | Participant event handling |
| 7 | 1.1.5.4 - Multi-Participant Room | M | Multi-participant verification |
| 8 | 1.1.6.4 - Multi-Participant Participant | M | Multi-participant verification |
| 9 | 1.1.6.5 - Webhook Verification | M | Server-side validation |
| 10 | 1.1.5.5 - Size Limits | S | Edge case documentation |
| 11 | 1.1.6.6 - Edge Cases | S | Edge case handling |

**Total Estimated Effort:** M (Medium) - approximately 4-6 days

---

## Appendix: LiveKit Metadata API Reference

### RoomServiceClient - Room Metadata

```java
// Update room metadata
roomService.updateRoomMetadata(roomName, metadata);

// Room info includes metadata
Room room = roomService.listRooms().get(0);
String metadata = room.getMetadata();
```

### RoomServiceClient - Participant Metadata

```java
// Update participant metadata
roomService.updateParticipant(roomName, identity, null, metadata, null);

// Participant info includes metadata
ParticipantInfo participant = roomService.getParticipant(roomName, identity);
String metadata = participant.getMetadata();
```

### Access Token with Metadata

```java
AccessToken token = new AccessToken(apiKey, apiSecret);
token.setIdentity(identity);
token.setMetadata(metadata);  // Participant metadata
token.addGrants(grants);
```

### JavaScript Client - Room Metadata Events

```javascript
room.on(RoomEvent.RoomMetadataChanged, (metadata) => {
  console.log('Room metadata changed:', metadata);
});
```

### JavaScript Client - Participant Metadata Events

```javascript
room.on(RoomEvent.ParticipantMetadataChanged, (metadata, participant) => {
  console.log('Participant', participant.identity, 'metadata changed:', metadata);
});

// Access participant metadata
const participant = room.participants.get(identity);
const metadata = participant.metadata;
```

### Webhook Events

```json
// participant_joined
{
  "event": "participant_joined",
  "room": { "name": "room_name" },
  "participant": {
    "identity": "user_id",
    "metadata": "custom_metadata"
  }
}

// participant_metadata_updated
{
  "event": "participant_metadata_updated",
  "room": { "name": "room_name" },
  "participant": {
    "identity": "user_id",
    "metadata": "new_metadata"
  }
}
```

---

## Related Stories

- **Story 1.1.4** (Data Channel) - Completed; similar event propagation patterns
- **Story 1.1.7** (Track Mute) - Completed; similar state synchronization
- **Story 1.1.3** (Dynacast) - Completed; similar multi-participant scenarios
- **Story 2.2.2** (Container Logs) - Future; helps debug metadata issues
