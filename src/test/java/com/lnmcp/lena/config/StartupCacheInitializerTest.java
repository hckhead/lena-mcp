package com.lnmcp.lena.config;

import com.lnmcp.lena.service.DatabaseService;
import com.lnmcp.lena.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StartupCacheInitializerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private StartupCacheInitializer startupCacheInitializer;

    @Test
    public void testCacheInitialization() throws Exception {
        // Setup mock document service
        List<String> mockDocuments = Arrays.asList("document1.pdf", "document2.pdf");
        when(documentService.getAllDocuments()).thenReturn(mockDocuments);
        
        // Setup mock database service
        List<String> mockTables = Arrays.asList("table1", "table2");
        when(databaseService.getAllTables()).thenReturn(mockTables);
        
        // Run the initializer
        startupCacheInitializer.run(new String[0]);
        
        // Verify that the document service methods were called with the correct parameters
        verify(documentService, timeout(5000)).getAllDocuments();
        verify(documentService, timeout(5000)).extractContextFromMultipleDocumentsByFilename(mockDocuments);
        
        // Verify that the database service methods were called with the correct parameters
        verify(databaseService, timeout(5000)).getAllTables();
        verify(databaseService, timeout(5000)).extractContextFromMultipleTables(mockTables);
    }
    
    @Test
    public void testEmptyDocumentList() throws Exception {
        // Setup mock document service to return empty list
        when(documentService.getAllDocuments()).thenReturn(Arrays.asList());
        
        // Setup mock database service
        List<String> mockTables = Arrays.asList("table1", "table2");
        when(databaseService.getAllTables()).thenReturn(mockTables);
        
        // Run the initializer
        startupCacheInitializer.run(new String[0]);
        
        // Verify that getAllDocuments was called but extractContextFromMultipleDocumentsByFilename was not
        verify(documentService, timeout(5000)).getAllDocuments();
        verify(documentService, never()).extractContextFromMultipleDocumentsByFilename(any());
        
        // Verify that the database service methods were still called
        verify(databaseService, timeout(5000)).getAllTables();
        verify(databaseService, timeout(5000)).extractContextFromMultipleTables(mockTables);
    }
    
    @Test
    public void testEmptyTableList() throws Exception {
        // Setup mock document service
        List<String> mockDocuments = Arrays.asList("document1.pdf", "document2.pdf");
        when(documentService.getAllDocuments()).thenReturn(mockDocuments);
        
        // Setup mock database service to return empty list
        when(databaseService.getAllTables()).thenReturn(Arrays.asList());
        
        // Run the initializer
        startupCacheInitializer.run(new String[0]);
        
        // Verify that the document service methods were called
        verify(documentService, timeout(5000)).getAllDocuments();
        verify(documentService, timeout(5000)).extractContextFromMultipleDocumentsByFilename(mockDocuments);
        
        // Verify that getAllTables was called but extractContextFromMultipleTables was not
        verify(databaseService, timeout(5000)).getAllTables();
        verify(databaseService, never()).extractContextFromMultipleTables(any());
    }
    
    @Test
    public void testDocumentServiceException() throws Exception {
        // Setup mock document service to throw exception
        when(documentService.getAllDocuments()).thenThrow(new IOException("Test exception"));
        
        // Setup mock database service
        List<String> mockTables = Arrays.asList("table1", "table2");
        when(databaseService.getAllTables()).thenReturn(mockTables);
        
        // Run the initializer
        startupCacheInitializer.run(new String[0]);
        
        // Verify that getAllDocuments was called but extractContextFromMultipleDocumentsByFilename was not
        verify(documentService, timeout(5000)).getAllDocuments();
        verify(documentService, never()).extractContextFromMultipleDocumentsByFilename(any());
        
        // Verify that the database service methods were still called
        verify(databaseService, timeout(5000)).getAllTables();
        verify(databaseService, timeout(5000)).extractContextFromMultipleTables(mockTables);
    }
}