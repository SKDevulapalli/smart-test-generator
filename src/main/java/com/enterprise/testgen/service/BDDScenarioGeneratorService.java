package com.enterprise.testgen.service;

import com.enterprise.testgen.model.BDDScenario;
import com.enterprise.testgen.model.BDDScenario.*;
import com.enterprise.testgen.model.PageAnalysis;
import com.enterprise.testgen.model.PageAnalysis.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating BDD (Gherkin) scenarios from page analysis.
 * Creates framework-agnostic, semantic scenarios based ONLY on actual DOM elements discovered.
 *
 * Key principle: Only generate scenarios for elements that actually exist on the page.
 */
@Service
public class BDDScenarioGeneratorService {

    /**
     * Generate BDD scenarios from page analysis.
     * All scenarios are derived from actual discovered elements.
     */
    public BDDScenario generateScenarios(PageAnalysis analysis) {
        String featureName = generateFeatureName(analysis);
        String featureDescription = generateFeatureDescription(analysis);

        List<Scenario> scenarios = new ArrayList<>();

        // Generate background steps based on actual URL
        List<String> background = generateBackground(analysis);

        // Generate form-based scenarios for each discovered form
        if (analysis.getForms() != null && !analysis.getForms().isEmpty()) {
            for (FormInfo form : analysis.getForms()) {
                scenarios.addAll(generateFormScenarios(form, analysis));
            }
        }

        // Generate scenarios for standalone interactive elements (not in forms)
        scenarios.addAll(generateElementScenarios(analysis));

        // Generate navigation scenarios for discovered links
        scenarios.addAll(generateNavigationScenarios(analysis));

        // If no scenarios were generated, create basic page load scenarios
        if (scenarios.isEmpty()) {
            scenarios.addAll(generateBasicPageScenarios(analysis));
        }

        return BDDScenario.builder()
                .feature(featureName)
                .featureDescription(featureDescription)
                .tags(generateFeatureTags(analysis))
                .background(background)
                .scenarios(scenarios)
                .build();
    }

    /**
     * Generate feature name from page title or URL.
     */
    private String generateFeatureName(PageAnalysis analysis) {
        if (analysis.getTitle() != null && !analysis.getTitle().isEmpty()) {
            // Clean up title - remove site name suffixes like " | Company Name"
            String title = analysis.getTitle();
            if (title.contains(" | ")) {
                title = title.substring(0, title.indexOf(" | "));
            }
            if (title.contains(" - ")) {
                title = title.substring(0, title.indexOf(" - "));
            }
            return title.trim();
        }

        // Fall back to URL-based name
        if (analysis.getUrl() != null) {
            try {
                java.net.URL url = new java.net.URL(analysis.getUrl());
                String path = url.getPath();
                if (path != null && !path.isEmpty() && !path.equals("/")) {
                    // Convert /some-page to "Some Page"
                    String pageName = path.replaceAll("^/|/$", "")
                            .replaceAll("[/-]", " ")
                            .replaceAll("\\.\\w+$", ""); // Remove file extension
                    return capitalizeWords(pageName);
                }
                return url.getHost() + " Homepage";
            } catch (Exception e) {
                return "Web Page";
            }
        }

        return "Page Functionality";
    }

    /**
     * Generate feature description based on actual page content.
     */
    private String generateFeatureDescription(PageAnalysis analysis) {
        StringBuilder desc = new StringBuilder();
        desc.append("Testing the functionality of ");

        if (analysis.getTitle() != null && !analysis.getTitle().isEmpty()) {
            desc.append(analysis.getTitle());
        } else {
            desc.append("this page");
        }

        int formCount = analysis.getForms() != null ? analysis.getForms().size() : 0;
        int elementCount = analysis.getElements() != null ? analysis.getElements().size() : 0;
        int linkCount = analysis.getNavigationLinks() != null ? analysis.getNavigationLinks().size() : 0;

        desc.append(". Page contains ");
        List<String> contents = new ArrayList<>();
        if (formCount > 0) contents.add(formCount + " form(s)");
        if (elementCount > 0) contents.add(elementCount + " interactive element(s)");
        if (linkCount > 0) contents.add(linkCount + " navigation link(s)");

        if (!contents.isEmpty()) {
            desc.append(String.join(", ", contents));
        } else {
            desc.append("no interactive elements detected");
        }
        desc.append(".");

        return desc.toString();
    }

