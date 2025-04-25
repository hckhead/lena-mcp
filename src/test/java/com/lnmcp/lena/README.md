# MCP API Testing Guide

This guide explains how to test the MCP (Message Context Protocol) API functionality.

## Overview

The MCP server provides an API endpoint at `http://localhost:8080/api/mcp/prompt` that accepts prompt requests and returns AI-generated responses. The API supports:

1. Basic prompt requests
2. Prompt requests with document references
3. Prompt requests with database references
4. Prompt requests with custom model parameters

## Automated Tests

The `McpControllerTest` class contains automated tests that verify the API functionality. These tests use Spring's MockMvc to simulate HTTP requests and mock the service layer to avoid actual calls to the AI model.

### Running the Tests

To run the tests:

```bash
./mvnw test -Dtest=McpControllerTest
```

The tests verify that:
- The API endpoint accepts requests correctly
- The prompt text is correctly passed to the service
- The response structure is as expected
- Document references are correctly processed
- Database references are correctly processed
- Model parameters are correctly processed

## Manual Testing with ApiClientExample

The `ApiClientExample` class provides a simple client that can be used to test the API from another project. This class demonstrates how to send different types of requests to the API endpoint.

### Prerequisites

1. Make sure the MCP server is running
2. The API endpoint is available at `http://localhost:8080/api/mcp/prompt`

### Using the ApiClientExample

The `ApiClientExample` class contains examples for:

1. **Basic prompt request**:
   ```java
   Map<String, Object> basicRequest = new HashMap<>();
   basicRequest.put("prompt", "What is Spring Boot?");
   ```

2. **Prompt with document references**:
   ```java
   Map<String, Object> docRequest = new HashMap<>();
   docRequest.put("prompt", "Explain the content of the manual");
   docRequest.put("documentReferences", Arrays.asList("manual.pdf", "guide.ppt"));
   ```

3. **Prompt with database references**:
   ```java
   Map<String, Object> dbRequest = new HashMap<>();
   dbRequest.put("prompt", "Show me product information");
   dbRequest.put("databaseReferences", Arrays.asList("products", "categories"));
   ```

4. **Prompt with model parameters**:
   ```java
   Map<String, Object> paramRequest = new HashMap<>();
   paramRequest.put("prompt", "Generate creative content");
   
   Map<String, Object> modelParams = new HashMap<>();
   modelParams.put("temperature", 0.9);
   modelParams.put("maxTokens", 2000);
   paramRequest.put("modelParameters", modelParams);
   ```

### Running the ApiClientExample

To run the example:

1. Make sure the MCP server is running
2. Run the `ApiClientExample` class
3. Check the console output for the API responses

## Integrating with Your Application

To integrate the MCP API with your application:

1. Use an HTTP client (like RestTemplate or HttpClient) to send POST requests to the API endpoint
2. Set the Content-Type header to `application/json`
3. Format your request body according to the examples in `ApiClientExample`
4. Parse the JSON response to extract the AI-generated content

Example request body:
```json
{
  "prompt": "Your prompt text here",
  "documentReferences": ["document1.pdf", "document2.ppt"],
  "databaseReferences": ["table1", "table2"],
  "modelParameters": {
    "temperature": 0.7,
    "maxTokens": 1000
  }
}
```

Example response:
```json
{
  "prompt": "Your prompt text here",
  "response": "AI-generated response",
  "timestamp": "2023-04-25T10:15:30",
  "documentSources": [...],
  "databaseSources": [...]
}
```

## Troubleshooting

If you encounter issues:

1. Make sure the MCP server is running
2. Check that the API endpoint URL is correct
3. Verify that your request format matches the expected format
4. Check the server logs for any error messages