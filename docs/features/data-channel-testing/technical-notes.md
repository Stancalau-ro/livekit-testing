# Data Channel Communication Testing - Technical Implementation Notes

## Architecture Overview

Data channel testing extends the existing WebRTC testing infrastructure to verify that participants can send and receive data messages through LiveKit's data channel API. The feature integrates with:

1. **Browser Layer (LiveKitMeet)** - JavaScript client handles data message sending and receiving
2. **Page Object Layer (LiveKitMeet.java)** - Selenium interactions for data channel operations
3. **Step Definition Layer** - Cucumber bindings for BDD scenarios
4. **Server Verification Layer** - Optional webhook integration for data packet events
5. **State Management Layer** - Message queue tracking for verification

```
+------------------+     +-------------------+     +------------------+
|   Feature File   | --> | Step Definitions  | --> |  LiveKitMeet.java|
| (Gherkin/BDD)    |     | (Cucumber)        |     |  (Page Object)   |
+------------------+     +-------------------+     +------------------+
                                 |                         |
                                 v                         v
                         +---------------+         +------------------+
                         | Token Manager |         | JavaScript       |
                         | (canPublishData)|        | (livekit-client) |
                         +---------------+         +------------------+
                                 |                         |
                                 v                         v
                         +----------------------------------------+
                         |          LiveKit Server                |
                         |    (DataPacket routing, permissions)   |
                         +----------------------------------------+
```

---

## Component Changes

### 1. JavaScript Test Helpers (test-helpers.js) Extensions

Add data channel helper functions to the existing `LiveKitTestHelpers` object in `src/test/resources/web/livekit-meet/js/test-helpers.js`:

```javascript
sendDataMessage: function(message, reliable, destinationIdentities, topic) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;
    return window.liveKitClient.sendDataMessage(message, reliable, destinationIdentities, topic);
},

sendDataMessageOfSize: function(sizeBytes, reliable) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;
    var message = window.liveKitClient.generateMessageOfSize(sizeBytes);
    return window.liveKitClient.sendDataMessage(message, reliable, null, null);
},

sendTimestampedMessage: function(message, reliable) {
    if (!window.liveKitClient || !window.liveKitClient.room) return false;
    var timestampedMsg = JSON.stringify({
        content: message,
        sendTimestamp: Date.now()
    });
    return window.liveKitClient.sendDataMessage(timestampedMsg, reliable, null, 'timestamped');
},

getReceivedDataMessages: function() {
    return window.dataChannelMessages || [];
},

getReceivedDataMessageCount: function() {
    return window.dataChannelMessages ? window.dataChannelMessages.length : 0;
},

getReceivedMessagesFromSender: function(senderIdentity) {
    if (!window.dataChannelMessages) return [];
    return window.dataChannelMessages.filter(function(msg) {
        return msg.from === senderIdentity;
    });
},

getLastDataMessage: function() {
    if (!window.dataChannelMessages || window.dataChannelMessages.length === 0) return null;
    return window.dataChannelMessages[window.dataChannelMessages.length - 1];
},

hasReceivedMessage: function(content, fromIdentity) {
    if (!window.dataChannelMessages) return false;
    return window.dataChannelMessages.some(function(msg) {
        var contentMatch = msg.content === content || msg.content.includes(content);
        var senderMatch = !fromIdentity || msg.from === fromIdentity;
        return contentMatch && senderMatch;
    });
},

getDataChannelLatencyStats: function() {
    if (!window.dataChannelMessages) return null;
    var timestampedMessages = window.dataChannelMessages.filter(function(msg) {
        return msg.topic === 'timestamped';
    });
    if (timestampedMessages.length === 0) return null;

    var latencies = timestampedMessages.map(function(msg) {
        try {
            var parsed = JSON.parse(msg.content);
            return msg.receiveTimestamp - parsed.sendTimestamp;
        } catch (e) {
            return null;
        }
    }).filter(function(l) { return l !== null; });

    if (latencies.length === 0) return null;

    return {
        count: latencies.length,
        min: Math.min.apply(null, latencies),
        max: Math.max.apply(null, latencies),
        avg: latencies.reduce(function(a, b) { return a + b; }, 0) / latencies.length
    };
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
```

