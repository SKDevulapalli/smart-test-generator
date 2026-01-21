package com.enterprise.testgen.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single step in a test case.
 */
@Data
@Builder
public class TestStep {

    /** Step number in sequence */
    private int stepNumber;

    /** Action to perform */
    private String action;

    /** Test data or input values */
    private String testData;

    /** Expected result for this step */
    private String expectedResult;
}
