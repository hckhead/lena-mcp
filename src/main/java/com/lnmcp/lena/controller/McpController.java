package com.lnmcp.lena.controller;

import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.model.PromptResponse;
import com.lnmcp.lena.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for MCP (Message Context Protocol) API.
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final McpService mcpService;
    
    /**
     * Process a prompt request synchronously
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return PromptResponse containing the AI-generated response and metadata
     */
    @PostMapping("/prompt")
    public ResponseEntity<PromptResponse> processPrompt(@RequestBody PromptRequest promptRequest) {
        try {
            log.info("Received prompt request: {}", promptRequest.getPrompt());
            PromptResponse response = mcpService.processPrompt(promptRequest);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error processing prompt", e);
            PromptResponse errorResponse = new PromptResponse();
            errorResponse.setPrompt(promptRequest.getPrompt());
            errorResponse.setResponse("Error processing prompt: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Process a prompt request asynchronously
     *
     * @param promptRequest The prompt request containing user prompt and optional parameters
     * @return DeferredResult containing the PromptResponse
     */
    @PostMapping("/prompt/async")
    public DeferredResult<ResponseEntity<PromptResponse>> processPromptAsync(@RequestBody PromptRequest promptRequest) {
        log.info("Received async prompt request: {}", promptRequest.getPrompt());
        
        DeferredResult<ResponseEntity<PromptResponse>> deferredResult = new DeferredResult<>(TimeUnit.MINUTES.toMillis(5));
        
        CompletableFuture<PromptResponse> future = mcpService.processPromptAsync(promptRequest);
        
        future.thenAccept(response -> {
            log.info("Async response ready for prompt: {}", promptRequest.getPrompt());
            deferredResult.setResult(ResponseEntity.ok(response));
        }).exceptionally(e -> {
            log.error("Error processing async prompt", e);
            PromptResponse errorResponse = new PromptResponse();
            errorResponse.setPrompt(promptRequest.getPrompt());
            errorResponse.setResponse("Error processing prompt: " + e.getMessage());
            deferredResult.setResult(ResponseEntity.internalServerError().body(errorResponse));
            return null;
        });
        
        return deferredResult;
    }
}