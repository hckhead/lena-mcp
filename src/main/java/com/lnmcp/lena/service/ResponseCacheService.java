package com.lnmcp.lena.service;

import com.lnmcp.lena.config.CacheConfig;
import com.lnmcp.lena.model.McpContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for caching AI responses to similar questions.
 * This service provides methods to get responses from the cache and to store responses in the cache.
 */
@Service
@Slf4j
public class ResponseCacheService {

    /**
     * List of common words to remove when normalizing prompts
     */
    private static final List<String> COMMON_WORDS = Arrays.asList(
            "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
            "in", "on", "at", "to", "for", "with", "by", "about", "like",
            "through", "over", "before", "after", "between", "under", "during",
            "of", "from", "up", "down", "into", "out", "as", "if", "when",
            "why", "how", "all", "any", "both", "each", "few", "more", "most",
            "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "can", "will", "just", "should", "now"
    );

    /**
     * Get a response from the cache for a given prompt.
     * If the cache doesn't contain a response for the prompt, return null.
     *
     * @param prompt The user's prompt
     * @return The cached McpContext or null if not found
     */
    @Cacheable(value = CacheConfig.RESPONSE_CACHE, key = "#root.method.name + '_' + #root.target.normalizePrompt(#prompt)", unless = "#result == null")
    public McpContext getCachedResponse(String prompt) {
        // This method body won't be executed if there's a cache hit
        // It's only executed on a cache miss, and in that case it returns null
        log.debug("Cache miss for prompt: {}", prompt);
        return null;
    }

    /**
     * Store a response in the cache for a given prompt.
     *
     * @param prompt The user's prompt
     * @param context The McpContext containing the response
     * @return The stored McpContext
     */
    @CachePut(value = CacheConfig.RESPONSE_CACHE, key = "'getCachedResponse' + '_' + #root.target.normalizePrompt(#prompt)")
    public McpContext cacheResponse(String prompt, McpContext context) {
        log.info("Caching response for prompt: {}", prompt);
        return context;
    }

    /**
     * Normalize a prompt to identify similar questions.
     * This method converts the prompt to lowercase, removes punctuation,
     * removes common words, and sorts the remaining words.
     *
     * @param prompt The user's prompt
     * @return The normalized prompt
     */
    public String normalizePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "";
        }

        // Convert to lowercase and split by non-alphanumeric characters
        String[] words = prompt.toLowerCase().split("[^a-zA-Z0-9가-힣]+");

        // Filter out common words and short words, then sort
        return Arrays.stream(words)
                .filter(word -> {
                    // Keep Korean words regardless of length
                    if (word.matches(".*[가-힣].*")) {
                        return true;
                    }
                    // For non-Korean words, filter out short words
                    return word.length() > 2;
                })
                .filter(word -> {
                    // Only filter out common words for English words
                    if (word.matches(".*[가-힣].*")) {
                        return true;
                    }
                    return !COMMON_WORDS.contains(word);
                })
                .sorted() // Sort words to handle different word orders
                .collect(Collectors.joining(" "));
    }
}
