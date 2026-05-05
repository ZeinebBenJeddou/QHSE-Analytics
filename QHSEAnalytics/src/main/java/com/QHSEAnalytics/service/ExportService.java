package com.QHSEAnalytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.exception.UserNotFoundException;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.dto.response.AdminAnalysteItemResponse;
import com.QHSEAnalytics.dto.response.AdminKpiCritiqueResponse;
import com.QHSEAnalytics.dto.response.AdminRepartitionResponse;
import com.QHSEAnalytics.dto.response.AdminStatsResponse;
import com.QHSEAnalytics.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.dto.response.ExportResponse;
import com.QHSEAnalytics.dto.response.LigneComparatifResponse;
import com.QHSEAnalytics.dto.response.ResumeCategorieResponse;
import com.QHSEAnalytics.entity.*;
import com.QHSEAnalytics.entity.KpiAnalysis;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.exception.AnalyseNotFoundException;
import com.QHSEAnalytics.exception.ImportNotFoundException;
import com.QHSEAnalytics.exception.ImportNotReadyException;
import com.QHSEAnalytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class ExportService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final AnalyseCategorieRepository analyseCategorieRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;
    private final UserRepository userRepository;
    private final DashboardAdminService dashboardAdminService;
    private final PdfTemplateBuilder pdfTemplateBuilder;
    private final PdfGeneratorService pdfGeneratorService;
    private final KpiAnalysisRepository kpiAnalysisRepository;

    @Transactional(readOnly = true)
    public ExportResponse exportAnalyste(Long userId, Long importId) {
        ImportSession session = importSessionRepository.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable"));

        if (!session.getStatut().isReadyForAi()) {
            throw new ImportNotReadyException("Impossible d'exporter un import non traite.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionIdWithKpi(importId).stream()
                .sorted(Comparator
                .comparing(this::safeCategorieCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(this::safeKpiOrdre, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<ResumeCategorieResponse> resumeCategories = buildResumeCategories(resultats);

        Map<String, KpiAnalysis> analysisMap = kpiAnalysisRepository
                .findByImportSessionIdOrderByIdAsc(importId)
                .stream()
            .filter(a -> normalizeKey(a.getKpiName()) != null)
            .collect(Collectors.toMap(a -> normalizeKey(a.getKpiName()), java.util.function.Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<LigneComparatifResponse> lignesComparatif = resultats.stream()
            .map(r -> {
                String normalizedKpiName = normalizeKey(safeKpiNom(r));
                KpiAnalysis analysis = findAnalysisForKpi(normalizedKpiName, analysisMap);
                log.debug("[Export] KPI '{}' (normalized='{}') -> analysis found: {}",
                    safeKpiNom(r), normalizedKpiName, analysis != null);
                return toLigneComparatif(r, analysis);
            })
                .toList();

        List<AnalyseCategorieResponse> analysesCategories = analyseCategorieRepository.findByImportSessionId(importId).stream()
                .sorted(Comparator.comparing(AnalyseCategorie::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAnalyseCategorieResponse)
                .toList();

        AnalyseGlobale analyseGlobale = analyseGlobaleRepository.findByImportSessionId(importId)
                .orElseThrow(() -> new AnalyseNotFoundException("Analyses IA non disponibles pour cet import."));

        AnalyseGlobaleResponse analyseGlobaleResponse = toAnalyseGlobaleResponse(analyseGlobale);

        String html = pdfTemplateBuilder.buildAnalysteTemplate(
                user.getNom(),
                user.getPrenom(),
                session.getPeriodeN1(),
                session.getPeriodeN(),
                LocalDateTime.now(),
                resumeCategories,
                lignesComparatif,
                analysesCategories,
                analyseGlobaleResponse
        );

        byte[] content = pdfGeneratorService.generatePdf(html);

        log.info("Rapport analyste exporte pour import {}", importId);

        return ExportResponse.builder()
                .content(content)
                .fileName(pdfGeneratorService.generateFileName("analyste", session.getPeriodeN1(), session.getPeriodeN()))
                .contentType(PDF_CONTENT_TYPE)
                .build();
    }

    @Transactional(readOnly = true)
    public ExportResponse exportAdmin() {
        AdminStatsResponse stats = dashboardAdminService.getStats();
        List<AdminAnalysteItemResponse> analystes = dashboardAdminService.getAnalystes();
        List<AdminKpiCritiqueResponse> kpisCritiques = dashboardAdminService.getTopKpisCritiques();
        AdminRepartitionResponse repartitionComplete = dashboardAdminService.getRepartitionComplete();

        String html = pdfTemplateBuilder.buildAdminTemplate(
                0,
                0,
                LocalDateTime.now(),
                stats,
                analystes,
            kpisCritiques,
            repartitionComplete
        );

        byte[] content = pdfGeneratorService.generatePdf(html);

        String fileName = "rapport_qhse_admin_global_" + LocalDate.now().format(FILE_DATE_FORMAT) + ".pdf";
        log.info("Rapport global admin exporte");

        return ExportResponse.builder()
                .content(content)
                .fileName(fileName)
                .contentType(PDF_CONTENT_TYPE)
                .build();
    }

    private List<ResumeCategorieResponse> buildResumeCategories(List<ResultatKpi> resultats) {
        Map<String, List<ResultatKpi>> byCategorie = resultats.stream()
                .collect(Collectors.groupingBy(this::safeCategorieCode, LinkedHashMap::new, Collectors.toList()));

        return byCategorie.values().stream()
                .map(this::toResumeCategorie)
                .sorted(Comparator.comparing(ResumeCategorieResponse::getCategorieCode, Comparator.nullsLast(String::compareTo)))
                .toList();
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
        String aiNote = analysis == null ? null : firstNonBlank(analysis.getNoteFinale(), analysis.getAiNote());
        LigneComparatifResponse.LigneComparatifResponseBuilder b = LigneComparatifResponse.builder()
                .kpiId(resultat.getKpi().getId())
                .kpiNom(safeKpiNom(resultat))
                .unite(resultat.getKpi().getUnite() == null ? null : resultat.getKpi().getUnite().name())
                .categorieCode(safeCategorieCode(resultat))
                .categorieLibelle(safeCategorieLibelle(resultat))
                .valeurN1(resultat.getValeurN1())
                .valeurN(resultat.getValeurN())
                .variationAbsolue(resultat.getVariationAbsolue())
                .variationRelative(resultat.getVariationRelative())
                .niveauVariation(resultat.getNiveauVariation() == null ? null : resultat.getNiveauVariation().name())
                .tendance(resultat.getTendance() == null ? null : resultat.getTendance().name())
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

    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
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
                log.debug("[Export] Fallback match for '{}' -> '{}'", normalizedKpiName, key);
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
                log.debug("[Export] Token match for '{}' -> '{}'", normalizedKpiName, key);
                return entry.getValue();
            }
        }

        return null;
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

    private String safeCategorieCode(ResultatKpi r) {
        if (r == null || r.getKpi() == null || r.getKpi().getCategorieKpi() == null || r.getKpi().getCategorieKpi().getCode() == null) {
            return "N/A";
        }
        return r.getKpi().getCategorieKpi().getCode();
    }

    private String safeCategorieLibelle(ResultatKpi r) {
        if (r == null || r.getKpi() == null || r.getKpi().getCategorieKpi() == null || r.getKpi().getCategorieKpi().getLibelle() == null) {
            return "Non categorise";
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

    private double round2(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.2f", value));
    }
}
