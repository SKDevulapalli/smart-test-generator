package com.enterprise.testgen.service;

import com.enterprise.testgen.model.PageAnalysis;
import com.enterprise.testgen.model.PageAnalysis.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for analyzing live web pages.
 * Uses JSoup for reliable HTML parsing that works in any environment.
 */
@Service
public class WebPageAnalyzerService {

    private static final Set<String> LOGIN_INDICATORS = Set.of(
            "login", "signin", "sign-in", "log-in", "authenticate", "password", "username");
    private static final Set<String> SIGNUP_INDICATORS = Set.of(
            "signup", "sign-up", "register", "create-account", "join", "registration");
    private static final Set<String> SEARCH_INDICATORS = Set.of(
            "search", "query", "find", "lookup");
    private static final Set<String> CHECKOUT_INDICATORS = Set.of(
            "checkout", "payment", "pay", "cart", "order", "purchase");
    private static final Set<String> CONTACT_INDICATORS = Set.of(
            "contact", "message", "inquiry", "feedback", "support");

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Analyze a live web page and extract all relevant information.
     * Uses JSoup for reliable HTML parsing without browser dependencies.
     */
    public PageAnalysis analyzePage(String url) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            // Normalize URL to force English for Google (which ignores Accept-Language based on IP)
            String normalizedUrl = normalizeUrlForEnglish(url);
            System.out.println("[WebPageAnalyzer] Fetching page: " + normalizedUrl);

            // Use JSoup to fetch and parse the page
            // Request English content to avoid geo-localized pages
            Document doc = Jsoup.connect(normalizedUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .cookie("PREF", "hl=en")  // Google language preference cookie
                    .cookie("NID", "")  // Clear Google geo cookie
                    .timeout(30000)
                    .followRedirects(true)
                    .get();

            String title = doc.title();
            String pageSource = doc.html().toLowerCase();

            System.out.println("[WebPageAnalyzer] Page fetched successfully: " + title);

            // Extract elements
            List<PageElement> elements = extractElements(doc);
            List<FormInfo> forms = extractForms(doc);
            List<LinkInfo> links = extractLinks(doc, url);

            // Infer page purpose
            String pagePurpose = inferPagePurpose(title, pageSource, elements, forms);
            String pageDescription = generatePageDescription(pagePurpose, forms, elements);

            // Infer user flows and edge cases
            List<String> inferredFlows = inferUserFlows(pagePurpose, forms, elements);
            List<String> edgeCases = identifyEdgeCases(pagePurpose, elements, forms);
            List<String> securityConsiderations = identifySecurityConsiderations(pagePurpose, elements, forms);

            // Add note about JavaScript
            if (pageSource.contains("<script") || pageSource.contains("react") ||
                pageSource.contains("angular") || pageSource.contains("vue")) {
                warnings.add("Note: This page may use JavaScript to render content. Some dynamic elements may not be detected.");
            }

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
                            .browser("JSoup HTML Parser")
                            .viewport("N/A (static analysis)")
                            .warnings(warnings)
                            .build())
                    .build();

        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String userFriendlyMessage;

            if (errorMessage != null && errorMessage.contains("Status=403")) {
                userFriendlyMessage = "Access denied (403). The website may be blocking automated requests.";
            } else if (errorMessage != null && errorMessage.contains("Status=404")) {
                userFriendlyMessage = "Page not found (404). Please check the URL.";
            } else if (errorMessage != null && (errorMessage.contains("UnknownHostException") ||
                       errorMessage.contains("Unable to resolve host"))) {
                userFriendlyMessage = "Could not resolve the URL. Please check that it's correct.";
            } else if (errorMessage != null && errorMessage.contains("SocketTimeoutException")) {
                userFriendlyMessage = "Connection timed out. The server may be slow or unreachable.";
            } else if (errorMessage != null && errorMessage.contains("SSLHandshakeException")) {
                userFriendlyMessage = "SSL/TLS error. The site may have certificate issues.";
            } else {
                userFriendlyMessage = "Failed to fetch page: " + (errorMessage != null ? errorMessage : "Unknown error");
            }

            warnings.add("Error: " + errorMessage);

