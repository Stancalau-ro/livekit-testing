# Dynacast Bandwidth Adaptation Testing - Requirements Document

## Epic Description

**Epic:** Test Dynacast Bandwidth Adaptation
**Story ID:** 1.1.3
**Parent Epic:** 1.1 - Expanded LiveKit Feature Coverage

**As a** test developer
**I want** to test dynacast functionality
**So that** I can verify automatic quality adaptation

---

## Story Breakdown

The original story is broken down into smaller, independent stories following INVEST criteria.

### Story 1.1.3.1: Enable Dynacast Configuration in Room Settings

**Size:** S (Small)

**As a** test developer
**I want** to enable or disable dynacast when creating a room connection
**So that** I can test both dynacast-enabled and dynacast-disabled scenarios

**Acceptance Criteria:**

**Given** a participant joins a room with dynacast enabled
**When** the room connection is inspected
**Then** the room should have dynacast active

**Given** a participant joins a room with dynacast disabled
**When** the room connection is inspected
**Then** the room should have dynacast inactive

**Given** dynacast is enabled in the browser client
**When** video is published with simulcast
**Then** both dynacast and simulcast should be active together

- [ ] Add browser-side dynacast enable/disable capability
- [ ] Verify dynacast flag is accessible via LiveKitMeet page object
- [ ] Test default dynacast behavior (enabled by default)
- [ ] Verify dynacast setting persists through connection

**Dependencies:** None (builds on existing room connection infrastructure)

---

### Story 1.1.3.2: Verify Dynacast Activation via Server API

**Size:** S (Small)

**As a** test developer
**I want** to verify dynacast is active via the server API
**So that** I can confirm dynacast configuration is properly applied

**Acceptance Criteria:**

**Given** dynacast is enabled for a room connection
**When** the room state is inspected via server API
**Then** evidence of dynacast activity should be detectable

**Given** a video track is published with dynacast and simulcast
**When** multiple quality layers are inspected
**Then** layer information should indicate dynacast-managed layers

**Given** dynacast is disabled for a room connection
**When** the room state is inspected
**Then** dynacast indicators should be absent or inactive

- [ ] Research available dynacast indicators in server API
- [ ] Implement step definitions for dynacast state verification
- [ ] Document observable dynacast behavior in TrackInfo
- [ ] Handle cases where dynacast state is not directly queryable

**Dependencies:** Story 1.1.3.1

---

### Story 1.1.3.3: Test Video Quality Adaptation Under Bandwidth Constraints

**Size:** M (Medium)

**As a** test developer
**I want** to verify video quality adapts when subscriber requests lower quality
**So that** I can confirm dynacast responds to subscriber preferences

**Acceptance Criteria:**

**Given** dynacast is enabled and subscriber is receiving high quality
**When** subscriber sets video quality preference to LOW
**Then** the received video quality should decrease

**Given** subscriber has set low quality preference
**When** the server processes the preference
**Then** unnecessary high-quality layers should be paused

**Given** subscriber changes quality preference
**When** the change is processed
**Then** video dimensions or bitrate should reflect the change

- [ ] Implement quality preference change detection
- [ ] Measure video dimension changes after preference change
- [ ] Verify quality change occurs within acceptable time
- [ ] Test with multiple quality levels (LOW, MEDIUM, HIGH)

**Dependencies:** Story 1.1.3.1, Story 1.1.2 (Simulcast)

---

### Story 1.1.3.4: Measure Dynacast Response Time

**Size:** M (Medium)

**As a** test developer
**I want** to measure the response time for dynacast quality adaptation
**So that** I can verify adaptation occurs within acceptable bounds

**Acceptance Criteria:**

**Given** a subscriber changes quality preference
**When** the timestamp of change is recorded
**Then** the time until quality change is observed should be measurable

**Given** multiple quality changes are requested
**When** response times are calculated
**Then** average response time should be less than configured threshold

**Given** response time is measured
**When** logged for analysis
**Then** min, max, and average response times should be reported

- [ ] Add timestamp recording for quality preference changes
- [ ] Implement quality change detection with timing
- [ ] Calculate and log response time statistics
- [ ] Define acceptable response time threshold (suggest 2000ms for containers)
- [ ] Add step definitions for response time verification

**Dependencies:** Story 1.1.3.3

---

### Story 1.1.3.5: Test Quality Recovery When Bandwidth Increases

**Size:** M (Medium)

**As a** test developer
**I want** to verify quality increases when subscriber requests higher quality
**So that** I can confirm dynacast supports quality recovery

**Acceptance Criteria:**

**Given** subscriber is receiving low quality video
**When** subscriber sets video quality preference to HIGH
**Then** the received video quality should increase

**Given** quality increase is requested
**When** sufficient layers are available
**Then** highest requested quality should be delivered

