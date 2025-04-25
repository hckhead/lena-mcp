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
            PromptResponse response = processPrompt(promptRequest);
            return CompletableFuture.completedFuture(response);
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
