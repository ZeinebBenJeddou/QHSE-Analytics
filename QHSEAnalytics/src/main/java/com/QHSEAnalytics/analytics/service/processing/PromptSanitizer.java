package com.QHSEAnalytics.analytics.service.processing;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;


@Component
public class PromptSanitizer {

    private static final int MAX_FIELD_LENGTH = 200;


    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        "(?i)(ignore\\s+(previous|all|above)|forget\\s+instructions?|" +
        "system\\s*:|override\\s+instructions?|act\\s+as|you\\s+are\\s+now|" +
        "disregard\\s+(all|previous)|new\\s+instructions?:|\\[INST\\]|<\\|im_start\\|>)",
        Pattern.CASE_INSENSITIVE
    );


    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\r\\n\\t]+");
    private static final Pattern MULTI_SPACE   = Pattern.compile("\\s{3,}");


    public String sanitize(String input) {
        return sanitize(input, MAX_FIELD_LENGTH);
    }


    public String sanitize(String input, int maxLength) {
        if (input == null) return "N/A";

        String cleaned = input.trim();


        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll(" ");


        if (INJECTION_PATTERN.matcher(cleaned).find()) {
            cleaned = "[CONTENU FILTRÉ]";
        }


        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();


        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength) + "…";
        }

        return cleaned.isBlank() ? "N/A" : cleaned;
    }


    public String sanitizeNumber(Number value) {
        if (value == null) return "0";
        double d = value.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) return "0";
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d);
    }
}
