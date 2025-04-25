package com.lnmcp.lena.service;

import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.model.PromptRequest;
import com.lnmcp.lena.service.impl.AIServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for AIService to verify Ollama API communication.
 */
@SpringBootTest
public class AIServiceTest {

    @Autowired
    private AIService aiService;

    @Test
    void testSimplePromptResponse() {
        // Create a simple prompt
        String prompt = "What is your name?";

        // Call the service
        String response = aiService.generateSimpleResponse(prompt, null);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] AI Response: " + response);

        // Verify that we got a valid response (not the error message)
        assertNotNull(response);
        assertNotEquals("No response from AI model", response);
        assertNotEquals("No response from AI model: Empty response body", response);
        assertNotEquals("No response from AI model: Unexpected response format", response);
    }

    @Test
    void testMcpContextResponse() {
        // Create a simple MCP context
        McpContext context = McpContext.builder()
                .userPrompt("What is your name?")
                .build();

        // Call the service
        McpContext responseContext = aiService.generateResponse(context);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] AI Response in context: " + responseContext.getAiResponse());

        // Verify that we got a valid response
        assertNotNull(responseContext.getAiResponse());
        assertNotEquals("No response from AI model", responseContext.getAiResponse());
        assertNotEquals("No response from AI model: Empty response body", responseContext.getAiResponse());
        assertNotEquals("No response from AI model: Unexpected response format", responseContext.getAiResponse());
    }

    @Test
    void testPromptRequestProcessing() {
        // Create a simple prompt request
        PromptRequest request = new PromptRequest();
        request.setPrompt("What is your name?");

        // Call the service
        String response = aiService.processPromptRequest(request);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] AI Response from request: " + response);

        // Verify that we got a valid response
        assertNotNull(response);
        assertNotEquals("No response from AI model", response);
        assertNotEquals("No response from AI model: Empty response body", response);
        assertNotEquals("No response from AI model: Unexpected response format", response);
    }

    @Test
    void testPromptRequestWithDocumentReferences() {
        // Create a prompt request with document references
        PromptRequest request = new PromptRequest();
        request.setPrompt("What information is in the document?");
        request.setDocumentReferences(java.util.Arrays.asList("래미안 원펜타스_입주자모집공고.pdf"));

        // Call the service
        String response = aiService.processPromptRequest(request);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] AI Response with document references: " + response);

        // Verify that we got a valid response
        assertNotNull(response);
        assertNotEquals("No response from AI model", response);
        assertNotEquals("No response from AI model: Empty response body", response);
        assertNotEquals("No response from AI model: Unexpected response format", response);

        // The response should contain information from the document
        System.out.println("[DEBUG_LOG] Response length: " + response.length());
    }
}
