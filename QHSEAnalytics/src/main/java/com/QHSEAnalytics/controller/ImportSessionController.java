package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import com.QHSEAnalytics.repository.StagingDonneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportSessionController {

    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final AnalyseCategorieRepository analyseCategorieRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final StagingDonneeRepository stagingDonneeRepository;
    private final UserRepository userRepository;

    @DeleteMapping("/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<Map<String, String>> deleteImport(@PathVariable Long importId) {
        User user = getCurrentUser();
        ImportSession session = importSessionRepository.findByIdAndUserId(importId, user.getId())
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));

        analyseCategorieRepository.deleteByImportSessionId(importId);
        analyseGlobaleRepository.deleteByImportSessionId(importId);
        resultatKpiRepository.deleteByImportSessionId(importId);
        stagingDonneeRepository.deleteByImportSessionId(importId);
        importSessionRepository.delete(session);

        return ResponseEntity.ok(Map.of("message", "Import supprimé avec succès."));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
