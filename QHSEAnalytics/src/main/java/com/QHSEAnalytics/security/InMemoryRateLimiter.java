package com.QHSEAnalytics.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter en mémoire — protège les endpoints d'auth contre le brute-force.
 * Clé = IP + endpoint. Fenêtre glissante basée sur la première tentative.
 */
@Component
public class InMemoryRateLimiter {

    @Value("${app.rate-limit.auth.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.rate-limit.auth.window-seconds:300}")
    private long windowSeconds;

    private record BucketEntry(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    /**
     * @return true si la requête est autorisée, false si le seuil est dépassé
     */
    public boolean allowRequest(String ip, String endpoint) {
        String key = ip + "|" + endpoint;
        Instant now = Instant.now();

        buckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart().plusSeconds(windowSeconds))) {
                return new BucketEntry(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        BucketEntry entry = buckets.get(key);
        return entry == null || entry.count().get() <= maxAttempts;
    }

    public void reset(String ip, String endpoint) {
        buckets.remove(ip + "|" + endpoint);
    }
}
