# Metadata Operations Testing - Technical Implementation Notes

## Architecture Overview

Metadata testing extends the existing LiveKit testing infrastructure to verify that room and participant metadata can be set, updated, and propagated correctly. The feature integrates with:

1. **Server API Layer (RoomServiceClient)** - Sets and retrieves metadata via gRPC
2. **Token Layer (AccessToken)** - Sets initial participant metadata at join time
3. **Browser Layer (LiveKitMeet + MetadataCapability)** - Receives metadata update events via SDK
4. **Webhook Layer (WebhookReceiver)** - Captures metadata in webhook payloads
5. **State Management Layer (MetadataStateManager)** - Tracks metadata values and events for verification

```
+------------------+     +-------------------+     +----------------------+
|   Feature File   | --> | Step Definitions  | --> | MetadataStateManager |
| (Gherkin/BDD)    |     | (Cucumber)        |     | (coordinates all)    |
+------------------+     +-------------------+     +----------------------+
                                                            |
                    +---------------------------------------+
                    |                   |                   |
                    v                   v                   v
          +------------------+  +------------------+  +------------------+
          | RoomClientState  |  | MeetSessionState |  | AccessTokenState |
          | Manager          |  | Manager          |  | Manager          |
          +------------------+  +------------------+  +------------------+
                    |                   |                   |
                    v                   v                   v
          +------------------+  +------------------+  +------------------+
          | RoomServiceClient|  | LiveKitMeet +    |  | AccessToken      |
          | (.execute()!)    |  | MetadataCapability| | (immutable)     |
          +------------------+  +------------------+  +------------------+
```

---

## Critical Architecture Requirements

### Requirement 1: Immutable Token Creation Pattern

**CRITICAL:** The existing `AccessTokenStateManager` uses an **immutable token creation pattern**. All token properties (including metadata) MUST be set during initial creation - NOT modified after.

**WRONG - Violates immutable pattern:**
```java
AccessToken token = accessTokenStateManager.createTokenWithGrants(identity, roomName, grants);
token.setMetadata(metadata);  // VIOLATION: modifying after creation
```

**CORRECT - Extend createTokenWithDynamicGrants:**
```java
public AccessToken createTokenWithDynamicGrants(
        String identity,
        String roomName,
        List<String> grantStrings,
        Map<String, String> customAttributes,
        String metadata,  // NEW parameter
        Long ttlMillis) {

    AccessToken token = new AccessToken(apiKey, apiSecret);
    token.setIdentity(identity);

    if (ttlMillis != null) {
        token.setTtl(ttlMillis);
    }

    if (metadata != null && !metadata.isEmpty()) {
        token.setMetadata(metadata);  // Set during creation, not after
    }

    if (customAttributes != null && !customAttributes.isEmpty()) {
        for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
            token.getAttributes().put(entry.getKey(), entry.getValue());
        }
    }

    List<VideoGrant> grants = new ArrayList<>();
    grants.add(new RoomName(roomName));
    grants.add(new RoomJoin(true));
    grants.addAll(parseGrants(grantStrings, roomName));

    if (!grants.isEmpty()) {
        token.addGrants(grants.toArray(new VideoGrant[0]));
    }

    tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(roomName, token);
    return token;
}
```

---

### Requirement 2: MetadataStateManager (New Component)

**CRITICAL:** Every feature domain in the codebase has a dedicated injectable state manager. Following the `DataChannelStateManager` pattern:

**Location:** `src/main/java/ro/stancalau/test/framework/state/MetadataStateManager.java`

