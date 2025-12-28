package ro.stancalau.test.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
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

  @Given("an Ingress service is running with service name {string}")
  public void ingressServiceRunning(String serviceName) {
    ContainerStateManager containerManager = ManagerProvider.containers();
    LiveKitContainer livekit = containerManager.getContainer("livekit1", LiveKitContainer.class);
    RedisContainer redis = containerManager.getContainer("redis", RedisContainer.class);
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

  @When("an RTMP ingress {string} is created for room {string} with identity {string}")
  public void createRtmpIngress(String ingressName, String roomName, String identity)
      throws Exception {
    createRtmpIngressWithName(ingressName, roomName, identity, identity);
  }

  @When(
      "an RTMP ingress {string} is created for room {string} with identity {string} and name {string}")
  public void createRtmpIngressWithName(
      String ingressName, String roomName, String identity, String displayName) throws Exception {
    IngressServiceClient client = getIngressClient();

    IngressInfo info =
        client
            .createIngress(
                ingressName,
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
    ManagerProvider.ingress().registerIngress(ingressName, info);

    log.info(
        "Created RTMP ingress {} for room {} with URL: {} and stream key: {}",
        ingressName,
        roomName,
        info.getUrl(),
        info.getStreamKey());
  }

  @When(
      "an RTMP ingress {string} is created for room {string} with identity {string} and video preset {string}")
  public void createRtmpIngressWithPreset(
      String ingressName, String roomName, String identity, String preset) throws Exception {
    IngressServiceClient client = getIngressClient();

    LivekitIngress.IngressVideoEncodingPreset videoPreset =
        LivekitIngress.IngressVideoEncodingPreset.valueOf(preset);
    LivekitIngress.IngressVideoOptions videoOptions =
        LivekitIngress.IngressVideoOptions.newBuilder().setPreset(videoPreset).build();

    IngressInfo info =
        client
            .createIngress(
                ingressName,
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
    ManagerProvider.ingress().registerIngress(ingressName, info);

    log.info("Created RTMP ingress {} with video preset {}", ingressName, preset);
  }

  @When(
      "a URL ingress {string} is created for room {string} with URL {string} and identity {string}")
  public void createUrlIngress(String ingressName, String roomName, String url, String identity)
      throws Exception {
    IngressServiceClient client = getIngressClient();

    IngressInfo info =
        client
            .createIngress(
                ingressName,
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
    ManagerProvider.ingress().registerIngress(ingressName, info);

    log.info("Created URL ingress {} for room {} with URL: {}", ingressName, roomName, url);
  }

  @When("an RTMP stream is sent to ingress {string}")
  public void sendRtmpStream(String ingressName) {
    sendRtmpStreamWithDuration(ingressName, DEFAULT_STREAM_DURATION_SECONDS);
  }

  @When("an RTMP stream is sent to ingress {string} with duration {int} seconds")
  public void sendRtmpStreamWithDuration(String ingressName, int durationSeconds) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressContainer ingressContainer =
        containerManager.getContainer("ingress1", IngressContainer.class);

    String containerAlias = "ffmpeg-" + ingressName;
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
    ManagerProvider.ingress().registerActiveStream(ingressName, containerAlias);

    waitForIngressPublishing(ingressName, info.getIngressId(), 30);

    log.info(
        "Started RTMP stream to ingress {} at {} for {} seconds",
        ingressName,
        rtmpUrl,
        durationSeconds);
  }

  private void waitForIngressPublishing(String ingressName, String ingressId, int timeoutSeconds) {
    IngressServiceClient client = getIngressClient();
    long startTime = System.currentTimeMillis();
    long timeoutMs = timeoutSeconds * 1000L;
    IngressState targetState = IngressState.publishing;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        List<IngressInfo> ingresses = client.listIngress(null, ingressId).execute().body();
        if (ingresses != null && !ingresses.isEmpty()) {
          IngressInfo current = ingresses.get(0);
          String currentSdkState = current.getState().getStatus().name();

          String errorMsg = current.getState().getError();
          if (errorMsg != null && !errorMsg.isEmpty()) {
            log.error("Ingress {} has error: {}", ingressName, errorMsg);
          }

          if (targetState.toSdkState().equals(currentSdkState)) {
            log.info(
                "Ingress {} reached {} after {}ms",
                ingressName,
                targetState.name(),
                System.currentTimeMillis() - startTime);
            return;
          }

          log.info(
              "Ingress {} current state: {}, waiting for {}",
              ingressName,
              IngressState.fromSdkState(currentSdkState).name(),
              targetState.name());
        }
        TimeUnit.MILLISECONDS.sleep(500);
      } catch (Exception e) {
        log.warn("Error checking ingress state: {}", e.getMessage());
        try {
          TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    fail(
        "Ingress "
            + ingressName
            + " did not reach state "
            + targetState.name()
            + " within "
            + timeoutSeconds
            + " seconds");
  }

  @When("an RTMP stream is sent to ingress {string} with resolution {string}")
  public void sendRtmpStreamWithResolution(String ingressName, String resolution) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressContainer ingressContainer =
        containerManager.getContainer("ingress1", IngressContainer.class);

    String containerAlias = "ffmpeg-" + ingressName;
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
    ManagerProvider.ingress().registerActiveStream(ingressName, containerAlias);

    waitForIngressPublishing(ingressName, info.getIngressId(), 30);

    log.info(
        "Started RTMP stream to ingress {} at {} with resolution {}",
        ingressName,
        rtmpUrl,
        resolution);
  }

  @When("an RTMP stream is sent to ingress {string} with wrong stream key")
  public void sendRtmpStreamWithWrongKey(String ingressName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");

    ContainerStateManager containerManager = ManagerProvider.containers();
    String containerAlias = "ffmpeg-wrong-" + ingressName;
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

  @When("the RTMP stream is stopped")
  public void stopRtmpStream() {
    IngressStateManager ingressManager = ManagerProvider.ingress();
    ContainerStateManager containerManager = ManagerProvider.containers();

    for (String ingressName : ingressManager.getAllIngresses().keySet()) {
      String containerAlias = ingressManager.getActiveStreamContainer(ingressName);
      if (containerAlias != null) {
        try {
          FFmpegContainer ffmpeg =
              containerManager.getContainer(containerAlias, FFmpegContainer.class);
          if (ffmpeg != null && ffmpeg.isRunning()) {
            ffmpeg.stop();
            log.info("Stopped RTMP stream container: {}", containerAlias);
          }
        } catch (Exception e) {
          log.warn("Failed to stop FFmpeg container {}: {}", containerAlias, e.getMessage());
        }
        ingressManager.removeActiveStream(ingressName);
      }
    }
  }

  @When("ingress {string} is deleted")
  public void deleteIngress(String ingressName) throws Exception {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");

    IngressServiceClient client = getIngressClient();
    client.deleteIngress(info.getIngressId()).execute();

    ManagerProvider.ingress().removeIngress(ingressName);
    log.info("Deleted ingress {} with ID {}", ingressName, info.getIngressId());
  }

  @When("ingresses are listed for room {string}")
  public void listIngressesForRoom(String roomName) throws Exception {
    IngressServiceClient client = getIngressClient();
    List<IngressInfo> ingresses = client.listIngress(roomName, null).execute().body();
    ManagerProvider.ingress().setLastListResult(ingresses);
    log.info("Listed {} ingresses for room {}", ingresses != null ? ingresses.size() : 0, roomName);
  }

  @Then("the ingress {string} should have input type {string}")
  public void verifyIngressInputType(String ingressName, String inputType) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");
    assertEquals(inputType, info.getInputType().name(), "Ingress input type mismatch");
  }

  @Then("the ingress {string} should have a valid RTMP URL")
  public void verifyIngressRtmpUrl(String ingressName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");
    assertNotNull(info.getUrl(), "Ingress URL should not be null");
    assertTrue(
        info.getUrl().startsWith("rtmp://"), "Ingress URL should be an RTMP URL: " + info.getUrl());
  }

  @Then("the ingress {string} should have a stream key")
  public void verifyIngressStreamKey(String ingressName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");
    assertNotNull(info.getStreamKey(), "Ingress stream key should not be null");
    assertFalse(info.getStreamKey().isEmpty(), "Ingress stream key should not be empty");
  }

  @Then("the ingress {string} should be in state {ingressState}")
  public void verifyIngressState(String ingressName, IngressState expectedState) throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(storedInfo, "Ingress " + ingressName + " not found");

    IngressServiceClient client = getIngressClient();
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

  @Then("the ingress {string} should be in state {ingressState} within {int} seconds")
  public void verifyIngressStateWithTimeout(
      String ingressName, IngressState expectedState, int timeoutSeconds) throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(storedInfo, "Ingress " + ingressName + " not found");

    IngressServiceClient client = getIngressClient();
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
              "Ingress {} reached state {} after {}ms",
              ingressName,
              expectedState.name(),
              System.currentTimeMillis() - startTime);
          return;
        }

        log.debug(
            "Ingress {} current state: {}, waiting for: {}",
            ingressName,
            IngressState.fromSdkState(currentSdkState).name(),
            expectedState.name());
      }

      TimeUnit.MILLISECONDS.sleep(STATE_POLL_INTERVAL_MS);
    }

    fail(
        "Ingress "
            + ingressName
            + " did not reach state "
            + expectedState.name()
            + " within "
            + timeoutSeconds
            + " seconds");
  }

  @Then("the ingress {string} should have participant name {string}")
  public void verifyIngressParticipantName(String ingressName, String expectedName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");
    assertEquals(expectedName, info.getParticipantName(), "Ingress participant name mismatch");
  }

  @Then("the ingress {string} should have room name {string}")
  public void verifyIngressRoomName(String ingressName, String expectedRoomName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " not found");
    assertEquals(expectedRoomName, info.getRoomName(), "Ingress room name mismatch");
  }

  @Then("the ingress {string} should be created successfully")
  public void verifyIngressCreatedSuccessfully(String ingressName) {
    IngressInfo info = ManagerProvider.ingress().getIngress(ingressName);
    assertNotNull(info, "Ingress " + ingressName + " should have been created");
    assertNotNull(info.getIngressId(), "Ingress ID should not be null");
  }

  @Then("the ingress {string} should not exist")
  public void verifyIngressNotExist(String ingressName) throws Exception {
    IngressInfo storedInfo = ManagerProvider.ingress().getIngress(ingressName);

    if (storedInfo != null) {
      IngressServiceClient client = getIngressClient();
      List<IngressInfo> ingresses =
          client.listIngress(null, storedInfo.getIngressId()).execute().body();
      assertTrue(
          ingresses == null || ingresses.isEmpty(),
          "Ingress " + ingressName + " should not exist but was found");
    }
  }

  @Then("the ingress {string} should remain in state {ingressState}")
  public void verifyIngressRemainsInState(String ingressName, IngressState expectedState)
      throws Exception {
    verifyIngressState(ingressName, expectedState);
  }

  @Then("the ingress list should contain {string}")
  public void verifyIngressListContains(String ingressName) {
    List<IngressInfo> ingresses = ManagerProvider.ingress().getLastListResult();
    assertNotNull(ingresses, "Ingress list should not be null");

    boolean found = ingresses.stream().anyMatch(info -> info.getName().equals(ingressName));
    assertTrue(found, "Ingress list should contain " + ingressName);
  }

  @Then("the ingress list should have {int} items")
  public void verifyIngressListSize(int count) {
    List<IngressInfo> ingresses = ManagerProvider.ingress().getLastListResult();
    assertNotNull(ingresses, "Ingress list should not be null");
    assertEquals(count, ingresses.size(), "Ingress list size mismatch");
  }

  @Then("the RTMP stream should fail to connect")
  public void verifyRtmpStreamFailed() {
    assertTrue(streamFailed, "RTMP stream should have failed to connect");
  }

  @And("the ingress {string} is in state {ingressState}")
  public void ingressIsInState(String ingressName, IngressState expectedState) throws Exception {
    verifyIngressStateWithTimeout(ingressName, expectedState, 30);
  }

  private IngressServiceClient getIngressClient() {
    LiveKitContainer livekit =
        ManagerProvider.containers().getContainer("livekit1", LiveKitContainer.class);
    return IngressServiceClient.createClient(
        livekit.getHttpUrl(), LiveKitContainer.API_KEY, LiveKitContainer.SECRET);
  }

  private void cleanupIngresses() {
    try {
      IngressServiceClient client = getIngressClient();
      IngressStateManager ingressManager = ManagerProvider.ingress();

      for (var entry : ingressManager.getAllIngresses().entrySet()) {
        try {
          client.deleteIngress(entry.getValue().getIngressId()).execute();
          log.info("Cleaned up ingress: {}", entry.getKey());
        } catch (Exception e) {
          log.warn("Failed to cleanup ingress {}: {}", entry.getKey(), e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to cleanup ingresses: {}", e.getMessage());
    }
  }

  private void cleanupFfmpegContainers() {
    ContainerStateManager containerManager = ManagerProvider.containers();
    IngressStateManager ingressManager = ManagerProvider.ingress();

    for (String ingressName : ingressManager.getAllIngresses().keySet()) {
      String containerAlias = ingressManager.getActiveStreamContainer(ingressName);
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
