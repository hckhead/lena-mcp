package com.lnmcp.lena.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class to verify the API endpoint works correctly with Ollama.
 * This simulates the curl command from the issue description.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ApiEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApiEndpoint() throws Exception {
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
        
        // Verify that the response doesn't contain the error message
        assertFalse(responseContent.contains("No response from AI model"));
        
        // Verify that the response contains a non-empty response field
        mockMvc.perform(post("/api/mcp/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isNotEmpty())
                .andExpect(jsonPath("$.response").isString());
    }
}