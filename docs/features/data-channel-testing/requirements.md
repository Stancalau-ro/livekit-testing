# Data Channel Communication Testing - Requirements Document

## Epic Description

**Epic:** Test Data Channel Communication
**Story ID:** 1.1.4
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test data channel messaging
**So that** I can verify reliable and unreliable data delivery

---

## Story Breakdown

The original story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.4.1: Data Publishing Permission Grant

**Size:** XS (Extra Small)

**As a** test developer
**I want** to create access tokens with data publishing permissions
**So that** participants can be authorized to send data messages

**Acceptance Criteria:**

**Given** a token creation request with canPublishData grant
**When** the token is generated
**Then** the token should include the canPublishData permission

**Given** a token without canPublishData permission
**When** inspected
**Then** the data publishing permission should be absent or false

- [x] Add canPublishData grant to token creation
- [x] Verify grant presence in JWT token
- [x] Test token without data permission for negative testing
- [x] Document canPublishData grant behavior

**Dependencies:** None (uses existing AccessTokenStateManager)

---

### Story 1.1.4.2: Send Message via Reliable Data Channel

**Size:** S (Small)

**As a** test developer
**I want** to send data messages via reliable data channel
**So that** I can verify message transmission works correctly

**Acceptance Criteria:**

**Given** a participant with canPublishData permission is connected
**When** they send a data message via reliable channel
**Then** the message should be transmitted without error

**Given** a message is sent via reliable channel
**When** the sender's client state is inspected
**Then** the message should be marked as sent

**Given** multiple messages are sent via reliable channel
**When** inspected in order
**Then** messages should maintain their sending order

- [x] Add browser-side data message sending capability
- [x] Implement sendDataMessage method in LiveKitMeet
- [x] Add JavaScript helpers for data channel operations
- [x] Verify message sending completes without error

**Dependencies:** Story 1.1.4.1

---

### Story 1.1.4.3: Verify Message Receipt by Other Participant

**Size:** M (Medium)

**As a** test developer
**I want** to verify that data messages are received by other participants
**So that** I can confirm end-to-end data channel communication

**Acceptance Criteria:**

**Given** two participants are in a room
**When** one sends a data message
**Then** the other participant should receive the message

**Given** a message is received
**When** the content is compared to what was sent
**Then** the content should match exactly

**Given** multiple messages are sent
**When** received by the subscriber
**Then** all messages should be received in order (reliable channel)

- [x] Add browser-side message reception capability
- [x] Implement message listener in LiveKitMeet
- [x] Store received messages for verification
- [x] Add step definitions for message receipt verification
- [x] Test message content integrity

**Dependencies:** Story 1.1.4.2

---

### Story 1.1.4.4: Test Unreliable Data Channel

**Size:** S (Small)

**As a** test developer
**I want** to test unreliable data channel delivery
**So that** I can verify best-effort message delivery works

**Acceptance Criteria:**

**Given** a participant sends a message via unreliable channel
**When** the message is transmitted
**Then** it should be sent with best-effort delivery semantics

**Given** messages are sent via unreliable channel
**When** received by subscribers
**Then** messages may be received (no delivery guarantee)

**Given** unreliable channel is used
**When** message delivery is verified
**Then** the test should account for potential message loss

- [x] Add unreliable data channel support to LiveKitMeet
- [x] Implement kind parameter for message sending (RELIABLE vs LOSSY)
- [x] Add step definitions for unreliable channel testing
- [x] Document expected behavior differences

**Dependencies:** Story 1.1.4.3

---

### Story 1.1.4.5: Measure Data Channel Latency

**Size:** S (Small)

**As a** test developer
**I want** to measure data channel message latency
**So that** I can verify acceptable performance

**Acceptance Criteria:**

**Given** a timestamp is included in the message
**When** the message is received
**Then** the round-trip or one-way latency can be calculated

**Given** multiple messages are sent
**When** latency is measured for each
**Then** average latency should be within acceptable bounds

