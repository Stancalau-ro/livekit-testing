package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.livekit.server.IngressServiceClient;
import java.util.List;
import java.util.concurrent.TimeUnit;
import livekit.LivekitIngress;
import livekit.LivekitIngress.IngressInfo;
import livekit.LivekitIngress.IngressInput;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.bdd.steps.params.IngressState;
import ro.stancalau.test.framework.config.TestConfig;
import ro.stancalau.test.framework.docker.FFmpegContainer;
import ro.stancalau.test.framework.docker.IngressContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.docker.RedisContainer;
import ro.stancalau.test.framework.state.ContainerStateManager;
import ro.stancalau.test.framework.state.IngressStateManager;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;

@Slf4j
public class LiveKitIngressSteps {

  private static final int DEFAULT_STREAM_DURATION_SECONDS = 30;
  private static final int STATE_POLL_INTERVAL_MS = 1000;

  private String currentScenarioLogPath;
  private boolean streamFailed = false;

  @Before
  public void setUpLiveKitIngressSteps(Scenario scenario) {
    String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
    String scenarioName = scenario.getName();
    String timestamp = DateUtils.generateScenarioTimestamp();

    String sanitizedFeatureName = FileUtils.sanitizeFileName(featureName);
    String sanitizedScenarioName = FileUtils.sanitizeFileName(scenarioName);

    currentScenarioLogPath =
        PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
    streamFailed = false;
  }

  @After
  public void tearDownLiveKitIngressSteps() {
    cleanupIngresses();
    cleanupFfmpegContainers();
  }

