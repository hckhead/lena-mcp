package com.lnmcp.lena.service;

import com.lnmcp.lena.model.McpContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ResponseCacheServiceTest {

    @Autowired
    private ResponseCacheService responseCacheService;

    @Test
    public void testNormalizePrompt() {
        // Test basic normalization
        assertEquals("hello world", responseCacheService.normalizePrompt("Hello world!"));

        // Test removing common words
        assertEquals("hello world", responseCacheService.normalizePrompt("Hello the world!"));

        // Test sorting words
        assertEquals("hello world", responseCacheService.normalizePrompt("world Hello!"));

        // Test removing short words
        assertEquals("hello world", responseCacheService.normalizePrompt("Hello to the world!"));

        // Test with Korean characters (note: words are sorted alphabetically)
        assertEquals("세계 안녕", responseCacheService.normalizePrompt("안녕 세계!"));

        // Test with mixed languages
        assertEquals("hello 세계", responseCacheService.normalizePrompt("Hello 세계!"));

        // Test with numbers (note: words are sorted alphabetically)
        assertEquals("123 hello world", responseCacheService.normalizePrompt("Hello 123 world!"));

        // Test with empty string
        assertEquals("", responseCacheService.normalizePrompt(""));

        // Test with null
        assertEquals("", responseCacheService.normalizePrompt(null));

        // Test with only common words and short words
        assertEquals("", responseCacheService.normalizePrompt("The a to in"));
    }

    @Test
    public void testCacheResponseAndGetCachedResponse() {
        // Create a test context
        McpContext context = McpContext.builder()
                .userPrompt("What is the capital of France?")
                .aiResponse("The capital of France is Paris.")
                .build();

        // Cache the response
        responseCacheService.cacheResponse(context.getUserPrompt(), context);

        // Get the cached response
        McpContext cachedContext = responseCacheService.getCachedResponse(context.getUserPrompt());

        // Verify that the cached response is correct
        assertNotNull(cachedContext);
        assertEquals(context.getUserPrompt(), cachedContext.getUserPrompt());
        assertEquals(context.getAiResponse(), cachedContext.getAiResponse());

        // Test with a similar question
        McpContext similarContext = responseCacheService.getCachedResponse("What is France's capital?");
        assertNotNull(similarContext);
        assertEquals(context.getAiResponse(), similarContext.getAiResponse());

        // Test with a different question
        McpContext differentContext = responseCacheService.getCachedResponse("What is the capital of Germany?");
        assertNull(differentContext);
    }
}