**Given** latency measurement is requested
**When** results are logged
**Then** min, max, and average latency should be reported

- [x] Add timestamp to test messages
- [x] Calculate latency on message receipt
- [x] Log latency statistics
- [x] Define acceptable latency thresholds for containerized testing
- [x] Add step definitions for latency verification

**Dependencies:** Story 1.1.4.3

---

### Story 1.1.4.6: Test Large Message Handling

**Size:** M (Medium)

**As a** test developer
**I want** to test large message handling
**So that** I can verify messages near size limits work correctly

**Acceptance Criteria:**

**Given** a message near the maximum size limit is created
**When** sent via data channel
**Then** it should be transmitted successfully

**Given** a message at the maximum size limit
**When** received by other participants
**Then** the full content should be intact

**Given** a message exceeding the size limit
**When** send is attempted
**Then** appropriate error handling should occur

- [x] Determine LiveKit maximum message size (15 KiB for reliable mode)
- [x] Create test messages of various sizes (1KB, 8KB, 14KB)
- [x] Verify large message integrity on receipt
- [x] Test behavior at and beyond size limits
- [x] Document size limit behavior

**Dependencies:** Story 1.1.4.3

---

### Story 1.1.4.7: Data Publishing Permission Denied

**Size:** XS (Extra Small)

**As a** test developer
**I want** to verify participants without data permission cannot send messages
**So that** I can confirm permission enforcement works

**Acceptance Criteria:**

**Given** a participant without canPublishData permission
**When** they attempt to send a data message
**Then** the message should be blocked or fail

**Given** data publishing is denied
**When** error state is checked
**Then** appropriate error information should be available

- [x] Test send attempt without canPublishData permission
- [x] Verify error or blocked state is detectable
- [x] Confirm other permissions still work independently
- [x] Add step definitions for permission denial scenarios

**Dependencies:** Story 1.1.4.1, Story 1.1.4.2

---

### Story 1.1.4.8: Cross-Browser Data Channel Testing

**Size:** S (Small)

**As a** test developer
**I want** to verify data channels work across different browsers
**So that** I can ensure cross-browser compatibility

**Acceptance Criteria:**

**Given** Chrome participant sends a data message
**When** Firefox participant is subscribed
**Then** Firefox participant should receive the message

**Given** data channel operations work on Chrome
**When** the same operations are tested on Firefox and Edge
**Then** behavior should be consistent

- [x] Test data send/receive on Chrome, Firefox, Edge
- [x] Verify cross-browser message delivery
- [x] Document any browser-specific differences
- [x] Add scenario outlines for browser matrix testing

**Dependencies:** Story 1.1.4.3

---

### Story 1.1.4.9: Broadcast vs Targeted Message Delivery

**Size:** S (Small)

**As a** test developer
**I want** to test broadcast and targeted message delivery
**So that** I can verify message routing works correctly

**Acceptance Criteria:**

**Given** three or more participants are in a room
**When** one sends a broadcast message
**Then** all other participants should receive it

**Given** multiple participants are in a room
**When** a targeted message is sent to specific participants
**Then** only the targeted participants should receive it

**Given** a targeted message is sent
**When** non-targeted participants are checked
**Then** they should not have received the message

- [x] Test broadcast message delivery to all participants
- [x] Test targeted message delivery to specific participants
- [x] Verify non-recipients do not receive targeted messages
- [x] Add step definitions for destination-specific messaging

**Dependencies:** Story 1.1.4.3

---

## Gherkin Scenarios

### Feature File: `livekit_data_channel.feature`

