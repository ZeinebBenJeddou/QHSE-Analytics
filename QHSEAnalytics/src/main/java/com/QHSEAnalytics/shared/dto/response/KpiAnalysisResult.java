package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiAnalysisResult {

    private String kpiName;


    private String riskLevel;
    private String riskJustification;
    private String identificationRisque;
    private Boolean objectiveReached;
    private Boolean improvementDetected;

    private String issueDetected;
    private String problemeDetecte;


    private String correctiveAction;
    private String preventiveAction;
    private String actionsPreventives;


    private String immediateAction;
    private String immediatePriority;
    private String actionImmediate;
    private String prioriteAction;


    private boolean requires8d;
    private String eightDDetails;
    private String methode8D;


    private String aiNote;
    private String noteFinale;
}
