package com.enterprise.testgen.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Represents a BDD (Gherkin) scenario generated from page analysis.
 */
@Data
@Builder
public class BDDScenario {

    /** Feature name */
    private String feature;

    /** Feature description */
    private String featureDescription;

    /** List of scenarios */
    private List<Scenario> scenarios;

    /** Background steps (common setup) */
    private List<String> background;

    /** Tags for the feature */
    private List<String> tags;

    /**
     * Individual scenario.
     */
    @Data
    @Builder
    public static class Scenario {
        /** Scenario name */
        private String name;

        /** Scenario type (Scenario, Scenario Outline) */
        private String type;

        /** Scenario description */
        private String description;

        /** Tags specific to this scenario */
        private List<String> tags;

        /** Given steps */
        private List<String> givenSteps;

        /** When steps */
        private List<String> whenSteps;

        /** Then steps */
        private List<String> thenSteps;

        /** And/But steps (additional steps) */
        private List<String> additionalSteps;

        /** Examples for Scenario Outline */
        private List<ScenarioExample> examples;

        /** Category (happy path, validation, security, edge case) */
        private String category;
    }

    /**
     * Example data for Scenario Outline.
     */
    @Data
    @Builder
    public static class ScenarioExample {
        /** Column headers */
        private List<String> headers;

        /** Data rows */
        private List<List<String>> rows;
    }

    /**
     * Converts this BDD scenario to Gherkin format string.
     */
    public String toGherkin() {
        StringBuilder sb = new StringBuilder();

        // Feature tags
        if (tags != null && !tags.isEmpty()) {
            sb.append(String.join(" ", tags)).append("\n");
        }

        // Feature
        sb.append("Feature: ").append(feature).append("\n");
        if (featureDescription != null && !featureDescription.isEmpty()) {
            sb.append("  ").append(featureDescription).append("\n");
        }
        sb.append("\n");

        // Background
        if (background != null && !background.isEmpty()) {
            sb.append("  Background:\n");
            for (String step : background) {
                sb.append("    ").append(step).append("\n");
            }
            sb.append("\n");
        }

        // Scenarios
        if (scenarios != null) {
            for (Scenario scenario : scenarios) {
                // Scenario tags
                if (scenario.getTags() != null && !scenario.getTags().isEmpty()) {
                    sb.append("  ").append(String.join(" ", scenario.getTags())).append("\n");
                }

                // Scenario type and name
                sb.append("  ").append(scenario.getType() != null ? scenario.getType() : "Scenario")
                  .append(": ").append(scenario.getName()).append("\n");

                // Given steps
                if (scenario.getGivenSteps() != null) {
                    for (int i = 0; i < scenario.getGivenSteps().size(); i++) {
                        String prefix = i == 0 ? "Given " : "And ";
                        sb.append("    ").append(prefix).append(scenario.getGivenSteps().get(i)).append("\n");
                    }
                }

                // When steps
                if (scenario.getWhenSteps() != null) {
                    for (int i = 0; i < scenario.getWhenSteps().size(); i++) {
                        String prefix = i == 0 ? "When " : "And ";
                        sb.append("    ").append(prefix).append(scenario.getWhenSteps().get(i)).append("\n");
                    }
                }

                // Then steps
                if (scenario.getThenSteps() != null) {
                    for (int i = 0; i < scenario.getThenSteps().size(); i++) {
                        String prefix = i == 0 ? "Then " : "And ";
                        sb.append("    ").append(prefix).append(scenario.getThenSteps().get(i)).append("\n");
                    }
                }

                // Examples for Scenario Outline
                if (scenario.getExamples() != null && !scenario.getExamples().isEmpty()) {
                    for (ScenarioExample example : scenario.getExamples()) {
                        sb.append("\n    Examples:\n");
                        if (example.getHeaders() != null) {
                            sb.append("      | ");
                            sb.append(String.join(" | ", example.getHeaders()));
                            sb.append(" |\n");
                        }
                        if (example.getRows() != null) {
                            for (List<String> row : example.getRows()) {
                                sb.append("      | ");
                                sb.append(String.join(" | ", row));
                                sb.append(" |\n");
                            }
                        }
                    }
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
