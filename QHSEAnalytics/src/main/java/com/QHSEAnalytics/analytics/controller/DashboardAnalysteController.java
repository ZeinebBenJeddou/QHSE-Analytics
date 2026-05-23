package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.auth.service.SecurityUtils;
import com.QHSEAnalytics.shared.dto.response.AlertesResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.shared.dto.response.ComparatifTableauResponse;
import com.QHSEAnalytics.shared.dto.response.GraphiquesDataResponse;
import com.QHSEAnalytics.shared.dto.response.HistoriqueAnalysteResponse;
import com.QHSEAnalytics.shared.dto.response.ResumeAnalysteResponse;
import com.QHSEAnalytics.analytics.service.DashboardAnalysteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard Analyste", description = "Tableau de bord et comparatif N vs N-1")
@RestController
@RequestMapping("/api/dashboard/analyste")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class DashboardAnalysteController {

    private final DashboardAnalysteService dashboardAnalysteService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Résumé général du tableau de bord", description = "Compteurs par niveau de classification, dernière session, score global")
    @GetMapping("/resume")
    public ResponseEntity<ResumeAnalysteResponse> getResume() {
        return ResponseEntity.ok(dashboardAnalysteService.getResume(securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "Tableau comparatif N vs N-1", description = "Liste des KPIs avec variations, classifications et analyses IA")
    @GetMapping("/comparatif/{importId}")
    public ResponseEntity<ComparatifTableauResponse> getComparatif(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getComparatif(securityUtils.getCurrentUserId(), importId));
    }

    @Operation(summary = "Données graphiques", description = "Barres groupées, radar, top KPIs dégradés")
    @GetMapping("/graphiques/{importId}")
    public ResponseEntity<GraphiquesDataResponse> getGraphiques(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getGraphiques(securityUtils.getCurrentUserId(), importId));
    }

    @Operation(summary = "Historique paginé des imports", description = "Liste des imports passés avec statut et métadonnées")
    @GetMapping("/historique")
    public ResponseEntity<HistoriqueAnalysteResponse> getHistorique(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dashboardAnalysteService.getHistorique(
                securityUtils.getCurrentUserId(), PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/analyses/{importId}")
    public ResponseEntity<AnalyseCompleteResponse> getAnalysesIa(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getAnalysesIa(securityUtils.getCurrentUserId(), importId));
    }

    @Operation(summary = "Alertes critiques actives", description = "KPIs en état CRITIQUE nécessitant une action immédiate")
    @GetMapping("/alertes")
    public ResponseEntity<AlertesResponse> getAlertes() {
        return ResponseEntity.ok(dashboardAnalysteService.getAlertes(securityUtils.getCurrentUserId()));
    }
}
