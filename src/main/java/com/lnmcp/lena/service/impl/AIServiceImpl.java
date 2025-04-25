package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DatabaseContext;
import com.lnmcp.lena.model.DocumentContext;
import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.service.AIService;
import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public McpContext generateResponse(McpContext mcpContext) {
        try {
            // Build system prompt from MCP context
            String systemPrompt = buildSystemPrompt(mcpContext);

            // Combine system prompt and user prompt
            String fullPrompt = systemPrompt + "\n\nUser: " + mcpContext.getUserPrompt();

            // Call Ollama API
            String response = callOllamaApi(fullPrompt, 0.7);

            // Update MCP context with AI response
            mcpContext.setAiResponse(response);

            return mcpContext;
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            mcpContext.setAiResponse("Error generating response: " + e.getMessage());
            return mcpContext;
        }
    }

    @Override
    @Async
    public CompletableFuture<McpContext> generateResponseAsync(McpContext mcpContext) {
        return CompletableFuture.completedFuture(generateResponse(mcpContext));
    }

    @Override
    public String generateSimpleResponse(String prompt, Map<String, Object> modelParameters) {
        try {
            double temperature = 0.7;
            if (modelParameters != null && modelParameters.containsKey("temperature")) {
                temperature = (Double) modelParameters.get("temperature");
            }

            return callOllamaApi(prompt, temperature);
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

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make POST request
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

        systemPrompt.append("Answer the user's question based on the provided context. If the information is not in the context, say so.");

        return systemPrompt.toString();
    }
}
