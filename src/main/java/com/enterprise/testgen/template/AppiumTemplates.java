package com.enterprise.testgen.template;

/**
 * Appium code templates for mobile automation testing.
 * Supports both iOS (XCUITest) and Android (UiAutomator2) platforms.
 * Uses Appium 2.0+ with Java client.
 */
public class AppiumTemplates {

    public static final String APPIUM_VERSION = "2.0+";

    /**
     * Base Mobile Page Object class template
     */
    public static final String BASE_MOBILE_PAGE_TEMPLATE = """
            package %s.pages;

            import io.appium.java_client.AppiumDriver;
            import io.appium.java_client.MobileElement;
            import org.openqa.selenium.support.PageFactory;
            import org.openqa.selenium.support.ui.ExpectedConditions;
            import org.openqa.selenium.support.ui.WebDriverWait;
            import java.time.Duration;

            /**
             * Base Mobile Page Object class providing common mobile automation functionality.
             * All mobile page objects should extend this class.
             */
            public abstract class BaseMobilePage {

                protected AppiumDriver driver;
                protected WebDriverWait wait;
                private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

                public BaseMobilePage(AppiumDriver driver) {
                    this.driver = driver;
                    this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
                    PageFactory.initElements(driver, this);
                }

                protected void waitForElementPresent(MobileElement element) {
                    wait.until(ExpectedConditions.presenceOfElementLocated((org.openqa.selenium.By) element));
                }

                protected void tap(MobileElement element) {
                    wait.until(ExpectedConditions.elementToBeClickable(element));
                    element.click();
                }

                protected void typeText(MobileElement element, String text) {
                    wait.until(ExpectedConditions.visibilityOf(element));
                    element.clear();
                    element.sendKeys(text);
                }

                protected String getText(MobileElement element) {
                    wait.until(ExpectedConditions.visibilityOf(element));
                    return element.getText();
                }

                protected void scrollDown() {
                    driver.executeScript("mobile: scroll", 
                        org.openqa.selenium.devtools.v119.dom.model.Quad.builder()
                            .direction("down")
                            .build());
                }

                protected void swipeLeft() {
                    driver.executeScript("mobile: swipe", 
                        org.openqa.selenium.devtools.v119.dom.model.Quad.builder()
                            .direction("left")
                            .build());
                }
            }
            """;

    /**
     * Mobile test case template for UI/functional testing
     */
    public static final String MOBILE_TEST_TEMPLATE = """
            package %s.tests;

            import io.appium.java_client.AppiumDriver;
            import io.appium.java_client.android.AndroidDriver;
            import io.appium.java_client.ios.IOSDriver;
            import org.openqa.selenium.remote.DesiredCapabilities;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.Test;
            import java.net.MalformedURLException;
            import java.net.URL;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * Mobile Test Class: %s
             * Platform: Android and iOS
             * Scenario: %s
             */
            public class %s {

                private AppiumDriver driver;
                private static final String APPIUM_SERVER_URL = "%s";
                private static final String ANDROID_APP_PACKAGE = "%s";
                private static final String iOS_BUNDLE_ID = "%s";

                @BeforeEach
                public void setUp() throws MalformedURLException {
                    // Initialize for Android
                    // Uncomment and configure for iOS testing
                    
                    DesiredCapabilities caps = new DesiredCapabilities();
                    caps.setCapability("platformName", "Android");
                    caps.setCapability("deviceName", "emulator-5554");
                    caps.setCapability("automationName", "UiAutomator2");
                    caps.setCapability("appPackage", ANDROID_APP_PACKAGE);
                    caps.setCapability("appActivity", ".MainActivity");
                    
                    driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), caps);
                }

                @Test
                public void test%s() throws InterruptedException {
                    // Test implementation goes here
                    // Example: Tap, enter text, verify elements
                    
                    assertNotNull(driver, "Driver should be initialized");
                    
                    // TODO: Implement test steps
                }

                @AfterEach
                public void tearDown() {
                    if (driver != null) {
                        driver.quit();
                    }
                }
            }
            """;

