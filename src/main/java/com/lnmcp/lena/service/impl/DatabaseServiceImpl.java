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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of DatabaseService for interacting with HSQLDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    // Cache for database contexts to avoid repeated querying of the same tables
    private final Map<String, DatabaseContext> tableCache = new ConcurrentHashMap<>();

    @Override
    public DatabaseContext extractContextFromTable(String tableName) {
        // Check if the table is already in the cache
        if (tableCache.containsKey(tableName)) {
            log.debug("Using cached context for table: {}", tableName);
            return tableCache.get(tableName);
        }

        // If not in cache, query the table
        String query = "SELECT * FROM " + tableName;
        DatabaseContext context = executeCustomQuery(query, "All data from table: " + tableName);

        // Store in cache for future use
        tableCache.put(tableName, context);
        return context;
    }

    @Override
    public List<DatabaseContext> extractContextFromMultipleTables(List<String> tableNames) {
        // Process tables in parallel using streams
        return tableNames.parallelStream()
                .map(tableName -> {
                    try {
                        return extractContextFromTable(tableName);
                    } catch (Exception e) {
                        log.error("Error extracting context from table: {}", tableName, e);
                        // Continue with other tables instead of failing completely
                        return null;
                    }
                })
                .filter(context -> context != null)
                .collect(Collectors.toList());
    }

    @Override
    public DatabaseContext executeCustomQuery(String query, String description) {
        DatabaseContext context = DatabaseContext.builder()
                .query(query)
                .description(description)
                .build();

        try {
            // Limit the number of rows returned to improve performance
            String limitedQuery = query;
            if (!query.toLowerCase().contains(" limit ")) {
                limitedQuery = query + " LIMIT 100";
            }
            List<Map<String, Object>> results = jdbcTemplate.queryForList(limitedQuery);

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

    @Override
    public List<String> findRelevantTables(String prompt) {
        // Get all available tables
        List<String> allTables = getAllTables();

        if (allTables.isEmpty()) {
            return new ArrayList<>();
        }

        // Simple relevance check: check if any keywords from the prompt appear in the table name
        // In a real implementation, this would be more sophisticated, possibly using schema information
        List<String> keywords = extractKeywords(prompt);

        return allTables.stream()
                .filter(tableName -> {
                    String lowercaseTableName = tableName.toLowerCase();
                    return keywords.stream()
                            .anyMatch(keyword -> lowercaseTableName.contains(keyword.toLowerCase()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract keywords from a prompt
     * This is a simple implementation that just splits the prompt by spaces and removes common words
     */
    private List<String> extractKeywords(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Convert to lowercase and split by non-alphanumeric characters
        String[] words = prompt.toLowerCase().split("[^a-zA-Z0-9가-힣]+");

        // Filter out common words and short words
        List<String> commonWords = Arrays.asList("the", "a", "an", "and", "or", "but", "is", "are", "was", "were", 
                                               "in", "on", "at", "to", "for", "with", "by", "about", "like", 
                                               "through", "over", "before", "after", "between", "under", "during",
                                               "of", "from", "up", "down", "into", "out", "as", "if", "when", 
                                               "why", "how", "all", "any", "both", "each", "few", "more", "most",
                                               "other", "some", "such", "no", "nor", "not", "only", "own", "same",
                                               "so", "than", "too", "very", "can", "will", "just", "should", "now");

        return Arrays.stream(words)
                .filter(word -> word.length() > 2) // Filter out short words
                .filter(word -> !commonWords.contains(word)) // Filter out common words
                .collect(Collectors.toList());
    }
}
