package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.QHSEAnalytics.shared.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.shared.dto.response.AiAnalysisStructuredResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.shared.dto.response.ResultatKpiResponse;
import com.QHSEAnalytics.shared.dto.llm.AiResponse;
import com.QHSEAnalytics.shared.entity.AnalyseCategorie;
import com.QHSEAnalytics.shared.entity.AnalyseGlobale;
import com.QHSEAnalytics.shared.entity.ImportSession;
import com.QHSEAnalytics.shared.entity.ResultatKpi;
import com.QHSEAnalytics.shared.enums.ImportStatut;

import com.QHSEAnalytics.shared.exception.AnalyseNotFoundException;
import com.QHSEAnalytics.shared.exception.ImportNotFoundException;
import com.QHSEAnalytics.shared.exception.ImportNotReadyException;
import com.QHSEAnalytics.shared.repository.AnalyseCategorieRepository;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.ImportSessionRepository;
import com.QHSEAnalytics.shared.repository.KpiAnalysisRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import com.QHSEAnalytics.shared.entity.KpiAnalysis;
import com.QHSEAnalytics.analytics.service.processing.TextNormalizer;
import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.processing.AnalysisAgent;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
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
    private final KpiAnalysisRepository kpiAnalysisRepository;
    private final AnalysisAgent analysisAgent;
    private final LlmProviderChain llmProviderChain;
    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void genererToutesLesAnalyses(Long importSessionId, Long userId) {
        genererToutesLesAnalyses(importSessionId, userId, false);
    }

    @Transactional
    public void genererToutesLesAnalyses(Long importSessionId, Long userId, boolean bypassStructuredCache) {
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
                            kpi -> TextNormalizer.normalizeForSearch(kpi.getName()),
                            kpi -> firstNonBlankText(
                                    kpi.getInsight(),
                                    kpi.getNoteFinale(),
                                    kpi.getAiNote()),
                            (first, second) -> first));

            for (ResultatKpi resultat : resultats) {
                String key = resultat.getKpi().getNom() == null ? null
                        : TextNormalizer.normalizeForSearch(resultat.getKpi().getNom());
                String analyseIa = key == null ? null : insightsByKpiName.get(key);


                if ((analyseIa == null || analyseIa.isBlank()) && key != null) {
                    for (Map.Entry<String, String> entry : insightsByKpiName.entrySet()) {
                        String mapKey = entry.getKey();
                        if (mapKey != null && (mapKey.contains(key) || key.contains(mapKey))) {
                            analyseIa = entry.getValue();
                            break;
                        }
                    }
                }


                if (analyseIa == null || analyseIa.isBlank()) {
                    double variation = resultat.getVariationRelative() == null ? 0.0 : resultat.getVariationRelative();
                    String niveau = resultat.getNiveauVariation() == null ? "FAIBLE" : resultat.getNiveauVariation().name();
                    analyseIa = String.format(
                            "Indicateur %s : variation de %.1f%%. Niveau : %s. Aucune anomalie critique détectée automatiquement — un examen manuel est recommandé.",
                            resultat.getKpi().getNom(), variation, niveau);
                    log.debug("[AnalyseIa] Fallback text generated for KPI '{}'", resultat.getKpi().getNom());
                }

                resultat.setAnalyseIa(analyseIa);
                resultatKpiRepository.save(resultat);
            }

            AnalyseGlobale analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importSessionId)
                    .orElseGet(AnalyseGlobale::new);
            analyseGlobale.setImportSession(session);
            analyseGlobale.setUser(loadUser(userId));
            analyseGlobale.setSynthese(globalSummary);
            analyseGlobale.setPlanActions(String.join("\n",
                    response.getRecommendations() == null ? List.of() : response.getRecommendations()));
            analyseGlobaleRepository.save(analyseGlobale);


            User user = loadUser(userId);
            Map<String, List<ResultatKpi>> byCategorie = resultats.stream()
                    .filter(r -> r.getKpi().getCategorieKpi() != null)
                    .collect(Collectors.groupingBy(
                            r -> r.getKpi().getCategorieKpi().getCode(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            for (Map.Entry<String, List<ResultatKpi>> entry : byCategorie.entrySet()) {
                String catCode    = entry.getKey();
                List<ResultatKpi> catKpis = entry.getValue();
                if (catKpis.isEmpty()) continue;

                String catLibelle = catKpis.get(0).getKpi().getCategorieKpi().getLibelle();
                long critiques = catKpis.stream().filter(r -> r.getNiveauVariation() != null
                        && "CRITIQUE".equals(r.getNiveauVariation().name())).count();
                long moderes   = catKpis.stream().filter(r -> r.getNiveauVariation() != null
                        && "MODERE".equals(r.getNiveauVariation().name())).count();

                StringBuilder catContenu = new StringBuilder();
                catContenu.append(String.format(
                        "Catégorie %s — %d indicateur(s) analysé(s). %d critique(s), %d modéré(s).\n\n",
                        catLibelle, catKpis.size(), critiques, moderes));

                for (ResultatKpi r : catKpis) {
                    String kpiNom    = r.getKpi().getNom();
                    String kpiInsight = r.getAnalyseIa();
                    String niveau    = r.getNiveauVariation() == null ? "N/A" : r.getNiveauVariation().name();
                    double variation = r.getVariationRelative() == null ? 0.0 : r.getVariationRelative();
                    catContenu.append(String.format("• %s [%s, %+.1f%%] : %s\n",
                            kpiNom, niveau, variation,
                            kpiInsight != null ? kpiInsight : "—"));
                }


                if (response.getRecommendations() != null && !response.getRecommendations().isEmpty()) {
                    catContenu.append("\nRecommandations globales :\n");
                    response.getRecommendations().forEach(rec -> catContenu.append("  → ").append(rec).append("\n"));
                }

                AnalyseCategorie ac = analyseCategorieRepository
                        .findByImportSessionIdAndCategorieCode(importSessionId, catCode)
                        .orElseGet(AnalyseCategorie::new);
                ac.setImportSession(session);
                ac.setUser(user);
                ac.setCategorieCode(catCode);
                ac.setCategorieLibelle(catLibelle);
                ac.setContenu(catContenu.toString().trim());
                analyseCategorieRepository.save(ac);
                log.debug("[AnalyseIa] AnalyseCategorie saved for category '{}' (session {})", catCode, importSessionId);
            }
            log.info("[AnalyseIa] {} AnalyseCategorie records saved for session {}", byCategorie.size(), importSessionId);

            boolean iaUnavailable = globalSummary != null && globalSummary.trim().equalsIgnoreCase("IA indisponible");
            if (iaUnavailable) {
                session.setMessageErreur("Analyse IA indisponible. Veuillez réessayer ultérieurement.");
            } else {
                session.setStatut(ImportStatut.TRAITE);
                session.setMessageErreur(null);
            }
            importSessionRepository.save(session);

            // Persister l'analyse structurée et enrichir les KpiAnalysis manquants
            if (!iaUnavailable) {
                try {
                    AiAnalysisStructuredResponse structured =
                            analysisAgent.analyzeStructured(cleanedData, importSessionId, bypassStructuredCache);
                    if (structured != null) {
                        analyseGlobale.setStructuredResponseJson(objectMapper.writeValueAsString(structured));
                        if (structured.getConfidence() != null && structured.getConfidence().getOverall() != null) {
                            analyseGlobale.setOverallConfidence(structured.getConfidence().getOverall().intValue());
                        }
                        analyseGlobaleRepository.save(analyseGlobale);
                        log.info("[IA] Analyse structurée persistée pour session {}", importSessionId);

                        // Enrichir les KpiAnalysis manquants depuis les kpiInsights structurés
                        enrichKpiAnalysisFromStructured(session, structured, resultats);
                    }
                } catch (Exception e) {
                    log.warn("[IA] Impossible de persister l'analyse structurée pour session {} : {}", importSessionId, e.getMessage());
                }
            }

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
    public CompletableFuture<Void> triggerAnalyseAsync(Long importSessionId, Long userId) {
        log.info("[IA-Async] Déclenchement analyse IA pour import {} user {} (thread: {})",
                importSessionId, userId, Thread.currentThread().getName());
        try {
            ImportSession session = loadSessionForAsync(importSessionId);
            if (session.getStatut() != ImportStatut.READY_FOR_AI) {
                log.warn("[IA-Async] Import {} pas en READY_FOR_AI (statut: {}), skip.",
                        importSessionId, session.getStatut());
                return CompletableFuture.completedFuture(null);
            }

            boolean alreadyStored = analyseGlobaleRepository.findByImportSessionId(importSessionId).isPresent()
                    || !analyseCategorieRepository.findByImportSessionId(importSessionId).isEmpty()
                    || resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId).stream()
                            .anyMatch(r -> r.getAnalyseIa() != null && !r.getAnalyseIa().isBlank());

            if (alreadyStored) {
                log.info("[IA-Async] Analyse IA déjà présente pour la session {}, skip.", importSessionId);
                return CompletableFuture.completedFuture(null);
            }

            genererToutesLesAnalyses(importSessionId, userId);
        } catch (Exception e) {
            log.error("[IA-Async] Erreur lors de l'analyse IA pour import {} : {}", importSessionId, e.getMessage(), e);
        }
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
    public AiAnalysisStructuredResponse getAnalyseStructured(Long importSessionId, Long userId, boolean isAdmin) {
        ImportSession session = loadSessionWithOwnership(importSessionId, userId, isAdmin);

        // Analyse encore en cours → ne pas déclencher un appel LLM concurrent
        if (session.getStatut() == ImportStatut.READY_FOR_AI) {
            log.info("[AnalyseStructured] Session {} encore en READY_FOR_AI — analyse en cours, structured non disponible.", importSessionId);
            return null;
        }

        // Lire depuis la base si déjà générée — ne pas relancer le LLM
        var analyseGlobaleOpt = analyseGlobaleRepository.findByImportSessionId(importSessionId);
        if (analyseGlobaleOpt.isPresent()) {
            String json = analyseGlobaleOpt.get().getStructuredResponseJson();
            if (json != null && !json.isBlank()) {
                try {
                    return objectMapper.readValue(json, AiAnalysisStructuredResponse.class);
                } catch (Exception e) {
                    log.warn("[AnalyseStructured] Échec désérialisation JSON pour session {}, recalcul.", importSessionId);
                }
            }
        }

        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId).stream()
                .filter(resultat -> resultat.getKpi() != null && resultat.getKpi().getCategorieKpi() != null)
                .toList();

        if (resultats.isEmpty()) {
            return AiAnalysisStructuredResponse.builder()
                    .status("FAILED")
                    .fallbackReason("Aucune donnée KPI disponible pour l'analyse structurée.")
                    .globalSummary("")
                    .confidence(null)
                    .kpiInsights(List.of())
                    .probableCauses(List.of())
                    .recommendations(List.of())
                    .actionPlan(List.of())
                    .traceability(null)
                    .build();
        }

        List<KpiCalculatedDTO> kpiData = resultats.stream().map(this::toKpiCalculatedDTO).toList();
        List<KpiCalculatedDTO> cleanedData = cleanKpis(kpiData);
        AiAnalysisStructuredResponse result = analysisAgent.analyzeStructured(cleanedData, importSessionId);

        if (result != null) {
            // Persister pour les consultations futures
            analyseGlobaleRepository.findByImportSessionId(importSessionId).ifPresent(ag -> {
                try {
                    ag.setStructuredResponseJson(objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    log.warn("[AnalyseStructured] Échec sérialisation JSON pour session {}", importSessionId);
                }
                if (result.getConfidence() != null && result.getConfidence().getOverall() != null) {
                    ag.setOverallConfidence(result.getConfidence().getOverall().intValue());
                }
                analyseGlobaleRepository.save(ag);
            });
        }

        return result;
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
            llmProviderChain.clearKpiAnalysisCache();

            List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId);
            for (ResultatKpi r : resultats) {
                r.setAnalyseIa(null);
            }
            resultatKpiRepository.saveAll(resultats);
            log.info("[Regenerer] Cleared analyseIa for {} ResultatKpi records of session {}", resultats.size(), importSessionId);
        } catch (Exception ex) {
            log.error("Erreur suppression anciennes analyses session {} : {}", importSessionId, ex.getMessage());
        }

        genererToutesLesAnalyses(importSessionId, userId, true);
        return getAnalyseComplete(importSessionId, userId, false);
    }

    @Transactional
    public AnalyseCompleteResponse regenererAsAdmin(Long importSessionId) {
        log.info("Admin demande la régénération pour session {}", importSessionId);
        ImportSession session = importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
        if (!session.getStatut().isReadyForAi()) {
            throw new ImportNotReadyException("Import non prêt pour analyse IA.");
        }
        Long sessionOwnerId = session.getUser().getId();

        try {
            analyseCategorieRepository.deleteByImportSessionId(importSessionId);
            analyseGlobaleRepository.deleteByImportSessionId(importSessionId);
            llmProviderChain.clearKpiAnalysisCache();
            List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdOrderByCreatedAtDesc(importSessionId);
            for (ResultatKpi r : resultats) {
                r.setAnalyseIa(null);
            }
            resultatKpiRepository.saveAll(resultats);
            log.info("[RegenerAsAdmin] Cleared analyseIa for {} ResultatKpi records of session {}", resultats.size(), importSessionId);
        } catch (Exception ex) {
            log.error("Erreur suppression analyses pour session {} : {}", importSessionId, ex.getMessage());
        }

        genererToutesLesAnalyses(importSessionId, sessionOwnerId, true);
        return getAnalyseComplete(importSessionId, sessionOwnerId, true);
    }

    private void enrichKpiAnalysisFromStructured(ImportSession session,
                                                   AiAnalysisStructuredResponse structured,
                                                   List<ResultatKpi> resultats) {
        if (structured.getKpiInsights() == null || structured.getKpiInsights().isEmpty()) return;

        // Index des KpiAnalysis déjà persistés pour cet import
        Map<String, com.QHSEAnalytics.shared.entity.KpiAnalysis> existingByName =
                kpiAnalysisRepository.findByImportSessionIdOrderByIdAsc(session.getId()).stream()
                        .filter(a -> a.getKpiName() != null)
                        .collect(Collectors.toMap(
                                a -> TextNormalizer.normalizeForSearch(a.getKpiName()),
                                a -> a,
                                (first, second) -> first));

        // Index des ResultatKpi pour retrouver les infos de classification
        Map<String, ResultatKpi> resultatByName = resultats.stream()
                .filter(r -> r.getKpi() != null && r.getKpi().getNom() != null)
                .collect(Collectors.toMap(
                        r -> TextNormalizer.normalizeForSearch(r.getKpi().getNom()),
                        r -> r,
                        (first, second) -> first));

        List<com.QHSEAnalytics.shared.entity.KpiAnalysis> toSave = new java.util.ArrayList<>();

        for (com.QHSEAnalytics.shared.dto.response.AiKpiInsightResponse insight : structured.getKpiInsights()) {
            if (insight.getKpiName() == null || insight.getKpiName().isBlank()) continue;

            String key = TextNormalizer.normalizeForSearch(insight.getKpiName());
            com.QHSEAnalytics.shared.entity.KpiAnalysis analysis = existingByName.get(key);

            if (analysis == null) {
                // Créer un nouveau KpiAnalysis depuis la réponse structurée
                ResultatKpi resultat = resultatByName.get(key);
                String riskLevel = resultat != null && resultat.getNiveauVariation() != null
                        ? mapNiveauToRiskLevel(resultat.getNiveauVariation().name())
                        : "Modéré";

                analysis = com.QHSEAnalytics.shared.entity.KpiAnalysis.builder()
                        .importSession(session)
                        .kpiName(insight.getKpiName())
                        .riskLevel(riskLevel)
                        .riskJustification(insight.getRiskIfNotDone())
                        .issueDetected(insight.getInsight())
                        .problemeDetecte(insight.getInsight())
                        .actionImmediate(insight.getActionImmediate())
                        .immediateAction(insight.getActionImmediate())
                        .prioriteAction(urgencyToFr(insight.getUrgency()))
                        .immediatePriority(urgencyToFr(insight.getUrgency()))
                        .aiNote(insight.getInsight())
                        .noteFinale(insight.getInsight())
                        .requires8d(false)
                        .build();
                toSave.add(analysis);
                log.debug("[IA-Enrich] KpiAnalysis créé pour '{}' (session {})", insight.getKpiName(), session.getId());
            } else {
                // Enrichir les champs vides depuis la réponse structurée
                boolean updated = false;
                if (isBlankOrNull(analysis.getImmediateAction()) && insight.getActionImmediate() != null) {
                    analysis.setImmediateAction(insight.getActionImmediate());
                    analysis.setActionImmediate(insight.getActionImmediate());
                    updated = true;
                }
                if (isBlankOrNull(analysis.getRiskJustification()) && insight.getRiskIfNotDone() != null) {
                    analysis.setRiskJustification(insight.getRiskIfNotDone());
                    updated = true;
                }
                if (isBlankOrNull(analysis.getIssueDetected()) && insight.getInsight() != null) {
                    analysis.setIssueDetected(insight.getInsight());
                    analysis.setProblemeDetecte(insight.getInsight());
                    updated = true;
                }
                if (isBlankOrNull(analysis.getAiNote()) && insight.getInsight() != null) {
                    analysis.setAiNote(insight.getInsight());
                    updated = true;
                }
                if (updated) {
                    toSave.add(analysis);
                    log.debug("[IA-Enrich] KpiAnalysis enrichi pour '{}' (session {})", insight.getKpiName(), session.getId());
                }
            }
        }

        if (!toSave.isEmpty()) {
            kpiAnalysisRepository.saveAll(toSave);
            log.info("[IA-Enrich] {} KpiAnalysis créés/enrichis depuis l'analyse structurée (session {})",
                    toSave.size(), session.getId());
        }
    }

    private String mapNiveauToRiskLevel(String niveau) {
        if (niveau == null) return "Modéré";
        return switch (niveau.toUpperCase()) {
            case "CRITIQUE"     -> "Élevé";
            case "PRE_ESCALADE" -> "Élevé";
            case "MODERE"       -> "Modéré";
            case "FAIBLE"       -> "Faible";
            case "EXCELLENT"    -> "Faible";
            default             -> "Modéré";
        };
    }

    private String urgencyToFr(String urgency) {
        if (urgency == null) return "Moyenne";
        return switch (urgency.toUpperCase()) {
            case "HIGH"   -> "Haute";
            case "MEDIUM" -> "Moyenne";
            case "LOW"    -> "Basse";
            default       -> "Moyenne";
        };
    }

    private boolean isBlankOrNull(String s) {
        return s == null || s.isBlank();
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
                .periodeN1(resultat.getPeriodeN1())
                .periodeN(resultat.getPeriodeN())
                .build();
    }


    private String firstNonBlankText(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c.trim();
        }
        return "";
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
                                : TextNormalizer.normalizeForSearch(kpi.getKpiName()),
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

    private ImportSession loadSessionForAsync(Long importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable pour l'analyse async"));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
