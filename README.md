# MCP Server

This is a Spring Boot application that integrates with Spring AI to process user prompts, extract context from documents and database, and generate AI responses.

## Maven Setup

This project uses Maven for dependency management and build automation. A Maven wrapper (`mvnw` and `mvnw.cmd`) is included in the project, but the wrapper JAR file might need to be downloaded:

```bash
# Download the Maven wrapper JAR file
mkdir -p .mvn/wrapper
wget https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar -O .mvn/wrapper/maven-wrapper.jar
```

After downloading the wrapper JAR, you can use the Maven wrapper to build and run the project:

```bash
# On Unix-like systems
./mvnw clean install

# On Windows
mvnw.cmd clean install
```

## API Testing Guide

This guide explains how to test the MCP (Message Context Protocol) API functionality.

## Overview

The MCP server provides an API endpoint at `http://localhost:8080/api/mcp/prompt` that accepts prompt requests and returns AI-generated responses. The API supports:

1. Basic prompt requests
2. Prompt requests with document references
3. Prompt requests with database references
4. Prompt requests with custom model parameters
5. Automatic context determination from documents and database tables

### Automatic Context Determination

The MCP server can automatically determine which documents and database tables are relevant to a user's prompt. When no document or database references are explicitly specified in the request, the system will:

1. Analyze the prompt to extract keywords
2. Search for documents and database tables that match these keywords
3. Process these relevant sources in parallel using asynchronous operations
4. Include the extracted context in the AI response

This feature allows users to submit general prompts without needing to specify which documents or tables to reference. The system will automatically find and use the most relevant sources of information.

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
  "documentSources": [],
  "databaseSources": []
}
```

## Troubleshooting

If you encounter issues:

1. Make sure the MCP server is running
2. Check that the API endpoint URL is correct
3. Verify that your request format matches the expected format
4. Check the server logs for any error messages

## Performance Optimizations

The MCP server includes several performance optimizations:

1. **Asynchronous Processing**: The system uses asynchronous processing to handle requests in parallel, improving response times.
2. **Parallel Document Processing**: Documents are processed in parallel using Java's parallel streams, utilizing multiple CPU cores.
3. **Parallel Database Queries**: Database tables are queried in parallel, improving performance when working with multiple tables.
4. **Document Caching**: Document contexts are cached to avoid repeatedly processing the same documents.
5. **Database Caching**: Database contexts are cached to avoid repeatedly querying the same tables.
6. **Limited Response Size**: API calls to the AI model include parameters to limit the response size, reducing processing time.
7. **Limited Database Queries**: Database queries are limited to 100 rows to reduce the amount of data transferred and processed.
8. **Startup Caching**: Documents and database tables are pre-parsed and cached at application startup, eliminating the initial delay when they are first accessed.
9. **In-memory Response Caching**: AI responses are cached in memory to avoid repeatedly generating responses for similar questions. The system normalizes prompts (removing common words, sorting words, etc.) to identify similar questions, allowing it to reuse responses even when questions are phrased differently.

## Automatic Context Determination

The MCP server includes a feature that automatically determines which documents and database tables are relevant to a user's prompt. When no document or database references are explicitly specified in the request, the system will:

1. Analyze the prompt to extract keywords
2. Search for documents and database tables that match these keywords
3. Process these relevant sources in parallel using asynchronous operations
4. Include the extracted context in the AI response

This feature allows users to submit general prompts without needing to specify which documents or tables to reference. The system will automatically find and use the most relevant sources of information.

### How It Works

1. **Keyword Extraction**: The system extracts meaningful keywords from the user's prompt by removing common words and short words.
2. **Document Matching**: It searches for documents whose filenames contain any of the extracted keywords.
3. **Table Matching**: It searches for database tables whose names contain any of the extracted keywords.
4. **Parallel Processing**: The system searches for relevant documents and tables in parallel using asynchronous operations.
5. **Context Integration**: The extracted context from relevant documents and tables is included in the AI response.

### Example

Instead of explicitly specifying document references:

```json
{
  "prompt": "Tell me about 래미안 원펜타스",
  "documentReferences": ["래미안 원펜타스_입주자모집공고.pdf"]
}
```

You can simply send:

```json
{
  "prompt": "Tell me about 래미안 원펜타스"
}
```

The system will automatically find and use the "래미안 원펜타스_입주자모집공고.pdf" document if it's available.
