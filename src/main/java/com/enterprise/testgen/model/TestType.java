package com.enterprise.testgen.model;

/**
 * Enum representing the type of test to generate.
 * UI tests use Selenium WebDriver, API tests use Rest Assured.
 */
public enum TestType {
    UI("UI Test - Selenium WebDriver"),
    API("API Test - Rest Assured");

    private final String displayName;

    TestType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
