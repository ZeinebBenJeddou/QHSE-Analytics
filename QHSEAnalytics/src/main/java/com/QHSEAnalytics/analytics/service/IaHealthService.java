package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.dto.response.IaHealthResponse;
import com.QHSEAnalytics.shared.dto.response.IaMetricsResponse;
import com.QHSEAnalytics.shared.dto.response.ProviderStatusResponse;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IaHealthService {

    private final MeterRegistry meterRegistry;
    private final ProviderCooldownManager cooldownManager;
    private final EmbeddingService embeddingService;
    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final CacheManager cacheManager;

    public IaHealthResponse getHealth() {
        return IaHealthResponse.builder()
                .providers(buildProviderStatuses())
                .metrics(buildMetrics())
                .embeddingConfigured(embeddingService.isConfigured())
                .ragEntriesCount(ragKnowledgeRepository.count())
                .llmCacheEnabled(cacheManager.getCache("kpiAnalysis") != null)
                .build();
    }

    private List<ProviderStatusResponse> buildProviderStatuses() {
        return List.of(
            buildStatus("groq"),
            buildStatus("gemini"),
            buildStatus("gemini-embedding")
        );
    }

    private ProviderStatusResponse buildStatus(String name) {
        boolean available = cooldownManager.isAvailable(name);
        Instant until = available ? null : cooldownManager.getCooldownUntil(name);
        return ProviderStatusResponse.builder()
                .name(name)
                .available(available)
                .cooldownUntil(until)
                .build();
    }

    private IaMetricsResponse buildMetrics() {
        return IaMetricsResponse.builder()
                .successCount(sumCounter("ai.request.count", "status", "success"))
                .failedCount(sumCounter("ai.request.count", "status", "failed"))
                .retryCount(sumCounter("ai.retry.count", null, null))
                .cacheHits(sumCounter("ai.cache.hit", null, null))
                .cacheMisses(sumCounter("ai.cache.miss", null, null))
                .avgLatencyMs(avgTimer("ai.latency.ms"))
                .parseErrors(sumCounter("ai.parse.error.count", null, null))
                .validationErrors(sumCounter("ai.validation.error.count", null, null))
                .providerGroqCount(sumCounter("ai.provider.selected", "provider", "groq"))
                .providerGeminiCount(sumCounter("ai.provider.selected", "provider", "gemini"))
                .build();
    }

    private double sumCounter(String name, String tagKey, String tagValue) {
        try {
            var search = meterRegistry.find(name);
            if (tagKey != null && tagValue != null) {
                search = search.tag(tagKey, tagValue);
            }
            return search.counters().stream()
                    .mapToDouble(Counter::count)
                    .sum();
        } catch (Exception e) {
            log.debug("[IaHealth] Compteur introuvable: {}", name);
            return 0.0;
        }
    }

    private double avgTimer(String name) {
        try {
            return meterRegistry.find(name).timers().stream()
                    .mapToDouble(t -> t.mean(TimeUnit.MILLISECONDS))
                    .average()
                    .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
