package com.enterprise.testgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with Ollama local LLM.
 * Provides AI-powered enhancements for test generation while keeping data on-premises.
 */
@Service
public class OllamaService {

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:llama3.2}")
    private String defaultModel;

    @Value("${ollama.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if Ollama is available and responding.
     */
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[OllamaService] Ollama not available: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the status of Ollama including available models.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("url", ollamaUrl);
        status.put("model", defaultModel);

        if (!enabled) {
            status.put("available", false);
            status.put("message", "Ollama is disabled in configuration");
            return status;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                status.put("available", true);
                status.put("models", json.get("models"));
                status.put("message", "Ollama is running");
            } else {
                status.put("available", false);
                status.put("message", "Ollama returned status " + response.statusCode());
            }
        } catch (Exception e) {
            status.put("available", false);
            status.put("message", "Cannot connect to Ollama: " + e.getMessage());
        }

        return status;
    }

    /**
     * Generate enhanced BDD scenarios using AI.
     */
    public String enhanceBddScenarios(String pageAnalysis, String currentScenarios) {
        if (!isAvailable()) {
            return currentScenarios;
        }

        String prompt = buildBddEnhancementPrompt(pageAnalysis, currentScenarios);
        return generate(prompt, 2000);
    }

    /**
     * Generate improved test code using AI.
     */
    public String enhanceTestCode(String requirements, String currentCode) {
        if (!isAvailable()) {
            return currentCode;
        }

        String prompt = buildCodeEnhancementPrompt(requirements, currentCode);
        return generate(prompt, 3000);
    }

    /**
     * Analyze page and suggest additional test scenarios.
     */
    public String suggestAdditionalScenarios(String pageAnalysis) {
        if (!isAvailable()) {
            return "";
        }

        String prompt = """
            You are a QA expert. Based on this page analysis, suggest additional BDD test scenarios
            that cover edge cases, error handling, and accessibility concerns.

            Page Analysis:
            %s

            Respond with Gherkin scenarios only, no explanations. Focus on:
            - Error handling scenarios
            - Boundary conditions
            - Accessibility checks
            - Security considerations
            """.formatted(pageAnalysis);

        return generate(prompt, 1500);
    }

    /**
     * Generate text using Ollama.
     */
    public String generate(String prompt, int maxTokens) {
        if (!isAvailable()) {
            return "";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", defaultModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                    "num_predict", maxTokens,
                    "temperature", 0.7
            ));

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            System.out.println("[OllamaService] Sending request to Ollama...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String result = json.get("response").asText();
                System.out.println("[OllamaService] Generated " + result.length() + " characters");
                return result;
            } else {
                System.err.println("[OllamaService] Error: " + response.statusCode() + " - " + response.body());
                return "";
            }
        } catch (Exception e) {
            System.err.println("[OllamaService] Generation failed: " + e.getMessage());
            return "";
        }
    }

    private String buildBddEnhancementPrompt(String pageAnalysis, String currentScenarios) {
        return """
            You are an expert QA engineer. Review these BDD scenarios and enhance them with:
            1. More specific Given/When/Then steps
            2. Data-driven examples where appropriate
            3. Better scenario names
            4. Additional edge case scenarios

            Page Analysis:
            %s

            Current Scenarios:
            %s

            Return improved Gherkin scenarios. Keep the same structure but make them more robust and comprehensive.
            Only output valid Gherkin syntax, no explanations.
            """.formatted(pageAnalysis, currentScenarios);
    }

    private String buildCodeEnhancementPrompt(String requirements, String currentCode) {
        return """
            You are an expert Java test automation engineer. Review this test code and enhance it with:
            1. Better assertions
            2. Improved wait strategies
            3. More descriptive method names
            4. Error handling

            Requirements:
            %s

            Current Code:
            %s

            Return improved Java code only, no explanations. Maintain the same structure and patterns.
            """.formatted(requirements, currentCode);
    }
}