### 2. JavaScript Client (livekit-client.js) Changes

Add data channel methods to the `LiveKitMeetClient` class in `src/test/resources/web/livekit-meet/js/livekit-client.js`:

#### Constructor Initialization

Add to the constructor:

```javascript
window.dataChannelMessages = [];
window.dataChannelErrors = [];
window.dataPublishingBlocked = false;
window.lastDataError = '';
```

#### Data Channel Methods

```javascript
sendDataMessage(message, reliable = true, destinationIdentities = null, topic = null) {
    if (!this.room || !this.room.localParticipant) {
        window.lastDataError = 'Not connected to room';
        return false;
    }

    try {
        var encoder = new TextEncoder();
        var data = encoder.encode(message);

        var options = {
            reliable: reliable
        };

        if (destinationIdentities && destinationIdentities.length > 0) {
            options.destinationIdentities = destinationIdentities;
        }

        if (topic) {
            options.topic = topic;
        }

        this.room.localParticipant.publishData(data, options);

        addTechnicalDetail('Data message sent: ' + message.substring(0, 50) +
            (message.length > 50 ? '...' : '') +
            ' (reliable: ' + reliable + ')');

        window.dataPublishingBlocked = false;
        return true;

    } catch (error) {
        console.error('Failed to send data message:', error);
        addTechnicalDetail('Data send failed: ' + error.message);

        window.dataChannelErrors.push({
            message: error.message,
            timestamp: Date.now()
        });

        var errorMsg = error.message ? error.message.toLowerCase() : '';
        if (errorMsg.includes('permission') || errorMsg.includes('denied') ||
            errorMsg.includes('not allowed') || errorMsg.includes('forbidden')) {
            window.dataPublishingBlocked = true;
        }

        window.lastDataError = error.message || 'Unknown error';
        return false;
    }
}

generateMessageOfSize(sizeBytes) {
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    var result = '';
    for (var i = 0; i < sizeBytes; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}
```

#### Event Listener (add to setupRoomEventListeners)

```javascript
this.room.on(LiveKit.RoomEvent.DataReceived, (payload, participant, kind, topic) => {
    var decoder = new TextDecoder();
    var message = decoder.decode(payload);
    var receiveTimestamp = Date.now();

    var dataMessage = {
        content: message,
        from: participant ? participant.identity : 'unknown',
        kind: kind === LiveKit.DataPacket_Kind.RELIABLE ? 'RELIABLE' : 'LOSSY',
        topic: topic || null,
        receiveTimestamp: receiveTimestamp,
        size: payload.length
    };

    window.dataChannelMessages.push(dataMessage);

    addTechnicalDetail('Data received from ' + dataMessage.from +
        ': ' + message.substring(0, 50) + (message.length > 50 ? '...' : '') +
        ' (' + dataMessage.kind + ')');

    console.log('DataReceived event:', dataMessage);
});
```

### 3. LiveKitMeet.java Page Object Extensions

Add to `src/main/java/ro/stancalau/test/framework/selenium/LiveKitMeet.java`:

