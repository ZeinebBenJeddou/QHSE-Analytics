package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.dto.request.ImportRequestDTO;
import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.ImportProcessingResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.KpiRawData;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.QualityStatus;
import com.QHSEAnalytics.enums.Tendance;
import com.QHSEAnalytics.exception.ImportTransitionException;
import com.QHSEAnalytics.exception.ImportValidationException;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiRawDataRepository;
import com.QHSEAnalytics.repository.KpiRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportProcessingService {

    private final KpiProcessingOrchestratorService orchestrator;
    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final KpiRawDataRepository kpiRawDataRepository;
    private final KpiRepository kpiRepository;

    @Transactional
    public ImportProcessingResponse processManualImport(ImportRequestDTO request, User user) {
        validateYearInputs(request.getYearN(), request.getYearN1());
        ImportSession session = buildImportSession(request.getFile(), user, request.getYearN(), request.getYearN1());
        ImportSession initialSession = importSessionRepository.save(session);
        ImportSession processingSession = advanceStatus(initialSession, ImportStatut.processing());

        ImportProcessingResponse processingResponse = orchestrator.process(request.getFile(), request.getMappingIndexes());
        String analyseIaGlobale = processingResponse.getAnalyseIa();
        persistRawRows(processingSession, processingResponse.getRawData());
        List<ResultatKpi> results = processingResponse.getCalculatedData().stream()
                .map(dto -> mapToResultatKpi(dto, processingSession, user, analyseIaGlobale))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ImportSession finalSession;
        if (results.isEmpty()) {
            finalSession = advanceStatus(processingSession, ImportStatut.ERREUR);
            finalSession.setMessageErreur("Aucun KPI valide n'a pu être traité.");
            importSessionRepository.save(finalSession);
        } else {
            ImportSession calculatedSession = advanceStatus(processingSession, ImportStatut.CALCULATED);
            resultatKpiRepository.saveAll(results);
            finalSession = advanceStatus(calculatedSession, ImportStatut.READY_FOR_AI);
        }

        return ImportProcessingResponse.builder()
                .importSessionId(finalSession.getId())
                .calculatedData(processingResponse.getCalculatedData())
                .rawData(processingResponse.getRawData())
                .extractionMethod(processingResponse.getExtractionMethod())
                .qualityScore(processingResponse.getQualityScore())
                .detectedHeaders(processingResponse.getDetectedHeaders())
                .charts(processingResponse.getCharts())
                .analyseIa(analyseIaGlobale)
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

    private ResultatKpi mapToResultatKpi(KpiCalculatedDTO dto, ImportSession session, User user, String analyseIa) {
        if (dto.getMatchedKpiId() == null) {
            return null;
        }

        Kpi kpi = kpiRepository.findById(dto.getMatchedKpiId()).orElse(null);
        if (kpi == null) {
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
                .analyseIa(analyseIa)
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
}
