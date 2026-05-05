package com.QHSEAnalytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GroqKeyRotator {

    private final List<String> keys;
    private final AtomicInteger index = new AtomicInteger(0);

    public GroqKeyRotator(@Value("${app.groq.api-keys}") String raw) {
        this.keys = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
        if (this.keys.isEmpty()) {
            throw new IllegalStateException("No Groq API keys configured");
        }
    }

    public String next() {
        return keys.get(Math.abs(index.getAndIncrement() % keys.size()));
    }

    public int keyCount() {
        return keys.size();
    }
}