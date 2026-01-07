package ro.stancalau.test.framework.selenium;

import java.io.File;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import ro.stancalau.test.framework.util.PathUtils;

@Getter
public class SeleniumConfig {

    private WebDriver driver;

    public SeleniumConfig(String browser) {
        this(browser, null);
    }

    public SeleniumConfig(String browser, String allowInsecureUrl) {
        if (browser.contains("firefox")) {
            FirefoxOptions options = getFirefoxOptions();
            driver = new FirefoxDriver(options);
        } else if (browser.contains("chrome")) {
            ChromeOptions options = getChromeOptions(allowInsecureUrl);
            driver = new ChromeDriver(options);
        } else if (browser.contains("edge")) {
            EdgeOptions options = getEdgeOptions(allowInsecureUrl);
            driver = new EdgeDriver(options);
        } else {
            ChromeOptions options = getChromeOptions(allowInsecureUrl);
            driver = new ChromeDriver(options);
        }

        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    public static FirefoxOptions getFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        // Basic media permissions
        options.addPreference("permissions.default.microphone", 1);
        options.addPreference("permissions.default.camera", 1);

        // Fake media streams for testing
        options.addPreference("media.navigator.streams.fake", true);
        options.addPreference("media.navigator.permission.disabled", true);
        options.addPreference("media.getusermedia.screensharing.enabled", true);
        options.addPreference("media.getusermedia.screensharing.allowed_domains", "localhost,webserver");

        // WebRTC specific settings
        options.addPreference("media.autoplay.default", 0);
        options.addPreference("media.autoplay.enabled", true);
        options.addPreference("media.peerconnection.enabled", true);
        options.addPreference("media.peerconnection.ice.relay_only", false);
        options.addPreference("media.peerconnection.use_document_iceservers", false);
        options.addPreference("media.peerconnection.identity.enabled", false);
        options.addPreference("media.peerconnection.identity.timeout", 1);
        options.addPreference("media.peerconnection.turn.disable", false);
        options.addPreference("media.peerconnection.ice.tcp", true);
        options.addPreference("media.peerconnection.ice.default_address_only", false);

        // Security and insecure origins
        options.addPreference("media.devices.insecure.enabled", true);
        options.addPreference("media.getusermedia.insecure.enabled", true);
        options.addPreference("network.websocket.allowInsecureFromHTTPS", true);
        options.addPreference("security.mixed_content.block_active_content", false);

        // WebSocket specific settings for container networking
        options.addPreference("network.websocket.max-connections", 200);
        options.addPreference("network.http.speculative-parallel-limit", 0);

        // Additional settings
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("devtools.console.stdout.content", true);

        // Enable logging
        options.addPreference("webrtc.logging.browser_level", "verbose");
        options.addPreference("webrtc.logging.aec_debug_dump", true);

        // Add Firefox arguments
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        options.setAcceptInsecureCerts(true);
        return options;
    }

    public static ChromeOptions getChromeOptions(String allowInsecureUrl) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("use-fake-device-for-media-stream");
        options.addArguments("use-fake-ui-for-media-stream");
        options.addArguments("--auto-select-desktop-capture-source=Entire screen");
        options.addArguments("--enable-usermedia-screen-capturing");
        options.addArguments("--disable-field-trial-config");
        options.addArguments("--disable-features=WebRtcHideLocalIpsWithMdnsg");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--reduce-security-for-testing");
        options.addArguments("--allow-running-insecure-content");
        if (allowInsecureUrl != null) {
            options.addArguments("--unsafely-treat-insecure-origin-as-secure=" + allowInsecureUrl);
        }

        options.setAcceptInsecureCerts(true);
        return options;
    }

    public static EdgeOptions getEdgeOptions(String allowInsecureUrl) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("use-fake-device-for-media-stream");
        options.addArguments("use-fake-ui-for-media-stream");
        options.addArguments("--auto-select-desktop-capture-source=Entire screen");
        options.addArguments("--enable-usermedia-screen-capturing");
        options.addArguments("--disable-field-trial-config");
        options.addArguments("--disable-features=WebRtcHideLocalIpsWithMdnsg");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--reduce-security-for-testing");
        options.addArguments("--allow-running-insecure-content");
        if (allowInsecureUrl != null) {
            options.addArguments("--unsafely-treat-insecure-origin-as-secure=" + allowInsecureUrl);
        }

        options.setAcceptInsecureCerts(true);
        return options;
    }

    static {
        if (OSValidator.isWindows()) {
            System.setProperty("webdriver.gecko.driver", findFile("geckodriver.exe"));
            System.setProperty("webdriver.chrome.driver", findFile("chromedriver.exe"));
        } else if (OSValidator.isMac()) {
            System.setProperty("webdriver.gecko.driver", findFile("geckodrivermac"));
        } else {
            System.setProperty("webdriver.gecko.driver", findFile("geckodriverlinux64"));
        }
    }

    private static String findFile(String filename) {
        String[] paths = {
            "", "bin", PathUtils.join("target", "classes"), PathUtils.join("src", "integration-test", "resources")
        };
        for (String path : paths) {
            File file = path.isEmpty() ? new File(filename) : new File(path, filename);
            if (file.exists()) {
                return file.getPath();
            }
        }
        return "";
    }
}
