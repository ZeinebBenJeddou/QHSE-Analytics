package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColumnProfileDTO {
    private int    columnIndex;
    private String detectedHeader;

    private String inferredType;
    private int    totalRows;
    private int    nullCount;

    private String likelySemantic;
    @Builder.Default
    private double confidenceScore = 0.0;

    private String warningMessage;
}
