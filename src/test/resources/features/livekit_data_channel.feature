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
    And the average data channel latency for "LatencyReceiver" should be less than 500 ms
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