**Given** quality was previously reduced
**When** subscriber requests high quality again
**Then** quality should return to original level

- [ ] Implement quality increase scenario (LOW to HIGH)
- [ ] Verify video dimensions increase after preference change
- [ ] Test recovery timing is within acceptable bounds
- [ ] Verify quality recovery works after multiple quality decreases

**Dependencies:** Story 1.1.3.3

---

### Story 1.1.3.6: Verify Dynacast Behavior with Multiple Subscribers

**Size:** L (Large)

**As a** test developer
**I want** to test dynacast with multiple subscribers having different quality preferences
**So that** I can verify each subscriber receives appropriate quality

**Acceptance Criteria:**

**Given** one publisher and multiple subscribers are in a room
**When** subscribers set different quality preferences
**Then** each subscriber should receive their requested quality level

**Given** one subscriber requests LOW and another requests HIGH quality
**When** both are actively subscribing
**Then** dynacast should deliver appropriate layers to each

**Given** all subscribers request LOW quality
**When** no subscriber needs high quality
**Then** dynacast should pause high quality layer transmission

**Given** one subscriber changes to HIGH quality
**When** previously paused layers are needed
**Then** high quality layer should resume

- [ ] Create multi-subscriber test scenario (3+ participants)
- [ ] Implement independent quality preference per subscriber
- [ ] Verify each subscriber receives appropriate quality
- [ ] Test layer pause/resume with varying subscriber preferences
- [ ] Document multi-subscriber dynacast behavior

**Dependencies:** Story 1.1.3.3, Story 1.1.3.5

---

## Gherkin Scenarios

### Feature File: `livekit_dynacast.feature`