```java
package ro.stancalau.test.framework.state;

import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.selenium.LiveKitMeet;
import ro.stancalau.test.framework.util.BrowserPollingHelper;

import java.util.List;
import java.util.Map;

@Slf4j
public class MetadataStateManager {

    private static final long WAIT_FOR_EVENT_MS = 2000;
    private static final long METADATA_PROPAGATION_TIMEOUT_MS = 10000;

    private final MeetSessionStateManager meetSessionStateManager;
    private final RoomClientStateManager roomClientStateManager;

    public MetadataStateManager(
            MeetSessionStateManager meetSessionStateManager,
            RoomClientStateManager roomClientStateManager) {
        this.meetSessionStateManager = meetSessionStateManager;
        this.roomClientStateManager = roomClientStateManager;
    }

    @SneakyThrows
    public void setRoomMetadata(String serviceName, String roomName, String metadata) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        client.updateRoomMetadata(roomName, metadata).execute();
        log.info("Set room {} metadata to: {}", roomName, metadata);
    }

    @SneakyThrows
    public String getRoomMetadata(String serviceName, String roomName) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        List<LivekitModels.Room> rooms = client.listRooms(List.of(roomName)).execute().body();
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalStateException("Room " + roomName + " not found");
        }
        return rooms.get(0).getMetadata();
    }

    @SneakyThrows
    public void updateParticipantMetadata(
            String serviceName, String roomName, String identity, String metadata) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        client.updateParticipant(roomName, identity, null, metadata, null).execute();
        log.info("Updated participant {} metadata to: {}", identity, metadata);
    }

    @SneakyThrows
    public String getParticipantMetadata(String serviceName, String roomName, String identity) {
        RoomServiceClient client = roomClientStateManager.getRoomServiceClient(serviceName);
        LivekitModels.ParticipantInfo participant =
            client.getParticipant(roomName, identity).execute().body();
        if (participant == null) {
            throw new IllegalStateException("Participant " + identity + " not found");
        }
        return participant.getMetadata();
    }

    public void startListeningForRoomMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.getMetadata().startListeningForRoomMetadataEvents();
    }

    public void startListeningForParticipantMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        meetInstance.getMetadata().startListeningForParticipantMetadataEvents();
    }

    public boolean waitForRoomMetadataEvent(String participantName, String expectedValue) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().waitForRoomMetadataEvent(
            expectedValue, (int)(METADATA_PROPAGATION_TIMEOUT_MS / 1000));
    }

    public boolean waitForParticipantMetadataEvent(
            String participantName, String targetIdentity, String expectedValue) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().waitForParticipantMetadataEvent(
            targetIdentity, expectedValue, (int)(METADATA_PROPAGATION_TIMEOUT_MS / 1000));
    }

    public String getCurrentRoomMetadataFromBrowser(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getCurrentRoomMetadata();
    }

    public String getParticipantMetadataFromBrowser(String participantName, String targetIdentity) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getParticipantMetadata(targetIdentity);
    }

    public List<Map<String, Object>> getRoomMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getRoomMetadataEvents();
    }

    public List<Map<String, Object>> getParticipantMetadataEvents(String participantName) {
        LiveKitMeet meetInstance = meetSessionStateManager.getMeetInstance(participantName);
        return meetInstance.getMetadata().getParticipantMetadataEvents();
    }

    public void clearAll() {
        log.info("Clearing metadata state");
        meetSessionStateManager.getAllMeetInstances().values().forEach(meet -> {
            try {
                meet.getMetadata().clearMetadataEvents();
            } catch (Exception e) {
                log.warn("Error clearing metadata state: {}", e.getMessage());
            }
        });
    }

    public String generateStringOfSize(int sizeBytes) {
        StringBuilder sb = new StringBuilder(sizeBytes);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < sizeBytes; i++) {
            sb.append(chars.charAt(i % chars.length()));
        }
        return sb.toString();
    }
}
```

---

### Requirement 3: RoomServiceClient .execute() Calls

**CRITICAL:** All RoomServiceClient operations return a `Call<T>` object that MUST have `.execute()` called to actually send the request.

**WRONG - Request never sent:**
```java
roomServiceClient.updateRoomMetadata(roomName, metadata);
roomServiceClient.updateParticipant(roomName, identity, null, metadata, null);
roomServiceClient.listRooms(List.of(roomName));
```

**CORRECT - Must call .execute():**
```java
roomServiceClient.updateRoomMetadata(roomName, metadata).execute();
roomServiceClient.updateParticipant(roomName, identity, null, metadata, null).execute();
List<Room> rooms = roomServiceClient.listRooms(List.of(roomName)).execute().body();
```

---

### Requirement 4: MetadataCapability Interface

