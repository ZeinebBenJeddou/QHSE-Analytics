package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.entity.AiConfig;
import com.QHSEAnalytics.shared.repository.AiConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository repository;

    public List<AiConfig> getAll() {
        return repository.findAll();
    }

    @Cacheable(value = "aiConfig", key = "#key")
    public double getDouble(String key, double defaultValue) {
        return repository.findById(key).map(c -> {
            try {
                return Double.parseDouble(c.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid double value for config key '{}': {}", key, c.getValue());
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    @Cacheable(value = "aiConfig", key = "#key")
    public int getInt(String key, int defaultValue) {
        return repository.findById(key).map(c -> {
            try {
                return Integer.parseInt(c.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid int value for config key '{}': {}", key, c.getValue());
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    @CacheEvict(value = "aiConfig", key = "#key")
    @Transactional
    public AiConfig update(String key, String value) {
        AiConfig config = repository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("Clé de configuration introuvable : " + key));
        config.setValue(value.trim());
        return repository.save(config);
    }
}
