package com.QHSEAnalytics.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ProviderCooldownManager {

    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public boolean isAvailable(String provider) {
        Instant until = cooldowns.get(provider);
        if (until == null) {
            return true;
        }
        if (Instant.now().isAfter(until)) {
            cooldowns.remove(provider);
            return true;
        }
        return false;
    }

    public void setCooldown(String provider, long seconds) {
        cooldowns.put(provider, Instant.now().plusSeconds(seconds));
        log.warn("[Cooldown] Provider '{}' paused for {}s", provider, seconds);
    }

    public void clearCooldown(String provider) {
        cooldowns.remove(provider);
    }

    public Instant getCooldownUntil(String provider) {
        return cooldowns.get(provider);
    }
}