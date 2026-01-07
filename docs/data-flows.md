# LiveKit Testing Framework Data Flows

This document describes the key data flows and execution sequences within the LiveKit Testing Framework, providing visual representations of container startup, test execution, token generation, and browser automation flows.

## Table of Contents

- [Container Startup and Network Setup Flow](#container-startup-and-network-setup-flow)
- [Test Execution Flow](#test-execution-flow)
- [Token Generation and Validation Flow](#token-generation-and-validation-flow)
- [Browser Automation Flow for WebRTC Testing](#browser-automation-flow-for-webrtc-testing)
- [Egress Recording Flow](#egress-recording-flow)
- [Webhook Event Flow](#webhook-event-flow)
- [Image Snapshot Flow](#image-snapshot-flow)
- [Manager Lifecycle Flow](#manager-lifecycle-flow)

---

## Container Startup and Network Setup Flow

This flow describes how Docker containers are orchestrated during test infrastructure setup.

### Sequence Diagram

```
BDD Step              ContainerStateManager         Docker               Container
   |                         |                         |                     |
   |   Given LiveKit...      |                         |                     |
   |------------------------>|                         |                     |
   |                         |                         |                     |
   |                         |  getOrCreateNetwork()   |                     |
   |                         |------------------------>|                     |
   |                         |                         | Network.newNetwork()|
   |                         |                         |<--------------------|
   |                         |<------------------------|                     |
   |                         |                         |                     |
   |                         | LiveKitContainer.       |                     |
   |                         | createContainer(...)    |                     |
   |                         |------------------------------------------>|  |
   |                         |                         |                 |  |
   |                         |                         |  Pull Image     |  |
   |                         |                         |<----------------|  |
   |                         |                         |                 |  |
   |                         |                         |  Create Container  |
   |                         |                         |<----------------|  |
   |                         |                         |                 |  |
   |                         |                         | Attach Network  |  |
   |                         |                         |<----------------|  |
   |                         |                         |                 |  |
   |                         |                         | Set Aliases     |  |
   |                         |<--------------------------------------|     |
   |                         |                         |                     |
   |                         | container.start()       |                     |
   |                         |------------------------>|                     |
   |                         |                         | Wait for log msg    |
   |                         |                         |-------------------->|
   |                         |                         |<--------------------|
   |                         |<------------------------|                     |
   |                         |                         |                     |
   |                         | registerContainer()     |                     |
   |                         |------+                  |                     |
   |                         |      | (store in map)   |                     |
   |                         |<-----+                  |                     |
   |                         |                         |                     |
   |<------------------------|                         |                     |
   |   Container Ready       |                         |                     |
```

### Network Setup Detail

```
                    +---------------------------+
                    |   Docker Bridge Network   |
                    |     (Auto-generated)      |
                    +---------------------------+
                               |
       +-----------------------+-----------------------+
       |           |           |           |           |
       v           v           v           v           v
  +---------+ +---------+ +---------+ +---------+ +---------+
  |livekit1 | | egress1 | |  redis  | | minio1  | |mocksvr1 |
  | :7880   | | :7980   | | :6379   | | :9000   | | :1080   |
  +---------+ +---------+ +---------+ +---------+ +---------+
       ^           |           ^           ^
       |           |           |           |
       |     WebSocket         |           |
       +-----------+           |           |
                   |    Redis URL          |
                   +-------------------+   |
                               |           |
                          S3 Endpoint      |
                               +-----------+
```

### Container Dependency Order

```
1. Network Creation
        |
        v
2. Redis Container (standalone)
        |
        v
3. MinIO Container (standalone)
        |
        v
4. MockServer Container (standalone)
        |
        v
5. LiveKit Container
   - Uses config profile
   - Webhook URL -> MockServer
        |
        v
6. Egress Container
   - WebSocket -> LiveKit
   - Redis URL -> Redis
   - S3 Endpoint -> MinIO
        |
        v
7. WebServer Container (standalone)
        |
        v
8. Browser Containers (parallel)
   - Network access to LiveKit
   - Network access to WebServer
```

---

## Test Execution Flow

This flow compares unit test execution versus BDD test execution.

### Unit Test Flow

```
JUnit 5 Platform
      |
      v
+------------------+
| Test Discovery   |
| (ClassPathScanner)|
+------------------+
      |
      v
+------------------+
| @Test Methods    |
| in *Test classes |
+------------------+
      |
      v
+------------------+
| Direct Container |
| Creation         |
| (no managers)    |
+------------------+
      |
      v
+------------------+
| Test Execution   |
+------------------+
      |
      v
+------------------+
| @AfterEach       |
| Cleanup          |
+------------------+
```

### BDD Test Flow

```
JUnit 5 Platform
      |
      v
+------------------------+
| RunCucumberTests Suite |
| @IncludeEngines        |
| ("cucumber")           |
+------------------------+
      |
      v
+------------------------+
| Feature File Discovery |
| src/test/resources/    |
| features/*.feature     |
+------------------------+
      |
      v
+------------------------+
| BaseSteps @Before      |
| (order = 0)            |
|                        |
| ManagerProvider.       |
| initializeManagers()   |
+------------------------+
      |
      v
+------------------------+
| Step Definition        |
| Execution              |
|                        |
| Each step uses         |
| ManagerProvider to     |
| access managers        |
+------------------------+
      |
      v
+------------------------+
| Feature-specific       |
| @After hooks           |
| (order = 1-999)        |
+------------------------+
      |
      v
+------------------------+
| BaseSteps @After       |
| (order = 1000)         |
|                        |
| ManagerProvider.       |
| cleanupManagers()      |
+------------------------+
```

### Scenario Isolation Flow

```
Thread 1 (Scenario A)                Thread 2 (Scenario B)
         |                                    |
         v                                    v
+------------------+                +------------------+
| ThreadLocal<     |                | ThreadLocal<     |
|   ManagerSet>    |                |   ManagerSet>    |
+------------------+                +------------------+
         |                                    |
         v                                    v
+------------------+                +------------------+
| ManagerSet A     |                | ManagerSet B     |
| - ContainerMgr A |                | - ContainerMgr B |
| - WebDriverMgr A |                | - WebDriverMgr B |
| - TokenMgr A     |                | - TokenMgr B     |
| - ...            |                | - ...            |
+------------------+                +------------------+
         |                                    |
         v                                    v
+------------------+                +------------------+
| Docker Network A |                | Docker Network B |
| - livekit1       |                | - livekit1       |
| - redis          |                | - redis          |
| - ...            |                | - ...            |
+------------------+                +------------------+
```

---

## Token Generation and Validation Flow

This flow describes how access tokens are created and validated in BDD tests.

### Token Creation Flow

```
Gherkin Step: "When an access token is created with identity 'Bob'
               and room 'TestRoom' with grants 'canPublish:true,roomAdmin:true'
               and attributes 'role=moderator'"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitAccessTokenSteps.createTokenWithGrantsAndAttributes()  |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| StringParsingUtils.parseCommaSeparatedValues("canPublish:true,|
|                                               roomAdmin:true")|
| Returns: ["canPublish:true", "roomAdmin:true"]                |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| StringParsingUtils.parseKeyValuePairs("role=moderator")       |
| Returns: {"role": "moderator"}                                |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| AccessTokenStateManager.createTokenWithDynamicGrants(         |
|   identity: "Bob",                                            |
|   roomName: "TestRoom",                                       |
|   grants: ["canPublish:true", "roomAdmin:true"],              |
|   attributes: {"role": "moderator"},                          |
|   ttlMillis: null                                             |
| )                                                             |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| AccessToken token = new AccessToken(apiKey, apiSecret)        |
| token.setIdentity("Bob")                                      |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| parseGrants() - Convert strings to VideoGrant objects:        |
| "canPublish:true" -> new CanPublish(true)                     |
| "roomAdmin:true"  -> new RoomAdmin(true)                      |
| Auto-add: new RoomJoin(true), new RoomName("TestRoom")        |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| token.addGrants(grants...)                                    |
| token.getAttributes().put("role", "moderator")                |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| tokens.computeIfAbsent("Bob", k -> new HashMap<>())           |
|       .put("TestRoom", token)                                 |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Return token (AccessToken object)                             |
| JWT available via token.toJwt()                               |
+---------------------------------------------------------------+
```

### Token Validation Flow

```
Gherkin Step: "Then the access token for 'Bob' in room 'TestRoom'
               should have the following grants:
               | grant      | value |
               | canPublish | true  |
               | roomAdmin  | true  |"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitAccessTokenSteps.verifyTokenGrants(identity, room,     |
|                                           DataTable grants)   |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| AccessToken token = tokenManager.getLastToken("Bob",          |
|                                               "TestRoom")     |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Decode JWT: String jwt = token.toJwt()                        |
| DecodedJWT decoded = JWT.decode(jwt)                          |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Extract 'video' claim from JWT payload                        |
| Map<String, Object> videoClaims = decoded.getClaim("video")   |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| For each grant in DataTable:                                  |
|   Assert videoClaims.get("canPublish") == true                |
|   Assert videoClaims.get("roomAdmin") == true                 |
+---------------------------------------------------------------+
```

---

## Browser Automation Flow for WebRTC Testing

This flow describes how browser sessions are managed for WebRTC testing.

### Browser Session Creation Flow

```
Gherkin Step: "When 'Alice' opens a 'Chrome' browser with LiveKit Meet page"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitBrowserWebrtcSteps.openBrowserWithLiveKitMeet(         |
|   actor: "Alice", browser: "Chrome")                          |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| webDriverManager.setScenarioRecordingPath(scenarioPath)       |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| webDriverManager.createWebDriver("meet", "Alice", "chrome")   |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| createBrowserContainer("meet:Alice", "chrome")                |
|                                                               |
| 1. Get network from ContainerStateManager                     |
| 2. Configure Chrome capabilities:                             |
|    - Fake media streams                                       |
|    - Insecure origin allowance                               |
|    - WebRTC settings                                         |
| 3. Configure VNC recording mode                               |
| 4. Start container                                            |
| 5. Set network alias                                          |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| BrowserWebDriverContainer.getWebDriver()                      |
| -> RemoteWebDriver connected to containerized browser         |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| If recording enabled:                                         |
|   container.beforeTest(testDescription)                       |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Store in maps:                                                |
| webDrivers.put("meet:Alice", driver)                          |
| browserContainers.put("meet:Alice", container)                |
| testDescriptions.put("meet:Alice", testDescription)           |
| testResults.put("meet:Alice", true)                           |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| WebServer URL construction:                                   |
| http://webserver/livekit-meet.html                           |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| driver.get(webServerUrl)                                      |
+---------------------------------------------------------------+
```

### WebRTC Connection Flow

```
Gherkin Step: "When 'Alice' connects to room 'TestRoom' using the access token"
                              |
                              v
+---------------------------------------------------------------+
| Get stored token: token = tokenManager.getLastToken("Alice",  |
|                                                    "TestRoom")|
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Get LiveKit WebSocket URL from container:                     |
| wsUrl = liveKitContainer.getNetworkUrl()                      |
| -> "ws://livekit1:7880"                                       |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Get WebDriver: driver = webDriverManager.getWebDriver("meet", |
|                                                       "Alice")|
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Fill LiveKit Meet form via JavaScript:                        |
| - Set URL field: wsUrl                                        |
| - Set Token field: token.toJwt()                              |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Click Connect button:                                         |
| driver.findElement(By.id("connect-button")).click()           |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Wait for connection (implicit wait):                          |
| Verify participant video element appears                      |
+---------------------------------------------------------------+

                         Browser                LiveKit Server
                            |                        |
                            | WebSocket Connect      |
                            |----------------------->|
                            |                        |
                            | Join Room (JWT)        |
                            |----------------------->|
                            |                        |
                            | Participant Joined     |
                            |<-----------------------|
                            |                        |
                            | ICE Negotiation        |
                            |<======================>|
                            |                        |
                            | Media Tracks (WebRTC)  |
                            |----------------------->|
                            |                        |
                            | Remote Tracks          |
                            |<-----------------------|
```

### Browser Session Cleanup Flow

```
BaseSteps @After (order = 1000)
           |
           v
+---------------------------------------------------------------+
| ManagerProvider.cleanupManagers()                             |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerSet.cleanup()                                          |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| webDriverManager.closeAllWebDrivers()                         |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| For each WebDriver entry:                                     |
|   1. Get test result (passed/failed)                          |
|   2. driver.quit()                                            |
|   3. If recording enabled:                                    |
|      - container.afterTest(desc, testFailure)                 |
|      - waitForRecordingFile(...)                              |
|   4. container.stop()                                         |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| Clear all maps:                                               |
| webDrivers.clear()                                            |
| browserContainers.clear()                                     |
| testDescriptions.clear()                                      |
| testResults.clear()                                           |
+---------------------------------------------------------------+
```

---

## Egress Recording Flow

This flow describes the egress recording lifecycle for video capture.

### Room Composite Recording Flow

```
Gherkin Step: "When room composite recording is started for room 'TestRoom'
               using LiveKit service 'livekit1'"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitEgressSteps.startRoomCompositeRecording(room, service) |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Get RoomServiceClient for 'livekit1'                          |
| Get EgressClient from container connection                    |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Build RoomCompositeEgressRequest:                             |
| - roomName: "TestRoom"                                        |
| - layout: "grid"                                              |
| - fileOutput: {path, filename}                                |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| egressClient.startRoomCompositeEgress(request)                |
+---------------------------------------------------------------+

                    LiveKit Server          Egress Service
                         |                       |
                         | gRPC: Start Egress    |
                         |---------------------->|
                         |                       |
                         |                       | Start Chrome
                         |                       | Load Room Template
                         |                       | Begin Recording
                         |                       |
                         | Webhook: egress_started
                         |---------------------->| MockServer
                         |                       |
                         |                       | Capture WebRTC
                         |                       | Encode to MP4
                         |                       |
                         | (Recording Active)    |
                         |                       |

Gherkin Step: "When room composite recording is stopped for room 'TestRoom'"
                              |
                              v
+---------------------------------------------------------------+
| Get active egressId from EgressStateManager                   |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| egressClient.stopEgress(egressId)                             |
+---------------------------------------------------------------+

                    LiveKit Server          Egress Service
                         |                       |
                         | gRPC: Stop Egress     |
                         |---------------------->|
                         |                       |
                         |                       | Finalize MP4
                         |                       | Write to Output
                         |                       |
                         | Webhook: egress_ended |
                         |---------------------->| MockServer
                         |                       |
                         |<----------------------|
                         | Recording Complete    |
```

### Track Composite Recording Flow

```
Step 1: Capture Track IDs
+---------------------------------------------------------------+
| Get participant info from RoomServiceClient                   |
| participant = roomClient.getParticipant(room, identity)       |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| Extract track IDs:                                            |
| videoTrackId = participant.tracks.find(t -> t.type == VIDEO)  |
| audioTrackId = participant.tracks.find(t -> t.type == AUDIO)  |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| Store in EgressStateManager:                                  |
| egressManager.storeTrackIds("Thomas", {                       |
|   "video": "TR_xxx",                                          |
|   "audio": "TR_yyy"                                           |
| })                                                            |
+---------------------------------------------------------------+

Step 2: Start Track Recording
+---------------------------------------------------------------+
| Get stored track IDs                                          |
| trackIds = egressManager.getTrackIds("Thomas")                |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| Build TrackCompositeEgressRequest:                            |
| - roomName: "TrackCompositeRoom"                              |
| - videoTrackId: trackIds.get("video")                         |
| - audioTrackId: trackIds.get("audio")                         |
| - fileOutput: {path, filename}                                |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| egressClient.startTrackCompositeEgress(request)               |
| Store egressId in EgressStateManager                          |
+---------------------------------------------------------------+
```

---

## Webhook Event Flow

This flow describes how webhook events are captured and validated.

### Webhook Capture Flow

```
LiveKit Server                MockServer                  Test Code
      |                           |                           |
      | Room Created              |                           |
      |                           |                           |
      | POST /webhook             |                           |
      | {event: "room_started",   |                           |
      |  room: {name: "Test"}}    |                           |
      |-------------------------->|                           |
      |                           | Store Request             |
      |                           |------+                    |
      |                           |      |                    |
      |                           |<-----+                    |
      |                           |                           |
      | 200 OK                    |                           |
      |<--------------------------|                           |
      |                           |                           |
      |                           |   Retrieve Events         |
      |                           |<--------------------------|
      |                           |                           |
      |                           | mockServerClient.         |
      |                           | retrieveRecordedRequests()|
      |                           |------+                    |
      |                           |      |                    |
      |                           |<-----+                    |
      |                           |                           |
      |                           | HttpRequest[]             |
      |                           |-------------------------->|
      |                           |                           |
      |                           |   Parse to WebhookEvent   |
      |                           |                           |
```

### Webhook Validation Flow

```
Gherkin Step: "Then 'mockserver' should have received a 'room_started'
               event for room 'TestRoom'"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitWebhookSteps.verifyWebhookReceived(mockService,        |
|                                           eventType, room)    |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Get MockServerClient from ContainerStateManager               |
| mockClient = container.getClient()                            |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| WebhookService.getWebhookEvents(mockClient)                   |
|                                                               |
| 1. Retrieve all recorded requests                             |
| 2. Filter by path: /webhook                                   |
| 3. Filter by method: POST                                     |
| 4. Parse JSON body to WebhookEvent                            |
| 5. Filter out initTest room events                            |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| webhookService.findEventByType(events, "room_started")        |
| -> Optional<WebhookEvent>                                     |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Assert event.isPresent()                                      |
| Assert event.get().getRoom().getName() == "TestRoom"          |
+---------------------------------------------------------------+
```

---

## Image Snapshot Flow

This flow describes on-demand image snapshot capture.

### Room Snapshot to S3 Flow

```
Gherkin Step: "When an on-demand snapshot is captured to S3 for room
               'SnapshotRoom' using LiveKit service 'livekit1' and
               MinIO service 'minio1'"
                              |
                              v
+---------------------------------------------------------------+
| LiveKitImageSnapshotSteps.captureRoomSnapshotToS3(...)        |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Get MinIO S3 endpoint URL from container:                     |
| s3Endpoint = minioContainer.getNetworkS3EndpointUrl()         |
| -> "http://minio1:9000"                                       |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Build S3Upload configuration:                                 |
| - endpoint: s3Endpoint                                        |
| - accessKey, secretKey                                        |
| - bucket: "snapshots"                                         |
| - filepath: "{room}-snapshot.jpeg"                            |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Build RoomCompositeEgressRequest with S3Upload                |
| egressClient.startRoomCompositeEgress(request)                |
+---------------------------------------------------------------+

                    Egress Service          MinIO
                         |                    |
                         | Capture Frame      |
                         | Encode JPEG        |
                         |                    |
                         | PUT /snapshots/... |
                         |------------------->|
                         |                    | Store Object
                         |                    |
                         | 200 OK             |
                         |<-------------------|
                         |                    |
+---------------------------------------------------------------+
| Store snapshot info in ImageSnapshotStateManager              |
| snapshotManager.storeS3Snapshot(room, bucket, key)            |
+---------------------------------------------------------------+
```

### Snapshot Validation Flow

```
Gherkin Step: "Then the S3 snapshot image is valid and contains
               actual image data"
                              |
                              v
+---------------------------------------------------------------+
| Get stored snapshot info from ImageSnapshotStateManager       |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Create MinioS3Client:                                         |
| s3Client = new MinioS3Client(endpoint, accessKey, secretKey)  |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| Download object:                                              |
| byte[] imageData = s3Client.getObject(bucket, key)            |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
| ImageValidationUtils.validateImage(imageData):                |
| 1. Check byte array not empty                                 |
| 2. Check JPEG magic bytes (0xFF 0xD8)                        |
| 3. Verify image dimensions > 0                                |
| 4. Check file size is reasonable                              |
+---------------------------------------------------------------+
```

---

## Manager Lifecycle Flow

This flow describes the complete manager lifecycle for a BDD scenario.

### Scenario Initialization

```
JUnit/Cucumber Platform
           |
           v
+---------------------------------------------------------------+
| Scenario Start                                                |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| BaseSteps @Before (order = 0)                                 |
| setUpManagers(Scenario scenario)                              |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerProvider.initializeManagers()                          |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| if (ThreadLocal.get() == null)                                |
|   ManagerSet managers = ManagerFactory.createManagerSet()     |
|   ThreadLocal.set(managers)                                   |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerFactory.createManagerSet():                            |
|                                                               |
| ContainerStateManager containerManager = new ...()            |
|      |                                                        |
|      +---> WebDriverStateManager(containerManager)            |
|      +---> RoomClientStateManager(containerManager)           |
|                                                               |
| AccessTokenStateManager = new ...()                           |
| EgressStateManager = new ...()                                |
| ImageSnapshotStateManager = new ...()                         |
|                                                               |
| return new ManagerSet(all managers)                           |
+---------------------------------------------------------------+
```

### Scenario Execution

```
Step Definition                ManagerProvider               Manager
      |                              |                          |
      | Given "livekit1"...          |                          |
      |                              |                          |
      | ManagerProvider.containers() |                          |
      |----------------------------->|                          |
      |                              | ThreadLocal.get()        |
      |                              |--------+                 |
      |                              |        |                 |
      |                              |<-------+                 |
      |                              |                          |
      |                              | managerSet.              |
      |                              | containerManager()       |
      |                              |--------+                 |
      |                              |        |                 |
      |<-----------------------------|<-------+                 |
      |                              |                          |
      | containerManager.            |                          |
      | getOrCreateNetwork()         |                          |
      |-------------------------------------------------->|    |
      |                              |                    |    |
      |                              |                    | Create Network
      |<--------------------------------------------------|    |
      |                              |                          |
      | containerManager.            |                          |
      | registerContainer(...)       |                          |
      |-------------------------------------------------->|    |
      |                              |                          |
```

### Scenario Cleanup

```
Step Definitions Complete
           |
           v
+---------------------------------------------------------------+
| BaseSteps @After (order = 1000)                               |
| cleanupManagers(Scenario scenario)                            |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerProvider.cleanupManagers()                             |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerSet managers = ThreadLocal.get()                       |
| if (managers != null)                                         |
|   managers.cleanup()                                          |
|   ThreadLocal.remove()                                        |
+---------------------------------------------------------------+
           |
           v
+---------------------------------------------------------------+
| ManagerSet.cleanup() - Reverse dependency order:              |
|                                                               |
| 1. webDriverManager.closeAllWebDrivers()                      |
|    - Quit all browsers                                        |
|    - Stop VNC recordings                                      |
|    - Stop browser containers                                  |
|                                                               |
| 2. roomClientManager.clearAll()                               |
|    - Clear RoomServiceClient cache                            |
|                                                               |
| 3. accessTokenManager.clearAll()                              |
|    - Clear stored tokens                                      |
|                                                               |
| 4. egressStateManager.clearAll()                              |
|    - Clear track IDs and active recordings                    |
|                                                               |
| 5. imageSnapshotStateManager.clearAll()                       |
|    - Clear snapshot references                                |
|                                                               |
| 6. containerManager.stopAllContainers()                       |
|    - Stop all registered containers                           |
|                                                               |
| 7. containerManager.closeNetwork()                            |
|    - Close Docker network                                     |
+---------------------------------------------------------------+
```

---

## Summary

These data flow diagrams illustrate the key execution paths through the LiveKit Testing Framework:

1. **Container Orchestration**: Dependencies are started in order with proper network configuration
2. **Test Execution**: BDD tests use ThreadLocal managers for isolation; unit tests use direct container access
3. **Token Management**: Flexible grant and attribute parsing with JWT generation
4. **Browser Automation**: Containerized browsers with fake media and VNC recording
5. **Egress Recording**: Room and track composite recording with S3 storage support
6. **Webhook Testing**: MockServer capture with event parsing and validation
7. **Manager Lifecycle**: Clean initialization and teardown per scenario