  @Given("an Ingress service {string} is running connected to {string} and {string}")
  public void ingressServiceRunning(
      String serviceName, String livekitServiceName, String redisServiceName) {
    ContainerStateManager containerManager = ManagerProvider.containers();
    LiveKitContainer livekit =
        containerManager.getContainer(livekitServiceName, LiveKitContainer.class);
    RedisContainer redis = containerManager.getContainer(redisServiceName, RedisContainer.class);
    Network network = containerManager.getOrCreateNetwork();

    String serviceLogPath =
        PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", serviceName);

    IngressContainer ingress =
        IngressContainer.createContainer(
            serviceName,
            network,
            TestConfig.getIngressVersion(),
            livekit.getNetworkUrl(),
            LiveKitContainer.API_KEY,
            LiveKitContainer.SECRET,
            redis.getNetworkRedisUrl(),
            serviceLogPath);

    ingress.start();
    assertTrue(
        ingress.isRunning(),
        "Ingress container with service name " + serviceName + " should be running");

    containerManager.registerContainer(serviceName, ingress);
    log.info("Ingress service {} started with RTMP URL: {}", serviceName, ingress.getRtmpUrl());

    try {
      TimeUnit.SECONDS.sleep(5);
      log.debug("Waited 5 seconds for ingress CPU to stabilize");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @When("{string} creates an RTMP ingress to room {string} on {string}")
  public void createRtmpIngress(String identity, String roomName, String serviceName)
      throws Exception {
    createRtmpIngressWithDisplayName(identity, roomName, serviceName, identity);
  }

  @When("{string} creates an RTMP ingress to room {string} on {string} with display name {string}")
  public void createRtmpIngressWithDisplayName(
      String identity, String roomName, String serviceName, String displayName) throws Exception {
    IngressServiceClient client = getIngressClient(serviceName);

    IngressInfo info =
        client
            .createIngress(
                identity,
                roomName,
                identity,
                displayName,
                IngressInput.RTMP_INPUT,
                null,
                null,
                null,
                null,
                null)
            .execute()
            .body();

    assertNotNull(info, "Ingress creation should return IngressInfo");
    ManagerProvider.ingress().registerIngress(identity, info);

    log.info(
        "Created RTMP ingress for {} in room {} on {} with URL: {} and stream key: {}",
        identity,
        roomName,
        serviceName,
        info.getUrl(),
        info.getStreamKey());
  }

  @When("{string} creates an RTMP ingress to room {string} on {string} with video preset {string}")
  public void createRtmpIngressWithPreset(
      String identity, String roomName, String serviceName, String preset) throws Exception {
    IngressServiceClient client = getIngressClient(serviceName);

    LivekitIngress.IngressVideoEncodingPreset videoPreset =
        LivekitIngress.IngressVideoEncodingPreset.valueOf(preset);
    LivekitIngress.IngressVideoOptions videoOptions =
        LivekitIngress.IngressVideoOptions.newBuilder().setPreset(videoPreset).build();

    IngressInfo info =
        client
            .createIngress(
                identity,
                roomName,
                identity,
                identity,
                IngressInput.RTMP_INPUT,
                null,
                videoOptions,
                null,
                null,
                null)
            .execute()
            .body();

    assertNotNull(info, "Ingress creation should return IngressInfo");
    ManagerProvider.ingress().registerIngress(identity, info);

    log.info("Created RTMP ingress for {} with video preset {}", identity, preset);
  }

  @When("{string} creates a URL ingress to room {string} on {string} with URL {string}")
  public void createUrlIngress(String identity, String roomName, String serviceName, String url)
      throws Exception {
    IngressServiceClient client = getIngressClient(serviceName);

    IngressInfo info =
        client
            .createIngress(
                identity,
                roomName,
                identity,
                identity,
                IngressInput.URL_INPUT,
                null,
                null,
                null,
                null,
                url)
            .execute()
            .body();

    assertNotNull(info, "Ingress creation should return IngressInfo");
    ManagerProvider.ingress().registerIngress(identity, info);

    log.info("Created URL ingress for {} in room {} with URL: {}", identity, roomName, url);
  }

  @When("{string} starts streaming via RTMP to {string}")
  public void startStreaming(String identity, String ingressServiceName) {
    startStreamingWithDuration(identity, ingressServiceName, DEFAULT_STREAM_DURATION_SECONDS);
  }

  @When("{string} starts streaming via RTMP to {string} with duration {int} seconds")
  public void startStreamingWithDuration(
      String identity, String ingressServiceName, int durationSeconds) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressContainer ingressContainer =
        containerManager.getContainer(ingressServiceName, IngressContainer.class);

    String containerAlias = "ffmpeg-" + identity;
    String logPath =
        PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", containerAlias);

    String rtmpUrl = ingressContainer.getNetworkRtmpUrl();

    FFmpegContainer ffmpeg =
        FFmpegContainer.createRtmpStream(
            containerAlias,
            containerManager.getOrCreateNetwork(),
            rtmpUrl,
            info.getStreamKey(),
            durationSeconds,
            logPath);

    ffmpeg.start();
    containerManager.registerContainer(containerAlias, ffmpeg);
    ManagerProvider.ingress().registerActiveStream(identity, containerAlias);

    log.info(
        "Started RTMP stream for {} to {} for {} seconds",
        identity,
        ingressServiceName,
        durationSeconds);
  }

  @When("{string} starts streaming via RTMP to {string} with resolution {string}")
  public void startStreamingWithResolution(
      String identity, String ingressServiceName, String resolution) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressContainer ingressContainer =
        containerManager.getContainer(ingressServiceName, IngressContainer.class);

    String containerAlias = "ffmpeg-" + identity;
    String logPath =
        PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", containerAlias);

    String rtmpUrl = ingressContainer.getNetworkRtmpUrl();

    FFmpegContainer ffmpeg =
        FFmpegContainer.createRtmpStream(
            containerAlias,
            containerManager.getOrCreateNetwork(),
            rtmpUrl,
            info.getStreamKey(),
            DEFAULT_STREAM_DURATION_SECONDS,
            resolution,
            30,
            logPath);

    ffmpeg.start();
    containerManager.registerContainer(containerAlias, ffmpeg);
    ManagerProvider.ingress().registerActiveStream(identity, containerAlias);

    log.info(
        "Started RTMP stream for {} to {} with resolution {}",
        identity,
        ingressServiceName,
        resolution);
  }

  @When("{string} starts streaming via RTMP to {string} with wrong stream key")
  public void startStreamingWithWrongKey(String identity, String ingressServiceName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    String containerAlias = "ffmpeg-wrong-" + identity;
    String logPath =
        PathUtils.containerLogPath(getCurrentScenarioLogPath(), "docker", containerAlias);

    FFmpegContainer ffmpeg =
        FFmpegContainer.createRtmpStreamWithWrongKey(
            containerAlias, containerManager.getOrCreateNetwork(), info.getUrl(), 10, logPath);

    try {
      ffmpeg.start();
      containerManager.registerContainer(containerAlias, ffmpeg);
      TimeUnit.SECONDS.sleep(5);
      streamFailed = !ffmpeg.isRunning();
    } catch (Exception e) {
      streamFailed = true;
      log.info("RTMP stream with wrong key failed as expected: {}", e.getMessage());
    }
  }

