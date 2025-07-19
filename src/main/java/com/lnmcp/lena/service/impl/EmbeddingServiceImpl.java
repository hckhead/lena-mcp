package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of EmbeddingService using Ollama API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.model:llama2}")
    private String embeddingModel;

    private final RestTemplate restTemplate;

    // In-memory storage for document embeddings
    private final Map<String, List<Float>> documentEmbeddings = new ConcurrentHashMap<>();

    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            String url = ollamaBaseUrl + "/api/embeddings";

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("prompt", text);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make POST request
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);

            // Extract embedding from response
            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("embedding")) {
                List<Double> doubleEmbedding = (List<Double>) responseBody.get("embedding");
                // Convert Double to Float for memory efficiency
                return doubleEmbedding.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());
            } else {
                log.error("Unexpected response format from Ollama API: {}", responseBody);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            return Collections.emptyList();
        }
    }

    @Override
    public float calculateSimilarity(List<Float> embedding1, List<Float> embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.isEmpty() || embedding2.isEmpty() || 
            embedding1.size() != embedding2.size()) {
            return 0.0f;
        }

        // Calculate dot product
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < embedding1.size(); i++) {
            float val1 = embedding1.get(i);
            float val2 = embedding2.get(i);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        // Calculate cosine similarity
        if (norm1 <= 0.0f || norm2 <= 0.0f) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public void storeDocumentEmbedding(String documentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Cannot generate embedding for empty content: {}", documentId);
            return;
        }

        // Generate embedding for document content
        List<Float> embedding = generateEmbedding(content);
        
        if (embedding.isEmpty()) {
            log.warn("Failed to generate embedding for document: {}", documentId);
            return;
        }

        // Store embedding
        documentEmbeddings.put(documentId, embedding);
        log.info("Stored embedding for document: {}", documentId);
    }

    @Override
    public Map<String, Float> findSimilarDocuments(String query, int maxResults) {
        if (documentEmbeddings.isEmpty()) {
            log.warn("No document embeddings available for similarity search");
            return Collections.emptyMap();
        }

        // Generate embedding for query
        List<Float> queryEmbedding = generateEmbedding(query);
        
        if (queryEmbedding.isEmpty()) {
            log.warn("Failed to generate embedding for query: {}", query);
            return Collections.emptyMap();
        }

        // Calculate similarity scores
        Map<String, Float> similarityScores = new HashMap<>();
        
        for (Map.Entry<String, List<Float>> entry : documentEmbeddings.entrySet()) {
            String documentId = entry.getKey();
            List<Float> documentEmbedding = entry.getValue();
            
            float similarity = calculateSimilarity(queryEmbedding, documentEmbedding);
            similarityScores.put(documentId, similarity);
        }

        // Sort by similarity score (descending) and limit results
        return similarityScores.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }

    @Override
    public void clearEmbeddings() {
        documentEmbeddings.clear();
        log.info("Cleared all document embeddings");
    }
}