Feature: LiveKit Screen Sharing
  As a test developer
  I want to test screen sharing functionality
  So that I can verify screen share track publishing and subscription

  Background:
    Given the LiveKit config is set to "basic"
    And a LiveKit server is running in a container with service name "livekit1"

  Scenario: Generate access token with screen share permission
    When an access token is created with identity "Oliver" and room "ScreenRoom" with grants "canPublish:true,canPublishSources:screen_share"
    Then the access token for "Oliver" in room "ScreenRoom" should be valid
    And the access token for "Oliver" in room "ScreenRoom" should have the following grants:
      | grant             | value          |
      | canPublish        | true           |
      | roomJoin          | true           |
      | canPublishSources | [screen_share] |

  Scenario: Generate access token without screen share permission
    When an access token is created with identity "Patricia" and room "RestrictedRoom" with grants "canPublish:true,canPublishSources:camera\,microphone"
    Then the access token for "Patricia" in room "RestrictedRoom" should be valid
    And the access token for "Patricia" in room "RestrictedRoom" should have the following grants:
      | grant             | value                |
      | canPublish        | true                 |
      | roomJoin          | true                 |
      | canPublishSources | [microphone, camera] |

  Scenario Outline: Participant can publish screen share track
    Given an access token is created with identity "Quinn" and room "ScreenShareRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone\,screen_share"
    And room "ScreenShareRoom" is created using service "livekit1"

    When "Quinn" opens a <browser> browser with LiveKit Meet page
    And "Quinn" connects to room "ScreenShareRoom" using the access token
    And connection is established successfully for "Quinn"
    And "Quinn" starts screen sharing

    Then room "ScreenShareRoom" should have 1 active participants in service "livekit1"
    And participant "Quinn" should be publishing screen share in room "ScreenShareRoom" using service "livekit1"
    And participant "Quinn" should have 3 published tracks in room "ScreenShareRoom" using service "livekit1"

    Examples:
      | browser  |
      | "Chrome" |
      | "Edge"   |

  Scenario: Multiple subscribers receive screen share track across browsers
    Given an access token is created with identity "Sam" and room "MultiSubRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone\,screen_share"
    And an access token is created with identity "Tina" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And an access token is created with identity "Uma" and room "MultiSubRoom" with grants "canPublish:false,canSubscribe:true"
    And room "MultiSubRoom" is created using service "livekit1"

    When "Sam" opens a "Chrome" browser with LiveKit Meet page
    And "Sam" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Sam"
    And "Sam" starts screen sharing

    Then participant "Sam" should be publishing screen share in room "MultiSubRoom" using service "livekit1"

    When "Tina" opens a "Chrome" browser with LiveKit Meet page
    And "Tina" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Tina"

    When "Uma" opens a "Firefox" browser with LiveKit Meet page
    And "Uma" connects to room "MultiSubRoom" using the access token
    And connection is established successfully for "Uma"

    Then room "MultiSubRoom" should have 3 active participants in service "livekit1"
    And participant "Tina" should see 1 remote screen share tracks in room "MultiSubRoom" using service "livekit1"
    And participant "Uma" should see 1 remote screen share tracks in room "MultiSubRoom" using service "livekit1"

  Scenario: Screen share can be stopped and restarted
    Given an access token is created with identity "Victor" and room "StopRestartRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone\,screen_share"
    And an access token is created with identity "Wendy" and room "StopRestartRoom" with grants "canPublish:false,canSubscribe:true"
    And room "StopRestartRoom" is created using service "livekit1"

    When "Victor" opens a "Chrome" browser with LiveKit Meet page
    And "Victor" connects to room "StopRestartRoom" using the access token
    And connection is established successfully for "Victor"
    And "Victor" starts screen sharing

    Then participant "Victor" should be publishing screen share in room "StopRestartRoom" using service "livekit1"

    When "Wendy" opens a "Chrome" browser with LiveKit Meet page
    And "Wendy" connects to room "StopRestartRoom" using the access token
    And connection is established successfully for "Wendy"

    Then participant "Wendy" should see 1 remote screen share tracks in room "StopRestartRoom" using service "livekit1"

    When "Victor" stops screen sharing

    Then participant "Victor" should not be publishing screen share in room "StopRestartRoom" using service "livekit1"
    And participant "Wendy" should see 0 remote screen share tracks in room "StopRestartRoom" using service "livekit1"

    When "Victor" starts screen sharing

    Then participant "Victor" should be publishing screen share in room "StopRestartRoom" using service "livekit1"
    And participant "Wendy" should see 1 remote screen share tracks in room "StopRestartRoom" using service "livekit1"

  Scenario: Camera works but screen share is blocked without screen share permission
    Given an access token is created with identity "Xavier" and room "PermissionRoom" with grants "canPublish:true,canSubscribe:true,canPublishSources:camera\,microphone"
    And room "PermissionRoom" is created using service "livekit1"

    When "Xavier" opens a "Chrome" browser with LiveKit Meet page
    And "Xavier" connects to room "PermissionRoom" using the access token
    And connection is established successfully for "Xavier"

    Then participant "Xavier" should be publishing video in room "PermissionRoom" using service "livekit1"
    And participant "Xavier" should not be publishing screen share in room "PermissionRoom" using service "livekit1"

    When "Xavier" attempts to start screen sharing

    Then participant "Xavier" should have screen share blocked due to permissions
    And participant "Xavier" should not be publishing screen share in room "PermissionRoom" using service "livekit1"
