package com.QHSEAnalytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the full Gemini AI analysis result for a single KPI line,
 * including risk assessment, issue detection, action plans, and the
 * optional 8D problem-solving output.
 */
@Entity
@Table(name = "kpi_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_session_id", nullable = false)
    private ImportSession importSession;

    @Column(name = "kpi_name", nullable = false, length = 200)
    private String kpiName;

    /** Faible / Modéré / Élevé */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_justification", columnDefinition = "TEXT")
    private String riskJustification;

    @Column(name = "identification_risque", columnDefinition = "TEXT")
    private String identificationRisque;

    @Column(name = "objective_reached")
    private Boolean objectiveReached;

    @Column(name = "improvement_detected")
    private Boolean improvementDetected;

    /** Anomaly / Degradation / Instability flags */
    @Column(name = "issue_detected", columnDefinition = "TEXT")
    private String issueDetected;

    @Column(name = "probleme_detecte", columnDefinition = "TEXT")
    private String problemeDetecte;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "preventive_action", columnDefinition = "TEXT")
    private String preventiveAction;

    @Column(name = "actions_preventives", columnDefinition = "TEXT")
    private String actionsPreventives;

    @Column(name = "immediate_action", columnDefinition = "TEXT")
    private String immediateAction;

    @Column(name = "action_immediate", columnDefinition = "TEXT")
    private String actionImmediate;

    @Column(name = "immediate_priority", length = 20)
    private String immediatePriority;

    @Column(name = "priorite_action", length = 20)
    private String prioriteAction;

    @Column(name = "requires_8d", nullable = false)
    @Builder.Default
    private boolean requires8d = false;

    /** JSON string containing the 8 steps D1-D8 when requires_8d = true */
    @Column(name = "eight_d_details", columnDefinition = "TEXT")
    private String eightDDetails;

    @Column(name = "methode_8d", columnDefinition = "TEXT")
    private String methode8D;

    /** Clear business explanation – "Final Note IA" */
    @Column(name = "ai_note", columnDefinition = "TEXT")
    private String aiNote;

    @Column(name = "note_finale", columnDefinition = "TEXT")
    private String noteFinale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