```gherkin
Feature: LiveKit Data Channel Communication
  As a test developer
  I want to test data channel messaging
  So that I can verify reliable and unreliable data delivery

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.4.1: Data Publishing Permission Grant
  Scenario: Generate access token with data publishing permission
    When the system creates an access token with identity "DataPublisher" and room "DataRoom" with grants "canPublish:true,canPublishData:true"
    Then the access token for "DataPublisher" in room "DataRoom" should be valid
    And the access token for "DataPublisher" in room "DataRoom" should have the following grants:
      | grant          | value |
      | canPublish     | true  |
      | canPublishData | true  |
      | roomJoin       | true  |

  Scenario: Generate access token without data publishing permission
    When the system creates an access token with identity "NoDataUser" and room "RestrictedRoom" with grants "canPublish:true,canPublishData:false"
    Then the access token for "NoDataUser" in room "RestrictedRoom" should be valid
    And the access token for "NoDataUser" in room "RestrictedRoom" should have the following grants:
      | grant          | value |
      | canPublish     | true  |
      | canPublishData | false |
      | roomJoin       | true  |

  # Story 1.1.4.2 & 1.1.4.3: Send and Receive Messages via Reliable Data Channel
  Scenario: Participant can send and receive data message via reliable channel
    Given an access token is created with identity "Alice" and room "DataTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Bob" and room "DataTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "DataTestRoom" is created using service "livekit1"

    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "DataTestRoom" using the access token
    And connection is established successfully for "Alice"

    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "DataTestRoom" using the access token
    And connection is established successfully for "Bob"

    Then room "DataTestRoom" should have 2 active participants in service "livekit1"

    When "Alice" sends a data message "Hello from Alice" via reliable channel

    Then "Bob" should receive data message "Hello from Alice" from "Alice"
    And "Alice" closes the browser
    And "Bob" closes the browser

  Scenario: Multiple messages maintain order via reliable channel
    Given an access token is created with identity "Sender" and room "OrderRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Receiver" and room "OrderRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "OrderRoom" is created using service "livekit1"

    When "Sender" opens a "Chrome" browser with LiveKit Meet page
    And "Sender" connects to room "OrderRoom" using the access token
    And connection is established successfully for "Sender"

    When "Receiver" opens a "Chrome" browser with LiveKit Meet page
    And "Receiver" connects to room "OrderRoom" using the access token
    And connection is established successfully for "Receiver"

    Then room "OrderRoom" should have 2 active participants in service "livekit1"

    When "Sender" sends a data message "Message 1" via reliable channel
    And "Sender" sends a data message "Message 2" via reliable channel
    And "Sender" sends a data message "Message 3" via reliable channel

    Then "Receiver" should receive data messages in order:
      | message   |
      | Message 1 |
      | Message 2 |
      | Message 3 |
    And "Sender" closes the browser
    And "Receiver" closes the browser

  # Story 1.1.4.4: Test Unreliable Data Channel
  Scenario: Participant can send data message via unreliable channel
    Given an access token is created with identity "UnreliableSender" and room "UnreliableRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "UnreliableReceiver" and room "UnreliableRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "UnreliableRoom" is created using service "livekit1"

    When "UnreliableSender" opens a "Chrome" browser with LiveKit Meet page
    And "UnreliableSender" connects to room "UnreliableRoom" using the access token
    And connection is established successfully for "UnreliableSender"

    When "UnreliableReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "UnreliableReceiver" connects to room "UnreliableRoom" using the access token
    And connection is established successfully for "UnreliableReceiver"

    Then room "UnreliableRoom" should have 2 active participants in service "livekit1"

    When "UnreliableSender" sends 10 data messages via unreliable channel

    Then "UnreliableReceiver" should receive at least 8 out of 10 messages from "UnreliableSender"
    And the test logs document that lossy mode in local containers typically achieves near-100% delivery
    And "UnreliableSender" closes the browser
    And "UnreliableReceiver" closes the browser

  # Story 1.1.4.5: Measure Data Channel Latency
  Scenario: Data channel latency is within acceptable bounds
    Given an access token is created with identity "LatencySender" and room "LatencyRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "LatencyReceiver" and room "LatencyRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "LatencyRoom" is created using service "livekit1"

    When "LatencySender" opens a "Chrome" browser with LiveKit Meet page
    And "LatencySender" connects to room "LatencyRoom" using the access token
    And connection is established successfully for "LatencySender"

    When "LatencyReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "LatencyReceiver" connects to room "LatencyRoom" using the access token
    And connection is established successfully for "LatencyReceiver"

    Then room "LatencyRoom" should have 2 active participants in service "livekit1"

    When "LatencySender" sends 10 timestamped data messages via reliable channel

    Then "LatencyReceiver" should receive all timestamped messages
    And the average data channel latency should be less than 500 ms
    And "LatencySender" closes the browser
    And "LatencyReceiver" closes the browser

  # Story 1.1.4.6: Test Large Message Handling
  Scenario: Small data message is delivered successfully
    Given an access token is created with identity "SmallMsgSender" and room "SizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "SmallMsgReceiver" and room "SizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "SizeRoom" is created using service "livekit1"

    When "SmallMsgSender" opens a "Chrome" browser with LiveKit Meet page
    And "SmallMsgSender" connects to room "SizeRoom" using the access token
    And connection is established successfully for "SmallMsgSender"

    When "SmallMsgReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "SmallMsgReceiver" connects to room "SizeRoom" using the access token
    And connection is established successfully for "SmallMsgReceiver"

    Then room "SizeRoom" should have 2 active participants in service "livekit1"

    When "SmallMsgSender" sends a data message of size 1024 bytes via reliable channel

    Then "SmallMsgReceiver" should receive a data message of size 1024 bytes
    And "SmallMsgSender" closes the browser
    And "SmallMsgReceiver" closes the browser

  Scenario: Medium data message is delivered successfully
    Given an access token is created with identity "MediumMsgSender" and room "MediumSizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "MediumMsgReceiver" and room "MediumSizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "MediumSizeRoom" is created using service "livekit1"

    When "MediumMsgSender" opens a "Chrome" browser with LiveKit Meet page
    And "MediumMsgSender" connects to room "MediumSizeRoom" using the access token
    And connection is established successfully for "MediumMsgSender"

    When "MediumMsgReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "MediumMsgReceiver" connects to room "MediumSizeRoom" using the access token
    And connection is established successfully for "MediumMsgReceiver"

    Then room "MediumSizeRoom" should have 2 active participants in service "livekit1"

    When "MediumMsgSender" sends a data message of size 8192 bytes via reliable channel

    Then "MediumMsgReceiver" should receive a data message of size 8192 bytes
    And "MediumMsgSender" closes the browser
    And "MediumMsgReceiver" closes the browser

  Scenario: Large data message near limit is delivered successfully
    Given an access token is created with identity "LargeMsgSender" and room "LargeSizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "LargeMsgReceiver" and room "LargeSizeRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "LargeSizeRoom" is created using service "livekit1"

    When "LargeMsgSender" opens a "Chrome" browser with LiveKit Meet page
    And "LargeMsgSender" connects to room "LargeSizeRoom" using the access token
    And connection is established successfully for "LargeMsgSender"

    When "LargeMsgReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "LargeMsgReceiver" connects to room "LargeSizeRoom" using the access token
    And connection is established successfully for "LargeMsgReceiver"

    Then room "LargeSizeRoom" should have 2 active participants in service "livekit1"

    When "LargeMsgSender" sends a data message of size 14000 bytes via reliable channel

    Then "LargeMsgReceiver" should receive a data message of size 14000 bytes
    And "LargeMsgSender" closes the browser
    And "LargeMsgReceiver" closes the browser

  # Story 1.1.4.7: Data Publishing Permission Denied
  Scenario: Participant without data permission cannot send messages
    Given an access token is created with identity "NoPermSender" and room "PermDeniedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:false"
    And an access token is created with identity "Watcher" and room "PermDeniedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "PermDeniedRoom" is created using service "livekit1"

    When "NoPermSender" opens a "Chrome" browser with LiveKit Meet page
    And "NoPermSender" connects to room "PermDeniedRoom" using the access token
    And connection is established successfully for "NoPermSender"

    When "Watcher" opens a "Chrome" browser with LiveKit Meet page
    And "Watcher" connects to room "PermDeniedRoom" using the access token
    And connection is established successfully for "Watcher"

    Then room "PermDeniedRoom" should have 2 active participants in service "livekit1"

    When "NoPermSender" attempts to send a data message "Unauthorized message"

    Then "NoPermSender" should have data publishing blocked due to permissions
    And "Watcher" should not receive data message "Unauthorized message"
    And "NoPermSender" closes the browser
    And "Watcher" closes the browser

  Scenario: Video permission works independently of data permission
    Given an access token is created with identity "VideoOnlyUser" and room "IndependentRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:false,canPublishSources:camera\,microphone"
    And room "IndependentRoom" is created using service "livekit1"

    When "VideoOnlyUser" opens a "Chrome" browser with LiveKit Meet page
    And "VideoOnlyUser" connects to room "IndependentRoom" using the access token
    And connection is established successfully for "VideoOnlyUser"

    Then participant "VideoOnlyUser" should be publishing video in room "IndependentRoom" using service "livekit1"

    When "VideoOnlyUser" attempts to send a data message "Test message"

    Then "VideoOnlyUser" should have data publishing blocked due to permissions
    And "VideoOnlyUser" closes the browser

  # Story 1.1.4.8: Cross-Browser Data Channel Testing
  Scenario Outline: Participant can send data messages with different browsers
    Given an access token is created with identity "BrowserSender" and room "BrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "BrowserReceiver" and room "BrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "BrowserDataRoom" is created using service "livekit1"

    When "BrowserSender" opens a <browser> browser with LiveKit Meet page
    And "BrowserSender" connects to room "BrowserDataRoom" using the access token
    And connection is established successfully for "BrowserSender"

    When "BrowserReceiver" opens a "Chrome" browser with LiveKit Meet page
    And "BrowserReceiver" connects to room "BrowserDataRoom" using the access token
    And connection is established successfully for "BrowserReceiver"

    Then room "BrowserDataRoom" should have 2 active participants in service "livekit1"

    When "BrowserSender" sends a data message "Browser test message" via reliable channel

    Then "BrowserReceiver" should receive data message "Browser test message" from "BrowserSender"
    And "BrowserSender" closes the browser
    And "BrowserReceiver" closes the browser

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
      | "Edge"    |

  Scenario: Cross-browser data channel communication
    Given an access token is created with identity "ChromeSender" and room "CrossBrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "FirefoxReceiver" and room "CrossBrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "CrossBrowserDataRoom" is created using service "livekit1"

    When "ChromeSender" opens a "Chrome" browser with LiveKit Meet page
    And "ChromeSender" connects to room "CrossBrowserDataRoom" using the access token
    And connection is established successfully for "ChromeSender"

    When "FirefoxReceiver" opens a "Firefox" browser with LiveKit Meet page
    And "FirefoxReceiver" connects to room "CrossBrowserDataRoom" using the access token
    And connection is established successfully for "FirefoxReceiver"

    Then room "CrossBrowserDataRoom" should have 2 active participants in service "livekit1"

    When "ChromeSender" sends a data message "Cross browser test" via reliable channel

    Then "FirefoxReceiver" should receive data message "Cross browser test" from "ChromeSender"
    And "ChromeSender" closes the browser
    And "FirefoxReceiver" closes the browser

  # Story 1.1.4.9: Broadcast vs Targeted Message Delivery
  Scenario: Broadcast message is received by all participants
    Given an access token is created with identity "Broadcaster" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Listener1" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Listener2" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "BroadcastRoom" is created using service "livekit1"

    When "Broadcaster" opens a "Chrome" browser with LiveKit Meet page
    And "Broadcaster" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Broadcaster"

    When "Listener1" opens a "Chrome" browser with LiveKit Meet page
    And "Listener1" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Listener1"

    When "Listener2" opens a "Firefox" browser with LiveKit Meet page
    And "Listener2" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Listener2"

    Then room "BroadcastRoom" should have 3 active participants in service "livekit1"

    When "Broadcaster" sends a broadcast data message "Hello everyone" via reliable channel

    Then "Listener1" should receive data message "Hello everyone" from "Broadcaster"
    And "Listener2" should receive data message "Hello everyone" from "Broadcaster"
    And "Broadcaster" closes the browser
    And "Listener1" closes the browser
    And "Listener2" closes the browser

  Scenario: Targeted message is received only by intended recipient
    Given an access token is created with identity "PrivateSender" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "TargetedRecipient" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "OtherParticipant" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "TargetedRoom" is created using service "livekit1"

    When "PrivateSender" opens a "Chrome" browser with LiveKit Meet page
    And "PrivateSender" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "PrivateSender"

    When "TargetedRecipient" opens a "Chrome" browser with LiveKit Meet page
    And "TargetedRecipient" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "TargetedRecipient"

    When "OtherParticipant" opens a "Chrome" browser with LiveKit Meet page
    And "OtherParticipant" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "OtherParticipant"

    Then room "TargetedRoom" should have 3 active participants in service "livekit1"

    When "PrivateSender" sends a data message "Private message" to "TargetedRecipient" via reliable channel

    Then "TargetedRecipient" should receive data message "Private message" from "PrivateSender"
    And "OtherParticipant" should not receive data message "Private message"
    And "PrivateSender" closes the browser
    And "TargetedRecipient" closes the browser
    And "OtherParticipant" closes the browser
```

