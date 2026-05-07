package com.QHSEAnalytics.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalizeForPrompt(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        cleaned = cleaned.replace('\u00A0', ' ');
        cleaned = cleaned.replaceAll("[\u2018\u2019\u201A\u201B\u2032\u2035]", "'");
        cleaned = cleaned.replaceAll("[\u201C\u201D\u201E\u201F\u00AB\u00BB]", "\"");
        cleaned = cleaned.replaceAll("[\u2013\u2014\u2015]", "-");
        cleaned = cleaned.replaceAll("\u2026", "...");
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
