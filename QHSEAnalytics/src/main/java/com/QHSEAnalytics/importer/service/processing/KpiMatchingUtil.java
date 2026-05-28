package com.QHSEAnalytics.importer.service.processing;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for evaluating how likely a raw cell label is a QHSE KPI name.
 *
 * <p>Confidence is computed in three stages:
 * <ol>
 *   <li>Exact substring match against domain terms → 1.0</li>
 *   <li>Jaro-Winkler fuzzy similarity against domain terms → 0.90 or 0.70</li>
 *   <li>Word-count fallback → 0.50 or 0.30</li>
 * </ol>
 */
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

    /**
     * Returns a confidence score in [0.0, 1.0] indicating how likely {@code rawKpiName}
     * is a QHSE indicator name.
     *
     * @param rawKpiName raw cell text from the imported Excel file
     * @return confidence score
     */
    public static double computeConfidence(String rawKpiName) {
        String normalized = ExcelParserUtil.normalizeText(rawKpiName);
        if (normalized.isBlank()) {
            return 0.3;
        }

        // Stage 1 — exact substring match
        for (String term : STRONG_KPI_TERMS) {
            if (normalized.contains(term)) {
                return 1.0;
            }
        }

        // Stage 2 — Jaro-Winkler fuzzy match
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

        // Stage 3 — word-count fallback
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

    /**
     * Computes the Jaro similarity between two strings.
     *
     * <p>The matching window is {@code floor(max(len1, len2) / 2) - 1}.
     * Characters within that window are considered "common"; half the number of
     * out-of-order common characters counts as transpositions.
     *
     * @param s1 first string (already normalised, non-null)
     * @param s2 second string (already normalised, non-null)
     * @return Jaro score in [0.0, 1.0]; 0.0 when either string is empty
     */
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

        // Count transpositions: positions where matched chars differ in order
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

    /**
     * Computes the Jaro-Winkler similarity, which boosts the Jaro score when
     * the two strings share a common prefix (up to 4 characters).
     *
     * <p>The boost factor {@code p} is fixed at 0.1, the standard value that
     * guarantees the result stays within [0.0, 1.0].
     *
     * @param s1 first string
     * @param s2 second string
     * @return Jaro-Winkler score in [0.0, 1.0]
     */
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