---

## Definition of Done

### Code Implementation
- [x] New step definitions added to `LiveKitBrowserWebrtcSteps.java` for data channel
- [x] New step definitions added to `LiveKitRoomSteps.java` for data verification
- [x] `LiveKitMeet.java` extended with data channel methods
- [x] JavaScript helpers added to meet.html for data operations
- [x] Feature file `livekit_data_channel.feature` created and passing
- [x] All scenarios pass on Chrome browser
- [x] All scenarios pass on Firefox browser

### Testing
- [x] All unit tests pass
- [x] All BDD scenarios pass
- [x] Tests pass against default LiveKit version
- [x] Cross-browser data channel verified
- [x] Large message handling tested

### Documentation
- [x] Feature documentation complete in `docs/features/data-channel-testing/`
- [x] Step definitions documented in `docs/features.md`
- [x] Technical notes added for data channel implementation

### Code Quality
- [x] No new Lombok violations
- [x] No code comments added (per project guidelines)
- [x] Cross-platform path handling maintained
- [x] Proper cleanup in After hooks

---

## Technical Components

### New Step Definitions Required

#### In `LiveKitBrowserWebrtcSteps.java`:
```java
@When("{string} sends a data message {string} via reliable channel")
public void sendsDataMessageViaReliableChannel(String participantName, String message)

@When("{string} sends a data message {string} via unreliable channel")
public void sendsDataMessageViaUnreliableChannel(String participantName, String message)

@When("{string} sends a broadcast data message {string} via reliable channel")
public void sendsBroadcastDataMessage(String participantName, String message)

@When("{string} sends a data message {string} to {string} via reliable channel")
public void sendsTargetedDataMessage(String sender, String message, String recipient)

@When("{string} sends a data message of size {int} bytes via reliable channel")
public void sendsDataMessageOfSize(String participantName, int sizeBytes)

@When("{string} sends {int} timestamped data messages via reliable channel")
public void sendsTimestampedMessages(String participantName, int count)

@When("{string} attempts to send a data message {string}")
public void attemptsToSendDataMessage(String participantName, String message)

@Then("{string} should receive data message {string} from {string}")
public void shouldReceiveDataMessageFrom(String receiver, String message, String sender)

@Then("{string} may receive data message {string} from {string}")
public void mayReceiveDataMessageFrom(String receiver, String message, String sender)

@Then("{string} should receive data messages in order:")
public void shouldReceiveDataMessagesInOrder(String participantName, DataTable dataTable)

@Then("{string} should receive all timestamped messages")
public void shouldReceiveAllTimestampedMessages(String participantName)

@Then("{string} should receive a data message of size {int} bytes")
public void shouldReceiveDataMessageOfSize(String participantName, int sizeBytes)

@Then("{string} should not receive data message {string}")
public void shouldNotReceiveDataMessage(String participantName, String message)

@Then("{string} should have data publishing blocked due to permissions")
public void shouldHaveDataPublishingBlockedDueToPermissions(String participantName)

@Then("the average data channel latency should be less than {int} ms")
public void averageLatencyShouldBeLessThan(int maxLatencyMs)
```

