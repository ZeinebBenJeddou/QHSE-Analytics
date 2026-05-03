package com.QHSEAnalytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Structured output from Gemini for a single KPI analysis request.
 * This is used internally for JSON parsing; the mapped fields are then
 * persisted into {@link com.QHSEAnalytics.entity.KpiAnalysis}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiAnalysisResult {

    private String kpiName;

    // ── Risk ──────────────────────────────────────────────────────────────
    private String riskLevel;          // Faible | Modéré | Élevé
    private String riskJustification;
    private Boolean objectiveReached;
    private Boolean improvementDetected;

    // ── Issue detection ───────────────────────────────────────────────────
    private String issueDetected;      // e.g. "Anomalie: chute de 22%"

    // ── Action plans ──────────────────────────────────────────────────────
    private String correctiveAction;
    private String preventiveAction;

    // ── Immediate actions ─────────────────────────────────────────────────
    private String immediateAction;
    private String immediatePriority;  // Haute | Moyenne | Basse

    // ── 8D ────────────────────────────────────────────────────────────────
    private boolean requires8d;
    private String eightDDetails;      // JSON string with D1-D8 steps

    // ── Final note ────────────────────────────────────────────────────────
    private String aiNote;
}
