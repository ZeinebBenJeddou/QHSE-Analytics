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
            "Tu es un consultant QHSE expert de niveau senior (15 ans d'expérience), auditeur certifié ISO 9001, ISO 14001 et ISO 45001, " +
            "maîtrisant les référentiels DREAL, INRS, et les méthodes d'analyse 8D, 5 Pourquoi et diagramme d'Ishikawa. " +
            "Tu produis des rapports d'analyse QHSE de qualité professionnelle, destinés à la direction et aux responsables opérationnels.\n\n" +
            "RÈGLES ABSOLUES :\n" +
            "1. Réponds UNIQUEMENT avec du JSON valide — aucun texte avant ou après, aucun markdown, aucun backtick.\n" +
            "2. Chaque champ texte doit être spécifique, chiffré et actionnable. " +
            "   Les valeurs génériques ('N/A', 'À analyser', 'Non disponible', texte < 15 mots) sont STRICTEMENT INTERDITES.\n" +
            "3. insight : analyse la variation chiffrée (valeur N-1 → N, écart %, seuil franchi), " +
            "   son impact QHSE réel (sécurité des personnes, conformité normative, coût, satisfaction client) " +
            "   et la norme ISO ou réglementation concernée.\n" +
            "4. probableCauses (par KPI) : chaque cause doit être classée dans une catégorie Ishikawa " +
            "   (Homme / Machine / Méthode / Milieu / Matière) et rédigée comme une phrase causale précise.\n" +
            "5. actionImmediate : action SMART — Spécifique, Mesurable, avec un verbe d'action fort " +
            "   (Organiser, Déployer, Auditer, Suspendre, Mettre en place), un responsable et un horizon temporel.\n" +
            "6. successMetric : indicateur de résultat précis et mesurable " +
            "   (ex : 'TF1 < 2,5 dans les 3 mois', 'FPY > 90 % à J+30').\n" +
            "7. riskIfNotDone : conséquence concrète si l'action n'est pas menée " +
            "   (pénalité réglementaire, accident grave, perte client, audit défavorable).\n" +
            "8. probableCauses (global) : liste de causes TRANSVERSALES classées par catégorie Ishikawa, " +
            "   couvrant les thèmes communs à plusieurs KPIs. Format : '[Catégorie] Cause précise'.\n" +
            "9. recommendations : chaque recommandation doit citer le(s) KPI(s) concerné(s), " +
            "   la norme ISO applicable, et l'impact attendu chiffré si possible.\n" +
            "10. actionPlan : plan d'actions priorisé, chaque action doit avoir un ownerRole précis " +
            "    (Responsable HSE / Responsable Qualité / Directeur de Production / RH...), " +
            "    un dueHorizon réaliste (48h / 1 semaine / 1 mois / 3 mois) et un successMetric mesurable.\n" +
            "11. Tu dois produire exactement un objet kpiInsights pour chaque KPI fourni.\n" +
            "12. Réponds intégralement en français.\n";

    private static final String FEW_SHOT_EXAMPLE =
            "\n\n=== EXEMPLES DE RÉPONSES ATTENDUES (respecte ce niveau de qualité) ===\n\n" +
            "EXEMPLE globalSummary :\n" +
            "\"L'analyse QHSE portant sur la période 2025 → 2026, couvrant 12 indicateurs répartis sur 4 catégories (Qualité, Hygiène, Sécurité, Environnement), révèle un score global de 42/100 (À SURVEILLER) avec 8 KPIs classés CRITIQUE. " +
            "La catégorie Qualité est la plus dégradée (score 20/100, 3 critiques), notamment le First Pass Yield en chute de −18,5 % (91 % → 74,2 %), franchissant le seuil critique ISO 9001 §8.7 sur la maîtrise des non-conformités. " +
            "Sur le plan sécurité (ISO 45001), le Taux d'Absentéisme progresse de +32 % (4,1 % → 5,4 %), signal d'alerte sur les conditions de travail, tandis que le Taux de Fréquence des Accidents affiche +15 %, exposant l'entreprise à un risque de mise en demeure DREAL. " +
            "Le Délai Moyen de Livraison (+28 %, 3,6 → 4,6 jours) génère un risque contractuel direct. " +
            "Il est impératif d'engager dans les 48h une revue sécurité d'urgence ciblant les postes à risque, et dans la semaine un plan de réduction des défauts visant FPY > 85 % ; sans action immédiate, la trajectoire actuelle conduira à une non-conformité multi-normes dans les 2 à 3 prochains mois.\"\n\n" +
            "EXEMPLE d'un kpiInsights (reproduis ce niveau de détail pour CHAQUE KPI) :\n" +
            "{\n" +
            "  \"kpiId\": 1547,\n" +
            "  \"kpiName\": \"Taux de Fréquence des Accidents (TF1)\",\n" +
            "  \"confidence\": 87,\n" +
            "  \"insight\": \"Le TF1 a progressé de +28 % entre N-1 et N (2,5 → 3,2), dépassant le seuil critique fixé à 3,0 selon le référentiel ISO 45001 §6.1.2. Cette dégradation, couplée à une augmentation de la cadence de production de 15 %, traduit une insuffisance des barrières préventives face à l'accroissement de l'activité. Un accident grave est statistiquement probable si la tendance n'est pas inversée sous 6 semaines.\",\n" +
            "  \"probableCauses\": [\n" +
            "    \"[Méthode] Absence de révision des analyses de risques lors de l'augmentation de cadence de production\",\n" +
            "    \"[Homme] Déficit de formation sécurité pour les opérateurs récemment embauchés (< 6 mois d'ancienneté)\",\n" +
            "    \"[Milieu] Sous-déclaration des presqu'accidents réduisant la visibilité sur les signaux faibles\"\n" +
            "  ],\n" +
            "  \"recommendations\": [\n" +
            "    \"Mettre à jour les analyses de risques (DUERP) pour intégrer les nouveaux postes créés lors de l'augmentation de cadence\",\n" +
            "    \"Déployer un programme de formation sécurité ciblé pour les opérateurs < 6 mois, avec évaluation des acquis\"\n" +
            "  ],\n" +
            "  \"actionImmediate\": \"Organiser une revue sécurité d'urgence avec les responsables de ligne et le Responsable HSE dans les 48h pour identifier et condamner les postes à risque élevé.\",\n" +
            "  \"urgency\": \"HIGH\",\n" +
            "  \"ownerRole\": \"Responsable HSE\",\n" +
            "  \"dueHorizon\": \"48h\",\n" +
            "  \"successMetric\": \"TF1 revient sous le seuil de 2,5 dans les 3 prochains mois et zéro accident avec arrêt sur les 30 prochains jours\",\n" +
            "  \"riskIfNotDone\": \"Risque d'accident grave avec arrêt de travail, mise en demeure DREAL, augmentation de la cotisation AT/MP et atteinte à l'image de l'entreprise\",\n" +
            "  \"note\": \"KPI sous surveillance prioritaire — reporting hebdomadaire à la direction obligatoire jusqu'au retour sous seuil. Envisager un audit interne ISO 45001 ciblé sur ce processus.\"\n" +
            "}\n\n" +
            "EXEMPLE probableCauses (global, causes transversales classées Ishikawa) :\n" +
            "[\"[Homme] Manque de formation et de sensibilisation des opérateurs aux exigences qualité et sécurité\",\n" +
            " \"[Méthode] Processus de contrôle qualité insuffisamment documentés et appliqués (écart ISO 9001 §8.5)\",\n" +
            " \"[Machine] Maintenance préventive insuffisante engendrant des défaillances récurrentes\",\n" +
            " \"[Milieu] Conditions de travail dégradées (bruit, température, ergonomie) favorisant les erreurs humaines\",\n" +
            " \"[Matière] Variabilité de la qualité des matières premières non détectée à réception\"]\n\n" +
            "EXEMPLE recommendation :\n" +
            "{\n" +
            "  \"title\": \"Renforcer le plan de formation sécurité et qualité\",\n" +
            "  \"rationale\": \"Les KPIs TF1 (+28 %) et First Pass Yield (−18,5 %) convergent vers un déficit de compétences opérateurs. ISO 45001 §7.2 et ISO 9001 §7.2 exigent la mise à jour des compétences lors de changements organisationnels.\",\n" +
            "  \"expectedBenefit\": \"Réduction du TF1 de 20 % et remontée du FPY au-dessus de 88 % sous 3 mois\",\n" +
            "  \"urgency\": \"HIGH\"\n" +
            "}\n\n" +
            "EXEMPLE actionPlan :\n" +
            "{\n" +
            "  \"action\": \"Déployer un audit interne ISO 9001 ciblé sur les processus de contrôle qualité en production\",\n" +
            "  \"priority\": \"HAUTE\",\n" +
            "  \"ownerRole\": \"Responsable Qualité\",\n" +
            "  \"dueHorizon\": \"1 mois\",\n" +
            "  \"successMetric\": \"Rapport d'audit remis, non-conformités majeures traitées, FPY > 88 % à J+30\",\n" +
            "  \"riskIfNotDone\": \"Maintien du FPY sous le seuil critique, risque de plaintes clients et de pertes de contrats\"\n" +
            "}\n\n" +
            "=== DONNÉES RÉELLES À ANALYSER (reproduis le même niveau de détail pour chaque KPI) ===\n";

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

        // Names-only overview of all session KPIs so the LLM can write a globalSummary that covers
        // all KPIs, not just the chunk being analyzed. Full data is intentionally omitted to prevent
        // the LLM from generating kpiInsights for KPIs outside the chunk.
        if (allKpis != null && allKpis.size() > orderedKpis.size()) {
            prompt.append("=== PÉRIMÈTRE GLOBAL DE LA SESSION (référence pour globalSummary UNIQUEMENT) ===\n");
            prompt.append("⚠️ IMPORTANT : Cette liste sert UNIQUEMENT à rédiger le champ globalSummary.\n");
            prompt.append("Tu NE DOIS PAS produire de kpiInsight pour ces KPIs — seulement pour ceux de la section KPI CONTEXT ci-dessous.\n");
            prompt.append("Noms des KPIs de la session (tous) : ");
            List<String> allNames = allKpis.stream().map(k -> safeText(k.getKpiName())).collect(Collectors.toList());
            prompt.append(String.join(", ", allNames)).append("\n");
            prompt.append("Scores pré-calculés disponibles dans la section SCORES ci-dessus — utilise-les dans globalSummary.\n\n");
        }

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
        prompt.append("Renvoie un unique objet JSON valide respectant exactement le schéma ci-dessous.\n");
        prompt.append("Aucun texte avant ou après, aucun markdown, aucun backtick.\n\n");
        prompt.append("QUALITÉ REQUISE PAR SECTION :\n");
        prompt.append("• insight (par KPI) : 3 phrases minimum — (1) variation chiffrée + seuil franchi, (2) impact QHSE réel + norme ISO concernée, (3) interprétation causale préliminaire.\n");
        prompt.append("• probableCauses (par KPI) : 2 à 4 causes, chacune préfixée par sa catégorie Ishikawa entre crochets : [Homme], [Machine], [Méthode], [Milieu] ou [Matière].\n");
        prompt.append("• probableCauses (global) : 4 à 8 causes TRANSVERSALES couvrant plusieurs KPIs, classées Ishikawa, format '[Catégorie] Cause précise'.\n");
        prompt.append("• recommendations : 3 à 6 recommandations. Chaque title cite le(s) KPI(s) concerné(s). rationale mentionne la clause ISO applicable. expectedBenefit est chiffré si possible.\n");
        prompt.append("• actionPlan : 4 à 8 actions SMART ordonnées par priorité décroissante. ownerRole = rôle précis (pas 'Responsable' générique). dueHorizon = délai réaliste. riskIfNotDone = conséquence concrète.\n");
        prompt.append("• successMetric : indicateur mesurable avec valeur cible et horizon temporel (ex: 'KPI X < seuil Y dans les Z mois').\n");
        prompt.append("• note (par KPI) : observation experte complémentaire — tendance préoccupante, lien avec un autre KPI, recommandation de surveillance renforcée.\n");
        prompt.append("Les valeurs confidence sont des entiers entre 0 et 100.\n\n");

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
        prompt.append("⚠️ RÈGLE ABSOLUE : pour chaque KPI ci-dessous, le champ \"kpiId\" dans kpiInsights DOIT être exactement l'entier indiqué après KPI_ID=. Ne génère jamais 1, 2, 3... séquentiellement — utilise UNIQUEMENT les IDs fournis.\n\n");
        for (KpiCalculatedDTO kpi : orderedKpis) {
            String realId = safeId(kpi.getMatchedKpiId(), kpi.getKpiName());
            prompt.append(String.format("sourceId: kpi_def_%s | sourceName: KPI definition for %s | relevanceScore: 0.85\n",
                    realId, safeText(kpi.getKpiName())));
            prompt.append(String.format("KPI_ID=%s | KPI: %s | Categorie: %s | Definition: %s | Seuils: Faible<%s, Modere<%s, Critique<%s | N-1=%s | N=%s | Variation=%s%% | Classification=%s\n",
                    realId,
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
        return "structured-qhse-v8";
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

    public static class ScoreSummary {
        public int globalScore;
        public String globalLabel;
        public int critiques;
        public int moderes;
        public int faibledOk;
        public KpiCalculatedDTO worstKpi;
        public double worstVariation;
        public List<CatScore> categories = new ArrayList<>();

        public static class CatScore {
            public String libelle;
            public int score;
            public int critiques;
            public int moderes;
            public int ok;
        }
    }

    public ScoreSummary computeScoresPublic(List<KpiCalculatedDTO> kpis) {
        return computeScores(kpis);
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
