package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.analytics.service.AiConfigService;
import com.QHSEAnalytics.shared.entity.AiConfig;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ia/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    public ResponseEntity<List<AiConfig>> getAll() {
        return ResponseEntity.ok(aiConfigService.getAll());
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La valeur ne peut pas être vide."));
        }
        try {
            return ResponseEntity.ok(aiConfigService.update(key, value));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
