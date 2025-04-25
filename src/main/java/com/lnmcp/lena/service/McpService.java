package com.lnmcp.lena.service;

import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.model.PromptResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing MCP (Message Context Protocol) context.
 */
public interface McpService {

    /**
     * Process a prompt request and generate a response
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return PromptResponse containing the AI-generated response and metadata
     * @throws IOException If there's an error processing documents
     */
    PromptResponse processPrompt(PromptRequest promptRequest) throws IOException;

    /**
     * Process a prompt request asynchronously and generate a response
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return CompletableFuture of PromptResponse containing the AI-generated response and metadata
     */
    CompletableFuture<PromptResponse> processPromptAsync(PromptRequest promptRequest);

    /**
     * Build an MCP context from a prompt request
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return McpContext containing the prompt and extracted context
     * @throws IOException If there's an error processing documents
     */
    McpContext buildContext(PromptRequest promptRequest) throws IOException;

    /**
     * Generate a response using the AI model with the given context
     *
     * @param mcpContext The MCP context containing prompt and context information
     * @return PromptResponse containing the AI-generated response and metadata
     */
    PromptResponse generateResponse(McpContext mcpContext);

    /**
     * Find relevant documents asynchronously
     *
     * @param prompt The user's prompt
     * @return CompletableFuture of a list of document filenames that might be relevant
     */
    CompletableFuture<List<String>> findRelevantDocumentsAsync(String prompt);

    /**
     * Find relevant tables asynchronously
     *
     * @param prompt The user's prompt
     * @return CompletableFuture of a list of table names that might be relevant
     */
    CompletableFuture<List<String>> findRelevantTablesAsync(String prompt);
}
