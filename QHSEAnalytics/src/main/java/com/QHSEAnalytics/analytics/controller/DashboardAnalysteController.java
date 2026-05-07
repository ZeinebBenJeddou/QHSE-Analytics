package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.shared.dto.response.ComparatifTableauResponse;
import com.QHSEAnalytics.shared.dto.response.GraphiquesDataResponse;
import com.QHSEAnalytics.shared.dto.response.HistoriqueAnalysteResponse;
import com.QHSEAnalytics.shared.dto.response.ResumeAnalysteResponse;
import com.QHSEAnalytics.analytics.service.DashboardAnalysteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/analyste")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class DashboardAnalysteController {

    private final DashboardAnalysteService dashboardAnalysteService;
    private final UserRepository userRepository;

    @GetMapping("/resume")
    public ResponseEntity<ResumeAnalysteResponse> getResume() {
        return ResponseEntity.ok(dashboardAnalysteService.getResume(getCurrentUserId()));
    }

    @GetMapping("/comparatif/{importId}")
    public ResponseEntity<ComparatifTableauResponse> getComparatif(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getComparatif(getCurrentUserId(), importId));
    }

    @GetMapping("/graphiques/{importId}")
    public ResponseEntity<GraphiquesDataResponse> getGraphiques(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getGraphiques(getCurrentUserId(), importId));
    }

    @GetMapping("/historique")
    public ResponseEntity<HistoriqueAnalysteResponse> getHistorique() {
        return ResponseEntity.ok(dashboardAnalysteService.getHistorique(getCurrentUserId()));
    }

    @GetMapping("/analyses/{importId}")
    public ResponseEntity<AnalyseCompleteResponse> getAnalysesIa(@PathVariable Long importId) {
        return ResponseEntity.ok(dashboardAnalysteService.getAnalysesIa(getCurrentUserId(), importId));
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."))
                .getId();
    }
}