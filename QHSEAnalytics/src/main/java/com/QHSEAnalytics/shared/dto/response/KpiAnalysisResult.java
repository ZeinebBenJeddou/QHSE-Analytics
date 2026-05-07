package com.QHSEAnalytics.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Structured output from Gemini for a single KPI analysis request.
 * This is used internally for JSON parsing; the mapped fields are then
 * persisted into {@link com.QHSEAnalytics.shared.entity.KpiAnalysis}.
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
    private String identificationRisque;
    private Boolean objectiveReached;
    private Boolean improvementDetected;

    // ── Issue detection ───────────────────────────────────────────────────
    private String issueDetected;      // e.g. "Anomalie: chute de 22%"
    private String problemeDetecte;

    // ── Action plans ──────────────────────────────────────────────────────
    private String correctiveAction;
    private String preventiveAction;
    private String actionsPreventives;

    // ── Immediate actions ─────────────────────────────────────────────────
    private String immediateAction;
    private String immediatePriority;  // Haute | Moyenne | Basse
    private String actionImmediate;
    private String prioriteAction;

    // ── 8D ────────────────────────────────────────────────────────────────
    private boolean requires8d;
    private String eightDDetails;      // JSON string with D1-D8 steps
    private String methode8D;

    // ── Final note ────────────────────────────────────────────────────────
    private String aiNote;
    private String noteFinale;
}
