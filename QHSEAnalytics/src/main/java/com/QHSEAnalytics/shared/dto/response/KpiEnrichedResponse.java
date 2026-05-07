package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Combined view of one KPI row: raw import data + Gemini AI analysis.
 * Returned by the KpiEnrichmentController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiEnrichedResponse {

    // ── Raw import preview data ────────────────────────────────────────────
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

    // ── AI Analysis ───────────────────────────────────────────────────────
    private Long analysisId;
    private String riskLevel;          // Faible | Modéré | Élevé
    private String riskJustification;
    private Boolean objectiveReached;
    private Boolean improvementDetected;
    private String issueDetected;
    private String correctiveAction;
    private String preventiveAction;
    private String immediateAction;
    private String immediatePriority;
    private boolean requires8d;
    private String eightDDetails;      // JSON string with D1-D8
    private String aiNote;

    private LocalDateTime createdAt;
}