```gherkin
Feature: LiveKit Dynacast Bandwidth Adaptation
  As a test developer
  I want to test dynacast functionality
  So that I can verify automatic quality adaptation

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  # Story 1.1.3.1: Enable Dynacast Configuration in Room Settings
  Scenario Outline: Participant joins room with dynacast enabled
    Given an access token is created with identity "DynaPublisher" and room "DynaRoom" with grants "canPublish:true,canSubscribe:true"
    And room "DynaRoom" is created using service "livekit1"

    When "DynaPublisher" opens a <browser> browser with LiveKit Meet page
    And "DynaPublisher" enables dynacast for the room connection
    And "DynaPublisher" enables simulcast for video publishing
    And "DynaPublisher" connects to room "DynaRoom" using the access token
    And connection is established successfully for "DynaPublisher"

    Then participant "DynaPublisher" should have dynacast active in room "DynaRoom" using service "livekit1"
    And "DynaPublisher" closes the browser

    Examples:
      | browser  |
      | "Chrome" |
      | "Edge"   |

  Scenario: Participant joins room with dynacast disabled
    Given an access token is created with identity "NoDynaPublisher" and room "NoDynaRoom" with grants "canPublish:true,canSubscribe:true"
    And room "NoDynaRoom" is created using service "livekit1"

    When "NoDynaPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "NoDynaPublisher" disables dynacast for the room connection
    And "NoDynaPublisher" connects to room "NoDynaRoom" using the access token
    And connection is established successfully for "NoDynaPublisher"

    Then participant "NoDynaPublisher" should have dynacast inactive in room "NoDynaRoom" using service "livekit1"
    And "NoDynaPublisher" closes the browser

  Scenario: Dynacast and simulcast work together
    Given an access token is created with identity "ComboPublisher" and room "ComboRoom" with grants "canPublish:true,canSubscribe:true"
    And room "ComboRoom" is created using service "livekit1"

    When "ComboPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "ComboPublisher" enables dynacast for the room connection
    And "ComboPublisher" enables simulcast for video publishing
    And "ComboPublisher" connects to room "ComboRoom" using the access token
    And connection is established successfully for "ComboPublisher"

    Then participant "ComboPublisher" should have dynacast active in room "ComboRoom" using service "livekit1"
    And participant "ComboPublisher" should have simulcast enabled for video in room "ComboRoom" using service "livekit1"
    And participant "ComboPublisher" video track should have at least 2 layers in room "ComboRoom" using service "livekit1"
    And "ComboPublisher" closes the browser

  # Story 1.1.3.2: Verify Dynacast Activation via Server API
  Scenario: Verify dynacast state is detectable via server API
    Given an access token is created with identity "APITestPublisher" and room "APITestRoom" with grants "canPublish:true,canSubscribe:true"
    And room "APITestRoom" is created using service "livekit1"

    When "APITestPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "APITestPublisher" enables dynacast for the room connection
    And "APITestPublisher" enables simulcast for video publishing
    And "APITestPublisher" connects to room "APITestRoom" using the access token
    And connection is established successfully for "APITestPublisher"

    Then participant "APITestPublisher" should be publishing video in room "APITestRoom" using service "livekit1"
    And participant "APITestPublisher" video track should have dynacast-managed layers in room "APITestRoom" using service "livekit1"
    And "APITestPublisher" closes the browser

  # Story 1.1.3.3: Test Video Quality Adaptation Under Bandwidth Constraints
  Scenario: Subscriber quality preference reduces received quality
    Given an access token is created with identity "HighResPublisher" and room "AdaptRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "LowBandwidthSub" and room "AdaptRoom" with grants "canPublish:false,canSubscribe:true"
    And room "AdaptRoom" is created using service "livekit1"

    When "HighResPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "HighResPublisher" enables dynacast for the room connection
    And "HighResPublisher" enables simulcast for video publishing
    And "HighResPublisher" connects to room "AdaptRoom" using the access token
    And connection is established successfully for "HighResPublisher"

    Then participant "HighResPublisher" video track should have at least 2 layers in room "AdaptRoom" using service "livekit1"

    When "LowBandwidthSub" opens a "Chrome" browser with LiveKit Meet page
    And "LowBandwidthSub" connects to room "AdaptRoom" using the access token
    And connection is established successfully for "LowBandwidthSub"
    And "LowBandwidthSub" waits for remote video from "HighResPublisher"

    Then "LowBandwidthSub" should be receiving video from "HighResPublisher" in room "AdaptRoom" using service "livekit1"

    When "LowBandwidthSub" records current received video dimensions
    And "LowBandwidthSub" sets video quality preference to "LOW"
    And "LowBandwidthSub" waits 2 seconds for quality adaptation

    Then "LowBandwidthSub" should be receiving lower quality video than initially recorded
    And "HighResPublisher" closes the browser
    And "LowBandwidthSub" closes the browser

  Scenario: Quality adapts through multiple levels
    Given an access token is created with identity "MultiQualityPub" and room "MultiQualityRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "AdaptiveSub" and room "MultiQualityRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiQualityRoom" is created using service "livekit1"

    When "MultiQualityPub" opens a "Chrome" browser with LiveKit Meet page
    And "MultiQualityPub" enables dynacast for the room connection
    And "MultiQualityPub" enables simulcast for video publishing
    And "MultiQualityPub" connects to room "MultiQualityRoom" using the access token
    And connection is established successfully for "MultiQualityPub"

    When "AdaptiveSub" opens a "Chrome" browser with LiveKit Meet page
    And "AdaptiveSub" connects to room "MultiQualityRoom" using the access token
    And connection is established successfully for "AdaptiveSub"
    And "AdaptiveSub" waits for remote video from "MultiQualityPub"

    When "AdaptiveSub" sets video quality preference to "HIGH"
    And "AdaptiveSub" waits 2 seconds for quality adaptation
    And "AdaptiveSub" records received video quality as "high_quality"

    When "AdaptiveSub" sets video quality preference to "MEDIUM"
    And "AdaptiveSub" waits 2 seconds for quality adaptation
    And "AdaptiveSub" records received video quality as "medium_quality"

    When "AdaptiveSub" sets video quality preference to "LOW"
    And "AdaptiveSub" waits 2 seconds for quality adaptation
    And "AdaptiveSub" records received video quality as "low_quality"

    Then recorded quality "high_quality" should be greater than "medium_quality"
    And recorded quality "medium_quality" should be greater than "low_quality"
    And "MultiQualityPub" closes the browser
    And "AdaptiveSub" closes the browser

  Scenario: Dynacast responds to quality preference OFF
    Given an access token is created with identity "OffTestPublisher" and room "OffTestRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "OffTestSubscriber" and room "OffTestRoom" with grants "canPublish:false,canSubscribe:true"
    And room "OffTestRoom" is created using service "livekit1"

    When "OffTestPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "OffTestPublisher" enables dynacast for the room connection
    And "OffTestPublisher" enables simulcast for video publishing
    And "OffTestPublisher" connects to room "OffTestRoom" using the access token
    And connection is established successfully for "OffTestPublisher"

    When "OffTestSubscriber" opens a "Chrome" browser with LiveKit Meet page
    And "OffTestSubscriber" connects to room "OffTestRoom" using the access token
    And connection is established successfully for "OffTestSubscriber"
    And "OffTestSubscriber" waits for remote video from "OffTestPublisher"

    When "OffTestSubscriber" sets video quality preference to "OFF"
    And "OffTestSubscriber" waits 2 seconds for quality adaptation

    Then "OffTestSubscriber" should not be receiving video from "OffTestPublisher"
    And "OffTestPublisher" closes the browser
    And "OffTestSubscriber" closes the browser

  # Story 1.1.3.4: Measure Dynacast Response Time
  Scenario: Quality adaptation response time is within acceptable bounds
    Given an access token is created with identity "ResponseTimePub" and room "ResponseTimeRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "ResponseTimeSub" and room "ResponseTimeRoom" with grants "canPublish:false,canSubscribe:true"
    And room "ResponseTimeRoom" is created using service "livekit1"

    When "ResponseTimePub" opens a "Chrome" browser with LiveKit Meet page
    And "ResponseTimePub" enables dynacast for the room connection
    And "ResponseTimePub" enables simulcast for video publishing
    And "ResponseTimePub" connects to room "ResponseTimeRoom" using the access token
    And connection is established successfully for "ResponseTimePub"

    When "ResponseTimeSub" opens a "Chrome" browser with LiveKit Meet page
    And "ResponseTimeSub" connects to room "ResponseTimeRoom" using the access token
    And connection is established successfully for "ResponseTimeSub"
    And "ResponseTimeSub" waits for remote video from "ResponseTimePub"
    And "ResponseTimeSub" sets video quality preference to "HIGH"
    And "ResponseTimeSub" waits 2 seconds for quality stabilization

    When "ResponseTimeSub" starts response time measurement
    And "ResponseTimeSub" sets video quality preference to "LOW"
    And "ResponseTimeSub" waits for quality change detection

    Then the measured dynacast response time should be less than 2000 ms
    And "ResponseTimePub" closes the browser
    And "ResponseTimeSub" closes the browser

  Scenario: Multiple quality changes show consistent response time
    Given an access token is created with identity "ConsistencyPub" and room "ConsistencyRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "ConsistencySub" and room "ConsistencyRoom" with grants "canPublish:false,canSubscribe:true"
    And room "ConsistencyRoom" is created using service "livekit1"

    When "ConsistencyPub" opens a "Chrome" browser with LiveKit Meet page
    And "ConsistencyPub" enables dynacast for the room connection
    And "ConsistencyPub" enables simulcast for video publishing
    And "ConsistencyPub" connects to room "ConsistencyRoom" using the access token
    And connection is established successfully for "ConsistencyPub"

    When "ConsistencySub" opens a "Chrome" browser with LiveKit Meet page
    And "ConsistencySub" connects to room "ConsistencyRoom" using the access token
    And connection is established successfully for "ConsistencySub"
    And "ConsistencySub" waits for remote video from "ConsistencyPub"

    When "ConsistencySub" measures response time for 5 quality changes between LOW and HIGH

    Then the average dynacast response time should be less than 2000 ms
    And the maximum dynacast response time should be less than 3000 ms
    And "ConsistencyPub" closes the browser
    And "ConsistencySub" closes the browser

  # Story 1.1.3.5: Test Quality Recovery When Bandwidth Increases
  Scenario: Quality increases when subscriber requests higher quality
    Given an access token is created with identity "RecoveryPublisher" and room "RecoveryRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "RecoverySubscriber" and room "RecoveryRoom" with grants "canPublish:false,canSubscribe:true"
    And room "RecoveryRoom" is created using service "livekit1"

    When "RecoveryPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "RecoveryPublisher" enables dynacast for the room connection
    And "RecoveryPublisher" enables simulcast for video publishing
    And "RecoveryPublisher" connects to room "RecoveryRoom" using the access token
    And connection is established successfully for "RecoveryPublisher"

    When "RecoverySubscriber" opens a "Chrome" browser with LiveKit Meet page
    And "RecoverySubscriber" connects to room "RecoveryRoom" using the access token
    And connection is established successfully for "RecoverySubscriber"
    And "RecoverySubscriber" waits for remote video from "RecoveryPublisher"

    When "RecoverySubscriber" sets video quality preference to "LOW"
    And "RecoverySubscriber" waits 2 seconds for quality adaptation
    And "RecoverySubscriber" records received video quality as "low_quality"

    When "RecoverySubscriber" sets video quality preference to "HIGH"
    And "RecoverySubscriber" waits 2 seconds for quality adaptation
    And "RecoverySubscriber" records received video quality as "recovered_quality"

    Then recorded quality "recovered_quality" should be greater than "low_quality"
    And "RecoveryPublisher" closes the browser
    And "RecoverySubscriber" closes the browser

  Scenario: Quality recovery after multiple degradations
    Given an access token is created with identity "MultiRecoveryPub" and room "MultiRecoveryRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "MultiRecoverySub" and room "MultiRecoveryRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiRecoveryRoom" is created using service "livekit1"

    When "MultiRecoveryPub" opens a "Chrome" browser with LiveKit Meet page
    And "MultiRecoveryPub" enables dynacast for the room connection
    And "MultiRecoveryPub" enables simulcast for video publishing
    And "MultiRecoveryPub" connects to room "MultiRecoveryRoom" using the access token
    And connection is established successfully for "MultiRecoveryPub"

    When "MultiRecoverySub" opens a "Chrome" browser with LiveKit Meet page
    And "MultiRecoverySub" connects to room "MultiRecoveryRoom" using the access token
    And connection is established successfully for "MultiRecoverySub"
    And "MultiRecoverySub" waits for remote video from "MultiRecoveryPub"

    When "MultiRecoverySub" sets video quality preference to "HIGH"
    And "MultiRecoverySub" waits 2 seconds for quality stabilization
    And "MultiRecoverySub" records received video quality as "initial_high"

    When "MultiRecoverySub" sets video quality preference to "LOW"
    And "MultiRecoverySub" waits 2 seconds for quality adaptation
    And "MultiRecoverySub" sets video quality preference to "HIGH"
    And "MultiRecoverySub" waits 2 seconds for quality adaptation
    And "MultiRecoverySub" sets video quality preference to "LOW"
    And "MultiRecoverySub" waits 2 seconds for quality adaptation
    And "MultiRecoverySub" sets video quality preference to "HIGH"
    And "MultiRecoverySub" waits 2 seconds for quality adaptation
    And "MultiRecoverySub" records received video quality as "final_high"

    Then recorded quality "final_high" should be similar to "initial_high"
    And "MultiRecoveryPub" closes the browser
    And "MultiRecoverySub" closes the browser

  # Story 1.1.3.6: Verify Dynacast Behavior with Multiple Subscribers
  Scenario: Multiple subscribers with different quality preferences
    Given an access token is created with identity "MultiSubPublisher" and room "MultiSubRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "HighQualitySub" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "LowQualitySub" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiSubRoom" is created using service "livekit1"

    When "MultiSubPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "MultiSubPublisher" enables dynacast for the room connection
    And "MultiSubPublisher" enables simulcast for video publishing
    And "MultiSubPublisher" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "MultiSubPublisher"

    When "HighQualitySub" opens a "Chrome" browser with LiveKit Meet page
    And "HighQualitySub" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "HighQualitySub"
    And "HighQualitySub" waits for remote video from "MultiSubPublisher"
    And "HighQualitySub" sets video quality preference to "HIGH"

    When "LowQualitySub" opens a "Firefox" browser with LiveKit Meet page
    And "LowQualitySub" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "LowQualitySub"
    And "LowQualitySub" waits for remote video from "MultiSubPublisher"
    And "LowQualitySub" sets video quality preference to "LOW"

    Then room "MultiSubRoom" should have 3 active participants in service "livekit1"

    When "HighQualitySub" waits 2 seconds for quality stabilization
    And "LowQualitySub" waits 2 seconds for quality stabilization
    And "HighQualitySub" records received video quality as "high_sub_quality"
    And "LowQualitySub" records received video quality as "low_sub_quality"

    Then recorded quality "high_sub_quality" should be greater than "low_sub_quality"
    And "MultiSubPublisher" closes the browser
    And "HighQualitySub" closes the browser
    And "LowQualitySub" closes the browser

  Scenario: All subscribers requesting low quality pauses high quality layers
    Given an access token is created with identity "AllLowPublisher" and room "AllLowRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "AllLowSub1" and room "AllLowRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "AllLowSub2" and room "AllLowRoom" with grants "canPublish:false,canSubscribe:true"
    And room "AllLowRoom" is created using service "livekit1"

    When "AllLowPublisher" opens a "Chrome" browser with LiveKit Meet page
    And "AllLowPublisher" enables dynacast for the room connection
    And "AllLowPublisher" enables simulcast for video publishing
    And "AllLowPublisher" connects to room "AllLowRoom" using the access token
    And connection is established successfully for "AllLowPublisher"

    When "AllLowSub1" opens a "Chrome" browser with LiveKit Meet page
    And "AllLowSub1" connects to room "AllLowRoom" using the access token
    And connection is established successfully for "AllLowSub1"
    And "AllLowSub1" waits for remote video from "AllLowPublisher"
    And "AllLowSub1" sets video quality preference to "LOW"

    When "AllLowSub2" opens a "Firefox" browser with LiveKit Meet page
    And "AllLowSub2" connects to room "AllLowRoom" using the access token
    And connection is established successfully for "AllLowSub2"
    And "AllLowSub2" waits for remote video from "AllLowPublisher"
    And "AllLowSub2" sets video quality preference to "LOW"

    When all subscribers wait 3 seconds for dynacast to pause unused layers

    Then dynacast should have paused high quality layers for "AllLowPublisher" video track
    And "AllLowSub1" should be receiving low quality video from "AllLowPublisher"
    And "AllLowSub2" should be receiving low quality video from "AllLowPublisher"
    And "AllLowPublisher" closes the browser
    And "AllLowSub1" closes the browser
    And "AllLowSub2" closes the browser

  Scenario: One subscriber requesting high quality resumes paused layers
    Given an access token is created with identity "ResumePublisher" and room "ResumeRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "ResumeLowSub" and room "ResumeRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "ResumeHighSub" and room "ResumeRoom" with grants "canPublish:false,canSubscribe:true"
    And room "ResumeRoom" is created using service "livekit1"

    When "ResumePublisher" opens a "Chrome" browser with LiveKit Meet page
    And "ResumePublisher" enables dynacast for the room connection
    And "ResumePublisher" enables simulcast for video publishing
    And "ResumePublisher" connects to room "ResumeRoom" using the access token
    And connection is established successfully for "ResumePublisher"

    When "ResumeLowSub" opens a "Chrome" browser with LiveKit Meet page
    And "ResumeLowSub" connects to room "ResumeRoom" using the access token
    And connection is established successfully for "ResumeLowSub"
    And "ResumeLowSub" waits for remote video from "ResumePublisher"
    And "ResumeLowSub" sets video quality preference to "LOW"

    When "ResumeHighSub" opens a "Firefox" browser with LiveKit Meet page
    And "ResumeHighSub" connects to room "ResumeRoom" using the access token
    And connection is established successfully for "ResumeHighSub"
    And "ResumeHighSub" waits for remote video from "ResumePublisher"
    And "ResumeHighSub" sets video quality preference to "LOW"

    When all subscribers wait 3 seconds for dynacast to pause unused layers
    Then dynacast should have paused high quality layers for "ResumePublisher" video track

    When "ResumeHighSub" sets video quality preference to "HIGH"
    And "ResumeHighSub" waits 2 seconds for quality adaptation

    Then dynacast should have resumed high quality layers for "ResumePublisher" video track
    And "ResumeHighSub" should be receiving high quality video from "ResumePublisher"
    And "ResumeLowSub" should be receiving low quality video from "ResumePublisher"
    And "ResumePublisher" closes the browser
    And "ResumeLowSub" closes the browser
    And "ResumeHighSub" closes the browser

  # Additional Edge Case Scenarios
  Scenario: Dynacast with dynacast disabled falls back to normal behavior
    Given an access token is created with identity "NoDynaPub" and room "NoDynaTestRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "NoDynaSub" and room "NoDynaTestRoom" with grants "canPublish:false,canSubscribe:true"
    And room "NoDynaTestRoom" is created using service "livekit1"

    When "NoDynaPub" opens a "Chrome" browser with LiveKit Meet page
    And "NoDynaPub" disables dynacast for the room connection
    And "NoDynaPub" enables simulcast for video publishing
    And "NoDynaPub" connects to room "NoDynaTestRoom" using the access token
    And connection is established successfully for "NoDynaPub"

    When "NoDynaSub" opens a "Chrome" browser with LiveKit Meet page
    And "NoDynaSub" connects to room "NoDynaTestRoom" using the access token
    And connection is established successfully for "NoDynaSub"
    And "NoDynaSub" waits for remote video from "NoDynaPub"

    When "NoDynaSub" sets video quality preference to "LOW"
    And "NoDynaSub" waits 2 seconds for any adaptation

    Then simulcast layers should still be available without dynacast optimization
    And "NoDynaPub" closes the browser
    And "NoDynaSub" closes the browser

  Scenario Outline: Cross-browser dynacast quality adaptation
    Given an access token is created with identity "CrossBrowserPub" and room "CrossBrowserDynaRoom" with grants "canPublish:true,canSubscribe:true"
    And an access token is created with identity "CrossBrowserSub" and room "CrossBrowserDynaRoom" with grants "canPublish:false,canSubscribe:true"
    And room "CrossBrowserDynaRoom" is created using service "livekit1"

    When "CrossBrowserPub" opens a <pubBrowser> browser with LiveKit Meet page
    And "CrossBrowserPub" enables dynacast for the room connection
    And "CrossBrowserPub" enables simulcast for video publishing
    And "CrossBrowserPub" connects to room "CrossBrowserDynaRoom" using the access token
    And connection is established successfully for "CrossBrowserPub"

    When "CrossBrowserSub" opens a <subBrowser> browser with LiveKit Meet page
    And "CrossBrowserSub" connects to room "CrossBrowserDynaRoom" using the access token
    And connection is established successfully for "CrossBrowserSub"
    And "CrossBrowserSub" waits for remote video from "CrossBrowserPub"

    When "CrossBrowserSub" records current received video dimensions
    And "CrossBrowserSub" sets video quality preference to "LOW"
    And "CrossBrowserSub" waits 2 seconds for quality adaptation

    Then "CrossBrowserSub" should be receiving lower quality video than initially recorded
    And "CrossBrowserPub" closes the browser
    And "CrossBrowserSub" closes the browser

    Examples:
      | pubBrowser | subBrowser |
      | "Chrome"   | "Firefox"  |
      | "Firefox"  | "Chrome"   |
      | "Chrome"   | "Edge"     |
```