**CRITICAL:** Browser capabilities are defined via interfaces following the established pattern (`DataChannelCapability`, `MediaControlCapability`, etc.).

**Location:** `src/main/java/ro/stancalau/test/framework/capabilities/MetadataCapability.java`

```java
package ro.stancalau.test.framework.capabilities;

import java.util.List;
import java.util.Map;

public interface MetadataCapability {

    void startListeningForRoomMetadataEvents();

    void startListeningForParticipantMetadataEvents();

    List<Map<String, Object>> getRoomMetadataEvents();

    List<Map<String, Object>> getParticipantMetadataEvents();

    String getCurrentRoomMetadata();

    String getParticipantMetadata(String identity);

    String getLocalParticipantMetadata();

    boolean waitForRoomMetadataEvent(String expectedValue, int timeoutSeconds);

    boolean waitForParticipantMetadataEvent(String identity, String expectedValue, int timeoutSeconds);

    int getRoomMetadataEventCount();

    int getParticipantMetadataEventCount();

    void clearMetadataEvents();
}
```

---

### Requirement 5: ManagerFactory and ManagerProvider Registration

**CRITICAL:** New state managers must be registered in `ManagerFactory` and exposed via `ManagerProvider`.

#### ManagerFactory.java Changes

```java
public static ManagerSet createManagerSet() {
    log.debug("Creating new manager set for test scenario");

    ContainerStateManager containerManager = new ContainerStateManager();
    WebDriverStateManager webDriverManager = new WebDriverStateManager(containerManager);
    RoomClientStateManager roomClientManager = new RoomClientStateManager(containerManager);
    AccessTokenStateManager accessTokenManager = new AccessTokenStateManager();
    EgressStateManager egressStateManager = new EgressStateManager();
    ImageSnapshotStateManager imageSnapshotStateManager = new ImageSnapshotStateManager();
    MeetSessionStateManager meetSessionStateManager = new MeetSessionStateManager(webDriverManager);
    VideoQualityStateManager videoQualityStateManager = new VideoQualityStateManager(meetSessionStateManager);
    DataChannelStateManager dataChannelStateManager = new DataChannelStateManager(meetSessionStateManager);

    // NEW: MetadataStateManager
    MetadataStateManager metadataStateManager = new MetadataStateManager(
        meetSessionStateManager,
        roomClientManager
    );

    return new ManagerSet(
        containerManager,
        webDriverManager,
        roomClientManager,
        accessTokenManager,
        egressStateManager,
        imageSnapshotStateManager,
        meetSessionStateManager,
        videoQualityStateManager,
        dataChannelStateManager,
        metadataStateManager  // NEW
    );
}

public record ManagerSet(
        ContainerStateManager containerManager,
        WebDriverStateManager webDriverManager,
        RoomClientStateManager roomClientManager,
        AccessTokenStateManager accessTokenManager,
        EgressStateManager egressStateManager,
        ImageSnapshotStateManager imageSnapshotStateManager,
        MeetSessionStateManager meetSessionStateManager,
        VideoQualityStateManager videoQualityStateManager,
        DataChannelStateManager dataChannelStateManager,
        MetadataStateManager metadataStateManager) {  // NEW

    public void cleanup() {
        // ... existing cleanup ...

        // NEW: cleanup metadata state
        try {
            metadataStateManager.clearAll();
        } catch (Exception e) {
            log.warn("Error cleaning up MetadataStateManager", e);
        }

        // ... rest of cleanup ...
    }
}
```

#### ManagerProvider.java Changes

```java
// Add getter method
public static MetadataStateManager getMetadataStateManager() {
    ManagerFactory.ManagerSet managers = managerSet.get();
    if (managers == null) {
        throw new IllegalStateException("Managers not initialized. Ensure BaseSteps @Before hook ran.");
    }
    return managers.metadataStateManager();
}

// Add short alias
public static MetadataStateManager metadata() {
    return getMetadataStateManager();
}
```

---

## Component Implementation

### 1. JavaScript Test Helpers Extensions

Add metadata event handling to the existing `LiveKitTestHelpers` object in `test-helpers.js`:

```javascript
window.roomMetadataEvents = [];
window.participantMetadataEvents = [];

var metadataHelpers = {
    startListeningForRoomMetadataEvents: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return false;
        window.roomMetadataEvents = [];
        return true;
    },

    startListeningForParticipantMetadataEvents: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return false;
        window.participantMetadataEvents = [];
        return true;
    },

    getRoomMetadataEvents: function() {
        return window.roomMetadataEvents || [];
    },

    getParticipantMetadataEvents: function() {
        return window.participantMetadataEvents || [];
    },

    getRoomMetadataEventCount: function() {
        return (window.roomMetadataEvents || []).length;
    },

    getParticipantMetadataEventCount: function() {
        return (window.participantMetadataEvents || []).length;
    },

    getCurrentRoomMetadata: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        return window.liveKitClient.room.metadata || null;
    },

    getParticipantMetadata: function(identity) {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        var participant = window.liveKitClient.room.participants.get(identity);
        return participant ? participant.metadata : null;
    },

    getLocalParticipantMetadata: function() {
        if (!window.liveKitClient || !window.liveKitClient.room) return null;
        return window.liveKitClient.room.localParticipant.metadata || null;
    },

    clearMetadataEvents: function() {
        window.roomMetadataEvents = [];
        window.participantMetadataEvents = [];
    }
};

Object.assign(window.LiveKitTestHelpers, metadataHelpers);
```

---

### 2. LiveKitMeetClient Event Listeners

Add to `setupRoomEventListeners()` in `livekit-client.js`:

```javascript
this.room.on(LiveKit.RoomEvent.RoomMetadataChanged, (metadata) => {
    window.roomMetadataEvents.push({
        metadata: metadata,
        timestamp: Date.now()
    });
    addTechnicalDetail('Room metadata changed: ' + (metadata ? metadata.substring(0, 50) : 'null'));
    console.log('RoomMetadataChanged event:', metadata);
});

this.room.on(LiveKit.RoomEvent.ParticipantMetadataChanged, (metadata, participant) => {
    window.participantMetadataEvents.push({
        participantIdentity: participant.identity,
        metadata: metadata,
        timestamp: Date.now()
    });
    addTechnicalDetail('Participant ' + participant.identity + ' metadata changed: ' +
        (metadata ? metadata.substring(0, 50) : 'null'));
    console.log('ParticipantMetadataChanged event:', participant.identity, metadata);
});
```

---

### 3. MetadataCapability Implementation in LiveKitMeet

**Location:** `src/main/java/ro/stancalau/test/framework/selenium/LiveKitMeetMetadataCapability.java`

```java
package ro.stancalau.test.framework.selenium;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import ro.stancalau.test.framework.capabilities.MetadataCapability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class LiveKitMeetMetadataCapability implements MetadataCapability {

    private final WebDriver driver;

    public LiveKitMeetMetadataCapability(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void startListeningForRoomMetadataEvents() {
        ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.startListeningForRoomMetadataEvents();"
        );
        log.info("Started listening for room metadata events");
    }

    @Override
    public void startListeningForParticipantMetadataEvents() {
        ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.startListeningForParticipantMetadataEvents();"
        );
        log.info("Started listening for participant metadata events");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoomMetadataEvents() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getRoomMetadataEvents();"
        );
        if (result instanceof List) {
            return (List<Map<String, Object>>) result;
        }
        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getParticipantMetadataEvents() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getParticipantMetadataEvents();"
        );
        if (result instanceof List) {
            return (List<Map<String, Object>>) result;
        }
        return new ArrayList<>();
    }

    @Override
    public String getCurrentRoomMetadata() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getCurrentRoomMetadata();"
        );
        return result != null ? result.toString() : null;
    }

    @Override
    public String getParticipantMetadata(String identity) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getParticipantMetadata(arguments[0]);",
            identity
        );
        return result != null ? result.toString() : null;
    }

    @Override
    public String getLocalParticipantMetadata() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getLocalParticipantMetadata();"
        );
        return result != null ? result.toString() : null;
    }

    @Override
    public boolean waitForRoomMetadataEvent(String expectedValue, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            wait.until(d -> {
                List<Map<String, Object>> events = getRoomMetadataEvents();
                return events.stream()
                    .anyMatch(e -> expectedValue.equals(e.get("metadata")));
            });
            return true;
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for room metadata event with value: {}", expectedValue);
            return false;
        }
    }

    @Override
    public boolean waitForParticipantMetadataEvent(String identity, String expectedValue, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            wait.until(d -> {
                List<Map<String, Object>> events = getParticipantMetadataEvents();
                return events.stream()
                    .anyMatch(e -> identity.equals(e.get("participantIdentity"))
                        && expectedValue.equals(e.get("metadata")));
            });
            return true;
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for participant {} metadata event with value: {}", identity, expectedValue);
            return false;
        }
    }

    @Override
    public int getRoomMetadataEventCount() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getRoomMetadataEventCount();"
        );
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Override
    public int getParticipantMetadataEventCount() {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "return window.LiveKitTestHelpers.getParticipantMetadataEventCount();"
        );
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Override
    public void clearMetadataEvents() {
        ((JavascriptExecutor) driver).executeScript(
            "window.LiveKitTestHelpers.clearMetadataEvents();"
        );
        log.info("Metadata events cleared");
    }
}
```

