package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.*;
import com.QHSEAnalytics.entity.*;
import com.QHSEAnalytics.enums.*;
import com.QHSEAnalytics.exception.*;
import com.QHSEAnalytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final TemplateExcelService templateExcelService;
    private final ExcelParserService excelParserService;
    private final NettoyageService nettoyageService;
    private final VariationService variationService;
    private final AnalyseIaService analyseIaService;

    private final ImportSessionRepository importSessionRepository;
    private final StagingDonneeRepository stagingDonneeRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final KpiRepository kpiRepository;
    private final UserRepository userRepository;

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
        log.info("Upload démarré userId={} fichier={}", userId, file.getOriginalFilename());
        validateUpload(periodeN1, periodeN, file);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        ImportSession session = ImportSession.builder()
            .user(user)
                .mode(ImportMode.TEMPLATE_OFFICIEL)
                .nomFichier(file.getOriginalFilename())
                .templateVersion("V1")
                .periodeN1(periodeN1)
                .periodeN(periodeN)
                .statut(ImportStatut.EN_ATTENTE)
                .build();
        session = importSessionRepository.save(session);

        List<ExcelParserService.DonneeExtraite> donnees = excelParserService.parseTemplateOfficiel(file);

        List<StagingDonnee> staging = new ArrayList<>();
        for (ExcelParserService.DonneeExtraite donnee : donnees) {
            if (donnee.kpiId() == null) {
                continue;
            }
            Kpi kpi = kpiRepository.findById(donnee.kpiId())
                    .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + donnee.kpiId()));
            staging.add(nettoyageService.nettoyerDonnee(donnee, session, kpi));
        }

        stagingDonneeRepository.saveAll(staging);
        Map<StatutNettoyage, Long> stats = countByStatus(staging);
        log.info("Nettoyage terminé sessionId={} ok={} corrige={} manquant={} invalide={} suspect={}",
            session.getId(),
            stats.getOrDefault(StatutNettoyage.OK, 0L),
            stats.getOrDefault(StatutNettoyage.CORRIGE, 0L),
            stats.getOrDefault(StatutNettoyage.MANQUANT, 0L),
            stats.getOrDefault(StatutNettoyage.INVALIDE, 0L),
            stats.getOrDefault(StatutNettoyage.SUSPECT, 0L));
        log.info("Upload terminé sessionId={} userId={} lignes={}", session.getId(), userId, staging.size());
        return toImportSessionResponse(session);
    }

    @Transactional(noRollbackFor = ImportValidationException.class)
    public ResultatGlobalResponse confirmer(Long userId, Long sessionId) {
        ImportSession session = loadSessionWithOwnership(userId, false, sessionId);

        if (session.getStatut() != ImportStatut.EN_ATTENTE) {
            throw new ImportNotReadyException("Import non confirmable dans l'état actuel");
        }

        Map<StatutNettoyage, Long> counts = getStagingCounts(session.getId());
        long missing = counts.getOrDefault(StatutNettoyage.MANQUANT, 0L);
        long invalid = counts.getOrDefault(StatutNettoyage.INVALIDE, 0L);
        if (missing > 0 || invalid > 0) {
            throw new ImportNotReadyException("Import non prêt: " + missing + " donnée(s) MANQUANT et " + invalid + " donnée(s) INVALIDE.");
        }

        session.setStatut(ImportStatut.EN_TRAITEMENT);
        importSessionRepository.save(session);
        log.info("Traitement démarré sessionId={}", sessionId);

        try {
            List<StagingDonnee> stagingList = stagingDonneeRepository.findByImportSessionId(sessionId);
            List<ResultatKpi> resultats = new ArrayList<>();

            for (StagingDonnee s : stagingList) {
                if (s.getStatutNettoyage() == StatutNettoyage.IGNORE || s.getValeurN1() == null || s.getValeurN() == null) {
                    continue;
                }

                double variationAbs = variationService.calculerVariationAbsolue(s.getValeurN1(), s.getValeurN());
                double variationRel = variationService.calculerVariationRelative(s.getValeurN1(), s.getValeurN());
                NiveauVariation niveau = variationService.classifierVariation(variationRel, s.getKpi());
                Tendance tendance = variationService.determinerTendance(variationRel);

                resultats.add(ResultatKpi.builder()
                        .importSession(session)
                        .kpi(s.getKpi())
                        .user(session.getUser())
                        .periodeN1(session.getPeriodeN1())
                        .periodeN(session.getPeriodeN())
                        .valeurN1(s.getValeurN1())
                        .valeurN(s.getValeurN())
                        .variationAbsolue(variationAbs)
                        .variationRelative(variationRel)
                        .niveauVariation(niveau)
                        .tendance(tendance)
                        .build());
            }

            List<ResultatKpi> saved = resultatKpiRepository.saveAll(resultats);

            try {
                analyseIaService.genererToutesLesAnalyses(sessionId, userId);
                log.info("Analyses IA générées pour la session {}", sessionId);
            } catch (Exception ex) {
                log.error("Erreur génération analyses IA (non bloquante) : {}", ex.getMessage());
            }

            List<ResultatKpi> resultatsEnrichis = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(sessionId);
            String synthese;
            try {
                synthese = analyseIaService.getAnalyseGlobale(sessionId, userId, false).getSynthese();
            } catch (Exception ex) {
                synthese = "Analyse IA temporairement indisponible.";
            }

            stagingDonneeRepository.deleteByImportSessionId(sessionId);
            log.info("Staging supprimé sessionId={}", sessionId);

            session.setStatut(ImportStatut.TRAITE);
            session.setMessageErreur(null);
            importSessionRepository.save(session);
            log.info("Traitement terminé sessionId={} resultats={}", sessionId, saved.size());

            return toResultatGlobalResponse(session, resultatsEnrichis, synthese);
        } catch (Exception ex) {
            resultatKpiRepository.deleteByImportSessionId(sessionId);
            session.setStatut(ImportStatut.ERREUR);
            session.setMessageErreur(ex.getMessage());
            importSessionRepository.save(session);
            log.error("Erreur traitement sessionId={} cause={}", sessionId, ex.getMessage());
            throw new ImportValidationException("Erreur durant le traitement: " + ex.getMessage());
        }
    }

    @Transactional(noRollbackFor = ImportValidationException.class)
    public ResultatGlobalResponse uploadAndConfirm(
            Long userId,
            int periodeN1,
            int periodeN,
            MultipartFile file
    ) {
        log.info("Upload & confirm automatique démarré userId={}", userId);
        
        // Step 1: Upload normalement
        ImportSessionResponse uploadResponse = upload(userId, periodeN1, periodeN, file);
        Long sessionId = uploadResponse.getId();
        
        // Step 2: Vérifier la qualité des données
        Map<StatutNettoyage, Long> counts = getStagingCounts(sessionId);
        long missing = counts.getOrDefault(StatutNettoyage.MANQUANT, 0L);
        long invalid = counts.getOrDefault(StatutNettoyage.INVALIDE, 0L);
        
        if (missing > 0 || invalid > 0) {
            // Données non prêtes, nettoyer le staging et retourner l'erreur
            stagingDonneeRepository.deleteByImportSessionId(sessionId);
            ImportSession session = importSessionRepository.findById(sessionId).orElseThrow();
            session.setStatut(ImportStatut.ERREUR);
            session.setMessageErreur(String.format("Données non valides: %d manquante(s), %d invalide(s)", missing, invalid));
            importSessionRepository.save(session);
            log.warn("Upload & confirm échoué sessionId={} - données invalides", sessionId);
            throw new ImportValidationException(String.format("Données non valides: %d manquante(s), %d invalide(s). Veuillez utiliser le mode détaillé.", missing, invalid));
        }
        
        // Step 3: Confirmer automatiquement
        log.info("Données valides - confirmation automatique sessionId={}", sessionId);
        return confirmer(userId, sessionId);
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

        return sessions.stream().map(this::toImportSessionResponse).toList();
    }

    @Transactional
    public void annuler(Long userId, Long sessionId) {
        ImportSession session = loadSessionWithOwnership(userId, false, sessionId);
        if (session.getStatut() != ImportStatut.EN_ATTENTE) {
            throw new ImportNotReadyException("Seul un import EN_ATTENTE peut être annulé");
        }

        stagingDonneeRepository.deleteByImportSessionId(sessionId);
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
        templateExcelService.validateTemplateSignature(file);
    }

    private ImportSession loadSessionWithOwnership(Long userId, boolean isAdmin, Long sessionId) {
        return isAdmin
                ? importSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"))
                : importSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
    }

    private ImportSessionResponse toImportSessionResponse(ImportSession session) {
        Map<StatutNettoyage, Long> counts = getStagingCounts(session.getId());
        long ok = counts.getOrDefault(StatutNettoyage.OK, 0L);
        long corrige = counts.getOrDefault(StatutNettoyage.CORRIGE, 0L);
        long manquant = counts.getOrDefault(StatutNettoyage.MANQUANT, 0L);
        long invalide = counts.getOrDefault(StatutNettoyage.INVALIDE, 0L);
        long suspect = counts.getOrDefault(StatutNettoyage.SUSPECT, 0L);

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
                .nombreTotal(ok + corrige + manquant + invalide + suspect)
                .nombreOk(ok)
                .nombreCorrige(corrige)
                .nombreManquant(manquant)
                .nombreInvalide(invalide)
                .nombreSuspect(suspect)
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

    private Map<StatutNettoyage, Long> getStagingCounts(Long sessionId) {
        Map<StatutNettoyage, Long> counts = new EnumMap<>(StatutNettoyage.class);
        stagingDonneeRepository.countByImportSessionIdGrouped(sessionId)
                .forEach(view -> counts.put(view.getStatutNettoyage(), view.getTotal()));
        return counts;
    }

    private Map<StatutNettoyage, Long> countByStatus(List<StagingDonnee> staging) {
        Map<StatutNettoyage, Long> counts = new EnumMap<>(StatutNettoyage.class);
        staging.stream()
                .collect(Collectors.groupingBy(StagingDonnee::getStatutNettoyage, Collectors.counting()))
                .forEach(counts::put);
        return counts;
    }
}
