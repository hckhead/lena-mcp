package com.lnmcp.lena.config;

import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Component that initializes caches at application startup.
 * This helps improve performance by pre-parsing documents and database tables.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupCacheInitializer implements CommandLineRunner {

    private final DocumentService documentService;
    private final DatabaseService databaseService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing caches at startup...");
        
        // Pre-cache documents and database tables in parallel
        CompletableFuture<Void> documentCacheFuture = CompletableFuture.runAsync(this::preloadDocumentCache);
        CompletableFuture<Void> databaseCacheFuture = CompletableFuture.runAsync(this::preloadDatabaseCache);
        
        // Wait for both caching operations to complete
        CompletableFuture.allOf(documentCacheFuture, databaseCacheFuture).join();
        
        log.info("Cache initialization completed");
    }
    
    /**
     * Preload all documents into the document cache
     */
    private void preloadDocumentCache() {
        try {
            log.info("Preloading document cache...");
            List<String> allDocuments = documentService.getAllDocuments();
            
            if (allDocuments.isEmpty()) {
                log.info("No documents found to preload");
                return;
            }
            
            log.info("Found {} documents to preload", allDocuments.size());
            
            // Extract context from all documents
            try {
                documentService.extractContextFromMultipleDocumentsByFilename(allDocuments);
                log.info("Successfully preloaded {} documents into cache", allDocuments.size());
            } catch (IOException e) {
                log.error("Error preloading documents into cache", e);
            }
        } catch (IOException e) {
            log.error("Error getting list of documents", e);
        }
    }
    
    /**
     * Preload all database tables into the database cache
     */
    private void preloadDatabaseCache() {
        try {
            log.info("Preloading database cache...");
            List<String> allTables = databaseService.getAllTables();
            
            if (allTables.isEmpty()) {
                log.info("No database tables found to preload");
                return;
            }
            
            log.info("Found {} database tables to preload", allTables.size());
            
            // Extract context from all tables
            databaseService.extractContextFromMultipleTables(allTables);
            log.info("Successfully preloaded {} database tables into cache", allTables.size());
        } catch (Exception e) {
            log.error("Error preloading database tables into cache", e);
        }
    }
}