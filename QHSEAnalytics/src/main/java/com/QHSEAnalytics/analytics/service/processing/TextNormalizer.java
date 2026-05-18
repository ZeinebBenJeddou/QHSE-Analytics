package com.QHSEAnalytics.analytics.service.processing;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalizeForPrompt(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        cleaned = cleaned.replace(' ', ' ');
        cleaned = cleaned.replaceAll("[‘’‚‛′‵]", "'");
        cleaned = cleaned.replaceAll("[“”„‟«»]", "\"");
        cleaned = cleaned.replaceAll("[–—―]", "-");
        cleaned = cleaned.replaceAll("…", "...");
        cleaned = cleaned.replaceAll("[\\u0000-\\u001F\\u007F]", " ");
        cleaned = normalizeUnicode(cleaned);
        cleaned = cleaned.replaceAll("[\\uFFFD\\u00B4\\u02BC\\u02BB]", "'");
        cleaned = cleaned.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        cleaned = collapseWhitespace(cleaned);
        return cleaned;
    }

    public static String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        String normalized = normalizeForPrompt(value);
        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9'\\- ]", " ");
        normalized = collapseWhitespace(normalized);
        return normalized;
    }

    public static String normalizeForMatching(String value) {
        return normalizeForSearch(value)
                .replaceAll("[\\s\\-']+", " ")
                .trim();
    }

    private static String collapseWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeUnicode(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        normalized = DIACRITICS.matcher(Normalizer.normalize(normalized, Normalizer.Form.NFD)).replaceAll("");
        return normalized;
    }
}
