package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.AccessToken;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.util.StringParsingUtils;

@Slf4j
public class LiveKitAccessTokenSteps {

  @When("the system creates an access token with identity {string} and room {string}")
  @Given("an access token is created with identity {string} and room {string}")
  public void theSystemCreatesAccessTokenWithIdentityAndRoom(String identity, String roomName) {
    AccessToken token = ManagerProvider.tokens().createTokenWithRoom(identity, roomName);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with publish permissions")
  @Given(
      "an access token is created with identity {string} and room {string} with publish permissions")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithPublishPermissions(
      String identity, String roomName) {
    AccessToken token =
        ManagerProvider.tokens().createTokenWithRoomAndPermissions(identity, roomName, true, false);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with subscribe permissions")
  @Given(
      "an access token is created with identity {string} and room {string} with subscribe permissions")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithSubscribePermissions(
      String identity, String roomName) {
    AccessToken token =
        ManagerProvider.tokens().createTokenWithRoomAndPermissions(identity, roomName, false, true);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with publish and subscribe permissions")
  @Given(
      "an access token is created with identity {string} and room {string} with publish and subscribe permissions")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithPublishAndSubscribePermissions(
      String identity, String roomName) {
    AccessToken token =
        ManagerProvider.tokens().createTokenWithRoomAndPermissions(identity, roomName, true, true);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} that expires in {int} seconds")
  @Given(
      "an access token is created with identity {string} and room {string} that expires in {int} seconds")
  public void theSystemCreatesAccessTokenWithIdentityThatExpiresInSeconds(
      String identity, String roomName, int seconds) {
    AccessToken token =
        ManagerProvider.tokens().createTokenWithExpiration(identity, seconds * 1000L);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string}")
  @Given("an access token is created with identity {string} and room {string} with grants {string}")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrants(
      String identity, String roomName, String grantsString) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    AccessToken token =
        ManagerProvider.tokens().createTokenWithDynamicGrants(identity, roomName, grants, null);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} and attributes {string}")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} and attributes {string}")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsAndAttributes(
      String identity, String roomName, String grantsString, String attributesString) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, attributes);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} that expires in {int} seconds")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} that expires in {int} seconds")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsThatExpiresInSeconds(
      String identity, String roomName, String grantsString, int seconds) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    long ttlMillis = seconds * 1000L;
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, null, ttlMillis);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} seconds")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} seconds")
  public void
      theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsAndAttributesThatExpiresInSeconds(
          String identity,
          String roomName,
          String grantsString,
          String attributesString,
          int seconds) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
    long ttlMillis = seconds * 1000L;
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, attributes, ttlMillis);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} that expires in {int} minutes")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} that expires in {int} minutes")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsThatExpiresInMinutes(
      String identity, String roomName, String grantsString, int minutes) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    long ttlMillis = minutes * 60 * 1000L;
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, null, ttlMillis);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} minutes")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} minutes")
  public void
      theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsAndAttributesThatExpiresInMinutes(
          String identity,
          String roomName,
          String grantsString,
          String attributesString,
          int minutes) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
    long ttlMillis = minutes * 60 * 1000L;
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, attributes, ttlMillis);
    assertNotNull(token, "Access token should be created");
  }

  @When("waiting for {int} seconds")
  public void waitingForSeconds(int seconds) throws InterruptedException {
    log.info("Waiting for {} seconds", seconds);
    Thread.sleep(seconds * 1000L);
  }

  @Then("the access token for {string} in room {string} should be valid")
  public void theAccessTokenForInRoomShouldBeValid(String identity, String roomName) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String tokenString = token.toJwt();
    assertNotNull(tokenString, "Token string should not be null");
    assertFalse(tokenString.isEmpty(), "Token string should not be empty");

    log.info("Generated access token for {} in room {}: {}", identity, roomName, tokenString);
  }

  @Then("the access token for {string} should contain room {string}")
  public void theAccessTokenForShouldContainRoom(String identity, String expectedRoom) {
    log.info("Verifying access token for {} contains room: {}", identity, expectedRoom);
    assertTrue(
        ManagerProvider.tokens().hasTokenForRoom(expectedRoom),
        "Token for room " + expectedRoom + " should exist");
  }

  @Then("the access token for {string} in room {string} should have grant {string} set to {string}")
  public void theAccessTokenForInRoomShouldHaveGrantSetTo(
      String identity, String roomName, String grantName, String expectedValue) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim videoClaim = decodedJWT.getClaim("video");
    assertNotNull(videoClaim, "Token should contain video claims");

    Map<String, Object> videoGrants = videoClaim.asMap();
    assertNotNull(videoGrants, "Video grants should be present");

    String actualValue = getGrantValue(videoGrants, grantName);
    String normalizedExpected = normalizeExpectedValue(expectedValue);
    assertEquals(
        normalizedExpected,
        actualValue,
        "Grant " + grantName + " should be set to " + normalizedExpected);

    log.info(
        "Verified grant {} is set to {} for {} in room {}",
        grantName,
        normalizedExpected,
        identity,
        roomName);
  }

  @Then("the access token for {string} in room {string} should not have grant {string}")
  public void theAccessTokenForInRoomShouldNotHaveGrant(
      String identity, String roomName, String grantName) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim videoClaim = decodedJWT.getClaim("video");
    if (videoClaim != null && !videoClaim.isNull()) {
      Map<String, Object> videoGrants = videoClaim.asMap();
      if (videoGrants != null) {
        assertFalse(
            videoGrants.containsKey(grantName)
                || (videoGrants.containsKey(grantName)
                    && isGrantValueFalsy(videoGrants.get(grantName))),
            "Grant " + grantName + " should not be present or should be false");
      }
    }

    log.info("Verified grant {} is not set for {} in room {}", grantName, identity, roomName);
  }

  @Then(
      "the access token for {string} in room {string} should have attribute {string} set to {string}")
  public void theAccessTokenForInRoomShouldHaveAttributeSetTo(
      String identity, String roomName, String attributeName, String expectedValue) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Map<String, Object> attributes = getTokenAttributes(decodedJWT);
    assertTrue(
        attributes.containsKey(attributeName), "Token should contain attribute " + attributeName);

    String actualValue = attributes.get(attributeName).toString();
    assertEquals(
        expectedValue,
        actualValue,
        "Attribute " + attributeName + " should be set to " + expectedValue);

    log.info(
        "Verified attribute {} is set to {} for {} in room {}",
        attributeName,
        expectedValue,
        identity,
        roomName);
  }

  @Then("the access token for {string} in room {string} should not have attribute {string}")
  public void theAccessTokenForInRoomShouldNotHaveAttribute(
      String identity, String roomName, String attributeName) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Map<String, Object> attributes = getTokenAttributes(decodedJWT);
    assertFalse(
        attributes.containsKey(attributeName),
        "Token should not contain attribute " + attributeName);

    log.info(
        "Verified attribute {} is not set for {} in room {}", attributeName, identity, roomName);
  }

  private String getGrantValue(Map<String, Object> videoGrants, String grantName) {
    Object value = videoGrants.get(grantName);
    if (value == null) {
      return null;
    }
    if (value instanceof List<?> list) {
      return normalizeListValue(list.stream().map(Object::toString).toList());
    }
    return value.toString();
  }

  private String normalizeListValue(List<String> items) {
    return items.stream().sorted().toList().toString();
  }

  private String normalizeExpectedValue(String expected) {
    if (expected != null && expected.startsWith("[") && expected.endsWith("]")) {
      String inner = expected.substring(1, expected.length() - 1);
      List<String> items = Arrays.stream(inner.split(",")).map(String::trim).toList();
      return normalizeListValue(items);
    }
    return expected;
  }

  private boolean isGrantValueFalsy(Object value) {
    return switch (value) {
      case null -> true;
      case Boolean b -> !b;
      case String str -> str.isEmpty() || "false".equalsIgnoreCase(str);
      default -> false;
    };
  }

  private Map<String, Object> getTokenAttributes(DecodedJWT decodedJWT) {
    Claim attributesClaim = decodedJWT.getClaim("attributes");

    if (attributesClaim != null && !attributesClaim.isNull()) {
      Map<String, Object> attributesMap = attributesClaim.asMap();
      return attributesMap != null ? attributesMap : new HashMap<>();
    }

    return new HashMap<>();
  }

  @Then("the access token for {string} in room {string} should have the following grants:")
  public void theAccessTokenForInRoomShouldHaveTheFollowingGrants(
      String identity, String roomName, DataTable dataTable) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim videoClaim = decodedJWT.getClaim("video");
    assertNotNull(videoClaim, "Token should contain video claims");

    Map<String, Object> videoGrants = videoClaim.asMap();
    assertNotNull(videoGrants, "Video grants should be present");

    List<Map<String, String>> grants = dataTable.asMaps(String.class, String.class);
    for (Map<String, String> grantRow : grants) {
      String grantName = grantRow.get("grant");
      String expectedValue = normalizeExpectedValue(grantRow.get("value"));

      String actualValue = getGrantValue(videoGrants, grantName);
      assertEquals(
          expectedValue, actualValue, "Grant " + grantName + " should be set to " + expectedValue);

      log.info(
          "Verified grant {} is set to {} for {} in room {}",
          grantName,
          expectedValue,
          identity,
          roomName);
    }
  }

  @Then("the access token for {string} in room {string} should not have the following grants:")
  public void theAccessTokenForInRoomShouldNotHaveTheFollowingGrants(
      String identity, String roomName, DataTable dataTable) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim videoClaim = decodedJWT.getClaim("video");
    if (videoClaim != null && !videoClaim.isNull()) {
      Map<String, Object> videoGrants = videoClaim.asMap();
      if (videoGrants != null) {
        List<List<String>> grants = dataTable.asLists(String.class);
        for (int i = 1; i < grants.size(); i++) {
          String grantName = grants.get(i).getFirst();

          assertFalse(
              videoGrants.containsKey(grantName)
                  || (videoGrants.containsKey(grantName)
                      && isGrantValueFalsy(videoGrants.get(grantName))),
              "Grant " + grantName + " should not be present or should be false");

          log.info("Verified grant {} is not set for {} in room {}", grantName, identity, roomName);
        }
      }
    }
  }

  @Then("the access token for {string} in room {string} should have the following attributes:")
  public void theAccessTokenForInRoomShouldHaveTheFollowingAttributes(
      String identity, String roomName, DataTable dataTable) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Map<String, Object> attributes = getTokenAttributes(decodedJWT);

    List<Map<String, String>> attributeRows = dataTable.asMaps(String.class, String.class);
    for (Map<String, String> attributeRow : attributeRows) {
      String attributeName = attributeRow.get("attribute");
      String expectedValue = attributeRow.get("value");

      assertTrue(
          attributes.containsKey(attributeName), "Token should contain attribute " + attributeName);

      String actualValue = attributes.get(attributeName).toString();
      assertEquals(
          expectedValue,
          actualValue,
          "Attribute " + attributeName + " should be set to " + expectedValue);

      log.info(
          "Verified attribute {} is set to {} for {} in room {}",
          attributeName,
          expectedValue,
          identity,
          roomName);
    }
  }

  @Then("the access token for {string} in room {string} should not have the following attributes:")
  public void theAccessTokenForInRoomShouldNotHaveTheFollowingAttributes(
      String identity, String roomName, DataTable dataTable) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Map<String, Object> attributes = getTokenAttributes(decodedJWT);

    List<List<String>> attributeNames = dataTable.asLists(String.class);
    for (int i = 1; i < attributeNames.size(); i++) {
      String attributeName = attributeNames.get(i).getFirst();

      assertFalse(
          attributes.containsKey(attributeName),
          "Token should not contain attribute " + attributeName);

      log.info(
          "Verified attribute {} is not set for {} in room {}", attributeName, identity, roomName);
    }
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} and metadata {string}")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} and metadata {string}")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsAndMetadata(
      String identity, String roomName, String grantsString, String metadata) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, null, metadata);
    assertNotNull(token, "Access token should be created");
  }

  @When(
      "the system creates an access token with identity {string} and room {string} with grants {string} and metadata of size {int} bytes")
  @Given(
      "an access token is created with identity {string} and room {string} with grants {string} and metadata of size {int} bytes")
  public void theSystemCreatesAccessTokenWithIdentityAndRoomWithGrantsAndMetadataOfSize(
      String identity, String roomName, String grantsString, int sizeBytes) {
    List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
    String metadata = ManagerProvider.metadata().generateStringOfSize(sizeBytes);
    AccessToken token =
        ManagerProvider.tokens()
            .createTokenWithDynamicGrants(identity, roomName, grants, null, metadata);
    assertNotNull(token, "Access token should be created");
  }

  @Then("the access token for {string} in room {string} should have metadata {string}")
  public void theAccessTokenForInRoomShouldHaveMetadata(
      String identity, String roomName, String expectedMetadata) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim metadataClaim = decodedJWT.getClaim("metadata");
    assertNotNull(metadataClaim, "Token should contain metadata claim");
    assertFalse(metadataClaim.isNull(), "Token metadata claim should not be null");

    String actualMetadata = metadataClaim.asString();
    assertEquals(expectedMetadata, actualMetadata, "Token metadata should match expected value");

    log.info(
        "Verified metadata is set to {} for {} in room {}", expectedMetadata, identity, roomName);
  }

  @Then("the access token for {string} in room {string} should have metadata of length {int}")
  public void theAccessTokenForInRoomShouldHaveMetadataOfLength(
      String identity, String roomName, int expectedLength) {
    AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
    assertNotNull(
        token, "Access token for " + identity + " in room " + roomName + " should not be null");

    String jwtToken = token.toJwt();
    DecodedJWT decodedJWT = JWT.decode(jwtToken);

    Claim metadataClaim = decodedJWT.getClaim("metadata");
    assertNotNull(metadataClaim, "Token should contain metadata claim");

    String actualMetadata = metadataClaim.asString();
    assertNotNull(actualMetadata, "Token metadata should not be null");
    assertEquals(
        expectedLength,
        actualMetadata.length(),
        "Token metadata length should match expected value");

    log.info(
        "Verified metadata length is {} for {} in room {}", expectedLength, identity, roomName);
  }
}
