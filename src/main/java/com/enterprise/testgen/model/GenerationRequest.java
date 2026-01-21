package com.enterprise.testgen.model;

import lombok.Data;

/**
 * Request object for test generation.
 */
@Data
public class GenerationRequest {

    /** Source of requirements: WORD_DOCUMENT or GITHUB_REPOSITORY */
    private InputSource inputSource;

    /** Type of tests to generate: UI or API */
    private TestType testType;

    /** GitHub repository URL (if inputSource is GITHUB_REPOSITORY) */
    private String repositoryUrl;

    /** GitHub personal access token for private repos */
    private String githubToken;

    /** Extracted content from uploaded document or repository */
    private String requirementsContent;

    /** Base package name for generated tests */
    private String basePackage;

    /** Application base URL for UI tests */
    private String applicationUrl;

    /** API base URL for API tests */
    private String apiBaseUrl;

    // Controls Panel Options
    /** Test category: smoke, regression, negative, boundary, security */
    private String testCategory = "smoke";

    /** Coverage level: 1 (minimal) to 5 (extensive) */
    private Integer coverageLevel = 3;

    /** Assertion strictness: loose, medium, strict */
    private String assertionStrictness = "medium";

    /** Wait strategy: implicit, explicit, smart */
    private String waitStrategy = "explicit";

    /** Generate negative test cases */
    private Boolean generateNegative = true;

    /** Add assertions automatically */
    private Boolean addAssertions = true;

    /** Auto-handle dynamic elements */
    private Boolean handleDynamic = true;

    /** Generate reusable utilities */
    private Boolean generateUtilities = true;
}
