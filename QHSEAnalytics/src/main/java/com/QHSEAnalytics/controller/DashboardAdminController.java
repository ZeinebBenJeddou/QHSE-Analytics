package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.dto.response.AdminAnalysteItemResponse;
import com.QHSEAnalytics.dto.response.AdminGraphiquesDataResponse;
import com.QHSEAnalytics.dto.response.AdminKpiCritiqueResponse;
import com.QHSEAnalytics.dto.response.AdminRepartitionResponse;
import com.QHSEAnalytics.dto.response.AdminStatsResponse;
import com.QHSEAnalytics.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.dto.response.ComparatifTableauResponse;
import com.QHSEAnalytics.service.AnalyseIaService;
import com.QHSEAnalytics.service.DashboardAdminService;
import com.QHSEAnalytics.service.DashboardAnalysteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;
    private final DashboardAnalysteService dashboardAnalysteService;
    private final AnalyseIaService analyseIaService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardAdminService.getStats());
    }

    @GetMapping("/analystes")
    public ResponseEntity<List<AdminAnalysteItemResponse>> getAnalystes() {
        return ResponseEntity.ok(dashboardAdminService.getAnalystes());
    }

    @GetMapping("/kpis-critiques")
    public ResponseEntity<List<AdminKpiCritiqueResponse>> getKpisCritiques() {
        return ResponseEntity.ok(dashboardAdminService.getTopKpisCritiques());
    }

    @GetMapping("/repartition")
    public ResponseEntity<AdminRepartitionResponse> getRepartitionComplete() {
        return ResponseEntity.ok(dashboardAdminService.getRepartitionComplete());
    }

    @GetMapping("/graphiques")
    public ResponseEntity<AdminGraphiquesDataResponse> getGraphiques() {
        return ResponseEntity.ok(dashboardAdminService.getGraphiques());
    }

    @GetMapping("/analystes/{userId}/comparatif/{importId}")
    public ResponseEntity<ComparatifTableauResponse> getComparatifAnalyste(@PathVariable Long userId, @PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getComparatif(userId, importId));
    }

    @GetMapping("/analystes/{userId}/analyses/{importId}")
    public ResponseEntity<AnalyseCompleteResponse> getAnalysesAnalyste(@PathVariable Long userId, @PathVariable Long importId) {
        return ResponseEntity.ok(analyseIaService.getAnalyseComplete(importId, userId, true));
    }
}