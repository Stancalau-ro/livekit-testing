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
    
    public AccessToken createTokenWithDynamicGrants(String identity, String roomName, List<String> grantStrings, Map<String, String> customAttributes) {
        return createTokenWithDynamicGrants(identity, roomName, grantStrings, customAttributes, null);
    }
    
    public AccessToken createTokenWithDynamicGrants(String identity, String roomName, List<String> grantStrings, Map<String, String> customAttributes, Long ttlMillis) {
        log.info("Creating access token for user: {} in room: {} with grants: {}, attributes: {}, and TTL: {} ms", 
                identity, roomName, grantStrings, customAttributes, ttlMillis);
        
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
        // Set expiration if provided
        if (ttlMillis != null) {
            token.setTtl(ttlMillis);
        }
        
        // Add custom attributes
        if (customAttributes != null && !customAttributes.isEmpty()) {
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                token.getAttributes().put(entry.getKey(), entry.getValue());
            }
        }
        
        // Always add RoomName and RoomJoin for room-based tokens
        List<VideoGrant> grants = new ArrayList<>();
        grants.add(new RoomName(roomName));
        grants.add(new RoomJoin(true));
        
        // Parse and add additional grants dynamically
        grants.addAll(parseGrants(grantStrings, roomName));
        
        if (!grants.isEmpty()) {
            token.addGrants(grants.toArray(new VideoGrant[0]));
        }
        
        tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(roomName, token);
        return token;
    }
    
    private List<VideoGrant> parseGrants(List<String> grantStrings, String roomName) {
        List<VideoGrant> grants = new ArrayList<>();
        
        for (String grantString : grantStrings) {
            String[] parts = grantString.trim().split(":");
            String grantType = parts[0].toLowerCase();
            String value = parts.length > 1 ? parts[1] : "true";
            
            switch (grantType) {
                case "roomjoin":
                    grants.add(new RoomJoin(Boolean.parseBoolean(value)));
                    break;
                case "canpublish":
                    grants.add(new CanPublish(Boolean.parseBoolean(value)));
                    break;
                case "cansubscribe":
                    grants.add(new CanSubscribe(Boolean.parseBoolean(value)));
                    break;
                case "canpublishdata":
                    grants.add(new CanPublishData(Boolean.parseBoolean(value)));
                    break;
                case "canupdateownmetadata":
                    grants.add(new CanUpdateOwnMetadata(Boolean.parseBoolean(value)));
                    break;
                case "roomcreate":
                    grants.add(new RoomCreate(Boolean.parseBoolean(value)));
                    break;
                case "roomlist":
                    grants.add(new RoomList(Boolean.parseBoolean(value)));
                    break;
                case "roomrecord":
                    grants.add(new RoomRecord(Boolean.parseBoolean(value)));
                    break;
                case "roomadmin":
                    grants.add(new RoomAdmin(Boolean.parseBoolean(value)));
                    break;
                case "hidden":
                    grants.add(new Hidden(Boolean.parseBoolean(value)));
                    break;
                case "recorder":
                    grants.add(new Recorder(Boolean.parseBoolean(value)));
                    break;
                case "ingressadmin":
                    grants.add(new IngressAdmin(Boolean.parseBoolean(value)));
                    break;
                case "agent":
                    grants.add(new Agent(Boolean.parseBoolean(value)));
                    break;
                default:
                    log.warn("Unknown grant type: {}", grantType);
                    break;
            }
        }
        
        return grants;
    }

    public AccessToken createTokenWithExpiration(String identity, long ttlMillis) {
        log.info("Creating access token for user: {} with expiration in {} second(s)", identity, ttlMillis);
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        
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

    public boolean hasToken(String identity, String roomName) {
        return getLastToken(identity, roomName) != null;
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