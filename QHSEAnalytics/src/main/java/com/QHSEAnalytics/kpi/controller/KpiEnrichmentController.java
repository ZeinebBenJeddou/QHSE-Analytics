package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.shared.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.kpi.service.KpiEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for KPI enrichment: saving import-preview rows,
 * triggering AI analysis, and retrieving the enriched dashboard view.
 */
@RestController
@RequestMapping("/api/kpi-enrichment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class KpiEnrichmentController {

    private final KpiEnrichmentService kpiEnrichmentService;

    /**
     * Save (or overwrite) preview rows for an import session.
     * Body: list of KpiPreviewInput objects.
     */
    @PostMapping("/{importSessionId}/preview")
    public ResponseEntity<Map<String, Object>> savePreview(
            @PathVariable Long importSessionId,
            @RequestBody List<KpiEnrichmentService.KpiPreviewInput> inputs) {

        kpiEnrichmentService.savePreviewRows(importSessionId, inputs);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "saved", inputs.size(),
                "importSessionId", importSessionId
        ));
    }

    /**
     * Trigger AI analysis for all preview rows of an import session.
     * Query param {@code force=true} re-analyses already-analysed KPIs.
     */
    @PostMapping("/{importSessionId}/analyse")
    public ResponseEntity<List<KpiEnrichedResponse>> analyse(
            @PathVariable Long importSessionId,
            @RequestParam(defaultValue = "false") boolean force) {

        List<KpiEnrichedResponse> results = kpiEnrichmentService.analyseAll(importSessionId, force);
        return ResponseEntity.ok(results);
    }

    /**
     * Return the enriched view (preview + AI analysis) for all KPIs of an import session.
     */
    @GetMapping("/{importSessionId}")
    public ResponseEntity<List<KpiEnrichedResponse>> getEnrichedView(
            @PathVariable Long importSessionId) {

        return ResponseEntity.ok(kpiEnrichmentService.getEnrichedView(importSessionId));
    }

    /**
     * Development-only endpoint to debug AI responses for a single preview row.
     * Remove or protect before production use.
     */
    @GetMapping("/test/{previewId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<Map<String, Object>> debugAnalyseOne(@PathVariable Long previewId,
                                                                @RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> result = kpiEnrichmentService.debugAnalyseOne(previewId, force);
        return ResponseEntity.ok(result);
    }
}
