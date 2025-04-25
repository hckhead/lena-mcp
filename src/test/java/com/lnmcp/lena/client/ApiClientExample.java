package com.lnmcp.lena.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Example client for testing the MCP API from another project.
 * This class demonstrates how to send requests to the MCP API endpoint.
 * 
 * Usage:
 * - Run the MCP server application
 * - Use this class to send test requests to the API
 */
public class ApiClientExample {

    private static final String API_URL = "http://localhost:8080/api/mcp/prompt";

    public static void main(String[] args) {
        // Create a RestTemplate for making HTTP requests
        RestTemplate restTemplate = new RestTemplate();

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Example 1: Basic prompt request
        System.out.println("Example 1: Basic prompt request");
        Map<String, Object> basicRequest = new HashMap<>();
        basicRequest.put("prompt", "What is Spring Boot?");

        HttpEntity<Map<String, Object>> basicEntity = new HttpEntity<>(basicRequest, headers);
        ResponseEntity<Map> basicResponse = restTemplate.postForEntity(API_URL, basicEntity, Map.class);

        System.out.println("Response status: " + basicResponse.getStatusCode());
        printResponse(basicResponse.getBody());
        System.out.println();

//        // Example 2: Prompt with document references
//        System.out.println("Example 2: Prompt with document references");
//        Map<String, Object> docRequest = new HashMap<>();
//        docRequest.put("prompt", "Explain the content of the manual");
//        docRequest.put("documentReferences", Arrays.asList("manual.pdf", "guide.ppt"));
//
//        HttpEntity<Map<String, Object>> docEntity = new HttpEntity<>(docRequest, headers);
//        ResponseEntity<Map> docResponse = restTemplate.postForEntity(API_URL, docEntity, Map.class);
//
//        System.out.println("Response status: " + docResponse.getStatusCode());
//        printResponse(docResponse.getBody());
//        System.out.println();
//
//        // Example 3: Prompt with database references
//        System.out.println("Example 3: Prompt with database references");
//        Map<String, Object> dbRequest = new HashMap<>();
//        dbRequest.put("prompt", "Show me product information");
//        dbRequest.put("databaseReferences", Arrays.asList("products", "categories"));
//
//        HttpEntity<Map<String, Object>> dbEntity = new HttpEntity<>(dbRequest, headers);
//        ResponseEntity<Map> dbResponse = restTemplate.postForEntity(API_URL, dbEntity, Map.class);
//
//        System.out.println("Response status: " + dbResponse.getStatusCode());
//        printResponse(dbResponse.getBody());
//        System.out.println();
//
//        // Example 4: Prompt with model parameters
//        System.out.println("Example 4: Prompt with model parameters");
//        Map<String, Object> paramRequest = new HashMap<>();
//        paramRequest.put("prompt", "Generate creative content");
//
//        Map<String, Object> modelParams = new HashMap<>();
//        modelParams.put("temperature", 0.9);
//        modelParams.put("maxTokens", 2000);
//        paramRequest.put("modelParameters", modelParams);
//
//        HttpEntity<Map<String, Object>> paramEntity = new HttpEntity<>(paramRequest, headers);
//        ResponseEntity<Map> paramResponse = restTemplate.postForEntity(API_URL, paramEntity, Map.class);
//
//        System.out.println("Response status: " + paramResponse.getStatusCode());
//        printResponse(paramResponse.getBody());
    }

    /**
     * Helper method to print the response in a more readable format
     */
    private static void printResponse(Map<String, Object> response) {
        System.out.println("Prompt: " + response.get("prompt"));
        System.out.println("Response: " + response.get("response"));
        System.out.println("Timestamp: " + response.get("timestamp"));

        if (response.containsKey("documentSources")) {
            System.out.println("Document Sources: " + response.get("documentSources"));
        }

        if (response.containsKey("databaseSources")) {
            System.out.println("Database Sources: " + response.get("databaseSources"));
        }
    }
}
