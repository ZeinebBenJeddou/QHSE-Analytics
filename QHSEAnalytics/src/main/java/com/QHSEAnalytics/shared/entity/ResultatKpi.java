package com.QHSEAnalytics.shared.entity;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.shared.enums.NiveauVariation;
import com.QHSEAnalytics.shared.enums.QualityStatus;
import com.QHSEAnalytics.shared.enums.Tendance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resultat_kpis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_session_id", nullable = false)
    private ImportSession importSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private Kpi kpi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "periode_n1", nullable = false)
    private Integer periodeN1;

    @Column(name = "periode_n", nullable = false)
    private Integer periodeN;

    @Column(name = "valeur_n1", nullable = false)
    private Double valeurN1;

    @Column(name = "valeur_n", nullable = false)
    private Double valeurN;

    @Column(name = "variation_absolue", nullable = false)
    private Double variationAbsolue;

    @Column(name = "variation_relative", nullable = false)
    private Double variationRelative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauVariation niveauVariation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tendance tendance;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QualityStatus qualityStatus;

    @Column(name = "analyse_ia", columnDefinition = "TEXT")
    private String analyseIa;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
