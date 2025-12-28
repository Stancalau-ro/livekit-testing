package ro.stancalau.test.framework.state;

import io.livekit.server.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ro.stancalau.test.framework.docker.LiveKitContainer;

@Slf4j
public class AccessTokenStateManager {

  private final String apiKey;
  private final String apiSecret;
  private final Map<String, Map<String, AccessToken>> tokens = new HashMap<>();

  public AccessTokenStateManager() {
    this.apiKey = LiveKitContainer.API_KEY;
    this.apiSecret = LiveKitContainer.SECRET;
  }

  public AccessTokenStateManager(String apiKey, String apiSecret) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  public AccessToken createTokenWithRoom(String identity, String roomName) {
    return createTokenWithRoomAndPermissions(identity, roomName, false, false);
  }

  public AccessToken createTokenWithRoomAndPermissions(
      String identity, String roomName, boolean canPublish, boolean canSubscribe) {
    log.info(
        "Creating access token for user: {} in room: {} with publish: {}, subscribe: {}",
        identity,
        roomName,
        canPublish,
        canSubscribe);

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

  public AccessToken createTokenWithDynamicGrants(
      String identity,
      String roomName,
      List<String> grantStrings,
      Map<String, String> customAttributes) {
    return createTokenWithDynamicGrants(
        identity, roomName, grantStrings, customAttributes, (String) null, (Long) null);
  }

  public AccessToken createTokenWithDynamicGrants(
      String identity,
      String roomName,
      List<String> grantStrings,
      Map<String, String> customAttributes,
      Long ttlMillis) {
    return createTokenWithDynamicGrants(
        identity, roomName, grantStrings, customAttributes, (String) null, ttlMillis);
  }

  public AccessToken createTokenWithDynamicGrants(
      String identity,
      String roomName,
      List<String> grantStrings,
      Map<String, String> customAttributes,
      String metadata) {
    return createTokenWithDynamicGrants(
        identity, roomName, grantStrings, customAttributes, metadata, (Long) null);
  }

  public AccessToken createTokenWithDynamicGrants(
      String identity,
      String roomName,
      List<String> grantStrings,
      Map<String, String> customAttributes,
      String metadata,
      Long ttlMillis) {
    log.info(
        "Creating access token for user: {} in room: {} with grants: {}, attributes: {}, metadata: {}, and TTL: {} ms",
        identity,
        roomName,
        grantStrings,
        customAttributes,
        metadata != null
            ? (metadata.length() > 50 ? metadata.substring(0, 50) + "..." : metadata)
            : null,
        ttlMillis);

    AccessToken token = new AccessToken(apiKey, apiSecret);
    token.setIdentity(identity);

    if (ttlMillis != null) {
      token.setTtl(ttlMillis);
    }

    if (metadata != null && !metadata.isEmpty()) {
      token.setMetadata(metadata);
    }

    if (customAttributes != null && !customAttributes.isEmpty()) {
      for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
        token.getAttributes().put(entry.getKey(), entry.getValue());
      }
    }

    List<VideoGrant> grants = new ArrayList<>();
    grants.add(new RoomName(roomName));
    grants.add(new RoomJoin(true));

    grants.addAll(parseGrants(grantStrings, roomName));

    if (!grants.isEmpty()) {
      token.addGrants(grants.toArray(new VideoGrant[0]));
    }

    tokens.computeIfAbsent(identity, k -> new HashMap<>()).put(roomName, token);
    return token;
  }

  private List<VideoGrant> parseGrants(List<String> grantStrings, String roomName) {
    List<VideoGrant> grants = new ArrayList<>();
    List<String> publishSources = new ArrayList<>();

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
        case "canpublishsources":
          try {
            String[] sources = value.split(",");
            for (String source : sources) {
              String sourceType = source.trim().toLowerCase();
              switch (sourceType) {
                case "camera":
                case "microphone":
                case "screen_share":
                case "screen_share_audio":
                  publishSources.add(sourceType);
                  break;
                default:
                  log.warn("Unknown track source type: {}", sourceType);
                  break;
              }
            }
          } catch (Exception e) {
            log.error("Failed to parse canPublishSources grant with value: {}", value, e);
          }
          break;
        default:
          log.warn("Unknown grant type: {}", grantType);
          break;
      }
    }

    if (!publishSources.isEmpty()) {
      grants.add(new CanPublishSources(publishSources));
    }

    return grants;
  }

  public AccessToken createTokenWithExpiration(String identity, long ttlMillis) {
    log.info(
        "Creating access token for user: {} with expiration in {} second(s)", identity, ttlMillis);
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
