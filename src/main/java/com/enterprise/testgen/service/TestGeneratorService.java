package com.enterprise.testgen.service;

import com.enterprise.testgen.model.*;
import com.enterprise.testgen.template.RestAssuredTemplates;
import com.enterprise.testgen.template.SeleniumTemplates;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core service for generating test cases from requirements.
 * Generates Java automation code using Selenium for UI tests and Rest Assured for API tests.
 */
@Service
public class TestGeneratorService {

    private static final Pattern SCENARIO_PATTERN = Pattern.compile(
            "(?i)(scenario|test case|user story|feature|requirement)[:\\s]*(.+)",
            Pattern.MULTILINE
    );

    private static final Pattern GIVEN_PATTERN = Pattern.compile(
            "(?i)given[:\\s]+(.+)", Pattern.MULTILINE
    );

    private static final Pattern WHEN_PATTERN = Pattern.compile(
            "(?i)when[:\\s]+(.+)", Pattern.MULTILINE
    );

    private static final Pattern THEN_PATTERN = Pattern.compile(
            "(?i)then[:\\s]+(.+)", Pattern.MULTILINE
    );

    private static final Pattern API_ENDPOINT_PATTERN = Pattern.compile(
            "(?i)(GET|POST|PUT|DELETE|PATCH)\\s+(/[\\w/{}\\-]+)"
    );

    // UI Action patterns for natural language parsing
    private static final Pattern NAVIGATE_PATTERN = Pattern.compile(
            "(?i)(navigate|go|open|visit|browse)\\s+(?:to\\s+)?(?:the\\s+)?(?:website\\s+)?(?:page\\s+)?[\"']?([^\"'\\n]+)[\"']?"
    );

    private static final Pattern LOGIN_PATTERN = Pattern.compile(
            "(?i)(login|log\\s*in|sign\\s*in|authenticate)(?:\\s+(?:with|using|as))?(?:\\s+(?:username|user|email)?\\s*[\"']?([^\"'\\n,]+)[\"']?)?(?:\\s*(?:and|,|/)\\s*(?:password)?\\s*[\"']?([^\"'\\n]+)[\"']?)?"
    );

