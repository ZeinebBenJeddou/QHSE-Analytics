package com.QHSEAnalytics.importer.service.processing;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class ColumnSemanticResolver {

    public enum Semantic { KPI_NAME, VALUE_N, VALUE_N1, CATEGORY, UNIT, UNKNOWN } // concepts attendus

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
            "current year", "this year", "actual", "current value", "value n",
            "2020", "2021", "2022", "2023", "2024", "2025", "2026"
    );

    private static final Set<String> VALUE_N1_SYNONYMS = Set.of(
            "n-1", "annee n-1", "valeur n-1", "resultat n-1", "previous",
            "annee precedente", "valeur precedente", "n1", "n moins 1",
            "realise n-1", "realise n1", "performance n-1", "performance n1",
            "resultat precedent", "valeur realisee n-1",
            "mesure n-1", "donnee n-1", "base", "reference",
            "previous year", "last year", "prior", "prior year", "baseline", "value n-1",
            "2020", "2021", "2022", "2023", "2024", "2025", "2026"
    );

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
            return new Result(Semantic.KPI_NAME, bestScore(h, KPI_SYNONYMS));


        if (VALUE_N1_SYNONYMS.contains(h) || h.contains("n1") || h.contains("n-1")
                || h.contains("precedent") || h.contains("previous") || h.contains("reference")
                || h.contains("base"))
            return new Result(Semantic.VALUE_N1, bestScore(h, VALUE_N1_SYNONYMS));

        if (VALUE_N_SYNONYMS.contains(h) || h.equals("n")
                || h.contains("actuel") || h.contains("current") || h.contains("realise")
                || h.contains("performance") || h.contains("resultat"))
            return new Result(Semantic.VALUE_N, bestScore(h, VALUE_N_SYNONYMS));

        if (CATEGORY_SYNONYMS.contains(h) || h.contains("categor") || h.contains("domain")
                || h.contains("theme") || h.contains("pilier") || h.contains("processus"))
            return new Result(Semantic.CATEGORY, bestScore(h, CATEGORY_SYNONYMS));

        if (UNIT_SYNONYMS.contains(h) || h.contains("unit") || h.contains("mesure")
                || h.contains("grandeur"))
            return new Result(Semantic.UNIT, bestScore(h, UNIT_SYNONYMS));


        double residual = allSynonyms().stream()
                .mapToDouble(syn -> jaroWinkler(h, syn))
                .max()
                .orElse(0.0);
        return new Result(Semantic.UNKNOWN, residual);
    }

    private static double bestScore(String h, Set<String> synonyms) {
        return synonyms.stream()
                .mapToDouble(syn -> jaroWinkler(h, syn))
                .max()
                .orElse(0.0);
    }

    private static Set<String> allSynonyms() {
        Set<String> all = new java.util.HashSet<>();
        all.addAll(KPI_SYNONYMS);
        all.addAll(VALUE_N_SYNONYMS);
        all.addAll(VALUE_N1_SYNONYMS);
        all.addAll(CATEGORY_SYNONYMS);
        all.addAll(UNIT_SYNONYMS);
        return all;
    }

    static double jaroWinkler(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int matchDist = Math.max(Math.max(s1.length(), s2.length()) / 2 - 1, 0);
        boolean[] s1Matched = new boolean[s1.length()];
        boolean[] s2Matched = new boolean[s2.length()];

        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDist);
            int end   = Math.min(i + matchDist + 1, s2.length());
            for (int j = start; j < end; j++) {
                if (!s2Matched[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matched[i] = true;
                    s2Matched[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matched[i]) continue;
            while (!s2Matched[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }

        double jaro = (matches / (double) s1.length()
                     + matches / (double) s2.length()
                     + (matches - transpositions / 2.0) / matches) / 3.0;

        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(s1.length(), s2.length())); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + prefix * 0.1 * (1.0 - jaro);
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
