package com.lnmcp.lena.service;

import java.util.List;
import java.util.Map;

/**
 * Service for generating and managing embeddings for documents.
 */
public interface EmbeddingService {

    /**
     * Generate an embedding for a text
     *
     * @param text The text to generate an embedding for
     * @return A list of floating-point numbers representing the embedding
     */
    List<Float> generateEmbedding(String text);

    /**
     * Calculate the cosine similarity between two embeddings
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return A similarity score between 0 and 1, where 1 is most similar
     */
    float calculateSimilarity(List<Float> embedding1, List<Float> embedding2);

    /**
     * Generate and store embeddings for a document
     *
     * @param documentId The document identifier (e.g., filename)
     * @param content The document content
     */
    void storeDocumentEmbedding(String documentId, String content);

    /**
     * Find the most similar documents to a query
     *
     * @param query The query text
     * @param maxResults The maximum number of results to return
     * @return A map of document IDs to similarity scores, sorted by similarity (descending)
     */
    Map<String, Float> findSimilarDocuments(String query, int maxResults);

    /**
     * Clear all stored embeddings
     */
    void clearEmbeddings();
}