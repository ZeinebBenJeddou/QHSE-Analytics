package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.analytics.service.AnalyseIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analyse IA", description = "Génération et consultation des analyses IA (Groq)")
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class AnalyseIaController {

    private final AnalyseIaService analyseIaService;
    private final UserRepository userRepository;

    @Operation(summary = "Analyse IA complète d'un import", description = "Synthèse, insights par KPI, causes probables, plan d'actions")
    @GetMapping("/{importId}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<AnalyseCompleteResponse> getAnalyseComplete(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalyseComplete(importId, user.getId(), isAdmin()));
    }

    @Operation(summary = "Analyse IA structurée JSON", description = "Réponse complète avec confidence, rootCauseAnalysis, predictiveAlerts")
    @GetMapping("/{importId}/structured")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<AiAnalysisStructuredResponse> getAnalyseStructured(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalyseStructured(importId, user.getId(), isAdmin()));
    }

    @Operation(summary = "Régénérer l'analyse IA", description = "Force une nouvelle analyse en bypassant le cache")
    @PostMapping("/{importId}/regenerer")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<AnalyseCompleteResponse> regenerer(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.regenerer(importId, user.getId()));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}