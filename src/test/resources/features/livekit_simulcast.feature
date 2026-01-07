Feature: Simulcast Video Publishing
  As a test developer
  I want to test simulcast video publishing
  So that I can verify multiple quality layers are published

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

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
    And participant "Diana" video track should have "1" layers in room "SingleLayerRoom" using service "livekit1"
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
    And participant "Edward" should have simulcast enabled for video in room "QualityRoom" using service "livekit1"
    And participant "Edward" video track should have ">=2" layers in room "QualityRoom" using service "livekit1"
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
    And "Grace" should be receiving video from "Frank"

    When "Frank" measures their video publish bitrate over 3 seconds
    And "Grace" sets video quality preference to "LOW"

    Then "Grace" should be receiving low quality video from "Frank"
    And "Frank"'s video publish bitrate should have dropped by at least 30 percent
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
    And "Ivy" should be receiving video from "Henry"

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
    And "Laura" should be receiving video from "Kevin"

    When "Kevin" measures their video publish bitrate over 3 seconds
    And "Laura" sets maximum receive bandwidth to 100 kbps

    Then "Laura" should be receiving a lower quality layer from "Kevin"
    And "Kevin"'s video publish bitrate should have dropped by at least 30 percent
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
