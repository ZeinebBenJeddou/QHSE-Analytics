package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportQualityReport {

    private String importMode;
    private int importedRowsCount;
    private int rejectedRowsCount;
    private List<Integer> rejectedRowIndexes;
    private List<RejectedReasonSummary> rejectedReasons;
    private boolean hardBlocking;
    private boolean softBlocking;
    private String blockingReason;



    private int totalRows;


    private int validRows;


    private int invalidRows;


    private int warningRows;


    private int duplicateRows;


    private int outlierRows;


    private double qualityScore;


    private boolean blocking;


    private List<ImportIssue> errors;


    private List<ImportIssue> warnings;


    private List<ImportIssue> infos;


    private List<ImportIssue> extractionIssues;
}
