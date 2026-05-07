package com.QHSEAnalytics.importer.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.entity.ImportSession;
import com.QHSEAnalytics.shared.exception.ImportNotFoundException;
import com.QHSEAnalytics.shared.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.ImportSessionRepository;
import com.QHSEAnalytics.shared.repository.KpiAnalysisRepository;
import com.QHSEAnalytics.shared.repository.KpiImportPreviewRepository;
import com.QHSEAnalytics.shared.repository.KpiRawDataRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import com.QHSEAnalytics.shared.repository.StagingDonneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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
    private final KpiAnalysisRepository kpiAnalysisRepository;
    private final KpiImportPreviewRepository kpiImportPreviewRepository;
    private final KpiRawDataRepository kpiRawDataRepository;
    private final UserRepository userRepository;

    @DeleteMapping("/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteImport(@PathVariable Long importId) {
        User user = getCurrentUser();
        ImportSession session = importSessionRepository.findByIdAndUserId(importId, user.getId())
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));

        analyseCategorieRepository.deleteByImportSessionId(importId);
        analyseGlobaleRepository.deleteByImportSessionId(importId);
        resultatKpiRepository.deleteByImportSessionId(importId);
        kpiAnalysisRepository.deleteByImportSessionId(importId);
        stagingDonneeRepository.deleteByImportSessionId(importId);
        kpiImportPreviewRepository.deleteByImportSessionId(importId);
        kpiRawDataRepository.deleteByImportSessionId(importId);
        importSessionRepository.delete(session);

        return ResponseEntity.ok(Map.of("message", "Import supprimé avec succès."));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
