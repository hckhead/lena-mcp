package com.lnmcp.lena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a response to a prompt request, containing the AI-generated answer
 * and metadata about the context used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {
    
    /**
     * The original user prompt
     */
    private String prompt;
    
    /**
     * The AI-generated response
     */
    private String response;
    
    /**
     * Timestamp when the response was generated
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * List of document sources used in generating the response
     */
    @Builder.Default
    private List<DocumentSource> documentSources = new ArrayList<>();
    
    /**
     * List of database sources used in generating the response
     */
    @Builder.Default
    private List<DatabaseSource> databaseSources = new ArrayList<>();
    
    /**
     * Represents a document source used in the response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSource {
        private String filename;
        private DocumentContext.DocumentType type;
        private List<Integer> pageNumbers;
    }
    
    /**
     * Represents a database source used in the response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseSource {
        private String tableName;
        private String query;
    }
}