package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.dto.response.ResultatKpiResponse;
import com.QHSEAnalytics.dto.ollama.AiResponse;
import com.QHSEAnalytics.entity.AnalyseCategorie;
import com.QHSEAnalytics.entity.AnalyseGlobale;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.ImportStatut;

import com.QHSEAnalytics.exception.AnalyseNotFoundException;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.exception.ImportNotReadyException;
import com.QHSEAnalytics.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import com.QHSEAnalytics.service.processing.AnalysisAgent;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyseIaService {

    private final ResultatKpiRepository resultatKpiRepository;
    private final AnalyseCategorieRepository analyseCategorieRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final AnalysisAgent analysisAgent;
    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void genererToutesLesAnalyses(Long importSessionId, Long userId) {
        ImportSession session = null;
        long startAt = System.currentTimeMillis();
        try {
            session = loadSessionWithOwnership(importSessionId, userId, false);
            log.info("Début de l'analyse IA pour la session {}", importSessionId);

            List<ResultatKpi> resultats = resultatKpiRepository
                    .findByImportSessionIdOrderByCreatedAtDesc(importSessionId).stream()
                    .filter(resultat -> resultat.getKpi() != null && resultat.getKpi().getCategorieKpi() != null)
                    .toList();

            if (resultats.isEmpty()) {
                log.info("Aucun résultat KPI exploitable pour la session {}", importSessionId);
                return;
            }

            List<KpiCalculatedDTO> kpiData = resultats.stream().map(this::toKpiCalculatedDTO).toList();
            List<KpiCalculatedDTO> cleanedData = cleanKpis(kpiData);
            log.info("KPI nettoyage pour session {} : {} -> {}", importSessionId, kpiData.size(), cleanedData.size());

            AiResponse response = analysisAgent.analyzeStrict(cleanedData);
            if (response == null) {
                response = AiResponse.builder()
                        .overallScore(0.0)
                        .summary("IA indisponible")
                        .recommendations(List.of())
                        .kpis(List.of())
                        .build();
            }

            String globalSummary = response.getSummary();
            if (globalSummary == null || globalSummary.isBlank()) {
                globalSummary = "IA indisponible";
            }

            Map<String, String> insightsByKpiName = response.getKpis().stream()
                    .filter(kpi -> kpi.getName() != null && !kpi.getName().isBlank())
                    .collect(Collectors.toMap(
                            kpi -> kpi.getName().trim().toLowerCase(),
                            kpi -> kpi.getInsight() == null ? "" : kpi.getInsight().trim(),
                            (first, second) -> first));

            for (ResultatKpi resultat : resultats) {
                String key = resultat.getKpi().getNom() == null ? null
                        : resultat.getKpi().getNom().trim().toLowerCase();
                String analyseIa = key == null ? null : insightsByKpiName.get(key);
                if (analyseIa != null && !analyseIa.isBlank()) {
                    resultat.setAnalyseIa(analyseIa);
                    resultatKpiRepository.save(resultat);
                }
            }

            AnalyseGlobale analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importSessionId)
                    .orElseGet(AnalyseGlobale::new);
            analyseGlobale.setImportSession(session);
            analyseGlobale.setUser(loadUser(userId));
            analyseGlobale.setSynthese(globalSummary);
            analyseGlobale.setPlanActions(String.join("\n",
                    response.getRecommendations() == null ? List.of() : response.getRecommendations()));
            analyseGlobaleRepository.save(analyseGlobale);

            boolean iaUnavailable = globalSummary != null && globalSummary.trim().equalsIgnoreCase("IA indisponible");
            if (iaUnavailable) {
                session.setMessageErreur("Analyse IA indisponible. Veuillez réessayer ultérieurement.");
            } else {
                session.setStatut(ImportStatut.TRAITE);
                session.setMessageErreur(null);
            }
            importSessionRepository.save(session);
            log.info("Analyse IA complète générée pour la session {} en {} ms", importSessionId,
                    System.currentTimeMillis() - startAt);
        } catch (Exception ex) {
            log.error("Erreur inattendue génération analyses session {} : {}", importSessionId, ex.getMessage(), ex);
            if (session != null) {
                try {
                    session.setStatut(ImportStatut.ERREUR);
                    importSessionRepository.save(session);
                } catch (Exception saveEx) {
                    log.error("Impossible de mettre à jour le statut ERREUR pour la session {} : {}", importSessionId,
                            saveEx.getMessage(), saveEx);
                }
            }
        }
    }

    @Async("aiAnalysisExecutor")
    @Transactional
    public CompletableFuture<Void> triggerAnalyseAsync(Long importSessionId, Long userId) {
        ImportSession session = loadSessionWithOwnership(importSessionId, userId, false);
        if (!session.getStatut().isReadyForAi()) {
            log.warn("Import {} non prêt pour analyse IA.", importSessionId);
            return CompletableFuture.completedFuture(null);
        }

        boolean alreadyStored = session.getStatut() == ImportStatut.TRAITE && (analyseGlobaleRepository
                .findByImportSessionId(importSessionId).isPresent()
                || !analyseCategorieRepository.findByImportSessionId(importSessionId).isEmpty()
                || resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId).stream()
                        .anyMatch(resultat -> resultat.getAnalyseIa() != null && !resultat.getAnalyseIa().isBlank()));

        if (alreadyStored) {
            log.info("Analyse IA déjà présente pour la session {}, aucune requête supplémentaire nécessaire.",
                    importSessionId);
            return CompletableFuture.completedFuture(null);
        }

        genererToutesLesAnalyses(importSessionId, userId);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public List<AnalyseCategorieResponse> getAnalysesCategories(Long importSessionId, Long userId, boolean isAdmin) {
        loadSessionWithOwnership(importSessionId, userId, isAdmin);
        return analyseCategorieRepository.findByImportSessionId(importSessionId).stream()
                .sorted(Comparator.comparing(AnalyseCategorie::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAnalyseCategorieResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalyseGlobaleResponse getAnalyseGlobale(Long importSessionId, Long userId, boolean isAdmin) {
        loadSessionWithOwnership(importSessionId, userId, isAdmin);
        AnalyseGlobale analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importSessionId)
                .orElseThrow(() -> new AnalyseNotFoundException("Aucune analyse trouvée pour cet import."));
        return toAnalyseGlobaleResponse(analyseGlobale);
    }

    @Transactional(readOnly = true)
    public AnalyseCompleteResponse getAnalyseComplete(Long importSessionId, Long userId, boolean isAdmin) {
        ImportSession session = loadSessionWithOwnership(importSessionId, userId, isAdmin);
        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId);
        List<AnalyseCategorieResponse> categories = getAnalysesCategories(importSessionId, userId, isAdmin);
        AnalyseGlobaleResponse analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importSessionId)
                .map(this::toAnalyseGlobaleResponse)
                .orElse(null);

        return AnalyseCompleteResponse.builder()
                .importSessionId(importSessionId)
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .analysesKpis(resultats.stream().map(this::toResultatKpiResponse).toList())
                .analysesCategories(categories)
                .analyseGlobale(analyseGlobale)
                .build();
    }

    @Transactional
    public AnalyseCompleteResponse regenerer(Long importSessionId, Long userId) {
        log.info("Régénération analyses demandée pour session {}", importSessionId);
        ImportSession session = loadSessionWithOwnership(importSessionId, userId, false);
        if (!session.getStatut().isReadyForAi()) {
            throw new ImportNotReadyException("Import non prêt pour analyse IA.");
        }

        try {
            analyseCategorieRepository.deleteByImportSessionId(importSessionId);
            analyseGlobaleRepository.deleteByImportSessionId(importSessionId);
        } catch (Exception ex) {
            log.error("Erreur suppression anciennes analyses session {} : {}", importSessionId, ex.getMessage());
        }

        genererToutesLesAnalyses(importSessionId, userId);
        return getAnalyseComplete(importSessionId, userId, false);
    }

    private ResultatKpiResponse toResultatKpiResponse(ResultatKpi resultat) {
        return ResultatKpiResponse.builder()
                .id(resultat.getId())
                .kpiId(resultat.getKpi().getId())
                .kpiNom(resultat.getKpi().getNom())
                .kpiUnite(resultat.getKpi().getUnite().name())
                .categorieCode(resultat.getKpi().getCategorieKpi().getCode())
                .categorieLibelle(resultat.getKpi().getCategorieKpi().getLibelle())
                .periodeN1(resultat.getPeriodeN1())
                .periodeN(resultat.getPeriodeN())
                .valeurN1(resultat.getValeurN1())
                .valeurN(resultat.getValeurN())
                .variationAbsolue(resultat.getVariationAbsolue())
                .variationRelative(resultat.getVariationRelative())
                .niveauVariation(resultat.getNiveauVariation())
                .tendance(resultat.getTendance())
                .analyseIa(resultat.getAnalyseIa())
                .createdAt(resultat.getCreatedAt())
                .build();
    }

    private AnalyseCategorieResponse toAnalyseCategorieResponse(AnalyseCategorie analyseCategorie) {
        return AnalyseCategorieResponse.builder()
                .id(analyseCategorie.getId())
                .importSessionId(analyseCategorie.getImportSession().getId())
                .categorieCode(analyseCategorie.getCategorieCode())
                .categorieLibelle(analyseCategorie.getCategorieLibelle())
                .contenu(analyseCategorie.getContenu())
                .createdAt(analyseCategorie.getCreatedAt())
                .build();
    }

    private AnalyseGlobaleResponse toAnalyseGlobaleResponse(AnalyseGlobale analyseGlobale) {
        return AnalyseGlobaleResponse.builder()
                .id(analyseGlobale.getId())
                .importSessionId(analyseGlobale.getImportSession().getId())
                .synthese(analyseGlobale.getSynthese())
                .planActions(analyseGlobale.getPlanActions())
                .createdAt(analyseGlobale.getCreatedAt())
                .build();
    }

    private KpiCalculatedDTO toKpiCalculatedDTO(ResultatKpi resultat) {
        return KpiCalculatedDTO.builder()
                .rowIndex(0)
                .kpiName(resultat.getKpi().getNom())
                .categorie(resultat.getKpi().getCategorieKpi().getLibelle())
                .categorieCode(resultat.getKpi().getCategorieKpi().getCode())
                .unite(resultat.getKpi().getUnite() == null ? null : resultat.getKpi().getUnite().name())
                .valeurN1(resultat.getValeurN1())
                .valeurN(resultat.getValeurN())
                .seuilFaible(resultat.getKpi().getSeuilFaible())
                .seuilModere(resultat.getKpi().getSeuilModere())
                .seuilCritique(resultat.getKpi().getSeuilCritique())
                .definition(resultat.getKpi().getDefinition())
                .variationAbsolute(resultat.getVariationAbsolue())
                .variationPercentage(resultat.getVariationRelative())
                .classification(resultat.getNiveauVariation() == null ? null : resultat.getNiveauVariation().name())
                .tendance(resultat.getTendance() == null ? null : resultat.getTendance().name())
                .matchedKpi(resultat.getKpi().getNom())
                .matchedKpiId(resultat.getKpi().getId())
                .build();
    }

    private List<KpiCalculatedDTO> cleanKpis(List<KpiCalculatedDTO> kpis) {
        if (kpis == null || kpis.isEmpty()) {
            return List.of();
        }

        return kpis.stream()
                .filter(kpi -> kpi.getKpiName() != null && !kpi.getKpiName().isBlank())
                .filter(kpi -> kpi.getVariationPercentage() != null)
                .collect(Collectors.toMap(
                        kpi -> kpi.getMatchedKpiId() != null ? kpi.getMatchedKpiId()
                                : kpi.getKpiName().trim().toLowerCase(),
                        kpi -> kpi,
                        (first, second) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private ImportSession loadSessionWithOwnership(Long importSessionId, Long userId, boolean isAdmin) {
        ImportSession session = isAdmin
                ? importSessionRepository.findById(importSessionId)
                        .orElseThrow(() -> new ImportNotFoundException("Import introuvable"))
                : importSessionRepository.findByIdAndUserId(importSessionId, userId)
                        .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
        return session;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}