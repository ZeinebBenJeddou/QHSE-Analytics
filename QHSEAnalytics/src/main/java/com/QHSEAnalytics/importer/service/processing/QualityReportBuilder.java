package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.shared.dto.response.ImportQualityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class QualityReportBuilder {

    @Value("${import.quality.score.valid:100}")
    private int scoreValid;

    @Value("${import.quality.score.warning:70}")
    private int scoreWarning;

    @Value("${import.quality.score.invalid:0}")
    private int scoreInvalid;

    @Value("${import.quality.max.issues.per.severity:200}")
    private int maxIssuesPerSeverity;

    public ImportQualityReport build(List<KpiRawDataDTO> rawData, boolean allowPartialImport) {
        String importMode = allowPartialImport ? "PARTIAL" : "STRICT";
        List<ImportIssue> allErrors   = new ArrayList<>();
        List<ImportIssue> allWarnings = new ArrayList<>();
        List<ImportIssue> allInfos    = new ArrayList<>();

        int uppercaseConverted = 0;
        int invalidRows = 0;
        int warningRows = 0;
        int duplicateRows = 0;
        int exactDuplicateRows = 0;
        int conflictDuplicateRows = 0;
        int outlierRows = 0;
        double scoreSum = 0.0;

        List<Integer> rejectedRowIndexes = new ArrayList<>();
        Map<String, Integer> rejectedReasonsMap = new java.util.HashMap<>();

        if (rawData != null) {
            for (KpiRawDataDTO row : rawData) {
                List<ImportIssue> issues = row.getIssues() == null ? List.of() : row.getIssues();

                if (row.getOriginalKpiName() != null && row.getKpiName() != null
                        && !row.getOriginalKpiName().equals(row.getKpiName())
                        && isAllCaps(row.getOriginalKpiName())) {
                    uppercaseConverted++;
                }

                boolean hasError   = issues.stream().anyMatch(i -> i.getSeverity() == ImportIssue.Severity.ERROR);
                boolean hasWarning = issues.stream().anyMatch(i -> i.getSeverity() == ImportIssue.Severity.WARNING);


                long duplicatesInThisRow = issues.stream().filter(i ->
                        ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode()) ||
                        ImportIssue.CODE_DUPLICATE_CONFLICT.equals(i.getCode())).count();
                duplicateRows += duplicatesInThisRow;

                exactDuplicateRows += issues.stream()
                        .filter(i -> ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode()))
                        .count();
                conflictDuplicateRows += issues.stream()
                        .filter(i -> ImportIssue.CODE_DUPLICATE_CONFLICT.equals(i.getCode()))
                        .count();

                boolean isOutlier = issues.stream().anyMatch(i ->
                        ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));

                if (isOutlier)  outlierRows++;


                int rowScore;
                if (!row.isValid() || hasError) {
                    rowScore = scoreInvalid; // 0 si au moins une erreur
                    invalidRows++;
                } else if (hasWarning) {
                    rowScore = scoreWarning; // 70 au moins un avert
                    warningRows++;
                } else {
                    rowScore = scoreValid; // 100 si ne prenste aucune anomalie
                }

                row.setRowQualityScore(rowScore);
                scoreSum += rowScore;


                for (ImportIssue issue : issues) {
                    switch (issue.getSeverity()) {
                        case ERROR   -> allErrors.add(issue);
                        case WARNING -> allWarnings.add(issue);
                        case INFO    -> allInfos.add(issue);
                    }
                }

                if (!row.isValid() || hasError) {
                    rejectedRowIndexes.add(row.getRowIndex());
                    for (ImportIssue issue : issues) {
                        if (issue.getSeverity() == ImportIssue.Severity.ERROR) {
                            rejectedReasonsMap.merge(issue.getCode(), 1, Integer::sum);
                        }
                    }
                }
            }
        }

        List<com.QHSEAnalytics.shared.dto.response.RejectedReasonSummary> rejectedReasons = rejectedReasonsMap.entrySet().stream()
                .map(e -> new com.QHSEAnalytics.shared.dto.response.RejectedReasonSummary(e.getKey(), e.getValue()))
                .toList();

        boolean errorsTruncated   = allErrors.size()   > maxIssuesPerSeverity;
        boolean warningsTruncated = allWarnings.size() > maxIssuesPerSeverity;
        boolean infosTruncated    = allInfos.size()    > maxIssuesPerSeverity;
        boolean issuesTruncated   = errorsTruncated || warningsTruncated || infosTruncated;

        List<ImportIssue> cappedErrors   = errorsTruncated
                ? new ArrayList<>(allErrors.subList(0, maxIssuesPerSeverity))   : allErrors;
        List<ImportIssue> cappedWarnings = warningsTruncated
                ? new ArrayList<>(allWarnings.subList(0, maxIssuesPerSeverity)) : allWarnings;
        List<ImportIssue> cappedInfos    = infosTruncated
                ? new ArrayList<>(allInfos.subList(0, maxIssuesPerSeverity))    : allInfos;

        if (issuesTruncated) {
            cappedInfos.add(ImportIssue.builder()
                    .code("ISSUES_TRUNCATED")
                    .severity(ImportIssue.Severity.INFO)
                    .message("Le nombre d'anomalies dépasse " + maxIssuesPerSeverity
                            + " par catégorie — seules les premières sont affichées.")
                    .build());
        }

        int uniqueRows = rawData == null ? 0 : rawData.size();
        int totalRows = uniqueRows + duplicateRows;
        int validRows = uniqueRows - invalidRows;
        int resultingRowsCount = uniqueRows;
        warningRows = allWarnings.size();
        long nullValuesCount = 0L;
        java.util.Map<String, Long> nullByToken = new java.util.LinkedHashMap<>();
        if (rawData != null) {
            java.util.Set<String> nullTokens = java.util.Set.of(
                    "n/a", "na", "nd", "nr", "nc", "–", "—", "/", "?", "x",
                    "néant", "neant", "aucun", "none", "null", "empty");
            nullByToken = rawData.stream()
                    .flatMap(r -> {
                        List<String> raws = new ArrayList<>();
                        if (r.getValeurN1Raw() != null) raws.add(r.getValeurN1Raw().trim().toLowerCase(java.util.Locale.ROOT));
                        if (r.getValeurNRaw()  != null) raws.add(r.getValeurNRaw().trim().toLowerCase(java.util.Locale.ROOT));
                        return raws.stream();
                    })
                    .filter(nullTokens::contains)
                    .collect(java.util.stream.Collectors.groupingBy(t -> t, java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()));
            nullValuesCount = nullByToken.values().stream().mapToLong(Long::longValue).sum();
        }
        String nullValuesDetail = nullByToken.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
        // Q
        double qualityScore = uniqueRows == 0 ? 0.0 : Math.round((scoreSum / uniqueRows) * 10.0) / 10.0;

        boolean hasHardBlocking = allErrors.stream().anyMatch(QualityReportBuilder::isHardBlockingIssue);
        boolean hasSoftBlocking = allErrors.stream().anyMatch(QualityReportBuilder::isSoftBlockingIssue);

        boolean blocking = allowPartialImport ? hasHardBlocking : (hasHardBlocking || hasSoftBlocking);

        int importedRowsCount = 0;
        int rejectedRowsCount = 0;

        if (!blocking) {
            importedRowsCount = validRows;
            rejectedRowsCount = allowPartialImport ? invalidRows : 0;
        } else {
            importedRowsCount = 0;
            rejectedRowsCount = totalRows;
        }

        String blockingReason = null;
        if (blocking) {
            if (hasHardBlocking) {
                blockingReason = "Erreur structurelle bloquante.";
            } else {
                blockingReason = allowPartialImport
                        ? "Import partiel impossible : aucune ligne valide exploitable."
                        : "Erreurs de données bloquantes en mode strict.";
            }
        }


        String anomaliesSummary = buildAnomaliesSummary(cappedErrors, cappedWarnings);
        log.info("\n[QUALITY]" +
                        "\n  Score global  : {}/100" +
                        "\n  Lignes valides: {} | Issues WARNING : {} | Invalides : {}" +
                        "\n  Anomalies     : {}",
                qualityScore,
                validRows, warningRows, invalidRows,
                anomaliesSummary.isEmpty() ? "aucune" : anomaliesSummary);

        return ImportQualityReport.builder()
                .importMode(importMode)
                .importedRowsCount(importedRowsCount)
                .rejectedRowsCount(rejectedRowsCount)
                .rejectedRowIndexes(rejectedRowIndexes)
                .rejectedReasons(rejectedReasons)
                .hardBlocking(hasHardBlocking)
                .softBlocking(hasSoftBlocking)
                .blockingReason(blockingReason)
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(invalidRows)
                .warningRows(warningRows)
                .duplicateRows(duplicateRows)
                .exactDuplicateRows(exactDuplicateRows)
                .conflictDuplicateRows(conflictDuplicateRows)
                .outlierRows(outlierRows)
                .uppercaseConvertedRows(uppercaseConverted)
                .nullValuesCount((int) nullValuesCount)
                .nullValuesDetail(nullValuesDetail.isBlank() ? "aucune" : nullValuesDetail)
                .resultingRowsCount(resultingRowsCount)
                .qualityScore(qualityScore)
                .blocking(blocking)
                .errors(cappedErrors)
                .warnings(cappedWarnings)
                .infos(cappedInfos)
                .issuesTruncated(issuesTruncated)
                .build();
    }

    private boolean isAllCaps(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String letters = value.replaceAll("[^a-zA-ZÀ-ÿ]", "");
        return !letters.isEmpty() && letters.equals(letters.toUpperCase());
    }

    private static String buildAnomaliesSummary(List<ImportIssue> errors, List<ImportIssue> warnings) {
        StringBuilder sb = new StringBuilder();
        for (ImportIssue issue : errors) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("[ERROR ligne %s %s]",
                    issue.getRowIndex() != null ? issue.getRowIndex() : "?",
                    issue.getColumn() != null ? issue.getColumn() : issue.getCode()));
        }
        for (ImportIssue issue : warnings) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("[WARN ligne %s %s]",
                    issue.getRowIndex() != null ? issue.getRowIndex() : "?",
                    issue.getColumn() != null ? issue.getColumn() : issue.getCode()));
        }
        return sb.toString();
    }

    private static boolean isHardBlockingIssue(ImportIssue issue) {
        if (issue == null || issue.getSeverity() != ImportIssue.Severity.ERROR) {
            return false;
        }
        return issue.getRowIndex() == null
                || ImportIssue.CODE_EMPTY_FILE.equals(issue.getCode())
                || ImportIssue.CODE_INVALID_FILE_TYPE.equals(issue.getCode())
                || ImportIssue.CODE_MISSING_REQUIRED_COLUMN.equals(issue.getCode());
    }

    private static boolean isSoftBlockingIssue(ImportIssue issue) {
        return issue != null && issue.getSeverity() == ImportIssue.Severity.ERROR && !isHardBlockingIssue(issue);
    }
}
