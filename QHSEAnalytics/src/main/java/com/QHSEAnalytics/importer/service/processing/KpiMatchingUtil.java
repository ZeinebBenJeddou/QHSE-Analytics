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

        Matcher matcher = WORD_PATTERN.matcher(normalized);
        int wordCount = 0;
        while (matcher.find()) {
            wordCount++;
            if (wordCount >= 2) {
                return 0.7;
            }
        }

        return 0.3;
    }
}