---

## Definition of Done

### Code Implementation
- [ ] New step definitions added to `LiveKitBrowserWebrtcSteps.java` for dynacast control
- [ ] New step definitions added to `LiveKitRoomSteps.java` for dynacast verification
- [ ] `LiveKitMeet.java` extended with dynacast enable/disable and quality monitoring methods
- [ ] JavaScript helpers added for dynacast state tracking and quality measurement
- [ ] Feature file `livekit_dynacast.feature` created and passing
- [ ] All scenarios pass on Chrome browser
- [ ] All scenarios pass on Firefox browser
- [ ] All scenarios pass on Edge browser

### Testing
- [ ] All unit tests pass
- [ ] All BDD scenarios pass
- [ ] Tests pass against default LiveKit version
- [ ] Quality adaptation is measurable and verifiable
- [ ] Response time measurement is implemented
- [ ] Multi-subscriber scenarios verified

### Documentation
- [ ] Feature documentation complete in `docs/features/dynacast-testing/`
- [ ] Step definitions documented in `docs/features.md`
- [ ] Technical notes added for dynacast implementation
- [ ] Response time thresholds documented
- [ ] Known limitations documented

### Code Quality
- [ ] No new Lombok violations
- [ ] No code comments added (per project guidelines)
- [ ] Cross-platform path handling maintained
- [ ] Proper cleanup in After hooks

