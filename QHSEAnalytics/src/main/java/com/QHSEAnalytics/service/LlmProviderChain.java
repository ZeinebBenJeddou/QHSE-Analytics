package com.QHSEAnalytics.service;

import com.QHSEAnalytics.exception.ProviderUnavailableException;
import com.QHSEAnalytics.service.processing.GeminiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LlmProviderChain {

    private final GroqService groqService;
    private final GeminiClientService geminiService;
    private final ProviderCooldownManager cooldownManager;

    public LlmProviderChain(GroqService groqService,
                            GeminiClientService geminiService,
                            ProviderCooldownManager cooldownManager) {
        this.groqService = groqService;
        this.geminiService = geminiService;
        this.cooldownManager = cooldownManager;
    }

    public String generate(String prompt) {
        if (cooldownManager.isAvailable("groq")) {
            try {
                String result = groqService.generate(prompt);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            } catch (ProviderUnavailableException e) {
                log.warn("[LlmChain] Groq unavailable: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("[LlmChain] Groq failed: {}", e.getMessage());
            }
        } else {
            log.info("[LlmChain] Groq on cooldown, skipping");
        }

        if (cooldownManager.isAvailable("gemini")) {
            try {
                String result = geminiService.generateRaw(prompt);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("[LlmChain] Gemini failed: {}", e.getMessage());
            }
        } else {
            log.info("[LlmChain] Gemini on cooldown, skipping");
        }

        log.error("[LlmChain] All providers exhausted for prompt");
        return null;
    }
}