```java
public boolean sendDataMessage(String message, boolean reliable) {
    Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.sendDataMessage(arguments[0], arguments[1], null, null);",
        message, reliable
    );
    log.info("LiveKitMeet sent data message: {} (reliable: {})",
        message.substring(0, Math.min(50, message.length())), reliable);
    return result != null && result;
}

public boolean sendDataMessageTo(String message, String recipientIdentity, boolean reliable) {
    Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.sendDataMessage(arguments[0], arguments[1], [arguments[2]], null);",
        message, reliable, recipientIdentity
    );
    log.info("LiveKitMeet sent targeted data message to {}: {} (reliable: {})",
        recipientIdentity, message.substring(0, Math.min(50, message.length())), reliable);
    return result != null && result;
}

public boolean sendBroadcastDataMessage(String message, boolean reliable) {
    return sendDataMessage(message, reliable);
}

public boolean sendDataMessageOfSize(int sizeBytes, boolean reliable) {
    Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.sendDataMessageOfSize(arguments[0], arguments[1]);",
        sizeBytes, reliable
    );
    log.info("LiveKitMeet sent data message of size {} bytes (reliable: {})", sizeBytes, reliable);
    return result != null && result;
}

public boolean sendTimestampedMessage(String message, boolean reliable) {
    Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.sendTimestampedMessage(arguments[0], arguments[1]);",
        message, reliable
    );
    log.info("LiveKitMeet sent timestamped message: {}", message);
    return result != null && result;
}

public int getReceivedDataMessageCount() {
    Long count = (Long) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.getReceivedDataMessageCount();"
    );
    return count != null ? count.intValue() : 0;
}

@SuppressWarnings("unchecked")
public List<Map<String, Object>> getReceivedDataMessages() {
    Object result = ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.getReceivedDataMessages();"
    );
    if (result instanceof List) {
        return (List<Map<String, Object>>) result;
    }
    return new ArrayList<>();
}

public boolean hasReceivedMessage(String content, String fromIdentity) {
    Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.hasReceivedMessage(arguments[0], arguments[1]);",
        content, fromIdentity
    );
    return result != null && result;
}

public boolean waitForMessage(String content, String fromIdentity, int timeoutSeconds) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    try {
        wait.until(d -> hasReceivedMessage(content, fromIdentity));
        return true;
    } catch (TimeoutException e) {
        log.warn("Timeout waiting for message '{}' from '{}'", content, fromIdentity);
        return false;
    }
}

public boolean waitForMessageCount(int expectedCount, int timeoutSeconds) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    try {
        wait.until(d -> getReceivedDataMessageCount() >= expectedCount);
        return true;
    } catch (TimeoutException e) {
        log.warn("Timeout waiting for {} messages, received {}", expectedCount, getReceivedDataMessageCount());
        return false;
    }
}

@SuppressWarnings("unchecked")
public Map<String, Object> getDataChannelLatencyStats() {
    Object result = ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.getDataChannelLatencyStats();"
    );
    if (result instanceof Map) {
        return (Map<String, Object>) result;
    }
    return null;
}

public boolean isDataPublishingBlocked() {
    Boolean blocked = (Boolean) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.isDataPublishingBlocked();"
    );
    return blocked != null && blocked;
}

public String getLastDataError() {
    String error = (String) ((JavascriptExecutor) driver).executeScript(
        "return window.LiveKitTestHelpers.getLastDataError();"
    );
    return error != null ? error : "";
}

public void clearDataChannelState() {
    ((JavascriptExecutor) driver).executeScript(
        "window.LiveKitTestHelpers.clearDataChannelState();"
    );
    log.info("LiveKitMeet data channel state cleared");
}
```

### 4. Step Definitions (LiveKitBrowserWebrtcSteps.java)

Add to `src/test/java/ro/stancalau/test/bdd/steps/LiveKitBrowserWebrtcSteps.java`:

```java
@When("{string} sends a data message {string} via reliable channel")
public void sendsDataMessageViaReliableChannel(String participantName, String message) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    boolean sent = meetInstance.sendDataMessage(message, true);
    assertTrue(sent, participantName + " should successfully send data message");
}

@When("{string} sends a data message {string} via unreliable channel")
public void sendsDataMessageViaUnreliableChannel(String participantName, String message) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    boolean sent = meetInstance.sendDataMessage(message, false);
    assertTrue(sent, participantName + " should successfully send data message via lossy channel");
}

@When("{string} sends a broadcast data message {string} via reliable channel")
public void sendsBroadcastDataMessage(String participantName, String message) {
    sendsDataMessageViaReliableChannel(participantName, message);
}

@When("{string} sends a data message {string} to {string} via reliable channel")
public void sendsTargetedDataMessage(String sender, String message, String recipient) {
    LiveKitMeet meetInstance = meetInstances.get(sender);
    assertNotNull(meetInstance, "Meet instance should exist for " + sender);
    boolean sent = meetInstance.sendDataMessageTo(message, recipient, true);
    assertTrue(sent, sender + " should successfully send targeted data message to " + recipient);
}

@When("{string} sends a data message of size {int} bytes via reliable channel")
public void sendsDataMessageOfSize(String participantName, int sizeBytes) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    boolean sent = meetInstance.sendDataMessageOfSize(sizeBytes, true);
    assertTrue(sent, participantName + " should successfully send data message of size " + sizeBytes);
}

@When("{string} sends {int} timestamped data messages via reliable channel")
public void sendsTimestampedMessages(String participantName, int count) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    for (int i = 0; i < count; i++) {
        boolean sent = meetInstance.sendTimestampedMessage("Timestamped message " + (i + 1), true);
        assertTrue(sent, participantName + " should send timestamped message " + (i + 1));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

@When("{string} sends {int} data messages via unreliable channel")
public void sendsMultipleUnreliableMessages(String participantName, int count) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    for (int i = 0; i < count; i++) {
        meetInstance.sendDataMessage("Lossy message " + (i + 1), false);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

@When("{string} attempts to send a data message {string}")
public void attemptsToSendDataMessage(String participantName, String message) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);
    meetInstance.sendDataMessage(message, true);
}

@Then("{string} should receive data message {string} from {string}")
public void shouldReceiveDataMessageFrom(String receiver, String message, String sender) {
    LiveKitMeet meetInstance = meetInstances.get(receiver);
    assertNotNull(meetInstance, "Meet instance should exist for " + receiver);

    boolean received = meetInstance.waitForMessage(message, sender, 10);
    assertTrue(received, receiver + " should receive message '" + message + "' from " + sender);
}

@Then("{string} should receive data messages in order:")
public void shouldReceiveDataMessagesInOrder(String participantName, io.cucumber.datatable.DataTable dataTable) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    List<String> expectedMessages = dataTable.asList().subList(1, dataTable.asList().size());

    boolean receivedAll = meetInstance.waitForMessageCount(expectedMessages.size(), 15);
    assertTrue(receivedAll, participantName + " should receive all " + expectedMessages.size() + " messages");

    List<Map<String, Object>> receivedMessages = meetInstance.getReceivedDataMessages();

    for (int i = 0; i < expectedMessages.size(); i++) {
        String expectedContent = expectedMessages.get(i);
        assertTrue(i < receivedMessages.size(), "Should have message at index " + i);
        String actualContent = (String) receivedMessages.get(i).get("content");
        assertEquals(expectedContent, actualContent,
            "Message " + (i + 1) + " should match. Expected: " + expectedContent + ", Actual: " + actualContent);
    }
}

@Then("{string} should receive all timestamped messages")
public void shouldReceiveAllTimestampedMessages(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    boolean receivedAll = meetInstance.waitForMessageCount(10, 15);
    assertTrue(receivedAll, participantName + " should receive all timestamped messages");
}

@Then("{string} should receive a data message of size {int} bytes")
public void shouldReceiveDataMessageOfSize(String participantName, int sizeBytes) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    boolean received = meetInstance.waitForMessageCount(1, 10);
    assertTrue(received, participantName + " should receive at least one message");

    List<Map<String, Object>> messages = meetInstance.getReceivedDataMessages();
    boolean foundCorrectSize = messages.stream()
        .anyMatch(msg -> {
            Object sizeObj = msg.get("size");
            int msgSize = sizeObj instanceof Number ? ((Number) sizeObj).intValue() : 0;
            return Math.abs(msgSize - sizeBytes) < 100;
        });
    assertTrue(foundCorrectSize, participantName + " should receive message of approximately " + sizeBytes + " bytes");
}

@Then("{string} should receive at least {int} out of {int} messages from {string}")
public void shouldReceiveAtLeastMessages(String receiver, int minCount, int totalCount, String sender) {
    LiveKitMeet meetInstance = meetInstances.get(receiver);
    assertNotNull(meetInstance, "Meet instance should exist for " + receiver);

    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    int receivedCount = meetInstance.getReceivedDataMessageCount();
    assertTrue(receivedCount >= minCount,
        receiver + " should receive at least " + minCount + " out of " + totalCount +
        " messages. Received: " + receivedCount);
}

@Then("{string} should not receive data message {string}")
public void shouldNotReceiveDataMessage(String participantName, String message) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    boolean received = meetInstance.hasReceivedMessage(message, null);
    assertFalse(received, participantName + " should NOT receive message '" + message + "'");
}

@Then("{string} should have data publishing blocked due to permissions")
public void shouldHaveDataPublishingBlockedDueToPermissions(String participantName) {
    LiveKitMeet meetInstance = meetInstances.get(participantName);
    assertNotNull(meetInstance, "Meet instance should exist for " + participantName);

    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    boolean blocked = meetInstance.isDataPublishingBlocked();
    String lastError = meetInstance.getLastDataError();

    assertTrue(blocked || !lastError.isEmpty(),
        participantName + " should have data publishing blocked. Blocked: " + blocked + ", Error: " + lastError);
}

@Then("the average data channel latency should be less than {int} ms")
public void averageLatencyShouldBeLessThan(int maxLatencyMs) {
    Map<String, Object> stats = null;
    for (LiveKitMeet meetInstance : meetInstances.values()) {
        stats = meetInstance.getDataChannelLatencyStats();
        if (stats != null) break;
    }

    assertNotNull(stats, "Should have latency statistics");

    Object avgObj = stats.get("avg");
    double avgLatency = avgObj instanceof Number ? ((Number) avgObj).doubleValue() : Double.MAX_VALUE;

    assertTrue(avgLatency < maxLatencyMs,
        "Average latency " + avgLatency + "ms should be less than " + maxLatencyMs + "ms");

    log.info("Data channel latency stats - min: {}ms, max: {}ms, avg: {}ms",
        stats.get("min"), stats.get("max"), stats.get("avg"));
}

@Then("the test logs document that lossy mode in local containers typically achieves near-100% delivery")
public void logsDocumentLossyBehavior() {
    log.info("NOTE: In local containerized testing, lossy/unreliable mode typically achieves " +
        "near-100% delivery due to absence of real network conditions. " +
        "This test verifies the API works correctly; actual packet loss testing requires " +
        "network simulation tools like tc (traffic control).");
}
```

