package com.QHSEAnalytics.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@Slf4j
public class InMemoryRateLimiter {

    @Value("${app.rate-limit.auth.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.rate-limit.auth.window-seconds:300}")
    private long windowSeconds;

    private record BucketEntry(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();


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


    @Scheduled(fixedDelay = 600_000)
    public void evictExpiredEntries() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        int before = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("[RateLimit] Purged {} expired bucket(s), {} remaining", removed, buckets.size());
        }
    }
}
