Feature: LiveKit Access Token Generation
  As a developer
  I want to create and validate LiveKit access tokens
  So that I can authenticate clients with the LiveKit server

  Background:
    Given a LiveKit server is running in a container
    And LiveKit server credentials are available

  Scenario: Generate access token with publish room permissions for Bob
    When an access token is created with identity "Bob" and room "BobsRoom" with publish permissions
    Then the access token for "Bob" in room "BobsRoom" should be valid
    And the access token for "Bob" should contain room "BobsRoom"

  Scenario: Generate access token with subscribe room permissions for Bob
    When an access token is created with identity "Bob" and room "BobsRoom" with subscribe permissions
    Then the access token for "Bob" in room "BobsRoom" should be valid
    And the access token for "Bob" should contain room "BobsRoom"

  Scenario: Generate access token with dynamic grants for Charlie
    When an access token is created with identity "Charlie" and room "MeetingRoom" with grants "canPublish:true,canSubscribe:true"
    Then the access token for "Charlie" in room "MeetingRoom" should be valid

  Scenario: Generate access token with admin permissions for Dave
    When an access token is created with identity "Dave" and room "AdminRoom" with grants "roomAdmin:true,roomCreate:true,roomList:true"
    Then the access token for "Dave" in room "AdminRoom" should be valid

  Scenario: Generate access token with recording permissions for Eve
    When an access token is created with identity "Eve" and room "RecordingRoom" with grants "roomRecord:true,recorder:true"
    Then the access token for "Eve" in room "RecordingRoom" should be valid

  Scenario: Generate access token with custom attributes for Frank
    When an access token is created with identity "Frank" and room "CustomRoom" with grants "canPublish:true" and attributes "role=moderator,department=engineering,level=senior"
    Then the access token for "Frank" in room "CustomRoom" should be valid

  Scenario: Generate access token with escaped comma attributes for Grace
    When an access token is created with identity "Grace" and room "TestRoom" with grants "canPublish:true,canSubscribe:true" and attributes "description=A room for testing\, debugging\, and development,tags=test\,debug\,dev,fullname=Grace O'Connor\, Senior Engineer"
    Then the access token for "Grace" in room "TestRoom" should be valid

  Scenario: Generate access token with hidden participant for Henry
    When an access token is created with identity "Henry" and room "SecretRoom" with grants "hidden:true,canSubscribe:true"
    Then the access token for "Henry" in room "SecretRoom" should be valid

  Scenario: Generate agent token for Ivy
    When an access token is created with identity "Ivy" and room "AgentRoom" with grants "agent:true,canPublishData:true"
    Then the access token for "Ivy" in room "AgentRoom" should be valid

  Scenario: Generate access token with expiration for Jack
    When an access token is created with identity "Jack" and room "TempRoom" with grants "canPublish:true,canSubscribe:true" that expires in 30 seconds
    Then the access token for "Jack" in room "TempRoom" should be valid

  Scenario: Generate access token with long expiration for Kate
    When an access token is created with identity "Kate" and room "LongTermRoom" with grants "roomAdmin:true" that expires in 60 minutes
    Then the access token for "Kate" in room "LongTermRoom" should be valid

  Scenario: Generate access token with expiration and custom attributes for Leo
    When an access token is created with identity "Leo" and room "CustomExpRoom" with grants "canPublish:true,canSubscribe:true" and attributes "role=presenter,session=demo" that expires in 45 seconds
    Then the access token for "Leo" in room "CustomExpRoom" should be valid

  Scenario: Generate access token with minute-based expiration for Maya
    When an access token is created with identity "Maya" and room "MeetingRoom" with grants "roomRecord:true,canPublish:true" and attributes "department=marketing,level=manager" that expires in 2 minutes
    Then the access token for "Maya" in room "MeetingRoom" should be valid