    /**
     * Generate feature tags based on actual page content.
     */
    private List<String> generateFeatureTags(PageAnalysis analysis) {
        List<String> tags = new ArrayList<>();
        tags.add("@automated");

        // Add tags based on actual elements found
        if (analysis.getForms() != null && !analysis.getForms().isEmpty()) {
            tags.add("@form");
        }

        // Check for specific element types
        boolean hasSearch = analysis.getElements() != null && analysis.getElements().stream()
                .anyMatch(e -> e.getInputType() != null && e.getInputType().equals("search") ||
                        (e.getSemanticName() != null && e.getSemanticName().toLowerCase().contains("search")));
        if (hasSearch) tags.add("@search");

        boolean hasFileUpload = analysis.getElements() != null && analysis.getElements().stream()
                .anyMatch(e -> "file".equals(e.getInputType()));
        if (hasFileUpload) tags.add("@file-upload");

        return tags;
    }

    /**
     * Generate background steps.
     */
    private List<String> generateBackground(PageAnalysis analysis) {
        List<String> background = new ArrayList<>();
        if (analysis.getUrl() != null) {
            background.add("the user navigates to \"" + analysis.getUrl() + "\"");
        } else {
            background.add("the user is on the page");
        }
        background.add("the page loads successfully");
        return background;
    }

    // ============================================
    // Form-Based Scenario Generation
    // ============================================

    /**
     * Generate scenarios for a specific form based on its actual fields.
     */
    private List<Scenario> generateFormScenarios(FormInfo form, PageAnalysis analysis) {
        List<Scenario> scenarios = new ArrayList<>();

        if (form.getFields() == null || form.getFields().isEmpty()) {
            return scenarios;
        }

        String formName = form.getIdentifier() != null ? form.getIdentifier() :
                (form.getPurpose() != null ? form.getPurpose() : "form");

        // 1. Happy path - fill all fields with valid data
        scenarios.add(generateFormHappyPathScenario(form, formName));

        // 2. Required field validation - only if there are required fields
        List<PageElement> requiredFields = form.getFields().stream()
                .filter(PageElement::isRequired)
                .collect(Collectors.toList());

        if (!requiredFields.isEmpty()) {
            scenarios.add(generateRequiredFieldsScenario(form, requiredFields, formName));
        }

        // 3. Individual field scenarios based on field types
        for (PageElement field : form.getFields()) {
            List<Scenario> fieldScenarios = generateFieldSpecificScenarios(field, formName);
            scenarios.addAll(fieldScenarios);
        }

        return scenarios;
    }

    /**
     * Generate happy path scenario for form submission.
     */
    private Scenario generateFormHappyPathScenario(FormInfo form, String formName) {
        List<String> whenSteps = new ArrayList<>();

        for (PageElement field : form.getFields()) {
            String fieldName = getFieldDisplayName(field);
            String action = generateFieldFillAction(field, fieldName);
            whenSteps.add(action);
        }

        // Add submit action
        if (form.getSubmitButton() != null) {
            String buttonText = form.getSubmitButton().getVisibleText();
            if (buttonText != null && !buttonText.isEmpty()) {
                whenSteps.add("the user clicks the \"" + buttonText + "\" button");
            } else {
                whenSteps.add("the user submits the form");
            }
        } else {
            whenSteps.add("the user submits the form");
        }

        return Scenario.builder()
                .name("Successfully submit " + formName + " with valid data")
                .type("Scenario")
                .category("happy path")
                .tags(List.of("@smoke", "@happy-path"))
                .givenSteps(List.of("the user is on the page with the " + formName))
                .whenSteps(whenSteps)
                .thenSteps(List.of(
                        "the form should be submitted successfully",
                        "the user should see a success confirmation or be redirected"
                ))
                .build();
    }