            System.err.println("[WebPageAnalyzer] Error analyzing URL: " + url);
            System.err.println("[WebPageAnalyzer] Message: " + errorMessage);

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
        }
    }

    /**
     * Analyze HTML content directly (for pasted HTML - fallback for blocked sites).
     */
    public PageAnalysis analyzeHtml(String html, String sourceUrl) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            System.out.println("[WebPageAnalyzer] Analyzing pasted HTML from: " + sourceUrl);

            // Parse the HTML string directly
            Document doc = Jsoup.parse(html);

            // If sourceUrl is provided, set it as the base URI for resolving relative links
            if (sourceUrl != null && sourceUrl.startsWith("http")) {
                doc.setBaseUri(sourceUrl);
            }

            String title = doc.title();
            if (title == null || title.isEmpty()) {
                title = "Pasted HTML Content";
            }
            String pageSource = doc.html().toLowerCase();

            System.out.println("[WebPageAnalyzer] HTML parsed successfully: " + title);

            // Extract elements
            List<PageElement> elements = extractElements(doc);
            List<FormInfo> forms = extractForms(doc);
            List<LinkInfo> links = extractLinks(doc, sourceUrl != null ? sourceUrl : "");

            // Infer page purpose
            String pagePurpose = inferPagePurpose(title, pageSource, elements, forms);
            String pageDescription = generatePageDescription(pagePurpose, forms, elements);

            // Infer user flows and edge cases
            List<String> inferredFlows = inferUserFlows(pagePurpose, forms, elements);
            List<String> edgeCases = identifyEdgeCases(pagePurpose, elements, forms);
            List<String> securityConsiderations = identifySecurityConsiderations(pagePurpose, elements, forms);

            // Add note about source
            warnings.add("Note: Analysis based on pasted HTML. Some dynamic JavaScript content may not be included.");

            // Add note about JavaScript if detected
            if (pageSource.contains("<script") || pageSource.contains("react") ||
                pageSource.contains("angular") || pageSource.contains("vue")) {
                warnings.add("This page uses JavaScript frameworks. Dynamic content rendered by JavaScript was not captured.");
            }

            long analysisTime = System.currentTimeMillis() - startTime;

            return PageAnalysis.builder()
                    .url(sourceUrl != null ? sourceUrl : "pasted-html")
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
                            .browser("JSoup HTML Parser (from pasted content)")
                            .viewport("N/A (static analysis)")
                            .warnings(warnings)
                            .build())
                    .build();

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            warnings.add("Error: " + errorMessage);

            System.err.println("[WebPageAnalyzer] Error analyzing pasted HTML");
            System.err.println("[WebPageAnalyzer] Message: " + errorMessage);

            return PageAnalysis.builder()
                    .url(sourceUrl != null ? sourceUrl : "pasted-html")
                    .pagePurpose("error")
                    .pageDescription("Failed to parse HTML: " + (errorMessage != null ? errorMessage : "Unknown error"))
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
        }
    }

    /**
     * Extract all interactive elements from the page.
     */
    private List<PageElement> extractElements(Document doc) {
        List<PageElement> elements = new ArrayList<>();

        // Extract inputs
        for (Element el : doc.select("input:not([type=hidden])")) {
            String type = el.attr("type");
            if (type.isEmpty()) type = "text";

            String id = el.attr("id");
            String name = el.attr("name");
            String placeholder = el.attr("placeholder");
            String ariaLabel = el.attr("aria-label");
            String label = findLabelFor(doc, el);

            elements.add(PageElement.builder()
                    .type("input")
                    .inputType(type)
                    .id(emptyToNull(id))
                    .name(emptyToNull(name))
                    .placeholder(emptyToNull(placeholder))
                    .ariaLabel(emptyToNull(ariaLabel))
                    .label(label)
                    .required(el.hasAttr("required"))
                    .validationPattern(emptyToNull(el.attr("pattern")))
                    .minLength(parseInteger(el.attr("minlength")))
                    .maxLength(parseInteger(el.attr("maxlength")))
                    .minValue(emptyToNull(el.attr("min")))
                    .maxValue(emptyToNull(el.attr("max")))
                    .cssClasses(emptyToNull(el.attr("class")))
                    .role(emptyToNull(el.attr("role")))
                    .semanticName(inferSemanticName(type, id, name, placeholder, label, ariaLabel))
                    .recommendedLocator(generateLocator(id, name, ariaLabel, null, label))
                    .dataAttributes(extractDataAttributes(el))
                    .build());
        }

        // Extract buttons
        for (Element el : doc.select("button")) {
            String text = el.text().trim();
            String id = el.attr("id");
            String name = el.attr("name");
            String type = el.attr("type");
            String ariaLabel = el.attr("aria-label");

            elements.add(PageElement.builder()
                    .type("button")
                    .inputType(type.isEmpty() ? "button" : type)
                    .id(emptyToNull(id))
                    .name(emptyToNull(name))
                    .visibleText(emptyToNull(text))
                    .ariaLabel(emptyToNull(ariaLabel))
                    .cssClasses(emptyToNull(el.attr("class")))
                    .semanticName(inferButtonSemanticName(text, id, name, ariaLabel))
                    .recommendedLocator(generateLocator(id, name, ariaLabel, text, null))
                    .dataAttributes(extractDataAttributes(el))
                    .build());
        }

        // Extract selects
        for (Element el : doc.select("select")) {
            String id = el.attr("id");
            String name = el.attr("name");
            String ariaLabel = el.attr("aria-label");
            String label = findLabelFor(doc, el);

            elements.add(PageElement.builder()
                    .type("select")
                    .id(emptyToNull(id))
                    .name(emptyToNull(name))
                    .ariaLabel(emptyToNull(ariaLabel))
                    .label(label)
                    .required(el.hasAttr("required"))
                    .cssClasses(emptyToNull(el.attr("class")))
                    .semanticName(inferSemanticName("select", id, name, null, label, ariaLabel))
                    .recommendedLocator(generateLocator(id, name, ariaLabel, null, label))
                    .dataAttributes(extractDataAttributes(el))
                    .build());
        }

        // Extract textareas
        for (Element el : doc.select("textarea")) {
            String id = el.attr("id");
            String name = el.attr("name");
            String placeholder = el.attr("placeholder");
            String ariaLabel = el.attr("aria-label");
            String label = findLabelFor(doc, el);

            elements.add(PageElement.builder()
                    .type("textarea")
                    .id(emptyToNull(id))
                    .name(emptyToNull(name))
                    .placeholder(emptyToNull(placeholder))
                    .ariaLabel(emptyToNull(ariaLabel))
                    .label(label)
                    .required(el.hasAttr("required"))
                    .minLength(parseInteger(el.attr("minlength")))
                    .maxLength(parseInteger(el.attr("maxlength")))
                    .cssClasses(emptyToNull(el.attr("class")))
                    .semanticName(inferSemanticName("textarea", id, name, placeholder, label, ariaLabel))
                    .recommendedLocator(generateLocator(id, name, ariaLabel, null, label))
                    .dataAttributes(extractDataAttributes(el))
                    .build());
        }

        // Extract role=button elements
        for (Element el : doc.select("[role=button]:not(button)")) {
            String text = el.text().trim();
            String id = el.attr("id");
            String ariaLabel = el.attr("aria-label");

            elements.add(PageElement.builder()
                    .type("role-button")
                    .id(emptyToNull(id))
                    .visibleText(emptyToNull(text))
                    .ariaLabel(emptyToNull(ariaLabel))
                    .role("button")
                    .cssClasses(emptyToNull(el.attr("class")))
                    .semanticName(inferButtonSemanticName(text, id, null, ariaLabel))
                    .recommendedLocator(generateLocator(id, null, ariaLabel, text, null))
                    .dataAttributes(extractDataAttributes(el))
                    .build());
        }

        return elements;
    }

    /**
     * Extract forms from the page.
     */
    private List<FormInfo> extractForms(Document doc) {
        List<FormInfo> forms = new ArrayList<>();

        for (Element form : doc.select("form")) {
            String id = form.attr("id");
            String name = form.attr("name");
            String action = form.attr("action");
            String method = form.attr("method");

            List<PageElement> fields = new ArrayList<>();

            for (Element el : form.select("input:not([type=hidden])")) {
                String type = el.attr("type");
                if (type.isEmpty()) type = "text";
                String elId = el.attr("id");
                String elName = el.attr("name");
                String label = findLabelFor(doc, el);

                fields.add(PageElement.builder()
                        .type("input")
                        .inputType(type)
                        .id(emptyToNull(elId))
                        .name(emptyToNull(elName))
                        .label(label)
                        .required(el.hasAttr("required"))
                        .semanticName(inferSemanticName(type, elId, elName, el.attr("placeholder"), label, el.attr("aria-label")))
                        .build());
            }

            for (Element el : form.select("select")) {
                String elId = el.attr("id");
                String elName = el.attr("name");
                String label = findLabelFor(doc, el);

                fields.add(PageElement.builder()
                        .type("select")
                        .id(emptyToNull(elId))
                        .name(emptyToNull(elName))
                        .label(label)
                        .required(el.hasAttr("required"))
                        .semanticName(inferSemanticName("select", elId, elName, null, label, el.attr("aria-label")))
                        .build());
            }

            for (Element el : form.select("textarea")) {
                String elId = el.attr("id");
                String elName = el.attr("name");
                String label = findLabelFor(doc, el);

                fields.add(PageElement.builder()
                        .type("textarea")
                        .id(emptyToNull(elId))
                        .name(emptyToNull(elName))
                        .label(label)
                        .required(el.hasAttr("required"))
                        .semanticName(inferSemanticName("textarea", elId, elName, el.attr("placeholder"), label, el.attr("aria-label")))
                        .build());
            }

            // Find submit button
            PageElement submitButton = null;
            Element submitEl = form.selectFirst("button[type=submit], input[type=submit], button:not([type])");
            if (submitEl != null) {
                String btnText = submitEl.tagName().equals("input") ? submitEl.attr("value") : submitEl.text().trim();
                submitButton = PageElement.builder()
                        .type(submitEl.tagName())
                        .inputType("submit")
                        .visibleText(emptyToNull(btnText))
                        .id(emptyToNull(submitEl.attr("id")))
                        .build();
            }

            // Check for CSRF token
            boolean hasCsrf = !form.select("input[name*=csrf], input[name*=token], input[name*=_token]").isEmpty();

            String purpose = inferFormPurpose(id, name, action, fields);

            forms.add(FormInfo.builder()
                    .identifier(id != null && !id.isEmpty() ? id : name)
                    .action(emptyToNull(action))
                    .method(method.isEmpty() ? "GET" : method.toUpperCase())
                    .purpose(purpose)
                    .fields(fields)
                    .submitButton(submitButton)
                    .hasCsrfToken(hasCsrf)
                    .validationMessages(new ArrayList<>())
                    .build());
        }

        return forms;
    }

    /**
     * Extract navigation links.
     */
    private List<LinkInfo> extractLinks(Document doc, String baseUrl) {
        List<LinkInfo> links = new ArrayList<>();
        String baseDomain = extractDomain(baseUrl);
        int count = 0;
        int maxLinks = 50;

        for (Element anchor : doc.select("a[href]")) {
            if (count >= maxLinks) break;

            String href = anchor.attr("abs:href");
            String text = anchor.text().trim();

            if (href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:")) {
                continue;
            }

            boolean isInternal = href.contains(baseDomain) || anchor.attr("href").startsWith("/");
            String purpose = inferLinkPurpose(text, href);

            links.add(LinkInfo.builder()
                    .text(text.isEmpty() ? anchor.attr("aria-label") : text)
                    .href(href)
                    .internal(isInternal)
                    .purpose(purpose)
                    .build());

            count++;
        }

        return links;
    }

    // Helper methods

    private String findLabelFor(Document doc, Element el) {
        String id = el.attr("id");
        if (!id.isEmpty()) {
            Element label = doc.selectFirst("label[for=" + id + "]");
            if (label != null) {
                return label.text().trim();
            }
        }

        // Check parent label
        Element parent = el.parent();
        while (parent != null) {
            if (parent.tagName().equals("label")) {
                return parent.text().trim();
            }
            parent = parent.parent();
        }

        return null;
    }

    private Map<String, String> extractDataAttributes(Element el) {
        Map<String, String> dataAttrs = new HashMap<>();
        el.attributes().forEach(attr -> {
            if (attr.getKey().startsWith("data-")) {
                dataAttrs.put(attr.getKey().substring(5), attr.getValue());
            }
        });
        return dataAttrs;
    }

    private String generateLocator(String id, String name, String ariaLabel, String text, String label) {
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

    private String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String inferPagePurpose(String title, String pageSource, List<PageElement> elements, List<FormInfo> forms) {
        String titleLower = title != null ? title.toLowerCase() : "";
        String combined = titleLower + " " + pageSource;

        for (FormInfo form : forms) {
            if (form.getPurpose() != null && !form.getPurpose().equals("general")) {
                return form.getPurpose();
            }
        }

        if (containsAny(combined, LOGIN_INDICATORS)) return "login";
        if (containsAny(combined, SIGNUP_INDICATORS)) return "registration";
        if (containsAny(combined, SEARCH_INDICATORS)) return "search";
        if (containsAny(combined, CHECKOUT_INDICATORS)) return "checkout";
        if (containsAny(combined, CONTACT_INDICATORS)) return "contact";

        boolean hasPasswordField = elements.stream().anyMatch(e -> "password".equals(e.getInputType()));
        boolean hasEmailField = elements.stream().anyMatch(e -> "email".equals(e.getInputType()) ||
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

    private String inferFormPurpose(String id, String name, String action, List<PageElement> fields) {
        String combined = ((id != null ? id : "") + " " + (name != null ? name : "") + " " + (action != null ? action : "")).toLowerCase();

        if (containsAny(combined, LOGIN_INDICATORS)) return "login";
        if (containsAny(combined, SIGNUP_INDICATORS)) return "registration";
        if (containsAny(combined, SEARCH_INDICATORS)) return "search";
        if (containsAny(combined, CONTACT_INDICATORS)) return "contact";

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

    private String generatePageDescription(String purpose, List<FormInfo> forms, List<PageElement> elements) {
        StringBuilder desc = new StringBuilder();

        switch (purpose) {
            case "login" -> desc.append("A login page that allows users to authenticate with their credentials.");
            case "registration" -> desc.append("A registration page for creating new user accounts.");
            case "search" -> desc.append("A search page that allows users to find content or items.");
            case "checkout" -> desc.append("A checkout page for completing purchases or transactions.");
            case "contact" -> desc.append("A contact form page for sending messages or inquiries.");
            case "dashboard" -> desc.append("A dashboard page displaying user-specific information and controls.");
            default -> desc.append("A general web page with interactive elements.");
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

    private List<String> inferUserFlows(String purpose, List<FormInfo> forms, List<PageElement> elements) {
        List<String> flows = new ArrayList<>();

        switch (purpose) {
            case "login" -> {
                flows.add("User enters valid credentials and successfully logs in");
                flows.add("User enters invalid credentials and sees error message");
                flows.add("User leaves required fields empty and sees validation errors");
                flows.add("User clicks 'forgot password' link if available");
            }
            case "registration" -> {
                flows.add("User fills all required fields with valid data and registers successfully");
                flows.add("User submits form with invalid email format");
                flows.add("User enters mismatched passwords (if confirmation field exists)");
                flows.add("User tries to register with existing email/username");
            }
            case "search" -> {
                flows.add("User enters search term and receives relevant results");
                flows.add("User searches with empty query");
                flows.add("User searches with special characters");
                flows.add("User filters or sorts search results");
            }
            case "checkout" -> {
                flows.add("User completes checkout with valid payment details");
                flows.add("User enters invalid payment information");
                flows.add("User modifies cart during checkout");
                flows.add("User applies discount code");
            }
            case "contact" -> {
                flows.add("User fills contact form and submits successfully");
                flows.add("User submits form with invalid email");
                flows.add("User exceeds character limit in message field");
            }
            default -> {
                flows.add("User interacts with primary form elements");
                flows.add("User navigates using available links");
            }
        }

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

    private List<String> identifyEdgeCases(String purpose, List<PageElement> elements, List<FormInfo> forms) {
        List<String> edgeCases = new ArrayList<>();

        edgeCases.add("Form submission with JavaScript disabled");
        edgeCases.add("Page behavior with slow network connection");
        edgeCases.add("Form submission during network timeout");

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

        edgeCases.add("Cross-browser compatibility (Chrome, Firefox, Safari, Edge)");
        edgeCases.add("Mobile responsive behavior");

        return edgeCases;
    }

    private List<String> identifySecurityConsiderations(String purpose, List<PageElement> elements, List<FormInfo> forms) {
        List<String> security = new ArrayList<>();

        boolean hasCsrf = forms.stream().anyMatch(FormInfo::isHasCsrfToken);
        if (!hasCsrf && !forms.isEmpty()) {
            security.add("CSRF protection: Forms should include anti-CSRF tokens");
        }

        boolean hasPassword = elements.stream().anyMatch(e -> "password".equals(e.getInputType()));
        if (hasPassword) {
            security.add("Password handling: Verify password is not logged or exposed");
            security.add("Password transmission: Ensure HTTPS is enforced");
            security.add("SQL injection: Test login fields for injection vulnerabilities");
        }

        security.add("XSS prevention: Test all input fields for script injection");
        security.add("Input validation: Verify server-side validation exists");

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

    /**
     * Normalize URL to force English content for sites that geo-localize.
     * Google ignores Accept-Language header and uses IP geolocation,
     * so we need to add hl=en parameter or use google.com/ncr.
     */
    private String normalizeUrlForEnglish(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            String host = parsed.getHost().toLowerCase();

            // Google: add hl=en parameter to force English
            if (host.contains("google.")) {
                String separator = url.contains("?") ? "&" : "?";
                // Only add if hl parameter not already present
                if (!url.contains("hl=")) {
                    return url + separator + "hl=en";
                }
            }

            return url;
        } catch (Exception e) {
            return url;
        }
    }
}
