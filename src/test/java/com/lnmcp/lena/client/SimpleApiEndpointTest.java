package com.lnmcp.lena.client;

import com.lnmcp.lena.model.McpContext;
import com.lnmcp.lena.service.AIService;
import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple test class to verify the API endpoint works correctly.
 * This test mocks the necessary services to avoid dependency issues.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SimpleApiEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AIService aiService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testApiEndpoint() throws Exception {
        // Mock AIService to return a simple response
        when(aiService.processPromptRequest(any())).thenReturn("This is a test response from the mocked AI service");

        // Mock AIService.generateResponse to return a non-null McpContext
        McpContext mockContext = new McpContext();
        mockContext.setUserPrompt("what is your name?");
        mockContext.setAiResponse("This is a test response from the mocked AI service");
        when(aiService.generateResponse(any(McpContext.class))).thenReturn(mockContext);

        // Create the JSON request body
        String requestBody = "{\"prompt\": \"what is your name?\"}";

        // Perform the POST request to the API endpoint
        MvcResult result = mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("what is your name?"))
                .andReturn();

        // Extract and print the response for debugging
        String responseContent = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] API Response: " + responseContent);

        // Verify that the response contains the mocked AI response
        assertTrue(responseContent.contains("This is a test response from the mocked AI service"));
    }
}
