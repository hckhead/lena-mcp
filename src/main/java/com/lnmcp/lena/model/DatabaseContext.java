package com.lnmcp.lena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents context extracted from the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseContext {
    
    /**
     * The table name from which the data was extracted
     */
    private String tableName;
    
    /**
     * The query used to extract the data
     */
    private String query;
    
    /**
     * A description of what this data represents
     */
    private String description;
    
    /**
     * The extracted data as key-value pairs
     * Key could be column name or identifier, value is the data
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * Add a data entry to the context
     */
    public void addData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }
}