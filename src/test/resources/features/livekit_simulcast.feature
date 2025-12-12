Feature: Simulcast Video Publishing
  As a test developer
  I want to test simulcast video publishing
  So that I can verify multiple quality layers are published

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Participant publishes video with simulcast enabled (default)
    Given an access token is created with identity "Alice" and room "SimulcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "SimulcastRoom" is created using service "livekit1"

    When "Alice" opens a "Chrome" browser with LiveKit Meet page
    And "Alice" connects to room "SimulcastRoom" using the access token
    And connection is established successfully for "Alice"

    Then room "SimulcastRoom" should have 1 active participants in service "livekit1"
    And participant "Alice" should be publishing video in room "SimulcastRoom" using service "livekit1"
    And participant "Alice" should have simulcast enabled for video in room "SimulcastRoom" using service "livekit1"
    And "Alice" closes the browser

  Scenario: Participant publishes video with simulcast disabled
    Given an access token is created with identity "Bob" and room "NoSimulcastRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "NoSimulcastRoom" is created using service "livekit1"

    When "Bob" opens a "Chrome" browser with LiveKit Meet page
    And "Bob" disables simulcast for video publishing
    And "Bob" connects to room "NoSimulcastRoom" using the access token
    And connection is established successfully for "Bob"

    Then room "NoSimulcastRoom" should have 1 active participants in service "livekit1"
    And participant "Bob" should be publishing video in room "NoSimulcastRoom" using service "livekit1"
    And participant "Bob" should have simulcast disabled for video in room "NoSimulcastRoom" using service "livekit1"
    And "Bob" closes the browser

  Scenario: Simulcast track has multiple video layers
    Given an access token is created with identity "Charlie" and room "LayerRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "LayerRoom" is created using service "livekit1"

    When "Charlie" opens a "Chrome" browser with LiveKit Meet page
    And "Charlie" enables simulcast for video publishing
    And "Charlie" connects to room "LayerRoom" using the access token
    And connection is established successfully for "Charlie"

    Then room "LayerRoom" should have 1 active participants in service "livekit1"
    And participant "Charlie" should be publishing video in room "LayerRoom" using service "livekit1"
    And participant "Charlie" should have simulcast enabled for video in room "LayerRoom" using service "livekit1"
    And participant "Charlie" video track should have at least 2 layers in room "LayerRoom" using service "livekit1"
    And "Charlie" closes the browser

  Scenario: Non-simulcast track has single video layer
    Given an access token is created with identity "Diana" and room "SingleLayerRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "SingleLayerRoom" is created using service "livekit1"

    When "Diana" opens a "Chrome" browser with LiveKit Meet page
    And "Diana" disables simulcast for video publishing
    And "Diana" connects to room "SingleLayerRoom" using the access token
    And connection is established successfully for "Diana"

    Then room "SingleLayerRoom" should have 1 active participants in service "livekit1"
    And participant "Diana" should be publishing video in room "SingleLayerRoom" using service "livekit1"
    And participant "Diana" should have simulcast disabled for video in room "SingleLayerRoom" using service "livekit1"
    And participant "Diana" video track should have exactly 1 layer in room "SingleLayerRoom" using service "livekit1"
    And "Diana" closes the browser

  Scenario: Simulcast layers have different resolutions
    Given an access token is created with identity "Edward" and room "QualityRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "QualityRoom" is created using service "livekit1"

    When "Edward" opens a "Chrome" browser with LiveKit Meet page
    And "Edward" enables simulcast for video publishing
    And "Edward" connects to room "QualityRoom" using the access token
    And connection is established successfully for "Edward"

    Then room "QualityRoom" should have 1 active participants in service "livekit1"
    And participant "Edward" should be publishing video in room "QualityRoom" using service "livekit1"
    And participant "Edward" video track should have at least 2 layers in room "QualityRoom" using service "livekit1"
    And participant "Edward" video layers should have different resolutions in room "QualityRoom" using service "livekit1"
    And participant "Edward" highest video layer should have greater resolution than lowest layer in room "QualityRoom" using service "livekit1"
    And "Edward" closes the browser

  Scenario: Subscriber can set video quality preference to LOW
    Given an access token is created with identity "Frank" and room "SubQualityRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Grace" and room "SubQualityRoom" with grants "canPublish:false,canSubscribe:true"
    And room "SubQualityRoom" is created using service "livekit1"

    When "Frank" opens a "Chrome" browser with LiveKit Meet page
    And "Frank" enables simulcast for video publishing
    And "Frank" connects to room "SubQualityRoom" using the access token
    And connection is established successfully for "Frank"

    Then room "SubQualityRoom" should have 1 active participants in service "livekit1"
    And participant "Frank" should be publishing video in room "SubQualityRoom" using service "livekit1"
    And participant "Frank" should have simulcast enabled for video in room "SubQualityRoom" using service "livekit1"

    When "Grace" opens a "Chrome" browser with LiveKit Meet page
    And "Grace" connects to room "SubQualityRoom" using the access token
    And connection is established successfully for "Grace"

    Then room "SubQualityRoom" should have 2 active participants in service "livekit1"

    When "Grace" sets video quality preference to "LOW"

    Then "Grace" should be receiving low quality video from "Frank"
    And "Frank" closes the browser
    And "Grace" closes the browser

  Scenario: Subscriber can set video quality preference to HIGH
    Given an access token is created with identity "Henry" and room "HighQualityRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Ivy" and room "HighQualityRoom" with grants "canPublish:false,canSubscribe:true"
    And room "HighQualityRoom" is created using service "livekit1"

    When "Henry" opens a "Chrome" browser with LiveKit Meet page
    And "Henry" enables simulcast for video publishing
    And "Henry" connects to room "HighQualityRoom" using the access token
    And connection is established successfully for "Henry"

    Then room "HighQualityRoom" should have 1 active participants in service "livekit1"
    And participant "Henry" should be publishing video in room "HighQualityRoom" using service "livekit1"

    When "Ivy" opens a "Chrome" browser with LiveKit Meet page
    And "Ivy" connects to room "HighQualityRoom" using the access token
    And connection is established successfully for "Ivy"

    Then room "HighQualityRoom" should have 2 active participants in service "livekit1"

    When "Ivy" sets video quality preference to "HIGH"

    Then "Ivy" should be receiving high quality video from "Henry"
    And "Henry" closes the browser
    And "Ivy" closes the browser

  Scenario: Subscriber receives lower quality under bandwidth constraints
    Given an access token is created with identity "Kevin" and room "BandwidthRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Laura" and room "BandwidthRoom" with grants "canPublish:false,canSubscribe:true"
    And room "BandwidthRoom" is created using service "livekit1"

    When "Kevin" opens a "Chrome" browser with LiveKit Meet page
    And "Kevin" enables simulcast for video publishing
    And "Kevin" connects to room "BandwidthRoom" using the access token
    And connection is established successfully for "Kevin"

    Then room "BandwidthRoom" should have 1 active participants in service "livekit1"
    And participant "Kevin" should be publishing video in room "BandwidthRoom" using service "livekit1"
    And participant "Kevin" should have simulcast enabled for video in room "BandwidthRoom" using service "livekit1"

    When "Laura" opens a "Chrome" browser with LiveKit Meet page
    And "Laura" connects to room "BandwidthRoom" using the access token
    And connection is established successfully for "Laura"

    Then room "BandwidthRoom" should have 2 active participants in service "livekit1"

    When "Laura" sets maximum receive bandwidth to 100 kbps

    Then "Laura" should be receiving a lower quality layer from "Kevin"
    And "Kevin" closes the browser
    And "Laura" closes the browser

  Scenario Outline: Participant can publish simulcast video with different browsers
    Given an access token is created with identity "Mike" and room "BrowserRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "BrowserRoom" is created using service "livekit1"

    When "Mike" opens a <browser> browser with LiveKit Meet page
    And "Mike" enables simulcast for video publishing
    And "Mike" connects to room "BrowserRoom" using the access token
    And connection is established successfully for "Mike"

    Then room "BrowserRoom" should have 1 active participants in service "livekit1"
    And participant "Mike" should be publishing video in room "BrowserRoom" using service "livekit1"
    And participant "Mike" should have simulcast enabled for video in room "BrowserRoom" using service "livekit1"
    And "Mike" closes the browser

    Examples:
      | browser   |
      | "Chrome"  |
      | "Firefox" |
