package com.QHSEAnalytics.export.controller;

import com.QHSEAnalytics.auth.service.SecurityUtils;
import com.QHSEAnalytics.shared.dto.response.ExportResponse;
import com.QHSEAnalytics.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "Export", description = "Export PDF des rapports QHSE")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Télécharger le rapport PDF analyste", description = "Génère et télécharge le rapport PDF avec tableau comparatif et analyse IA")
    @GetMapping("/analyste/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<byte[]> exportAnalyste(@PathVariable Long importId) {
        ExportResponse exportResponse = exportService.exportAnalyste(securityUtils.getCurrentUserId(), importId);
        return buildPdfResponse(exportResponse);
    }

    @Operation(summary = "Télécharger le rapport PDF global admin", description = "Rapport consolidé tous analystes")
    @GetMapping("/admin/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAdmin() {
        ExportResponse exportResponse = exportService.exportAdmin();
        return buildPdfResponse(exportResponse);
    }

    private ResponseEntity<byte[]> buildPdfResponse(ExportResponse exportResponse) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(exportResponse.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(exportResponse.getContent(), headers, HttpStatus.OK);
    }
}