    /**
     * Generate required fields validation scenario.
     */
    private Scenario generateRequiredFieldsScenario(FormInfo form, List<PageElement> requiredFields, String formName) {
        List<String> thenSteps = new ArrayList<>();
        thenSteps.add("the form submission should be prevented");

        for (PageElement field : requiredFields) {
            String fieldName = getFieldDisplayName(field);
            thenSteps.add("a validation error should appear for the \"" + fieldName + "\" field");
        }

        return Scenario.builder()
                .name("Validation errors shown when required fields are empty in " + formName)
                .type("Scenario")
                .category("validation")
                .tags(List.of("@validation", "@required"))
                .givenSteps(List.of("the user is on the page with the " + formName))
                .whenSteps(List.of(
                        "the user leaves all required fields empty",
                        "the user attempts to submit the form"
                ))
                .thenSteps(thenSteps)
                .build();
    }

    /**
     * Generate scenarios specific to a field's type and attributes.
     */
    private List<Scenario> generateFieldSpecificScenarios(PageElement field, String formName) {
        List<Scenario> scenarios = new ArrayList<>();
        String fieldName = getFieldDisplayName(field);
        String inputType = field.getInputType();

        // Email field validation
        if ("email".equals(inputType)) {
            scenarios.add(Scenario.builder()
                    .name("Validate email format in \"" + fieldName + "\" field")
                    .type("Scenario Outline")
                    .category("validation")
                    .tags(List.of("@validation", "@email"))
                    .givenSteps(List.of("the user is on the page with the " + formName))
                    .whenSteps(List.of("the user enters \"<email>\" in the \"" + fieldName + "\" field"))
                    .thenSteps(List.of("the field should show <validation_state>"))
                    .examples(List.of(ScenarioExample.builder()
                            .headers(List.of("email", "validation_state"))
                            .rows(List.of(
                                    List.of("user@example.com", "valid"),
                                    List.of("invalid-email", "invalid format error"),
                                    List.of("user@", "invalid format error"),
                                    List.of("", field.isRequired() ? "required field error" : "no error")
                            ))
                            .build()))
                    .build());
        }

        // Number field with min/max
        if ("number".equals(inputType) && (field.getMinValue() != null || field.getMaxValue() != null)) {
            List<List<String>> rows = new ArrayList<>();
            if (field.getMinValue() != null) {
                int min = Integer.parseInt(field.getMinValue());
                rows.add(List.of(String.valueOf(min - 1), "below minimum error"));
                rows.add(List.of(String.valueOf(min), "valid"));
            }
            if (field.getMaxValue() != null) {
                int max = Integer.parseInt(field.getMaxValue());
                rows.add(List.of(String.valueOf(max), "valid"));
                rows.add(List.of(String.valueOf(max + 1), "above maximum error"));
            }

            if (!rows.isEmpty()) {
                scenarios.add(Scenario.builder()
                        .name("Validate number boundaries in \"" + fieldName + "\" field")
                        .type("Scenario Outline")
                        .category("boundary")
                        .tags(List.of("@validation", "@boundary"))
                        .givenSteps(List.of("the user is on the page with the " + formName))
                        .whenSteps(List.of("the user enters \"<value>\" in the \"" + fieldName + "\" field"))
                        .thenSteps(List.of("the field should show <validation_state>"))
                        .examples(List.of(ScenarioExample.builder()
                                .headers(List.of("value", "validation_state"))
                                .rows(rows)
                                .build()))
                        .build());
            }
        }

        // Text field with min/max length
        if ((field.getMinLength() != null || field.getMaxLength() != null) &&
                ("text".equals(inputType) || "textarea".equals(field.getType()))) {
            List<String> whenSteps = new ArrayList<>();
            List<String> thenSteps = new ArrayList<>();

            if (field.getMaxLength() != null) {
                whenSteps.add("the user enters text exceeding " + field.getMaxLength() + " characters in \"" + fieldName + "\"");
                thenSteps.add("the input should be limited to " + field.getMaxLength() + " characters or show an error");
            }
            if (field.getMinLength() != null) {
                whenSteps.add("the user enters text with fewer than " + field.getMinLength() + " characters in \"" + fieldName + "\"");
                thenSteps.add("a minimum length validation error should be displayed");
            }

            if (!whenSteps.isEmpty()) {
                scenarios.add(Scenario.builder()
                        .name("Validate character limits in \"" + fieldName + "\" field")
                        .type("Scenario")
                        .category("boundary")
                        .tags(List.of("@validation", "@boundary"))
                        .givenSteps(List.of("the user is on the page with the " + formName))
                        .whenSteps(whenSteps)
                        .thenSteps(thenSteps)
                        .build());
            }
        }

        // Password field
        if ("password".equals(inputType)) {
            scenarios.add(Scenario.builder()
                    .name("Password field \"" + fieldName + "\" masks input")
                    .type("Scenario")
                    .category("security")
                    .tags(List.of("@security", "@password"))
                    .givenSteps(List.of("the user is on the page with the " + formName))
                    .whenSteps(List.of("the user enters text in the \"" + fieldName + "\" field"))
                    .thenSteps(List.of("the entered text should be masked (shown as dots or asterisks)"))
                    .build());
        }

        // Checkbox
        if ("checkbox".equals(inputType)) {
            scenarios.add(Scenario.builder()
                    .name("Toggle \"" + fieldName + "\" checkbox")
                    .type("Scenario")
                    .category("interaction")
                    .tags(List.of("@interaction"))
                    .givenSteps(List.of("the user is on the page with the " + formName))
                    .whenSteps(List.of("the user clicks on the \"" + fieldName + "\" checkbox"))
                    .thenSteps(List.of(
                            "the checkbox state should toggle",
                            "the visual indicator should reflect the new state"
                    ))
                    .build());
        }

        // Select/Dropdown
        if ("select".equals(field.getType())) {
            scenarios.add(Scenario.builder()
                    .name("Select option from \"" + fieldName + "\" dropdown")
                    .type("Scenario")
                    .category("interaction")
                    .tags(List.of("@interaction", "@dropdown"))
                    .givenSteps(List.of("the user is on the page with the " + formName))
                    .whenSteps(List.of(
                            "the user clicks on the \"" + fieldName + "\" dropdown",
                            "the user selects an option from the list"
                    ))
                    .thenSteps(List.of(
                            "the selected option should be displayed in the dropdown",
                            "any dependent fields should update accordingly"
                    ))
                    .build());
        }

        // File upload
        if ("file".equals(inputType)) {
            scenarios.add(Scenario.builder()
                    .name("Upload file using \"" + fieldName + "\" input")
                    .type("Scenario")
                    .category("file-upload")
                    .tags(List.of("@file-upload"))
                    .givenSteps(List.of("the user is on the page with the " + formName))
                    .whenSteps(List.of("the user selects a valid file to upload via \"" + fieldName + "\""))
                    .thenSteps(List.of(
                            "the selected file name should be displayed",
                            "the file should be ready for form submission"
                    ))
                    .build());
        }

        return scenarios;
    }