  @When("{string} stops streaming to {string}")
  public void stopStreaming(String identity, String ingressServiceName) {
    IngressStateManager ingressManager = ManagerProvider.ingress();
    ContainerStateManager containerManager = ManagerProvider.containers();

    String containerAlias = ingressManager.getActiveStreamContainer(identity);
    if (containerAlias != null) {
      try {
        FFmpegContainer ffmpeg =
            containerManager.getContainer(containerAlias, FFmpegContainer.class);
        if (ffmpeg != null && ffmpeg.isRunning()) {
          ffmpeg.stop();
          log.info("Stopped RTMP stream for {} to {}", identity, ingressServiceName);
        }
      } catch (Exception e) {
        log.warn("Failed to stop FFmpeg container for {}: {}", identity, e.getMessage());
      }
      ingressManager.removeActiveStream(identity);
    }
  }

  @When("the ingress for {string} on {string} is deleted")
  public void deleteIngress(String identity, String serviceName) throws Exception {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");

    IngressServiceClient client = getIngressClient(serviceName);
    client.deleteIngress(info.getIngressId()).execute();

    ManagerProvider.ingress().removeIngress(identity);
    log.info("Deleted ingress for {} with ID {}", identity, info.getIngressId());
  }

  @Then("room {string} on {string} should have {int} ingresses")
  public void verifyRoomIngressCount(String roomName, String serviceName, int expectedCount)
      throws Exception {
    IngressServiceClient client = getIngressClient(serviceName);
    List<IngressInfo> ingresses = client.listIngress(roomName, null).execute().body();
    assertNotNull(ingresses, "Ingress list should not be null");
    assertEquals(
        expectedCount,
        ingresses.size(),
        "Room " + roomName + " should have " + expectedCount + " ingresses");
  }

  @Then("room {string} on {string} should have ingresses for {string}")
  public void verifyRoomIngressIdentities(
      String roomName, String serviceName, String expectedIdentities) throws Exception {
    IngressServiceClient client = getIngressClient(serviceName);
    List<IngressInfo> ingresses = client.listIngress(roomName, null).execute().body();
    assertNotNull(ingresses, "Ingress list should not be null");

    String[] identities = expectedIdentities.split(",\\s*");
    for (String identity : identities) {
      boolean found =
          ingresses.stream()
              .anyMatch(
                  info ->
                      info.getParticipantIdentity().equals(identity)
                          || info.getName().equals(identity));
      assertTrue(found, "Room " + roomName + " should have ingress for " + identity);
    }
  }

