package com.lnmcp.lena.service;

import com.lnmcp.lena.model.DocumentContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service for processing documents (PDF, PPT) and extracting context.
 */
public interface DocumentService {

    /**
     * Extract context from a document file
     *
     * @param filePath Path to the document file
     * @return DocumentContext containing extracted information
     * @throws IOException If there's an error reading or processing the file
     */
    DocumentContext extractContext(Path filePath) throws IOException;

    /**
     * Extract context from multiple document files
     *
     * @param filePaths List of paths to document files
     * @return List of DocumentContext objects containing extracted information
     * @throws IOException If there's an error reading or processing any file
     */
    List<DocumentContext> extractContextFromMultipleDocuments(List<Path> filePaths) throws IOException;

    /**
     * Extract context from a document file by name
     *
     * @param filename Name of the document file (will be looked up in the configured documents path)
     * @return DocumentContext containing extracted information
     * @throws IOException If there's an error reading or processing the file
     */
    DocumentContext extractContextByFilename(String filename) throws IOException;

    /**
     * Extract context from multiple document files by name
     *
     * @param filenames List of document filenames
     * @return List of DocumentContext objects containing extracted information
     * @throws IOException If there's an error reading or processing any file
     */
    List<DocumentContext> extractContextFromMultipleDocumentsByFilename(List<String> filenames) throws IOException;

    /**
     * Find documents that might be relevant to the given prompt
     *
     * @param prompt The user's prompt
     * @return List of document filenames that might be relevant
     * @throws IOException If there's an error reading or processing any file
     */
    List<String> findRelevantDocuments(String prompt) throws IOException;

    /**
     * Get a list of all available documents
     *
     * @return List of document filenames
     * @throws IOException If there's an error reading the documents directory
     */
    List<String> getAllDocuments() throws IOException;
}
