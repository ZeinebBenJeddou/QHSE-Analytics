package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.Tendance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroqPromptBuilder {

    public String buildKpiPrompt(
            String kpiNom,
            String definition,
            String unite,
            String categorieLibelle,
            double valeurN1,
            double valeurN,
            double variationRelative,
            NiveauVariation niveau,
            Tendance tendance,
            int periodeN1,
            int periodeN
    ) {
        return "Tu es un expert QHSE certifié ISO 9001/14001/45001.\n\n"
                + "KPI : " + safe(kpiNom) + " — " + safe(definition) + "\n"
                + "Catégorie : " + safe(categorieLibelle) + " | Unité : " + safe(unite) + "\n"

                                + "Valeur " + periodeN1 + " : " + formatValue(valeurN1) + " " + safe(unite) + "\n"
                                + "Valeur " + periodeN + " : " + formatValue(valeurN) + " " + safe(unite) + "\n"
                                + "Variation : " + format(variationRelative) + "% → niveau " + safeEnum(niveau) + "\n"
                                + "Tendance : " + safeEnum(tendance) + "\n\n"
                + "Génère une analyse structurée en 3 points :\n"
                + "1. CONSTAT : décris objectivement cette évolution en 1-2 phrases.\n"
                + "2. CAUSES PROBABLES : identifie les 2-3 causes les plus vraisemblables\n"
                + "   de cette variation dans un contexte QHSE professionnel.\n"
                + "3. RECOMMANDATION : propose une action concrète et mesurable\n"
                + "   avec un délai réaliste.\n\n"
                + "Réponds en français, de façon professionnelle et concise,\n"
                + "directement utilisable par un responsable QHSE.";
    }

    public String buildCategoriePrompt(
            String categorieLibelle,
            List<ResultatKpi> resultats,
            int periodeN1,
            int periodeN
    ) {

        Map<String, List<ResultatKpi>> byCategory = (resultats == null ? List.<ResultatKpi>of() : resultats).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::resolveCategorieLibelle, LinkedHashMap::new, Collectors.toList()));

        String targetCategory = safe(categorieLibelle);
        List<ResultatKpi> categoryResults = byCategory.getOrDefault(
                targetCategory,
                byCategory.values().stream().findFirst().orElse(List.of())
        );

        String lignes = categoryResults.stream()
                .map(resultat -> "- " + safeKpiNom(resultat)
                        + " : " + formatValue(resultat.getValeurN1())
                        + " → " + formatValue(resultat.getValeurN()) + " " + safeUnite(resultat)
                        + " (" + format(safeVariation(resultat)) + "% / " + safeEnum(resultat.getNiveauVariation()) + ")")
                .collect(Collectors.joining("\n"));

        if (lignes.isBlank()) {
            lignes = "- Aucun KPI exploitable";
        }

        return "Tu es un expert QHSE certifié ISO 9001/14001/45001.\n\n"
                + "Voici les résultats de la catégorie " + targetCategory
                + " pour la période " + periodeN1 + " → " + periodeN + " :\n\n"
                + lignes + "\n\n"
                + "Génère une analyse catégorielle structurée en 3 points :\n"
                + "1. BILAN : synthèse de la performance globale de cette catégorie en 2-3 phrases.\n"
                + "2. CORRÉLATIONS ET CAUSES : identifie les liens entre les KPIs et les causes\n"
                + "   probables des évolutions observées. Sois précis et contextuel.\n"
                + "3. RECOMMANDATIONS : propose 2 à 3 actions prioritaires pour améliorer\n"
                + "   cette catégorie, avec des indicateurs de suivi mesurables.\n\n"
                + "Réponds en français, de façon professionnelle.";
    }

    public String buildSynthesePrompt(
            List<ResultatKpi> tousLesResultats,
            int periodeN1,
            int periodeN
    ) {
        Map<String, List<ResultatKpi>> byCategory = tousLesResultats.stream()
                .filter(resultat -> resultat.getKpi() != null && resultat.getKpi().getCategorieKpi() != null)
                .collect(Collectors.groupingBy(
                        resultat -> resultat.getKpi().getCategorieKpi().getCode(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        StringBuilder builder = new StringBuilder();
        builder.append("Tu es un expert QHSE certifié ISO 9001/14001/45001.\n\n")
                .append("Voici le bilan comparatif QHSE complet ").append(periodeN1).append(" → ").append(periodeN).append(" :\n\n");

        appendCategoryBlock(builder, "QUALITÉ (Q)", byCategory.get("Q"));
        appendCategoryBlock(builder, "HYGIÈNE (H)", byCategory.get("H"));
        appendCategoryBlock(builder, "SÉCURITÉ (S)", byCategory.get("S"));
        appendCategoryBlock(builder, "ENVIRONNEMENT (E)", byCategory.get("E"));

        builder.append("\nGénère une synthèse globale en 2 points :\n")
                .append("1. BILAN GÉNÉRAL : évalue la performance globale QHSE, identifie\n")
                .append("   les 3 points forts et les 3 points les plus critiques.\n")
                .append("2. CAUSES TRANSVERSALES : identifie les facteurs qui expliquent\n")
                .append("   les tendances observées sur plusieurs catégories simultanément.\n\n")
                .append("Réponds en français, de façon professionnelle et synthétique.");

        return builder.toString();
    }

    public String buildPlanActionsPrompt(
            List<ResultatKpi> tousLesResultats,
            int periodeN1,
            int periodeN
    ) {
        List<ResultatKpi> ordered = (tousLesResultats == null ? List.<ResultatKpi>of() : tousLesResultats).stream()
                .filter(Objects::nonNull)
                .filter(resultat -> resultat.getNiveauVariation() != null)
                .sorted(Comparator.comparingInt((ResultatKpi resultat) -> severityRank(resultat.getNiveauVariation()))
                        .thenComparing(ResultatKpi::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        String critiques = ordered.stream()
                .filter(resultat -> resultat.getNiveauVariation() == NiveauVariation.CRITIQUE)
                .map(resultat -> "- " + safeKpiNom(resultat) + " | " + format(safeVariation(resultat)) + "% | " + safeEnum(resultat.getTendance()))
                .collect(Collectors.joining("\n"));

        String moderes = ordered.stream()
                .filter(resultat -> resultat.getNiveauVariation() == NiveauVariation.MODERE)
                .map(resultat -> "- " + safeKpiNom(resultat) + " | " + format(safeVariation(resultat)) + "% | " + safeEnum(resultat.getTendance()))
                .collect(Collectors.joining("\n"));

        String faibles = ordered.stream()
                .filter(resultat -> resultat.getNiveauVariation() == NiveauVariation.FAIBLE)
                .map(resultat -> "- " + safeKpiNom(resultat) + " | " + format(safeVariation(resultat)) + "%")
                .collect(Collectors.joining("\n"));

        return "Tu es un expert QHSE certifié ISO 9001/14001/45001.\n\n"
                + "Sur la base des résultats QHSE " + periodeN1 + " → " + periodeN + ",\n"
                + "génère un plan d'actions opérationnel structuré :\n\n"
                + "KPIs CRITIQUES (variation ≥ seuil critique) :\n"
                + (critiques.isBlank() ? "- Aucun KPI critique\n" : critiques + "\n")
                + "\nKPIs MODÉRÉS (variation entre seuil faible et critique) :\n"
                + (moderes.isBlank() ? "- Aucun KPI modéré\n" : moderes + "\n")
                + "\nKPIs FAIBLES ou STABLES :\n"
                + (faibles.isBlank() ? "- Aucun KPI faible\n" : faibles + "\n")
                + "\nPLAN D'ACTIONS :\n\n"
                + "🔴 ACTIONS CRITIQUES — À traiter immédiatement :\n"
                + "Pour chaque KPI critique :\n"
                + "→ Action : [action concrète et mesurable]\n"
                + "→ Responsable suggéré : [fonction QHSE appropriée]\n"
                + "→ Délai : [délai réaliste]\n"
                + "→ Indicateur de succès : [KPI cible chiffré]\n\n"
                + "🟡 ACTIONS IMPORTANTES — Sous 3 mois :\n"
                + "Pour chaque KPI modéré dégradé :\n"
                + "→ Action préventive + délai + indicateur de suivi\n\n"
                + "🟢 SURVEILLANCE :\n"
                + "→ KPIs à monitorer + fréquence de suivi recommandée\n\n"
                + "Réponds en français, de façon structurée et directement\n"
                + "utilisable en réunion de direction QHSE.";
    }

    private void appendCategoryBlock(StringBuilder builder, String title, List<ResultatKpi> resultats) {
        builder.append(title).append(" :\n");
        if (resultats == null || resultats.isEmpty()) {
            builder.append("Aucun KPI\n\n");
            return;
        }

                for (ResultatKpi resultat : resultats) {
                        if (resultat == null) {
                                continue;
                        }
            builder.append("- ")
                                        .append(safeKpiNom(resultat))
                    .append(" | ")
                                        .append(format(safeVariation(resultat)))
                    .append("% | ")
                                        .append(safeEnum(resultat.getNiveauVariation()))
                    .append('\n');
        }
        builder.append('\n');
    }

        private String resolveCategorieLibelle(ResultatKpi resultat) {
                if (resultat == null || resultat.getKpi() == null || resultat.getKpi().getCategorieKpi() == null) {
                        return "Non catégorisé";
                }
                return safe(resultat.getKpi().getCategorieKpi().getLibelle());
        }

        private String safeKpiNom(ResultatKpi resultat) {
                if (resultat == null || resultat.getKpi() == null) {
                        return "KPI inconnu";
                }
                return safe(resultat.getKpi().getNom());
        }

        private String safeUnite(ResultatKpi resultat) {
                if (resultat == null || resultat.getKpi() == null || resultat.getKpi().getUnite() == null) {
                        return "";
                }
                return safe(resultat.getKpi().getUnite().name());
        }

        private double safeVariation(ResultatKpi resultat) {
                if (resultat == null || resultat.getVariationRelative() == null) {
                        return 0d;
                }
                return resultat.getVariationRelative();
        }

    private String safe(String value) {
        return value == null ? "" : value;
    }

        private String safeEnum(Enum<?> value) {
                return value == null ? "N/A" : value.name();
        }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

        private String formatValue(Double value) {
                if (value == null) {
                        return "N/A";
                }
                return String.format(Locale.ROOT, "%.2f", value);
        }

    private int severityRank(NiveauVariation niveauVariation) {
        return switch (niveauVariation) {
            case CRITIQUE -> 0;
            case MODERE -> 1;
            case FAIBLE -> 2;
        };
    }
}