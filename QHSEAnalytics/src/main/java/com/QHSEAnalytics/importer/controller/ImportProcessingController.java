package com.QHSEAnalytics.importer.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.request.ImportRequestDTO;
import com.QHSEAnalytics.shared.dto.response.ColumnProfileDTO;
import com.QHSEAnalytics.shared.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.shared.exception.ImportValidationException;
import com.QHSEAnalytics.importer.service.ImportProgressService;
import com.QHSEAnalytics.importer.service.processing.ColumnProfileService;
import com.QHSEAnalytics.importer.service.processing.ImportProcessingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import/manual")
@RequiredArgsConstructor
public class ImportProcessingController {

    private final ImportProcessingService importProcessingService;
    private final ImportProgressService importProgressService;
    private final ColumnProfileService columnProfileService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * SSE stream de progression.
     * Le frontend ouvre cette connexion AVANT de soumettre le fichier,
     * en passant le même clientId dans le multipart du POST.
     */
    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progressStream(@RequestParam("clientId") String clientId) {
        return importProgressService.register(clientId);
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<List<ColumnProfileDTO>> profileColumns(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(columnProfileService.profile(file));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<ImportProcessingResponse> previewImport(
            @RequestPart("file") MultipartFile file,
            @RequestPart("yearN") String yearN,
            @RequestPart("yearN1") String yearN1,
            @RequestPart("mapping") String mappingJson
    ) {
        int n = parseYear(yearN, "Année N");
        int n1 = parseYear(yearN1, "Année N-1");
        validateYearRange(n, n1);

        Map<String, Integer> mapping = parseMapping(mappingJson);
        ImportRequestDTO request = ImportRequestDTO.builder()
                .file(file)
                .yearN(n)
                .yearN1(n1)
                .mappingIndexes(mapping)
                .build();
        return ResponseEntity.ok(importProcessingService.previewImport(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<ImportProcessingResponse> processManualImport(
            @RequestPart("file") MultipartFile file,
            @RequestPart("yearN") String yearN,
            @RequestPart("yearN1") String yearN1,
            @RequestPart("mapping") String mappingJson,
            @RequestPart(value = "allowPartialImport", required = false) String allowPartialStr,
            @RequestParam(value = "clientId", required = false) String clientId
    ) {
        int n = parseYear(yearN, "Année N");
        int n1 = parseYear(yearN1, "Année N-1");
        validateYearRange(n, n1);

        boolean allowPartial = Boolean.parseBoolean(allowPartialStr);
        Map<String, Integer> mapping = parseMapping(mappingJson);
        User user = getCurrentUser();
        ImportRequestDTO request = ImportRequestDTO.builder()
                .file(file)
                .yearN(n)
                .yearN1(n1)
                .mappingIndexes(mapping)
                .allowPartialImport(allowPartial)
                .clientId(clientId)
                .build();
        return ResponseEntity.ok(importProcessingService.processManualImport(request, user));
    }

    private Map<String, Integer> parseMapping(String mappingJson) {
        if (mappingJson == null || mappingJson.isBlank()) {
            throw new ImportValidationException("Le mapping des colonnes est requis.");
        }
        try {
            return objectMapper.readValue(mappingJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception ex) {
            throw new ImportValidationException("Le mapping des colonnes est invalide.");
        }
    }

    private int parseYear(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ImportValidationException(fieldName + " est requis.");
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            throw new ImportValidationException(fieldName + " doit être une année valide.");
        }
    }

    private void validateYearRange(int yearN, int yearNMinus1) {
        if (yearNMinus1 != yearN - 1) {
            throw new ImportValidationException("L'année N-1 doit être exactement l'année N moins 1.");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
