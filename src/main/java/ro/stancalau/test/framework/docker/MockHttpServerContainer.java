package ro.stancalau.test.framework.docker;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Slf4j
public class MockHttpServerContainer extends GenericContainer<MockHttpServerContainer> {
    
    private static final String MOCKSERVER_IMAGE = "mockserver/mockserver:5.15.0";
    private static final int HTTP_PORT = 1080;
    private MockServerClient mockServerClient;
    
    public MockHttpServerContainer(String scenarioLogPath, String serviceName) {
        super(DockerImageName.parse(MOCKSERVER_IMAGE));
        
        addExposedPort(HTTP_PORT);
        
        withEnv("MOCKSERVER_LOG_LEVEL", "INFO");
        withEnv("MOCKSERVER_LIVENESS_HTTP_GET_PATH", "/liveness/probe");
        
        String logPath = scenarioLogPath + "/docker/" + serviceName;
        new File(logPath).mkdirs();
        
        withLogConsumer(outputFrame -> {
            try {
                String logFile = logPath + "/mockserver.log";
                Files.write(
                    Paths.get(logFile),
                    (outputFrame.getUtf8String()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (Exception e) {
                log.error("Failed to write log", e);
            }
        });
        
        withCommand("-serverPort", String.valueOf(HTTP_PORT));
        
        waitingFor(new HttpWaitStrategy()
            .forPath("/liveness/probe")
            .forPort(HTTP_PORT)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(60)));
        
        log.info("Created MockHttpServerContainer with MockServer and logs at: {}", logPath);
    }
    
    public String getNetworkUrl(String networkAlias) {
        return "http://" + networkAlias + ":" + HTTP_PORT;
    }
    
    public String getHostUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }
    
    public void setupDefaultExpectation() {
        try {
            if (mockServerClient == null) {
                mockServerClient = new MockServerClient(getHost(), getMappedPort(HTTP_PORT));
            }
            
            mockServerClient
                .when(
                    request()
                        .withMethod("POST")
                        .withPath("/webhook")
                )
                .respond(
                    response()
                        .withStatusCode(200)
                        .withBody("OK")
                );
            
            log.info("Set up default expectation to return 200 OK for webhook POST requests");
        } catch (Exception e) {
            log.error("Failed to setup default expectation", e);
        }
    }
    
    public MockServerClient getMockServerClient() {
        if (mockServerClient == null) {
            mockServerClient = new MockServerClient(getHost(), getMappedPort(HTTP_PORT));
        }
        return mockServerClient;
    }
    
    @Override
    public void stop() {
        if (mockServerClient != null) {
            try {
                mockServerClient.close();
            } catch (Exception e) {
                log.warn("Failed to close MockServer client", e);
            }
        }
        super.stop();
    }
    
    public MockHttpServerContainer withNetworkAliasAndStart(Network network, String alias) {
        withNetwork(network);
        withNetworkAliases(alias);
        start();
        
        setupDefaultExpectation();
        
        log.info("Started MockHttpServerContainer with network alias: {} - URL: {}", 
                alias, getNetworkUrl(alias));
        return this;
    }
}