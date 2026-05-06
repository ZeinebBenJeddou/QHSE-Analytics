package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.ImportIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CleaningAgent: sanitisation, dédoublonnage tracé et détection outliers.
 *
 * Règles de dédoublonnage :
 *  - Doublon exact (même KPI normalisé + mêmes valeurs N/N-1) → garder une ligne + INFO DUPLICATE_KPI
 *  - Doublon avec valeurs différentes → garder la première + WARNING DUPLICATE_CONFLICT
 *
 * Les outliers (variation > 500 %) sont conservés mais signalés via WARNING OUTLIER_VARIATION.
 */
@Service
@Slf4j
public class CleaningAgent {

    private static final double OUTLIER_THRESHOLD_PERCENT = 500.0;

    public List<KpiRawDataDTO> clean(List<KpiRawDataDTO> rawData) {
        if (rawData == null || rawData.isEmpty()) return new ArrayList<>();

        log.info("CleaningAgent: démarrage pour {} lignes", rawData.size());

        List<KpiRawDataDTO> cleaned = rawData.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeRow)
                .collect(Collectors.toList());

        cleaned = deduplicateWithIssues(cleaned);

        log.info("CleaningAgent: terminé. Lignes résultantes : {}", cleaned.size());
        return cleaned;
    }

    // ── Sanitisation d'une ligne ─────────────────────────────────────────────
    private KpiRawDataDTO sanitizeRow(KpiRawDataDTO row) {
        // Normaliser les champs texte
        row.setKpiName(trimAndNormalize(row.getKpiName()));
        row.setCategorie(trimAndNormalize(row.getCategorie()));
        row.setUnite(trimAndNormalize(row.getUnite()));

        // Recalculer la clé de normalisation après trim (au cas où ExtractionAgent ne l'a pas fait)
        if (row.getNormalizedKpiName() == null && row.getKpiName() != null) {
            row.setNormalizedKpiName(ExtractionAgent.normalizeForMatching(row.getKpiName()));
        }

        // Détection outlier sur les lignes valides
        if (row.isValid() && row.getValeurN1() != null && row.getValeurN() != null) {
            double v1 = row.getValeurN1();
            double v  = row.getValeurN();
            if (v1 != 0) {
                double variation = Math.abs((v - v1) / v1) * 100;
                if (variation > OUTLIER_THRESHOLD_PERCENT) {
                    String msg = String.format(
                            "Variation extrême détectée pour '%s' (%.1f%%). Vérifiez les données.",
                            row.getKpiName(), variation);
                    log.warn("CleaningAgent: outlier détecté — {}", msg);

                    // Ajouter issue WARNING (ne rend pas la ligne invalide)
                    List<ImportIssue> issues = new ArrayList<>(
                            row.getIssues() == null ? List.of() : row.getIssues());
                    issues.add(ImportIssue.builder()
                            .rowIndex(row.getRowIndex())
                            .column("Valeur N / Valeur N-1")
                            .code(ImportIssue.CODE_OUTLIER_VARIATION)
                            .severity(ImportIssue.Severity.WARNING)
                            .message(msg)
                            .build());
                    row.setIssues(issues);

                    // Mettre à jour le message de validation affichable
                    row.setValidationMessage(msg);
                }
            }
        }
        return row;
    }

    // ── Dédoublonnage tracé ──────────────────────────────────────────────────
    private List<KpiRawDataDTO> deduplicateWithIssues(List<KpiRawDataDTO> rows) {
        // clé : normalizedKpiName (ou "UNKNOWN_<rowIndex>" si null)
        Map<String, KpiRawDataDTO> seen = new LinkedHashMap<>();
        List<KpiRawDataDTO> result = new ArrayList<>();

        for (KpiRawDataDTO row : rows) {
            String key = buildDedupeKey(row);

            if (!seen.containsKey(key)) {
                seen.put(key, row);
                result.add(row);
                continue;
            }

            KpiRawDataDTO existing = seen.get(key);
            boolean exactDuplicate = isExactDuplicate(existing, row);

            if (exactDuplicate) {
                // Doublon exact → INFO sur la ligne conservée
                log.info("CleaningAgent: doublon exact ignoré pour '{}' (ligne {})",
                        row.getKpiName(), row.getRowIndex());
                addIssueToRow(existing,
                        ImportIssue.builder()
                                .rowIndex(row.getRowIndex())
                                .column("KPI")
                                .code(ImportIssue.CODE_DUPLICATE_KPI)
                                .severity(ImportIssue.Severity.INFO)
                                .message(String.format(
                                        "Doublon exact ignoré : '%s' (ligne %d est identique à la ligne %d)",
                                        row.getKpiName(), row.getRowIndex(), existing.getRowIndex()))
                                .build());
            } else {
                // Doublon avec valeurs différentes → WARNING sur la ligne conservée
                log.warn("CleaningAgent: doublon conflictuel pour '{}' (lignes {} et {})",
                        row.getKpiName(), existing.getRowIndex(), row.getRowIndex());
                addIssueToRow(existing,
                        ImportIssue.builder()
                                .rowIndex(row.getRowIndex())
                                .column("KPI")
                                .code(ImportIssue.CODE_DUPLICATE_CONFLICT)
                                .severity(ImportIssue.Severity.WARNING)
                                .message(String.format(
                                        "Doublon conflictuel : '%s' présent en lignes %d et %d avec des valeurs différentes. Première occurrence conservée.",
                                        row.getKpiName(), existing.getRowIndex(), row.getRowIndex()))
                                .build());
            }
            // Dans les deux cas on ne conserve que la première occurrence
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String buildDedupeKey(KpiRawDataDTO row) {
        String norm = row.getNormalizedKpiName();
        if (norm == null || norm.isBlank()) {
            return "UNKNOWN_" + row.getRowIndex();
        }
        return norm;
    }

    private boolean isExactDuplicate(KpiRawDataDTO a, KpiRawDataDTO b) {
        return Objects.equals(a.getValeurN(),  b.getValeurN())
                && Objects.equals(a.getValeurN1(), b.getValeurN1());
    }

    /** Ajoute une issue à un DTO dont la liste peut être immutable (issue de @Singular). */
    private void addIssueToRow(KpiRawDataDTO row, ImportIssue issue) {
        List<ImportIssue> mutable = new ArrayList<>(
                row.getIssues() == null ? List.of() : row.getIssues());
        mutable.add(issue);
        row.setIssues(mutable);
    }

    private String trimAndNormalize(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }
}
