package ro.stancalau.test.framework.docker;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import ro.stancalau.test.framework.util.PathUtils;

@Slf4j
public class CLIPublisherContainer extends GenericContainer<CLIPublisherContainer> {

  private static final String DEFAULT_CLI_ALIAS = "livekit-cli";
  private static final String LIVEKIT_CLI_IMAGE = "livekit/livekit-cli:latest";

  @Getter private final Network network;

  private String alias;
  private final String wsUrl;
  private final String apiKey;
  private final String apiSecret;
  private final String token;
  private final String roomName;
  private final PublisherConfig config;

  @Getter
  public static class PublisherConfig {
    private final PublisherType type;
    private final int videoPublishers;
    private final int audioPublishers;
    private final int subscribers;
    private final String videoResolution;
    private final boolean simulcast;
    private final Integer duration;
    private final Integer numPerSecond;
    private final String layout;
    private final boolean simulateSpeakers;

    private PublisherConfig(Builder builder) {
      this.type = builder.type;
      this.videoPublishers = builder.videoPublishers;
      this.audioPublishers = builder.audioPublishers;
      this.subscribers = builder.subscribers;
      this.videoResolution = builder.videoResolution;
      this.simulcast = builder.simulcast;
      this.duration = builder.duration;
      this.numPerSecond = builder.numPerSecond;
      this.layout = builder.layout;
      this.simulateSpeakers = builder.simulateSpeakers;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private PublisherType type = PublisherType.LOAD_TEST;
      private int videoPublishers = 0;
      private int audioPublishers = 0;
      private int subscribers = 0;
      private String videoResolution = "high";
      private boolean simulcast = true;
      private Integer duration;
      private Integer numPerSecond;
      private String layout;
      private boolean simulateSpeakers = false;

      public Builder type(PublisherType type) {
        this.type = type;
        return this;
      }

      public Builder videoPublishers(int count) {
        this.videoPublishers = count;
        return this;
      }

      public Builder audioPublishers(int count) {
        this.audioPublishers = count;
        return this;
      }

      public Builder subscribers(int count) {
        this.subscribers = count;
        return this;
      }

      public Builder videoResolution(String resolution) {
        this.videoResolution = resolution;
        return this;
      }

      public Builder simulcast(boolean enabled) {
        this.simulcast = enabled;
        return this;
      }

      public Builder duration(Integer seconds) {
        this.duration = seconds;
        return this;
      }

      public Builder numPerSecond(Integer num) {
        this.numPerSecond = num;
        return this;
      }

      public Builder layout(String layout) {
        this.layout = layout;
        return this;
      }

      public Builder simulateSpeakers(boolean enabled) {
        this.simulateSpeakers = enabled;
        return this;
      }

      public PublisherConfig build() {
        return new PublisherConfig(this);
      }
    }
  }

  public enum PublisherType {
    LOAD_TEST,
    JOIN
  }

  private CLIPublisherContainer(
      Network network,
      String wsUrl,
      String apiKey,
      String apiSecret,
      String token,
      String roomName,
      PublisherConfig config) {
    super(LIVEKIT_CLI_IMAGE);
    this.network = network;
    this.wsUrl = wsUrl;
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.token = token;
    this.roomName = roomName;
    this.config = config;
  }

