package com.enterprise.testgen.controller;

import com.enterprise.testgen.model.*;
import com.enterprise.testgen.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for test generation endpoints.
 */
@Controller
public class TestGeneratorController {

    @Autowired
    private TestGeneratorService generatorService;

    @Autowired
    private DocumentParserService documentParser;

    @Autowired
    private GitHubReaderService gitHubReader;

    @Autowired
    private CopilotService copilotService;

    @Autowired
    private WebPageAnalyzerService webPageAnalyzer;

    @Autowired
    private BDDScenarioGeneratorService bddGenerator;

    @Autowired
    private OllamaService ollamaService;

    /**
     * Main page
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Documentation page
     */
    @GetMapping("/docs")
    public String docs() {
        return "docs";
    }

    /**
     * API documentation page
     */
    @GetMapping("/api-docs")
    public String apiDocs() {
        return "api-docs";
    }

    /**
     * Generate tests from uploaded Word document.
     */
    @PostMapping("/api/generate/document")
    @ResponseBody
    public ResponseEntity<GenerationResponse> generateFromDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("testType") String testType,
            @RequestParam(value = "basePackage", defaultValue = "com.enterprise.tests") String basePackage,
            @RequestParam(value = "applicationUrl", required = false) String applicationUrl,
            @RequestParam(value = "apiBaseUrl", required = false) String apiBaseUrl,
            @RequestParam(value = "testCategory", defaultValue = "smoke") String testCategory,
            @RequestParam(value = "coverageLevel", defaultValue = "3") Integer coverageLevel,
            @RequestParam(value = "assertionStrictness", defaultValue = "medium") String assertionStrictness,
            @RequestParam(value = "waitStrategy", defaultValue = "explicit") String waitStrategy,
            @RequestParam(value = "generateNegative", defaultValue = "true") Boolean generateNegative,
            @RequestParam(value = "addAssertions", defaultValue = "true") Boolean addAssertions,
            @RequestParam(value = "handleDynamic", defaultValue = "true") Boolean handleDynamic,
            @RequestParam(value = "generateUtilities", defaultValue = "true") Boolean generateUtilities) {

        try {
            // Parse document
            String content = documentParser.parseWordDocument(file);

            // Create request
            GenerationRequest request = new GenerationRequest();
            request.setInputSource(InputSource.WORD_DOCUMENT);
            request.setTestType(TestType.valueOf(testType.toUpperCase()));
            request.setRequirementsContent(content);
            request.setBasePackage(basePackage);
            request.setApplicationUrl(applicationUrl);
            request.setApiBaseUrl(apiBaseUrl);
            // Control panel options
            request.setTestCategory(testCategory);
            request.setCoverageLevel(coverageLevel);
            request.setAssertionStrictness(assertionStrictness);
            request.setWaitStrategy(waitStrategy);
            request.setGenerateNegative(generateNegative);
            request.setAddAssertions(addAssertions);
            request.setHandleDynamic(handleDynamic);
            request.setGenerateUtilities(generateUtilities);

            // Generate tests
            GenerationResponse response = generatorService.generateTests(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    GenerationResponse.builder()
                            .success(false)
                            .message("Error processing document: " + e.getMessage())
                            .warnings(List.of("Exception: " + e.getClass().getSimpleName()))
                            .build()
            );
        }
    }

    /**
     * Generate tests from GitHub repository.
     */
    @PostMapping("/api/generate/github")
    @ResponseBody
    public ResponseEntity<GenerationResponse> generateFromGitHub(
            @RequestParam("repositoryUrl") String repositoryUrl,
            @RequestParam("testType") String testType,
            @RequestParam(value = "githubToken", required = false) String githubToken,
            @RequestParam(value = "basePackage", defaultValue = "com.enterprise.tests") String basePackage,
            @RequestParam(value = "applicationUrl", required = false) String applicationUrl,
            @RequestParam(value = "apiBaseUrl", required = false) String apiBaseUrl,
            @RequestParam(value = "testCategory", defaultValue = "smoke") String testCategory,
            @RequestParam(value = "coverageLevel", defaultValue = "3") Integer coverageLevel,
            @RequestParam(value = "assertionStrictness", defaultValue = "medium") String assertionStrictness,
            @RequestParam(value = "waitStrategy", defaultValue = "explicit") String waitStrategy,
            @RequestParam(value = "generateNegative", defaultValue = "true") Boolean generateNegative,
            @RequestParam(value = "addAssertions", defaultValue = "true") Boolean addAssertions,
            @RequestParam(value = "handleDynamic", defaultValue = "true") Boolean handleDynamic,
            @RequestParam(value = "generateUtilities", defaultValue = "true") Boolean generateUtilities) {

        try {
            // Read repository content
            String content = gitHubReader.readRepositoryContent(repositoryUrl, githubToken);

            // Create request
            GenerationRequest request = new GenerationRequest();
            request.setInputSource(InputSource.GITHUB_REPOSITORY);
            request.setTestType(TestType.valueOf(testType.toUpperCase()));
            request.setRepositoryUrl(repositoryUrl);
            request.setGithubToken(githubToken);
            request.setRequirementsContent(content);
            request.setBasePackage(basePackage);
            request.setApplicationUrl(applicationUrl);
            request.setApiBaseUrl(apiBaseUrl);
            // Control panel options
            request.setTestCategory(testCategory);
            request.setCoverageLevel(coverageLevel);
            request.setAssertionStrictness(assertionStrictness);
            request.setWaitStrategy(waitStrategy);
            request.setGenerateNegative(generateNegative);
            request.setAddAssertions(addAssertions);
            request.setHandleDynamic(handleDynamic);
            request.setGenerateUtilities(generateUtilities);

            // Generate tests
            GenerationResponse response = generatorService.generateTests(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    GenerationResponse.builder()
                            .success(false)
                            .message("Error reading repository: " + e.getMessage())
                            .warnings(List.of("Exception: " + e.getClass().getSimpleName()))
                            .build()
            );
        }
    }

    /**
     * Generate tests from direct text input.
     */
    @PostMapping("/api/generate/text")
    @ResponseBody
    public ResponseEntity<GenerationResponse> generateFromText(
            @RequestBody Map<String, Object> payload) {

        try {
            String content = (String) payload.get("content");
            String testType = (String) payload.getOrDefault("testType", "UI");
            String basePackage = (String) payload.getOrDefault("basePackage", "com.enterprise.tests");
            String applicationUrl = (String) payload.get("applicationUrl");
            String apiBaseUrl = (String) payload.get("apiBaseUrl");
            // Control panel options
            String testCategory = (String) payload.getOrDefault("testCategory", "smoke");
            Integer coverageLevel = payload.get("coverageLevel") != null ?
                    Integer.parseInt(payload.get("coverageLevel").toString()) : 3;
            String assertionStrictness = (String) payload.getOrDefault("assertionStrictness", "medium");
            String waitStrategy = (String) payload.getOrDefault("waitStrategy", "explicit");
            Boolean generateNegative = payload.get("generateNegative") != null ?
                    Boolean.parseBoolean(payload.get("generateNegative").toString()) : true;
            Boolean addAssertions = payload.get("addAssertions") != null ?
                    Boolean.parseBoolean(payload.get("addAssertions").toString()) : true;
            Boolean handleDynamic = payload.get("handleDynamic") != null ?
                    Boolean.parseBoolean(payload.get("handleDynamic").toString()) : true;
            Boolean generateUtilities = payload.get("generateUtilities") != null ?
                    Boolean.parseBoolean(payload.get("generateUtilities").toString()) : true;

            // Create request
            GenerationRequest request = new GenerationRequest();
            request.setInputSource(InputSource.WORD_DOCUMENT); // Treat as document
            request.setTestType(TestType.valueOf(testType.toUpperCase()));
            request.setRequirementsContent(content);
            request.setBasePackage(basePackage);
            request.setApplicationUrl(applicationUrl);
            request.setApiBaseUrl(apiBaseUrl);
            // Control panel options
            request.setTestCategory(testCategory);
            request.setCoverageLevel(coverageLevel);
            request.setAssertionStrictness(assertionStrictness);
            request.setWaitStrategy(waitStrategy);
            request.setGenerateNegative(generateNegative);
            request.setAddAssertions(addAssertions);
            request.setHandleDynamic(handleDynamic);
            request.setGenerateUtilities(generateUtilities);

            // Generate tests
            GenerationResponse response = generatorService.generateTests(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    GenerationResponse.builder()
                            .success(false)
                            .message("Error generating tests: " + e.getMessage())
                            .warnings(List.of("Exception: " + e.getClass().getSimpleName()))
                            .build()
            );
        }
    }

    /**
     * List available files in a GitHub repository.
     */
    @GetMapping("/api/github/files")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listGitHubFiles(
            @RequestParam("repositoryUrl") String repositoryUrl,
            @RequestParam(value = "githubToken", required = false) String githubToken) {

        Map<String, Object> result = new HashMap<>();
        try {
            List<String> files = gitHubReader.listRequirementFiles(repositoryUrl, githubToken);
            result.put("success", true);
            result.put("files", files);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Kade Test Gen");
        health.put("version", "1.0.0");
        health.put("frameworks", Map.of(
                "selenium", "4.16.1",
                "restAssured", "5.4.0"
        ));
        return ResponseEntity.ok(health);
    }

    /**
     * Check Copilot/AI integration status.
     */
    @GetMapping("/api/copilot/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> copilotStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", copilotService.isEnabled());
        status.put("provider", "Microsoft Azure OpenAI");
        status.put("features", List.of(
                "AI-powered requirements analysis",
                "Smart test scenario suggestions",
                "Enhanced code generation",
                "Intelligent element locators"
        ));
        return ResponseEntity.ok(status);
    }

    /**
     * Check Ollama (local LLM) status.
     */
    @GetMapping("/api/ai/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> aiStatus() {
        Map<String, Object> status = ollamaService.getStatus();
        status.put("features", List.of(
                "AI-enhanced BDD scenarios",
                "Smart edge case detection",
                "Improved test suggestions",
                "Local processing - data stays on-premises"
        ));
        return ResponseEntity.ok(status);
    }

    /**
     * Enhance BDD scenarios with AI (optional endpoint).
     */
    @PostMapping("/api/ai/enhance-bdd")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enhanceBddWithAI(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();

        try {
            String scenarios = payload.get("scenarios");
            String pageContext = payload.getOrDefault("context", "");

            if (!ollamaService.isAvailable()) {
                result.put("success", false);
                result.put("enhanced", false);
                result.put("scenarios", scenarios);
                result.put("message", "AI not available. Ollama is not running or not configured.");
                return ResponseEntity.ok(result);
            }

            String enhanced = ollamaService.enhanceBddScenarios(pageContext, scenarios);

            if (enhanced != null && !enhanced.isEmpty()) {
                result.put("success", true);
                result.put("enhanced", true);
                result.put("scenarios", enhanced);
                result.put("message", "Scenarios enhanced with AI");
            } else {
                result.put("success", true);
                result.put("enhanced", false);
                result.put("scenarios", scenarios);
                result.put("message", "AI returned empty response, using original scenarios");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Analyze a live URL and generate BDD scenarios.
     * Uses headless browser to analyze DOM structure dynamically.
     */
    @PostMapping("/api/analyze/url")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeUrl(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String url = payload.get("url");
            if (url == null || url.isEmpty()) {
                result.put("success", false);
                result.put("error", "URL is required");
                return ResponseEntity.badRequest().body(result);
            }

            // Validate URL format
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // Analyze the page
            PageAnalysis analysis = webPageAnalyzer.analyzePage(url);

            // Check if analysis encountered an error
            boolean isError = "error".equals(analysis.getPagePurpose());

            // Generate BDD scenarios
            BDDScenario bddScenario = bddGenerator.generateScenarios(analysis);
            String gherkinOutput = bddScenario.toGherkin();

            result.put("success", !isError);
            result.put("url", url);
            result.put("analysis", Map.of(
                    "title", analysis.getTitle() != null ? analysis.getTitle() : "",
                    "pagePurpose", analysis.getPagePurpose(),
                    "pageDescription", analysis.getPageDescription(),
                    "totalElements", analysis.getMetadata().getTotalElements(),
                    "totalForms", analysis.getMetadata().getTotalForms(),
                    "totalLinks", analysis.getMetadata().getTotalLinks(),
                    "analysisTimeMs", analysis.getMetadata().getAnalysisTimeMs()
            ));
            result.put("elements", analysis.getElements());
            result.put("forms", analysis.getForms());
            result.put("inferredFlows", analysis.getInferredFlows());
            result.put("edgeCases", analysis.getEdgeCases());
            result.put("securityConsiderations", analysis.getSecurityConsiderations());
            result.put("bddScenarios", gherkinOutput);
            result.put("scenarioCount", bddScenario.getScenarios() != null ? bddScenario.getScenarios().size() : 0);
            result.put("totalTimeMs", System.currentTimeMillis() - startTime);

            // Include warnings from analysis
            if (analysis.getMetadata().getWarnings() != null && !analysis.getMetadata().getWarnings().isEmpty()) {
                result.put("warnings", analysis.getMetadata().getWarnings());
            }

            // If there was an error, include the error message
            if (isError) {
                result.put("error", analysis.getPageDescription());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Error analyzing URL: " + e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Analyze pasted HTML content and generate BDD scenarios.
     * Fallback for sites that block automated requests.
     */
    @PostMapping("/api/analyze/html")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeHtml(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String html = payload.get("html");
            String sourceUrl = payload.getOrDefault("sourceUrl", "pasted-html");

            if (html == null || html.isEmpty()) {
                result.put("success", false);
                result.put("error", "HTML content is required");
                return ResponseEntity.badRequest().body(result);
            }

            // Analyze the HTML content directly
            PageAnalysis analysis = webPageAnalyzer.analyzeHtml(html, sourceUrl);

            // Check if analysis encountered an error
            boolean isError = "error".equals(analysis.getPagePurpose());

            // Generate BDD scenarios
            BDDScenario bddScenario = bddGenerator.generateScenarios(analysis);
            String gherkinOutput = bddScenario.toGherkin();

            result.put("success", !isError);
            result.put("url", sourceUrl);
            result.put("analysis", Map.of(
                    "title", analysis.getTitle() != null ? analysis.getTitle() : "",
                    "pagePurpose", analysis.getPagePurpose(),
                    "pageDescription", analysis.getPageDescription(),
                    "totalElements", analysis.getMetadata().getTotalElements(),
                    "totalForms", analysis.getMetadata().getTotalForms(),
                    "totalLinks", analysis.getMetadata().getTotalLinks(),
                    "analysisTimeMs", analysis.getMetadata().getAnalysisTimeMs()
            ));
            result.put("elements", analysis.getElements());
            result.put("forms", analysis.getForms());
            result.put("inferredFlows", analysis.getInferredFlows());
            result.put("edgeCases", analysis.getEdgeCases());
            result.put("securityConsiderations", analysis.getSecurityConsiderations());
            result.put("bddScenarios", gherkinOutput);
            result.put("scenarioCount", bddScenario.getScenarios() != null ? bddScenario.getScenarios().size() : 0);
            result.put("totalTimeMs", System.currentTimeMillis() - startTime);

            if (analysis.getMetadata().getWarnings() != null && !analysis.getMetadata().getWarnings().isEmpty()) {
                result.put("warnings", analysis.getMetadata().getWarnings());
            }

            if (isError) {
                result.put("error", analysis.getPageDescription());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Error analyzing HTML: " + e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get page analysis details (elements, forms, etc.) without BDD generation.
     */
    @GetMapping("/api/analyze/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalysisDetails(@RequestParam("url") String url) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            PageAnalysis analysis = webPageAnalyzer.analyzePage(url);

            result.put("success", true);
            result.put("analysis", analysis);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
