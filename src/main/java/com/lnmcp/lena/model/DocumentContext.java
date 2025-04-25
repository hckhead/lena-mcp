package com.lnmcp.lena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents context extracted from a document (PDF or PPT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContext {
    
    /**
     * The source document filename
     */
    private String filename;
    
    /**
     * The document type (PDF, PPT, etc.)
     */
    private DocumentType documentType;
    
    /**
     * The extracted text content from the document
     */
    private String content;
    
    /**
     * Page number or slide number where the content was extracted
     */
    private Integer pageNumber;
    
    /**
     * Document types supported by the system
     */
    public enum DocumentType {
        PDF,
        PPT,
        UNKNOWN
    }
}