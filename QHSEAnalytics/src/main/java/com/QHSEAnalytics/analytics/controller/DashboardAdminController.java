package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.shared.dto.response.AdminAnalysteItemResponse;
import com.QHSEAnalytics.shared.dto.response.AdminGraphiquesDataResponse;
import com.QHSEAnalytics.shared.dto.response.AdminKpiCritiqueResponse;
import com.QHSEAnalytics.shared.dto.response.AdminRepartitionResponse;
import com.QHSEAnalytics.shared.dto.response.AdminStatsResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.shared.dto.response.HistoriqueAnalysteResponse;
import com.QHSEAnalytics.shared.dto.response.ComparatifTableauResponse;
import com.QHSEAnalytics.analytics.service.AnalyseIaService;
import com.QHSEAnalytics.analytics.service.DashboardAdminService;
import com.QHSEAnalytics.analytics.service.DashboardAnalysteService;
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

    @GetMapping("/historique")
    public ResponseEntity<HistoriqueAnalysteResponse> getHistorique() {
        return ResponseEntity.ok(dashboardAdminService.getHistorique());
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