---

### 4. LiveKitMeet.java Integration

Add the capability to `LiveKitMeet.java`:

```java
private MetadataCapability metadataCapability;

public MetadataCapability getMetadata() {
    if (metadataCapability == null) {
        metadataCapability = new LiveKitMeetMetadataCapability(driver);
    }
    return metadataCapability;
}
```

---

## Step Definitions

Step definitions should use `ManagerProvider` to access state managers. They should be thin wrappers that delegate to the state manager.

### LiveKitMetadataSteps.java

```java
package ro.stancalau.test.bdd.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitMetadataSteps {

    @When("room metadata for {string} is set to {string} using service {string}")
    public void setRoomMetadata(String roomName, String metadata, String serviceName) {
        ManagerProvider.metadata().setRoomMetadata(serviceName, roomName, metadata);
    }

    @When("room metadata for {string} is set to a string of {int} bytes using service {string}")
    public void setRoomMetadataOfSize(String roomName, int sizeBytes, String serviceName) {
        String metadata = ManagerProvider.metadata().generateStringOfSize(sizeBytes);
        ManagerProvider.metadata().setRoomMetadata(serviceName, roomName, metadata);
    }

    @Then("room {string} should have metadata {string} in service {string}")
    public void verifyRoomMetadata(String roomName, String expectedMetadata, String serviceName) {
        String actual = ManagerProvider.metadata().getRoomMetadata(serviceName, roomName);
        assertEquals(expectedMetadata, actual, "Room metadata should match");
    }

    @When("participant {string} metadata is updated to {string} in room {string} using service {string}")
    public void updateParticipantMetadata(String identity, String metadata, String roomName, String serviceName) {
        ManagerProvider.metadata().updateParticipantMetadata(serviceName, roomName, identity, metadata);
    }

    @Then("participant {string} should have metadata {string} in room {string} using service {string}")
    public void verifyParticipantMetadata(String identity, String expectedMetadata, String roomName, String serviceName) {
        String actual = ManagerProvider.metadata().getParticipantMetadata(serviceName, roomName, identity);
        assertEquals(expectedMetadata, actual, "Participant metadata should match");
    }

    @When("{string} starts listening for room metadata events")
    public void startListeningForRoomMetadataEvents(String participantName) {
        ManagerProvider.metadata().startListeningForRoomMetadataEvents(participantName);
    }

    @When("{string} starts listening for participant metadata events")
    public void startListeningForParticipantMetadataEvents(String participantName) {
        ManagerProvider.metadata().startListeningForParticipantMetadataEvents(participantName);
    }

    @Then("{string} should receive a room metadata update event with value {string}")
    public void shouldReceiveRoomMetadataEvent(String participantName, String expectedValue) {
        boolean received = ManagerProvider.metadata().waitForRoomMetadataEvent(participantName, expectedValue);
        assertTrue(received, participantName + " should receive room metadata event with value: " + expectedValue);
    }

    @Then("{string} should receive a participant metadata update event for {string} with value {string}")
    public void shouldReceiveParticipantMetadataEvent(String watcher, String targetIdentity, String expectedValue) {
        boolean received = ManagerProvider.metadata().waitForParticipantMetadataEvent(
            watcher, targetIdentity, expectedValue);
        assertTrue(received, watcher + " should receive metadata event for " + targetIdentity);
    }

    @Then("{string} should see room metadata {string}")
    public void shouldSeeRoomMetadata(String participantName, String expectedMetadata) {
        String actual = ManagerProvider.metadata().getCurrentRoomMetadataFromBrowser(participantName);
        assertEquals(expectedMetadata, actual, participantName + " should see room metadata");
    }

    @Then("{string} should see participant {string} with metadata {string}")
    public void shouldSeeParticipantMetadata(String observer, String targetIdentity, String expectedMetadata) {
        String actual = ManagerProvider.metadata().getParticipantMetadataFromBrowser(observer, targetIdentity);
        assertEquals(expectedMetadata, actual, observer + " should see participant metadata");
    }

    @Then("{string} should have received {int} room metadata update events")
    public void shouldHaveReceivedRoomMetadataEventCount(String participantName, int expectedCount) {
        List<Map<String, Object>> events = ManagerProvider.metadata().getRoomMetadataEvents(participantName);
        assertEquals(expectedCount, events.size(), participantName + " should have received " + expectedCount + " events");
    }
}
```

