package com.lnmcp.lena.service;

import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for interacting with AI models.
 */
public interface AIService {
    
    /**
     * Generate a response using the AI model with the given context
     *
     * @param mcpContext The MCP context containing prompt and context information
     * @return Updated MCP context with AI response
     */
    McpContext generateResponse(McpContext mcpContext);
    
    /**
     * Generate a response asynchronously using the AI model with the given context
     *
     * @param mcpContext The MCP context containing prompt and context information
     * @return CompletableFuture of updated MCP context with AI response
     */
    CompletableFuture<McpContext> generateResponseAsync(McpContext mcpContext);
    
    /**
     * Generate a response using the AI model with the given prompt and model parameters
     *
     * @param prompt The user's prompt
     * @param modelParameters Optional parameters for the AI model
     * @return The AI-generated response
     */
    String generateSimpleResponse(String prompt, Map<String, Object> modelParameters);
    
    /**
     * Process a prompt request and generate a response
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return The AI-generated response
     */
    String processPromptRequest(PromptRequest promptRequest);
}