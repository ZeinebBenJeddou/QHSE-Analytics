package com.QHSEAnalytics.importer.controller;

import com.QHSEAnalytics.auth.service.SecurityUtils;
import com.QHSEAnalytics.importer.service.ImportProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportSessionController {

    private final ImportProcessingService importProcessingService;
    private final SecurityUtils securityUtils;

    @DeleteMapping("/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<Map<String, String>> deleteImport(@PathVariable Long importId) {
        importProcessingService.deleteImport(importId, securityUtils.getCurrentUser().getId());
        return ResponseEntity.ok(Map.of("message", "Import supprimé avec succès."));
    }
}
