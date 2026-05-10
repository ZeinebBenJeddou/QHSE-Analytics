package com.QHSEAnalytics.analytics.controller;

import com.QHSEAnalytics.analytics.service.RagAdminService;
import com.QHSEAnalytics.shared.dto.request.RagKnowledgeRequest;
import com.QHSEAnalytics.shared.dto.request.RagSearchTestRequest;
import com.QHSEAnalytics.shared.dto.response.RagKnowledgeResponse;
import com.QHSEAnalytics.shared.dto.response.RagSearchTestResultItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RagAdminController {

    private final RagAdminService ragAdminService;

    @GetMapping
    public ResponseEntity<List<RagKnowledgeResponse>> getAll() {
        return ResponseEntity.ok(ragAdminService.getAll());
    }

    @PostMapping
    public ResponseEntity<RagKnowledgeResponse> create(@Valid @RequestBody RagKnowledgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ragAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RagKnowledgeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RagKnowledgeRequest request) {
        return ResponseEntity.ok(ragAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ragAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<List<RagSearchTestResultItem>> testSearch(
            @Valid @RequestBody RagSearchTestRequest request) {
        return ResponseEntity.ok(ragAdminService.testSearch(request));
    }
}
