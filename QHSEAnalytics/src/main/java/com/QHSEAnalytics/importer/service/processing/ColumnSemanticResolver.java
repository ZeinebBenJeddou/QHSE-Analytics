package com.QHSEAnalytics.importer.service.processing;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class ColumnSemanticResolver {

    public enum Semantic { KPI_NAME, VALUE_N, VALUE_N1, CATEGORY, UNIT, UNKNOWN }

    public record Result(Semantic semantic, double confidence) {}

    private static final Set<String> KPI_SYNONYMS = Set.of(
            "kpi", "indicateur", "indicateur qhse", "libelle",
            "nom indicateur", "metric", "metrics", "nom kpi", "designation",
            "intitule", "intitule kpi", "intitule indicateur",
            "indicator", "indicators", "measure", "measures", "item", "items");

    private static final Set<String> TARGET_SYNONYMS = Set.of(
            "objectif", "objectifs", "target", "targets",
            "goal", "goals", "cible", "cibles");

    private static final Set<String> VALUE_N_SYNONYMS = Set.of(
            "n", "annee n", "valeur n", "resultat n", "current", "annee actuelle",
            "valeur actuelle", "n courant",
            "realise", "realise n", "valeur realisee", "performance",
            "performance n", "resultat", "resultat actuel",
            "valeur courante", "mesure n", "donnee n",
            "current year", "this year", "actual", "current value", "value n");

    private static final Set<String> VALUE_N1_SYNONYMS = Set.of(
            "n-1", "annee n-1", "valeur n-1", "resultat n-1", "previous",
            "annee precedente", "valeur precedente", "n1", "n moins 1",
            "realise n-1", "realise n1", "performance n-1", "performance n1",
            "resultat precedent", "valeur realisee n-1",
            "mesure n-1", "donnee n-1", "base", "reference",
            "previous year", "last year", "prior", "prior year", "baseline", "value n-1");

    private static final Set<String> CATEGORY_SYNONYMS = Set.of(
            "categorie", "domaine", "famille", "axe", "qhse", "type",
            "pilier", "theme", "thematique", "processus", "pole",
            "departement", "service", "direction", "secteur");

    private static final Set<String> UNIT_SYNONYMS = Set.of(
            "unite", "unit", "mesure", "unites",
            "unite de mesure", "um", "grandeur", "echelle");

    private ColumnSemanticResolver() {}

    public static Result resolve(String rawHeader) {
        String h = normalize(rawHeader);

        if (KPI_SYNONYMS.contains(h) || h.contains("kpi") || h.contains("indicateur")
                || h.contains("intitule") || h.contains("designation"))
            return new Result(Semantic.KPI_NAME, 0.95);

        // VALUE_N1 testé AVANT VALUE_N pour éviter que "n-1" matche "actuel"
        if (VALUE_N1_SYNONYMS.contains(h) || h.contains("n1") || h.contains("n-1")
                || h.contains("precedent") || h.contains("previous") || h.contains("reference")
                || h.contains("base"))
            return new Result(Semantic.VALUE_N1, 0.90);

        if (VALUE_N_SYNONYMS.contains(h) || h.equals("n")
                || h.contains("actuel") || h.contains("current") || h.contains("realise")
                || h.contains("performance") || h.contains("resultat"))
            return new Result(Semantic.VALUE_N, 0.85);

        if (CATEGORY_SYNONYMS.contains(h) || h.contains("categor") || h.contains("domain")
                || h.contains("theme") || h.contains("pilier") || h.contains("processus"))
            return new Result(Semantic.CATEGORY, 0.88);

        if (UNIT_SYNONYMS.contains(h) || h.contains("unit") || h.contains("mesure")
                || h.contains("grandeur"))
            return new Result(Semantic.UNIT, 0.85);

        return new Result(Semantic.UNKNOWN, 0.30);
    }

    public static boolean matches(String rawHeader, Set<String> synonyms) {
        String h = normalize(rawHeader);
        if (synonyms.contains(h)) return true;
        for (String syn : synonyms) {
            if (h.contains(syn) || syn.contains(h)) return true;
        }
        return false;
    }

    public static Set<String> synonymsFor(Semantic semantic) {
        return switch (semantic) {
            case KPI_NAME  -> KPI_SYNONYMS;
            case VALUE_N   -> VALUE_N_SYNONYMS;
            case VALUE_N1  -> VALUE_N1_SYNONYMS;
            case CATEGORY  -> CATEGORY_SYNONYMS;
            case UNIT      -> UNIT_SYNONYMS;
            default        -> Set.of();
        };
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
