package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.request.ChangePasswordRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateProfilRequest;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.ProfilResponse;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.auth.service.ProfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ProfilResponse> getProfil() {
        User user = getCurrentUser();
        return ResponseEntity.ok(profilService.getProfil(user.getId()));
    }

    @PutMapping
    public ResponseEntity<ProfilResponse> updateProfil(@Valid @RequestBody UpdateProfilRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(profilService.updateProfil(user.getId(), request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(profilService.changePassword(user.getId(), request));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));
    }
}