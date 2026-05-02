package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CleaningAgent: Data sanitization and quality control
 * 
 * Responsibilities:
 * - Sanitize numeric values and text fields
 * - Detect outliers (extreme variations)
 * - deduplicate raw KPI entries
 * - Flag data quality issues for user review
 */
@Service
@Slf4j
public class CleaningAgent {

    private static final double OUTLIER_THRESHOLD_PERCENT = 500.0; // Flag variations > 500%

    public List<KpiRawDataDTO> clean(List<KpiRawDataDTO> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("CleaningAgent: Starting data cleaning for {} rows", rawData.size());

        List<KpiRawDataDTO> cleaned = rawData.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeRow)
                .collect(Collectors.toList());

        // Deduplication based on normalized name
        cleaned = deduplicate(cleaned);

        log.info("CleaningAgent: Completed. Resulting rows: {}", cleaned.size());
        return cleaned;
    }

    private KpiRawDataDTO sanitizeRow(KpiRawDataDTO row) {
        // Sanitize strings
        row.setKpiName(trimAndNormalize(row.getKpiName()));
        row.setCategorie(trimAndNormalize(row.getCategorie()));
        row.setUnite(trimAndNormalize(row.getUnite()));

        // Outlier detection if valid
        if (row.isValid() && row.getValeurN1() != null && row.getValeurN() != null) {
            double v1 = row.getValeurN1();
            double v = row.getValeurN();
            
            if (v1 != 0) {
                double variation = Math.abs((v - v1) / v1) * 100;
                if (variation > OUTLIER_THRESHOLD_PERCENT) {
                    log.warn("CleaningAgent: Outlier detected for {}: variation={}%", row.getKpiName(), variation);
                    row.setValidationMessage(String.format("Variation extrême détectée (%.1f%%). Vérifiez les données.", variation));
                    // We keep it valid but flagged
                }
            }
        }

        return row;
    }

    private List<KpiRawDataDTO> deduplicate(List<KpiRawDataDTO> rows) {
        // Simple deduplication: if multiple rows have the same normalized name, keep the first one
        // In a real RAG/AI scenario, we might want to merge them, but for QHSE Excel import, 
        // duplicate names usually mean header repetition or error.
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (row.getKpiName() == null ? "UNKNOWN_" + row.getRowIndex() : row.getKpiName().toLowerCase().trim()),
                        row -> row,
                        (existing, replacement) -> {
                            log.debug("CleaningAgent: Deduplicating row for {}", existing.getKpiName());
                            return existing;
                        }
                ))
                .values()
                .stream()
                .toList();
    }

    private String trimAndNormalize(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " "); // Normalize internal spaces
    }
}
