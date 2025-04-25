package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DatabaseContext;
import com.lnmcp.lena.model.DocumentContext;
import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.model.PromptResponse;
import com.lnmcp.lena.service.AIService;
import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
import com.lnmcp.lena.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of McpService for managing MCP context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpServiceImpl implements McpService {

    private final DocumentService documentService;
    private final DatabaseService databaseService;
    private final AIService aiService;

    @Override
    public PromptResponse processPrompt(PromptRequest promptRequest) throws IOException {
        // Build context from prompt request
        McpContext mcpContext = buildContext(promptRequest);

        // Generate response using AI service
        return generateResponse(mcpContext);
    }

    @Override
    @Async
    public CompletableFuture<PromptResponse> processPromptAsync(PromptRequest promptRequest) {
        try {
            // Build context from prompt request asynchronously
            McpContext mcpContext = buildContext(promptRequest);

            // Generate response using AI service asynchronously
            CompletableFuture<McpContext> aiResponseFuture = aiService.generateResponseAsync(mcpContext);

            // When AI response is ready, build the final response
            return aiResponseFuture.thenApply(this::generateResponse);
        } catch (IOException e) {
            log.error("Error processing prompt asynchronously", e);
            PromptResponse errorResponse = new PromptResponse();
            errorResponse.setPrompt(promptRequest.getPrompt());
            errorResponse.setResponse("Error processing prompt: " + e.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());
            return CompletableFuture.completedFuture(errorResponse);
        }
    }

    @Override
    public McpContext buildContext(PromptRequest promptRequest) throws IOException {
        McpContext mcpContext = McpContext.builder()
                .userPrompt(promptRequest.getPrompt())
                .build();

        // If document references are explicitly specified, use them
        if (promptRequest.getDocumentReferences() != null && !promptRequest.getDocumentReferences().isEmpty()) {
            List<DocumentContext> documentContexts = documentService.extractContextFromMultipleDocumentsByFilename(
                    promptRequest.getDocumentReferences());
            documentContexts.forEach(mcpContext::addDocumentContext);
        } 
        // If database references are explicitly specified, use them
        if (promptRequest.getDatabaseReferences() != null && !promptRequest.getDatabaseReferences().isEmpty()) {
            List<DatabaseContext> databaseContexts = databaseService.extractContextFromMultipleTables(
                    promptRequest.getDatabaseReferences());
            databaseContexts.forEach(mcpContext::addDatabaseContext);
        }
        // If no references are specified, automatically determine relevant documents and tables
        else if ((promptRequest.getDocumentReferences() == null || promptRequest.getDocumentReferences().isEmpty()) &&
                 (promptRequest.getDatabaseReferences() == null || promptRequest.getDatabaseReferences().isEmpty())) {
            try {
                // Use CompletableFuture to search documents and databases in parallel
                CompletableFuture<List<String>> relevantDocumentsFuture = findRelevantDocumentsAsync(promptRequest.getPrompt());
                CompletableFuture<List<String>> relevantTablesFuture = findRelevantTablesAsync(promptRequest.getPrompt());

                // Wait for both futures to complete
                CompletableFuture.allOf(relevantDocumentsFuture, relevantTablesFuture).join();

                // Get the results
                List<String> relevantDocuments = relevantDocumentsFuture.get();
                List<String> relevantTables = relevantTablesFuture.get();

                // Extract context from relevant documents
                if (!relevantDocuments.isEmpty()) {
                    log.info("Found {} relevant documents for prompt: {}", relevantDocuments.size(), promptRequest.getPrompt());
                    List<DocumentContext> documentContexts = documentService.extractContextFromMultipleDocumentsByFilename(relevantDocuments);
                    documentContexts.forEach(mcpContext::addDocumentContext);
                }

                // Extract context from relevant tables
                if (!relevantTables.isEmpty()) {
                    log.info("Found {} relevant tables for prompt: {}", relevantTables.size(), promptRequest.getPrompt());
                    List<DatabaseContext> databaseContexts = databaseService.extractContextFromMultipleTables(relevantTables);
                    databaseContexts.forEach(mcpContext::addDatabaseContext);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error finding relevant documents or tables", e);
                // Continue with empty context if there's an error
            }
        }

        return mcpContext;
    }

    /**
     * Find relevant documents asynchronously
     */
    @Async
    public CompletableFuture<List<String>> findRelevantDocumentsAsync(String prompt) {
        try {
            List<String> relevantDocuments = documentService.findRelevantDocuments(prompt);
            return CompletableFuture.completedFuture(relevantDocuments);
        } catch (IOException e) {
            log.error("Error finding relevant documents", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    /**
     * Find relevant tables asynchronously
     */
    @Async
    public CompletableFuture<List<String>> findRelevantTablesAsync(String prompt) {
        try {
            List<String> relevantTables = databaseService.findRelevantTables(prompt);
            return CompletableFuture.completedFuture(relevantTables);
        } catch (Exception e) {
            log.error("Error finding relevant tables", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public PromptResponse generateResponse(McpContext mcpContext) {
        // Generate response using AI service
        McpContext updatedContext = aiService.generateResponse(mcpContext);

        // Create document sources
        List<PromptResponse.DocumentSource> documentSources = new ArrayList<>();
        if (updatedContext.getDocumentContexts() != null && !updatedContext.getDocumentContexts().isEmpty()) {
            for (DocumentContext doc : updatedContext.getDocumentContexts()) {
                PromptResponse.DocumentSource source = new PromptResponse.DocumentSource();
                source.setFilename(doc.getFilename());
                source.setType(doc.getDocumentType());
                source.setPageNumbers(List.of(doc.getPageNumber()));
                documentSources.add(source);
            }
        }

        // Create database sources
        List<PromptResponse.DatabaseSource> databaseSources = new ArrayList<>();
        if (updatedContext.getDatabaseContexts() != null && !updatedContext.getDatabaseContexts().isEmpty()) {
            for (DatabaseContext db : updatedContext.getDatabaseContexts()) {
                PromptResponse.DatabaseSource source = new PromptResponse.DatabaseSource();
                source.setTableName(db.getTableName());
                source.setQuery(db.getQuery());
                databaseSources.add(source);
            }
        }

        // Build response
        PromptResponse response = new PromptResponse();
        response.setPrompt(updatedContext.getUserPrompt());
        response.setResponse(updatedContext.getAiResponse());
        response.setTimestamp(LocalDateTime.now());
        response.setDocumentSources(documentSources);
        response.setDatabaseSources(databaseSources);

        return response;
    }

}
