package com.QHSEAnalytics.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportIssue {

    /** Index de ligne dans le fichier (null si l'issue concerne le fichier entier). */
    private Integer rowIndex;

    /** Nom de la colonne concernée (null si non applicable). */
    private String column;

    /** Code machine de l'issue. */
    private String code;

    /** Gravité : ERROR, WARNING ou INFO. */
    private Severity severity;

    /** Message lisible par l'utilisateur. */
    private String message;

    /** Valeur brute d'origine avant correction (null si non applicable). */
    private String originalValue;

    /** Valeur après correction automatique (null si aucune correction). */
    private String cleanedValue;

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    // ---------------------------------------------------------------
    // Codes d'issues reconnus par le pipeline
    // ---------------------------------------------------------------
    public static final String CODE_EMPTY_FILE              = "EMPTY_FILE";
    public static final String CODE_INVALID_FILE_TYPE       = "INVALID_FILE_TYPE";
    public static final String CODE_MISSING_REQUIRED_COLUMN = "MISSING_REQUIRED_COLUMN";
    public static final String CODE_MISSING_KPI_NAME        = "MISSING_KPI_NAME";
    public static final String CODE_MISSING_VALUE_N         = "MISSING_VALUE_N";
    public static final String CODE_MISSING_VALUE_N1        = "MISSING_VALUE_N1";
    public static final String CODE_INVALID_NUMBER          = "INVALID_NUMBER";
    public static final String CODE_AMBIGUOUS_NUMBER        = "AMBIGUOUS_NUMBER";
    public static final String CODE_DUPLICATE_KPI           = "DUPLICATE_KPI";
    public static final String CODE_DUPLICATE_CONFLICT      = "DUPLICATE_CONFLICT";
    public static final String CODE_OUTLIER_VARIATION       = "OUTLIER_VARIATION";
    public static final String CODE_LOW_CONFIDENCE          = "LOW_CONFIDENCE_EXTRACTION";
    public static final String CODE_UNKNOWN_CATEGORY        = "UNKNOWN_CATEGORY";
    public static final String CODE_UNKNOWN_UNIT            = "UNKNOWN_UNIT";
    public static final String CODE_AUTO_CORRECTED          = "AUTO_CORRECTED_VALUE";
}
