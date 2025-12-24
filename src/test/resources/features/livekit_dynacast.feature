Feature: Dynacast Bandwidth Adaptation
  As a test developer
  I want to test dynacast functionality
  So that I can verify automatic quality adaptation

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Video track pauses when unsubscribed and resumes when resubscribed
    Given an access token is created with identity "Oscar" and room "DynacastRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Patricia" and room "DynacastRoom" with grants "canPublish:false,canSubscribe:true"
    And room "DynacastRoom" is created using service "livekit1"

    When "Oscar" opens a "Chrome" browser with LiveKit Meet page
    And "Oscar" enables simulcast for video publishing
    And "Oscar" connects to room "DynacastRoom" using the access token
    And connection is established successfully for "Oscar"

    And "Patricia" opens a "Chrome" browser with LiveKit Meet page
    And "Patricia" connects to room "DynacastRoom" using the access token
    And connection is established successfully for "Patricia"

    Then room "DynacastRoom" should have 2 active participants in service "livekit1"
    And dynacast should be enabled in the room for "Patricia"
    And "Oscar"'s video track should be active for "Patricia"

    When "Patricia" unsubscribes from "Oscar"'s video
    Then "Oscar"'s video track should be paused for "Patricia"

    When "Patricia" subscribes to "Oscar"'s video
    Then "Oscar"'s video track should be active for "Patricia"

    And "Oscar" closes the browser
    And "Patricia" closes the browser

  Scenario: Quality preference change triggers layer adaptation
    Given an access token is created with identity "Victor" and room "QualityRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Wendy" and room "QualityRoom" with grants "canPublish:false,canSubscribe:true"
    And room "QualityRoom" is created using service "livekit1"

    When "Victor" opens a "Chrome" browser with LiveKit Meet page
    And "Victor" enables simulcast for video publishing
    And "Victor" connects to room "QualityRoom" using the access token
    And connection is established successfully for "Victor"

    And "Wendy" opens a "Chrome" browser with LiveKit Meet page
    And "Wendy" connects to room "QualityRoom" using the access token
    And connection is established successfully for "Wendy"

    Then room "QualityRoom" should have 2 active participants in service "livekit1"
    And participant "Victor" should have simulcast enabled for video in room "QualityRoom" using service "livekit1"

    When "Victor" measures their video publish bitrate over 3 seconds
    And "Wendy" sets video quality preference to "LOW"
    Then "Wendy" should be receiving low quality video from "Victor"
    And "Victor"'s video publish bitrate should have dropped by at least 30 percent

    And "Victor" closes the browser
    And "Wendy" closes the browser

  Scenario Outline: Dynacast subscription control works across browsers
    Given an access token is created with identity "Xavier" and room "CrossBrowserRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And an access token is created with identity "Yvonne" and room "CrossBrowserRoom" with grants "canPublish:false,canSubscribe:true"
    And room "CrossBrowserRoom" is created using service "livekit1"

    When "Xavier" opens a <publisher_browser> browser with LiveKit Meet page
    And "Xavier" enables simulcast for video publishing
    And "Xavier" connects to room "CrossBrowserRoom" using the access token
    And connection is established successfully for "Xavier"

    And "Yvonne" opens a <subscriber_browser> browser with LiveKit Meet page
    And "Yvonne" connects to room "CrossBrowserRoom" using the access token
    And connection is established successfully for "Yvonne"

    Then room "CrossBrowserRoom" should have 2 active participants in service "livekit1"

    When "Yvonne" unsubscribes from "Xavier"'s video
    Then "Xavier"'s video track should be paused for "Yvonne"

    And "Xavier" closes the browser
    And "Yvonne" closes the browser

    Examples:
      | publisher_browser | subscriber_browser |
      | "Chrome"          | "Firefox"          |
      | "Firefox"         | "Chrome"           |
