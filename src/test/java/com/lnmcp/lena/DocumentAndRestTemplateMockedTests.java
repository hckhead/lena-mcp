package com.lnmcp.lena;

import com.lnmcp.lena.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
public class DocumentAndRestTemplateMockedTests {

    @MockBean
    private DocumentService documentService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void contextLoads() {
        // This test will fail if the application context cannot be loaded
    }
}