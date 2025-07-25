package ro.stancalau.test.bdd.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.*;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.bdd.state.AccessTokenStateManager;
import ro.stancalau.test.framework.util.StringParsingUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LiveKitAccessTokenSteps {

    private AccessTokenStateManager stateManager;
    private String apiKey = "devkey";
    private String apiSecret = "secret";

    @Before
    public void setUp() {
        log.info("Setting up access token state manager");
        stateManager = new AccessTokenStateManager(apiKey, apiSecret);
    }

    @Given("LiveKit server credentials are available")
    public void liveKitServerCredentialsAreAvailable() {
        log.info("Setting up LiveKit server credentials");
        assertNotNull(apiKey, "API key should be configured");
        assertNotNull(apiSecret, "API secret should be configured");
    }

    @When("an access token is created with identity {string} and room {string}")
    public void anAccessTokenIsCreatedWithIdentityAndRoom(String identity, String roomName) {
        AccessToken token = stateManager.createTokenWithRoom(identity, roomName);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with publish permissions")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithPublishPermissions(String identity, String roomName) {
        AccessToken token = stateManager.createTokenWithRoomAndPermissions(identity, roomName, true, false);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with subscribe permissions")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithSubscribePermissions(String identity, String roomName) {
        AccessToken token = stateManager.createTokenWithRoomAndPermissions(identity, roomName, false, true);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with publish and subscribe permissions")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithPublishAndSubscribePermissions(String identity, String roomName) {
        AccessToken token = stateManager.createTokenWithRoomAndPermissions(identity, roomName, true, true);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} that expires in {int} seconds")
    public void anAccessTokenIsCreatedWithIdentityThatExpiresInSeconds(String identity, String roomName, int seconds) {
        AccessToken token = stateManager.createTokenWithExpiration(identity, seconds * 1000L);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string}")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrants(String identity, String roomName, String grantsString) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, null);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string} and attributes {string}")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrantsAndAttributes(String identity, String roomName, String grantsString, String attributesString) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, attributes);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string} that expires in {int} seconds")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrantsThatExpiresInSeconds(String identity, String roomName, String grantsString, int seconds) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        long ttlMillis = seconds * 1000L;
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, null, ttlMillis);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} seconds")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrantsAndAttributesThatExpiresInSeconds(String identity, String roomName, String grantsString, String attributesString, int seconds) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
        long ttlMillis = seconds * 1000L;
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, attributes, ttlMillis);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string} that expires in {int} minutes")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrantsThatExpiresInMinutes(String identity, String roomName, String grantsString, int minutes) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        long ttlMillis = minutes * 60 * 1000L;
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, null, ttlMillis);
        assertNotNull(token, "Access token should be created");
    }

    @When("an access token is created with identity {string} and room {string} with grants {string} and attributes {string} that expires in {int} minutes")
    public void anAccessTokenIsCreatedWithIdentityAndRoomWithGrantsAndAttributesThatExpiresInMinutes(String identity, String roomName, String grantsString, String attributesString, int minutes) {
        List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsString);
        Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesString);
        long ttlMillis = minutes * 60 * 1000L;
        AccessToken token = stateManager.createTokenWithDynamicGrants(identity, roomName, grants, attributes, ttlMillis);
        assertNotNull(token, "Access token should be created");
    }

    @When("waiting for {int} seconds")
    public void waitingForSeconds(int seconds) throws InterruptedException {
        log.info("Waiting for {} seconds", seconds);
        Thread.sleep(seconds * 1000L);
    }


    @Then("the access token for {string} in room {string} should be valid")
    public void theAccessTokenForInRoomShouldBeValid(String identity, String roomName) {
        AccessToken token = stateManager.getLastToken(identity, roomName);
        assertNotNull(token, "Access token for " + identity + " in room " + roomName + " should not be null");
        
        String tokenString = token.toJwt();
        assertNotNull(tokenString, "Token string should not be null");
        assertFalse(tokenString.isEmpty(), "Token string should not be empty");
        
        log.info("Generated access token for {} in room {}: {}", identity, roomName, tokenString.substring(0, Math.min(50, tokenString.length())) + "...");
    }

    @Then("the access token for {string} should contain room {string}")
    public void theAccessTokenForShouldContainRoom(String identity, String expectedRoom) {
        log.info("Verifying access token for {} contains room: {}", identity, expectedRoom);
        assertTrue(stateManager.hasTokenForRoom(expectedRoom), "Token for room " + expectedRoom + " should exist");
    }


}