---

## Technical Components

### New Step Definitions Required

#### In `LiveKitBrowserWebrtcSteps.java`:
```java
@When("{string} enables dynacast for the room connection")
public void enablesDynacastForRoomConnection(String participantName)

@When("{string} disables dynacast for the room connection")
public void disablesDynacastForRoomConnection(String participantName)

@When("{string} waits for remote video from {string}")
public void waitsForRemoteVideoFrom(String subscriber, String publisher)

@When("{string} records current received video dimensions")
public void recordsCurrentReceivedVideoDimensions(String participantName)

@When("{string} records received video quality as {string}")
public void recordsReceivedVideoQualityAs(String participantName, String qualityLabel)

@When("{string} waits {int} seconds for quality adaptation")
public void waitsSecondsForQualityAdaptation(String participantName, int seconds)

@When("{string} waits {int} seconds for quality stabilization")
public void waitsSecondsForQualityStabilization(String participantName, int seconds)

@When("{string} starts response time measurement")
public void startsResponseTimeMeasurement(String participantName)

@When("{string} waits for quality change detection")
public void waitsForQualityChangeDetection(String participantName)

@When("{string} measures response time for {int} quality changes between LOW and HIGH")
public void measuresResponseTimeForQualityChanges(String participantName, int count)

@When("all subscribers wait {int} seconds for dynacast to pause unused layers")
public void allSubscribersWaitForDynacastPause(int seconds)

@Then("{string} should be receiving lower quality video than initially recorded")
public void shouldBeReceivingLowerQualityThanRecorded(String participantName)

@Then("{string} should be receiving video from {string} in room {string} using service {string}")
public void shouldBeReceivingVideoFrom(String subscriber, String publisher, String room, String service)

@Then("{string} should not be receiving video from {string}")
public void shouldNotBeReceivingVideoFrom(String subscriber, String publisher)

@Then("recorded quality {string} should be greater than {string}")
public void recordedQualityShouldBeGreaterThan(String higherLabel, String lowerLabel)

@Then("recorded quality {string} should be similar to {string}")
public void recordedQualityShouldBeSimilarTo(String label1, String label2)

@Then("the measured dynacast response time should be less than {int} ms")
public void measuredResponseTimeShouldBeLessThan(int maxMs)

@Then("the average dynacast response time should be less than {int} ms")
public void averageResponseTimeShouldBeLessThan(int maxMs)

@Then("the maximum dynacast response time should be less than {int} ms")
public void maximumResponseTimeShouldBeLessThan(int maxMs)
```

