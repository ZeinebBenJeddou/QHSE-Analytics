package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.shared.dto.request.CreateKpiRequest;
import com.QHSEAnalytics.shared.dto.request.UpdateKpiRequest;
import com.QHSEAnalytics.shared.dto.response.CategorieKpiResponse;
import com.QHSEAnalytics.shared.dto.response.KpiDeleteResponse;
import com.QHSEAnalytics.shared.dto.response.KpiResponse;
import com.QHSEAnalytics.kpi.service.KpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<Page<KpiResponse>> getKpis(
            @RequestParam(required = false) String categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(kpiService.getKpis(categorie, PageRequest.of(page, size)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<List<CategorieKpiResponse>> getAllCategories() {
        return ResponseEntity.ok(kpiService.getAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KpiResponse> createKpi(@Valid @RequestBody CreateKpiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kpiService.createKpi(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KpiResponse> updateKpi(@PathVariable Long id, @Valid @RequestBody UpdateKpiRequest request) {
        return ResponseEntity.ok(kpiService.updateKpi(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KpiDeleteResponse> deleteKpi(@PathVariable Long id) {
        return ResponseEntity.ok(kpiService.deleteKpi(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KpiResponse> restoreKpi(@PathVariable Long id) {
        return ResponseEntity.ok(kpiService.restoreKpi(id));
    }
}
