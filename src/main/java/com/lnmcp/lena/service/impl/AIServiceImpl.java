package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DatabaseContext;
import com.lnmcp.lena.model.DocumentContext;
import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.service.AIService;
import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
import com.lnmcp.lena.service.ResponseCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of AIService using direct REST calls to Ollama API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.model}")
    private String ollamaModel;

    private final RestTemplate restTemplate;
    private final DocumentService documentService;
    private final DatabaseService databaseService;
    private final ResponseCacheService responseCacheService;

    @Override
    public McpContext generateResponse(McpContext mcpContext) {
        try {
            // Check if a response for a similar prompt is already in the cache
            McpContext cachedContext = responseCacheService.getCachedResponse(mcpContext.getUserPrompt());
            if (cachedContext != null) {
                log.info("Using cached response for prompt: {}", mcpContext.getUserPrompt());
                // Copy the AI response from the cached context to the current context
                mcpContext.setAiResponse(cachedContext.getAiResponse());
                return mcpContext;
            }

            // If not in cache, try to find relevant documents first
            log.info("Generating new response for prompt: {}", mcpContext.getUserPrompt());
            
            // If the context doesn't already have document contexts, try to find relevant ones
            if ((mcpContext.getDocumentContexts() == null || mcpContext.getDocumentContexts().isEmpty()) && 
                mcpContext.getUserPrompt() != null && !mcpContext.getUserPrompt().isEmpty()) {
                
                try {
                    // Find relevant documents
                    List<String> relevantDocuments = documentService.findRelevantDocuments(mcpContext.getUserPrompt());
                    
                    if (!relevantDocuments.isEmpty()) {
                        log.info("Found {} relevant documents for prompt: {}", relevantDocuments.size(), mcpContext.getUserPrompt());
                        
                        // Extract context from relevant documents
                        List<DocumentContext> documentContexts = documentService.extractContextFromMultipleDocumentsByFilename(relevantDocuments);
                        
                        // Add document contexts to MCP context
                        documentContexts.forEach(mcpContext::addDocumentContext);
                        
                        // Check if we can generate a response directly from documents
                        String documentResponse = tryGenerateResponseFromDocuments(mcpContext);
                        
                        if (documentResponse != null) {
                            // We found a high-confidence response from documents
                            log.info("Generated response directly from documents for prompt: {}", mcpContext.getUserPrompt());
                            mcpContext.setAiResponse(documentResponse);
                            
                            // Cache the response for future use
                            responseCacheService.cacheResponse(mcpContext.getUserPrompt(), mcpContext);
                            
                            return mcpContext;
                        }
                        
                        // If we couldn't generate a response directly from documents, continue with LLM
                        log.info("No high-confidence document match found, falling back to LLM for prompt: {}", mcpContext.getUserPrompt());
                    }
                } catch (IOException e) {
                    log.warn("Error finding relevant documents, falling back to LLM: {}", e.getMessage());
                    // Continue with LLM if there's an error finding documents
                }
            }

            // If we couldn't generate a response from documents, use LLM
            // Build system prompt from MCP context
            String systemPrompt = buildSystemPrompt(mcpContext);

            // Combine system prompt and user prompt
            String fullPrompt = systemPrompt + "\n\nUser: " + mcpContext.getUserPrompt();

            // Call Ollama API
            String response = callOllamaApi(fullPrompt, 0.7);

            // Update MCP context with AI response
            mcpContext.setAiResponse(response);

            // Cache the response for future use
            responseCacheService.cacheResponse(mcpContext.getUserPrompt(), mcpContext);

            return mcpContext;
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            mcpContext.setAiResponse("Error generating response: " + e.getMessage());
            return mcpContext;
        }
    }
    
    /**
     * Try to generate a response directly from document contexts without calling LLM
     * Returns null if no high-confidence match is found
     */
    private String tryGenerateResponseFromDocuments(McpContext mcpContext) {
        if (mcpContext.getDocumentContexts() == null || mcpContext.getDocumentContexts().isEmpty()) {
            return null;
        }
        
        String userPrompt = mcpContext.getUserPrompt().toLowerCase();
        List<String> promptKeywords = extractKeywords(userPrompt);
        
        // Track the best matching document and its score
        DocumentContext bestMatch = null;
        double bestScore = 0.0;
        String bestMatchingSection = null;
        
        // Check each document context for relevance
        for (DocumentContext doc : mcpContext.getDocumentContexts()) {
            if (doc.getContent() == null || doc.getContent().isEmpty()) {
                continue;
            }
            
            // Split content into paragraphs for more precise matching
            String[] paragraphs = doc.getContent().split("\n\\s*\n");
            
            for (String paragraph : paragraphs) {
                if (paragraph.trim().length() < 50) {
                    continue; // Skip short paragraphs
                }
                
                double score = calculateRelevanceScore(paragraph, userPrompt, promptKeywords);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = doc;
                    bestMatchingSection = paragraph;
                }
            }
        }
        
        // If we found a good match, generate a response
        if (bestMatch != null && bestScore >= 0.7) { // Threshold for high confidence
            return generateResponseFromDocument(mcpContext.getUserPrompt(), bestMatch, bestMatchingSection);
        }
        
        return null; // No high-confidence match found
    }
    
    /**
     * Generate a response from a document context
     */
    private String generateResponseFromDocument(String userPrompt, DocumentContext doc, String matchingSection) {
        StringBuilder response = new StringBuilder();
        
        response.append("Based on the information from \"").append(doc.getFilename()).append("\":\n\n");
        response.append(matchingSection.trim());
        
        // Add source information
        response.append("\n\n(Source: ").append(doc.getFilename());
        if (doc.getPageNumber() != null) {
            response.append(", Page/Slide: ").append(doc.getPageNumber());
        }
        response.append(")");
        
        return response.toString();
    }
    
    /**
     * Calculate relevance score between a paragraph and a user prompt
     */
    private double calculateRelevanceScore(String paragraph, String prompt, List<String> promptKeywords) {
        String lowerParagraph = paragraph.toLowerCase();
        double score = 0.0;
        
        // Check for exact phrase match (highest weight)
        if (lowerParagraph.contains(prompt)) {
            score += 0.6;
        }
        
        // Check for keyword matches
        int keywordMatches = 0;
        for (String keyword : promptKeywords) {
            if (lowerParagraph.contains(keyword)) {
                keywordMatches++;
            }
        }
        
        // Calculate keyword match percentage and add to score
        if (!promptKeywords.isEmpty()) {
            double keywordMatchPercentage = (double) keywordMatches / promptKeywords.size();
            score += keywordMatchPercentage * 0.3;
        }
        
        // Check for question words and their nearby answers
        if (prompt.contains("what") || prompt.contains("how") || prompt.contains("when") || 
            prompt.contains("where") || prompt.contains("why") || prompt.contains("who")) {
            
            // If the paragraph contains phrases like "is", "are", "means", etc. after a keyword
            for (String keyword : promptKeywords) {
                if (lowerParagraph.contains(keyword + " is") || 
                    lowerParagraph.contains(keyword + " are") ||
                    lowerParagraph.contains(keyword + " means") ||
                    lowerParagraph.contains(keyword + " refers to")) {
                    score += 0.2;
                    break;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Extract keywords from a prompt (simplified version)
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
        
        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 2 && !commonWords.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    @Override
    @Async
    public CompletableFuture<McpContext> generateResponseAsync(McpContext mcpContext) {
        try {
            // Check if a response for a similar prompt is already in the cache
            McpContext cachedContext = responseCacheService.getCachedResponse(mcpContext.getUserPrompt());
            if (cachedContext != null) {
                log.info("Using cached response for prompt (async): {}", mcpContext.getUserPrompt());
                // Copy the AI response from the cached context to the current context
                mcpContext.setAiResponse(cachedContext.getAiResponse());
                return CompletableFuture.completedFuture(mcpContext);
            }

            // If not in cache, try to find relevant documents first
            log.info("Generating new response for prompt (async): {}", mcpContext.getUserPrompt());
            
            // If the context doesn't already have document contexts, try to find relevant ones
            if ((mcpContext.getDocumentContexts() == null || mcpContext.getDocumentContexts().isEmpty()) && 
                mcpContext.getUserPrompt() != null && !mcpContext.getUserPrompt().isEmpty()) {
                
                try {
                    // Find relevant documents
                    List<String> relevantDocuments = documentService.findRelevantDocuments(mcpContext.getUserPrompt());
                    
                    if (!relevantDocuments.isEmpty()) {
                        log.info("Found {} relevant documents for prompt (async): {}", relevantDocuments.size(), mcpContext.getUserPrompt());
                        
                        // Extract context from relevant documents
                        List<DocumentContext> documentContexts = documentService.extractContextFromMultipleDocumentsByFilename(relevantDocuments);
                        
                        // Add document contexts to MCP context
                        documentContexts.forEach(mcpContext::addDocumentContext);
                        
                        // Check if we can generate a response directly from documents
                        String documentResponse = tryGenerateResponseFromDocuments(mcpContext);
                        
                        if (documentResponse != null) {
                            // We found a high-confidence response from documents
                            log.info("Generated response directly from documents for prompt (async): {}", mcpContext.getUserPrompt());
                            mcpContext.setAiResponse(documentResponse);
                            
                            // Cache the response for future use
                            responseCacheService.cacheResponse(mcpContext.getUserPrompt(), mcpContext);
                            
                            return CompletableFuture.completedFuture(mcpContext);
                        }
                        
                        // If we couldn't generate a response directly from documents, continue with LLM
                        log.info("No high-confidence document match found, falling back to LLM for prompt (async): {}", mcpContext.getUserPrompt());
                    }
                } catch (IOException e) {
                    log.warn("Error finding relevant documents, falling back to LLM (async): {}", e.getMessage());
                    // Continue with LLM if there's an error finding documents
                }
            }

            // If we couldn't generate a response from documents, use LLM
            // Build system prompt from MCP context
            String systemPrompt = buildSystemPrompt(mcpContext);

            // Combine system prompt and user prompt
            String fullPrompt = systemPrompt + "\n\nUser: " + mcpContext.getUserPrompt();

            // Call Ollama API
            String response = callOllamaApi(fullPrompt, 0.7);

            // Update MCP context with AI response
            mcpContext.setAiResponse(response);

            // Cache the response for future use
            responseCacheService.cacheResponse(mcpContext.getUserPrompt(), mcpContext);

            return CompletableFuture.completedFuture(mcpContext);
        } catch (Exception e) {
            log.error("Error generating AI response asynchronously", e);
            mcpContext.setAiResponse("Error generating response: " + e.getMessage());
            return CompletableFuture.completedFuture(mcpContext);
        }
    }

    @Override
    public String generateSimpleResponse(String prompt, Map<String, Object> modelParameters) {
        try {
            // Check if a response for a similar prompt is already in the cache
            McpContext cachedContext = responseCacheService.getCachedResponse(prompt);
            if (cachedContext != null) {
                log.info("Using cached response for simple prompt: {}", prompt);
                return cachedContext.getAiResponse();
            }

            // If not in cache, generate a new response
            log.info("Generating new response for simple prompt: {}", prompt);

            double temperature = 0.7;
            if (modelParameters != null && modelParameters.containsKey("temperature")) {
                temperature = (Double) modelParameters.get("temperature");
            }

            String response = callOllamaApi(prompt, temperature);

            // Cache the response for future use
            McpContext context = McpContext.builder()
                    .userPrompt(prompt)
                    .aiResponse(response)
                    .build();
            responseCacheService.cacheResponse(prompt, context);

            return response;
        } catch (Exception e) {
            log.error("Error generating simple AI response", e);
            return "Error generating response: " + e.getMessage();
        }
    }

    /**
     * Call Ollama API directly using RestTemplate
     */
    private String callOllamaApi(String prompt, double temperature) {
        try {
            String url = ollamaBaseUrl + "/api/generate";

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("temperature", temperature);
            requestBody.put("stream", false);

            // Add max_tokens parameter to limit response length
            requestBody.put("max_tokens", 2000);

            // Add num_predict parameter as an alternative way to limit response length
            requestBody.put("num_predict", 2000);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make POST request with timeout
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);

            // Extract response
            Map<String, Object> responseBody = responseEntity.getBody();
            log.debug("Ollama API response: {}", responseBody);

            if (responseBody != null) {
                if (responseBody.containsKey("response")) {
                    return (String) responseBody.get("response");
                } else if (responseBody.containsKey("text")) {
                    return (String) responseBody.get("text");
                } else {
                    log.error("Unexpected response format from Ollama API: {}", responseBody);
                    return "No response from AI model: Unexpected response format";
                }
            } else {
                return "No response from AI model: Empty response body";
            }
        } catch (Exception e) {
            log.error("Error calling Ollama API", e);
            return "Error calling AI service: " + e.getMessage();
        }
    }

    @Override
    public String processPromptRequest(PromptRequest promptRequest) {
        try {
            // Build context from prompt request
            McpContext mcpContext = buildContextFromPromptRequest(promptRequest);

            // Generate response using the context
            McpContext updatedContext = generateResponse(mcpContext);

            return updatedContext.getAiResponse();
        } catch (Exception e) {
            log.error("Error processing prompt request", e);
            return "Error processing request: " + e.getMessage();
        }
    }

    /**
     * Build a McpContext from a PromptRequest
     */
    private McpContext buildContextFromPromptRequest(PromptRequest promptRequest) throws IOException {
        McpContext mcpContext = McpContext.builder()
                .userPrompt(promptRequest.getPrompt())
                .build();

        // Extract context from documents if specified
        if (promptRequest.getDocumentReferences() != null && !promptRequest.getDocumentReferences().isEmpty()) {
            List<DocumentContext> documentContexts = documentService.extractContextFromMultipleDocumentsByFilename(
                    promptRequest.getDocumentReferences());
            documentContexts.forEach(mcpContext::addDocumentContext);
        }

        // Extract context from database tables if specified
        if (promptRequest.getDatabaseReferences() != null && !promptRequest.getDatabaseReferences().isEmpty()) {
            List<DatabaseContext> databaseContexts = databaseService.extractContextFromMultipleTables(
                    promptRequest.getDatabaseReferences());
            databaseContexts.forEach(mcpContext::addDatabaseContext);
        }

        return mcpContext;
    }

    /**
     * Build a system prompt from the MCP context
     */
    private String buildSystemPrompt(McpContext mcpContext) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an AI assistant that provides helpful and accurate information. ");
        systemPrompt.append("Use the following context to answer the user's question:\n\n");

        // Add document contexts
        if (mcpContext.getDocumentContexts() != null && !mcpContext.getDocumentContexts().isEmpty()) {
            systemPrompt.append("DOCUMENT CONTEXT:\n");
            for (DocumentContext doc : mcpContext.getDocumentContexts()) {
                systemPrompt.append("Document: ").append(doc.getFilename()).append("\n");
                systemPrompt.append("Type: ").append(doc.getDocumentType()).append("\n");
                systemPrompt.append("Content:\n").append(doc.getContent()).append("\n\n");
            }
        }

        // Add database contexts
        if (mcpContext.getDatabaseContexts() != null && !mcpContext.getDatabaseContexts().isEmpty()) {
            systemPrompt.append("DATABASE CONTEXT:\n");
            for (DatabaseContext db : mcpContext.getDatabaseContexts()) {
                systemPrompt.append("Table: ").append(db.getTableName()).append("\n");
                systemPrompt.append("Description: ").append(db.getDescription()).append("\n");
                systemPrompt.append("Data: ").append(db.getData()).append("\n\n");
            }
        }

        systemPrompt.append("IMPORTANT INSTRUCTIONS:\n");
        systemPrompt.append("1. Answer the user's question based ONLY on the provided context above.\n");
        systemPrompt.append("2. If the information needed to answer the question is not in the context, explicitly state: \"I don't have enough information in the referenced materials to answer this question.\"\n");
        systemPrompt.append("3. Do not make up or infer information that is not explicitly stated in the context.\n");
        systemPrompt.append("4. If you're unsure about any part of your answer, indicate your uncertainty.\n");
        systemPrompt.append("5. Always cite the specific document or database source for your information.\n");

        return systemPrompt.toString();
    }
}
