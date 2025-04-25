package com.lnmcp.lena.service;

import com.lnmcp.lena.model.DatabaseContext;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the database and extracting context.
 */
public interface DatabaseService {
    
    /**
     * Extract context from a specific table
     *
     * @param tableName The name of the table to query
     * @return DatabaseContext containing extracted information
     */
    DatabaseContext extractContextFromTable(String tableName);
    
    /**
     * Extract context from multiple tables
     *
     * @param tableNames List of table names to query
     * @return List of DatabaseContext objects containing extracted information
     */
    List<DatabaseContext> extractContextFromMultipleTables(List<String> tableNames);
    
    /**
     * Execute a custom SQL query and extract context
     *
     * @param query The SQL query to execute
     * @param description Description of what this query is for
     * @return DatabaseContext containing extracted information
     */
    DatabaseContext executeCustomQuery(String query, String description);
    
    /**
     * Get a list of all available tables in the database
     *
     * @return List of table names
     */
    List<String> getAllTables();
    
    /**
     * Get the schema information for a specific table
     *
     * @param tableName The name of the table
     * @return Map of column names to their data types
     */
    Map<String, String> getTableSchema(String tableName);
}