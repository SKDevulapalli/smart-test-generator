package com.enterprise.testgen.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Response object containing generated test cases.
 */
@Data
@Builder
public class GenerationResponse {

    /** Whether generation was successful */
    private boolean success;

    /** Message describing the result */
    private String message;

    /** List of generated test cases */
    private List<TestCase> testCases;

    /** Total number of tests generated */
    private int totalTests;

    /** Time taken to generate tests in milliseconds */
    private long generationTimeMs;

    /** Framework version information */
    private String frameworkInfo;

    /** Any warnings or informational messages */
    private List<String> warnings;
}
