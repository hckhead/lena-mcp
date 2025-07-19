# Enhanced RAG Capabilities

This document describes the enhanced RAG (Retrieval-Augmented Generation) capabilities implemented in the MCP server.

## Multiple Document Format Support

The system now supports various document formats:

- **PDF files**: Using Apache PDFBox for text extraction
- **PowerPoint files (PPT/PPTX)**: Using Apache POI for slide content extraction
- **Text files (TXT)**: Direct text reading with standard Java file I/O

This enhancement allows the system to process a wider range of document types, making it more versatile for different use cases.

## Vector Embeddings for Semantic Search

The system now uses vector embeddings for semantic search:

- Documents are automatically embedded when processed
- Embeddings are generated using Ollama's embedding API
- Queries are embedded and compared to document embeddings using cosine similarity
- This enables finding semantically relevant documents even when keywords don't match exactly

Vector embeddings provide a more sophisticated way to match documents to queries, capturing semantic relationships rather than just keyword matches.

## Hybrid Search Strategy

The system employs a hybrid approach to document retrieval:

- **Primary**: Vector similarity search for semantic matching
  - Generates embeddings for the query
  - Compares to stored document embeddings using cosine similarity
  - Returns documents with highest similarity scores

- **Fallback**: Keyword-based search with fuzzy matching
  - Used when vector search is disabled or fails
  - Extracts keywords from the query
  - Matches documents based on keyword frequency and fuzzy matching
  - Includes edit distance calculations for approximate matching

This hybrid approach ensures robust document retrieval even when one method might not yield optimal results.

## Explicit Unknown Information Handling

When information is not available in the referenced documents:

- The AI explicitly states: "I don't have enough information in the referenced materials to answer this question."
- The system avoids making up or inferring information not in the context
- Responses include source citations for transparency

This enhancement improves the reliability of the system by clearly indicating when information is not available, rather than generating potentially incorrect responses.

## Configuration Options

The enhanced RAG capabilities can be configured through application properties:

- `mcp.embeddings.enabled`: Enable/disable vector embeddings (default: true)
- `spring.ai.ollama.embedding.model`: Model to use for embeddings (default: llama2)

## Implementation Details

The enhanced RAG capabilities are implemented through:

1. **EmbeddingService**: A new service that handles generating and storing embeddings
   - Uses Ollama's API to generate embeddings
   - Stores embeddings in memory for quick retrieval
   - Provides methods for similarity search

2. **DocumentService**: Enhanced to support TXT files and use embeddings
   - Added TXT file type to DocumentType enum
   - Implemented TXT file parsing
   - Integrated with EmbeddingService for document relevance

3. **AIService**: Improved response handling for unknown information
   - Enhanced system prompt with explicit instructions
   - Added source citation requirements

These enhancements significantly improve the system's ability to find relevant information and provide accurate responses based on the available context.