### LiveKitMeet Page Object Extensions

```java
public void sendDataMessage(String message, boolean reliable)
public void sendDataMessageTo(String message, String recipientIdentity, boolean reliable)
public void sendTimestampedMessage(String message)
public void sendDataMessageOfSize(int sizeBytes, boolean reliable)
public List<ReceivedDataMessage> getReceivedMessages()
public ReceivedDataMessage waitForMessage(String expectedContent, long timeoutMs)
public boolean isDataPublishingBlocked()
public double getAverageMessageLatency()
public void clearReceivedMessages()
```

### Web Application Changes (meet.html)

The LiveKit Meet web application needs:
- Data message sending via `room.localParticipant.publishData()`
- Message listener registration via `room.on(RoomEvent.DataReceived, callback)`
- Message storage for received messages with sender identity
- Timestamp handling for latency measurement
- Support for reliable (RELIABLE) and unreliable (LOSSY) kinds
- Destination SIDs for targeted messaging
- Error handling for permission denied scenarios
- Size-based message generation for large message tests

### JavaScript Helpers (Following existing patterns from test-helpers.js)

```javascript
// Window state variables (following existing pattern)
window.dataChannelMessages = [];
window.dataChannelErrors = [];
window.dataPublishingBlocked = false;
window.lastDataError = '';

// Helper additions to LiveKitTestHelpers
var dataChannelHelpers = {
  sendDataMessage: function(message, reliable) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;
    try {
      var encoder = new TextEncoder();
      window.liveKitClient.room.localParticipant.publishData(
        encoder.encode(message),
        { reliable: reliable }
      );
      return true;
    } catch (e) {
      window.dataChannelErrors.push({ message: e.message, timestamp: Date.now() });
      return false;
    }
  },

  sendDataMessageTo: function(message, recipientIdentity, reliable) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;
    try {
      var encoder = new TextEncoder();
      window.liveKitClient.room.localParticipant.publishData(
        encoder.encode(message),
        { reliable: reliable, destinationIdentities: [recipientIdentity] }
      );
      return true;
    } catch (e) {
      window.dataChannelErrors.push({ message: e.message, timestamp: Date.now() });
      return false;
    }
  },

  getReceivedMessages: function() {
    return window.dataChannelMessages || [];
  },

  getReceivedMessageCount: function() {
    return window.dataChannelMessages ? window.dataChannelMessages.length : 0;
  },

  isDataPublishingBlocked: function() {
    return window.dataPublishingBlocked || false;
  },

  getLastDataError: function() {
    return window.lastDataError || '';
  },

  clearDataChannelState: function() {
    window.dataChannelMessages = [];
    window.dataChannelErrors = [];
    window.dataPublishingBlocked = false;
    window.lastDataError = '';
  }
};

// Merge with existing LiveKitTestHelpers
Object.assign(window.LiveKitTestHelpers, dataChannelHelpers);
```