---

## Data Flow

### Send Data Message Flow

```
1. Step: "Alice" sends a data message "Hello" via reliable channel
       |
       v
2. LiveKitBrowserWebrtcSteps.sendsDataMessageViaReliableChannel()
       |
       v
3. LiveKitMeet.sendDataMessage("Hello", true)
       |
       v
4. JavaScript: window.LiveKitTestHelpers.sendDataMessage("Hello", true, null, null)
       |
       v
5. LiveKitMeetClient.sendDataMessage()
       |
       v
6. TextEncoder.encode("Hello") -> Uint8Array
       |
       v
7. room.localParticipant.publishData(data, { reliable: true })
       |
       v
8. LiveKit SDK sends data packet via SCTP data channel
       |
       v
9. LiveKit Server routes data packet to recipients
       |
       v
10. Recipients receive DataReceived event
```

### Receive Data Message Flow

```
1. LiveKit Server delivers DataPacket to client
       |
       v
2. RoomEvent.DataReceived fires with (payload, participant, kind, topic)
       |
       v
3. Event listener in livekit-client.js
       |
       v
4. TextDecoder.decode(payload) -> message string
       |
       v
5. Push to window.dataChannelMessages array:
   {
       content: "Hello",
       from: "Alice",
       kind: "RELIABLE",
       topic: null,
       receiveTimestamp: 1702900000000,
       size: 5
   }
       |
       v
6. Step verification polls getReceivedDataMessages()
       |
       v
7. Assert message content and sender match expected
```

