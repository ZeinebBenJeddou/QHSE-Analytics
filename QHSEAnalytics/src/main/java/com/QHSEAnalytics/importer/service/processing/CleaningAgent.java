package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CleaningAgent {

    @Value("${import.cleaning.outlier.threshold.percent:500.0}")
    private double outlierThresholdPercent;

    @Value("${import.cleaning.score.base:100}")
    private int scoreBase;

    @Value("${import.cleaning.score.penalty.invalid:50}")
    private int penaltyInvalid;

    @Value("${import.cleaning.score.penalty.error:30}")
    private int penaltyError;

    @Value("${import.cleaning.score.penalty.warning:10}")
    private int penaltyWarning;

    @Value("${import.cleaning.score.penalty.info:5}")
    private int penaltyInfo;

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


    private KpiRawDataDTO sanitizeRow(KpiRawDataDTO row) {

        String cleanedName     = toTitleCaseIfAllCaps(trimAndNormalize(row.getKpiName()));
        String cleanedCategorie = trimAndNormalize(row.getCategorie());
        String cleanedUnite    = trimAndNormalize(row.getUnite());

        // normalizedKpiName calculé sur le nom déjà nettoyé, pas sur l'original
        String normalizedName = (row.getNormalizedKpiName() != null)
                ? row.getNormalizedKpiName()
                : (cleanedName != null ? ExtractionAgent.normalizeForMatching(cleanedName) : null);

        // Copie défensive de la liste d'issues — ne jamais toucher l'original
        List<ImportIssue> issues = new ArrayList<>(
                row.getIssues() == null ? List.of() : row.getIssues());

        String validationMessage = row.getValidationMessage();

        if (row.isValid() && row.getValeurN1() != null && row.getValeurN() != null) {
            double v1 = row.getValeurN1();
            double v  = row.getValeurN();
            if (v1 != 0) {
                double variation = Math.abs((v - v1) / v1) * 100;
                if (variation > outlierThresholdPercent) {
                    String msg = String.format(
                            "Variation extrême détectée pour '%s' (%.1f%%). Vérifiez les données.",
                            cleanedName, variation);
                    log.warn("CleaningAgent: outlier détecté — {}", msg);

                    issues.add(ImportIssue.builder()
                            .rowIndex(row.getRowIndex())
                            .column("Valeur N / Valeur N-1")
                            .code(ImportIssue.CODE_OUTLIER_VARIATION)
                            .severity(ImportIssue.Severity.WARNING)
                            .message(msg)
                            .build());

                    validationMessage = msg;
                }
            }
        }

        int score = scoreBase;
        if (!row.isValid()) score -= penaltyInvalid;
        for (ImportIssue issue : issues) {
            switch (issue.getSeverity()) {
                case ERROR   -> score -= penaltyError;
                case WARNING -> score -= penaltyWarning;
                case INFO    -> score -= penaltyInfo;
            }
        }
        score = Math.max(0, score);

        return KpiRawDataDTO.builder()
                .rowIndex(row.getRowIndex())
                .kpiName(cleanedName)
                .originalKpiName(row.getOriginalKpiName())
                .normalizedKpiName(normalizedName)
                .categorie(cleanedCategorie)
                .unite(cleanedUnite)
                .valeurN1(row.getValeurN1())
                .valeurN(row.getValeurN())
                .valeurN1Raw(row.getValeurN1Raw())
                .valeurNRaw(row.getValeurNRaw())
                .valid(row.isValid())
                .validationMessage(validationMessage)
                .methodeExtraction(row.getMethodeExtraction())
                .scoreConfiance(row.getScoreConfiance())
                .rowQualityScore(score)
                .issues(issues)
                .build();
    }


    private List<KpiRawDataDTO> deduplicateWithIssues(List<KpiRawDataDTO> rows) {

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

        }
        return result;
    }


    private String buildDedupeKey(KpiRawDataDTO row) {
        String norm = row.getNormalizedKpiName();
        if (norm == null || norm.isBlank()) {
            return "UNKNOWN_" + row.getRowIndex();
        }
        String cat = row.getCategorie() != null ? row.getCategorie().trim().toLowerCase() : "unknown";
        return norm + "|" + cat;
    }

    private boolean isExactDuplicate(KpiRawDataDTO a, KpiRawDataDTO b) {
        return Objects.equals(a.getValeurN(),  b.getValeurN())
                && Objects.equals(a.getValeurN1(), b.getValeurN1());
    }


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

    private String toTitleCaseIfAllCaps(String value) {
        if (value == null || value.isBlank()) return value;
        String letters = value.replaceAll("[^a-zA-ZÀ-ÿ]", "");
        if (letters.isEmpty()) return value;
        if (!letters.equals(letters.toUpperCase())) return value;
        return Arrays.stream(value.split("\\s+"))
                .map(word -> word.isEmpty() ? word :
                        Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
