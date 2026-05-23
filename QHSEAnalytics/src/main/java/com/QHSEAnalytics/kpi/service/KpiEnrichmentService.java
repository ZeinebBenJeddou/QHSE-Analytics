package com.QHSEAnalytics.kpi.service;

import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.QHSEAnalytics.analytics.service.processing.GroqPromptBuilder;
import com.QHSEAnalytics.shared.dto.response.KpiAnalysisResult;
import com.QHSEAnalytics.shared.dto.response.KpiEnrichedResponse;
import com.QHSEAnalytics.shared.entity.*;
import com.QHSEAnalytics.shared.exception.ImportNotFoundException;
import com.QHSEAnalytics.shared.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class KpiEnrichmentService {

    private final ImportSessionRepository importSessionRepository;
    private final KpiImportPreviewRepository previewRepository;
    private final KpiAnalysisRepository analysisRepository;
    private final KpiRepository kpiRepository;
    private final LlmProviderChain llmProviderChain;
    private final GroqPromptBuilder groqPromptBuilder;
    private final ObjectProvider<KpiEnrichmentService> selfProvider;
    private final ObjectMapper objectMapper;


    @Transactional
    public void savePreviewRows(Long importSessionId, List<KpiPreviewInput> inputs) {
        ImportSession session = loadSession(importSessionId);
        previewRepository.deleteByImportSessionId(importSessionId);

        List<KpiImportPreview> rows = inputs.stream().map(input -> KpiImportPreview.builder()
                .importSession(session)
                .kpiName(input.getKpiName())
                .category(input.getCategory())
                .unit(input.getUnit())
                .definition(input.getDefinition())
                .valueN(input.getValueN())
                .valueN1(input.getValueN1())
                .status(input.getStatus())
                .commentaire(input.getCommentaire())
                .variationPercent(computeVariation(input.getValueN(), input.getValueN1()))
                .ecart(computeEcart(input.getValueN(), input.getValueN1()))
                .build()
        ).collect(Collectors.toList());

        previewRepository.saveAll(rows);
        log.info("[KpiEnrichment] Saved {} preview rows for session {}", rows.size(), importSessionId);
    }


    @Transactional
    public List<KpiEnrichedResponse> analyseAll(Long importSessionId, boolean force) {
        List<KpiImportPreview> previews = previewRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId);

        if (previews.isEmpty()) {
            log.warn("[KpiEnrichment] No preview rows found for session {}", importSessionId);
            return List.of();
        }

        List<KpiEnrichedResponse> results = new ArrayList<>();

        for (KpiImportPreview preview : previews) {
            try {
                results.add(selfProvider.getObject().analyseOne(preview, importSessionId, force));
            } catch (Exception ex) {
                log.error("[KpiEnrichment] Error analysing KPI '{}': {}", preview.getKpiName(), ex.getMessage(), ex);
                // continue to next KPI – do not stop the whole batch
                results.add(toEnrichedResponse(preview, null));
            }
        }

        return results;
    }


    @Transactional(readOnly = true)
    public List<KpiEnrichedResponse> getEnrichedView(Long importSessionId) {
        List<KpiImportPreview> previews = previewRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId);
        Map<String, KpiAnalysis> analysisByName = analysisRepository
                .findByImportSessionIdOrderByIdAsc(importSessionId)
                .stream()
            .filter(a -> normalizeKey(a.getKpiName()) != null)
            .collect(Collectors.toMap(a -> normalizeKey(a.getKpiName()), a -> a, (a, b) -> a, LinkedHashMap::new));

        return previews.stream()
            .map(p -> toEnrichedResponse(p, analysisByName.get(normalizeKey(p.getKpiName()))))
                .collect(Collectors.toList());
    }


        @Cacheable(
            value = "kpiAnalysis",
            key = "#preview.getKpiName().toLowerCase().trim() + '_' + T(java.util.Objects).hashCode(#preview.getValueN()) + '_' + T(java.util.Objects).hashCode(#preview.getValueN1())",
            unless = "#result == null",
            condition = "!#force"
        )
        public KpiEnrichedResponse analyseOne(KpiImportPreview preview, Long importSessionId, boolean force) {

        if (!force) {
            Optional<KpiAnalysis> existing = analysisRepository
                    .findByImportSessionIdAndKpiName(importSessionId, preview.getKpiName());
            if (existing.isPresent()) {
                log.debug("[KpiEnrichment] Skipping already-analysed KPI '{}'", preview.getKpiName());
                return toEnrichedResponse(preview, existing.get());
            }
        }


        String prompt = buildEnrichmentPrompt(preview);


        String rawJson = llmProviderChain.generate(prompt);


        KpiAnalysisResult result = parseAnalysisResult(rawJson, preview.getKpiName());


        KpiAnalysis analysis = saveAnalysis(preview.getImportSession(), preview.getKpiName(), result);


        return toEnrichedResponse(preview, analysis);
    }

    private String buildEnrichmentPrompt(KpiImportPreview preview) {
        double valeurN = preview.getValueN() == null ? 0d : preview.getValueN();
        double valeurN1 = preview.getValueN1() == null ? 0d : preview.getValueN1();
        double variation = preview.getVariationPercent() == null ? 0d : preview.getVariationPercent();

        int currentYear = LocalDateTime.now().getYear();
        int periodeN = currentYear;
        int periodeN1 = currentYear - 1;

        return groqPromptBuilder.buildKpiPrompt(
                preview.getKpiName(),
                preview.getDefinition(),
                preview.getUnit(),
                preview.getCategory(),
                valeurN1,
                valeurN,
                variation,
                null,
                null,
                periodeN1,
                periodeN
        );
    }

    private KpiAnalysisResult parseAnalysisResult(String rawJson, String kpiName) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("[KpiEnrichment] AI returned empty response for KPI '{}'", kpiName);
            return buildFallbackResult(kpiName);
        }

        String cleaned = cleanAiResponse(rawJson);
        try {
            log.debug("[KpiEnrichment] Cleaned KPI analysis response preview: {}",
                    cleaned.length() > 300 ? cleaned.substring(0, 300) : cleaned);

            JsonNode root = objectMapper.readTree(cleaned);

            KpiAnalysisResult result = new KpiAnalysisResult();
            result.setKpiName(kpiName);


            String rawRiskLevel = textOrNull(root, "risqueIa") != null ? textOrNull(root, "risqueIa") : textOrNull(root, "riskLevel");
            result.setRiskLevel(sanitizeRiskLevel(rawRiskLevel));

            String identification = truncateField(textOrNull(root, "identificationRisque"), 1000);
            String noteIa = truncateField(textOrNull(root, "noteIa"), 1000);
            result.setRiskJustification(identification != null ? identification : (noteIa != null ? noteIa : truncateField(textOrNull(root, "riskJustification"), 1000)));
            result.setIdentificationRisque(identification);

            result.setObjectiveReached(root.path("objectiveAtteint").asBoolean(root.path("objectiveReached").asBoolean(false)));
            result.setImprovementDetected(root.path("ameliorationDetectee").asBoolean(root.path("improvementDetected").asBoolean(false)));

            String problemeDetecte = truncateField(textOrNull(root, "problemeDetecte"), 1000);
            result.setIssueDetected(problemeDetecte != null ? problemeDetecte : truncateField(textOrNull(root, "issueDetected"), 1000));
            result.setProblemeDetecte(problemeDetecte);

            result.setCorrectiveAction(truncateField(textOrNull(root, "actionsCorrectives") != null ? textOrNull(root, "actionsCorrectives") : textOrNull(root, "correctiveAction"), 1000));
            String actionsPreventives = truncateField(textOrNull(root, "actionsPreventives"), 1000);
            result.setPreventiveAction(actionsPreventives != null ? actionsPreventives : truncateField(textOrNull(root, "preventiveAction"), 1000));
            result.setActionsPreventives(actionsPreventives);

            String actionImmediate = truncateField(textOrNull(root, "actionImmediate"), 500);
            result.setImmediateAction(actionImmediate != null ? actionImmediate : truncateField(textOrNull(root, "immediateAction"), 500));
            result.setActionImmediate(actionImmediate);
            String prioriteAction = textOrNull(root, "prioriteAction");
            result.setImmediatePriority(prioriteAction != null ? prioriteAction : textOrNull(root, "immediatePriority"));
            result.setPrioriteAction(prioriteAction);

            result.setRequires8d(root.path("methode8D").isObject() || root.path("requires8d").asBoolean(false));


            JsonNode methode8D = root.path("methode8D");
            if (methode8D.isMissingNode() || methode8D.isNull()) {
                methode8D = root.path("eightDDetails");
            }
            if (!methode8D.isMissingNode() && !methode8D.isNull() && methode8D.isObject()) {
                String methode8DJson = objectMapper.writeValueAsString(methode8D);
                result.setEightDDetails(methode8DJson);
                result.setMethode8D(methode8DJson);
            }


            String noteFinale = truncateField(textOrNull(root, "noteFinale"), 1000);
            result.setAiNote(noteFinale != null ? noteFinale : truncateField(textOrNull(root, "aiNote"), 1000));
            result.setNoteFinale(noteFinale);

            return result;

        } catch (Exception ex) {
            String preview = cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
            log.warn("[KpiEnrichment] JSON parse failed for '{}': {} | Raw preview: {}", kpiName, ex.getMessage(), preview);
            return buildFallbackResult(kpiName);
        }
    }

    private String cleanAiResponse(String rawResponse) {
        if (rawResponse == null) return null;
        String cleaned = rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return cleaned;
    }

    private KpiAnalysisResult buildFallbackResult(String kpiName) {
        return KpiAnalysisResult.builder()
                .kpiName(kpiName)
                .riskLevel("Modéré")
                .riskJustification("Analyse IA indisponible.")
            .identificationRisque("Analyse IA indisponible.")
                .issueDetected(null)
            .problemeDetecte(null)
                .correctiveAction("Vérification manuelle recommandée.")
                .preventiveAction("Maintenir la surveillance régulière.")
            .actionsPreventives("Maintenir la surveillance régulière.")
                .immediateAction("Revoir les données manuellement.")
            .actionImmediate("Revoir les données manuellement.")
                .immediatePriority("Moyenne")
            .prioriteAction("Moyenne")
                .requires8d(false)
                .eightDDetails(null)
            .methode8D(null)
                .aiNote("L'analyse automatique n'a pas pu être générée. Une revue manuelle de ce KPI est recommandée.")
            .noteFinale("L'analyse automatique n'a pas pu être générée. Une revue manuelle de ce KPI est recommandée.")
                .build();
    }

    @Transactional
    protected KpiAnalysis saveAnalysis(ImportSession session, String kpiName, KpiAnalysisResult result) {
        KpiAnalysis existing = analysisRepository
                .findByImportSessionIdAndKpiName(session.getId(), kpiName)
                .orElse(null);

        if (existing != null && isFallbackResult(result)) {
            log.info("[KpiEnrichment] Keeping existing analysis for KPI '{}' because AI returned fallback content", kpiName);
            return existing;
        }


        KpiAnalysis analysis = existing != null
                ? existing
                : KpiAnalysis.builder().importSession(session).kpiName(kpiName).build();

        analysis.setRiskLevel(firstNonBlank(result.getRiskLevel(), analysis.getRiskLevel()));
        analysis.setRiskJustification(firstNonBlank(result.getRiskJustification(), analysis.getRiskJustification()));
        analysis.setIdentificationRisque(firstNonBlank(result.getIdentificationRisque(), analysis.getIdentificationRisque()));
        analysis.setObjectiveReached(result.getObjectiveReached() != null ? result.getObjectiveReached() : analysis.getObjectiveReached());
        analysis.setImprovementDetected(result.getImprovementDetected() != null ? result.getImprovementDetected() : analysis.getImprovementDetected());
        analysis.setIssueDetected(firstNonBlank(result.getIssueDetected(), analysis.getIssueDetected()));
        analysis.setProblemeDetecte(firstNonBlank(result.getProblemeDetecte(), analysis.getProblemeDetecte()));
        analysis.setCorrectiveAction(firstNonBlank(result.getCorrectiveAction(), analysis.getCorrectiveAction()));
        analysis.setPreventiveAction(firstNonBlank(result.getPreventiveAction(), analysis.getPreventiveAction()));
        analysis.setActionsPreventives(firstNonBlank(result.getActionsPreventives(), analysis.getActionsPreventives()));
        analysis.setImmediateAction(firstNonBlank(result.getImmediateAction(), analysis.getImmediateAction()));
        analysis.setActionImmediate(firstNonBlank(result.getActionImmediate(), analysis.getActionImmediate()));
        analysis.setImmediatePriority(firstNonBlank(result.getImmediatePriority(), analysis.getImmediatePriority()));
        analysis.setPrioriteAction(firstNonBlank(result.getPrioriteAction(), analysis.getPrioriteAction()));
        analysis.setRequires8d(result.isRequires8d() || analysis.isRequires8d());
        analysis.setEightDDetails(firstNonBlank(result.getEightDDetails(), analysis.getEightDDetails()));
        analysis.setMethode8D(firstNonBlank(result.getMethode8D(), analysis.getMethode8D()));
        analysis.setAiNote(firstNonBlank(result.getAiNote(), analysis.getAiNote()));
        analysis.setNoteFinale(firstNonBlank(result.getNoteFinale(), analysis.getNoteFinale()));

        return analysisRepository.save(analysis);
    }

    private boolean isFallbackResult(KpiAnalysisResult result) {
        if (result == null) {
            return true;
        }
        return "Analyse IA indisponible.".equals(result.getRiskJustification())
                || (result.getAiNote() != null && result.getAiNote().contains("n'a pas pu être générée"));
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

    private KpiEnrichedResponse toEnrichedResponse(KpiImportPreview preview, KpiAnalysis analysis) {
        KpiEnrichedResponse.KpiEnrichedResponseBuilder b = KpiEnrichedResponse.builder()
                .previewId(preview.getId())
                .importSessionId(preview.getImportSession().getId())
                .kpiName(preview.getKpiName())
                .category(preview.getCategory())
                .unit(preview.getUnit())
                .definition(preview.getDefinition())
                .valueN(preview.getValueN())
                .valueN1(preview.getValueN1())
                .status(preview.getStatus())
                .commentaire(preview.getCommentaire())
                .variationPercent(preview.getVariationPercent())
                .ecart(preview.getEcart())
                .createdAt(preview.getCreatedAt());

        if (analysis != null) {
            b.analysisId(analysis.getId())
             .riskLevel(analysis.getRiskLevel())
             .riskJustification(analysis.getRiskJustification())
             .objectiveReached(analysis.getObjectiveReached())
             .improvementDetected(analysis.getImprovementDetected())
             .issueDetected(analysis.getIssueDetected())
             .correctiveAction(analysis.getCorrectiveAction())
             .preventiveAction(analysis.getPreventiveAction())
             .immediateAction(analysis.getImmediateAction())
             .immediatePriority(analysis.getImmediatePriority())
             .requires8d(analysis.isRequires8d())
             .eightDDetails(analysis.getEightDDetails())
             .aiNote(analysis.getAiNote());
        }

        return b.build();
    }

    private ImportSession loadSession(Long id) {
        return importSessionRepository.findById(id)
                .orElseThrow(() -> new ImportNotFoundException("Import introuvable: " + id));
    }

    private Double computeVariation(Double n, Double n1) {
        if (n == null || n1 == null || n1 == 0.0) return null;
        return ((n - n1) / Math.abs(n1)) * 100.0;
    }

    private Double computeEcart(Double n, Double n1) {
        if (n == null || n1 == null) return null;
        return n - n1;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode f = node.path(field);
        if (f.isMissingNode() || f.isNull()) return null;
        String v = f.asText("").trim();
        return v.isEmpty() ? null : v;
    }

    private String truncateField(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) + "…" : value;
    }

    private static final Set<String> VALID_RISK_LEVELS = Set.of("élevé", "modéré", "faible");

    private String sanitizeRiskLevel(String raw) {
        if (raw == null || raw.isBlank()) return "Modéré";
        String normalized = Normalizer
                .normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        if (normalized.equals("eleve") || normalized.startsWith("elev") || normalized.equals("haut") || normalized.equals("high") || normalized.equals("critique")) {
            return "Élevé";
        }
        if (normalized.equals("modere") || normalized.startsWith("moder") || normalized.equals("medium") || normalized.equals("moyen")) {
            return "Modéré";
        }
        if (normalized.equals("faible") || normalized.equals("low") || normalized.equals("bas")) {
            return "Faible";
        }
        log.warn("[KpiEnrichment] Unrecognised riskLevel '{}' — defaulting to Modéré", raw);
        return "Modéré";
    }



    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class KpiPreviewInput {
        private String kpiName;
        private String category;
        private String unit;
        private String definition;
        private Double valueN;
        private Double valueN1;
        private String status;
        private String commentaire;
    }
}
