package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.AnalysteProfilDTO;
import com.QHSEAnalytics.auth.service.AnalysteProfilService;
import com.QHSEAnalytics.auth.service.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class AnalysteProfilController {

    private final AnalysteProfilService profilService;
    private final SecurityUtils securityUtils;

    @GetMapping("/qhse-context")
    public ResponseEntity<AnalysteProfilDTO> getProfil() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(profilService.getProfil(userId));
    }

    @PutMapping("/qhse-context")
    public ResponseEntity<AnalysteProfilDTO> saveProfil(@Valid @RequestBody AnalysteProfilDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(profilService.saveProfil(userId, dto));
    }
}
