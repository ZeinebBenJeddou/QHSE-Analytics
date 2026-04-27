package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.request.MappingTemplateRequest;
import com.QHSEAnalytics.dto.response.MappingTemplateResponse;
import com.QHSEAnalytics.service.MappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 2 – Mapping Agent REST Controller.
 *
 * Endpoints:
 *   POST   /api/mapping/templates           – save / replace a template
 *   GET    /api/mapping/templates           – list templates for current user
 *   DELETE /api/mapping/templates/{id}      – delete a template
 */
@RestController
@RequestMapping("/api/mapping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class MappingController {

    private final MappingService mappingService;
    private final UserRepository userRepository;

    @PostMapping("/templates")
    public ResponseEntity<MappingTemplateResponse> saveTemplate(
            @Valid @RequestBody MappingTemplateRequest request) {
        Long userId = getCurrentUserId();
        MappingTemplateResponse response = mappingService.saveTemplate(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<MappingTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(mappingService.getTemplates(getCurrentUserId()));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        mappingService.deleteTemplate(id, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"))
                .getId();
    }
}
