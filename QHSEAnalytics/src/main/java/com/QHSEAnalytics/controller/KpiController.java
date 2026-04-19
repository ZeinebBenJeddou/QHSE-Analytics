package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.dto.request.CreateKpiRequest;
import com.QHSEAnalytics.dto.request.UpdateKpiRequest;
import com.QHSEAnalytics.dto.response.CategorieKpiResponse;
import com.QHSEAnalytics.dto.response.KpiDeleteResponse;
import com.QHSEAnalytics.dto.response.KpiResponse;
import com.QHSEAnalytics.service.KpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<KpiResponse>> getKpis(@RequestParam(required = false) String categorie) {
        return ResponseEntity.ok(kpiService.getKpis(categorie));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<List<CategorieKpiResponse>> getAllCategories() {
        return ResponseEntity.ok(kpiService.getAllCategories());
    }

    @GetMapping("/inactifs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KpiResponse>> getInactiveKpis() {
        return ResponseEntity.ok(kpiService.getInactiveKpis());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<KpiResponse> getKpiById(@PathVariable Long id) {
        return ResponseEntity.ok(kpiService.getKpiById(id));
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