### Targeted Message Routing

```
3 Participants: Sender, Target, Other

1. Sender sends: publishData(msg, { destinationIdentities: ["Target"] })
       |
       v
2. LiveKit Server checks destinationIdentities
       |
       v
3. Server routes ONLY to "Target" participant
       |
       v
4. "Target" receives DataReceived event
       |
       v
5. "Other" does NOT receive event
       |
       v
6. Test verifies:
   - Target.hasReceivedMessage("msg", "Sender") == true
   - Other.hasReceivedMessage("msg", "Sender") == false
```

---

## State Management Approach

### Message Queue Pattern

The data channel implementation uses a window-global message queue pattern consistent with other LiveKit event tracking in the codebase:

```javascript
window.dataChannelMessages = [];
```

Each received message is pushed with full metadata:

```javascript
{
    content: string,        // Decoded message content
    from: string,           // Sender identity
    kind: string,           // "RELIABLE" or "LOSSY"
    topic: string | null,   // Optional topic for filtering
    receiveTimestamp: number, // Date.now() at receipt
    size: number            // Payload byte length
}
```

### State Reset

State is cleared in two scenarios:

1. **Per-scenario cleanup**: The `@After` hook in step definitions should call `clearDataChannelState()` if data channel tests were run
2. **Explicit step**: A step definition can be added for scenarios requiring mid-test state reset

### Error State Tracking

```javascript
window.dataPublishingBlocked = false;  // Set true on permission errors
window.lastDataError = '';              // Last error message
window.dataChannelErrors = [];          // Error history with timestamps
```

---

## Error Handling Strategy

### Permission Denied Detection

When a participant without `canPublishData` permission attempts to send:

1. **Client-side**: The LiveKit SDK throws an error on `publishData()`
2. **Error capture**: Catch block checks error message for permission keywords
3. **State update**: Set `window.dataPublishingBlocked = true`
4. **Test verification**: `isDataPublishingBlocked()` returns true

```javascript
try {
    this.room.localParticipant.publishData(data, options);
} catch (error) {
    var errorMsg = error.message ? error.message.toLowerCase() : '';
    if (errorMsg.includes('permission') || errorMsg.includes('denied') ||
        errorMsg.includes('not allowed') || errorMsg.includes('forbidden')) {
        window.dataPublishingBlocked = true;
    }
    window.lastDataError = error.message || 'Unknown error';
    return false;
}
```

### Message Size Limit Handling

| Channel | Limit | Behavior on Exceed |
|---------|-------|-------------------|
| Reliable | 15 KiB (15,360 bytes) | SDK throws error |
| Lossy | 1,300 bytes | Message may fragment and fail |

The implementation should:
1. Log warnings for messages approaching limits
2. Capture errors when limits are exceeded
3. Provide clear test assertions for size-related failures

### Timeout Handling

Message receipt verification uses polling with timeout:

```java
public boolean waitForMessage(String content, String fromIdentity, int timeoutSeconds) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    try {
        wait.until(d -> hasReceivedMessage(content, fromIdentity));
        return true;
    } catch (TimeoutException e) {
        log.warn("Timeout waiting for message '{}' from '{}'", content, fromIdentity);
        return false;
    }
}
```

Default timeout: 10 seconds for single message, 15 seconds for multiple messages.

---

## Testing Considerations

### Implementation Order

| Phase | Story | Focus | Key Files |
|-------|-------|-------|-----------|
| 1 | 1.1.4.1 | Permission Grant | AccessTokenStateManager (already done) |
| 2 | 1.1.4.2 | Send Message | test-helpers.js, livekit-client.js |
| 3 | 1.1.4.3 | Receive Message | livekit-client.js event listener |
| 4 | 1.1.4.7 | Permission Denied | Error handling in send |
| 5 | 1.1.4.8 | Cross-Browser | Step definitions with browser param |
| 6 | 1.1.4.4 | Unreliable Channel | Kind parameter handling |
| 7 | 1.1.4.9 | Broadcast/Targeted | destinationIdentities parameter |
| 8 | 1.1.4.6 | Large Messages | Size generation and verification |
| 9 | 1.1.4.5 | Latency Measurement | Timestamp embedding and stats |

