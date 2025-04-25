package com.lnmcp.lena.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration class for caching.
 * This class enables caching in the application and configures a simple in-memory cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache name for AI responses
     */
    public static final String RESPONSE_CACHE = "responseCache";

    /**
     * Configure a simple cache manager
     * 
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache(RESPONSE_CACHE)
        ));
        return cacheManager;
    }
}
