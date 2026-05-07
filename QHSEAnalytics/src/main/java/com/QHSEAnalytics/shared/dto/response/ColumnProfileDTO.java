package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ColumnProfileDTO {
    private int    columnIndex;
    private String detectedHeader;
    // TEXT | NUMERIC | BOOLEAN | DATE | MIXED
    private String inferredType;
    private List<String> sampleValues;
    private int    totalRows;
    private int    nullCount;
    private int    uniqueCount;
    private Double numericMin;
    private Double numericMax;
    private Double numericMean;
    // KPI_NAME | VALUE_N | VALUE_N1 | CATEGORY | UNIT | UNKNOWN
    private String likelySemantic;
    private double semanticConfidence; // 0.0–1.0
}
