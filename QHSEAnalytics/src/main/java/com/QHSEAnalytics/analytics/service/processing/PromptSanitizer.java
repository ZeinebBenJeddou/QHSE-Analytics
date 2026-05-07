package com.QHSEAnalytics.analytics.service.processing;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sanitise les données utilisateurs avant injection dans les prompts LLM.
 * Protège contre le prompt injection : instructions cachées dans les noms de KPI
 * ou valeurs provenant du fichier Excel.
 */
@Component
public class PromptSanitizer {

    private static final int MAX_FIELD_LENGTH = 200;

    // Mots-clés d'injection LLM courants
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        "(?i)(ignore\\s+(previous|all|above)|forget\\s+instructions?|" +
        "system\\s*:|override\\s+instructions?|act\\s+as|you\\s+are\\s+now|" +
        "disregard\\s+(all|previous)|new\\s+instructions?:|\\[INST\\]|<\\|im_start\\|>)",
        Pattern.CASE_INSENSITIVE
    );

    // Séquences de contrôle et retours à la ligne multiples
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\r\\n\\t]+");
    private static final Pattern MULTI_SPACE   = Pattern.compile("\\s{3,}");

    /**
     * Sanitise un champ texte provenant du fichier Excel ou d'une entrée utilisateur.
     */
    public String sanitize(String input) {
        if (input == null) return "N/A";

        String cleaned = input.trim();

        // 1. Supprimer les retours à la ligne (vecteurs d'injection classiques)
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll(" ");

        // 2. Détecter et neutraliser les tentatives d'injection LLM
        if (INJECTION_PATTERN.matcher(cleaned).find()) {
            cleaned = "[CONTENU FILTRÉ]";
        }

        // 3. Supprimer les espaces excessifs
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();

        // 4. Limiter la longueur pour éviter les débordements de contexte
        if (cleaned.length() > MAX_FIELD_LENGTH) {
            cleaned = cleaned.substring(0, MAX_FIELD_LENGTH) + "…";
        }

        return cleaned.isBlank() ? "N/A" : cleaned;
    }

    /**
     * Sanitise une valeur numérique affichée dans le prompt.
     */
    public String sanitizeNumber(Number value) {
        if (value == null) return "0";
        double d = value.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) return "0";
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d);
    }
}
