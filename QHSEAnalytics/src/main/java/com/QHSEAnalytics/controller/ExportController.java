package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.ExportResponse;
import com.QHSEAnalytics.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;
    private final UserRepository userRepository;

    @GetMapping("/analyste/{importId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<byte[]> exportAnalyste(@PathVariable Long importId) {
        ExportResponse exportResponse = exportService.exportAnalyste(getCurrentUserId(), importId);
        return buildPdfResponse(exportResponse);
    }

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

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."))
                .getId();
    }
}