#### In `LiveKitRoomSteps.java`:
```java
@Then("participant {string} should have dynacast active in room {string} using service {string}")
public void participantShouldHaveDynacastActive(String identity, String room, String service)

@Then("participant {string} should have dynacast inactive in room {string} using service {string}")
public void participantShouldHaveDynacastInactive(String identity, String room, String service)

@Then("participant {string} video track should have dynacast-managed layers in room {string} using service {string}")
public void videoTrackShouldHaveDynacastManagedLayers(String identity, String room, String service)

@Then("dynacast should have paused high quality layers for {string} video track")
public void dynacastShouldHavePausedHighQualityLayers(String identity)

@Then("dynacast should have resumed high quality layers for {string} video track")
public void dynacastShouldHaveResumedHighQualityLayers(String identity)

@Then("simulcast layers should still be available without dynacast optimization")
public void simulcastLayersShouldBeAvailableWithoutDynacast()
```

### LiveKitMeet Page Object Extensions

```java
public void setDynacastEnabled(boolean enabled)
public boolean isDynacastEnabled()
public Map<String, Integer> recordVideoDimensions(String publisherIdentity)
public void recordQualitySnapshot(String label)
public Map<String, Object> getRecordedQuality(String label)
public int compareRecordedQualities(String label1, String label2)
public void startResponseTimeMeasurement()
public long waitForQualityChange(int timeoutMs)
public Map<String, Object> measureMultipleResponseTimes(int count)
public boolean isReceivingVideoFrom(String publisherIdentity)
public Map<String, Integer> getCurrentReceivedDimensions(String publisherIdentity)
```

