package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.dto.request.ImportRequestDTO;
import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.dto.response.ImportQualityReport;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.entity.CategorieKpi;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.KpiRawData;
import com.QHSEAnalytics.entity.RagKnowledge;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.QualityStatus;
import com.QHSEAnalytics.enums.Tendance;
import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.exception.ImportTransitionException;
import com.QHSEAnalytics.exception.ImportValidationException;
import com.QHSEAnalytics.repository.CategorieKpiRepository;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiAnalysisRepository;
import com.QHSEAnalytics.repository.KpiImportPreviewRepository;
import com.QHSEAnalytics.repository.KpiRawDataRepository;
import com.QHSEAnalytics.repository.KpiRepository;
import com.QHSEAnalytics.repository.RagKnowledgeRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportProcessingService {

    private final KpiProcessingOrchestratorService orchestrator;
    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final KpiImportPreviewRepository kpiImportPreviewRepository;
    private final KpiRawDataRepository kpiRawDataRepository;
    private final KpiRepository kpiRepository;
    private final KpiAnalysisRepository kpiAnalysisRepository;
    private final CategorieKpiRepository categorieKpiRepository;
    private final RagKnowledgeRepository ragKnowledgeRepository;

    @Transactional
    public ImportProcessingResponse processManualImport(ImportRequestDTO request, User user) {
        validateYearInputs(request.getYearN(), request.getYearN1());
        ImportSession session = buildImportSession(request.getFile(), user, request.getYearN(), request.getYearN1());
        ImportSession initialSession = importSessionRepository.save(session);
        ImportSession processingSession = advanceStatus(initialSession, ImportStatut.processing());

        ImportProcessingResponse processingResponse = orchestrator.process(
                request.getFile(), request.getMappingIndexes(),
                Boolean.TRUE.equals(request.getAllowPartialImport()));
        ImportQualityReport qualityReport = processingResponse.getQualityReport();
        boolean allowPartial = Boolean.TRUE.equals(request.getAllowPartialImport());

        // Si le rapport qualité est bloquant, ne pas persister et retourner une erreur métier claire
        if (qualityReport != null && qualityReport.isBlocking()) {
            log.warn("[ImportProcessing] Import bloqué ({}) pour session {}, {} erreur(s)",
                    allowPartial ? "hardBlocking" : "blocking",
                    processingSession.getId(),
                    qualityReport.getErrors().size());
            ImportSession erreurSession = advanceStatus(processingSession, ImportStatut.ERREUR);
            String reason = qualityReport.getBlockingReason() != null
                    ? qualityReport.getBlockingReason()
                    : "Import refusé : des erreurs bloquantes ont été détectées. Consultez le rapport qualité.";
            erreurSession.setMessageErreur(reason);
            importSessionRepository.save(erreurSession);

            return ImportProcessingResponse.builder()
                    .importSessionId(erreurSession.getId())
                    .rawData(processingResponse.getRawData())
                    .calculatedData(processingResponse.getCalculatedData())
                    .extractionMethod(processingResponse.getExtractionMethod())
                    .qualityScore(qualityReport.getQualityScore())
                    .detectedHeaders(processingResponse.getDetectedHeaders())
                    .qualityReport(qualityReport)
                    .build();
        }

        // --- Filtrage en mode PARTIAL ---
        List<com.QHSEAnalytics.dto.request.KpiRawDataDTO> rawToProcess = processingResponse.getRawData();
        List<com.QHSEAnalytics.dto.response.KpiCalculatedDTO> calcToProcess = processingResponse.getCalculatedData();

        if (allowPartial && qualityReport != null && qualityReport.isSoftBlocking()) {
            java.util.Set<Integer> rejectedIndexes = qualityReport.getRejectedRowIndexes() == null
                    ? java.util.Collections.emptySet()
                    : new java.util.HashSet<>(qualityReport.getRejectedRowIndexes());

            rawToProcess = processingResponse.getRawData() == null ? List.of() :
                    processingResponse.getRawData().stream()
                            .filter(r -> r.isValid() && !rejectedIndexes.contains(r.getRowIndex()))
                            .collect(Collectors.toList());

            if (calcToProcess != null && !calcToProcess.isEmpty()) {
                java.util.Set<String> validKpiNames = rawToProcess.stream()
                        .map(com.QHSEAnalytics.dto.request.KpiRawDataDTO::getKpiName)
                        .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
                calcToProcess = calcToProcess.stream()
                        .filter(k -> validKpiNames.contains(k.getKpiName()))
                        .collect(Collectors.toList());
            }
            log.info("[ImportProcessing] Mode PARTIAL: {} valides, {} rejetées pour session {}",
                    rawToProcess.size(), rejectedIndexes.size(), processingSession.getId());
        }

        String analyseIaGlobale = processingResponse.getAnalyseIa();
        persistRawRows(processingSession, rawToProcess);
        persistPreviewRows(processingSession, calcToProcess);

        List<ResultatKpi> results = calcToProcess.stream()
                .map(dto -> mapToResultatKpi(dto, processingSession, user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ImportSession finalSession;
        if (results.isEmpty()) {
            if (allowPartial && qualityReport != null && qualityReport.isSoftBlocking()) {
                // Partial import with all rows rejected
                ImportSession erreurSession = advanceStatus(processingSession, ImportStatut.ERREUR);
                erreurSession.setMessageErreur("Import partiel : aucune ligne valide n'a pu être importée.");
                importSessionRepository.save(erreurSession);
                finalSession = erreurSession;
            } else {
                finalSession = advanceStatus(processingSession, ImportStatut.ERREUR);
                finalSession.setMessageErreur("Aucun KPI valide n'a pu être traité.");
                importSessionRepository.save(finalSession);
            }
        } else {
            ImportSession calculatedSession = advanceStatus(processingSession, ImportStatut.CALCULATED);
            resultatKpiRepository.saveAll(results);

            if (processingResponse.getAiResponse() != null && processingResponse.getAiResponse().getKpis() != null) {
                persistKpiAnalysis(processingSession, processingResponse.getAiResponse());
            }

            finalSession = advanceStatus(calculatedSession, ImportStatut.READY_FOR_AI);
            // Renseigner le message de session si import partiel
            if (allowPartial && qualityReport != null && qualityReport.isSoftBlocking()) {
                int imported = rawToProcess.size();
                int rejected = qualityReport.getRejectedRowsCount();
                finalSession.setMessageErreur(String.format("Import partiel: %d importées, %d rejetées.", imported, rejected));
                importSessionRepository.save(finalSession);
                log.info("[ImportProcessing] Import partiel terminé: {} importées, {} rejetées, session {}",
                        imported, rejected, finalSession.getId());
            }
        }

        return ImportProcessingResponse.builder()
                .importSessionId(finalSession.getId())
                .calculatedData(calcToProcess)
                .rawData(rawToProcess)
                .extractionMethod(processingResponse.getExtractionMethod())
                .qualityScore(processingResponse.getQualityScore())
                .detectedHeaders(processingResponse.getDetectedHeaders())
                .charts(processingResponse.getCharts())
                .analyseIa(analyseIaGlobale)
                .qualityReport(qualityReport)
                .build();
    }

    public ImportProcessingResponse previewImport(ImportRequestDTO request) {
        validateYearInputs(request.getYearN(), request.getYearN1());
        return orchestrator.preview(request.getFile(), request.getMappingIndexes());
    }

    private ImportSession buildImportSession(MultipartFile file, User user, int yearN, int yearNMinus1) {
        return ImportSession.builder()
                .user(user)
                .mode(ImportMode.MANUAL)
                .nomFichier(file.getOriginalFilename())
                .templateVersion(null)
                .fileContent(null)
                .periodeN1(yearNMinus1)
                .periodeN(yearN)
                .statut(ImportStatut.initial())
                .messageErreur(null)
                .build();
    }

    private void validateYearInputs(int yearN, int yearNMinus1) {
        if (yearN < 1900 || yearN > 2100) {
            throw new ImportValidationException("Année N doit être comprise entre 1900 et 2100.");
        }
        if (yearNMinus1 < 1900 || yearNMinus1 > 2100) {
            throw new ImportValidationException("Année N-1 doit être comprise entre 1900 et 2100.");
        }
        if (yearNMinus1 != yearN - 1) {
            throw new ImportValidationException("L'année N-1 doit être exactement l'année N moins 1.");
        }
    }

    private ImportSession advanceStatus(ImportSession session, ImportStatut nextStatut) {
        ImportStatut currentStatut = session.getStatut();
        if (!currentStatut.canTransitionTo(nextStatut)) {
            String message = String.format("Transition de statut invalide pour l'import %s : %s -> %s",
                    session.getId(), currentStatut, nextStatut);
            log.warn(message);
            throw new ImportTransitionException(message);
        }
        session.setStatut(nextStatut);
        ImportSession savedSession = importSessionRepository.save(session);
        log.info("ImportSession {} statut mis à jour : {} -> {}", savedSession.getId(), currentStatut, nextStatut);
        return savedSession;
    }

    private void persistKpiAnalysis(ImportSession session, com.QHSEAnalytics.dto.llm.AiResponse aiResponse) {
        if (aiResponse == null || aiResponse.getKpis() == null || aiResponse.getKpis().isEmpty()) {
            log.warn("[ImportProcessing] persistKpiAnalysis called with null/empty aiResponse for import {}", session.getId());
            return;
        }
        
        List<com.QHSEAnalytics.entity.KpiAnalysis> analyses = aiResponse.getKpis().stream()
                .map(insight -> {
                    String aiNote = firstNonBlank(firstNonBlank(insight.getNoteFinale(), insight.getAiNote()), insight.getInsight());
                    log.debug("[ImportProcessing] Creating KpiAnalysis: name='{}', aiNote='{}', identificationRisque='{}'", 
                        insight.getName(), 
                        aiNote != null ? aiNote.substring(0, Math.min(40, aiNote.length())) : "null",
                        insight.getIdentificationRisque());
                    
                    return com.QHSEAnalytics.entity.KpiAnalysis.builder()
                        .importSession(session)
                        .kpiName(insight.getName())
                        .riskLevel("Modéré")
                        .riskJustification(firstNonBlank(insight.getIdentificationRisque(), insight.getRiskJustification()))
                        .identificationRisque(insight.getIdentificationRisque())
                        .problemeDetecte(firstNonBlank(insight.getProblemeDetecte(), insight.getIssueDetected()))
                        .actionsPreventives(firstNonBlank(insight.getActionsPreventives(), insight.getPreventiveAction()))
                        .actionImmediate(insight.getActionImmediate())
                        .prioriteAction(insight.getPrioriteAction())
                        .methode8D(insight.getMethode8D())
                        .aiNote(aiNote)
                        .noteFinale(firstNonBlank(insight.getNoteFinale(), aiNote))
                        .requires8d(false)
                        .build();
                })
                .collect(Collectors.toList());
        
        kpiAnalysisRepository.saveAll(analyses);
        log.info("[ImportProcessing] Persisted {} KpiAnalysis records for import {}", analyses.size(), session.getId());
    }

    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private void persistPreviewRows(ImportSession session, List<KpiCalculatedDTO> data) {
        if (data == null || data.isEmpty()) return;

        List<com.QHSEAnalytics.entity.KpiImportPreview> previews = data.stream()
                .map(dto -> com.QHSEAnalytics.entity.KpiImportPreview.builder()
                        .importSession(session)
                        .kpiName(dto.getKpiName())
                        .category(dto.getCategorie())
                        .unit(dto.getUnite())
                        .definition(dto.getDefinition())
                        .valueN(dto.getValeurN())
                        .valueN1(dto.getValeurN1())
                        .status(dto.getStatus())
                        .commentaire(dto.getCommentaire())
                        .variationPercent(dto.getVariationPercentage())
                        .ecart(dto.getVariationAbsolute())
                        .build())
                .collect(Collectors.toList());
        kpiImportPreviewRepository.saveAll(previews);
        log.info("Enregistré {} lignes de prévisualisation pour l'import {}", previews.size(), session.getId());
    }

    private void persistRawRows(ImportSession session, List<KpiRawDataDTO> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return;
        }

        List<KpiRawData> entities = rawData.stream()
                .map(dto -> KpiRawData.builder()
                        .importSession(session)
                        .kpiNom(dto.getKpiName())
                        .valeurN1(dto.getValeurN1Raw())
                        .valeurN(dto.getValeurNRaw())
                        .methodeExtraction(dto.getMethodeExtraction())
                        .scoreConfiance(dto.getScoreConfiance())
                        .ligneFichier(dto.getRowIndex())
                        .build())
                .toList();
        kpiRawDataRepository.saveAll(entities);
    }

    private ResultatKpi mapToResultatKpi(KpiCalculatedDTO dto, ImportSession session, User user) {
        // Try to get matched KPI, or auto-create if not found
        Kpi kpi;
        if (dto.getMatchedKpiId() != null) {
            kpi = kpiRepository.findById(dto.getMatchedKpiId()).orElse(null);
        } else {
            // Auto-create KPI if not matched
            kpi = createOrGetKpi(dto);
        }

        if (kpi == null) {
            log.warn("[ImportProcessing] Could not create KPI for '{}' in category '{}'", dto.getKpiName(), dto.getCategorieCode());
            return null;
        }

        return ResultatKpi.builder()
                .importSession(session)
                .user(user)
                .kpi(kpi)
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .valeurN1(dto.getValeurN1() == null ? 0d : dto.getValeurN1())
                .valeurN(dto.getValeurN() == null ? 0d : dto.getValeurN())
                .variationAbsolue(dto.getVariationAbsolute() == null ? 0d : dto.getVariationAbsolute())
                .variationRelative(dto.getVariationPercentage() == null ? 0d : dto.getVariationPercentage())
                .niveauVariation(parseNiveau(dto.getClassification()))
                .tendance(parseTendance(dto.getTendance()))
                .confidenceScore(1.0d)
                .qualityStatus(QualityStatus.OK)
                .status(dto.getStatus())
                .commentaire(dto.getCommentaire())
                .analyseIa(null)
                .build();
    }

    private NiveauVariation parseNiveau(String classification) {
        if (classification == null) {
            return NiveauVariation.FAIBLE;
        }
        try {
            return NiveauVariation.valueOf(classification);
        } catch (IllegalArgumentException ex) {
            return NiveauVariation.FAIBLE;
        }
    }

    private Tendance parseTendance(String tendance) {
        if (tendance == null) {
            return Tendance.STABLE;
        }
        try {
            return Tendance.valueOf(tendance);
        } catch (IllegalArgumentException ex) {
            return Tendance.STABLE;
        }
    }

    /**
     * Auto-create a KPI if it doesn't exist in the database.
     * Also creates corresponding RagKnowledge entry for RAG system enrichment.
     */
    private Kpi createOrGetKpi(KpiCalculatedDTO dto) {
        String kpiName = dto.getKpiName();
        if (kpiName == null || kpiName.isBlank()) {
            return null;
        }

        // Check if KPI already exists by name
        Optional<Kpi> existing = kpiRepository.findByNom(kpiName);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            // Determine category - use categorieCode if available, otherwise default to "AUTO" category
            CategorieKpi categorie = null;
            String categorieCode = dto.getCategorieCode();
            
            if (categorieCode != null && !categorieCode.equals("AUTO")) {
                categorie = categorieKpiRepository.findByCode(categorieCode).orElse(null);
            }
            
            // Fallback to a default category if not found
            if (categorie == null) {
                categorie = categorieKpiRepository.findByCode("Q").orElse(null);
            }

            if (categorie == null) {
                log.warn("[KPI Creation] No category found for KPI '{}', skipping creation", kpiName);
                return null;
            }

            // Create new Kpi
            Kpi newKpi = Kpi.builder()
                    .nom(kpiName)
                    .definition("KPI auto-créé à partir d'un import. Catégorie détectée: " + dto.getCategorie())
                    .unite(parseUnite(dto.getUnite()))
                    .categorieKpi(categorie)
                    .seuilFaible(10.0)
                    .seuilModere(25.0)
                    .seuilCritique(50.0)
                    .ordre(999) // Auto-created KPIs get high order number
                    .isActive(true)
                    .build();

            Kpi savedKpi = kpiRepository.save(newKpi);
            log.info("[KPI Creation] Auto-created KPI: name='{}', category='{}', id={}", 
                kpiName, categorie.getCode(), savedKpi.getId());

            // Create corresponding RagKnowledge entry for RAG system
            createRagKnowledgeForKpi(savedKpi, categorie);

            return savedKpi;
        } catch (Exception ex) {
            log.error("[KPI Creation] Error auto-creating KPI '{}': {}", kpiName, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Create or update RagKnowledge entry for a newly created KPI.
     */
    private void createRagKnowledgeForKpi(Kpi kpi, CategorieKpi categorie) {
        try {
            // Check if RagKnowledge already exists
            if (ragKnowledgeRepository.findByKpiName(kpi.getNom()).isPresent()) {
                return;
            }

            RagKnowledge ragKnowledge = RagKnowledge.builder()
                    .kpiName(kpi.getNom())
                    .definition(kpi.getDefinition())
                    .category(categorie.getCode())
                    .thresholds(buildThresholdJson(kpi))
                    .build();

            ragKnowledgeRepository.save(ragKnowledge);
            log.info("[RAG Knowledge] Created RAG knowledge for KPI: {}", kpi.getNom());
        } catch (Exception ex) {
            log.error("[RAG Knowledge] Error creating RAG knowledge for KPI '{}': {}", kpi.getNom(), ex.getMessage());
            // Don't fail the import if RAG creation fails
        }
    }

    /**
     * Build a JSON string representing KPI thresholds.
     */
    private String buildThresholdJson(Kpi kpi) {
        return String.format(Locale.US,
            "{\"faible\":%f, \"modere\":%f, \"critique\":%f}",
            kpi.getSeuilFaible(),
            kpi.getSeuilModere(),
            kpi.getSeuilCritique()
        );
    }

    /**
     * Parse UniteKpi from string representation.
     */
    private UniteKpi parseUnite(String unite) {
        if (unite == null || unite.isBlank()) {
            return UniteKpi.NOMBRE;
        }
        try {
            return UniteKpi.valueOf(unite);
        } catch (IllegalArgumentException ex) {
            return UniteKpi.NOMBRE;
        }
    }
}