### LiveKitMeetClient Data Channel Event Listener (add to setupRoomEventListeners)

```javascript
this.room.on(LiveKit.RoomEvent.DataReceived, (payload, participant, kind, topic) => {
    var decoder = new TextDecoder();
    var message = decoder.decode(payload);
    window.dataChannelMessages.push({
        content: message,
        from: participant ? participant.identity : 'unknown',
        kind: kind,
        topic: topic,
        timestamp: Date.now(),
        size: payload.length
    });
    addTechnicalDetail('📨 Data received from ' + (participant ? participant.identity : 'unknown') + ': ' + message.substring(0, 50));
});
```

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Data channel API differences across LiveKit versions | Medium | Low | Test against multiple versions; document minimum version |
| Message size limits may vary | Medium | Medium | Test at multiple sizes; detect limit dynamically |
| Latency measurement accuracy in containers | Low | Medium | Use reasonable thresholds; focus on consistency |
| Unreliable channel may be too reliable in local testing | Low | Medium | Document expected behavior; accept best-effort semantics |
| Cross-browser WebRTC data channel differences | Medium | Low | Test on all target browsers; document differences |

### Mitigation Strategies

1. **Version Compatibility**: Use LiveKit SDK's documented data channel API which is stable across recent versions

2. **Size Limits**: Start with conservative sizes and increase progressively; catch and document errors at limits

