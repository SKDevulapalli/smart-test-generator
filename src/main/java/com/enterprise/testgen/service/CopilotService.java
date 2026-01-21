package com.enterprise.testgen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Microsoft Copilot/Azure OpenAI integration service.
 * Enhances test generation with AI-powered suggestions and analysis.
 */
@Service
public class CopilotService {

    @Value("${azure.openai.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.openai.api-key:}")
    private String apiKey;

    @Value("${azure.openai.deployment:gpt-4}")
    private String deploymentName;

    @Value("${azure.openai.api-version:2024-02-15-preview}")
    private String apiVersion;

    private final RestTemplate restTemplate;

    public CopilotService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Check if Copilot integration is configured and enabled.
     */
    public boolean isEnabled() {
        return azureEndpoint != null && !azureEndpoint.isEmpty()
            && apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Enhance requirements analysis using Copilot.
     * Extracts test scenarios, edge cases, and suggestions from requirements.
     */
    public CopilotAnalysis analyzeRequirements(String requirements, String testType) {
        if (!isEnabled()) {
            return CopilotAnalysis.disabled();
        }

        try {
            String prompt = buildAnalysisPrompt(requirements, testType);
            String response = callCopilot(prompt);
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            return CopilotAnalysis.error("Copilot analysis failed: " + e.getMessage());
        }
    }

    /**
     * Generate improved test code using Copilot.
     */
    public String enhanceTestCode(String baseCode, String scenario, String testType) {
        if (!isEnabled()) {
            return baseCode;
        }

        try {
            String prompt = buildCodeEnhancementPrompt(baseCode, scenario, testType);
            return callCopilot(prompt);
        } catch (Exception e) {
            return baseCode; // Return original code on failure
        }
    }

    /**
     * Suggest additional test scenarios based on requirements.
     */
    public List<String> suggestAdditionalScenarios(String requirements, String testType) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        try {
            String prompt = buildSuggestionPrompt(requirements, testType);
            String response = callCopilot(prompt);
            return parseSuggestionsResponse(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Generate smart element locators using AI.
     */
    public Map<String, String> generateSmartLocators(String pageDescription) {
        if (!isEnabled()) {
            return Collections.emptyMap();
        }

        try {
            String prompt = buildLocatorPrompt(pageDescription);
            String response = callCopilot(prompt);
            return parseLocatorsResponse(response);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String buildAnalysisPrompt(String requirements, String testType) {
        return String.format("""
            Analyze the following requirements and extract test scenarios for %s testing.

            Requirements:
            %s

            Please provide:
            1. List of test scenarios with Given/When/Then format
            2. Identified edge cases and boundary conditions
            3. Potential gaps or missing information
            4. Suggested test data

            Format your response as JSON with keys: scenarios, edgeCases, gaps, testData
            """, testType, requirements);
    }

    private String buildCodeEnhancementPrompt(String baseCode, String scenario, String testType) {
        return String.format("""
            Enhance the following %s test code to be more robust and comprehensive.

            Scenario: %s

            Base Code:
            ```java
            %s
            ```

            Please:
            1. Add better error handling
            2. Improve assertions
            3. Add appropriate waits and retries
            4. Add meaningful comments
            5. Follow best practices for %s testing

            Return only the enhanced Java code.
            """, testType, scenario, baseCode, testType);
    }

    private String buildSuggestionPrompt(String requirements, String testType) {
        return String.format("""
            Based on these requirements, suggest additional %s test scenarios that should be covered:

            %s

            Consider:
            - Edge cases and boundary conditions
            - Error scenarios and negative testing
            - Security considerations
            - Performance scenarios

            Return a JSON array of scenario descriptions.
            """, testType, requirements);
    }

    private String buildLocatorPrompt(String pageDescription) {
        return String.format("""
            Generate robust XPath/CSS locators for the following page elements:

            %s

            For each element, provide:
            1. Primary locator (most reliable)
            2. Fallback locator (alternative)

            Return as JSON with element names as keys and locator objects as values.
            """, pageDescription);
    }

    private String callCopilot(String prompt) {
        String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                azureEndpoint, deploymentName, apiVersion);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a test automation expert specializing in Selenium and REST Assured. Provide practical, production-ready code and suggestions."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return "";
    }

    private CopilotAnalysis parseAnalysisResponse(String response) {
        // Parse the JSON response from Copilot
        CopilotAnalysis analysis = new CopilotAnalysis();
        analysis.setEnabled(true);
        analysis.setRawResponse(response);

        // Simple parsing - in production, use proper JSON parsing
        if (response.contains("scenarios")) {
            analysis.setScenarios(extractJsonArray(response, "scenarios"));
        }
        if (response.contains("edgeCases")) {
            analysis.setEdgeCases(extractJsonArray(response, "edgeCases"));
        }
        if (response.contains("gaps")) {
            analysis.setGaps(extractJsonArray(response, "gaps"));
        }

        return analysis;
    }

    private List<String> parseSuggestionsResponse(String response) {
        return extractJsonArray(response, null);
    }

    private Map<String, String> parseLocatorsResponse(String response) {
        // Simple parsing - in production, use proper JSON parsing
        Map<String, String> locators = new HashMap<>();
        // Parse response and populate locators map
        return locators;
    }

    private List<String> extractJsonArray(String json, String key) {
        List<String> items = new ArrayList<>();
        // Simple extraction - in production, use proper JSON parsing
        try {
            int start = key != null ? json.indexOf("\"" + key + "\"") : 0;
            if (start >= 0) {
                int arrayStart = json.indexOf("[", start);
                int arrayEnd = json.indexOf("]", arrayStart);
                if (arrayStart >= 0 && arrayEnd > arrayStart) {
                    String arrayContent = json.substring(arrayStart + 1, arrayEnd);
                    String[] parts = arrayContent.split("\",\\s*\"");
                    for (String part : parts) {
                        String cleaned = part.replaceAll("^\"|\"$", "").trim();
                        if (!cleaned.isEmpty()) {
                            items.add(cleaned);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return items;
    }

    /**
     * Analysis result from Copilot.
     */
    public static class CopilotAnalysis {
        private boolean enabled;
        private String error;
        private String rawResponse;
        private List<String> scenarios = new ArrayList<>();
        private List<String> edgeCases = new ArrayList<>();
        private List<String> gaps = new ArrayList<>();
        private Map<String, Object> testData = new HashMap<>();

        public static CopilotAnalysis disabled() {
            CopilotAnalysis analysis = new CopilotAnalysis();
            analysis.setEnabled(false);
            return analysis;
        }

        public static CopilotAnalysis error(String message) {
            CopilotAnalysis analysis = new CopilotAnalysis();
            analysis.setEnabled(true);
            analysis.setError(message);
            return analysis;
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
        public List<String> getScenarios() { return scenarios; }
        public void setScenarios(List<String> scenarios) { this.scenarios = scenarios; }
        public List<String> getEdgeCases() { return edgeCases; }
        public void setEdgeCases(List<String> edgeCases) { this.edgeCases = edgeCases; }
        public List<String> getGaps() { return gaps; }
        public void setGaps(List<String> gaps) { this.gaps = gaps; }
        public Map<String, Object> getTestData() { return testData; }
        public void setTestData(Map<String, Object> testData) { this.testData = testData; }
    }
}
