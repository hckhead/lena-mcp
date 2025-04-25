package com.lnmcp.lena.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.model.PromptResponse;
import com.lnmcp.lena.service.McpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for McpController to verify API functionality.
 * These tests verify that user input is correctly sent as prompt text to the API.
 */
@WebMvcTest(McpController.class)
public class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McpService mcpService;

    @Autowired
    private ObjectMapper objectMapper;

    private PromptResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create a mock response that the service will return
        mockResponse = new PromptResponse();
        mockResponse.setPrompt("Test prompt");
        mockResponse.setResponse("This is a test response from the AI model");
        mockResponse.setTimestamp(LocalDateTime.now());
        mockResponse.setDocumentSources(new ArrayList<>());
        mockResponse.setDatabaseSources(new ArrayList<>());

        // Configure the mock service to return our mock response
        // We need to handle the IOException that processPrompt can throw
        try {
            // This approach handles the checked exception
            when(mcpService.processPrompt(any(PromptRequest.class))).thenAnswer(invocation -> mockResponse);
        } catch (IOException e) {
            // This should not happen with thenAnswer, but we include it for completeness
            fail("Exception during test setup: " + e.getMessage());
        }
    }

    /**
     * Test that verifies a basic prompt request is correctly processed.
     * This test checks that:
     * 1. The API endpoint accepts the request
     * 2. The prompt text is correctly passed to the service
     * 3. The response structure is as expected
     */
    @Test
    void testBasicPromptRequest() throws Exception {
        // Create a test prompt request
        PromptRequest request = new PromptRequest();
        request.setPrompt("Test prompt");

        // Perform the POST request to the API endpoint
        MvcResult result = mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("Test prompt"))
                .andExpect(jsonPath("$.response").value("This is a test response from the AI model"))
                .andReturn();

        // Verify that the service was called with the correct prompt
        verify(mcpService, times(1)).processPrompt(any(PromptRequest.class));

        // Extract and print the response for debugging
        String responseContent = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] API Response: " + responseContent);
    }

    /**
     * Test that verifies a prompt request with document references is correctly processed.
     */
    @Test
    void testPromptRequestWithDocumentReferences() throws Exception {
        // Create a test prompt request with document references
        PromptRequest request = new PromptRequest();
        request.setPrompt("Tell me about the manual");
        request.setDocumentReferences(List.of("manual.pdf", "guide.ppt"));

        // Perform the POST request to the API endpoint
        mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("Test prompt")) // From mock response
                .andReturn();

        // Verify that the service was called with the correct prompt and document references
        verify(mcpService, times(1)).processPrompt(argThat(req -> 
            req.getPrompt().equals("Tell me about the manual") && 
            req.getDocumentReferences().size() == 2 &&
            req.getDocumentReferences().contains("manual.pdf") &&
            req.getDocumentReferences().contains("guide.ppt")
        ));
    }

    /**
     * Test that verifies a prompt request with database references is correctly processed.
     */
    @Test
    void testPromptRequestWithDatabaseReferences() throws Exception {
        // Create a test prompt request with database references
        PromptRequest request = new PromptRequest();
        request.setPrompt("Show me product information");
        request.setDatabaseReferences(List.of("products", "categories"));

        // Perform the POST request to the API endpoint
        mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify that the service was called with the correct prompt and database references
        verify(mcpService, times(1)).processPrompt(argThat(req -> 
            req.getPrompt().equals("Show me product information") && 
            req.getDatabaseReferences().size() == 2 &&
            req.getDatabaseReferences().contains("products") &&
            req.getDatabaseReferences().contains("categories")
        ));
    }

    /**
     * Test that verifies a prompt request with model parameters is correctly processed.
     */
    @Test
    void testPromptRequestWithModelParameters() throws Exception {
        // Create a test prompt request with custom model parameters
        PromptRequest request = new PromptRequest();
        request.setPrompt("Generate creative content");

        PromptRequest.ModelParameters params = new PromptRequest.ModelParameters();
        params.setTemperature(0.9);
        params.setMaxTokens(2000);
        request.setModelParameters(params);

        // Perform the POST request to the API endpoint
        mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify that the service was called with the correct prompt and model parameters
        verify(mcpService, times(1)).processPrompt(argThat(req -> 
            req.getPrompt().equals("Generate creative content") && 
            req.getModelParameters().getTemperature() == 0.9 &&
            req.getModelParameters().getMaxTokens() == 2000
        ));
    }
}
