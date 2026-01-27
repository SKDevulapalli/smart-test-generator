package com.enterprise.testgen.service;

import com.enterprise.testgen.model.PageAnalysis;
import com.enterprise.testgen.model.PageAnalysis.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing live web pages using headless browser.
 * Extracts DOM structure, interactive elements, and infers page purpose.
 */
@Service
public class WebPageAnalyzerService {

    private static final Set<String> LOGIN_INDICATORS = Set.of(
            "login", "signin", "sign-in", "log-in", "authenticate", "password", "username"
    );
    private static final Set<String> SIGNUP_INDICATORS = Set.of(
            "signup", "sign-up", "register", "create-account", "join", "registration"
    );
    private static final Set<String> SEARCH_INDICATORS = Set.of(
            "search", "query", "find", "lookup"
    );
    private static final Set<String> CHECKOUT_INDICATORS = Set.of(
            "checkout", "payment", "pay", "cart", "order", "purchase"
    );
    private static final Set<String> CONTACT_INDICATORS = Set.of(
            "contact", "message", "inquiry", "feedback", "support"
    );

    /**
     * Analyze a live web page and extract all relevant information.
     */
    public PageAnalysis analyzePage(String url) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        // Check for pre-installed ChromeDriver (Docker/Railway environment)
        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        String chromeBinPath = System.getenv("CHROME_BIN");

        System.out.println("[WebPageAnalyzer] Environment check:");
        System.out.println("  CHROMEDRIVER_PATH=" + chromeDriverPath);
        System.out.println("  CHROME_BIN=" + chromeBinPath);

        // Possible ChromeDriver paths (in order of preference)
        String[] possibleDriverPaths = {
            chromeDriverPath,
            "/usr/local/bin/chromedriver",
            "/usr/bin/chromedriver"
        };

        // Possible Chrome binary paths (in order of preference)
        String[] possibleChromePaths = {
            chromeBinPath,
            "/usr/bin/google-chrome-stable",
            "/usr/bin/google-chrome",
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium"
        };

        // Find and set ChromeDriver path
        System.out.println("[WebPageAnalyzer] Checking ChromeDriver paths:");
        boolean driverFound = false;
        for (String path : possibleDriverPaths) {
            if (path != null && !path.isEmpty()) {
                boolean exists = new File(path).exists();
                System.out.println("  " + path + " -> " + (exists ? "EXISTS" : "not found"));
                if (exists) {
                    System.out.println("[WebPageAnalyzer] Using ChromeDriver at: " + path);
                    System.setProperty("webdriver.chrome.driver", path);
                    driverFound = true;
                    break;
                }
            }
        }

        if (!driverFound) {
            System.out.println("[WebPageAnalyzer] No pre-installed ChromeDriver found, using WebDriverManager");
            WebDriverManager.chromedriver().setup();
        }

        ChromeOptions options = new ChromeOptions();

        // Find and set Chrome binary path
        System.out.println("[WebPageAnalyzer] Checking Chrome binary paths:");
        String foundChromePath = null;
        for (String path : possibleChromePaths) {
            if (path != null && !path.isEmpty()) {
                boolean exists = new File(path).exists();
                System.out.println("  " + path + " -> " + (exists ? "EXISTS" : "not found"));
                if (exists) {
                    System.out.println("[WebPageAnalyzer] Using Chrome binary: " + path);
                    options.setBinary(path);
                    foundChromePath = path;
                    break;
                }
            }
        }

