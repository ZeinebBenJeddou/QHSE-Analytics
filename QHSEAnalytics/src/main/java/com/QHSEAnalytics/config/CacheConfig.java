package com.QHSEAnalytics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(@Value("${cache.kpiAnalysis.ttl.hours:24}") long cacheTtlHours) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                caffeine("kpiAnalysis",  cacheTtlHours, TimeUnit.HOURS,   500),
                caffeine("ragKnowledge", cacheTtlHours, TimeUnit.HOURS,   500),
                caffeine("embeddings",   cacheTtlHours, TimeUnit.HOURS,   500),
                caffeine("aiConfig",     60,            TimeUnit.SECONDS,  50)
        ));
        return manager;
    }

    private CaffeineCache caffeine(String name, long ttl, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl, unit)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
