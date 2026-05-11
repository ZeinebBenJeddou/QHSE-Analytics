package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportIssue {


    private Integer rowIndex;

    private String column;


    private String code;


    private Severity severity;


    private String message;


    private String originalValue;


    private String cleanedValue;

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }


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
