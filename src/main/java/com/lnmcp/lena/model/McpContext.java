package com.lnmcp.lena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MCP (Message Context Protocol) context structure.
 * This is the main container for all context information used in the AI conversation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpContext {
    
    /**
     * The user's original prompt/query
     */
    private String userPrompt;
    
    /**
     * List of document contexts extracted from PDF/PPT files
     */
    @Builder.Default
    private List<DocumentContext> documentContexts = new ArrayList<>();
    
    /**
     * List of database contexts extracted from HSQLDB
     */
    @Builder.Default
    private List<DatabaseContext> databaseContexts = new ArrayList<>();
    
    /**
     * The AI-generated response
     */
    private String aiResponse;
    
    /**
     * Add a document context to the MCP context
     */
    public void addDocumentContext(DocumentContext documentContext) {
        if (documentContexts == null) {
            documentContexts = new ArrayList<>();
        }
        documentContexts.add(documentContext);
    }
    
    /**
     * Add a database context to the MCP context
     */
    public void addDatabaseContext(DatabaseContext databaseContext) {
        if (databaseContexts == null) {
            databaseContexts = new ArrayList<>();
        }
        databaseContexts.add(databaseContext);
    }
}