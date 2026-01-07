Feature: LiveKit Data Channel Communication
  As a test developer
  I want to test data channel messaging
  So that I can verify reliable and unreliable data delivery

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit"

  Scenario Outline: Generate access token with data publishing permission settings
    When the system creates an access token with identity "<identity>" and room "<room>" with grants "canPublish:true,canPublishData:<canPublishData>"
    Then the access token for "<identity>" in room "<room>" should be valid
    And the access token for "<identity>" in room "<room>" should have the following grants:
      | grant          | value            |
      | canPublish     | true             |
      | canPublishData | <canPublishData> |
      | roomJoin       | true             |

    Examples:
      | identity | room           | canPublishData |
      | Richard  | DataRoom       | true           |
      | Diana    | RestrictedRoom | false          |

  Scenario: Participant can send and receive data message via reliable channel
    Given an access token is created with identity "Alice" and room "DataTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Bob" and room "DataTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "DataTestRoom" is created using service "livekit"

    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "DataTestRoom" using the access token
    And connection is established successfully for "Alice"

    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" connects to room "DataTestRoom" using the access token
    And connection is established successfully for "Bob"

    Then room "DataTestRoom" should have 2 active participants in service "livekit"

    When "Alice" sends a data message "Hello from Alice" via reliable channel

    Then "Bob" should receive data message "Hello from Alice" from "Alice"
    And "Alice" closes the browser
    And "Bob" closes the browser

  Scenario: Multiple messages maintain order via reliable channel
    Given an access token is created with identity "Oliver" and room "OrderRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Emma" and room "OrderRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "OrderRoom" is created using service "livekit"

    When "Oliver" opens a "Chrome" browser with LiveKit Meet page
    And "Oliver" connects to room "OrderRoom" using the access token
    And connection is established successfully for "Oliver"

    When "Emma" opens a "Chrome" browser with LiveKit Meet page
    And "Emma" connects to room "OrderRoom" using the access token
    And connection is established successfully for "Emma"

    Then room "OrderRoom" should have 2 active participants in service "livekit"

    When "Oliver" sends a data message "Message 1" via reliable channel
    And "Oliver" sends a data message "Message 2" via reliable channel
    And "Oliver" sends a data message "Message 3" via reliable channel

    Then "Emma" should receive data messages in order:
      | message   |
      | Message 1 |
      | Message 2 |
      | Message 3 |
    And "Oliver" closes the browser
    And "Emma" closes the browser

  Scenario: Participant can send data message via unreliable channel
    Given an access token is created with identity "James" and room "UnreliableRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Sophia" and room "UnreliableRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "UnreliableRoom" is created using service "livekit"

    When "James" opens a "Chrome" browser with LiveKit Meet page
    And "James" connects to room "UnreliableRoom" using the access token
    And connection is established successfully for "James"

    When "Sophia" opens a "Chrome" browser with LiveKit Meet page
    And "Sophia" connects to room "UnreliableRoom" using the access token
    And connection is established successfully for "Sophia"

    Then room "UnreliableRoom" should have 2 active participants in service "livekit"

    When "James" sends 10 data messages via unreliable channel

    Then "Sophia" should receive at least 8 out of 10 messages from "James"
    And the test logs document that lossy mode in local containers typically achieves near-100% delivery
    And "James" closes the browser
    And "Sophia" closes the browser

  Scenario: Data channel latency is within acceptable bounds
    Given an access token is created with identity "Henry" and room "LatencyRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Grace" and room "LatencyRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "LatencyRoom" is created using service "livekit"

    When "Henry" opens a "Chrome" browser with LiveKit Meet page
    And "Henry" connects to room "LatencyRoom" using the access token
    And connection is established successfully for "Henry"

    When "Grace" opens a "Chrome" browser with LiveKit Meet page
    And "Grace" connects to room "LatencyRoom" using the access token
    And connection is established successfully for "Grace"

    Then room "LatencyRoom" should have 2 active participants in service "livekit"

    When "Henry" sends 10 timestamped data messages via reliable channel

    Then "Grace" should receive all timestamped messages
    And the average data channel latency for "Grace" should be less than 500 ms
    And "Henry" closes the browser
    And "Grace" closes the browser

  Scenario Outline: Data message of varying sizes is delivered successfully
    Given an access token is created with identity "Thomas" and room "SizeTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Charlotte" and room "SizeTestRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "SizeTestRoom" is created using service "livekit"

    When "Thomas" opens a "Chrome" browser with LiveKit Meet page
    And "Thomas" connects to room "SizeTestRoom" using the access token
    And connection is established successfully for "Thomas"

    When "Charlotte" opens a "Chrome" browser with LiveKit Meet page
    And "Charlotte" connects to room "SizeTestRoom" using the access token
    And connection is established successfully for "Charlotte"

    Then room "SizeTestRoom" should have 2 active participants in service "livekit"

    When "Thomas" sends a data message of size <size> bytes via reliable channel

    Then "Charlotte" should receive a data message of size <size> bytes
    And "Thomas" closes the browser
    And "Charlotte" closes the browser

    Examples:
      | size  |
      | 1024  |
      | 8192  |
      | 14000 |

  Scenario: Participant without data permission cannot send messages
    Given an access token is created with identity "Michael" and room "PermDeniedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:false"
    And an access token is created with identity "Isabella" and room "PermDeniedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "PermDeniedRoom" is created using service "livekit"

    When "Michael" opens a "Chrome" browser with LiveKit Meet page
    And "Michael" connects to room "PermDeniedRoom" using the access token
    And connection is established successfully for "Michael"

    When "Isabella" opens a "Chrome" browser with LiveKit Meet page
    And "Isabella" connects to room "PermDeniedRoom" using the access token
    And connection is established successfully for "Isabella"

    Then room "PermDeniedRoom" should have 2 active participants in service "livekit"

    When "Michael" attempts to send a data message "Unauthorized message"

    Then "Michael" should have data publishing blocked due to permissions
    And "Isabella" should not receive data message "Unauthorized message"
    And "Michael" closes the browser
    And "Isabella" closes the browser

  Scenario Outline: Participant can send data messages with different browsers
    Given an access token is created with identity "David" and room "BrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Mia" and room "BrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "BrowserDataRoom" is created using service "livekit"

    When "David" opens a <browser> browser with LiveKit Meet page
    And "David" connects to room "BrowserDataRoom" using the access token
    And connection is established successfully for "David"

    When "Mia" opens a "Chrome" browser with LiveKit Meet page
    And "Mia" connects to room "BrowserDataRoom" using the access token
    And connection is established successfully for "Mia"

    Then room "BrowserDataRoom" should have 2 active participants in service "livekit"

    When "David" sends a data message "Browser test message" via reliable channel

    Then "Mia" should receive data message "Browser test message" from "David"
    And "David" closes the browser
    And "Mia" closes the browser

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
      | "Edge"    |

  Scenario: Cross-browser data channel communication
    Given an access token is created with identity "Benjamin" and room "CrossBrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Victoria" and room "CrossBrowserDataRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "CrossBrowserDataRoom" is created using service "livekit"

    When "Benjamin" opens a "Chrome" browser with LiveKit Meet page
    And "Benjamin" connects to room "CrossBrowserDataRoom" using the access token
    And connection is established successfully for "Benjamin"

    When "Victoria" opens a "Firefox" browser with LiveKit Meet page
    And "Victoria" connects to room "CrossBrowserDataRoom" using the access token
    And connection is established successfully for "Victoria"

    Then room "CrossBrowserDataRoom" should have 2 active participants in service "livekit"

    When "Benjamin" sends a data message "Cross browser test" via reliable channel

    Then "Victoria" should receive data message "Cross browser test" from "Benjamin"
    And "Benjamin" closes the browser
    And "Victoria" closes the browser

  Scenario: Broadcast message is received by all participants
    Given an access token is created with identity "Nathan" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Eleanor" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Hannah" and room "BroadcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "BroadcastRoom" is created using service "livekit"

    When "Nathan" opens a "Chrome" browser with LiveKit Meet page
    And "Nathan" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Nathan"

    When "Eleanor" opens a "Chrome" browser with LiveKit Meet page
    And "Eleanor" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Eleanor"

    When "Hannah" opens a "Firefox" browser with LiveKit Meet page
    And "Hannah" connects to room "BroadcastRoom" using the access token
    And connection is established successfully for "Hannah"

    Then room "BroadcastRoom" should have 3 active participants in service "livekit"

    When "Nathan" sends a broadcast data message "Hello everyone" via reliable channel

    Then "Eleanor" should receive data message "Hello everyone" from "Nathan"
    And "Hannah" should receive data message "Hello everyone" from "Nathan"
    And "Nathan" closes the browser
    And "Eleanor" closes the browser
    And "Hannah" closes the browser

  Scenario: Targeted message is received only by intended recipient
    Given an access token is created with identity "Matthew" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Chloe" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And an access token is created with identity "Sophie" and room "TargetedRoom" with grants "canPublish:true,canSubscribe:true,canPublishData:true"
    And room "TargetedRoom" is created using service "livekit"

    When "Matthew" opens a "Chrome" browser with LiveKit Meet page
    And "Matthew" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "Matthew"

    When "Chloe" opens a "Chrome" browser with LiveKit Meet page
    And "Chloe" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "Chloe"

    When "Sophie" opens a "Chrome" browser with LiveKit Meet page
    And "Sophie" connects to room "TargetedRoom" using the access token
    And connection is established successfully for "Sophie"

    Then room "TargetedRoom" should have 3 active participants in service "livekit"

    When "Matthew" sends a data message "Private message" to "Chloe" via reliable channel

    Then "Chloe" should receive data message "Private message" from "Matthew"
    And "Sophie" should not receive data message "Private message"
    And "Matthew" closes the browser
    And "Chloe" closes the browser
    And "Sophie" closes the browser
