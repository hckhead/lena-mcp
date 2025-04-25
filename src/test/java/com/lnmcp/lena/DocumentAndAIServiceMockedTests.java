package com.lnmcp.lena;

import com.lnmcp.lena.service.AIService;
import com.lnmcp.lena.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class DocumentAndAIServiceMockedTests {

    @MockBean
    private DocumentService documentService;

    @MockBean
    private AIService aiService;

    @Test
    void contextLoads() {
        // This test will fail if the application context cannot be loaded
    }
}