    // ============================================
    // Element-Based Scenario Generation
    // ============================================

    /**
     * Generate scenarios for standalone buttons and interactive elements.
     */
    private List<Scenario> generateElementScenarios(PageAnalysis analysis) {
        List<Scenario> scenarios = new ArrayList<>();

        if (analysis.getElements() == null) return scenarios;

        // Find buttons that are not form submit buttons
        List<PageElement> standaloneButtons = analysis.getElements().stream()
                .filter(e -> "button".equals(e.getType()) || "role-button".equals(e.getType()))
                .filter(e -> e.getInputType() == null || !"submit".equals(e.getInputType()))
                .filter(e -> e.getVisibleText() != null && !e.getVisibleText().isEmpty())
                .collect(Collectors.toList());

        for (PageElement button : standaloneButtons) {
            String buttonText = button.getVisibleText();
            scenarios.add(Scenario.builder()
                    .name("Click \"" + buttonText + "\" button")
                    .type("Scenario")
                    .category("interaction")
                    .tags(List.of("@interaction", "@button"))
                    .givenSteps(List.of("the user is on the page"))
                    .whenSteps(List.of("the user clicks the \"" + buttonText + "\" button"))
                    .thenSteps(List.of(
                            "the expected action should be triggered",
                            "appropriate feedback should be displayed to the user"
                    ))
                    .build());
        }

        // Search inputs (standalone)
        List<PageElement> searchInputs = analysis.getElements().stream()
                .filter(e -> "search".equals(e.getInputType()) ||
                        (e.getSemanticName() != null && e.getSemanticName().toLowerCase().contains("search")))
                .collect(Collectors.toList());

        for (PageElement search : searchInputs) {
            String searchName = getFieldDisplayName(search);
            scenarios.add(Scenario.builder()
                    .name("Perform search using \"" + searchName + "\"")
                    .type("Scenario")
                    .category("search")
                    .tags(List.of("@search"))
                    .givenSteps(List.of("the user is on the page"))
                    .whenSteps(List.of(
                            "the user enters a search term in the \"" + searchName + "\" field",
                            "the user initiates the search"
                    ))
                    .thenSteps(List.of(
                            "search results should be displayed",
                            "results should be relevant to the search term"
                    ))
                    .build());

            // Empty search
            scenarios.add(Scenario.builder()
                    .name("Submit empty search in \"" + searchName + "\"")
                    .type("Scenario")
                    .category("edge-case")
                    .tags(List.of("@search", "@edge-case"))
                    .givenSteps(List.of("the user is on the page"))
                    .whenSteps(List.of(
                            "the user leaves the \"" + searchName + "\" field empty",
                            "the user initiates the search"
                    ))
                    .thenSteps(List.of(
                            "an appropriate message should be displayed",
                            "the system should handle the empty search gracefully"
                    ))
                    .build());
        }

        return scenarios;
    }

