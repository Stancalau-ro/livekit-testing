Feature: LiveKit Access Token Generation
  As a developer
  I want to create and validate LiveKit access tokens
  So that I can authenticate clients with the LiveKit server

  Background:
    Given a LiveKit server is running in a container
    And LiveKit server credentials are available

  Scenario: Generate a valid access token for Bob
    When an access token is created with identity "Bob"
    Then the access token for "Bob" should be valid
    And the access token for "Bob" should contain the correct identity

  Scenario: Generate access token with room permissions for Bob
    When an access token is created with identity "Bob" and room "BobsRoom" with publish permissions
    Then the access token for "Bob" in room "BobsRoom" should be valid
    And the access token for "Bob" should contain room "BobsRoom"
    And the access token for "Bob" in room "BobsRoom" should have publish permissions

  Scenario: Generate access token with expiration time for Bob
    When an access token is created with identity "Bob" that expires in 1 hour
    Then the access token for "Bob" should be valid
    And the access token for "Bob" should not be expired

  Scenario: Generate access token that expires quickly for Alice
    When an access token is created with identity "Alice" that expires in 3 seconds
    Then the access token for "Alice" should be valid
    When waiting for 4 seconds
    Then the access token for "Alice" should be expired