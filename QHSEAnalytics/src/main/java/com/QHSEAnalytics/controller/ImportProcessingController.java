package com.QHSEAnalytics.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.request.ImportRequestDTO;
import com.QHSEAnalytics.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.exception.ImportValidationException;
import com.QHSEAnalytics.service.processing.ImportProcessingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/import/manual")
@RequiredArgsConstructor
public class ImportProcessingController {

    private final ImportProcessingService importProcessingService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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
            @RequestPart(value = "allowPartialImport", required = false) String allowPartialStr
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
