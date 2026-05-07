package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.kpi.service.KpiEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Development-only endpoints for KPI enrichment debugging.
 * Loaded only when 'dev' profile is active.
 */
@RestController
@RequestMapping("/api/kpi/enrichment")
@Profile("dev")
@RequiredArgsConstructor
public class KpiEnrichmentDevController {

    private final KpiEnrichmentService kpiEnrichmentService;

    @GetMapping("/test/{kpiId}")
    public ResponseEntity<Map<String, Object>> debugAnalyse(@PathVariable("kpiId") Long kpiId,
                                                             @RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> result = kpiEnrichmentService.debugAnalyseOne(kpiId, force);
        return ResponseEntity.ok(result);
    }
}
