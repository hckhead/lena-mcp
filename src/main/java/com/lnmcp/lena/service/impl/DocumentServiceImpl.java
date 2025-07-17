package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DocumentContext;
import com.lnmcp.lena.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of DocumentService for processing PDF and PPT documents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    @Value("${mcp.documents.path}")
    private String documentsPath;

    // Cache for document contexts to avoid repeated processing of the same documents
    private final Map<String, DocumentContext> documentCache = new ConcurrentHashMap<>();

    @Override
    public DocumentContext extractContext(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();

        // Check if the document is already in the cache
        if (documentCache.containsKey(filename)) {
            log.debug("Using cached context for document: {}", filename);
            return documentCache.get(filename);
        }

        // If not in cache, extract the context
        DocumentContext context;
        String lowercaseFilename = filename.toLowerCase();

        if (lowercaseFilename.endsWith(".pdf")) {
            context = extractPdfContext(filePath);
        } else if (lowercaseFilename.endsWith(".ppt") || lowercaseFilename.endsWith(".pptx")) {
            context = extractPptContext(filePath);
        } else {
            throw new IOException("Unsupported file format: " + filename);
        }

        // Store in cache for future use
        documentCache.put(filename, context);
        return context;
    }

    @Override
    public List<DocumentContext> extractContextFromMultipleDocuments(List<Path> filePaths) throws IOException {
        // Process documents in parallel using streams
        List<DocumentContext> contexts = filePaths.parallelStream()
                .map(filePath -> {
                    try {
                        return extractContext(filePath);
                    } catch (IOException e) {
                        log.error("Error extracting context from file: {}", filePath, e);
                        // We can't throw checked exceptions in streams, so we'll return null and filter it out
                        return null;
                    }
                })
                .filter(context -> context != null)
                .collect(Collectors.toList());

        // If no valid contexts were extracted, throw an exception
        if (contexts.isEmpty() && !filePaths.isEmpty()) {
            throw new IOException("Failed to extract context from any of the provided files");
        }

        return contexts;
    }

    @Override
    public DocumentContext extractContextByFilename(String filename) throws IOException {
        Path filePath = Paths.get(documentsPath, filename);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }
        return extractContext(filePath);
    }

    @Override
    public List<DocumentContext> extractContextFromMultipleDocumentsByFilename(List<String> filenames) throws IOException {
        List<Path> filePaths = filenames.stream()
                .map(filename -> Paths.get(documentsPath, filename))
                .collect(Collectors.toList());

        return extractContextFromMultipleDocuments(filePaths);
    }

    /**
     * Extract context from a PDF file
     */
    private DocumentContext extractPdfContext(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return DocumentContext.builder()
                    .filename(filePath.getFileName().toString())
                    .documentType(DocumentContext.DocumentType.PDF)
                    .content(text)
                    .pageNumber(document.getNumberOfPages()) // Total pages
                    .build();
        }
    }

    /**
     * Extract context from a PPT file
     */
    private DocumentContext extractPptContext(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString().toLowerCase();
        StringBuilder content = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            for (XSLFSlide slide : ppt.getSlides()) {
                // Try to get slide title if available
                String title = null;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        if (textShape.getText() != null && !textShape.getText().isEmpty()) {
                            if (title == null) {
                                title = textShape.getText(); // Use first text shape as title
                                content.append("Slide Title: ").append(title).append("\n");
                            } else {
                                content.append("Content: ").append(textShape.getText()).append("\n");
                            }
                        }
                    }
                }
                content.append("\n");
            }

            return DocumentContext.builder()
                    .filename(filename)
                    .documentType(DocumentContext.DocumentType.PPT)
                    .content(content.toString())
                    .pageNumber(ppt.getSlides().size()) // Total slides
                    .build();
        }
    }

    @Override
    public List<String> getAllDocuments() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(documentsPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(filename -> filename.toLowerCase().endsWith(".pdf") || 
                                       filename.toLowerCase().endsWith(".ppt") || 
                                       filename.toLowerCase().endsWith(".pptx"))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<String> findRelevantDocuments(String prompt) throws IOException {
        // Get all available documents
        List<String> allDocuments = getAllDocuments();

        if (allDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // Extract keywords from the prompt
        List<String> keywords = extractKeywords(prompt);
        
        if (keywords.isEmpty()) {
            log.warn("No meaningful keywords extracted from prompt: {}", prompt);
            return new ArrayList<>();
        }

        // Create a map to store document relevance scores
        Map<String, Double> documentScores = new ConcurrentHashMap<>();

        // Process documents in parallel for better performance
        allDocuments.parallelStream().forEach(filename -> {
            try {
                // First, check if the filename contains any keywords (quick check)
                String lowercaseFilename = filename.toLowerCase();
                boolean filenameMatch = keywords.stream()
                        .anyMatch(keyword -> lowercaseFilename.contains(keyword.toLowerCase()));
                
                // Initialize score based on filename match
                double score = filenameMatch ? 0.3 : 0.0;
                
                // Extract document content if not already in cache
                DocumentContext docContext;
                try {
                    docContext = extractContextByFilename(filename);
                } catch (IOException e) {
                    log.error("Error extracting context from file: {}", filename, e);
                    return; // Skip this document
                }
                
                // If we have content, search through it
                if (docContext != null && docContext.getContent() != null) {
                    String content = docContext.getContent().toLowerCase();
                    
                    // Calculate content match score
                    double contentScore = calculateContentScore(content, keywords, prompt.toLowerCase());
                    
                    // Combine scores
                    score += contentScore;
                    
                    // Store the score if it's above threshold
                    if (score > 0.1) {
                        documentScores.put(filename, score);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing document for relevance: {}", filename, e);
            }
        });
        
        // Sort documents by relevance score (descending) and return top results
        return documentScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5) // Limit to top 5 most relevant documents
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate content relevance score based on keyword matches and fuzzy matching
     */
    private double calculateContentScore(String content, List<String> keywords, String originalPrompt) {
        double score = 0.0;
        
        // Check for exact keyword matches
        for (String keyword : keywords) {
            // Count occurrences of the keyword in content
            int occurrences = countOccurrences(content, keyword.toLowerCase());
            if (occurrences > 0) {
                // Add score based on number of occurrences (with diminishing returns)
                score += Math.min(0.2, 0.05 * occurrences);
            }
            
            // Check for fuzzy matches (keywords with one character different)
            List<String> fuzzyMatches = findFuzzyMatches(content, keyword.toLowerCase());
            if (!fuzzyMatches.isEmpty()) {
                score += Math.min(0.1, 0.02 * fuzzyMatches.size());
            }
        }
        
        // Check for exact phrase matches (higher weight)
        if (content.contains(originalPrompt)) {
            score += 0.5;
        }
        
        // Check for sentence fragments (3+ word sequences)
        String[] promptWords = originalPrompt.split("\\s+");
        if (promptWords.length >= 3) {
            for (int i = 0; i <= promptWords.length - 3; i++) {
                String fragment = promptWords[i] + " " + promptWords[i+1] + " " + promptWords[i+2];
                if (content.contains(fragment)) {
                    score += 0.3;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Count occurrences of a substring in a string
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Find fuzzy matches for a keyword in text
     * This implements a simple edit distance of 1 character
     */
    private List<String> findFuzzyMatches(String text, String keyword) {
        List<String> matches = new ArrayList<>();
        
        // Only apply fuzzy matching for keywords of reasonable length
        if (keyword.length() < 4) {
            return matches;
        }
        
        // Split text into words
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            // Skip words with big length difference
            if (Math.abs(word.length() - keyword.length()) > 1) {
                continue;
            }
            
            // Check edit distance
            if (word.equals(keyword)) {
                continue; // Skip exact matches
            }
            
            if (calculateEditDistance(word, keyword) <= 1) {
                matches.add(word);
            }
        }
        
        return matches;
    }
    
    /**
     * Calculate Levenshtein edit distance between two strings
     */
    private int calculateEditDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        
        return costs[s2.length()];
    }

    /**
     * Extract keywords from a prompt
     * This is a simple implementation that just splits the prompt by spaces and removes common words
     */
    private List<String> extractKeywords(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Convert to lowercase and split by non-alphanumeric characters
        String[] words = prompt.toLowerCase().split("[^a-zA-Z0-9가-힣]+");

        // Filter out common words and short words
        List<String> commonWords = Arrays.asList("the", "a", "an", "and", "or", "but", "is", "are", "was", "were", 
                                               "in", "on", "at", "to", "for", "with", "by", "about", "like", 
                                               "through", "over", "before", "after", "between", "under", "during",
                                               "of", "from", "up", "down", "into", "out", "as", "if", "when", 
                                               "why", "how", "all", "any", "both", "each", "few", "more", "most",
                                               "other", "some", "such", "no", "nor", "not", "only", "own", "same",
                                               "so", "than", "too", "very", "can", "will", "just", "should", "now");

        return Arrays.stream(words)
                .filter(word -> word.length() > 2) // Filter out short words
                .filter(word -> !commonWords.contains(word)) // Filter out common words
                .collect(Collectors.toList());
    }
}
