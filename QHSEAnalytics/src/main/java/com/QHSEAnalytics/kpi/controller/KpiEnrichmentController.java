package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.shared.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.kpi.service.KpiEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/kpi-enrichment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class KpiEnrichmentController {

    private final KpiEnrichmentService kpiEnrichmentService;

    @PostMapping("/{importSessionId}/analyse")
    public ResponseEntity<List<KpiEnrichedResponse>> analyse(
            @PathVariable Long importSessionId,
            @RequestParam(defaultValue = "false") boolean force) {

        List<KpiEnrichedResponse> results = kpiEnrichmentService.analyseAll(importSessionId, force);
        return ResponseEntity.ok(results);
    }
}
