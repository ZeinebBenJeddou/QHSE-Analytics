package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryScoreDTO {
    private String categoryCode;
    private String categoryLibelle;
    private int    kpiCount;
    private int    faibleCount;
    private int    modereCount;
    private int    critiqueCount;
    private int    indetermineCount;

    private double compositeScore;
    private String compositeLabel;
}
