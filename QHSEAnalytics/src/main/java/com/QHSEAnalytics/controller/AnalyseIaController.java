package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.service.AnalyseIaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class AnalyseIaController {

    private final AnalyseIaService analyseIaService;
    private final UserRepository userRepository;

    @GetMapping("/{importId}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<AnalyseCompleteResponse> getAnalyseComplete(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalyseComplete(importId, user.getId(), isAdmin()));
    }

    @GetMapping("/{importId}/categories")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<List<AnalyseCategorieResponse>> getAnalysesCategories(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalysesCategories(importId, user.getId(), isAdmin()));
    }

    @GetMapping("/{importId}/globale")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<AnalyseGlobaleResponse> getAnalyseGlobale(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalyseGlobale(importId, user.getId(), isAdmin()));
    }

    @GetMapping("/{importId}/structured")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<AiAnalysisStructuredResponse> getAnalyseStructured(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.getAnalyseStructured(importId, user.getId(), isAdmin()));
    }

    @PostMapping("/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<AnalyseCompleteResponse> lancerAnalyse(@PathVariable Long importId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(analyseIaService.regenerer(importId, user.getId()));
    }

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