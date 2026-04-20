package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.dto.response.ResultatKpiResponse;
import com.QHSEAnalytics.entity.AnalyseCategorie;
import com.QHSEAnalytics.entity.AnalyseGlobale;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.exception.AnalyseGenerationException;
import com.QHSEAnalytics.exception.AnalyseNotFoundException;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyseIaService {

    private final ResultatKpiRepository resultatKpiRepository;
    private final AnalyseCategorieRepository analyseCategorieRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final GroqService groqService;
    private final GroqPromptBuilder groqPromptBuilder;
    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void genererToutesLesAnalyses(Long importSessionId, Long userId) {
        try {
            ImportSession session = loadSessionWithOwnership(importSessionId, userId, false);
            List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId).stream()
                    .filter(resultat -> resultat.getKpi() != null && resultat.getKpi().getCategorieKpi() != null)
                    .toList();

            if (resultats.isEmpty()) {
                log.info("Aucun résultat KPI exploitable pour la session {}", importSessionId);
                return;
            }

            try {
                for (ResultatKpi resultat : resultats) {
                    try {
                        String prompt = groqPromptBuilder.buildKpiPrompt(
                                resultat.getKpi().getNom(),
                                resultat.getKpi().getDefinition(),
                                resultat.getKpi().getUnite().name(),
                                resultat.getKpi().getCategorieKpi().getLibelle(),
                                resultat.getValeurN1(),
                                resultat.getValeurN(),
                                resultat.getVariationRelative(),
                                resultat.getNiveauVariation(),
                                resultat.getTendance(),
                                resultat.getPeriodeN1(),
                                resultat.getPeriodeN()
                        );
                        resultat.setAnalyseIa(groqService.analyserKpi(prompt));
                        resultatKpiRepository.save(resultat);
                        log.info("Analyse KPI générée : {}", resultat.getKpi().getNom());
                    } catch (Exception ex) {
                        log.error("Erreur génération analyse KPI {} : {}", resultat.getKpi().getNom(), ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.error("Erreur bloc niveau KPI session {} : {}", importSessionId, ex.getMessage());
            }

            Map<String, List<ResultatKpi>> byCategory = resultats.stream()
                    .collect(Collectors.groupingBy(
                            resultat -> resultat.getKpi().getCategorieKpi().getCode(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            try {
                for (Map.Entry<String, List<ResultatKpi>> entry : byCategory.entrySet()) {
                    ResultatKpi first = entry.getValue().get(0);
                    String categorieCode = entry.getKey();
                    String categorieLibelle = first.getKpi().getCategorieKpi().getLibelle();
                    try {
                        String prompt = groqPromptBuilder.buildCategoriePrompt(
                                categorieLibelle,
                                entry.getValue(),
                                session.getPeriodeN1(),
                                session.getPeriodeN()
                        );

                        String contenu = groqService.analyserCategorie(prompt);
                        AnalyseCategorie analyse = analyseCategorieRepository
                                .findByImportSessionIdAndCategorieCode(importSessionId, categorieCode)
                                .orElseGet(AnalyseCategorie::new);
                        analyse.setImportSession(session);
                        analyse.setUser(loadUser(userId));
                        analyse.setCategorieCode(categorieCode);
                        analyse.setCategorieLibelle(categorieLibelle);
                        analyse.setContenu(contenu);
                        analyseCategorieRepository.save(analyse);
                        log.info("Analyse catégorie générée : {}", categorieCode);
                    } catch (Exception ex) {
                        log.error("Erreur génération analyse catégorie {} : {}", categorieCode, ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.error("Erreur bloc niveau catégorie session {} : {}", importSessionId, ex.getMessage());
            }

            String synthese = "";
            String planActions = "";
            try {
                String synthesePrompt = groqPromptBuilder.buildSynthesePrompt(resultats, session.getPeriodeN1(), session.getPeriodeN());
                synthese = groqService.genererSynthese(synthesePrompt);
            } catch (Exception ex) {
                log.error("Erreur génération synthèse globale session {} : {}", importSessionId, ex.getMessage());
                synthese = "Analyse IA temporairement indisponible.";
            }

            try {
                String planPrompt = groqPromptBuilder.buildPlanActionsPrompt(resultats, session.getPeriodeN1(), session.getPeriodeN());
                planActions = groqService.genererPlanActions(planPrompt);
            } catch (Exception ex) {
                log.error("Erreur génération plan d'actions session {} : {}", importSessionId, ex.getMessage());
                planActions = "Analyse IA temporairement indisponible.";
            }

            try {
                AnalyseGlobale analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importSessionId)
                        .orElseGet(AnalyseGlobale::new);
                analyseGlobale.setImportSession(session);
                analyseGlobale.setUser(loadUser(userId));
                analyseGlobale.setSynthese(synthese);
                analyseGlobale.setPlanActions(planActions);
                analyseGlobaleRepository.save(analyseGlobale);
                log.info("Synthèse globale et plan d'actions générés pour la session {}", importSessionId);
            } catch (Exception ex) {
                log.error("Erreur sauvegarde analyse globale session {} : {}", importSessionId, ex.getMessage());
            }
        } catch (Exception ex) {
            log.error("Erreur inattendue génération analyses session {} : {}", importSessionId, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AnalyseCategorieResponse> getAnalysesCategories(Long importSessionId, Long userId, boolean isAdmin) {
        loadSessionWithOwnership(importSessionId, userId, isAdmin);
        return analyseCategorieRepository.findByImportSessionId(importSessionId).stream()
                .sorted(Comparator.comparing(AnalyseCategorie::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
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
        loadSessionWithOwnership(importSessionId, userId, false);

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