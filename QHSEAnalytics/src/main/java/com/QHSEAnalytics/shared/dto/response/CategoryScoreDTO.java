package com.QHSEAnalytics.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryScoreDTO {
    private String categoryCode;
    private String categoryLibelle;
    private int    kpiCount;
    private int    excellentCount;
    private int    faibleCount;
    private int    preEscaladeCount;
    private int    modereCount;
    private int    critiqueCount;
    private int    indetermineCount;
    // Weighted score: EXCELLENT=100, FAIBLE=80, INDETERMINE=50, MODERE=40, PRE_ESCALADE=20, CRITIQUE=0
    private double compositeScore;   // 0–100
    private String compositeLabel;   // Excellent / Bon / Acceptable / À surveiller / Critique
}
