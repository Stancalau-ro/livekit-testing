Feature: LiveKit Access Token Generation
  As a developer
  I want to create and validate LiveKit access tokens
  So that I can authenticate clients with the LiveKit server

  Scenario: Generate access token with publish room permissions for Bob
    When the system creates an access token with identity "Bob" and room "BobsRoom" with publish permissions
    Then the access token for "Bob" in room "BobsRoom" should be valid
    And the access token for "Bob" should contain room "BobsRoom"

  Scenario: Generate access token with subscribe room permissions for Bob
    When the system creates an access token with identity "Bob" and room "BobsRoom" with subscribe permissions
    Then the access token for "Bob" in room "BobsRoom" should be valid
    And the access token for "Bob" should contain room "BobsRoom"

  Scenario: Generate access token with dynamic grants for Charlie
    When the system creates an access token with identity "Charlie" and room "MeetingRoom" with grants "canPublish:true,canSubscribe:true"
    Then the access token for "Charlie" in room "MeetingRoom" should be valid
    And the access token for "Charlie" in room "MeetingRoom" should have the following grants:
      | grant        | value |
      | canPublish   | true  |
      | canSubscribe | true  |
      | roomJoin     | true  |
    And the access token for "Charlie" in room "MeetingRoom" should not have the following grants:
      | grant     |
      | roomAdmin |

  Scenario: Generate access token with admin permissions for Dave
    When the system creates an access token with identity "Dave" and room "AdminRoom" with grants "roomAdmin:true,roomCreate:true,roomList:true"
    Then the access token for "Dave" in room "AdminRoom" should be valid
    And the access token for "Dave" in room "AdminRoom" should have the following grants:
      | grant      | value |
      | roomAdmin  | true  |
      | roomCreate | true  |
      | roomList   | true  |
      | roomJoin   | true  |
    And the access token for "Dave" in room "AdminRoom" should not have the following grants:
      | grant      |
      | canPublish |

  Scenario: Generate access token with recording permissions for Eve
    When the system creates an access token with identity "Eve" and room "RecordingRoom" with grants "roomRecord:true,recorder:true"
    Then the access token for "Eve" in room "RecordingRoom" should be valid

  Scenario: Generate access token with custom attributes for Frank
    When the system creates an access token with identity "Frank" and room "CustomRoom" with grants "canPublish:true" and attributes "role=moderator,department=engineering,level=senior"
    Then the access token for "Frank" in room "CustomRoom" should be valid
    And the access token for "Frank" in room "CustomRoom" should have the following attributes:
      | attribute  | value       |
      | role       | moderator   |
      | department | engineering |
      | level      | senior      |
    And the access token for "Frank" in room "CustomRoom" should not have the following attributes:
      | attribute |
      | team      |

  Scenario: Generate access token with escaped comma attributes for Grace
    When the system creates an access token with identity "Grace" and room "TestRoom" with grants "canPublish:true,canSubscribe:true" and attributes "description=A room for testing\, debugging\, and development,tags=test\,debug\,dev,fullname=Grace O'Connor\, Senior Engineer"
    Then the access token for "Grace" in room "TestRoom" should be valid
    And the access token for "Grace" in room "TestRoom" should have the following attributes:
      | attribute   | value                                            |
      | description | A room for testing, debugging, and development   |
      | tags        | test,debug,dev                                   |
      | fullname    | Grace O'Connor, Senior Engineer                  |

  Scenario: Generate access token with hidden participant for Henry
    When the system creates an access token with identity "Henry" and room "SecretRoom" with grants "hidden:true,canSubscribe:true"
    Then the access token for "Henry" in room "SecretRoom" should be valid
    And the access token for "Henry" in room "SecretRoom" should have the following grants:
      | grant        | value |
      | canSubscribe | true  |
      | roomJoin     | true  |
      | hidden       | true  |
    And the access token for "Henry" in room "SecretRoom" should not have the following grants:
      | grant      |
      | canPublish |
      | roomAdmin  |
      | agent      |

  Scenario: Generate agent token for Ivy
    When the system creates an access token with identity "Ivy" and room "AgentRoom" with grants "agent:true,canPublishData:true"
    Then the access token for "Ivy" in room "AgentRoom" should be valid
    And the access token for "Ivy" in room "AgentRoom" should have the following grants:
      | grant           | value |
      | agent           | true  |
      | canPublishData  | true  |
      | roomJoin        | true  |
    And the access token for "Ivy" in room "AgentRoom" should not have the following grants:
      | grant        |
      | canPublish   |
      | canSubscribe |
      | roomAdmin    |
      | hidden       |

  Scenario: Generate access token with expiration for Jack
    When the system creates an access token with identity "Jack" and room "TempRoom" with grants "canPublish:true,canSubscribe:true" that expires in 30 seconds
    Then the access token for "Jack" in room "TempRoom" should be valid
    And the access token for "Jack" in room "TempRoom" should have the following grants:
      | grant        | value |
      | canPublish   | true  |
      | canSubscribe | true  |
      | roomJoin     | true  |
    And the access token for "Jack" in room "TempRoom" should not have the following grants:
      | grant     |
      | roomAdmin |
      | agent     |
      | hidden    |

  Scenario: Generate access token with long expiration for Kate
    When the system creates an access token with identity "Kate" and room "LongTermRoom" with grants "roomAdmin:true" that expires in 60 minutes
    Then the access token for "Kate" in room "LongTermRoom" should be valid
    And the access token for "Kate" in room "LongTermRoom" should have the following grants:
      | grant     | value |
      | roomAdmin | true  |
      | roomJoin  | true  |
    And the access token for "Kate" in room "LongTermRoom" should not have the following grants:
      | grant        |
      | canPublish   |
      | canSubscribe |
      | agent        |
      | roomRecord   |

  Scenario: Generate access token with expiration and custom attributes for Leo
    When the system creates an access token with identity "Leo" and room "CustomExpRoom" with grants "canPublish:true,canSubscribe:true" and attributes "role=presenter,session=demo" that expires in 45 seconds
    Then the access token for "Leo" in room "CustomExpRoom" should be valid
    And the access token for "Leo" in room "CustomExpRoom" should have the following grants:
      | grant        | value |
      | canPublish   | true  |
      | canSubscribe | true  |
      | roomJoin     | true  |
    And the access token for "Leo" in room "CustomExpRoom" should not have the following grants:
      | grant     |
      | roomAdmin |
      | agent     |
      | hidden    |
    And the access token for "Leo" in room "CustomExpRoom" should have the following attributes:
      | attribute | value     |
      | role      | presenter |
      | session   | demo      |

  Scenario: Generate access token with minute-based expiration for Maya
    When the system creates an access token with identity "Maya" and room "MeetingRoom" with grants "roomRecord:true,canPublish:true" and attributes "department=marketing,level=manager" that expires in 2 minutes
    Then the access token for "Maya" in room "MeetingRoom" should be valid
    And the access token for "Maya" in room "MeetingRoom" should have the following grants:
      | grant      | value |
      | roomRecord | true  |
      | canPublish | true  |
      | roomJoin   | true  |
    And the access token for "Maya" in room "MeetingRoom" should not have the following grants:
      | grant        |
      | canSubscribe |
      | roomAdmin    |
      | agent        |
      | hidden       |
    And the access token for "Maya" in room "MeetingRoom" should have the following attributes:
      | attribute  | value     |
      | department | marketing |
      | level      | manager   |

  Scenario: Comprehensive access token validation for Nick
    When the system creates an access token with identity "Nick" and room "ComprehensiveRoom" with grants "canPublish:true,canSubscribe:true,roomAdmin:true" and attributes "role=admin,team=backend,clearance=high"
    Then the access token for "Nick" in room "ComprehensiveRoom" should be valid
    And the access token for "Nick" in room "ComprehensiveRoom" should have the following grants:
      | grant        | value |
      | canPublish   | true  |
      | canSubscribe | true  |
      | roomAdmin    | true  |
      | roomJoin     | true  |
    And the access token for "Nick" in room "ComprehensiveRoom" should not have the following grants:
      | grant      |
      | roomRecord |
      | hidden     |
      | recorder   |
    And the access token for "Nick" in room "ComprehensiveRoom" should have the following attributes:
      | attribute | value   |
      | role      | admin   |
      | team      | backend |
      | clearance | high    |
    And the access token for "Nick" in room "ComprehensiveRoom" should not have the following attributes:
      | attribute |
      | project   |
      | temp      |