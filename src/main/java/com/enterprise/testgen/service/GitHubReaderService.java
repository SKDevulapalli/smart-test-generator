package com.enterprise.testgen.service;

import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for reading content from GitHub repositories (read-only access).
 */
@Service
public class GitHubReaderService {

    @Value("${github.token:}")
    private String defaultToken;

    private static final List<String> REQUIREMENT_FILE_PATTERNS = List.of(
            "requirements", "specs", "specification", "features",
            "user-stories", "userstories", "test-cases", "testcases",
            "readme", "docs", "documentation"
    );

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".txt", ".feature", ".yaml", ".yml", ".json"
    );

    /**
     * Read requirements content from a GitHub repository.
     *
     * @param repositoryUrl full GitHub repository URL
     * @param token         optional personal access token for private repos
     * @return extracted content from requirement files
     * @throws IOException if repository cannot be accessed
     */
    public String readRepositoryContent(String repositoryUrl, String token) throws IOException {
        String repoFullName = extractRepoName(repositoryUrl);

        GitHub github = createGitHubClient(token);
        GHRepository repository = github.getRepository(repoFullName);

        StringBuilder content = new StringBuilder();
        content.append("# Repository: ").append(repoFullName).append("\n\n");

        // Read repository description
        if (repository.getDescription() != null) {
            content.append("## Description\n");
            content.append(repository.getDescription()).append("\n\n");
        }

        // Find and read requirement files
        List<GHContent> requirementFiles = findRequirementFiles(repository);

        if (requirementFiles.isEmpty()) {
            content.append("## Notice\n");
            content.append("No standard requirement files found. Reading README.md if available.\n\n");

            try {
                GHContent readme = repository.getReadme();
                content.append("## README\n");
                content.append(readFileContent(readme)).append("\n\n");
            } catch (Exception e) {
                content.append("README not found.\n\n");
            }
        } else {
            for (GHContent file : requirementFiles) {
                content.append("## File: ").append(file.getPath()).append("\n");
                content.append(readFileContent(file)).append("\n\n");
            }
        }

        return content.toString();
    }

    /**
     * List available files in a repository that might contain requirements.
     *
     * @param repositoryUrl repository URL
     * @param token         optional token
     * @return list of file paths
     * @throws IOException if repository cannot be accessed
     */
    public List<String> listRequirementFiles(String repositoryUrl, String token) throws IOException {
        String repoFullName = extractRepoName(repositoryUrl);
        GitHub github = createGitHubClient(token);
        GHRepository repository = github.getRepository(repoFullName);

        List<String> filePaths = new ArrayList<>();
        for (GHContent file : findRequirementFiles(repository)) {
            filePaths.add(file.getPath());
        }
        return filePaths;
    }

    private GitHub createGitHubClient(String token) throws IOException {
        String effectiveToken = (token != null && !token.isEmpty()) ? token : defaultToken;

        if (effectiveToken != null && !effectiveToken.isEmpty()) {
            return new GitHubBuilder().withOAuthToken(effectiveToken).build();
        }
        return GitHub.connectAnonymously();
    }

    private String extractRepoName(String repositoryUrl) {
        // Handle various GitHub URL formats
        String url = repositoryUrl.trim()
                .replaceAll("\\.git$", "")
                .replaceAll("/$", "");

        if (url.contains("github.com/")) {
            String[] parts = url.split("github\\.com/");
            if (parts.length > 1) {
                return parts[1];
            }
        }

        // Assume it's already in owner/repo format
        return url;
    }

    private List<GHContent> findRequirementFiles(GHRepository repository) throws IOException {
        List<GHContent> requirementFiles = new ArrayList<>();

        try {
            // Check root directory
            for (GHContent content : repository.getDirectoryContent("")) {
                if (isRequirementFile(content)) {
                    requirementFiles.add(content);
                }
            }

            // Check common directories
            for (String dir : List.of("docs", "documentation", "specs", "requirements", "features")) {
                try {
                    for (GHContent content : repository.getDirectoryContent(dir)) {
                        if (isRequirementFile(content)) {
                            requirementFiles.add(content);
                        }
                    }
                } catch (Exception e) {
                    // Directory doesn't exist, continue
                }
            }
        } catch (Exception e) {
            // Handle gracefully
        }

        return requirementFiles;
    }

    private boolean isRequirementFile(GHContent content) {
        if (content.isDirectory()) {
            return false;
        }

        String name = content.getName().toLowerCase();

        // Check file extension
        boolean hasValidExtension = SUPPORTED_EXTENSIONS.stream()
                .anyMatch(name::endsWith);

        if (!hasValidExtension) {
            return false;
        }

        // Check if filename suggests requirements
        return REQUIREMENT_FILE_PATTERNS.stream()
                .anyMatch(pattern -> name.contains(pattern));
    }

    private String readFileContent(GHContent content) throws IOException {
        return content.getContent() != null ?
                new String(java.util.Base64.getDecoder().decode(
                        content.getContent().replaceAll("\\s", ""))) :
                content.read().toString();
    }
}