  @Then("the ingress for {string} on {string} should have input type {string}")
  public void verifyIngressInputType(String identity, String serviceName, String inputType) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");
    assertEquals(inputType, info.getInputType().name(), "Ingress input type mismatch");
  }

  @Then("the ingress for {string} on {string} should have a valid RTMP URL")
  public void verifyIngressRtmpUrl(String identity, String serviceName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");
    assertNotNull(info.getUrl(), "Ingress URL should not be null");
    assertTrue(
        info.getUrl().startsWith("rtmp://"), "Ingress URL should be an RTMP URL: " + info.getUrl());
  }

  @Then("the ingress for {string} on {string} should have a stream key")
  public void verifyIngressStreamKey(String identity, String serviceName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");
    assertNotNull(info.getStreamKey(), "Ingress stream key should not be null");
    assertFalse(info.getStreamKey().isEmpty(), "Ingress stream key should not be empty");
  }

  @Then("the ingress for {string} on {string} should be {ingressState}")
  public void verifyIngressState(String identity, String serviceName, IngressState expectedState)
      throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(storedInfo, "Ingress for " + identity + " not found");

    IngressServiceClient client = getIngressClient(serviceName);
    List<IngressInfo> ingresses =
        client.listIngress(null, storedInfo.getIngressId()).execute().body();

    assertNotNull(ingresses, "Ingress list should not be null");
    assertFalse(ingresses.isEmpty(), "Ingress should exist");

    IngressInfo currentInfo = ingresses.get(0);
    String actualSdkState = currentInfo.getState().getStatus().name();
    assertEquals(
        expectedState.toSdkState(),
        actualSdkState,
        "Ingress state mismatch. Expected: "
            + expectedState.name()
            + ", Actual: "
            + IngressState.fromSdkState(actualSdkState).name());
  }

  @Then("the ingress for {string} on {string} should be {ingressState} within {int} seconds")
  public void verifyIngressStateWithTimeout(
      String identity, String serviceName, IngressState expectedState, int timeoutSeconds)
      throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(storedInfo, "Ingress for " + identity + " not found");

    IngressServiceClient client = getIngressClient(serviceName);
    long startTime = System.currentTimeMillis();
    long timeoutMs = timeoutSeconds * 1000L;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      List<IngressInfo> ingresses =
          client.listIngress(null, storedInfo.getIngressId()).execute().body();

      if (ingresses != null && !ingresses.isEmpty()) {
        IngressInfo currentInfo = ingresses.get(0);
        String currentSdkState = currentInfo.getState().getStatus().name();

        if (currentSdkState.equals(expectedState.toSdkState())) {
          log.info(
              "Ingress for {} reached state {} after {}ms",
              identity,
              expectedState.name(),
              System.currentTimeMillis() - startTime);
          return;
        }

        log.debug(
            "Ingress for {} current state: {}, waiting for: {}",
            identity,
            IngressState.fromSdkState(currentSdkState).name(),
            expectedState.name());
      }

      TimeUnit.MILLISECONDS.sleep(STATE_POLL_INTERVAL_MS);
    }

    fail(
        "Ingress for "
            + identity
            + " did not reach state "
            + expectedState.name()
            + " within "
            + timeoutSeconds
            + " seconds");
  }

  @Then("the ingress for {string} on {string} should have participant name {string}")
  public void verifyIngressParticipantName(
      String identity, String serviceName, String expectedName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");
    assertEquals(expectedName, info.getParticipantName(), "Ingress participant name mismatch");
  }

  @Then("the ingress for {string} on {string} should have room name {string}")
  public void verifyIngressRoomName(String identity, String serviceName, String expectedRoomName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " not found");
    assertEquals(expectedRoomName, info.getRoomName(), "Ingress room name mismatch");
  }

  @Then("the ingress for {string} on {string} should be created successfully")
  public void verifyIngressCreatedSuccessfully(String identity, String serviceName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(identity);
    assertNotNull(info, "Ingress for " + identity + " should have been created");
    assertNotNull(info.getIngressId(), "Ingress ID should not be null");
  }

  @Then("the ingress for {string} on {string} should not exist")
  public void verifyIngressNotExist(String identity, String serviceName) throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(identity);

    if (storedInfo != null) {
      IngressServiceClient client = getIngressClient(serviceName);
      List<IngressInfo> ingresses =
          client.listIngress(null, storedInfo.getIngressId()).execute().body();
      assertTrue(
          ingresses == null || ingresses.isEmpty(),
          "Ingress for " + identity + " should not exist but was found");
    }
  }

  @Then("the ingress for {string} on {string} should remain {ingressState}")
  public void verifyIngressRemainsInState(
      String identity, String serviceName, IngressState expectedState) throws Exception {
    verifyIngressState(identity, serviceName, expectedState);
  }

  @Then("the RTMP stream should fail to connect")
  public void verifyRtmpStreamFailed() {
    assertTrue(streamFailed, "RTMP stream should have failed to connect");
  }

  @Given("the ingress for {string} on {string} is {ingressState}")
  public void ingressIsInState(String identity, String serviceName, IngressState expectedState)
      throws Exception {
    verifyIngressStateWithTimeout(identity, serviceName, expectedState, 30);
  }

  private IngressServiceClient getIngressClient(String serviceName) {
    LiveKitContainer livekit =
        ManagerProvider.containers().getContainer(serviceName, LiveKitContainer.class);
    return IngressServiceClient.createClient(
        livekit.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);
  }

  private void cleanupIngresses() {
    try {
      ContainerStateManager containerManager = ManagerProvider.containers();
      IngressStateManager ingressManager = ManagerProvider.ingress();

      var livekitEntry = containerManager.getFirstContainerOfType(LiveKitContainer.class);
      if (livekitEntry == null) {
        log.warn("No LiveKit container found for cleanup");
        return;
      }

      LiveKitContainer livekit = livekitEntry.getValue();
      IngressServiceClient client =
          IngressServiceClient.createClient(
              livekit.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);

      for (var entry : ingressManager.getAllIngresses().entrySet()) {
        try {
          client.deleteIngress(entry.getValue().getIngressId()).execute();
          log.info("Cleaned up ingress for: {}", entry.getKey());
        } catch (Exception e) {
          log.warn("Failed to cleanup ingress for {}: {}", entry.getKey(), e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to cleanup ingresses: {}", e.getMessage());
    }
  }

  private void cleanupFfmpegContainers() {
    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressStateManager ingressManager = ManagerProvider.ingress();

    for (String identity : ingressManager.getAllIngresses().keySet()) {
      String containerAlias = ingressManager.getActiveStreamContainer(identity);
      if (containerAlias != null) {
        try {
          containerManager.stopContainer(containerAlias);
          log.debug("Cleaned up FFmpeg container: {}", containerAlias);
        } catch (Exception e) {
          log.warn("Failed to cleanup FFmpeg container {}: {}", containerAlias, e.getMessage());
        }
      }
    }
  }

  private String getCurrentScenarioLogPath() {
    return currentScenarioLogPath != null
        ? currentScenarioLogPath
        : PathUtils.currentScenarioPath();
  }
}
