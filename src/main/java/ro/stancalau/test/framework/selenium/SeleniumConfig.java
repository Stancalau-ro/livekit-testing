package ro.stancalau.test.framework.selenium;

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
        } else {
            SafariOptions options = new SafariOptions();
            driver = new SafariDriver(options);
        }

        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    public static FirefoxOptions getFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addPreference("permissions.default.microphone", 1);
        options.addPreference("permissions.default.camera", 1);
        options.setAcceptInsecureCerts(true);
        return options;
    }

    public static ChromeOptions getChromeOptions(String allowInsecureUrl) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("use-fake-device-for-media-stream");
        options.addArguments("use-fake-ui-for-media-stream");
        options.addArguments("--disable-field-trial-config");
        options.addArguments("--disable-features=WebRtcHideLocalIpsWithMdnsg");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--reduce-security-for-testing");
        options.addArguments("--allow-running-insecure-content");
        if (allowInsecureUrl != null) {
            options.addArguments("--unsafely-treat-insecure-origin-as-secure=http://" + allowInsecureUrl);
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

    static private String findFile(String filename) {
        String paths[] = {"", "bin/", "target/classes/", "src/integration-test/resources/"};
        for (String path : paths) {
            if (new File(path + filename).exists()) {
                return path + filename;
            }
        }
        return "";
    }
}