    // ============================================
    // Navigation Scenario Generation
    // ============================================

    /**
     * Generate scenarios for navigation links.
     */
    private List<Scenario> generateNavigationScenarios(PageAnalysis analysis) {
        List<Scenario> scenarios = new ArrayList<>();

        if (analysis.getNavigationLinks() == null || analysis.getNavigationLinks().isEmpty()) {
            return scenarios;
        }

        // Get meaningful internal links (skip empty text, anchors, etc.)
        List<LinkInfo> meaningfulLinks = analysis.getNavigationLinks().stream()
                .filter(LinkInfo::isInternal)
                .filter(l -> l.getText() != null && !l.getText().trim().isEmpty())
                .filter(l -> !l.getText().equals("#"))
                .limit(10) // Limit to avoid too many scenarios
                .collect(Collectors.toList());

        if (!meaningfulLinks.isEmpty()) {
            List<List<String>> linkRows = meaningfulLinks.stream()
                    .map(l -> List.of(l.getText().trim()))
                    .collect(Collectors.toList());

            scenarios.add(Scenario.builder()
                    .name("Navigate using internal links")
                    .type("Scenario Outline")
                    .category("navigation")
                    .tags(List.of("@navigation"))
                    .givenSteps(List.of("the user is on the page"))
                    .whenSteps(List.of("the user clicks on the \"<link_text>\" link"))
                    .thenSteps(List.of(
                            "the user should be navigated to the target page",
                            "the target page should load without errors"
                    ))
                    .examples(List.of(ScenarioExample.builder()
                            .headers(List.of("link_text"))
                            .rows(linkRows)
                            .build()))
                    .build());
        }

        return scenarios;
    }

