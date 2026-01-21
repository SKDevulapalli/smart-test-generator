package com.enterprise.testgen.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Represents a generated test case with all required components
 * for enterprise automation framework integration.
 */
@Data
@Builder
public class TestCase {

    /** Unique identifier for the test case */
    private String testId;

    /** Clear, descriptive test name following naming conventions */
    private String testName;

    /** Type of test: UI or API */
    private TestType testType;

    /** Brief description of what the test validates */
    private String scenarioDescription;

    /** Required setup conditions before test execution */
    private List<String> preconditions;

    /** Step-by-step test execution instructions */
    private List<TestStep> testSteps;

    /** Expected outcomes after test execution */
    private List<String> expectedResults;

    /** Generated Java automation code */
    private String automationCode;

    /** Class name for the generated test */
    private String className;

    /** Package name for the generated test */
    private String packageName;

    /** Any gaps or missing information identified */
    private List<String> identifiedGaps;
}
