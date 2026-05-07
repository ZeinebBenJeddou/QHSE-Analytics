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


    /** Nombre total de lignes lues, doublons inclus. */
    private int totalRows;

    /** Lignes uniques conservées et valides sans warning. */
    private int validRows;

    /** Lignes uniques rejetées ou invalides (au moins une ERROR). */
    private int invalidRows;

    /** Lignes uniques valides avec au moins un warning. */
    private int warningRows;

    /** Occurrences de doublons écartées. */
    private int duplicateRows;

    /** Lignes uniques ayant une variation outlier. */
    private int outlierRows;

    /**
     * Score qualité global (0–100).
     * Calculé comme la moyenne des scores lignes :
     *  - ligne valide sans warning : 100
     *  - ligne valide avec warning : 70
     *  - ligne invalide            : 0
     */
    private double qualityScore;

    /**
     * true s'il existe au moins une ERROR bloquante.
     * Quand blocking=true, l'import confirmé doit être refusé.
     */
    private boolean blocking;

    /** Issues de sévérité ERROR. */
    private List<ImportIssue> errors;

    /** Issues de sévérité WARNING. */
    private List<ImportIssue> warnings;

    /** Issues de sévérité INFO. */
    private List<ImportIssue> infos;

    /** Issues globales d'extraction ou de mapping, non rattachées à une ligne. */
    private List<ImportIssue> extractionIssues;
}
