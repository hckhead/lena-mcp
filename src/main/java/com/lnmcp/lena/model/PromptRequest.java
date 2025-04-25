package com.lnmcp.lena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a request containing a user prompt and optional context parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequest {
    
    /**
     * The user's prompt/query
     */
    private String prompt;
    
    /**
     * Optional list of document filenames to include in context
     */
    @Builder.Default
    private List<String> documentReferences = new ArrayList<>();
    
    /**
     * Optional list of database tables to query for context
     */
    @Builder.Default
    private List<String> databaseReferences = new ArrayList<>();
    
    /**
     * Optional parameters for AI model (temperature, etc.)
     */
    @Builder.Default
    private ModelParameters modelParameters = new ModelParameters();
    
    /**
     * Parameters for the AI model
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelParameters {
        /**
         * Temperature for the AI model (controls randomness)
         */
        @Builder.Default
        private Double temperature = 0.7;
        
        /**
         * Maximum tokens to generate in the response
         */
        @Builder.Default
        private Integer maxTokens = 1000;
    }
}