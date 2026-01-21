package com.enterprise.testgen.template;

/**
 * Selenium WebDriver code templates following Page Object Model best practices.
 * Uses Selenium 4.16.1 (LTE/stable version).
 */
public class SeleniumTemplates {

    public static final String SELENIUM_VERSION = "4.16.1";

    /**
     * Base Page Object class template
     */
    public static final String BASE_PAGE_TEMPLATE = """
            package %s.pages;

            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.WebElement;
            import org.openqa.selenium.support.PageFactory;
            import org.openqa.selenium.support.ui.ExpectedConditions;
            import org.openqa.selenium.support.ui.WebDriverWait;
            import java.time.Duration;

            /**
             * Base Page Object class providing common functionality.
             * All page objects should extend this class.
             */
            public abstract class BasePage {

                protected WebDriver driver;
                protected WebDriverWait wait;
                private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

                public BasePage(WebDriver driver) {
                    this.driver = driver;
                    this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
                    PageFactory.initElements(driver, this);
                }

                protected void waitForElementVisible(WebElement element) {
                    wait.until(ExpectedConditions.visibilityOf(element));
                }

                protected void waitForElementClickable(WebElement element) {
                    wait.until(ExpectedConditions.elementToBeClickable(element));
                }

                protected void click(WebElement element) {
                    waitForElementClickable(element);
                    element.click();
                }

                protected void type(WebElement element, String text) {
                    waitForElementVisible(element);
                    element.clear();
                    element.sendKeys(text);
                }

                protected String getText(WebElement element) {
                    waitForElementVisible(element);
                    return element.getText();
                }

                protected boolean isDisplayed(WebElement element) {
                    try {
                        return element.isDisplayed();
                    } catch (Exception e) {
                        return false;
                    }
                }

                public String getPageTitle() {
                    return driver.getTitle();
                }

                public String getCurrentUrl() {
                    return driver.getCurrentUrl();
                }
            }
            """;

    /**
     * Page Object class template for specific pages
     */
    public static final String PAGE_OBJECT_TEMPLATE = """
            package %s.pages;

            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.WebElement;
            import org.openqa.selenium.support.FindBy;

            /**
             * Page Object for %s.
             * %s
             */
            public class %sPage extends BasePage {

            %s

                public %sPage(WebDriver driver) {
                    super(driver);
                }

            %s
            }
            """;

    /**
     * Base Test class template
     */
    public static final String BASE_TEST_TEMPLATE = """
            package %s.tests;

            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.chrome.ChromeDriver;
            import org.openqa.selenium.chrome.ChromeOptions;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.BeforeEach;
            import java.time.Duration;

            /**
             * Base Test class providing WebDriver setup and teardown.
             * All test classes should extend this class.
             */
            public abstract class BaseTest {

                protected WebDriver driver;
                protected static final String BASE_URL = "%s";

                @BeforeEach
                public void setUp() {
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--start-maximized");
                    options.addArguments("--disable-notifications");
                    // Uncomment for headless execution
                    // options.addArguments("--headless");

                    driver = new ChromeDriver(options);
                    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                }

                @AfterEach
                public void tearDown() {
                    if (driver != null) {
                        driver.quit();
                    }
                }

                protected void navigateTo(String path) {
                    driver.get(BASE_URL + path);
                }
            }
            """;

    /**
     * UI Test class template
     */
    public static final String UI_TEST_TEMPLATE = """
            package %s.tests;

            import %s.pages.*;
            import org.openqa.selenium.By;
            import org.openqa.selenium.WebElement;
            import org.openqa.selenium.support.FindBy;
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.DisplayName;
            import org.junit.jupiter.api.Tag;
            import java.util.List;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * Test Class: %s
             * Test Type: UI Test - Selenium WebDriver
             *
             * Scenario: %s
             *
             * Preconditions:
             * %s
             */
            public class %s extends BaseTest {

            %s
            }
            """;

    /**
     * Individual test method template
     */
    public static final String TEST_METHOD_TEMPLATE = """
                /**
                 * Test: %s
                 *
                 * Test Steps:
                 * %s
                 *
                 * Expected Results:
                 * %s
                 */
                @Test
                @DisplayName("%s")
                public void %s() {
                    // Arrange
                    navigateTo("%s");
                    %s page = new %s(driver);

                    // Act
            %s

                    // Assert
            %s
                }
            """;

    /**
     * WebElement field template
     */
    public static final String ELEMENT_FIELD_TEMPLATE = """
                @FindBy(%s = "%s")
                private WebElement %s;
            """;

    /**
     * Page method template for actions
     */
    public static final String PAGE_METHOD_TEMPLATE = """
                public %s %s(%s) {
                    %s
                    return %s;
                }
            """;

    private SeleniumTemplates() {
        // Utility class
    }
}
