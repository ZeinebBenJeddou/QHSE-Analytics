package com.QHSEAnalytics.shared.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a snapshot of every KPI row from the "Aperçu des données" table
 * so that the raw import data is preserved independently of the processed
 * ResultatKpi records.
 */
@Entity
@Table(name = "kpi_import_preview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiImportPreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_session_id", nullable = false)
    private ImportSession importSession;

    @Column(name = "kpi_name", nullable = false, length = 200)
    private String kpiName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Column(name = "value_n")
    private Double valueN;

    @Column(name = "value_n1")
    private Double valueN1;

    /** Human-readable status label e.g. "Bon", "À surveiller", "Critique" */
    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "variation_percent")
    private Double variationPercent;

    @Column(name = "ecart")
    private Double ecart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
