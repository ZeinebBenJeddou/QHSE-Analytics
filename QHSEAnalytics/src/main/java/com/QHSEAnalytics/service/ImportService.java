package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.*;
import com.QHSEAnalytics.entity.*;
import com.QHSEAnalytics.enums.*;
import com.QHSEAnalytics.exception.*;
import com.QHSEAnalytics.importengine.AutoImportEngine;
import com.QHSEAnalytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final TemplateExcelService templateExcelService;
    private final AutoImportEngine autoImportEngine;
    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;

    @Transactional(readOnly = true)
    public byte[] downloadTemplate() {
        return templateExcelService.generateTemplate();
    }


    @Transactional
    public ImportSessionResponse upload(
            Long userId,
            int periodeN1,
            int periodeN,
            MultipartFile file
    ) {
        log.info("Upload automatique démarré userId={} fichier={}", userId, file.getOriginalFilename());
        validateUpload(periodeN1, periodeN, file);

        AutoImportEngine.AutoImportResult result = autoImportEngine.process(file, userId, periodeN1, periodeN);
        log.info("Import automatique traité sessionId={} userId={} resultats={} rejets={}",
                result.session().getId(), userId, result.records().size(), result.rejectedCount());

        return toImportSessionResponse(result.session(), result.records().size(), result.rejectedCount());
    }

    @Transactional
    public AutoImportResultResponse uploadAuto(Long userId, int periodeN1, int periodeN, MultipartFile file) {
        log.info("Import automatique full pipeline démarré userId={} fichier={}", userId, file.getOriginalFilename());
        validateUpload(periodeN1, periodeN, file);

        AutoImportEngine.AutoImportResult result = autoImportEngine.process(file, userId, periodeN1, periodeN);
        log.info("Import automatique complet sessionId={} userId={} resultats={} rejets={}",
                result.session().getId(), userId, result.records().size(), result.rejectedCount());

        List<ResultatKpiResponse> responses = result.records().stream().map(r -> ResultatKpiResponse.builder()
                .id(r.getId())
                .kpiId(r.getKpi().getId())
                .kpiNom(r.getKpi().getNom())
                .kpiUnite(r.getKpi().getUnite().name())
                .categorieCode(r.getKpi().getCategorieKpi().getCode())
                .categorieLibelle(r.getKpi().getCategorieKpi().getLibelle())
                .periodeN1(r.getPeriodeN1())
                .periodeN(r.getPeriodeN())
                .valeurN1(r.getValeurN1())
                .valeurN(r.getValeurN())
                .variationAbsolue(r.getVariationAbsolue())
                .variationRelative(r.getVariationRelative())
                .niveauVariation(r.getNiveauVariation())
                .tendance(r.getTendance())
                .confidenceScore(r.getConfidenceScore())
                .qualityStatus(r.getQualityStatus())
                .analyseIa(r.getAnalyseIa())
                .createdAt(r.getCreatedAt())
                .build()).toList();

        return AutoImportResultResponse.builder()
                .importSession(toImportSessionResponse(result.session(), result.records().size(), result.rejectedCount()))
                .resultats(responses)
                .nombreRejetes(result.rejectedCount())
                .build();
    }

    @Transactional(readOnly = true)
    public ResultatGlobalResponse getResultats(Long userId, boolean isAdmin, Long sessionId) {
        ImportSession session = loadSessionWithOwnership(userId, isAdmin, sessionId);
        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(sessionId);
        String synthese = resultats.stream().map(ResultatKpi::getAnalyseIa).findFirst().orElse("Analyse IA temporairement indisponible.");
        return toResultatGlobalResponse(session, resultats, synthese);
    }

    @Transactional(readOnly = true)
    public List<ImportSessionResponse> getHistorique(Long userId, boolean isAdmin) {
        List<ImportSession> sessions = isAdmin
                ? importSessionRepository.findAllByOrderByCreatedAtDesc()
                : importSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return sessions.stream()
                .map(session -> toImportSessionResponse(session, 0, 0))
                .toList();
    }

    @Transactional
    public void annuler(Long userId, Long sessionId) {
        ImportSession session = loadSessionWithOwnership(userId, false, sessionId);
        if (session.getStatut() != ImportStatut.EN_ATTENTE) {
            throw new ImportNotReadyException("Seul un import EN_ATTENTE peut être annulé");
        }

        resultatKpiRepository.deleteByImportSessionId(sessionId);
        importSessionRepository.delete(session);
        log.info("Import annulé sessionId={} userId={}", sessionId, userId);
    }

    private void validateUpload(int periodeN1, int periodeN, MultipartFile file) {
        if (periodeN != periodeN1 + 1) {
            throw new ImportValidationException("La période N doit être égale à N-1 + 1");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidFileFormatException("Seuls les fichiers .xlsx sont autorisés");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("La taille du fichier dépasse 10MB");
        }
    }

    private ImportSession loadSessionWithOwnership(Long userId, boolean isAdmin, Long sessionId) {
        return isAdmin
                ? importSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"))
                : importSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
    }

    private ImportSessionResponse toImportSessionResponse(ImportSession session, int acceptedCount, int rejectedCount) {
        return ImportSessionResponse.builder()
                .id(session.getId())
                .mode(session.getMode())
                .nomFichier(session.getNomFichier())
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .statut(session.getStatut())
                .messageErreur(session.getMessageErreur())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .nombreTotal(acceptedCount + rejectedCount)
                .nombreOk(acceptedCount)
                .nombreCorrige(rejectedCount)
                .nombreManquant(0)
                .nombreInvalide(0)
                .nombreSuspect(0)
                .build();
    }

    private ResultatGlobalResponse toResultatGlobalResponse(ImportSession session, List<ResultatKpi> resultats, String synthese) {
        List<ResultatKpi> ordered = resultats.stream()
            .sorted(Comparator.comparing(ResultatKpi::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();

        List<ResultatKpiResponse> list = ordered.stream().map(r -> ResultatKpiResponse.builder()
                .id(r.getId())
                .kpiId(r.getKpi().getId())
                .kpiNom(r.getKpi().getNom())
                .kpiUnite(r.getKpi().getUnite().name())
                .categorieCode(r.getKpi().getCategorieKpi().getCode())
                .categorieLibelle(r.getKpi().getCategorieKpi().getLibelle())
                .periodeN1(r.getPeriodeN1())
                .periodeN(r.getPeriodeN())
                .valeurN1(r.getValeurN1())
                .valeurN(r.getValeurN())
                .variationAbsolue(r.getVariationAbsolue())
                .variationRelative(r.getVariationRelative())
                .niveauVariation(r.getNiveauVariation())
                .tendance(r.getTendance())
                .confidenceScore(r.getConfidenceScore())
                .qualityStatus(r.getQualityStatus())
                .analyseIa(r.getAnalyseIa())
                .createdAt(r.getCreatedAt())
                .build()).toList();

        Map<NiveauVariation, Long> counts = ordered.stream()
                .collect(Collectors.groupingBy(ResultatKpi::getNiveauVariation, Collectors.counting()));

        return ResultatGlobalResponse.builder()
                .importSessionId(session.getId())
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .resultats(list)
                .nombreFaible(counts.getOrDefault(NiveauVariation.FAIBLE, 0L))
                .nombreModere(counts.getOrDefault(NiveauVariation.MODERE, 0L))
                .nombreCritique(counts.getOrDefault(NiveauVariation.CRITIQUE, 0L))
                .analyseGlobaleIa(synthese)
                .build();
    }

}
