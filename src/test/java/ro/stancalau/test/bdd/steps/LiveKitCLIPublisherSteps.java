package ro.stancalau.test.bdd.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.livekit.server.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import ro.stancalau.test.framework.docker.CLIPublisherContainer;
import ro.stancalau.test.framework.docker.LiveKitContainer;
import ro.stancalau.test.framework.util.DateUtils;
import ro.stancalau.test.framework.util.FileUtils;
import ro.stancalau.test.framework.util.PathUtils;
import ro.stancalau.test.framework.util.ScenarioNamingUtils;
import ro.stancalau.test.framework.util.StringParsingUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class LiveKitCLIPublisherSteps {

    private String currentScenarioLogPath;
    private int cliPublisherCount = 0;

    public LiveKitCLIPublisherSteps() {
    }

    @Before
    public void setUpCLIPublisherSteps(Scenario scenario) {
        String featureName = ScenarioNamingUtils.extractFeatureName(scenario.getUri().toString());
        String scenarioName = scenario.getName();
        String timestamp = DateUtils.generateScenarioTimestamp();

        String sanitizedFeatureName = FileUtils.sanitizeFileNameStrict(featureName);
        String sanitizedScenarioName = FileUtils.sanitizeFileNameStrict(scenarioName);

        currentScenarioLogPath = PathUtils.scenarioPath(sanitizedFeatureName, sanitizedScenarioName, timestamp);
        cliPublisherCount = 0; // Reset counter for each scenario
    }

    @After
    public void tearDownCLIPublisherSteps() {
        ManagerProvider.containers().cleanup(CLIPublisherContainer.class);
    }

    @Given("a CLI load test publisher with {int} video publishers connects to room {string} using service {string}")
    public void aCLILoadTestPublisherWithVideoPublishersConnectsToRoom(int videoPublishers, String roomName, String serviceName) {
        startLoadTestPublisher(serviceName, roomName, videoPublishers, 0, 0, null);
    }

    @Given("a CLI load test publisher with {int} audio publishers connects to room {string} using service {string}")
    public void aCLILoadTestPublisherWithAudioPublishersConnectsToRoom(int audioPublishers, String roomName, String serviceName) {
        startLoadTestPublisher(serviceName, roomName, 0, audioPublishers, 0, null);
    }

    @Given("a CLI load test with {int} video publishers and {int} subscribers connects to room {string} using service {string}")
    public void aCLILoadTestWithVideoPublishersAndSubscribersConnectsToRoom(int videoPublishers, int subscribers,
                                                                            String roomName, String serviceName) {
        startLoadTestPublisher(serviceName, roomName, videoPublishers, 0, subscribers, null);
    }

    @Given("a CLI load test with config {string} connects to room {string} using service {string}")
    public void aCLILoadTestWithConfigConnectsToRoom(String configString, String roomName, String serviceName) {
        // Replace colons with equals for parsing
        String normalizedConfig = configString.replace(":", "=");
        Map<String, String> config = StringParsingUtils.parseKeyValuePairs(normalizedConfig);

        CLIPublisherContainer.PublisherConfig.Builder builder = CLIPublisherContainer.PublisherConfig.builder();

        if (config.containsKey("videoPublishers")) {
            builder.videoPublishers(Integer.parseInt(config.get("videoPublishers")));
        }
        if (config.containsKey("audioPublishers")) {
            builder.audioPublishers(Integer.parseInt(config.get("audioPublishers")));
        }
        if (config.containsKey("subscribers")) {
            builder.subscribers(Integer.parseInt(config.get("subscribers")));
        }
        if (config.containsKey("videoResolution")) {
            builder.videoResolution(config.get("videoResolution"));
        }
        if (config.containsKey("simulcast")) {
            builder.simulcast(Boolean.parseBoolean(config.get("simulcast")));
        }
        if (config.containsKey("duration")) {
            builder.duration(Integer.parseInt(config.get("duration")));
        }
        if (config.containsKey("numPerSecond")) {
            builder.numPerSecond(Integer.parseInt(config.get("numPerSecond")));
        }
        if (config.containsKey("layout")) {
            builder.layout(config.get("layout"));
        }
        if (config.containsKey("simulateSpeakers")) {
            builder.simulateSpeakers(Boolean.parseBoolean(config.get("simulateSpeakers")));
        }

        startLoadTestPublisher(serviceName, roomName, builder.build());
    }

    @When("{string} starts a CLI publisher with {string} to room {string} using service {string}")
    public void startsACLIPublisherWithMediaToRoomUsingService(String identity, String mediaType, String roomName, String serviceName) {
        boolean publishVideo;
        boolean publishAudio;
        
        switch (mediaType.toLowerCase()) {
            case "video":
                publishVideo = true;
                publishAudio = false;
                break;
            case "audio":
                publishVideo = false;
                publishAudio = true;
                break;
            case "a/v":
            case "audio/video":
                publishVideo = true;
                publishAudio = true;
                break;
            default:
                throw new IllegalArgumentException("Invalid media type: " + mediaType + ". Valid options are: 'audio', 'video', 'a/v'");
        }
        
        startJoinPublisher(identity, roomName, serviceName, publishVideo, publishAudio);
    }

    @When("{string} starts a CLI publisher to room {string} using service {string}")
    public void startsACLIPublisherToRoomUsingService(String identity, String roomName, String serviceName) {
        startJoinPublisher(identity, roomName, serviceName, true, true);
    }

    @When("{string} starts a CLI video-only publisher to room {string} using service {string}")
    public void startsACLIVideoOnlyPublisherToRoomUsingService(String identity, String roomName, String serviceName) {
        startJoinPublisher(identity, roomName, serviceName, true, false);
    }

    @When("{string} starts a CLI audio-only publisher to room {string} using service {string}")
    public void startsACLIAudioOnlyPublisherToRoomUsingService(String identity, String roomName, String serviceName) {
        startJoinPublisher(identity, roomName, serviceName, false, true);
    }

    @Then("the CLI load test for room {string} should complete successfully within {int} seconds")
    public void theCLILoadTestForRoomShouldCompleteSuccessfullyWithinSeconds(String roomName, int timeoutSeconds) throws InterruptedException {
        String containerAlias = "cli-load-test-" + roomName.toLowerCase();
        CLIPublisherContainer container = ManagerProvider.containers()
                .getContainer(containerAlias, CLIPublisherContainer.class);

        assertNotNull(container, "CLI load test container should exist");

        int waited = 0;
        while (container.isRunning() && waited < timeoutSeconds) {
            TimeUnit.SECONDS.sleep(1);
            waited++;
        }

        assertTrue(!container.isRunning() || waited < timeoutSeconds,
                "CLI load test should complete within " + timeoutSeconds + " seconds");
    }

    @Then("the CLI publisher {string} should be connected to room {string}")
    public void theCLIPublisherShouldBeConnectedToRoom(String identity, String roomName) {
        String containerName = "cli-publisher-" + identity.toLowerCase();
        CLIPublisherContainer container = ManagerProvider.containers()
                .getContainer(containerName, CLIPublisherContainer.class);

        assertNotNull(container, "CLI publisher container for " + identity + " should exist");
        assertTrue(container.isRunning(), "CLI publisher for " + identity + " should be running");
    }

    private void startLoadTestPublisher(String serviceName, String roomName, int videoPublishers,
                                        int audioPublishers, int subscribers, Integer duration) {
        CLIPublisherContainer.PublisherConfig.Builder builder = CLIPublisherContainer.PublisherConfig.builder()
                .videoPublishers(videoPublishers)
                .audioPublishers(audioPublishers)
                .subscribers(subscribers);

        if (duration != null) {
            builder.duration(duration);
        }

        CLIPublisherContainer.PublisherConfig config = builder.build();
        startLoadTestPublisher(serviceName, roomName, config);
    }

    private void startLoadTestPublisher(String serviceName, String roomName, CLIPublisherContainer.PublisherConfig config) {
        Network network = ManagerProvider.containers().getOrCreateNetwork();
        LiveKitContainer liveKitContainer = ManagerProvider.containers()
                .getContainer(serviceName, LiveKitContainer.class);

        assertNotNull(liveKitContainer, "LiveKit container " + serviceName + " should exist");

        String wsUrl = liveKitContainer.getNetworkUrl();
        String apiKey = LiveKitContainer.API_KEY;
        String apiSecret = LiveKitContainer.SECRET;

        String containerAlias = "cli-load-test-" + roomName.toLowerCase();
        String logPath = PathUtils.containerLogPath(getScenarioLogPath(), "docker", containerAlias);

        CLIPublisherContainer cliContainer = CLIPublisherContainer.createLoadTestContainer(
                containerAlias, network, wsUrl, apiKey, apiSecret, roomName, config, logPath);

        cliContainer.start();
        log.info("Started CLI load test publisher for room {} with config: {}", roomName, config);

        ManagerProvider.containers().registerContainer(containerAlias, cliContainer);

        // Give the CLI container time to connect and create participants
        try {
            Thread.sleep(3000); // Wait 3 seconds for participants to join
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startJoinPublisher(String identity, String roomName, String serviceName,
                                    boolean publishVideo, boolean publishAudio) {
        Network network = ManagerProvider.containers().getOrCreateNetwork();
        LiveKitContainer liveKitContainer = ManagerProvider.containers()
                .getContainer(serviceName, LiveKitContainer.class);

        assertNotNull(liveKitContainer, "LiveKit container " + serviceName + " should exist");

        String wsUrl = liveKitContainer.getNetworkUrl();
        String apiKey = LiveKitContainer.API_KEY;
        String apiSecret = LiveKitContainer.SECRET;

        // Note: We still check the token exists to ensure it was created, 
        // but we use API credentials for the CLI
        AccessToken token = ManagerProvider.tokens().getLastToken(identity, roomName);
        assertNotNull(token, "Access token for " + identity + " in room " + roomName + " should exist");

        // Add progressive delay to avoid simultaneous connections
        cliPublisherCount++;
        if (cliPublisherCount > 1) {
            try {
                int delaySeconds = (cliPublisherCount - 1) * 3; // 3 seconds between each CLI publisher
                log.info("Adding {} second delay before starting CLI publisher {} (count: {})", 
                        delaySeconds, identity, cliPublisherCount);
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String containerAlias = "cli-publisher-" + identity.toLowerCase();
        String logPath = PathUtils.containerLogPath(getScenarioLogPath(), "docker", containerAlias);

        CLIPublisherContainer cliContainer = CLIPublisherContainer.createJoinContainer(
                containerAlias, network, wsUrl, apiKey, apiSecret, roomName, identity,
                publishVideo, publishAudio, logPath);

        cliContainer.start();
        log.info("Started CLI publisher for {} in room {} (video: {}, audio: {})",
                identity, roomName, publishVideo, publishAudio);

        ManagerProvider.containers().registerContainer(containerAlias, cliContainer);

        // Give the CLI container time to connect
        try {
            Thread.sleep(5000); // Wait 5 seconds for connection
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getScenarioLogPath() {
        if (currentScenarioLogPath == null) {
            currentScenarioLogPath = PathUtils.currentScenarioPath();
        }
        return currentScenarioLogPath;
    }
}