### Polling Strategy

Data channel tests require polling due to asynchronous message delivery:

```java
private static final int MAX_POLLING_ATTEMPTS = 20;
private static final int POLLING_INTERVAL_MS = 500;

for (int attempt = 0; attempt < MAX_POLLING_ATTEMPTS; attempt++) {
    if (meetInstance.hasReceivedMessage(expectedContent, expectedSender)) {
        return;
    }
    Thread.sleep(POLLING_INTERVAL_MS);
}
fail("Message not received within timeout");
```

### Containerized Testing Considerations

| Factor | Impact | Mitigation |
|--------|--------|------------|
| No real network latency | Unreliable may appear 100% reliable | Document expected behavior |
| Docker network isolation | Messages routed through LiveKit server | Ensure containers share network |
| Fake media streams | Data channel unaffected | N/A |
| Browser sandbox | May affect some SCTP features | Use standard Chrome/Firefox configs |

### Cross-Browser Testing

The existing `SeleniumConfig` supports Chrome, Firefox, and Edge. Data channel testing should:

1. Run primary scenarios on Chrome (most stable)
2. Run compatibility scenarios with Scenario Outline for all browsers
3. Document any browser-specific behaviors discovered

---

## API Reference

### LiveKit Client SDK Data API

```typescript
interface DataPublishOptions {
    reliable?: boolean;           // Default: true (RELIABLE), false = LOSSY
    destinationIdentities?: string[];  // Empty/null = broadcast to all
    topic?: string;               // Optional topic for message categorization
}

room.localParticipant.publishData(
    data: Uint8Array,
    options?: DataPublishOptions
): void;

room.on(RoomEvent.DataReceived, (
    payload: Uint8Array,
    participant: RemoteParticipant | undefined,
    kind: DataPacket_Kind,
    topic?: string
) => void);
```

### DataPacket Kind Constants

```typescript
enum DataPacket_Kind {
    RELIABLE = 0,  // Ordered, guaranteed delivery via SCTP
    LOSSY = 1      // Best-effort, unordered via SCTP
}
```

### VideoGrant Data Permission

The `canPublishData` grant is already supported in `AccessTokenStateManager`:

```gherkin
Given an access token is created with identity "DataUser" and room "Room" with grants "canPublish:true,canPublishData:true"
```

---

## File Changes Summary

| File | Changes Required |
|------|------------------|
| `test-helpers.js` | Add 12+ data channel helper functions |
| `livekit-client.js` | Add DataReceived listener, sendDataMessage method |
| `LiveKitMeet.java` | Add 12+ data channel methods |
| `LiveKitBrowserWebrtcSteps.java` | Add 15+ step definitions |
| `livekit_data_channel.feature` | New feature file (from requirements.md) |

---

## Existing Infrastructure Leverage

The data channel implementation builds on existing components:

1. **Event array pattern** - `window.dataChannelMessages = []` follows `window.muteEvents = []` pattern
2. **LiveKitTestHelpers object** - Extend existing helper pattern
3. **LiveKitMeetClient class** - Add methods following existing patterns
4. **JavascriptExecutor pattern** - Standard Selenium JS execution
5. **Polling verification** - Established `waitFor*` patterns in LiveKitMeet
6. **AccessTokenStateManager** - `canPublishData` grant already supported

---

## Version Compatibility

| LiveKit Version | Data Channel Support | Notes |
|-----------------|---------------------|-------|
| v1.5.0+ | Full support | destinationIdentities, topic |
| v1.4.x | Basic support | Broadcast only |
| < v1.4.0 | Limited | Test required |

The `publishData` API with options object is available from LiveKit v1.5.0+.
