package com.abcde.hdbresale.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caching for external API responses (Section 2.5 item 6). The cache type comes from configuration: an
 * in-memory map by default, Redis in the {@code docker} profile (spring.cache.type=redis, with a TTL).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
