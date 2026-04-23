package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.request.SaveMappingRequest;
import com.QHSEAnalytics.dto.response.*;
import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final UserRepository userRepository;

    @GetMapping("/template/download")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = importService.downloadTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("template_qhse_v1.xlsx").build());

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping("/detect-columns")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<List<ColonneDetecteeResponse>> detectColumns(
            @RequestParam MultipartFile file,
            @RequestParam int ligneEntete
    ) {
        return ResponseEntity.ok(importService.detecterColonnes(file, ligneEntete));
    }

    @GetMapping("/mappings")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<List<UserMappingTemplateResponse>> getMappings() {
        User user = getCurrentUser();
        return ResponseEntity.ok(importService.getMappings(user.getId()));
    }

    @PostMapping("/mappings")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<UserMappingTemplateResponse> saveMapping(@Valid @RequestBody SaveMappingRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.saveMapping(user.getId(), request));
    }

    @DeleteMapping("/mappings/{mappingId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<MessageResponse> deleteMapping(@PathVariable Long mappingId) {
        User user = getCurrentUser();
        importService.deleteMapping(user.getId(), mappingId);
        return ResponseEntity.ok(new MessageResponse("Mapping supprimé."));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<ImportSessionResponse> upload(
            @RequestParam ImportMode mode,
            @RequestParam(required = false) Long mappingTemplateId,
            @RequestParam int periodeN1,
            @RequestParam int periodeN,
            @RequestParam MultipartFile file
    ) {
        User user = getCurrentUser();
        ImportSessionResponse response = importService.upload(user.getId(), mode, mappingTemplateId, periodeN1, periodeN, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload-and-process")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<ResultatGlobalResponse> uploadAndProcess(
            @RequestParam ImportMode mode,
            @RequestParam(required = false) Long mappingTemplateId,
            @RequestParam int periodeN1,
            @RequestParam int periodeN,
            @RequestParam MultipartFile file
    ) {
        User user = getCurrentUser();
        ResultatGlobalResponse response = importService.uploadAndConfirm(user.getId(), mode, mappingTemplateId, periodeN1, periodeN, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<List<ImportSessionResponse>> getHistorique() {
        User user = getCurrentUser();
        return ResponseEntity.ok(importService.getHistorique(user.getId(), isAdmin()));
    }

    @GetMapping("/{sessionId}/resultats")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYSTE')")
    public ResponseEntity<ResultatGlobalResponse> getResultats(@PathVariable Long sessionId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(importService.getResultats(user.getId(), isAdmin(), sessionId));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<MessageResponse> annuler(@PathVariable Long sessionId) {
        User user = getCurrentUser();
        importService.annuler(user.getId(), sessionId);
        return ResponseEntity.ok(new MessageResponse("Import annulé."));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