    /**
     * Android-specific test template
     */
    public static final String ANDROID_TEST_TEMPLATE = """
            package %s.tests;

            import io.appium.java_client.android.AndroidDriver;
            import io.appium.java_client.android.nativekey.AndroidKey;
            import io.appium.java_client.android.nativekey.KeyEvent;
            import org.openqa.selenium.By;
            import org.openqa.selenium.remote.DesiredCapabilities;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.Test;
            import java.net.MalformedURLException;
            import java.net.URL;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * Android-specific Test: %s
             * App Package: %s
             * Scenario: %s
             */
            public class %s {

                private AndroidDriver driver;
                private static final String APPIUM_SERVER_URL = "%s";
                private static final String APP_PACKAGE = "%s";

                @BeforeEach
                public void setUp() throws MalformedURLException {
                    DesiredCapabilities caps = new DesiredCapabilities();
                    caps.setCapability("platformName", "Android");
                    caps.setCapability("deviceName", "emulator-5554");
                    caps.setCapability("automationName", "UiAutomator2");
                    caps.setCapability("appPackage", APP_PACKAGE);
                    caps.setCapability("appActivity", ".MainActivity");
                    caps.setCapability("noReset", false);
                    
                    driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), caps);
                }

                @Test
                public void test%s() {
                    // Android-specific test implementation
                    // Test steps here
                    
                    assertNotNull(driver, "Android driver should be initialized");
                    
                    // Example: Press back button
                    // driver.pressKey(new KeyEvent(AndroidKey.BACK));
                }

                @AfterEach
                public void tearDown() {
                    if (driver != null) {
                        driver.quit();
                    }
                }
            }
            """;

    /**
     * iOS-specific test template
     */
    public static final String iOS_TEST_TEMPLATE = """
            package %s.tests;

            import io.appium.java_client.ios.IOSDriver;
            import io.appium.java_client.ios.IOSElement;
            import org.openqa.selenium.remote.DesiredCapabilities;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.Test;
            import java.net.MalformedURLException;
            import java.net.URL;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * iOS-specific Test: %s
             * Bundle ID: %s
             * Scenario: %s
             */
            public class %s {

                private IOSDriver driver;
                private static final String APPIUM_SERVER_URL = "%s";
                private static final String BUNDLE_ID = "%s";

                @BeforeEach
                public void setUp() throws MalformedURLException {
                    DesiredCapabilities caps = new DesiredCapabilities();
                    caps.setCapability("platformName", "iOS");
                    caps.setCapability("deviceName", "iPhone 14");
                    caps.setCapability("automationName", "XCUITest");
                    caps.setCapability("bundleId", BUNDLE_ID);
                    caps.setCapability("udid", "auto");
                    
                    driver = new IOSDriver(new URL(APPIUM_SERVER_URL), caps);
                }

                @Test
                public void test%s() {
                    // iOS-specific test implementation
                    // Test steps here
                    
                    assertNotNull(driver, "iOS driver should be initialized");
                }

                @AfterEach
                public void tearDown() {
                    if (driver != null) {
                        driver.quit();
                    }
                }
            }
            """;

    /**
     * Helper utilities for mobile testing
     */
    public static final String MOBILE_UTILS_TEMPLATE = """
            package %s.utils;

            import io.appium.java_client.AppiumDriver;
            import org.openqa.selenium.Dimension;
            import org.openqa.selenium.remote.RemoteWebElement;

            /**
             * Utility class for common mobile automation operations
             */
            public class MobileUtils {

                public static void tap(AppiumDriver driver, int x, int y) {
                    driver.executeScript("mobile: tap", 
                        org.openqa.selenium.devtools.v119.dom.model.Quad.builder()
                            .x(x)
                            .y(y)
                            .build());
                }

                public static void swipe(AppiumDriver driver, String direction) {
                    driver.executeScript("mobile: swipe", 
                        org.openqa.selenium.devtools.v119.dom.model.Quad.builder()
                            .direction(direction)
                            .build());
                }

                public static void hideKeyboard(AppiumDriver driver) {
                    try {
                        driver.hideKeyboard();
                    } catch (Exception e) {
                        // Keyboard might already be hidden
                    }
                }

                public static boolean isElementVisible(AppiumDriver driver, RemoteWebElement element) {
                    try {
                        return element.isDisplayed();
                    } catch (Exception e) {
                        return false;
                    }
                }

                public static void waitForAppToLoad(long milliseconds) {
                    try {
                        Thread.sleep(milliseconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            """;
}
