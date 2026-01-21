package com.enterprise.testgen.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for parsing Word documents (.docx) to extract requirements.
 */
@Service
public class DocumentParserService {

    /**
     * Parse a document and extract text content.
     * Supports .docx, .doc, and .txt files.
     *
     * @param file the uploaded document
     * @return extracted text content
     * @throws IOException if document cannot be parsed
     */
    public String parseWordDocument(MultipartFile file) throws IOException {
        validateFile(file);

        String filename = file.getOriginalFilename().toLowerCase();

        if (filename.endsWith(".txt")) {
            return parseTextFile(file);
        } else if (filename.endsWith(".doc") && !filename.endsWith(".docx")) {
            return parseDocFile(file);
        } else {
            return parseDocxFile(file);
        }
    }

    /**
     * Parse a .docx file (Office Open XML format).
     */
    private String parseDocxFile(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            StringBuilder content = new StringBuilder();

            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            // Extract tables
            for (XWPFTable table : document.getTables()) {
                content.append(parseTable(table));
            }

            return content.toString();
        }
    }

    /**
     * Parse a .doc file (legacy Word format).
     */
    private String parseDocFile(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(document)) {

            StringBuilder content = new StringBuilder();
            String[] paragraphs = extractor.getParagraphText();

            for (String paragraph : paragraphs) {
                String text = paragraph.trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            return content.toString();
        }
    }

    /**
     * Parse a .txt file (plain text).
     */
    private String parseTextFile(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Extract structured requirements from parsed content.
     *
     * @param content the raw text content
     * @return list of requirement sections
     */
    public List<RequirementSection> extractRequirements(String content) {
        List<RequirementSection> sections = new ArrayList<>();
        String[] lines = content.split("\n");

        RequirementSection currentSection = null;
        StringBuilder sectionContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Check if this is a section header
            if (isSectionHeader(line)) {
                // Save previous section
                if (currentSection != null) {
                    currentSection.setContent(sectionContent.toString().trim());
                    sections.add(currentSection);
                }
                // Start new section
                currentSection = new RequirementSection();
                currentSection.setTitle(line);
                sectionContent = new StringBuilder();
            } else if (currentSection != null) {
                sectionContent.append(line).append("\n");
            }
        }

        // Don't forget the last section
        if (currentSection != null) {
            currentSection.setContent(sectionContent.toString().trim());
            sections.add(currentSection);
        }

        return sections;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty or null");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("Filename is required");
        }

        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".docx") && !lowerFilename.endsWith(".doc") && !lowerFilename.endsWith(".txt")) {
            throw new IOException("Only .docx, .doc, and .txt files are supported");
        }
    }

    private String parseTable(XWPFTable table) {
        StringBuilder tableContent = new StringBuilder();
        tableContent.append("\n[TABLE]\n");

        for (XWPFTableRow row : table.getRows()) {
            List<String> cellTexts = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cellTexts.add(cell.getText().trim());
            }
            tableContent.append(String.join(" | ", cellTexts)).append("\n");
        }

        tableContent.append("[/TABLE]\n");
        return tableContent.toString();
    }

    private boolean isSectionHeader(String line) {
        // Common patterns for section headers
        return line.matches("^\\d+\\..*") ||                    // 1. Section
               line.matches("^[A-Z][A-Z\\s]+$") ||              // ALL CAPS
               line.matches("^#+\\s.*") ||                       // Markdown headers
               line.matches("(?i)^(Feature|Scenario|Given|When|Then|Test Case|Requirement|User Story):?.*");
    }

    /**
     * Inner class representing a requirement section.
     */
    public static class RequirementSection {
        private String title;
        private String content;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
