package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.shared.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.kpi.service.KpiEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/kpi-enrichment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class KpiEnrichmentController {

    private final KpiEnrichmentService kpiEnrichmentService;


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


    @PostMapping("/{importSessionId}/analyse")
    public ResponseEntity<List<KpiEnrichedResponse>> analyse(
            @PathVariable Long importSessionId,
            @RequestParam(defaultValue = "false") boolean force) {

        List<KpiEnrichedResponse> results = kpiEnrichmentService.analyseAll(importSessionId, force);
        return ResponseEntity.ok(results);
    }


    @GetMapping("/{importSessionId}")
    public ResponseEntity<List<KpiEnrichedResponse>> getEnrichedView(
            @PathVariable Long importSessionId) {

        return ResponseEntity.ok(kpiEnrichmentService.getEnrichedView(importSessionId));
    }


}