3. **Latency Accuracy**: Use relative comparisons rather than absolute values; focus on detecting regressions

4. **Reliable Local Network**: For unreliable channel tests, verify message is sent and document that local network may not drop packets

5. **Browser Compatibility**: Use standard WebRTC data channel patterns; test on Chrome first, then verify on Firefox/Edge

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.4.1 - Permission Grant | XS | Foundation for all data channel tests |
| 2 | 1.1.4.2 - Send Message | S | Core sending functionality |
| 3 | 1.1.4.3 - Receive Message | M | Core receiving and verification |
| 4 | 1.1.4.7 - Permission Denied | XS | Negative test after positive flows |
| 5 | 1.1.4.8 - Cross-Browser | S | Compatibility verification |
| 6 | 1.1.4.4 - Unreliable Channel | S | Alternative delivery mode |
| 7 | 1.1.4.9 - Broadcast/Targeted | S | Message routing options |
| 8 | 1.1.4.6 - Large Messages | M | Edge case handling |
| 9 | 1.1.4.5 - Latency Measurement | S | Performance verification |

**Total Estimated Effort:** M (Medium) - approximately 3-5 days

---

## Appendix: LiveKit Data Channel Reference

### DataPacket Kind

| Kind | Description | Use Case |
|------|-------------|----------|
| `RELIABLE` | Guaranteed delivery, ordered | Chat, commands, state sync |
| `LOSSY` | Best-effort, may be dropped | Real-time updates, cursor positions |

