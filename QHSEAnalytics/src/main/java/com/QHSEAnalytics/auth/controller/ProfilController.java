package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.request.ChangePasswordRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateProfilRequest;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.ProfilResponse;
import com.QHSEAnalytics.auth.service.ProfilService;
import com.QHSEAnalytics.auth.service.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
public class ProfilController {

    private final ProfilService profilService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ProfilResponse> getProfil() {
        return ResponseEntity.ok(profilService.getProfil(securityUtils.getCurrentUserId()));
    }

    @PutMapping
    public ResponseEntity<ProfilResponse> updateProfil(@Valid @RequestBody UpdateProfilRequest request) {
        return ResponseEntity.ok(profilService.updateProfil(securityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(profilService.changePassword(securityUtils.getCurrentUserId(), request));
    }
}