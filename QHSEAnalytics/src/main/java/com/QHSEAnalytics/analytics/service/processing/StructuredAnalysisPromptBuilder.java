package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.analytics.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StructuredAnalysisPromptBuilder {

    public static final String GENERIC_RETRY_SUFFIX =
            "\n\nATTENTION : ta réponse précédente contenait des champs vides ou génériques. " +
            "Cette fois, chaque champ DOIT contenir une analyse réelle et détaillée. " +
            "Les valeurs 'N/A', vides ou inférieures à 20 mots sont refusées. " +
            "Sois précis, concret et spécifique à chaque KPI.";

    private static final String SYSTEM_PROMPT =
            "Tu es un expert QHSE senior maîtrisant ISO 9001, ISO 14001 et ISO 45001. " +
            "Tu analyses des indicateurs de performance QHSE et tu fournis des diagnostics " +
            "précis, des causes probables documentées et des plans d'action concrets et actionnables. " +
            "RÈGLES ABSOLUES :\n" +
            "1. Réponds UNIQUEMENT avec du JSON valide — aucun texte avant ou après.\n" +
            "2. Chaque champ texte doit contenir une analyse réelle et spécifique au KPI fourni " +
            "   (minimum 20 mots). Les valeurs génériques comme 'N/A', 'À analyser', " +
            "   'Non disponible' ou les champs vides sont INTERDITS.\n" +
            "3. actionImmediate doit décrire une action concrète avec un verbe d'action " +
            "   (ex : 'Organiser une réunion de revue...', 'Mettre en place un suivi...').\n" +
            "4. insight doit expliquer pourquoi cette variation est significative dans un " +
            "   contexte QHSE réel.\n" +
            "5. Tu dois produire exactement un objet kpiInsights pour chaque KPI fourni.\n" +
            "6. Réponds en français.\n";

    private static final String FEW_SHOT_EXAMPLE =
            "\n\nEXEMPLE DE globalSummary ATTENDU — un seul paragraphe narratif fluide, rédigé comme un rapport d'expert QHSE senior :\n" +
            "\"L'analyse QHSE portant sur la période 2025 → 2026, couvrant 12 indicateurs répartis sur 4 catégories, révèle un score global de 42/100 (À SURVEILLER) avec 8 KPIs classés CRITIQUE. " +
            "La catégorie Qualité est la plus dégradée (score 20/100, 3 critiques), notamment le First Pass Yield qui chute de −18,5 % (91 % → 74,2 %), franchissant le seuil critique ISO 9001. " +
            "Sur le plan sécurité, le Taux d'Absentéisme progresse de +32 % (4,1 % → 5,4 %), signal d'alerte sur les conditions de travail au sens ISO 45001, tandis que le Taux de Fréquence des Accidents affiche +15 %, exposant l'entreprise à un risque de non-conformité DREAL. " +
            "Le Délai Moyen de Livraison (+28 %, 3,6 → 4,6 jours) génère un risque contractuel croissant. " +
            "Il est impératif d'engager dans les 48h une revue sécurité d'urgence sur les postes à risque et de lancer dans la semaine un plan de réduction des défauts pour remonter le First Pass Yield au-dessus de 85 % ; sans intervention rapide, la trajectoire actuelle conduira à une situation de non-conformité multi-normes dans les 2 à 3 prochains mois.\"\n\n" +
            "EXEMPLE DE RÉPONSE ATTENDUE POUR UN KPI (respecte ce niveau de détail) :\n" +
            "{\n" +
            "  \"kpiId\": 42,\n" +
            "  \"kpiName\": \"Taux de Fréquence des Accidents (TF1)\",\n" +
            "  \"confidence\": 87,\n" +
            "  \"insight\": \"Le TF1 a augmenté de 28% entre N-1 et N, passant de 2.5 à 3.2. " +
            "Cette hausse dépasse le seuil critique ISO 45001 et indique une dégradation " +
            "significative des conditions de sécurité, probablement liée à une augmentation " +
            "de la cadence de production ou à un déficit de formation.\",\n" +
            "  \"probableCauses\": [\n" +
            "    \"Augmentation de la cadence de production sans adaptation des mesures de sécurité\",\n" +
            "    \"Déficit de formation sécurité pour les nouveaux opérateurs\",\n" +
            "    \"Sous-déclaration des presqu'accidents réduisant les actions préventives\"\n" +
            "  ],\n" +
            "  \"actionImmediate\": \"Suspendre les postes à risque identifiés et organiser " +
            "une revue sécurité d'urgence avec les responsables de ligne dans les 48h.\",\n" +
            "  \"urgency\": \"HIGH\",\n" +
            "  \"ownerRole\": \"Responsable HSE\",\n" +
            "  \"dueHorizon\": \"48h\",\n" +
            "  \"successMetric\": \"TF1 revient sous 2.5 dans les 3 prochains mois\",\n" +
            "  \"riskIfNotDone\": \"Risque d'accident grave, pénalités réglementaires DREAL\",\n" +
            "  \"note\": \"Indicateur sous surveillance prioritaire — nécessite un reporting " +
            "hebdomadaire à la direction.\"\n" +
            "}\n" +
            "DONNÉES RÉELLES À ANALYSER (produis le même niveau de détail pour chaque KPI) :\n";

    private final RagSearchService ragSearchService;
    private final PromptSanitizer promptSanitizer;

    public String buildPrompt(List<KpiCalculatedDTO> kpis) {
        return buildPrompt(kpis, kpis == null ? 0 : kpis.size(), null);
    }

    /**
     * @param kpis            KPIs in this chunk (what the LLM must analyse)
     * @param totalKpiCount   total number of KPIs in the session (used only for globalSummary context)
     * @param allKpis         full list used to compute session-wide scores; falls back to kpis when null
     */
    public String buildPrompt(List<KpiCalculatedDTO> kpis, int totalKpiCount, List<KpiCalculatedDTO> allKpis) {
        if (kpis == null || kpis.isEmpty()) {
            throw new IllegalArgumentException("At least one KPI is required to build a structured IA prompt.");
        }

        List<KpiCalculatedDTO> orderedKpis = kpis.stream()
                .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage() == null ? 0.0 : k.getVariationPercentage())))
                .collect(Collectors.toList());

        // Compute scores on the full session KPI list so globalSummary always reflects the real totals
        List<KpiCalculatedDTO> scoreSource = (allKpis != null && !allKpis.isEmpty()) ? allKpis : orderedKpis;
        ScoreSummary scores = computeScores(scoreSource);
        int reportedTotal = totalKpiCount > 0 ? totalKpiCount : orderedKpis.size();

        StringBuilder prompt = new StringBuilder();
        prompt.append("SYSTEM:\n");
        prompt.append(SYSTEM_PROMPT);
        prompt.append(String.format("Nombre exact d'objets kpiInsights attendus : %d.\n\n", orderedKpis.size()));

        prompt.append("USER:\n");
        prompt.append("You have the following targeted context sources and KPI results.\n");
        prompt.append("Use the provided sources to answer. Do not use any external knowledge.\n\n");

        // Resolve actual years from KPI data
        String periodeN1Str = scoreSource.stream()
                .map(KpiCalculatedDTO::getPeriodeN1).filter(p -> p != null && p > 0)
                .findFirst().map(String::valueOf).orElse("N-1");
        String periodeNStr = scoreSource.stream()
                .map(KpiCalculatedDTO::getPeriodeN).filter(p -> p != null && p > 0)
                .findFirst().map(String::valueOf).orElse("N");

        // Inject pre-computed scores as ground truth for globalSummary
        prompt.append("=== SCORES CALCULÉS (à citer tels quels dans globalSummary) ===\n");
        prompt.append(String.format("Période analysée : %s → %s\n", periodeN1Str, periodeNStr));
        prompt.append(String.format("Score global : %d/100 (%s) | KPIs totaux session : %d | Critiques : %d | Modérés : %d | Faibles/OK : %d\n",
                scores.globalScore, scores.globalLabel, reportedTotal,
                scores.critiques, scores.moderes, scores.faibledOk));
        for (ScoreSummary.CatScore cs : scores.categories) {
            prompt.append(String.format("  • %s : score %d/100 | %d critique(s), %d modéré(s), %d OK\n",
                    cs.libelle, cs.score, cs.critiques, cs.moderes, cs.ok));
        }
        if (scores.worstKpi != null) {
            prompt.append(String.format("KPI le plus dégradé : %s (%+.1f%%, %s→%s, %s)\n",
                    scores.worstKpi.getKpiName(), scores.worstVariation,
                    safeNumber(scores.worstKpi.getValeurN1()), safeNumber(scores.worstKpi.getValeurN()),
                    safeText(scores.worstKpi.getClassification())));
        }
        prompt.append("\n");


        String combinedQuery = orderedKpis.stream()
                .map(k -> safeText(k.getKpiName()))
                .collect(Collectors.joining(", "));
        List<RagKnowledge> ragResults = ragSearchService.findRelevant(combinedQuery, 15, null, 0.60);
        if (!ragResults.isEmpty()) {
            prompt.append("=== BASE DE CONNAISSANCES QHSE (RAG vectoriel) ===\n");
            prompt.append("sourceId: rag_knowledge | sourceName: QHSE enriched definitions | relevanceScore: 0.95\n");
            int ragCharsUsed = 0;
            final int RAG_CHAR_CAP = 5000;
            for (RagKnowledge r : ragResults) {
                String chunkLabel = r.getChunkType() != null ? " [" + r.getChunkType() + "]" : "";
                String entry = "• " + r.getKpiName() + chunkLabel + ": " + r.getDefinition()
                    + (r.getThresholds() != null ? " | Seuils: " + r.getThresholds() : "") + "\n";
                if (ragCharsUsed + entry.length() > RAG_CHAR_CAP) break;
                prompt.append(entry);
                ragCharsUsed += entry.length();
            }
            prompt.append("\n");
        }

        prompt.append("=== INSTRUCTIONS DE SORTIE ===\n");
        prompt.append("Return a single JSON object with the following structure exactly.\n");
        prompt.append("Do not include extra fields outside the defined schema.\n");
        prompt.append("Do not use markdown or backticks.\n");
        prompt.append("If a field cannot be determined with certainty from the provided data, provide a prudent QHSE analysis grounded in the KPI values and explain the uncertainty without leaving the field empty.\n");
        prompt.append("Do not omit any provided KPI: every KPI must appear in kpiInsights, even if the insight is a concise justification based on the provided data.\n");
        prompt.append("Cite the KPI names or ids that justify each recommendation, cause, and action.\n");
        prompt.append("Confidence values should be numeric percentages between 0 and 100.\n\n");

        prompt.append("OUTPUT SCHEMA:\n");
        prompt.append("{\n");
        prompt.append("  \"globalSummary\": string,  // OBLIGATOIRE : un seul paragraphe narratif fluide (5 à 8 phrases), rédigé comme un rapport d'expert QHSE senior destiné à la direction.\n");
        prompt.append("  // Doit couvrir dans l'ordre, sans sauts de ligne : (1) période et périmètre analysés, (2) score global chiffré et bilan par catégorie, (3) les 2-3 KPIs les plus critiques avec valeurs N-1→N et impact réel ISO/réglementaire, (4) les 2 actions prioritaires avec horizon temporel, (5) conclusion sur la trajectoire globale.\n");
        prompt.append("  // INTERDIT : sauts de ligne (\\n), puces, titres de section, texte générique sans valeurs chiffrées, répétitions.\n");
        prompt.append("  \"confidence\": {\n");
        prompt.append("    \"overall\": number,\n");
        prompt.append("    \"sections\": {\n");
        prompt.append("      \"summary\": number,\n");
        prompt.append("      \"probableCauses\": number,\n");
        prompt.append("      \"recommendations\": number,\n");
        prompt.append("      \"actionPlan\": number\n");
        prompt.append("    }\n");
        prompt.append("  },\n");
        prompt.append("  \"kpiInsights\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiId\": number,\n");
        prompt.append("      \"kpiName\": string,\n");
        prompt.append("      \"confidence\": number,\n");
        prompt.append("      \"insight\": string,\n");
        prompt.append("      \"probableCauses\": [string],\n");
        prompt.append("      \"recommendations\": [string],\n");
        prompt.append("      \"actionImmediate\": string,\n");
        prompt.append("      \"urgency\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string,\n");
        prompt.append("      \"note\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"probableCauses\": [string],\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": string,\n");
        prompt.append("      \"rationale\": string,\n");
        prompt.append("      \"expectedBenefit\": string,\n");
        prompt.append("      \"urgency\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"actionPlan\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"action\": string,\n");
        prompt.append("      \"priority\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"rootCauseAnalysis\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiRef\": string,\n");
        prompt.append("      \"method\": \"5_whys\",\n");
        prompt.append("      \"whyChain\": [string],\n");
        prompt.append("      \"ishikawaCategory\": string,\n");
        prompt.append("      \"rootCause\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"predictiveAlerts\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiRef\": string,\n");
        prompt.append("      \"projection\": string,\n");
        prompt.append("      \"estimatedHorizonMonths\": number,\n");
        prompt.append("      \"confidence\": number,\n");
        prompt.append("      \"severity\": \"LOW|MEDIUM|HIGH\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"traceability\": {\n");
        prompt.append("    \"modelName\": string,\n");
        prompt.append("    \"generatedAt\": string,\n");
        prompt.append("    \"contextSourcesUsed\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"sourceId\": string,\n");
        prompt.append("        \"sourceName\": string,\n");
        prompt.append("        \"relevanceScore\": number\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append(FEW_SHOT_EXAMPLE);
        prompt.append("=== KPI CONTEXT & RAG SOURCES ===\n");
        for (KpiCalculatedDTO kpi : orderedKpis) {
            prompt.append(String.format("sourceId: kpi_def_%s | sourceName: KPI definition for %s | relevanceScore: 0.85\n",
                    safeId(kpi.getMatchedKpiId(), kpi.getKpiName()), safeText(kpi.getKpiName())));
            prompt.append(String.format("KPI: %s | Categorie: %s | Definition: %s | Seuils: Faible<%s, Modere<%s, Critique<%s | N-1=%s | N=%s | Variation=%s%% | Classification=%s\n",
                    safeText(kpi.getKpiName()), safeText(kpi.getCategorie()), safeText(kpi.getDefinition()),
                    safeNumber(kpi.getSeuilFaible()), safeNumber(kpi.getSeuilModere()), safeNumber(kpi.getSeuilCritique()),
                    safeNumber(kpi.getValeurN1()), safeNumber(kpi.getValeurN()), safeNumber(kpi.getVariationPercentage()),
                    safeText(kpi.getClassification())));
            prompt.append("\n");
        }

        prompt.append("Make sure the output is valid JSON.\n");
        prompt.append("If you cannot provide a structured section, return an empty array/object for it instead of text.\n");
        prompt.append("\n=== INSTRUCTIONS SPÉCIFIQUES rootCauseAnalysis ===\n");
        prompt.append("For each CRITIQUE or MODERE KPI, produce exactly one rootCauseAnalysis entry.\n");
        prompt.append("whyChain must contain 3 to 5 successive 'Why' questions leading to the root cause.\n");
        prompt.append("ishikawaCategory must be one of: Homme, Machine, Méthode, Milieu, Matière.\n");
        prompt.append("rootCause must be a single concise sentence identifying the fundamental cause.\n");
        prompt.append("\n=== INSTRUCTIONS SPÉCIFIQUES predictiveAlerts ===\n");
        prompt.append("For each KPI showing a deteriorating trend (trendDirection DETERIORATING or 2+ consecutive negative variations), produce a predictiveAlert.\n");
        prompt.append("projection must describe what will happen if the trend continues (e.g. 'Si la tendance continue, le KPI X atteindra le seuil critique dans environ N mois').\n");
        prompt.append("estimatedHorizonMonths must be an integer between 1 and 24. Use null if trend is stable or improving.\n");
        prompt.append("severity must be LOW (horizon > 12 months), MEDIUM (6-12 months) or HIGH (< 6 months).\n");
        prompt.append("confidence must be a number between 0 and 100.\n");

        prompt.append("=== ADDITIONAL CONTEXT SOURCES ===\n");
        prompt.append("sourceId: historical_actions | sourceName: recent action plans | relevanceScore: 0.65\n");
        prompt.append("Analyze current KPI variations against the most recent action plans provided.\n");
        prompt.append("\n");

        prompt.append("Remember: the structured JSON is the single source of truth. Do not output other prose.\n");

        return prompt.toString();
    }

    public String buildRetryPrompt(String originalPrompt, String validationErrors) {
        StringBuilder retryPrompt = new StringBuilder(originalPrompt);
        retryPrompt.append("\n\nATTENTION : ta réponse précédente était invalide.\n");
        retryPrompt.append("Erreurs de validation : ").append(safeText(validationErrors)).append(".\n");
        retryPrompt.append("Régénère le JSON avec exactement le même schéma et corrige toutes les erreurs ci-dessus.\n");
        retryPrompt.append("Réponds uniquement avec du JSON valide.\n");
        return retryPrompt.toString();
    }

    public String getPromptVersion() {
        return "structured-qhse-v7";
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String normalized = TextNormalizer.normalizeForPrompt(value);
        return promptSanitizer.sanitize(normalized, 1000);
    }

    private String safeNumber(Number value) {
        if (value == null) return "0";
        return value.toString();
    }

    private String safeId(Long id, String name) {
        if (id != null) {
            return String.valueOf(id);
        }
        String safeName = TextNormalizer.normalizeForSearch(name);
        return safeName.isBlank() ? "unknown" : safeName.replaceAll("\\s+", "_");
    }

    // ── Score pre-computation ─────────────────────────────────────────────────

    static class ScoreSummary {
        int globalScore;
        String globalLabel;
        int critiques;
        int moderes;
        int faibledOk;
        KpiCalculatedDTO worstKpi;
        double worstVariation;
        List<CatScore> categories = new ArrayList<>();

        static class CatScore {
            String libelle;
            int score;
            int critiques;
            int moderes;
            int ok;
        }
    }

    private ScoreSummary computeScores(List<KpiCalculatedDTO> kpis) {
        ScoreSummary s = new ScoreSummary();

        // Global counts
        for (KpiCalculatedDTO k : kpis) {
            String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
            if (cls.contains("CRITIQUE")) s.critiques++;
            else if (cls.contains("MODERE") || cls.contains("MODERÉ")) s.moderes++;
            else s.faibledOk++;
        }

        // Weighted global score (CRITIQUE counts double)
        double weightedSum = 0;
        double totalWeight = 0;
        for (KpiCalculatedDTO k : kpis) {
            String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
            double base   = cls.contains("CRITIQUE") ? 20 : cls.contains("MODERE") || cls.contains("MODERÉ") ? 60 : 100;
            double weight = cls.contains("CRITIQUE") ? 2 : 1;
            weightedSum += base * weight;
            totalWeight += weight;
        }
        s.globalScore = totalWeight > 0 ? (int) Math.round(weightedSum / totalWeight) : 0;
        if (s.globalScore >= 80)      s.globalLabel = "EXCELLENT";
        else if (s.globalScore >= 60) s.globalLabel = "SATISFAISANT";
        else if (s.globalScore >= 40) s.globalLabel = "À SURVEILLER";
        else                          s.globalLabel = "CRITIQUE";

        // Worst KPI by absolute variation
        kpis.stream()
            .filter(k -> k.getVariationPercentage() != null)
            .max(Comparator.comparingDouble(k -> Math.abs(((KpiCalculatedDTO) k).getVariationPercentage())))
            .ifPresent(k -> {
                s.worstKpi = k;
                s.worstVariation = k.getVariationPercentage();
            });

        // Per-category scores
        Map<String, List<KpiCalculatedDTO>> byCat = kpis.stream()
            .filter(k -> k.getCategorie() != null)
            .collect(Collectors.groupingBy(KpiCalculatedDTO::getCategorie, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<KpiCalculatedDTO>> entry : byCat.entrySet()) {
            ScoreSummary.CatScore cs = new ScoreSummary.CatScore();
            cs.libelle = entry.getKey();
            List<KpiCalculatedDTO> catKpis = entry.getValue();
            double cw = 0, cws = 0;
            for (KpiCalculatedDTO k : catKpis) {
                String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
                if (cls.contains("CRITIQUE")) { cs.critiques++; cws += 20 * 2; cw += 2; }
                else if (cls.contains("MODERE") || cls.contains("MODERÉ")) { cs.moderes++; cws += 60; cw += 1; }
                else { cs.ok++; cws += 100; cw += 1; }
            }
            cs.score = cw > 0 ? (int) Math.round(cws / cw) : 0;
            s.categories.add(cs);
        }
        return s;
    }
}