        if (foundChromePath == null) {
            System.out.println("[WebPageAnalyzer] WARNING: No Chrome binary found in expected paths!");
        }

        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        // Additional options for containerized environments (Railway, Docker)
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--single-process");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-translate");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--mute-audio");
        options.addArguments("--safebrowsing-disable-auto-update");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

            driver.get(url);

            // Wait for page to stabilize - reduced timeout for faster response
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));

            // Extract page information
            String title = driver.getTitle();
            String pageSource = driver.getPageSource().toLowerCase();

            // Analyze elements
            List<PageElement> elements = extractInteractiveElements(driver);
            List<FormInfo> forms = extractForms(driver);
            List<LinkInfo> links = extractLinks(driver, url);

            // Infer page purpose
            String pagePurpose = inferPagePurpose(title, pageSource, elements, forms);
            String pageDescription = generatePageDescription(pagePurpose, forms, elements);

            // Infer user flows and edge cases
            List<String> inferredFlows = inferUserFlows(pagePurpose, forms, elements);
            List<String> edgeCases = identifyEdgeCases(pagePurpose, elements, forms);
            List<String> securityConsiderations = identifySecurityConsiderations(pagePurpose, elements, forms);

            long analysisTime = System.currentTimeMillis() - startTime;

            return PageAnalysis.builder()
                    .url(url)
                    .title(title)
                    .pagePurpose(pagePurpose)
                    .pageDescription(pageDescription)
                    .elements(elements)
                    .forms(forms)
                    .navigationLinks(links)
                    .inferredFlows(inferredFlows)
                    .edgeCases(edgeCases)
                    .securityConsiderations(securityConsiderations)
                    .metadata(AnalysisMetadata.builder()
                            .analysisTimeMs(analysisTime)
                            .totalElements(elements.size())
                            .totalForms(forms.size())
                            .totalLinks(links.size())
                            .browser("Chrome (Headless)")
                            .viewport("1920x1080")
                            .warnings(warnings)
                            .build())
                    .build();

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String errorType = e.getClass().getSimpleName();

            // Provide more helpful error messages for common issues
            String userFriendlyMessage;
            if (errorMessage != null && (errorMessage.contains("ChromeDriver") ||
                    errorMessage.contains("chrome") || errorMessage.contains("Chrome"))) {
                userFriendlyMessage = "Chrome browser is not installed or configured. " +
                        "The live page analysis feature requires Chrome to be installed on the server.";
                warnings.add("Chrome/ChromeDriver error: " + errorMessage);
            } else if (errorMessage != null && errorMessage.contains("timeout")) {
                userFriendlyMessage = "Page load timed out. The target URL may be slow to respond or unreachable.";
                warnings.add("Timeout error: " + errorMessage);
            } else if (errorMessage != null && errorMessage.contains("ERR_NAME_NOT_RESOLVED")) {
                userFriendlyMessage = "Could not resolve the URL. Please check that the URL is correct.";
                warnings.add("DNS resolution error: " + errorMessage);
            } else {
                userFriendlyMessage = "Failed to analyze page: " + errorMessage;
                warnings.add("Analysis error (" + errorType + "): " + errorMessage);
            }

            // Log the full error for debugging
            System.err.println("[WebPageAnalyzer] Error analyzing URL: " + url);
            System.err.println("[WebPageAnalyzer] Exception type: " + errorType);
            System.err.println("[WebPageAnalyzer] Message: " + errorMessage);
            e.printStackTrace();

            return PageAnalysis.builder()
                    .url(url)
                    .pagePurpose("error")
                    .pageDescription(userFriendlyMessage)
                    .elements(Collections.emptyList())
                    .forms(Collections.emptyList())
                    .navigationLinks(Collections.emptyList())
                    .inferredFlows(Collections.emptyList())
                    .edgeCases(Collections.emptyList())
                    .securityConsiderations(Collections.emptyList())
                    .metadata(AnalysisMetadata.builder()
                            .analysisTimeMs(System.currentTimeMillis() - startTime)
                            .warnings(warnings)
                            .build())
                    .build();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Extract all interactive elements from the page using optimized batch JavaScript extraction.
     */
    private List<PageElement> extractInteractiveElements(WebDriver driver) {
        List<PageElement> elements = new ArrayList<>();

        try {
            // Use JavaScript to extract all elements in one call for better performance
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> batchElements = (List<Map<String, Object>>) ((JavascriptExecutor) driver)
                .executeScript(BATCH_ELEMENT_EXTRACTION_SCRIPT);

            if (batchElements != null) {
                for (Map<String, Object> elData : batchElements) {
                    try {
                        PageElement element = buildPageElementFromMap(elData, driver);
                        if (element != null) {
                            elements.add(element);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // Fallback to traditional extraction if batch fails
            elements.addAll(extractInputs(driver));
            elements.addAll(extractButtons(driver));
            elements.addAll(extractSelects(driver));
            elements.addAll(extractTextareas(driver));
            elements.addAll(extractRoleButtons(driver));
        }

        return elements;
    }

    /**
     * JavaScript for batch element extraction - significantly faster than individual Selenium calls.
     */
    private static final String BATCH_ELEMENT_EXTRACTION_SCRIPT = """
        (function() {
            var results = [];
            var maxElements = 100; // Limit to prevent slow pages

            function getLabelFor(el) {
                if (el.id) {
                    var label = document.querySelector('label[for="' + el.id + '"]');
                    if (label) return label.textContent.trim();
                }
                var parent = el.closest('label');
                if (parent) return parent.textContent.trim();
                return null;
            }

            function getDataAttributes(el) {
                var data = {};
                Array.from(el.attributes).forEach(function(attr) {
                    if (attr.name.startsWith('data-')) {
                        data[attr.name.substring(5)] = attr.value;
                    }
                });
                return data;
            }

            // Extract inputs (excluding hidden)
            var inputs = document.querySelectorAll('input:not([type="hidden"])');
            for (var i = 0; i < Math.min(inputs.length, maxElements); i++) {
                var el = inputs[i];
                results.push({
                    type: 'input',
                    inputType: el.type || 'text',
                    id: el.id || null,
                    name: el.name || null,
                    placeholder: el.placeholder || null,
                    ariaLabel: el.getAttribute('aria-label'),
                    label: getLabelFor(el),
                    required: el.required,
                    pattern: el.pattern || null,
                    minLength: el.minLength > 0 ? el.minLength : null,
                    maxLength: el.maxLength > 0 ? el.maxLength : null,
                    min: el.min || null,
                    max: el.max || null,
                    cssClasses: el.className || null,
                    role: el.getAttribute('role'),
                    dataAttributes: getDataAttributes(el)
                });
            }

            // Extract buttons
            var buttons = document.querySelectorAll('button');
            for (var i = 0; i < Math.min(buttons.length, maxElements); i++) {
                var el = buttons[i];
                results.push({
                    type: 'button',
                    inputType: el.type || 'button',
                    id: el.id || null,
                    name: el.name || null,
                    visibleText: el.textContent.trim(),
                    ariaLabel: el.getAttribute('aria-label'),
                    cssClasses: el.className || null,
                    dataAttributes: getDataAttributes(el)
                });
            }

            // Extract selects
            var selects = document.querySelectorAll('select');
            for (var i = 0; i < Math.min(selects.length, maxElements); i++) {
                var el = selects[i];
                results.push({
                    type: 'select',
                    id: el.id || null,
                    name: el.name || null,
                    ariaLabel: el.getAttribute('aria-label'),
                    label: getLabelFor(el),
                    required: el.required,
                    cssClasses: el.className || null,
                    dataAttributes: getDataAttributes(el)
                });
            }

            // Extract textareas
            var textareas = document.querySelectorAll('textarea');
            for (var i = 0; i < Math.min(textareas.length, maxElements); i++) {
                var el = textareas[i];
                results.push({
                    type: 'textarea',
                    id: el.id || null,
                    name: el.name || null,
                    placeholder: el.placeholder || null,
                    ariaLabel: el.getAttribute('aria-label'),
                    label: getLabelFor(el),
                    required: el.required,
                    minLength: el.minLength > 0 ? el.minLength : null,
                    maxLength: el.maxLength > 0 ? el.maxLength : null,
                    cssClasses: el.className || null,
                    dataAttributes: getDataAttributes(el)
                });
            }

            // Extract role=button elements (excluding actual buttons)
            var roleButtons = document.querySelectorAll('[role="button"]:not(button)');
            for (var i = 0; i < Math.min(roleButtons.length, maxElements); i++) {
                var el = roleButtons[i];
                results.push({
                    type: 'role-button',
                    id: el.id || null,
                    visibleText: el.textContent.trim(),
                    ariaLabel: el.getAttribute('aria-label'),
                    role: 'button',
                    cssClasses: el.className || null,
                    dataAttributes: getDataAttributes(el)
                });
            }

            return results;
        })();
        """;

    /**
     * Build PageElement from JavaScript extraction map.
     */
    private PageElement buildPageElementFromMap(Map<String, Object> data, WebDriver driver) {
        String type = (String) data.get("type");
        String inputType = (String) data.get("inputType");
        String id = (String) data.get("id");
        String name = (String) data.get("name");
        String placeholder = (String) data.get("placeholder");
        String ariaLabel = (String) data.get("ariaLabel");
        String label = (String) data.get("label");
        String visibleText = (String) data.get("visibleText");
        Boolean required = data.get("required") instanceof Boolean ? (Boolean) data.get("required") : false;
        String pattern = (String) data.get("pattern");
        String cssClasses = (String) data.get("cssClasses");
        String role = (String) data.get("role");

        Integer minLength = data.get("minLength") instanceof Number ? ((Number) data.get("minLength")).intValue() : null;
        Integer maxLength = data.get("maxLength") instanceof Number ? ((Number) data.get("maxLength")).intValue() : null;
        String min = (String) data.get("min");
        String max = (String) data.get("max");

        @SuppressWarnings("unchecked")
        Map<String, String> dataAttributes = data.get("dataAttributes") instanceof Map
                ? convertToStringMap((Map<String, Object>) data.get("dataAttributes"))
                : new HashMap<>();

        String semanticName = inferSemanticName(type, id, name, placeholder, label, ariaLabel);
        if (semanticName == null && visibleText != null && !visibleText.isEmpty()) {
            semanticName = visibleText;
        }

        String recommendedLocator = generateRecommendedLocatorFromData(id, name, ariaLabel, visibleText, label);

        return PageElement.builder()
                .type(type)
                .inputType(inputType)
                .id(id)
                .name(name)
                .placeholder(placeholder)
                .ariaLabel(ariaLabel)
                .label(label)
                .visibleText(visibleText)
                .required(required)
                .validationPattern(pattern)
                .minLength(minLength)
                .maxLength(maxLength)
                .minValue(min)
                .maxValue(max)
                .cssClasses(cssClasses)
                .role(role)
                .semanticName(semanticName)
                .recommendedLocator(recommendedLocator)
                .dataAttributes(dataAttributes)
                .build();
    }

    private Map<String, String> convertToStringMap(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        if (map != null) {
            map.forEach((k, v) -> result.put(k, v != null ? String.valueOf(v) : null));
        }
        return result;
    }

    private String generateRecommendedLocatorFromData(String id, String name, String ariaLabel, String text, String label) {
        if (id != null && !id.isEmpty()) {
            return "By.id(\"" + id + "\")";
        }
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            return "By.cssSelector(\"[aria-label='" + ariaLabel + "']\")";
        }
        if (name != null && !name.isEmpty()) {
            return "By.name(\"" + name + "\")";
        }
        if (text != null && !text.isEmpty()) {
            return "By.xpath(\"//*[contains(text(),'" + text + "')]\")";
        }
        if (label != null && !label.isEmpty()) {
            return "By.xpath(\"//label[contains(text(),'" + label + "')]/following::input[1]\")";
        }
        return "// Custom locator needed";
    }

    /**
     * Extract all interactive elements from the page (fallback method).
     */
    private List<PageElement> extractInteractiveElementsFallback(WebDriver driver) {
        List<PageElement> elements = new ArrayList<>();

        // Input elements
        elements.addAll(extractInputs(driver));

        // Buttons
        elements.addAll(extractButtons(driver));

        // Select dropdowns
        elements.addAll(extractSelects(driver));

        // Textareas
        elements.addAll(extractTextareas(driver));

        // Clickable elements with role=button
        elements.addAll(extractRoleButtons(driver));

        return elements;
    }

    private List<PageElement> extractInputs(WebDriver driver) {
        List<PageElement> inputs = new ArrayList<>();
        List<WebElement> inputElements = driver.findElements(By.tagName("input"));

        for (WebElement el : inputElements) {
            try {
                String type = el.getAttribute("type");
                if (type == null) type = "text";

                // Skip hidden inputs for main analysis
                if ("hidden".equals(type)) continue;

                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String placeholder = el.getAttribute("placeholder");
                String ariaLabel = el.getAttribute("aria-label");
                String label = findLabelForElement(driver, el);

                inputs.add(PageElement.builder()
                        .type("input")
                        .inputType(type)
                        .id(id)
                        .name(name)
                        .placeholder(placeholder)
                        .ariaLabel(ariaLabel)
                        .label(label)
                        .required(el.getAttribute("required") != null)
                        .validationPattern(el.getAttribute("pattern"))
                        .minLength(parseInteger(el.getAttribute("minlength")))
                        .maxLength(parseInteger(el.getAttribute("maxlength")))
                        .minValue(el.getAttribute("min"))
                        .maxValue(el.getAttribute("max"))
                        .cssClasses(el.getAttribute("class"))
                        .role(el.getAttribute("role"))
                        .semanticName(inferSemanticName(type, id, name, placeholder, label, ariaLabel))
                        .recommendedLocator(generateRecommendedLocator(el, id, name, ariaLabel, label))
                        .dataAttributes(extractDataAttributes(el))
                        .build());
            } catch (StaleElementReferenceException ignored) {}
        }
        return inputs;
    }

    private List<PageElement> extractButtons(WebDriver driver) {
        List<PageElement> buttons = new ArrayList<>();
        List<WebElement> buttonElements = driver.findElements(By.tagName("button"));

        for (WebElement el : buttonElements) {
            try {
                String text = el.getText().trim();
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String type = el.getAttribute("type");
                String ariaLabel = el.getAttribute("aria-label");

                buttons.add(PageElement.builder()
                        .type("button")
                        .inputType(type != null ? type : "button")
                        .id(id)
                        .name(name)
                        .visibleText(text)
                        .ariaLabel(ariaLabel)
                        .cssClasses(el.getAttribute("class"))
                        .semanticName(inferButtonSemanticName(text, id, name, ariaLabel))
                        .recommendedLocator(generateRecommendedLocator(el, id, name, ariaLabel, text))
                        .dataAttributes(extractDataAttributes(el))
                        .build());
            } catch (StaleElementReferenceException ignored) {}
        }
        return buttons;
    }

    private List<PageElement> extractSelects(WebDriver driver) {
        List<PageElement> selects = new ArrayList<>();
        List<WebElement> selectElements = driver.findElements(By.tagName("select"));

        for (WebElement el : selectElements) {
            try {
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String ariaLabel = el.getAttribute("aria-label");
                String label = findLabelForElement(driver, el);

                selects.add(PageElement.builder()
                        .type("select")
                        .id(id)
                        .name(name)
                        .ariaLabel(ariaLabel)
                        .label(label)
                        .required(el.getAttribute("required") != null)
                        .cssClasses(el.getAttribute("class"))
                        .semanticName(inferSemanticName("select", id, name, null, label, ariaLabel))
                        .recommendedLocator(generateRecommendedLocator(el, id, name, ariaLabel, label))
                        .dataAttributes(extractDataAttributes(el))
                        .build());
            } catch (StaleElementReferenceException ignored) {}
        }
        return selects;
    }

    private List<PageElement> extractTextareas(WebDriver driver) {
        List<PageElement> textareas = new ArrayList<>();
        List<WebElement> textareaElements = driver.findElements(By.tagName("textarea"));

        for (WebElement el : textareaElements) {
            try {
                String id = el.getAttribute("id");
                String name = el.getAttribute("name");
                String placeholder = el.getAttribute("placeholder");
                String ariaLabel = el.getAttribute("aria-label");
                String label = findLabelForElement(driver, el);

                textareas.add(PageElement.builder()
                        .type("textarea")
                        .id(id)
                        .name(name)
                        .placeholder(placeholder)
                        .ariaLabel(ariaLabel)
                        .label(label)
                        .required(el.getAttribute("required") != null)
                        .minLength(parseInteger(el.getAttribute("minlength")))
                        .maxLength(parseInteger(el.getAttribute("maxlength")))
                        .cssClasses(el.getAttribute("class"))
                        .semanticName(inferSemanticName("textarea", id, name, placeholder, label, ariaLabel))
                        .recommendedLocator(generateRecommendedLocator(el, id, name, ariaLabel, label))
                        .dataAttributes(extractDataAttributes(el))
                        .build());
            } catch (StaleElementReferenceException ignored) {}
        }
        return textareas;
    }

    private List<PageElement> extractRoleButtons(WebDriver driver) {
        List<PageElement> roleButtons = new ArrayList<>();
        List<WebElement> elements = driver.findElements(By.cssSelector("[role='button']"));

        for (WebElement el : elements) {
            try {
                // Skip actual buttons already captured
                if ("button".equalsIgnoreCase(el.getTagName())) continue;

                String text = el.getText().trim();
                String id = el.getAttribute("id");
                String ariaLabel = el.getAttribute("aria-label");

                roleButtons.add(PageElement.builder()
                        .type("role-button")
                        .id(id)
                        .visibleText(text)
                        .ariaLabel(ariaLabel)
                        .role("button")
                        .cssClasses(el.getAttribute("class"))
                        .semanticName(inferButtonSemanticName(text, id, null, ariaLabel))
                        .recommendedLocator(generateRecommendedLocator(el, id, null, ariaLabel, text))
                        .dataAttributes(extractDataAttributes(el))
                        .build());
            } catch (StaleElementReferenceException ignored) {}
        }
        return roleButtons;
    }

    /**
     * Extract forms from the page.
     */
    private List<FormInfo> extractForms(WebDriver driver) {
        List<FormInfo> forms = new ArrayList<>();
        List<WebElement> formElements = driver.findElements(By.tagName("form"));

        for (WebElement form : formElements) {
            try {
                String id = form.getAttribute("id");
                String name = form.getAttribute("name");
                String action = form.getAttribute("action");
                String method = form.getAttribute("method");

                List<PageElement> fields = new ArrayList<>();
                List<WebElement> formInputs = form.findElements(By.tagName("input"));
                List<WebElement> formSelects = form.findElements(By.tagName("select"));
                List<WebElement> formTextareas = form.findElements(By.tagName("textarea"));

                for (WebElement el : formInputs) {
                    String type = el.getAttribute("type");
                    if ("hidden".equals(type)) continue;
                    fields.add(buildPageElementFromWebElement(driver, el, "input"));
                }
                for (WebElement el : formSelects) {
                    fields.add(buildPageElementFromWebElement(driver, el, "select"));
                }
                for (WebElement el : formTextareas) {
                    fields.add(buildPageElementFromWebElement(driver, el, "textarea"));
                }

                // Find submit button
                PageElement submitButton = null;
                List<WebElement> buttons = form.findElements(By.tagName("button"));
                for (WebElement btn : buttons) {
                    String type = btn.getAttribute("type");
                    if ("submit".equals(type) || type == null) {
                        submitButton = PageElement.builder()
                                .type("button")
                                .inputType("submit")
                                .visibleText(btn.getText().trim())
                                .id(btn.getAttribute("id"))
                                .build();
                        break;
                    }
                }
                if (submitButton == null) {
                    List<WebElement> submitInputs = form.findElements(By.cssSelector("input[type='submit']"));
                    if (!submitInputs.isEmpty()) {
                        WebElement si = submitInputs.get(0);
                        submitButton = PageElement.builder()
                                .type("input")
                                .inputType("submit")
                                .visibleText(si.getAttribute("value"))
                                .id(si.getAttribute("id"))
                                .build();
                    }
                }

                // Check for CSRF token
                boolean hasCsrf = !form.findElements(By.cssSelector("input[name*='csrf'], input[name*='token']")).isEmpty();

                // Extract validation messages
                List<String> validationMessages = new ArrayList<>();
                List<WebElement> errorElements = form.findElements(By.cssSelector("[class*='error'], [class*='invalid'], [role='alert']"));
                for (WebElement err : errorElements) {
                    String text = err.getText().trim();
                    if (!text.isEmpty()) {
                        validationMessages.add(text);
                    }
                }

                String purpose = inferFormPurpose(id, name, action, fields);

                forms.add(FormInfo.builder()
                        .identifier(id != null ? id : name)
                        .action(action)
                        .method(method != null ? method.toUpperCase() : "GET")
                        .purpose(purpose)
                        .fields(fields)
                        .submitButton(submitButton)
                        .hasCsrfToken(hasCsrf)
                        .validationMessages(validationMessages)
                        .build());

            } catch (StaleElementReferenceException ignored) {}
        }
        return forms;
    }

    /**
     * Extract navigation links using optimized batch JavaScript extraction.
     */
    private List<LinkInfo> extractLinks(WebDriver driver, String baseUrl) {
        List<LinkInfo> links = new ArrayList<>();
        String baseDomain = extractDomain(baseUrl);

        try {
            // Use JavaScript to extract all links in one call
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> batchLinks = (List<Map<String, Object>>) ((JavascriptExecutor) driver)
                .executeScript(BATCH_LINK_EXTRACTION_SCRIPT);

            if (batchLinks != null) {
                for (Map<String, Object> linkData : batchLinks) {
                    String href = (String) linkData.get("href");
                    String text = (String) linkData.get("text");
                    String ariaLabel = (String) linkData.get("ariaLabel");

                    if (href == null || href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:")) {
                        continue;
                    }

                    boolean isInternal = href.contains(baseDomain) || href.startsWith("/");
                    String displayText = (text != null && !text.isEmpty()) ? text : ariaLabel;
                    String purpose = inferLinkPurpose(displayText, href);

                    links.add(LinkInfo.builder()
                            .text(displayText)
                            .href(href)
                            .internal(isInternal)
                            .purpose(purpose)
                            .build());
                }
            }
        } catch (Exception e) {
            // Fallback to traditional extraction
            List<WebElement> anchorElements = driver.findElements(By.tagName("a"));
            for (WebElement anchor : anchorElements) {
                try {
                    String href = anchor.getAttribute("href");
                    String text = anchor.getText().trim();

                    if (href == null || href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:")) {
                        continue;
                    }

                    boolean isInternal = href.contains(baseDomain) || href.startsWith("/");
                    String purpose = inferLinkPurpose(text, href);

                    links.add(LinkInfo.builder()
                            .text(text.isEmpty() ? anchor.getAttribute("aria-label") : text)
                            .href(href)
                            .internal(isInternal)
                            .purpose(purpose)
                            .build());
                } catch (StaleElementReferenceException ignored) {}
            }
        }
        return links;
    }

    /**
     * JavaScript for batch link extraction.
     */
    private static final String BATCH_LINK_EXTRACTION_SCRIPT = """
        (function() {
            var results = [];
            var maxLinks = 50; // Limit to prevent slow pages
            var anchors = document.querySelectorAll('a[href]');

            for (var i = 0; i < Math.min(anchors.length, maxLinks); i++) {
                var el = anchors[i];
                results.push({
                    href: el.href,
                    text: el.textContent.trim(),
                    ariaLabel: el.getAttribute('aria-label')
                });
            }
            return results;
        })();
        """;

    /**
     * Infer the purpose of the page based on various signals.
     */
    private String inferPagePurpose(String title, String pageSource, List<PageElement> elements, List<FormInfo> forms) {
        String titleLower = title != null ? title.toLowerCase() : "";
        String combined = titleLower + " " + pageSource;

        // Check forms first
        for (FormInfo form : forms) {
            if (form.getPurpose() != null && !form.getPurpose().equals("general")) {
                return form.getPurpose();
            }
        }

        // Check indicators
        if (containsAny(combined, LOGIN_INDICATORS)) return "login";
        if (containsAny(combined, SIGNUP_INDICATORS)) return "registration";
        if (containsAny(combined, SEARCH_INDICATORS)) return "search";
        if (containsAny(combined, CHECKOUT_INDICATORS)) return "checkout";
        if (containsAny(combined, CONTACT_INDICATORS)) return "contact";

        // Check element patterns
        boolean hasPasswordField = elements.stream()
                .anyMatch(e -> "password".equals(e.getInputType()));
        boolean hasEmailField = elements.stream()
                .anyMatch(e -> "email".equals(e.getInputType()) ||
                        (e.getName() != null && e.getName().toLowerCase().contains("email")));

        if (hasPasswordField && hasEmailField) {
            if (elements.stream().anyMatch(e -> e.getSemanticName() != null &&
                    e.getSemanticName().toLowerCase().contains("confirm"))) {
                return "registration";
            }
            return "login";
        }

        if (combined.contains("dashboard")) return "dashboard";
        if (combined.contains("profile")) return "profile";
        if (combined.contains("settings")) return "settings";

        return "general";
    }

    /**
     * Generate a human-readable description of the page.
     */
    private String generatePageDescription(String purpose, List<FormInfo> forms, List<PageElement> elements) {
        StringBuilder desc = new StringBuilder();

        switch (purpose) {
            case "login":
                desc.append("A login page that allows users to authenticate with their credentials.");
                break;
            case "registration":
                desc.append("A registration page for creating new user accounts.");
                break;
            case "search":
                desc.append("A search page that allows users to find content or items.");
                break;
            case "checkout":
                desc.append("A checkout page for completing purchases or transactions.");
                break;
            case "contact":
                desc.append("A contact form page for sending messages or inquiries.");
                break;
            case "dashboard":
                desc.append("A dashboard page displaying user-specific information and controls.");
                break;
            default:
                desc.append("A general web page with interactive elements.");
        }

        if (!forms.isEmpty()) {
            desc.append(" Contains ").append(forms.size()).append(" form(s).");
        }

        long inputCount = elements.stream().filter(e -> "input".equals(e.getType())).count();
        long buttonCount = elements.stream().filter(e -> "button".equals(e.getType())).count();

        if (inputCount > 0 || buttonCount > 0) {
            desc.append(" Features ").append(inputCount).append(" input field(s) and ")
                    .append(buttonCount).append(" button(s).");
        }

        return desc.toString();
    }

    /**
     * Infer possible user flows based on page structure.
     */
    private List<String> inferUserFlows(String purpose, List<FormInfo> forms, List<PageElement> elements) {
        List<String> flows = new ArrayList<>();

        switch (purpose) {
            case "login":
                flows.add("User enters valid credentials and successfully logs in");
                flows.add("User enters invalid credentials and sees error message");
                flows.add("User leaves required fields empty and sees validation errors");
                flows.add("User clicks 'forgot password' link if available");
                break;
            case "registration":
                flows.add("User fills all required fields with valid data and registers successfully");
                flows.add("User submits form with invalid email format");
                flows.add("User enters mismatched passwords (if confirmation field exists)");
                flows.add("User tries to register with existing email/username");
                break;
            case "search":
                flows.add("User enters search term and receives relevant results");
                flows.add("User searches with empty query");
                flows.add("User searches with special characters");
                flows.add("User filters or sorts search results");
                break;
            case "checkout":
                flows.add("User completes checkout with valid payment details");
                flows.add("User enters invalid payment information");
                flows.add("User modifies cart during checkout");
                flows.add("User applies discount code");
                break;
            case "contact":
                flows.add("User fills contact form and submits successfully");
                flows.add("User submits form with invalid email");
                flows.add("User exceeds character limit in message field");
                break;
            default:
                flows.add("User interacts with primary form elements");
                flows.add("User navigates using available links");
        }

        // Add flows based on specific elements
        for (PageElement el : elements) {
            if ("checkbox".equals(el.getInputType())) {
                flows.add("User toggles '" + (el.getSemanticName() != null ? el.getSemanticName() : "checkbox") + "' option");
            }
            if ("select".equals(el.getType())) {
                flows.add("User selects option from '" + (el.getSemanticName() != null ? el.getSemanticName() : "dropdown") + "'");
            }
        }

        return flows;
    }

    /**
     * Identify potential edge cases.
     */
    private List<String> identifyEdgeCases(String purpose, List<PageElement> elements, List<FormInfo> forms) {
        List<String> edgeCases = new ArrayList<>();

        // General edge cases
        edgeCases.add("Form submission with JavaScript disabled");
        edgeCases.add("Page behavior with slow network connection");
        edgeCases.add("Form submission during network timeout");

        // Based on elements
        for (PageElement el : elements) {
            if (el.getMinLength() != null || el.getMaxLength() != null) {
                edgeCases.add("Input '" + el.getSemanticName() + "' at min/max character boundary");
            }
            if ("email".equals(el.getInputType())) {
                edgeCases.add("Email field with various valid but unusual formats");
            }
            if ("number".equals(el.getInputType())) {
                edgeCases.add("Number input with decimal values and negative numbers");
            }
            if ("password".equals(el.getInputType())) {
                edgeCases.add("Password with special characters and unicode");
            }
            if (el.getValidationPattern() != null) {
                edgeCases.add("Input '" + el.getSemanticName() + "' pattern boundary testing");
            }
        }

        // Browser-specific
        edgeCases.add("Cross-browser compatibility (Chrome, Firefox, Safari, Edge)");
        edgeCases.add("Mobile responsive behavior");

        return edgeCases;
    }

    /**
     * Identify security considerations.
     */
    private List<String> identifySecurityConsiderations(String purpose, List<PageElement> elements, List<FormInfo> forms) {
        List<String> security = new ArrayList<>();

        // Check for CSRF protection
        boolean hasCsrf = forms.stream().anyMatch(FormInfo::isHasCsrfToken);
        if (!hasCsrf && !forms.isEmpty()) {
            security.add("CSRF protection: Forms should include anti-CSRF tokens");
        }

        // Password fields
        boolean hasPassword = elements.stream().anyMatch(e -> "password".equals(e.getInputType()));
        if (hasPassword) {
            security.add("Password handling: Verify password is not logged or exposed");
            security.add("Password transmission: Ensure HTTPS is enforced");
            security.add("SQL injection: Test login fields for injection vulnerabilities");
        }

        // Input validation
        security.add("XSS prevention: Test all input fields for script injection");
        security.add("Input validation: Verify server-side validation exists");

        // Authentication specific
        if ("login".equals(purpose)) {
            security.add("Brute force protection: Verify rate limiting exists");
            security.add("Account enumeration: Error messages should not reveal user existence");
            security.add("Session management: Verify secure session handling");
        }

        if ("registration".equals(purpose)) {
            security.add("Password strength: Verify password policy enforcement");
            security.add("Email verification: Verify email confirmation flow");
        }

        return security;
    }

    // Helper methods

    private String findLabelForElement(WebDriver driver, WebElement element) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            try {
                List<WebElement> labels = driver.findElements(By.cssSelector("label[for='" + id + "']"));
                if (!labels.isEmpty()) {
                    return labels.get(0).getText().trim();
                }
            } catch (Exception ignored) {}
        }

        // Try parent label
        try {
            WebElement parent = element.findElement(By.xpath("./ancestor::label"));
            return parent.getText().trim();
        } catch (Exception ignored) {}

        return null;
    }

    private PageElement buildPageElementFromWebElement(WebDriver driver, WebElement el, String type) {
        String inputType = "input".equals(type) ? el.getAttribute("type") : null;
        String id = el.getAttribute("id");
        String name = el.getAttribute("name");
        String placeholder = el.getAttribute("placeholder");
        String ariaLabel = el.getAttribute("aria-label");
        String label = findLabelForElement(driver, el);

        return PageElement.builder()
                .type(type)
                .inputType(inputType)
                .id(id)
                .name(name)
                .placeholder(placeholder)
                .ariaLabel(ariaLabel)
                .label(label)
                .required(el.getAttribute("required") != null)
                .validationPattern(el.getAttribute("pattern"))
                .minLength(parseInteger(el.getAttribute("minlength")))
                .maxLength(parseInteger(el.getAttribute("maxlength")))
                .semanticName(inferSemanticName(type, id, name, placeholder, label, ariaLabel))
                .recommendedLocator(generateRecommendedLocator(el, id, name, ariaLabel, label))
                .dataAttributes(extractDataAttributes(el))
                .build();
    }

    private String inferSemanticName(String type, String id, String name, String placeholder, String label, String ariaLabel) {
        if (label != null && !label.isEmpty()) return label;
        if (ariaLabel != null && !ariaLabel.isEmpty()) return ariaLabel;
        if (placeholder != null && !placeholder.isEmpty()) return placeholder;
        if (name != null && !name.isEmpty()) return humanize(name);
        if (id != null && !id.isEmpty()) return humanize(id);
        return type + " field";
    }

    private String inferButtonSemanticName(String text, String id, String name, String ariaLabel) {
        if (text != null && !text.isEmpty()) return text;
        if (ariaLabel != null && !ariaLabel.isEmpty()) return ariaLabel;
        if (name != null && !name.isEmpty()) return humanize(name);
        if (id != null && !id.isEmpty()) return humanize(id);
        return "button";
    }

    private String humanize(String text) {
        if (text == null) return "";
        return text.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[_-]", " ")
                .trim();
    }

    private String generateRecommendedLocator(WebElement el, String id, String name, String ariaLabel, String text) {
        if (id != null && !id.isEmpty()) {
            return "By.id(\"" + id + "\")";
        }
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            return "By.cssSelector(\"[aria-label='" + ariaLabel + "']\")";
        }
        if (name != null && !name.isEmpty()) {
            return "By.name(\"" + name + "\")";
        }
        if (text != null && !text.isEmpty()) {
            return "By.xpath(\"//*[contains(text(),'" + text + "')]\")";
        }
        return "// Custom locator needed";
    }

    private Map<String, String> extractDataAttributes(WebElement el) {
        Map<String, String> dataAttrs = new HashMap<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) ((WrapsDriver) el).getWrappedDriver();
            @SuppressWarnings("unchecked")
            Map<String, Object> dataset = (Map<String, Object>) js.executeScript(
                    "return Object.assign({}, arguments[0].dataset)", el);
            if (dataset != null) {
                dataset.forEach((k, v) -> dataAttrs.put(k, String.valueOf(v)));
            }
        } catch (Exception ignored) {}
        return dataAttrs;
    }

    private String inferFormPurpose(String id, String name, String action, List<PageElement> fields) {
        String combined = ((id != null ? id : "") + " " + (name != null ? name : "") + " " + (action != null ? action : "")).toLowerCase();

        if (containsAny(combined, LOGIN_INDICATORS)) return "login";
        if (containsAny(combined, SIGNUP_INDICATORS)) return "registration";
        if (containsAny(combined, SEARCH_INDICATORS)) return "search";
        if (containsAny(combined, CONTACT_INDICATORS)) return "contact";

        // Check fields
        boolean hasPassword = fields.stream().anyMatch(f -> "password".equals(f.getInputType()));
        boolean hasEmail = fields.stream().anyMatch(f -> "email".equals(f.getInputType()));

        if (hasPassword && hasEmail) {
            if (fields.stream().anyMatch(f -> f.getName() != null && f.getName().toLowerCase().contains("confirm"))) {
                return "registration";
            }
            return "login";
        }

        return "general";
    }

    private String inferLinkPurpose(String text, String href) {
        String combined = (text + " " + href).toLowerCase();
        if (containsAny(combined, LOGIN_INDICATORS)) return "authentication";
        if (containsAny(combined, SIGNUP_INDICATORS)) return "registration";
        if (combined.contains("logout") || combined.contains("sign out")) return "logout";
        if (combined.contains("home")) return "home navigation";
        if (combined.contains("about")) return "about page";
        if (combined.contains("contact")) return "contact page";
        if (combined.contains("help") || combined.contains("faq")) return "help/support";
        return "navigation";
    }

    private String extractDomain(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            return parsed.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean containsAny(String text, Set<String> indicators) {
        return indicators.stream().anyMatch(text::contains);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
