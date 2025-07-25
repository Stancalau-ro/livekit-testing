package ro.stancalau.test.bdd.state;

import io.livekit.server.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AccessTokenStateManager {
    
    private final String apiKey;
    private final String apiSecret;
    private final Map<String, Map<String, AccessToken>> tokens = new HashMap<>();
    
    public AccessTokenStateManager(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public AccessToken createToken(String identity) {
        log.info("Creating access token for user: {}", identity);
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
        tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(null, token);
        return token;
    }
    
    public AccessToken createTokenWithRoom(String identity, String roomName) {
        return createTokenWithRoomAndPermissions(identity, roomName, false, false);
    }
    
    public AccessToken createTokenWithRoomAndPermissions(String identity, String roomName, boolean canPublish, boolean canSubscribe) {
        log.info("Creating access token for user: {} in room: {} with publish: {}, subscribe: {}", 
                identity, roomName, canPublish, canSubscribe);
        
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
        List<VideoGrant> grants = new ArrayList<>();
        grants.add(new RoomJoin(true));
        grants.add(new RoomName(roomName));
        
        if (canPublish) {
            grants.add(new CanPublish(true));
        }
        
        if (canSubscribe) {
            grants.add(new CanSubscribe(true));
        }
        
        token.addGrants(grants.toArray(new VideoGrant[0]));
        
        tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(roomName, token);
        return token;
    }
    
    public AccessToken createTokenWithExpiration(String identity, int hours) {
        log.info("Creating access token for user: {} with expiration in {} hour(s)", identity, hours);
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
        long ttlMillis = Duration.ofHours(hours).toMillis();
        token.setTtl(ttlMillis);
        
        tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(null, token);
        return token;
    }
    
    public AccessToken createTokenWithExpirationSeconds(String identity, int seconds) {
        log.info("Creating access token for user: {} with expiration in {} second(s)", identity, seconds);
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
        long ttlMillis = Duration.ofSeconds(seconds).toMillis();
        token.setTtl(ttlMillis);
        
        tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(null, token);
        return token;
    }
    
    public AccessToken getLastToken(String identity, String roomName) {
        Map<String, AccessToken> userTokens = tokens.get(identity);
        if (userTokens == null) {
            return null;
        }
        return userTokens.get(roomName);
    }
    
    public AccessToken getLastToken(String identity) {
        Map<String, AccessToken> userTokens = tokens.get(identity);
        if (userTokens == null) {
            return null;
        }
        // Return first token if only one exists, otherwise null for ambiguity
        if (userTokens.size() == 1) {
            return userTokens.values().iterator().next();
        }
        return null;
    }
    
    public boolean hasToken(String identity, String roomName) {
        return getLastToken(identity, roomName) != null;
    }
    
    public boolean hasToken(String identity) {
        return tokens.containsKey(identity) && !tokens.get(identity).isEmpty();
    }
    
    public void clearAll() {
        log.info("Clearing all access tokens and state");
        tokens.clear();
    }
    
    public void clearUser(String identity) {
        log.info("Clearing access token and state for user: {}", identity);
        tokens.remove(identity);
    }
    
    public boolean hasTokenForRoom(String roomName) {
        for (Map<String, AccessToken> userTokens : tokens.values()) {
            if (userTokens.containsKey(roomName)) {
                return true;
            }
        }
        return false;
    }
}