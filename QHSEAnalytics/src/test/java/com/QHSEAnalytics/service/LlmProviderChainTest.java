package com.QHSEAnalytics.service;

import com.QHSEAnalytics.shared.exception.ProviderUnavailableException;
import com.QHSEAnalytics.analytics.service.processing.GeminiClientService;
import com.QHSEAnalytics.analytics.service.GroqService;
import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.ProviderCooldownManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmProviderChainTest {

    @Mock
    private GroqService groqService;

    @Mock
    private GeminiClientService geminiService;

    @Mock
    private ProviderCooldownManager cooldownManager;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private LlmProviderChain llmProviderChain;

    @Test
    void generate_shouldReturnGroqResponseWhenAvailable() throws Exception {
        when(cooldownManager.isAvailable("groq")).thenReturn(true);
        when(groqService.generate("test prompt")).thenReturn("groq response");

        String result = llmProviderChain.generate("test prompt");

        assertThat(result).isEqualTo("groq response");
    }

    @Test
    void generate_shouldFallbackToGeminiWhenGroqFails() throws Exception {
        when(cooldownManager.isAvailable("groq")).thenReturn(true);
        when(cooldownManager.isAvailable("gemini")).thenReturn(true);
        when(groqService.generate("test prompt")).thenThrow(new ProviderUnavailableException("groq", "Groq down"));
        when(geminiService.generateRaw("test prompt")).thenReturn("gemini response");

        String result = llmProviderChain.generate("test prompt");

        assertThat(result).isEqualTo("gemini response");
    }

    @Test
    void generate_shouldReturnNullWhenAllProvidersFail() throws Exception {
        when(cooldownManager.isAvailable("groq")).thenReturn(true);
        when(cooldownManager.isAvailable("gemini")).thenReturn(true);
        when(groqService.generate("test prompt")).thenThrow(new RuntimeException("Error"));
        when(geminiService.generateRaw("test prompt")).thenThrow(new RuntimeException("Error"));

        String result = llmProviderChain.generate("test prompt");

        assertThat(result).isNull();
    }

    @Test
    void generate_shouldSkipGroqWhenOnCooldown() throws Exception {
        when(cooldownManager.isAvailable("groq")).thenReturn(false);
        when(cooldownManager.isAvailable("gemini")).thenReturn(true);
        when(geminiService.generateRaw("test prompt")).thenReturn("gemini response");

        String result = llmProviderChain.generate("test prompt");

        assertThat(result).isEqualTo("gemini response");
    }

    @Test
    void generate_shouldUseDifferentCacheKeysForDifferentImportSessions() {
        when(cacheManager.getCache("kpiAnalysis")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null);
        when(cooldownManager.isAvailable("groq")).thenReturn(true);
        when(groqService.generate("test prompt")).thenReturn("groq response");

        var first = llmProviderChain.generate(
                "test prompt",
                "importSessionId=1|mode=structured|promptVersion=v1|schemaVersion=1.1|providerChainVersion=v1",
                false);
        var second = llmProviderChain.generate(
                "test prompt",
                "importSessionId=2|mode=structured|promptVersion=v1|schemaVersion=1.1|providerChainVersion=v1",
                false);

        assertThat(first.response()).isEqualTo("groq response");
        assertThat(second.response()).isEqualTo("groq response");
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(cache, org.mockito.Mockito.times(2)).get(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0)).isNotEqualTo(captor.getAllValues().get(1));
    }

    @Test
    void generate_shouldBypassCacheWhenRequested() {
        when(cooldownManager.isAvailable("groq")).thenReturn(true);
        when(groqService.generate("test prompt")).thenReturn("fresh groq response");

        var result = llmProviderChain.generate(
                "test prompt",
                "importSessionId=9|mode=structured|promptVersion=v1|schemaVersion=1.1|providerChainVersion=v1",
                true);

        assertThat(result.response()).isEqualTo("fresh groq response");
    }
}
