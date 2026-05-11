package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import com.QHSEAnalytics.shared.dto.response.ImportQualityReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class QualityReportBuilder {


    public ImportQualityReport build(List<KpiRawDataDTO> rawData, boolean allowPartialImport) {
        String importMode = allowPartialImport ? "PARTIAL" : "STRICT";
        List<ImportIssue> allErrors   = new ArrayList<>();
        List<ImportIssue> allWarnings = new ArrayList<>();
        List<ImportIssue> allInfos    = new ArrayList<>();

        int invalidRows = 0;
        int warningRows = 0;
        int duplicateRows = 0;
        int outlierRows = 0;
        double scoreSum = 0.0;

        List<Integer> rejectedRowIndexes = new ArrayList<>();
        Map<String, Integer> rejectedReasonsMap = new java.util.HashMap<>();

        if (rawData != null) {
            for (KpiRawDataDTO row : rawData) {
                List<ImportIssue> issues = row.getIssues() == null ? List.of() : row.getIssues();

                boolean hasError   = issues.stream().anyMatch(i -> i.getSeverity() == ImportIssue.Severity.ERROR);
                boolean hasWarning = issues.stream().anyMatch(i -> i.getSeverity() == ImportIssue.Severity.WARNING);


                long duplicatesInThisRow = issues.stream().filter(i ->
                        ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode()) ||
                        ImportIssue.CODE_DUPLICATE_CONFLICT.equals(i.getCode())).count();
                duplicateRows += duplicatesInThisRow;

                boolean isOutlier = issues.stream().anyMatch(i ->
                        ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));

                if (isOutlier)  outlierRows++;


                int rowScore;
                if (!row.isValid() || hasError) {
                    rowScore = 0;
                    invalidRows++;
                } else if (hasWarning) {
                    rowScore = 70;
                    warningRows++;
                } else {
                    rowScore = 100;
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

        int uniqueRows = rawData == null ? 0 : rawData.size();
        int totalRows = uniqueRows + duplicateRows;
        int validRows = uniqueRows - invalidRows;
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
                .outlierRows(outlierRows)
                .qualityScore(qualityScore)
                .blocking(blocking)
                .errors(allErrors)
                .warnings(allWarnings)
                .infos(allInfos)
                .build();
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
