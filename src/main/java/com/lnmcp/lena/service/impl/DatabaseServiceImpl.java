package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DatabaseContext;
import com.lnmcp.lena.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of DatabaseService for interacting with HSQLDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public DatabaseContext extractContextFromTable(String tableName) {
        String query = "SELECT * FROM " + tableName;
        return executeCustomQuery(query, "All data from table: " + tableName);
    }

    @Override
    public List<DatabaseContext> extractContextFromMultipleTables(List<String> tableNames) {
        List<DatabaseContext> contexts = new ArrayList<>();
        
        for (String tableName : tableNames) {
            try {
                contexts.add(extractContextFromTable(tableName));
            } catch (Exception e) {
                log.error("Error extracting context from table: {}", tableName, e);
                // Continue with other tables instead of failing completely
            }
        }
        
        return contexts;
    }

    @Override
    public DatabaseContext executeCustomQuery(String query, String description) {
        DatabaseContext context = DatabaseContext.builder()
                .query(query)
                .description(description)
                .build();
        
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            
            // Extract table name from query if possible
            String tableName = extractTableNameFromQuery(query);
            context.setTableName(tableName);
            
            // Process results
            if (!results.isEmpty()) {
                // For simplicity, we'll just take the first row
                // In a real application, you might want to handle multiple rows differently
                Map<String, Object> firstRow = results.get(0);
                context.setData(firstRow);
                
                // Add row count information
                context.addData("rowCount", results.size());
                
                // Add summary of other rows if there are more
                if (results.size() > 1) {
                    context.addData("additionalRows", results.size() - 1);
                    context.addData("allRows", results);
                }
            }
        } catch (Exception e) {
            log.error("Error executing query: {}", query, e);
            context.addData("error", e.getMessage());
        }
        
        return context;
    }

    @Override
    public List<String> getAllTables() {
        List<String> tables = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            log.error("Error getting all tables", e);
        }
        
        return tables;
    }

    @Override
    public Map<String, String> getTableSchema(String tableName) {
        Map<String, String> schema = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // Get column information
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, "PUBLIC", tableName, "%");
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                schema.put(columnName, dataType);
            }
        } catch (SQLException e) {
            log.error("Error getting schema for table: {}", tableName, e);
        }
        
        return schema;
    }
    
    /**
     * Simple method to extract table name from a SQL query
     * This is a basic implementation and might not work for complex queries
     */
    private String extractTableNameFromQuery(String query) {
        String upperQuery = query.toUpperCase();
        int fromIndex = upperQuery.indexOf("FROM");
        
        if (fromIndex != -1) {
            String fromClause = upperQuery.substring(fromIndex + 4).trim();
            // Extract the first word after FROM
            int spaceIndex = fromClause.indexOf(' ');
            if (spaceIndex != -1) {
                return fromClause.substring(0, spaceIndex).trim();
            } else {
                return fromClause.trim();
            }
        }
        
        return "Unknown";
    }
}