  public static CLIPublisherContainer createLoadTestContainer(
      String alias,
      Network network,
      String wsUrl,
      String apiKey,
      String apiSecret,
      String roomName,
      PublisherConfig config,
      @Nullable String logDestinationPath) {

    String logDirPath =
        (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

    File logDirRoot = new File(logDirPath);
    logDirRoot.mkdirs();

    CLIPublisherContainer container =
        new CLIPublisherContainer(network, wsUrl, apiKey, apiSecret, null, roomName, config);

    String[] cmdArray = buildLoadTestCommandArray(wsUrl, apiKey, apiSecret, roomName, config);
    container = container.withCommand(cmdArray);

    container = ContainerLogUtils.withLogCapture(container, logDirRoot, "cli-publisher.log");

    container = container.withNetwork(network).withNetworkAliases(alias);

    // Don't wait for specific log messages, just ensure container starts
    container =
        container.waitingFor(
            Wait.forLogMessage(".*", 1).withStartupTimeout(Duration.ofSeconds(10)));

    container.alias = alias;

    return container;
  }

  public static CLIPublisherContainer createJoinContainer(
      String alias,
      Network network,
      String wsUrl,
      String apiKey,
      String apiSecret,
      String roomName,
      String identity,
      boolean publishVideo,
      boolean publishAudio,
      @Nullable String logDestinationPath) {

    String logDirPath =
        (logDestinationPath != null)
            ? logDestinationPath
            : PathUtils.containerLogPath(PathUtils.currentScenarioPath(), "docker", alias);

    File logDirRoot = new File(logDirPath);
    logDirRoot.mkdirs();

    PublisherConfig config = PublisherConfig.builder().type(PublisherType.JOIN).build();

    CLIPublisherContainer container =
        new CLIPublisherContainer(network, wsUrl, apiKey, apiSecret, null, roomName, config);

    String[] cmdArray =
        buildJoinCommandArray(
            wsUrl, apiKey, apiSecret, roomName, identity, publishVideo, publishAudio);
    container = container.withCommand(cmdArray);

    container = ContainerLogUtils.withLogCapture(container, logDirRoot, "cli-publisher.log");

    container =
        container
            .withNetwork(network)
            .withNetworkAliases(alias)
            .waitingFor(Wait.forLogMessage(".*", 1).withStartupTimeout(Duration.ofSeconds(10)));

    container.alias = alias;

    return container;
  }

  private static String[] buildLoadTestCommandArray(
      String wsUrl, String apiKey, String apiSecret, String roomName, PublisherConfig config) {
    List<String> command = buildLoadTestCommand(wsUrl, apiKey, apiSecret, roomName, config);
    log.info("CLI Load Test Command: {}", String.join(" ", command));
    return command.toArray(new String[0]);
  }

  private static List<String> buildLoadTestCommand(
      String wsUrl, String apiKey, String apiSecret, String roomName, PublisherConfig config) {
    List<String> command = new ArrayList<>();
    command.add("perf");
    command.add("load-test");
    command.add("--url");
    command.add(wsUrl);
    command.add("--api-key");
    command.add(apiKey);
    command.add("--api-secret");
    command.add(apiSecret);
    command.add("--room");
    command.add(roomName);

    if (config.videoPublishers > 0) {
      command.add("--video-publishers");
      command.add(String.valueOf(config.videoPublishers));
    }

    if (config.audioPublishers > 0) {
      command.add("--audio-publishers");
      command.add(String.valueOf(config.audioPublishers));
    }

    if (config.subscribers > 0) {
      command.add("--subscribers");
      command.add(String.valueOf(config.subscribers));
    }

    if (config.videoResolution != null && !config.videoResolution.equals("high")) {
      command.add("--video-resolution");
      command.add(config.videoResolution);
    }

    if (!config.simulcast) {
      command.add("--no-simulcast");
    }

    if (config.duration != null) {
      command.add("--duration");
      command.add(config.duration + "s");
    }

    if (config.numPerSecond != null) {
      command.add("--num-per-second");
      command.add(String.valueOf(config.numPerSecond));
    }

    if (config.layout != null) {
      command.add("--layout");
      command.add(config.layout);
    }

    if (config.simulateSpeakers) {
      command.add("--simulate-speakers");
    }

    command.add("--verbose");

    return command;
  }

  private static String[] buildJoinCommandArray(
      String wsUrl,
      String apiKey,
      String apiSecret,
      String roomName,
      String identity,
      boolean publishVideo,
      boolean publishAudio) {
    List<String> command =
        buildJoinCommand(wsUrl, apiKey, apiSecret, roomName, identity, publishVideo, publishAudio);
    log.info("CLI Join Command: {}", String.join(" ", command));
    return command.toArray(new String[0]);
  }

  private static List<String> buildJoinCommand(
      String wsUrl,
      String apiKey,
      String apiSecret,
      String roomName,
      String identity,
      boolean publishVideo,
      boolean publishAudio) {
    List<String> command = new ArrayList<>();
    command.add("room");
    command.add("join");
    command.add("--url");
    command.add(wsUrl);
    command.add("--api-key");
    command.add(apiKey);
    command.add("--api-secret");
    command.add(apiSecret);
    command.add("--identity");
    command.add(identity);

    // --publish-demo publishes both audio and video
    // For now, we'll use it for any publishing scenario
    if (publishVideo || publishAudio) {
      command.add("--publish-demo");
    }

    command.add("--verbose");
    command.add(roomName); // Room name is positional argument

    return command;
  }

  public String getAlias() {
    return alias != null ? alias : getNetworkAliases().getFirst();
  }

  public String getRoomName() {
    return roomName;
  }

  public PublisherConfig getPublisherConfig() {
    return config;
  }
}
