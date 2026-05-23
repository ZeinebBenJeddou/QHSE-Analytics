package com.QHSEAnalytics.kpi.controller;

import com.QHSEAnalytics.auth.service.SecurityUtils;
import com.QHSEAnalytics.shared.dto.request.MappingTemplateRequest;
import com.QHSEAnalytics.shared.dto.response.MappingTemplateResponse;
import com.QHSEAnalytics.kpi.service.MappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/mapping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALYSTE')")
public class MappingController {

    private final MappingService mappingService;
    private final SecurityUtils securityUtils;

    @PostMapping("/templates")
    public ResponseEntity<MappingTemplateResponse> saveTemplate(
            @Valid @RequestBody MappingTemplateRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        MappingTemplateResponse response = mappingService.saveTemplate(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<MappingTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(mappingService.getTemplates(securityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        mappingService.deleteTemplate(id, securityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
