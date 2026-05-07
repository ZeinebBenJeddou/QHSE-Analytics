package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStatusResponse;

import com.QHSEAnalytics.shared.enums.AnalyseStatus;
import com.QHSEAnalytics.analytics.service.AnalyseIaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalyseIaService analyseIaService;
    private final UserRepository userRepository;

    @PostMapping("/{importId}/ai")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<AiAnalysisStatusResponse> runAi(@PathVariable Long importId) {
        User user = getCurrentUser();
        analyseIaService.triggerAnalyseAsync(importId, user.getId());
        return ResponseEntity.accepted()
                .body(AiAnalysisStatusResponse.builder()
                        .status(AnalyseStatus.PROCESSING)
                        .message("Analyse IA lancée et traitée en arrière-plan.")
                        .build());
    }

    @PostMapping("/run-ai/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<AiAnalysisStatusResponse> runAiLegacy(@PathVariable Long importId) {
        User user = getCurrentUser();
        analyseIaService.triggerAnalyseAsync(importId, user.getId());
        return ResponseEntity.accepted()
                .body(AiAnalysisStatusResponse.builder()
                        .status(AnalyseStatus.PROCESSING)
                        .message("Analyse IA lancée et traitée en arrière-plan.")
                        .build());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
