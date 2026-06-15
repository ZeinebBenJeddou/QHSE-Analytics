package com.QHSEAnalytics.importer.service.processing;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class KpiMatchingUtil {

    private static final Set<String> STRONG_KPI_TERMS = Set.of(
            "kpi",
            "indicateur",
            "taux",
            "accident",
            "incident",
            "reclamation",
            "energie",
            "emission",
            "qualite",
            "securite",
            "environnement",
            "production",
            "defaut",
            "arret"
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z]{2,}");

    private KpiMatchingUtil() {
    }


    public static double computeConfidence(String rawKpiName) {
        String normalized = ExcelParserUtil.normalizeText(rawKpiName);
        if (normalized.isBlank()) {
            return 0.3;
        }


        for (String term : STRONG_KPI_TERMS) {
            if (normalized.contains(term)) {
                return 1.0;
            }
        }


        double bestScore = 0.0;
        for (String term : STRONG_KPI_TERMS) {
            double score = jaroWinkler(normalized, term);
            if (score > bestScore) {
                bestScore = score;
            }
        }
        if (bestScore >= 0.85) {
            return 0.90;
        }
        if (bestScore >= 0.70) {
            return 0.70;
        }


        Matcher matcher = WORD_PATTERN.matcher(normalized);
        int wordCount = 0;
        while (matcher.find()) {
            wordCount++;
            if (wordCount >= 2) {
                return 0.50;
            }
        }

        return 0.30;
    }


    private static double jaro(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }

        int len1 = s1.length();
        int len2 = s2.length();
        int window = Math.max(Math.max(len1, len2) / 2 - 1, 0);

        boolean[] matched1 = new boolean[len1];
        boolean[] matched2 = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - window);
            int end   = Math.min(i + window + 1, len2);
            for (int j = start; j < end; j++) {
                if (!matched2[j] && s1.charAt(i) == s2.charAt(j)) {
                    matched1[i] = true;
                    matched2[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0.0;
        }


        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!matched1[i]) continue;
            while (!matched2[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        double m = matches;
        return ((m / len1) + (m / len2) + ((m - transpositions / 2.0) / m)) / 3.0;
    }


    private static double jaroWinkler(String s1, String s2) {
        double jaroScore = jaro(s1, s2);

        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        while (prefixLength < maxPrefix && s1.charAt(prefixLength) == s2.charAt(prefixLength)) {
            prefixLength++;
        }

        return jaroScore + (prefixLength * 0.1 * (1.0 - jaroScore));
    }
}