### Web Application Changes (livekit-client.js)

The LiveKit Meet web application needs:
- Dynacast toggle setting (stored before room creation)
- Quality dimension tracking for received videos
- Quality snapshot recording with labels
- Response time measurement state
- Quality change detection callbacks
- Enhanced remote video tracking

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Layer pause state not directly observable via API | High | Medium | Use indirect metrics (quality preference acknowledgment, dimension changes) |
| Response time measurement accuracy limited | Medium | Medium | Use multiple samples; establish baseline in container environment |
| Fake video stream affects quality measurements | Low | Medium | Focus on relative quality changes, not absolute values |
| Multi-browser timing coordination complex | Medium | Medium | Add sufficient wait times; use server-side verification |
| Dynacast behavior varies across LiveKit versions | Medium | Low | Test against default version; document version differences |

### Mitigation Strategies

1. **Layer State Detection**: Focus on observable outcomes (received dimensions, quality preference acknowledgment) rather than internal dynacast state

2. **Response Time**: Establish realistic thresholds for containerized testing (2000ms suggested); use statistical analysis

3. **Quality Measurement**: Record relative changes (before/after) rather than absolute quality values

4. **Synchronization**: Use server-side participant and track verification for coordination

5. **Version Compatibility**: Document tested version; add version checks if behavior differs significantly

