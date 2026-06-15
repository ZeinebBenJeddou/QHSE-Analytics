package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.exception.ProviderUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class LlmProviderChain {

    public static final String PROVIDER_CHAIN_VERSION = "v1";
    private final GroqService groqService;
    private final ProviderCooldownManager cooldownManager;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;


    private final ExecutorService sharedExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "llm-chain-worker");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdown() {
        sharedExecutor.shutdown();
    }

    public LlmProviderChain(GroqService groqService,
                            ProviderCooldownManager cooldownManager,
                            MeterRegistry meterRegistry,
                            CacheManager cacheManager) {
        this.groqService = groqService;
        this.cooldownManager = cooldownManager;
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
    }

    public record ProviderResult(String response, String provider) {
    }

    public String generate(String prompt) {
        return generate(prompt, "default", false).response();
    }

    public ProviderResult generate(String prompt, String cacheKeyPrefix, boolean bypassCache) {
        if (prompt == null || prompt.isBlank()) {
            return new ProviderResult(null, "none");
        }


        String stablePrefix = cacheKeyPrefix == null ? ""
                : cacheKeyPrefix.replaceAll("(?:^|\\|)session=[^|]*", "").replaceAll("^\\|", "").trim();
        String fullCacheKey = "llm:" + toHexString(sha256(stablePrefix + "|" + prompt));
        Cache cache = getCache();
        if (!bypassCache && cache != null) {
            Cache.ValueWrapper cached = cache.get(fullCacheKey);
            if (cached != null && cached.get() instanceof ProviderResult providerResult) {
                log.info("[LlmChain] cache_hit mode={} cache_key_prefix={} provider={}", extractMode(cacheKeyPrefix), cacheKeyPrefix, providerResult.provider());
                incrementCounter("ai.cache.hit", Tags.of("mode", extractMode(cacheKeyPrefix)));
                return providerResult;
            }
        }

        log.info("[LlmChain] cache_miss mode={} cache_key_prefix={} cache_key_prefix_hash={}", extractMode(cacheKeyPrefix), cacheKeyPrefix, prefixHash(fullCacheKey));
        incrementCounter("ai.cache.miss", Tags.of("mode", extractMode(cacheKeyPrefix)));

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Instant start = Instant.now();
            try {
                ProviderResult result = executeWithTimeout(prompt, Duration.ofSeconds(30));
                if (result != null && result.response() != null && !result.response().isBlank()) {
                    Duration latency = Duration.between(start, Instant.now());
                    log.info("[LlmChain] provider response in {}ms on attempt {} provider={} cache_prefix={}", latency.toMillis(), attempt, result.provider(), cacheKeyPrefix);
                    cacheResult(cache, fullCacheKey, result);
                    return result;
                }
                log.warn("[LlmChain] provider returned empty response on attempt {} cache_prefix={}", attempt, cacheKeyPrefix);
            } catch (TimeoutException e) {
                log.warn("[LlmChain] provider timeout on attempt {}: {}", attempt, e.getMessage());
            } catch (ExecutionException e) {
                log.warn("[LlmChain] provider execution failed on attempt {}: {}", attempt, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            } catch (Exception e) {
                log.warn("[LlmChain] provider error on attempt {}: {}", attempt, e.getMessage());
            }
        }

        log.error("[LlmChain] All providers exhausted after {} attempts cache_prefix={}", maxAttempts, cacheKeyPrefix);
        return new ProviderResult(null, "none");
    }

    public void clearKpiAnalysisCache() {
        Cache cache = getCache();
        if (cache != null) {
            cache.clear();
            log.info("[LlmChain] Cleared kpiAnalysis cache");
        }
    }

    private void cacheResult(Cache cache, String cacheKey, ProviderResult result) {
        if (cache != null && result != null && result.response() != null) {
            cache.put(cacheKey, result);
        }
    }

    private Cache getCache() {
        if (cacheManager == null) return null;
        return cacheManager.getCache("kpiAnalysis");
    }

    private ProviderResult executeWithTimeout(String prompt, Duration timeout) throws ExecutionException, InterruptedException, TimeoutException {
        Future<ProviderResult> future = sharedExecutor.submit(() -> callProviders(prompt));
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    private ProviderResult callProviders(String prompt) {
        if (cooldownManager.isAvailable("groq")) {
            try {
                String result = groqService.generate(prompt);
                if (result != null && !result.isBlank()) {
                    log.info("[LlmChain] Selected provider=groq");
                    incrementCounter("ai.provider.selected", Tags.of("provider", "groq"));
                    return new ProviderResult(result, "groq");
                }
            } catch (ProviderUnavailableException e) {
                log.warn("[LlmChain] Groq unavailable: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("[LlmChain] Groq failed: {}", e.getMessage());
            }
        } else {
            log.info("[LlmChain] Groq on cooldown, skipping");
        }

        throw new ProviderUnavailableException("groq", "Service IA temporairement indisponible, réessayez dans quelques minutes");
    }

    private void incrementCounter(String metricName, Tags tags) {
        if (meterRegistry == null || tags == null) {
            return;
        }
        var counter = meterRegistry.counter(metricName, tags);
        if (counter != null) {
            counter.increment();
        }
    }

    private String extractMode(String cacheKeyPrefix) {
        if (cacheKeyPrefix == null || cacheKeyPrefix.isBlank()) {
            return "unknown";
        }
        for (String token : cacheKeyPrefix.split("\\|")) {
            if (token.startsWith("mode=")) {
                return token.substring("mode=".length());
            }
        }
        return "unknown";
    }

    private String prefixHash(String fullCacheKey) {
        if (fullCacheKey == null || fullCacheKey.length() < 16) {
            return fullCacheKey;
        }
        return fullCacheKey.substring(0, 16);
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, using hashCode fallback");
            return String.valueOf(input.hashCode()).getBytes(StandardCharsets.UTF_8);
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