    // ============================================
    // Basic Page Scenarios (Fallback)
    // ============================================

    /**
     * Generate basic scenarios when no specific elements are found.
     */
    private List<Scenario> generateBasicPageScenarios(PageAnalysis analysis) {
        List<Scenario> scenarios = new ArrayList<>();

        scenarios.add(Scenario.builder()
                .name("Page loads successfully")
                .type("Scenario")
                .category("smoke")
                .tags(List.of("@smoke"))
                .givenSteps(List.of("the user has a valid browser session"))
                .whenSteps(List.of("the user navigates to the page"))
                .thenSteps(List.of(
                        "the page should load without errors",
                        "the page title should be displayed correctly"
                ))
                .build());

        scenarios.add(Scenario.builder()
                .name("Page is responsive")
                .type("Scenario")
                .category("accessibility")
                .tags(List.of("@accessibility", "@responsive"))
                .givenSteps(List.of("the user is on the page"))
                .whenSteps(List.of("the viewport is resized to mobile dimensions"))
                .thenSteps(List.of(
                        "the page content should adapt to the viewport",
                        "all content should remain accessible"
                ))
                .build());

        return scenarios;
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Get a human-readable display name for a field.
     */
    private String getFieldDisplayName(PageElement field) {
        if (field.getLabel() != null && !field.getLabel().isEmpty()) {
            return field.getLabel();
        }
        if (field.getAriaLabel() != null && !field.getAriaLabel().isEmpty()) {
            return field.getAriaLabel();
        }
        if (field.getPlaceholder() != null && !field.getPlaceholder().isEmpty()) {
            return field.getPlaceholder();
        }
        if (field.getSemanticName() != null && !field.getSemanticName().isEmpty()) {
            return field.getSemanticName();
        }
        if (field.getName() != null && !field.getName().isEmpty()) {
            return capitalizeWords(field.getName().replace("_", " ").replace("-", " "));
        }
        if (field.getId() != null && !field.getId().isEmpty()) {
            return capitalizeWords(field.getId().replace("_", " ").replace("-", " "));
        }
        return field.getType() + " field";
    }

    /**
     * Generate appropriate fill action based on field type.
     */
    private String generateFieldFillAction(PageElement field, String fieldName) {
        String inputType = field.getInputType();

        if ("checkbox".equals(inputType)) {
            return "the user checks the \"" + fieldName + "\" checkbox if required";
        }
        if ("radio".equals(inputType)) {
            return "the user selects an appropriate \"" + fieldName + "\" option";
        }
        if ("select".equals(field.getType())) {
            return "the user selects an option from the \"" + fieldName + "\" dropdown";
        }
        if ("file".equals(inputType)) {
            return "the user uploads a valid file via \"" + fieldName + "\"";
        }
        if ("email".equals(inputType)) {
            return "the user enters a valid email address in the \"" + fieldName + "\" field";
        }
        if ("password".equals(inputType)) {
            return "the user enters a valid password in the \"" + fieldName + "\" field";
        }
        if ("number".equals(inputType)) {
            return "the user enters a valid number in the \"" + fieldName + "\" field";
        }
        if ("tel".equals(inputType)) {
            return "the user enters a valid phone number in the \"" + fieldName + "\" field";
        }
        if ("url".equals(inputType)) {
            return "the user enters a valid URL in the \"" + fieldName + "\" field";
        }
        if ("date".equals(inputType) || "datetime-local".equals(inputType)) {
            return "the user selects a valid date in the \"" + fieldName + "\" field";
        }

        return "the user enters valid data in the \"" + fieldName + "\" field";
    }

    /**
     * Capitalize first letter of each word.
     */
    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        return Arrays.stream(str.split("\\s+"))
                .map(word -> word.isEmpty() ? word :
                        Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
