package com.QHSEAnalytics.importer.controller;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.request.ImportRequestDTO;
import com.QHSEAnalytics.shared.dto.response.ColumnProfileDTO;
// import com.QHSEAnalytics.shared.dto.response.DualFileProfileResponse; // DUAL disabled
import com.QHSEAnalytics.shared.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.shared.exception.ImportValidationException;
import com.QHSEAnalytics.importer.service.ImportProgressService;
import com.QHSEAnalytics.importer.service.processing.ColumnProfileService;
import com.QHSEAnalytics.importer.service.ImportProcessingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Import", description = "Gestion de l'import des données QHSE ")
@RestController
@RequestMapping("/api/import/manual")
@RequiredArgsConstructor
public class ImportProcessingController {

    private final ImportProcessingService importProcessingService;
    private final ImportProgressService importProgressService;
    private final ColumnProfileService columnProfileService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progressStream(@RequestParam("clientId") String clientId) {
        return importProgressService.register(clientId);
    }

    @Operation(summary = "Analyser les colonnes d'un fichier Excel", description = "Retourne le profil de chaque colonne (type, sémantique, statistiques)")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<List<ColumnProfileDTO>> profileColumns(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(columnProfileService.profile(file));
    }

    @Operation(summary = "Prévisualiser l'import sans persister", description = "Retourne le rapport qualité et les KPIs calculés sans sauvegarde")
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

    @Operation(summary = "Confirmer et exécuter l'import", description = "Pipeline complet : extraction, calcul, classification, analyse IA")
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

    /* DUAL mode — disabled
    @Operation(summary = "Prévisualiser un import dual", description = "Fusionne les deux fichiers et retourne la prévisualisation")
    @PostMapping(value = "/dual/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE') or hasRole('ADMIN')")
    public ResponseEntity<ImportProcessingResponse> previewDualFile(
            @RequestPart("fileN1")  MultipartFile fileN1,
            @RequestPart("fileN")   MultipartFile fileN,
            @RequestParam("yearN1") int yearN1,
            @RequestParam("yearN")  int yearN,
            @RequestParam("kpiColN1") int kpiColN1,
            @RequestParam("valColN1") int valColN1,
            @RequestParam("kpiColN")  int kpiColN,
            @RequestParam("valColN")  int valColN
    ) {
        validateYearRange(yearN, yearN1);
        Map<String, Integer> mappingN1 = Map.of("kpiNameIndex", kpiColN1, "valueIndex", valColN1);
        Map<String, Integer> mappingN  = Map.of("kpiNameIndex", kpiColN,  "valueIndex", valColN);
        return ResponseEntity.ok(
                importProcessingService.previewDualFileImport(fileN1, fileN, yearN1, yearN, mappingN1, mappingN)
        );
    }

    @Operation(summary = "Profiler deux fichiers séparés", description = "Retourne les colonnes de chaque fichier pour le mapping dual")
    @PostMapping(value = "/dual/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<DualFileProfileResponse> profileDualFiles(
            @RequestPart("fileN1") MultipartFile fileN1,
            @RequestPart("fileN")  MultipartFile fileN
    ) {
        DualFileProfileResponse response = DualFileProfileResponse.builder()
                .columnsN1(columnProfileService.profile(fileN1))
                .columnsN(columnProfileService.profile(fileN))
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirmer un import dual", description = "Import avec deux fichiers séparés (un par année)")
    @PostMapping(value = "/dual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYSTE')")
    public ResponseEntity<ImportProcessingResponse> importDualFile(
            @RequestPart("fileN1")  MultipartFile fileN1,
            @RequestPart("fileN")   MultipartFile fileN,
            @RequestParam("yearN1") int yearN1,
            @RequestParam("yearN")  int yearN,
            @RequestParam("kpiColN1") int kpiColN1,
            @RequestParam("valColN1") int valColN1,
            @RequestParam("kpiColN")  int kpiColN,
            @RequestParam("valColN")  int valColN,
            @RequestParam(value = "allowPartial", defaultValue = "false") boolean allowPartial,
            @RequestParam(value = "clientId", required = false, defaultValue = "") String clientId
    ) {
        validateYearRange(yearN, yearN1);
        Map<String, Integer> mappingN1 = Map.of("kpiNameIndex", kpiColN1, "valueIndex", valColN1);
        Map<String, Integer> mappingN  = Map.of("kpiNameIndex", kpiColN,  "valueIndex", valColN);
        User user = getCurrentUser();
        return ResponseEntity.ok(
                importProcessingService.processDualFileImport(
                        fileN1, fileN, yearN1, yearN, mappingN1, mappingN, allowPartial, clientId, user)
        );
    }
    */

    private Map<String, Integer> parseMapping(String mappingJson) {
        if (mappingJson == null || mappingJson.isBlank()) {
            throw new ImportValidationException("Le mapping des colonnes est requis.");
        }
        Map<String, Integer> mapping;
        try {
            mapping = objectMapper.readValue(mappingJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception ex) {
            throw new ImportValidationException("Le mapping des colonnes est invalide.");
        }
        long distinctIndexes = mapping.values().stream().filter(v -> v != null && v >= 0).distinct().count();
        if (distinctIndexes < mapping.values().stream().filter(v -> v != null && v >= 0).count()) {
            throw new ImportValidationException(
                "Mapping invalide : deux champs différents pointent vers la même colonne.");
        }
        return mapping;
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
