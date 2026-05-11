package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ColumnProfileDTO {
    private int    columnIndex;
    private String detectedHeader;

    private String inferredType;
    private List<String> sampleValues;
    private int    totalRows;
    private int    nullCount;
    private int    uniqueCount;
    private Double numericMin;
    private Double numericMax;
    private Double numericMean;

    private String likelySemantic;
    private double semanticConfidence;
}
