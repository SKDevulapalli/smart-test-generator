package com.enterprise.testgen.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Result of analyzing a live web page DOM.
 */
@Data
@Builder
public class PageAnalysis {

    /** The URL that was analyzed */
    private String url;

    /** Page title */
    private String title;

    /** Inferred page purpose (login, signup, dashboard, etc.) */
    private String pagePurpose;

    /** Description of what the page appears to be for */
    private String pageDescription;

    /** All discovered interactive elements */
    private List<PageElement> elements;

    /** Discovered forms on the page */
    private List<FormInfo> forms;

    /** Navigation links found */
    private List<LinkInfo> navigationLinks;

    /** Inferred user flows based on page structure */
    private List<String> inferredFlows;

    /** Potential edge cases identified */
    private List<String> edgeCases;

    /** Security considerations noted */
    private List<String> securityConsiderations;

    /** Analysis metadata */
    private AnalysisMetadata metadata;

    /**
     * Represents an interactive page element.
     */
    @Data
    @Builder
    public static class PageElement {
        /** Element type (input, button, select, textarea, etc.) */
        private String type;

        /** Semantic name/purpose of the element */
        private String semanticName;

        /** Input type attribute if applicable */
        private String inputType;

        /** Label text associated with the element */
        private String label;

        /** Placeholder text */
        private String placeholder;

        /** ARIA label */
        private String ariaLabel;

        /** ARIA describedby text */
        private String ariaDescribedBy;

        /** Visible text content */
        private String visibleText;

        /** Whether the field is required */
        private boolean required;

        /** Validation pattern if specified */
        private String validationPattern;

        /** Min/max length constraints */
        private Integer minLength;
        private Integer maxLength;

        /** Min/max value constraints for number inputs */
        private String minValue;
        private String maxValue;

        /** ID attribute */
        private String id;

        /** Name attribute */
        private String name;

        /** CSS classes */
        private String cssClasses;

        /** Data attributes that might be useful */
        private Map<String, String> dataAttributes;

        /** Recommended locator strategy */
        private String recommendedLocator;

        /** Element role attribute */
        private String role;
    }

    /**
     * Information about a form on the page.
     */
    @Data
    @Builder
    public static class FormInfo {
        /** Form name or ID */
        private String identifier;

        /** Form action URL */
        private String action;

        /** HTTP method */
        private String method;

        /** Form purpose (login, registration, search, etc.) */
        private String purpose;

        /** Fields within the form */
        private List<PageElement> fields;

        /** Submit button info */
        private PageElement submitButton;

        /** Whether form has CSRF token */
        private boolean hasCsrfToken;

        /** Validation messages found */
        private List<String> validationMessages;
    }

    /**
     * Information about a navigation link.
     */
    @Data
    @Builder
    public static class LinkInfo {
        /** Link text */
        private String text;

        /** Link href */
        private String href;

        /** Whether it's internal or external */
        private boolean internal;

        /** Link purpose (navigation, action, etc.) */
        private String purpose;
    }

    /**
     * Metadata about the analysis.
     */
    @Data
    @Builder
    public static class AnalysisMetadata {
        /** Time taken to analyze in milliseconds */
        private long analysisTimeMs;

        /** Total elements found */
        private int totalElements;

        /** Total forms found */
        private int totalForms;

        /** Total links found */
        private int totalLinks;

        /** Browser used for analysis */
        private String browser;

        /** Viewport size used */
        private String viewport;

        /** Any warnings during analysis */
        private List<String> warnings;
    }
}
