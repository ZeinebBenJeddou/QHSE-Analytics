package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.analytics.service.IaHealthService;
import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.ProviderCooldownManager;
import com.QHSEAnalytics.shared.dto.response.IaHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class IaAdminController {

    private final IaHealthService iaHealthService;
    private final LlmProviderChain llmProviderChain;
    private final ProviderCooldownManager cooldownManager;

    @GetMapping("/health")
    public ResponseEntity<IaHealthResponse> getHealth() {
        return ResponseEntity.ok(iaHealthService.getHealth());
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        llmProviderChain.clearKpiAnalysisCache();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/providers/{name}/cooldown")
    public ResponseEntity<Void> clearCooldown(@PathVariable String name) {
        cooldownManager.clearCooldown(name);
        return ResponseEntity.noContent().build();
    }
}
