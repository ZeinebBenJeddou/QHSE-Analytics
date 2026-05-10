package com.QHSEAnalytics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(@Value("${cache.kpiAnalysis.ttl.hours:24}") long cacheTtlHours) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of("kpiAnalysis", "ragKnowledge", "embeddings"));
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheTtlHours, TimeUnit.HOURS)
                        .maximumSize(500)
                        .recordStats()
        );
        return manager;
    }
}