    private static final Pattern CLICK_PATTERN = Pattern.compile(
            "(?i)(click|tap|press|select|choose)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"'\\n]+)[\"']?"
    );

    private static final Pattern ENTER_TEXT_PATTERN = Pattern.compile(
            "(?i)(enter|type|input|fill|write)\\s+[\"']?([^\"'\\n]+)[\"']?\\s+(?:in|into|to)\\s+(?:the\\s+)?[\"']?([^\"'\\n]+)[\"']?"
    );

    private static final Pattern VERIFY_PATTERN = Pattern.compile(
            "(?i)(verify|check|validate|confirm|ensure|assert|see|should\\s+see|should\\s+be|should\\s+have|should\\s+display|should\\s+show)\\s+(?:that\\s+)?(?:the\\s+)?[\"']?([^\"'\\n]+)[\"']?"
    );

    private static final Pattern MENU_PATTERN = Pattern.compile(
            "(?i)(menu|navigation|nav|sidebar|header|footer|dropdown)\\s*(?:items?|links?|options?|elements?)?"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-./]+)"
    );

    /**
     * Get the effective URL to use for test generation.
     * Priority: 1) Explicitly provided URL, 2) URL extracted from content, 3) Default value
     *
     * @param providedUrl URL provided by user (may be null or empty)
     * @param content Requirements content to search for URLs
     * @param defaultUrl Default URL to use if none found
     * @return The effective URL to use
     */
    private String getEffectiveUrl(String providedUrl, String content, String defaultUrl) {
        // If user provided a URL, use it
        if (providedUrl != null && !providedUrl.trim().isEmpty()) {
            return providedUrl.trim();
        }

        // Try to extract URL from content
        if (content != null && !content.isEmpty()) {
            Matcher matcher = URL_PATTERN.matcher(content);
            if (matcher.find()) {
                String extractedUrl = matcher.group(1);
                // Clean up trailing punctuation
                extractedUrl = extractedUrl.replaceAll("[.,;:!?)]+$", "");
                return extractedUrl;
            }
        }

        // Fall back to default
        return defaultUrl;
    }

    /**
     * Generate test cases from requirements content.
     *
     * @param request generation request with content and configuration
     * @return response containing generated test cases
     */
    public GenerationResponse generateTests(GenerationRequest request) {
        long startTime = System.currentTimeMillis();
        List<TestCase> testCases = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String content = request.getRequirementsContent();
        if (content == null || content.trim().isEmpty()) {
            return GenerationResponse.builder()
                    .success(false)
                    .message("No requirements content provided")
                    .testCases(Collections.emptyList())
                    .warnings(List.of("GAP: Requirements content is empty or null"))
                    .build();
        }

        // Parse scenarios from content
        List<ParsedScenario> scenarios = parseScenarios(content);

        if (scenarios.isEmpty()) {
            warnings.add("GAP: No clear test scenarios found in the provided content. " +
                    "Please structure requirements with clear 'Scenario:', 'Given/When/Then', or 'Test Case:' markers.");
        }

        // Generate test cases based on type
        String basePackage = request.getBasePackage() != null ?
                request.getBasePackage() : "com.enterprise.tests";

        for (int i = 0; i < scenarios.size(); i++) {
            ParsedScenario scenario = scenarios.get(i);
            TestCase testCase = createTestCase(scenario, request.getTestType(), basePackage, i + 1, request);
            testCases.add(testCase);
        }

        // Generate base classes (separate files for BasePage and BaseTest)
        if (!testCases.isEmpty()) {
            List<TestCase> baseClasses = generateBaseClasses(request);
            testCases.addAll(0, baseClasses);
        }

        long generationTime = System.currentTimeMillis() - startTime;

        String frameworkInfo = request.getTestType() == TestType.UI ?
                "Selenium WebDriver " + SeleniumTemplates.SELENIUM_VERSION :
                "Rest Assured " + RestAssuredTemplates.RESTASSURED_VERSION;

        return GenerationResponse.builder()
                .success(true)
                .message("Successfully generated " + testCases.size() + " test artifacts")
                .testCases(testCases)
                .totalTests(testCases.size())
                .generationTimeMs(generationTime)
                .frameworkInfo(frameworkInfo)
                .warnings(warnings)
                .build();
    }

    private List<ParsedScenario> parseScenarios(String content) {
        List<ParsedScenario> scenarios = new ArrayList<>();

        // Try to find structured scenarios
        Matcher scenarioMatcher = SCENARIO_PATTERN.matcher(content);
        while (scenarioMatcher.find()) {
            ParsedScenario scenario = new ParsedScenario();
            scenario.setTitle(scenarioMatcher.group(2).trim());

            // Find the content between this scenario and the next
            int start = scenarioMatcher.end();
            int end = content.length();

            // Look for next scenario marker
            Matcher nextMatcher = SCENARIO_PATTERN.matcher(content.substring(start));
            if (nextMatcher.find()) {
                end = start + nextMatcher.start();
            }

            String scenarioContent = content.substring(start, end);
            parseScenarioDetails(scenario, scenarioContent);
            scenarios.add(scenario);
        }

        // If no structured scenarios found, try to extract from paragraphs
        if (scenarios.isEmpty()) {
            scenarios.addAll(extractScenariosFromParagraphs(content));
        }

        return scenarios;
    }

    private void parseScenarioDetails(ParsedScenario scenario, String content) {
        // Extract Given (preconditions)
        Matcher givenMatcher = GIVEN_PATTERN.matcher(content);
        while (givenMatcher.find()) {
            scenario.getPreconditions().add(givenMatcher.group(1).trim());
        }

        // Extract When (actions)
        Matcher whenMatcher = WHEN_PATTERN.matcher(content);
        while (whenMatcher.find()) {
            scenario.getActions().add(whenMatcher.group(1).trim());
        }

        // Extract Then (expectations)
        Matcher thenMatcher = THEN_PATTERN.matcher(content);
        while (thenMatcher.find()) {
            scenario.getExpectations().add(thenMatcher.group(1).trim());
        }

        // Extract API endpoints if present
        Matcher apiMatcher = API_ENDPOINT_PATTERN.matcher(content);
        while (apiMatcher.find()) {
            scenario.getApiEndpoints().add(new ApiEndpoint(
                    apiMatcher.group(1).toUpperCase(),
                    apiMatcher.group(2)
            ));
        }

        // Parse natural language UI actions if no structured format found
        if (scenario.getActions().isEmpty()) {
            parseNaturalLanguageActions(scenario, content);
        }

        // Store raw content for reference
        scenario.setRawContent(content.trim());
    }

    private void parseNaturalLanguageActions(ParsedScenario scenario, String content) {
        // Extract URL for navigation
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        if (urlMatcher.find()) {
            scenario.setTargetUrl(urlMatcher.group(1));
        }

        // Extract navigate actions
        Matcher navMatcher = NAVIGATE_PATTERN.matcher(content);
        while (navMatcher.find()) {
            String target = navMatcher.group(2).trim();
            scenario.getUiActions().add(new UiAction("NAVIGATE", target, null));
            if (scenario.getPreconditions().isEmpty()) {
                scenario.getPreconditions().add("Browser is open");
            }
        }

        // Extract login actions
        Matcher loginMatcher = LOGIN_PATTERN.matcher(content);
        while (loginMatcher.find()) {
            String username = loginMatcher.group(2) != null ? loginMatcher.group(2).trim() : "testuser";
            String password = loginMatcher.group(3) != null ? loginMatcher.group(3).trim() : "testpassword";
            scenario.getUiActions().add(new UiAction("LOGIN", username, password));
        }

        // Extract click actions
        Matcher clickMatcher = CLICK_PATTERN.matcher(content);
        while (clickMatcher.find()) {
            String element = clickMatcher.group(2).trim();
            scenario.getUiActions().add(new UiAction("CLICK", element, null));
        }

        // Extract text entry actions
        Matcher enterMatcher = ENTER_TEXT_PATTERN.matcher(content);
        while (enterMatcher.find()) {
            String text = enterMatcher.group(2).trim();
            String element = enterMatcher.group(3).trim();
            scenario.getUiActions().add(new UiAction("ENTER", element, text));
        }

        // Extract verify/check actions
        Matcher verifyMatcher = VERIFY_PATTERN.matcher(content);
        while (verifyMatcher.find()) {
            String element = verifyMatcher.group(2).trim();
            scenario.getUiActions().add(new UiAction("VERIFY", element, null));
            scenario.getExpectations().add("Verify " + element);
        }

        // Check for menu verification
        Matcher menuMatcher = MENU_PATTERN.matcher(content);
        if (menuMatcher.find()) {
            scenario.getUiActions().add(new UiAction("VERIFY_MENU", menuMatcher.group(0).trim(), null));
            if (!scenario.getExpectations().stream().anyMatch(e -> e.toLowerCase().contains("menu"))) {
                scenario.getExpectations().add("Menu items are visible and accessible");
            }
        }

        // Convert UI actions to regular actions for step generation
        for (UiAction action : scenario.getUiActions()) {
            scenario.getActions().add(formatUiAction(action));
        }
    }

    private String formatUiAction(UiAction action) {
        return switch (action.type()) {
            case "NAVIGATE" -> "Navigate to " + action.target();
            case "LOGIN" -> "Login with username '" + action.target() + "' and password";
            case "CLICK" -> "Click on " + action.target();
            case "ENTER" -> "Enter '" + action.data() + "' into " + action.target();
            case "VERIFY" -> "Verify " + action.target();
            case "VERIFY_MENU" -> "Verify " + action.target() + " items are displayed";
            default -> action.target();
        };
    }

    private List<ParsedScenario> extractScenariosFromParagraphs(String content) {
        List<ParsedScenario> scenarios = new ArrayList<>();
        String[] paragraphs = content.split("\n\n+");

        for (int i = 0; i < paragraphs.length; i++) {
            String para = paragraphs[i].trim();
            if (para.length() > 50 && containsTestableContent(para)) {
                ParsedScenario scenario = new ParsedScenario();
                scenario.setTitle("Extracted Scenario " + (i + 1));
                scenario.setRawContent(para);
                scenarios.add(scenario);
            }
        }

        return scenarios;
    }

    private boolean containsTestableContent(String content) {
        String lower = content.toLowerCase();
        return lower.contains("user") || lower.contains("should") ||
                lower.contains("must") || lower.contains("verify") ||
                lower.contains("validate") || lower.contains("test") ||
                lower.contains("click") || lower.contains("enter") ||
                lower.contains("api") || lower.contains("endpoint") ||
                lower.contains("request") || lower.contains("response");
    }

    private TestCase createTestCase(ParsedScenario scenario, TestType testType,
                                     String basePackage, int index, GenerationRequest request) {
        String testName = generateTestName(scenario.getTitle(), index);
        String className = generateClassName(scenario.getTitle(), index);

        List<TestStep> steps = generateTestSteps(scenario);
        List<String> gaps = identifyGaps(scenario);

        String automationCode = testType == TestType.UI ?
                generateSeleniumCode(scenario, className, basePackage, steps, request) :
                generateRestAssuredCode(scenario, className, basePackage, steps, request);

        return TestCase.builder()
                .testId("TC-" + String.format("%03d", index))
                .testName(testName)
                .testType(testType)
                .scenarioDescription(scenario.getTitle())
                .preconditions(scenario.getPreconditions().isEmpty() ?
                        List.of("Application is accessible", "Test data is available") :
                        scenario.getPreconditions())
                .testSteps(steps)
                .expectedResults(scenario.getExpectations().isEmpty() ?
                        List.of("Test completes successfully", "Expected state is achieved") :
                        scenario.getExpectations())
                .automationCode(automationCode)
                .className(className)
                .packageName(basePackage + ".tests")
                .identifiedGaps(gaps)
                .build();
    }

    private String generateTestName(String title, int index) {
        if (title == null || title.isEmpty()) {
            return "test_scenario_" + index;
        }
        return "test_" + title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "")
                .substring(0, Math.min(50, title.length()));
    }

    private String generateClassName(String title, int index) {
        if (title == null || title.isEmpty()) {
            return "Scenario" + index + "Test";
        }
        String className = Arrays.stream(title.split("[^a-zA-Z0-9]+"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .reduce("", String::concat);
        return (className.isEmpty() ? "Scenario" + index : className) + "Test";
    }

    private List<TestStep> generateTestSteps(ParsedScenario scenario) {
        List<TestStep> steps = new ArrayList<>();
        int stepNum = 1;

        // Add precondition steps
        for (String precondition : scenario.getPreconditions()) {
            steps.add(TestStep.builder()
                    .stepNumber(stepNum++)
                    .action("Verify precondition: " + precondition)
                    .testData("")
                    .expectedResult("Precondition is met")
                    .build());
        }

        // Add action steps
        for (String action : scenario.getActions()) {
            steps.add(TestStep.builder()
                    .stepNumber(stepNum++)
                    .action(action)
                    .testData(extractTestData(action))
                    .expectedResult("Action completed successfully")
                    .build());
        }

        // Add expectation steps
        for (String expectation : scenario.getExpectations()) {
            steps.add(TestStep.builder()
                    .stepNumber(stepNum++)
                    .action("Verify: " + expectation)
                    .testData("")
                    .expectedResult(expectation)
                    .build());
        }

        // If no structured steps, generate from raw content
        if (steps.isEmpty() && scenario.getRawContent() != null) {
            steps.add(TestStep.builder()
                    .stepNumber(1)
                    .action("Execute scenario as described")
                    .testData("See scenario description")
                    .expectedResult("Scenario completes successfully")
                    .build());
        }

        return steps;
    }

    private String extractTestData(String action) {
        // Extract quoted strings or values
        Pattern dataPattern = Pattern.compile("\"([^\"]+)\"|'([^']+)'|\\b(\\d+)\\b");
        Matcher matcher = dataPattern.matcher(action);
        StringBuilder data = new StringBuilder();
        while (matcher.find()) {
            if (data.length() > 0) data.append(", ");
            data.append(matcher.group(1) != null ? matcher.group(1) :
                    matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
        }
        return data.toString();
    }

    private List<String> identifyGaps(ParsedScenario scenario) {
        List<String> gaps = new ArrayList<>();

        if (scenario.getPreconditions().isEmpty()) {
            gaps.add("GAP: No preconditions specified. Assuming default test environment.");
        }

        if (scenario.getActions().isEmpty()) {
            gaps.add("GAP: No clear actions/steps defined. Test steps inferred from description.");
        }

        if (scenario.getExpectations().isEmpty()) {
            gaps.add("GAP: No expected results specified. Generic assertions will be used.");
        }

        if (scenario.getRawContent() != null && scenario.getRawContent().contains("TODO")) {
            gaps.add("GAP: TODO markers found in requirements. Implementation may be incomplete.");
        }

        return gaps;
    }

    private List<TestCase> generateBaseClasses(GenerationRequest request) {
        List<TestCase> baseClasses = new ArrayList<>();
        String basePackage = request.getBasePackage() != null ?
                request.getBasePackage() : "com.enterprise.tests";

        TestType testType = request.getTestType();

        if (testType == TestType.UI) {
            String url = getEffectiveUrl(request.getApplicationUrl(), request.getRequirementsContent(), "http://localhost:8080");

            // Generate BasePage.java
            String basePageCode = generateBasePageCode(basePackage, request);
            baseClasses.add(TestCase.builder()
                    .testId("TC-000-A")
                    .testName("BasePage.java")
                    .testType(testType)
                    .scenarioDescription("Base Page Object class with common methods")
                    .preconditions(List.of("Java 17+", "Selenium WebDriver " + SeleniumTemplates.SELENIUM_VERSION))
                    .testSteps(List.of())
                    .expectedResults(List.of("Page Object infrastructure ready"))
                    .automationCode(basePageCode)
                    .className("BasePage")
                    .packageName(basePackage + ".pages")
                    .identifiedGaps(List.of())
                    .build());

            // Generate BaseTest.java
            String baseTestCode = generateBaseTestCode(basePackage, url, request);
            baseClasses.add(TestCase.builder()
                    .testId("TC-000-B")
                    .testName("BaseTest.java")
                    .testType(testType)
                    .scenarioDescription("Base Test class with setup and teardown")
                    .preconditions(List.of("Java 17+", "Selenium WebDriver " + SeleniumTemplates.SELENIUM_VERSION))
                    .testSteps(List.of())
                    .expectedResults(List.of("Test infrastructure ready"))
                    .automationCode(baseTestCode)
                    .className("BaseTest")
                    .packageName(basePackage + ".tests")
                    .identifiedGaps(List.of())
                    .build());
        } else {
            String url = getEffectiveUrl(request.getApiBaseUrl(), request.getRequirementsContent(), "http://localhost:8080/api");
            String code = String.format(RestAssuredTemplates.BASE_API_TEST_TEMPLATE, basePackage, url);

            baseClasses.add(TestCase.builder()
                    .testId("TC-000")
                    .testName("BaseApiTest.java")
                    .testType(testType)
                    .scenarioDescription("Base API Test class")
                    .preconditions(List.of("Java 17+", "Rest Assured " + RestAssuredTemplates.RESTASSURED_VERSION))
                    .testSteps(List.of())
                    .expectedResults(List.of("API test infrastructure ready"))
                    .automationCode(code)
                    .className("BaseApiTest")
                    .packageName(basePackage + ".tests")
                    .identifiedGaps(List.of())
                    .build());
        }

        return baseClasses;
    }

    private String generateBasePageCode(String basePackage, GenerationRequest request) {
        String waitStrategy = request.getWaitStrategy() != null ? request.getWaitStrategy() : "explicit";
        boolean handleDynamic = Boolean.TRUE.equals(request.getHandleDynamic());

        StringBuilder code = new StringBuilder();
        code.append(String.format("""
            package %s.pages;

            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.WebElement;
            import org.openqa.selenium.support.PageFactory;
            import org.openqa.selenium.support.ui.ExpectedConditions;
            import org.openqa.selenium.support.ui.WebDriverWait;
            import org.openqa.selenium.JavascriptExecutor;
            import java.time.Duration;

            /**
             * Base Page Object class providing common functionality.
             * All page objects should extend this class.
             *
             * Wait Strategy: %s
             * Dynamic Element Handling: %s
             */
            public abstract class BasePage {

                protected WebDriver driver;
                protected WebDriverWait wait;
                protected JavascriptExecutor js;
                private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
                private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

                public BasePage(WebDriver driver) {
                    this.driver = driver;
                    this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
                    this.js = (JavascriptExecutor) driver;
                    PageFactory.initElements(driver, this);
                }
            """, basePackage, waitStrategy.toUpperCase(), handleDynamic ? "ENABLED" : "DISABLED"));

        // Add wait methods based on strategy
        if ("smart".equals(waitStrategy)) {
            code.append("""

                /**
                 * Smart wait - AI-powered adaptive waiting that adjusts based on page state.
                 */
                protected void smartWait(WebElement element) {
                    // Wait for page to be fully loaded
                    wait.until(driver -> js.executeScript("return document.readyState").equals("complete"));
                    // Wait for jQuery if present
                    try {
                        wait.until(driver -> (Boolean) js.executeScript("return (typeof jQuery === 'undefined') || (jQuery.active === 0)"));
                    } catch (Exception ignored) {}
                    // Wait for element to be visible and stable
                    wait.until(ExpectedConditions.visibilityOf(element));
                    // Additional stability check - element position should be stable
                    waitForElementStability(element);
                }

                private void waitForElementStability(WebElement element) {
                    try {
                        org.openqa.selenium.Point initialLocation = element.getLocation();
                        Thread.sleep(100);
                        org.openqa.selenium.Point finalLocation = element.getLocation();
                        if (!initialLocation.equals(finalLocation)) {
                            Thread.sleep(200); // Wait a bit more if element is still moving
                        }
                    } catch (Exception ignored) {}
                }
            """);
        } else if ("implicit".equals(waitStrategy)) {
            code.append("""

                /**
                 * Implicit wait - relies on driver's implicit wait setting.
                 */
                protected void waitForElement(WebElement element) {
                    // Implicit wait is set in BaseTest, just check element is present
                    element.isDisplayed();
                }
            """);
        } else {
            // explicit (default)
            code.append("""

                /**
                 * Explicit wait - condition-based waiting for elements.
                 */
                protected void waitForElementVisible(WebElement element) {
                    wait.until(ExpectedConditions.visibilityOf(element));
                }

                protected void waitForElementClickable(WebElement element) {
                    wait.until(ExpectedConditions.elementToBeClickable(element));
                }
            """);
        }

        // Add dynamic element handling if enabled
        if (handleDynamic) {
            code.append("""

                /**
                 * Handle dynamic elements with retry mechanism.
                 */
                protected void clickWithRetry(WebElement element, int maxRetries) {
                    int attempts = 0;
                    while (attempts < maxRetries) {
                        try {
                            wait.until(ExpectedConditions.elementToBeClickable(element));
                            element.click();
                            return;
                        } catch (org.openqa.selenium.StaleElementReferenceException e) {
                            attempts++;
                            if (attempts >= maxRetries) throw e;
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                        }
                    }
                }

                protected void scrollToElement(WebElement element) {
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            """);
        }

        // Add common methods
        code.append("""

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

                protected void waitForElementClickable(WebElement element) {
                    wait.until(ExpectedConditions.elementToBeClickable(element));
                }

                protected void waitForElementVisible(WebElement element) {
                    wait.until(ExpectedConditions.visibilityOf(element));
                }

                public String getPageTitle() {
                    return driver.getTitle();
                }

                public String getCurrentUrl() {
                    return driver.getCurrentUrl();
                }
            }
            """);

        return code.toString();
    }

    private String generateBaseTestCode(String basePackage, String baseUrl, GenerationRequest request) {
        String testCategory = request.getTestCategory() != null ? request.getTestCategory() : "smoke";
        String waitStrategy = request.getWaitStrategy() != null ? request.getWaitStrategy() : "explicit";

        return String.format("""
            package %s.tests;

            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.chrome.ChromeDriver;
            import org.openqa.selenium.chrome.ChromeOptions;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.Tag;
            import java.time.Duration;

            /**
             * Base Test class providing WebDriver setup and teardown.
             * All test classes should extend this class.
             *
             * Test Category: %s
             * Wait Strategy: %s
             */
            @Tag("%s")
            public abstract class BaseTest {

                protected WebDriver driver;
                protected static final String BASE_URL = "%s";

                @BeforeEach
                public void setUp() {
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--start-maximized");
                    options.addArguments("--disable-notifications");
                    options.addArguments("--disable-popup-blocking");
                    // Uncomment for headless execution
                    // options.addArguments("--headless=new");

                    driver = new ChromeDriver(options);
                    %s
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

                protected void navigateToUrl(String url) {
                    driver.get(url);
                }
            }
            """, basePackage, testCategory.toUpperCase(), waitStrategy.toUpperCase(), testCategory,
                baseUrl,
                "implicit".equals(waitStrategy) ?
                        "driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));" :
                        "// Using explicit waits - see BasePage");
    }

    private String generateSeleniumCode(ParsedScenario scenario, String className,
                                         String basePackage, List<TestStep> steps, GenerationRequest request) {
        String methodName = generateTestName(scenario.getTitle(), 0);
        String pageName = className.replace("Test", "");
        String testCategory = request.getTestCategory() != null ? request.getTestCategory() : "smoke";
        String assertionStrictness = request.getAssertionStrictness() != null ? request.getAssertionStrictness() : "medium";

        StringBuilder stepsDoc = new StringBuilder();
        StringBuilder actCode = new StringBuilder();
        StringBuilder assertCode = new StringBuilder();
        StringBuilder elementLocators = new StringBuilder();

        // Determine the navigation URL
        String navUrl = scenario.getTargetUrl() != null && !scenario.getTargetUrl().isEmpty() ? scenario.getTargetUrl() : "BASE_URL";

        // Generate code based on UI actions if present
        if (!scenario.getUiActions().isEmpty()) {
            int locatorIndex = 1;
            for (UiAction action : scenario.getUiActions()) {
                stepsDoc.append(" *   ").append(locatorIndex).append(". ")
                        .append(formatUiAction(action)).append("\n");

                switch (action.type()) {
                    case "NAVIGATE" -> {
                        // Navigation is handled by navigateTo() call
                        if (action.target().startsWith("http")) {
                            navUrl = action.target();
                        }
                    }
                    case "LOGIN" -> {
                        String usernameField = "usernameField_" + locatorIndex;
                        String passwordField = "passwordField_" + locatorIndex;
                        String loginBtn = "loginButton_" + locatorIndex;

                        elementLocators.append(generateLocatorCode(usernameField, "id", "username"));
                        elementLocators.append(generateLocatorCode(passwordField, "id", "password"));
                        elementLocators.append(generateLocatorCode(loginBtn, "xpath", "//button[contains(text(),'Login') or contains(text(),'Sign in') or @type='submit']"));

                        actCode.append("        // Login with credentials\n");
                        actCode.append("        type(").append(usernameField).append(", \"").append(action.target()).append("\");\n");
                        actCode.append("        type(").append(passwordField).append(", \"").append(action.data() != null ? action.data() : "testpassword").append("\");\n");
                        actCode.append("        click(").append(loginBtn).append(");\n\n");
                    }
                    case "CLICK" -> {
                        String elementName = toVariableName(action.target()) + "_" + locatorIndex;
                        String xpath = generateXpathForElement(action.target());
                        elementLocators.append(generateLocatorCode(elementName, "xpath", xpath));

                        actCode.append("        // Click on ").append(action.target()).append("\n");
                        actCode.append("        click(").append(elementName).append(");\n\n");
                    }
                    case "ENTER" -> {
                        String elementName = toVariableName(action.target()) + "_" + locatorIndex;
                        String xpath = generateXpathForElement(action.target());
                        elementLocators.append(generateLocatorCode(elementName, "xpath", xpath));

                        actCode.append("        // Enter text into ").append(action.target()).append("\n");
                        actCode.append("        type(").append(elementName).append(", \"").append(action.data()).append("\");\n\n");
                    }
                    case "VERIFY" -> {
                        String elementName = toVariableName(action.target()) + "_" + locatorIndex;
                        String xpath = generateXpathForElement(action.target());
                        elementLocators.append(generateLocatorCode(elementName, "xpath", xpath));

                        assertCode.append("        // Verify ").append(action.target()).append("\n");
                        assertCode.append("        assertTrue(isDisplayed(").append(elementName).append("), \"").append(action.target()).append(" should be visible\");\n");
                    }
                    case "VERIFY_MENU" -> {
                        String menuElement = "menuContainer_" + locatorIndex;
                        elementLocators.append(generateLocatorCode(menuElement, "xpath", "//nav | //ul[contains(@class,'menu') or contains(@class,'nav')] | //*[@role='navigation'] | //header//ul"));

                        assertCode.append("        // Verify menu/navigation is displayed\n");
                        assertCode.append("        assertTrue(isDisplayed(").append(menuElement).append("), \"Menu should be visible\");\n\n");

                        // Add code to get and verify menu items
                        assertCode.append("        // Get all menu items and verify they are present\n");
                        assertCode.append("        List<WebElement> menuItems = driver.findElements(By.xpath(\"//nav//a | //ul[contains(@class,'menu') or contains(@class,'nav')]//a | //*[@role='navigation']//a\"));\n");
                        assertCode.append("        assertFalse(menuItems.isEmpty(), \"Menu should contain items\");\n\n");
                        assertCode.append("        // Log menu items found\n");
                        assertCode.append("        System.out.println(\"Found \" + menuItems.size() + \" menu items:\");\n");
                        assertCode.append("        for (WebElement item : menuItems) {\n");
                        assertCode.append("            String text = item.getText().trim();\n");
                        assertCode.append("            String href = item.getAttribute(\"href\");\n");
                        assertCode.append("            if (!text.isEmpty()) {\n");
                        assertCode.append("                System.out.println(\"  - \" + text + \" -> \" + href);\n");
                        assertCode.append("            }\n");
                        assertCode.append("        }\n");
                    }
                }
                locatorIndex++;
            }
        } else {
            // Fallback to step-based generation
            for (TestStep step : steps) {
                stepsDoc.append(" *   ").append(step.getStepNumber())
                        .append(". ").append(step.getAction()).append("\n");

                if (step.getAction().toLowerCase().contains("click")) {
                    actCode.append("        // ").append(step.getAction()).append("\n");
                    actCode.append("        // click(elementLocator);\n");
                } else if (step.getAction().toLowerCase().contains("enter") ||
                        step.getAction().toLowerCase().contains("type")) {
                    actCode.append("        // ").append(step.getAction()).append("\n");
                    actCode.append("        // type(elementLocator, \"").append(step.getTestData()).append("\");\n");
                } else if (step.getAction().toLowerCase().contains("verify")) {
                    assertCode.append("        // ").append(step.getAction()).append("\n");
                    assertCode.append("        // assertTrue(isDisplayed(elementLocator));\n");
                }
            }
        }

        if (actCode.length() == 0) {
            actCode.append("        // Perform test actions\n");
            actCode.append("        // Add your test actions here based on the scenario\n");
        }
        if (assertCode.length() == 0) {
            assertCode.append("        // Verify expected results\n");
            assertCode.append("        assertNotNull(driver.getTitle(), \"Page should have a title\");\n");
        }

        String preconditions = scenario.getPreconditions().isEmpty() ?
                " *   - Application is running and accessible" :
                scenario.getPreconditions().stream()
                        .map(p -> " *   - " + p)
                        .reduce("", (a, b) -> a + "\n" + b);

        String expectedResults = scenario.getExpectations().isEmpty() ?
                " *   - Test completes successfully" :
                scenario.getExpectations().stream()
                        .map(e -> " *   - " + e)
                        .reduce("", (a, b) -> a + "\n" + b);

        // Generate the test with element locators
        String testMethodWithLocators = generateTestMethodWithLocators(
                scenario.getTitle(),
                stepsDoc.toString(),
                expectedResults,
                methodName,
                navUrl,
                elementLocators.toString(),
                actCode.toString(),
                assertCode.toString()
        );

        // Add @Tag annotation for test category
        String tagAnnotation = String.format("@Tag(\"%s\")\n", testCategory);

        return String.format(SeleniumTemplates.UI_TEST_TEMPLATE,
                basePackage,
                basePackage,
                className,
                scenario.getTitle() + " [" + testCategory.toUpperCase() + "]",
                preconditions,
                className,
                tagAnnotation + testMethodWithLocators
        );
    }

    private String generateLocatorCode(String fieldName, String locatorType, String locatorValue) {
        return String.format("    @FindBy(%s = \"%s\")\n    private WebElement %s;\n\n",
                locatorType, escapeJavaString(locatorValue), fieldName);
    }

    private String generateXpathForElement(String elementDescription) {
        String lower = elementDescription.toLowerCase();

        // Common element patterns
        if (lower.contains("button")) {
            return "//button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')] | //input[@type='button' or @type='submit'][contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')]";
        } else if (lower.contains("link") || lower.contains("menu item")) {
            return "//a[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')] | //a[contains(translate(@href,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')]";
        } else if (lower.contains("input") || lower.contains("field") || lower.contains("textbox")) {
            return "//input[contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')] | //input[contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')] | //input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')]";
        } else {
            // Generic text-based locator
            return "//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')] | //*[contains(translate(@title,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" +
                    extractKeyword(lower) + "')]";
        }
    }

    private String extractKeyword(String text) {
        // Remove common words and extract meaningful keyword
        return text.replaceAll("(?i)(the|a|an|button|link|field|input|textbox|menu|item|on|is|are|should|be|displayed|visible)", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private String toVariableName(String text) {
        return text.replaceAll("[^a-zA-Z0-9]", "")
                .replaceAll("^\\d+", "")
                .substring(0, Math.min(20, text.replaceAll("[^a-zA-Z0-9]", "").length()));
    }

    private String escapeJavaString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String generateTestMethodWithLocators(String title, String stepsDoc, String expectedResults,
                                                   String methodName, String navUrl,
                                                   String elementLocators, String actCode, String assertCode) {
        StringBuilder method = new StringBuilder();

        // Add element locators as fields
        if (!elementLocators.isEmpty()) {
            method.append(elementLocators).append("\n");
        }

        // Add test method
        method.append(String.format("""
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
                    // Arrange - Navigate to the page
                    String url = "BASE_URL".equals("%s") ? BASE_URL : "%s";
                    driver.get(url);

                    // Act - Perform test actions
            %s
                    // Assert - Verify expected results
            %s
                }
            """, title, stepsDoc, expectedResults, title, methodName, navUrl, navUrl, actCode, assertCode));

        return method.toString();
    }

    private String generateRestAssuredCode(ParsedScenario scenario, String className,
                                            String basePackage, List<TestStep> steps, GenerationRequest request) {
        String testCategory = request.getTestCategory() != null ? request.getTestCategory() : "smoke";
        StringBuilder testMethods = new StringBuilder();

        // Check for API endpoints in scenario
        if (!scenario.getApiEndpoints().isEmpty()) {
            for (ApiEndpoint endpoint : scenario.getApiEndpoints()) {
                testMethods.append(generateApiTestMethod(scenario, endpoint, steps));
            }
        } else {
            // Generate generic API test
            testMethods.append(generateGenericApiTest(scenario, steps));
        }

        String preconditions = scenario.getPreconditions().isEmpty() ?
                " *   - API server is running and accessible" :
                scenario.getPreconditions().stream()
                        .map(p -> " *   - " + p)
                        .reduce("", (a, b) -> a + "\n" + b);

        return String.format(RestAssuredTemplates.API_TEST_TEMPLATE,
                basePackage,
                className,
                scenario.getTitle(),
                preconditions,
                className,
                testMethods.toString()
        );
    }

    private String generateApiTestMethod(ParsedScenario scenario, ApiEndpoint endpoint,
                                          List<TestStep> steps) {
        String methodName = "test_" + endpoint.method().toLowerCase() + "_" +
                endpoint.path().replaceAll("[^a-zA-Z0-9]", "_");

        StringBuilder stepsDoc = new StringBuilder();
        for (TestStep step : steps) {
            stepsDoc.append(" *   ").append(step.getStepNumber())
                    .append(". ").append(step.getAction()).append("\n");
        }

        String expectedResults = scenario.getExpectations().isEmpty() ?
                " *   - API returns expected response" :
                scenario.getExpectations().stream()
                        .map(e -> " *   - " + e)
                        .reduce("", (a, b) -> a + "\n" + b);

        return switch (endpoint.method()) {
            case "POST" -> String.format(RestAssuredTemplates.POST_TEST_TEMPLATE,
                    scenario.getTitle(),
                    stepsDoc.toString(),
                    expectedResults,
                    scenario.getTitle(),
                    methodName,
                    endpoint.path(),
                    "{\n            \"key\": \"value\"\n        }",
                    201,
                    ".body(\"id\", notNullValue())"
            );
            case "PUT" -> String.format(RestAssuredTemplates.PUT_TEST_TEMPLATE,
                    scenario.getTitle(),
                    stepsDoc.toString(),
                    expectedResults,
                    scenario.getTitle(),
                    methodName,
                    endpoint.path(),
                    "{\n            \"key\": \"updatedValue\"\n        }",
                    200,
                    ".body(\"key\", equalTo(\"updatedValue\"))"
            );
            case "DELETE" -> String.format(RestAssuredTemplates.DELETE_TEST_TEMPLATE,
                    scenario.getTitle(),
                    stepsDoc.toString(),
                    expectedResults,
                    scenario.getTitle(),
                    methodName,
                    endpoint.path(),
                    204,
                    ""
            );
            default -> String.format(RestAssuredTemplates.GET_TEST_TEMPLATE,
                    scenario.getTitle(),
                    stepsDoc.toString(),
                    expectedResults,
                    scenario.getTitle(),
                    methodName,
                    endpoint.path(),
                    "",
                    200,
                    ".body(\"$\", notNullValue())"
            );
        };
    }

    private String generateGenericApiTest(ParsedScenario scenario, List<TestStep> steps) {
        String methodName = generateTestName(scenario.getTitle(), 0);

        StringBuilder stepsDoc = new StringBuilder();
        for (TestStep step : steps) {
            stepsDoc.append(" *   ").append(step.getStepNumber())
                    .append(". ").append(step.getAction()).append("\n");
        }

        String expectedResults = scenario.getExpectations().isEmpty() ?
                " *   - API returns expected response" :
                scenario.getExpectations().stream()
                        .map(e -> " *   - " + e)
                        .reduce("", (a, b) -> a + "\n" + b);

        return String.format(RestAssuredTemplates.GET_TEST_TEMPLATE,
                scenario.getTitle(),
                stepsDoc.toString(),
                expectedResults,
                scenario.getTitle(),
                methodName,
                "BASE_URL + \"/api/v1/resource\"",
                "// TODO: Add query parameters if needed",
                200,
                "// TODO: Add response body assertions"
        );
    }

    // Inner classes for parsing
    private static class ParsedScenario {
        private String title = "";
        private String rawContent;
        private String targetUrl;
        private List<String> preconditions = new ArrayList<>();
        private List<String> actions = new ArrayList<>();
        private List<String> expectations = new ArrayList<>();
        private List<ApiEndpoint> apiEndpoints = new ArrayList<>();
        private List<UiAction> uiActions = new ArrayList<>();

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getRawContent() { return rawContent; }
        public void setRawContent(String rawContent) { this.rawContent = rawContent; }
        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        public List<String> getPreconditions() { return preconditions; }
        public List<String> getActions() { return actions; }
        public List<String> getExpectations() { return expectations; }
        public List<ApiEndpoint> getApiEndpoints() { return apiEndpoints; }
        public List<UiAction> getUiActions() { return uiActions; }
    }

    private record ApiEndpoint(String method, String path) {}
    private record UiAction(String type, String target, String data) {}
}
