package com.enterprise.testgen.model;

/**
 * Enum representing the source of requirements input.
 */
public enum InputSource {
    WORD_DOCUMENT("Word Document Upload"),
    GITHUB_REPOSITORY("GitHub Repository (Read-Only)");

    private final String displayName;

    InputSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
