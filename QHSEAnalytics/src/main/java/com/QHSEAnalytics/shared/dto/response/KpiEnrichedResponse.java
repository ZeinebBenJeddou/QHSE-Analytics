package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiEnrichedResponse {


    private Long previewId;
    private Long importSessionId;
    private String kpiName;
    private String category;
    private String unit;
    private String definition;
    private Double valueN;
    private Double valueN1;
    private String status;
    private String commentaire;
    private Double variationPercent;
    private Double ecart;


    private Long analysisId;
    private String riskLevel;
    private String riskJustification;
    private Boolean objectiveReached;
    private Boolean improvementDetected;
    private String issueDetected;
    private String correctiveAction;
    private String preventiveAction;
    private String immediateAction;
    private String immediatePriority;
    private boolean requires8d;
    private String eightDDetails;
    private String aiNote;

    private LocalDateTime createdAt;
}