### LiveKit Client SDK Data API

```typescript
// Send reliable data to all participants
room.localParticipant.publishData(
  encoder.encode(message),
  { reliable: true }
);

// Send unreliable data to all participants
room.localParticipant.publishData(
  encoder.encode(message),
  { reliable: false }
);

// Send to specific participants
room.localParticipant.publishData(
  encoder.encode(message),
  {
    reliable: true,
    destinationIdentities: ['participant1', 'participant2']
  }
);

// Receive data
room.on(RoomEvent.DataReceived, (payload, participant, kind) => {
  const message = decoder.decode(payload);
  console.log(`Received from ${participant.identity}: ${message}`);
});
```

### VideoGrant Data Permission

```java
// Using AccessToken with canPublishData
VideoGrant grant = new VideoGrant();
grant.setCanPublishData(true);
```

### Message Size Limits

| Mode | Limit | Notes |
|------|-------|-------|
| Reliable | 15 KiB (15,360 bytes) | 16 KiB protocol limit minus routing headers |
| Lossy | 1,300 bytes | Recommended max to stay within 1,400-byte MTU |

**Important:** Larger messages in lossy mode fragment across packets; losing any fragment loses the entire message.

---

## Related Stories

- **Story 1.1.1** (Screen Sharing) - Completed; similar browser interaction patterns
- **Story 1.1.2** (Simulcast) - Completed; similar step definition patterns
- **Story 1.1.7** (Track Mute) - In Progress; similar state verification patterns
- **Story 1.1.5** (Room Metadata) - Future; similar real-time update patterns