---

## Implementation Order

| Order | Story | Size | Reason |
|-------|-------|------|--------|
| 1 | 1.1.3.1 - Dynacast Configuration | S | Foundation for all dynacast tests |
| 2 | 1.1.3.2 - Verify via Server API | S | Establishes verification pattern |
| 3 | 1.1.3.3 - Quality Adaptation | M | Core functionality test |
| 4 | 1.1.3.5 - Quality Recovery | M | Complements adaptation testing |
| 5 | 1.1.3.4 - Response Time | M | Performance measurement |
| 6 | 1.1.3.6 - Multiple Subscribers | L | Complex scenario, builds on all others |

**Total Estimated Effort:** L (Large) - approximately 5-8 days

---

## Appendix: LiveKit Dynacast Reference

### Room Options for Dynacast

```typescript
const room = new Room({
    adaptiveStream: true,  // Enable adaptive streaming
    dynacast: true,        // Enable dynacast (dynamic broadcasting)
    videoCaptureDefaults: {
        resolution: VideoPresets.h720,
    },
    publishDefaults: {
        simulcast: true,   // Required for dynacast to work effectively
        videoSimulcastLayers: [
            VideoPresets.h180,
            VideoPresets.h360,
        ],
    },
});
```

### VideoQuality Enum

```typescript
enum VideoQuality {
  LOW = 0,     // Lowest quality layer
  MEDIUM = 1,  // Medium quality layer
  HIGH = 2,    // Highest quality layer
  OFF = 3      // Disable video reception
}
```

### Subscriber Quality Control

```typescript
// Set quality for a specific remote track
publication.setVideoQuality(VideoQuality.LOW);

// The server/dynacast responds by adjusting which layers are sent
```

### Dynacast Behavior

| Subscriber State | Dynacast Response |
|------------------|-------------------|
| Quality = HIGH | Transmit all layers needed for high quality |
| Quality = MEDIUM | Transmit medium and low layers, pause high |
| Quality = LOW | Transmit only low quality layer |
| Quality = OFF | Pause all layer transmission |
| Video hidden | May pause transmission (visibility-based) |
| Multiple subscribers with mixed quality | Transmit highest quality any subscriber needs |

---

## Related Stories

- **Story 1.1.2** (Simulcast) - Completed; dynacast works with simulcast layers
- **Story 1.1.7** (Track Mute) - Completed; similar state verification patterns
- **Story 5.2.1** (Network Simulation) - Future; could provide real bandwidth testing
- **Story 4.2.2** (Resource Utilization) - Future; could measure dynacast bandwidth savings