---

## File Changes Summary

| File | Changes Required |
|------|------------------|
| `AccessTokenStateManager.java` | Add metadata parameter to createTokenWithDynamicGrants |
| `ManagerFactory.java` | Add MetadataStateManager creation and cleanup |
| `ManagerProvider.java` | Add getMetadataStateManager() and metadata() alias |
| **New:** `MetadataStateManager.java` | Central metadata state management |
| **New:** `MetadataCapability.java` | Interface for browser metadata operations |
| **New:** `LiveKitMeetMetadataCapability.java` | Implementation of MetadataCapability |
| `LiveKitMeet.java` | Add getMetadata() method |
| `test-helpers.js` | Add metadata event helper functions |
| `livekit-client.js` | Add RoomMetadataChanged, ParticipantMetadataChanged listeners |
| **New:** `LiveKitMetadataSteps.java` | Step definitions using ManagerProvider |
| **New:** `livekit_metadata.feature` | Feature file |

---

## Implementation Order

| Phase | Components | Focus |
|-------|------------|-------|
| 1 | MetadataCapability interface | Define browser capability contract |
| 2 | LiveKitMeetMetadataCapability | Implement browser capability |
| 3 | MetadataStateManager | Central state management |
| 4 | ManagerFactory + ManagerProvider | Wire up dependency injection |
| 5 | AccessTokenStateManager extension | Token metadata support (immutable) |
| 6 | JavaScript helpers | Event listening infrastructure |
| 7 | Step definitions | BDD integration |
| 8 | Feature file | Test scenarios |

---

## Architecture Principles Applied

| Principle | Application |
|-----------|-------------|
| **Single Responsibility (SRP)** | MetadataStateManager handles metadata; steps are thin wrappers |
| **Open/Closed (OCP)** | MetadataCapability interface allows new implementations |
| **Dependency Inversion (DIP)** | Steps depend on ManagerProvider abstractions |
| **Don't Repeat Yourself (DRY)** | Reuse existing patterns (capability, state manager, ManagerProvider) |
| **Injectable State Managers** | MetadataStateManager follows established injection pattern |
| **Immutable Tokens** | Token properties set at creation, not modified after |

---

## References

- [LiveKit Room Service API](https://docs.livekit.io/realtime/server/managing-rooms/)
- [LiveKit Participant Management](https://docs.livekit.io/realtime/server/managing-participants/)
- [LiveKit JS SDK - Room Events](https://docs.livekit.io/client-sdk-js/enums/RoomEvent.html)
- [LiveKit Server SDK Java](https://github.com/livekit/server-sdk-java)
