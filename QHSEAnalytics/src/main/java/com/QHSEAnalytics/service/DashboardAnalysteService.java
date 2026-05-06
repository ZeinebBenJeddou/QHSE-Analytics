package com.QHSEAnalytics.service;

import com.QHSEAnalytics.dto.response.AnalyseCompleteResponse;
import com.QHSEAnalytics.dto.response.BarreGroupeeData;
import com.QHSEAnalytics.dto.response.ComparatifTableauResponse;
import com.QHSEAnalytics.dto.response.GraphiquesDataResponse;
import com.QHSEAnalytics.dto.response.HistoriqueAnalysteResponse;
import com.QHSEAnalytics.dto.response.HistoriqueItemResponse;
import com.QHSEAnalytics.dto.response.KpiDegrade;
import com.QHSEAnalytics.dto.response.LigneComparatifResponse;
import com.QHSEAnalytics.dto.response.RadarPoint;
import com.QHSEAnalytics.dto.response.ResumeAnalysteResponse;
import com.QHSEAnalytics.dto.response.ResumeCategorieResponse;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.KpiAnalysis;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.exception.ImportNotReadyException;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiAnalysisRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAnalysteService {

    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final KpiAnalysisRepository kpiAnalysisRepository;

    private final AnalyseIaService analyseIaService;

    public ResumeAnalysteResponse getResume(Long userId) {
        ImportSession session = importSessionRepository
                .findTopByUserIdAndStatutInOrderByCreatedAtDesc(userId, List.of(ImportStatut.CALCULATED, ImportStatut.READY_FOR_AI, ImportStatut.TRAITE))
                .orElseThrow(() -> new ImportNotFoundException("Aucune analyse disponible. Veuillez importer des données."));

        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdWithKpi(session.getId());
        Map<String, List<ResultatKpi>> byCategorie = groupByCategorie(resultats);

        List<ResumeCategorieResponse> resumeCategories = byCategorie.values().stream()
                .map(this::toResumeCategorie)
                .sorted(Comparator.comparing(ResumeCategorieResponse::getCategorieCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        int critiques = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count();
        int moderes = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.MODERE).count();
        int faibles = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.FAIBLE).count();

        return ResumeAnalysteResponse.builder()
                .dernierImportId(session.getId())
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .dateAnalyse(session.getUpdatedAt())
                .resumeCategories(resumeCategories)
                .nombreTotalKpis(resultats.size())
                .nombreTotalCritiques(critiques)
                .nombreTotalModeres(moderes)
                .nombreTotalFaibles(faibles)
                .build();
    }

    public ComparatifTableauResponse getComparatif(Long userId, Long importId) {
        ImportSession session = loadOwnedCompletedSession(userId, importId);
        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdWithKpi(importId);

        // Build a lookup map of Gemini AI analysis by KPI name
        List<KpiAnalysis> analyses = kpiAnalysisRepository
                .findByImportSessionIdOrderByIdAsc(importId);
        log.info("[Dashboard] Found {} KpiAnalysis records for import {}", analyses.size(), importId);
        for (KpiAnalysis a : analyses) {
            log.debug("[Dashboard] KpiAnalysis: kpiName='{}', aiNote='{}', noteFinale='{}'", 
                a.getKpiName(), a.getAiNote(), a.getNoteFinale());
        }
        
        Map<String, KpiAnalysis> analysisMap = analyses
                .stream()
                .filter(a -> normalizeKey(a.getKpiName()) != null)
                .collect(Collectors.toMap(a -> normalizeKey(a.getKpiName()), a -> a, (a, b) -> a, LinkedHashMap::new));
        log.info("[Dashboard] Built analysis map with {} normalized keys", analysisMap.size());

        List<ResultatKpi> sorted = resultats.stream()
                .sorted(Comparator
                        .comparing((ResultatKpi r) -> safeCategorieCode(r), Comparator.nullsLast(String::compareTo))
                        .thenComparing(r -> safeKpiOrdre(r), Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<LigneComparatifResponse> lignes = sorted.stream()
            .map(r -> {
                String normalizedKpiName = normalizeKey(safeKpiNom(r));
                KpiAnalysis analysis = findAnalysisForKpi(normalizedKpiName, analysisMap);
                log.debug("[Dashboard] KPI '{}' (normalized='{}') -> analysis found: {}", 
                    safeKpiNom(r), normalizedKpiName, analysis != null);
                return toLigneComparatif(r, analysis);
            })
                .toList();
        int critiques = (int) sorted.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count();
        int moderes = (int) sorted.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.MODERE).count();
        int faibles = (int) sorted.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.FAIBLE).count();

        return ComparatifTableauResponse.builder()
                .importId(session.getId())
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .dateAnalyse(session.getUpdatedAt())
                .lignes(lignes)
                .nombreCritiques(critiques)
                .nombreModeres(moderes)
                .nombreFaibles(faibles)
                .build();
    }

    public GraphiquesDataResponse getGraphiques(Long userId, Long importId) {
        loadOwnedCompletedSession(userId, importId);
        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdWithKpi(importId);
        Map<String, List<ResultatKpi>> byCategorie = groupByCategorie(resultats);

        List<BarreGroupeeData> barres = byCategorie.values().stream()
                .map(list -> BarreGroupeeData.builder()
                        .categorie(safeCategorieLibelle(list.get(0)))
                        .valeurMoyenneN1(round2(list.stream().mapToDouble(ResultatKpi::getValeurN1).average().orElse(0d)))
                        .valeurMoyenneN(round2(list.stream().mapToDouble(ResultatKpi::getValeurN).average().orElse(0d)))
                        .variationMoyenne(round2(list.stream().mapToDouble(ResultatKpi::getVariationRelative).average().orElse(0d)))
                        .build())
                .toList();

        List<RadarPoint> radar = byCategorie.values().stream()
                .map(list -> {
                    long total = list.size();
                    long critiques = list.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count();

                    double score = total == 0 ? 100d : (100d - (((double) critiques / (double) total) * 100d));
                    return RadarPoint.builder()
                            .categorie(safeCategorieLibelle(list.get(0)))
                    .score(round1(Math.max(0d, score)))
                            .build();
                })
                .toList();

        List<KpiDegrade> topDegrades = resultats.stream()
                .sorted(Comparator
                        .comparing(ResultatKpi::getVariationRelative, Comparator.nullsLast(Double::compareTo))
                        .thenComparing((ResultatKpi r) -> severityRank(r.getNiveauVariation())))
                .limit(5)
                .map(resultat -> KpiDegrade.builder()
                        .kpiNom(safeKpiNom(resultat))
                        .categorieCode(safeCategorieCode(resultat))
                        .variationRelative(round2(resultat.getVariationRelative()))
                        .niveauVariation(resultat.getNiveauVariation() == null ? null : resultat.getNiveauVariation().name())
                        .tendance(resultat.getTendance() == null ? null : resultat.getTendance().name())
                        .build())
                .toList();

        return GraphiquesDataResponse.builder()
                .barresGroupees(barres)
                .radarData(radar)
                .topKpisDegrades(topDegrades)
                .build();
    }

    public HistoriqueAnalysteResponse getHistorique(Long userId) {
        List<ImportSession> sessions = importSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<ResultatKpi> resultatsUtilisateur = resultatKpiRepository.findByUserId(userId);
        Map<Long, Long> critiquesBySession = resultatsUtilisateur.stream()
            .filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE && r.getImportSession() != null)
            .collect(Collectors.groupingBy(r -> r.getImportSession().getId(), Collectors.counting()));

        List<HistoriqueItemResponse> items = sessions.stream().map(session -> {
            int critiques = session.getStatut().isReadyForAi()
                ? critiquesBySession.getOrDefault(session.getId(), 0L).intValue()
                : 0;

            return HistoriqueItemResponse.builder()
                    .importId(session.getId())
                    .nomFichier(session.getNomFichier())
                    .periodeN1(session.getPeriodeN1())
                    .periodeN(session.getPeriodeN())
                    .statut(session.getStatut().name())
                    .messageErreur(session.getMessageErreur())
                    .nombreKpisCritiques(critiques)
                    .dateImport(session.getCreatedAt())
                    .build();
        }).toList();

        int totalImports = sessions.size();
        int totalTraites = (int) sessions.stream().filter(s -> s.getStatut().isReadyForAi()).count();
        int totalErreurs = (int) sessions.stream().filter(s -> s.getStatut() == ImportStatut.ERREUR).count();

        return HistoriqueAnalysteResponse.builder()
                .items(items)
                .totalImports(totalImports)
                .totalTraites(totalTraites)
                .totalErreurs(totalErreurs)
                .build();
    }

    public AnalyseCompleteResponse getAnalysesIa(Long userId, Long importId) {
        loadOwnedTraiteSession(userId, importId);
        return analyseIaService.getAnalyseComplete(importId, userId, false);
    }

    private ImportSession loadOwnedCompletedSession(Long userId, Long importId) {
        ImportSession session = importSessionRepository.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
        if (!session.getStatut().canViewComparatif()) {
            throw new ImportNotReadyException(createNotReadyMessage(session));
        }
        return session;
    }

    private ImportSession loadOwnedTraiteSession(Long userId, Long importId) {
        ImportSession session = importSessionRepository.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));
        if (!session.getStatut().canViewAnalysesIa()) {
            throw new ImportNotReadyException(createNotReadyMessage(session));
        }
        return session;
    }

    private String createNotReadyMessage(ImportSession session) {
        if (session.getStatut().isFailed()) {
            return "Import échoué.";
        }
        if (session.getStatut().isProcessing()) {
            return "Import en cours. Veuillez réessayer plus tard.";
        }
        return "Import non traité.";
    }

    private ResumeCategorieResponse toResumeCategorie(List<ResultatKpi> resultats) {
        int critiques = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count();
        int moderes = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.MODERE).count();
        int faibles = (int) resultats.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.FAIBLE).count();

        String couleur;
        if (critiques == 0) {
            couleur = "VERT";
        } else if (critiques >= moderes) {
            couleur = "ROUGE";
        } else {
            couleur = "ORANGE";
        }

        ResultatKpi first = resultats.get(0);
        return ResumeCategorieResponse.builder()
                .categorieCode(safeCategorieCode(first))
                .categorieLibelle(safeCategorieLibelle(first))
                .variationMoyenne(round2(resultats.stream().mapToDouble(ResultatKpi::getVariationRelative).average().orElse(0d)))
                .nombreKpisCritiques(critiques)
                .nombreKpisModeres(moderes)
                .nombreKpisFaibles(faibles)
                .couleur(couleur)
                .build();
    }

    private LigneComparatifResponse toLigneComparatif(ResultatKpi resultat, KpiAnalysis analysis) {
        String kpiNom = safeKpiNom(resultat);
        String aiNote = analysis == null ? null : firstNonBlank(analysis.getNoteFinale(), analysis.getAiNote());
        log.debug("[Dashboard] KPI '{}' aiNote='{}'", kpiNom, aiNote);

        LigneComparatifResponse.LigneComparatifResponseBuilder b = LigneComparatifResponse.builder()
                .kpiId(resultat.getKpi().getId())
            .kpiNom(kpiNom)
                .unite(resultat.getKpi().getUnite() == null ? null : resultat.getKpi().getUnite().name())
                .categorieCode(safeCategorieCode(resultat))
                .categorieLibelle(safeCategorieLibelle(resultat))
                .valeurN1(resultat.getValeurN1())
                .valeurN(resultat.getValeurN())
                .variationAbsolue(resultat.getVariationAbsolue())
                .variationRelative(resultat.getVariationRelative())
                .niveauVariation(resultat.getNiveauVariation() == null ? null : resultat.getNiveauVariation().name())
                .tendance(resultat.getTendance() == null ? null : resultat.getTendance().name())
                .status(resultat.getStatus())
                .commentaire(resultat.getCommentaire())
                .analyseIa(resultat.getAnalyseIa());

        if (analysis != null) {
            b.riskLevel(analysis.getRiskLevel())
             .riskJustification(analysis.getRiskJustification())
             .identificationRisque(analysis.getIdentificationRisque())
             .objectiveReached(analysis.getObjectiveReached())
             .improvementDetected(analysis.getImprovementDetected())
             .issueDetected(analysis.getIssueDetected())
             .problemeDetecte(analysis.getProblemeDetecte())
             .correctiveAction(analysis.getCorrectiveAction())
             .preventiveAction(analysis.getPreventiveAction())
             .actionsPreventives(analysis.getActionsPreventives())
             .immediateAction(analysis.getImmediateAction())
             .actionImmediate(analysis.getActionImmediate())
             .immediatePriority(analysis.getImmediatePriority())
             .prioriteAction(analysis.getPrioriteAction())
             .requires8d(analysis.isRequires8d())
             .eightDDetails(analysis.getEightDDetails())
             .methode8D(analysis.getMethode8D())
             .aiNote(aiNote)
             .noteFinale(analysis.getNoteFinale());
        }

        return b.build();
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replace("’", "'")
                .replace("‘", "'")
                .replace("`", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replaceAll("[^\\p{Alnum}'\"]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private KpiAnalysis findAnalysisForKpi(String normalizedKpiName, Map<String, KpiAnalysis> analysisMap) {
        if (normalizedKpiName == null || analysisMap == null || analysisMap.isEmpty()) {
            return null;
        }

        KpiAnalysis directMatch = analysisMap.get(normalizedKpiName);
        if (directMatch != null) {
            return directMatch;
        }

        for (Map.Entry<String, KpiAnalysis> entry : analysisMap.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if (key.contains(normalizedKpiName) || normalizedKpiName.contains(key)) {
                log.debug("[Dashboard] Fallback match for '{}' -> '{}'", normalizedKpiName, key);
                return entry.getValue();
            }
        }

        Set<String> targetWords = new HashSet<>(List.of(normalizedKpiName.split("\\s+")));
        for (Map.Entry<String, KpiAnalysis> entry : analysisMap.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            Set<String> candidateWords = new HashSet<>(List.of(key.split("\\s+")));
            if (candidateWords.containsAll(targetWords) || targetWords.containsAll(candidateWords)) {
                log.debug("[Dashboard] Token match for '{}' -> '{}'", normalizedKpiName, key);
                return entry.getValue();
            }
        }

        return null;
    }

    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private Map<String, List<ResultatKpi>> groupByCategorie(List<ResultatKpi> resultats) {
        return resultats.stream().collect(Collectors.groupingBy(this::safeCategorieCode, LinkedHashMap::new, Collectors.toList()));
    }

    private String safeCategorieCode(ResultatKpi r) {
        if (r == null || r.getKpi() == null || r.getKpi().getCategorieKpi() == null || r.getKpi().getCategorieKpi().getCode() == null) {
            return "N/A";
        }
        return r.getKpi().getCategorieKpi().getCode();
    }

    private String safeCategorieLibelle(ResultatKpi r) {
        if (r == null || r.getKpi() == null || r.getKpi().getCategorieKpi() == null || r.getKpi().getCategorieKpi().getLibelle() == null) {
            return "Non catégorisé";
        }
        return r.getKpi().getCategorieKpi().getLibelle();
    }

    private String safeKpiNom(ResultatKpi r) {
        if (r == null || r.getKpi() == null || r.getKpi().getNom() == null) {
            return "KPI inconnu";
        }
        return r.getKpi().getNom();
    }

    private Integer safeKpiOrdre(ResultatKpi r) {
        if (r == null || r.getKpi() == null) {
            return Integer.MAX_VALUE;
        }
        return r.getKpi().getOrdre();
    }

    private int severityRank(NiveauVariation niveauVariation) {
        if (niveauVariation == null) {
            return 99;
        }
        return switch (niveauVariation) {
            case CRITIQUE -> 0;
            case MODERE -> 1;
            case FAIBLE -> 2;
        };
    }

    private double round1(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.1f", value));
    }

    private double round2(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.2f", value));
    }
}