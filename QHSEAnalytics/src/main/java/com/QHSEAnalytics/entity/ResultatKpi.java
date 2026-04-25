package com.QHSEAnalytics.entity;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.QualityStatus;
import com.QHSEAnalytics.enums.Tendance;
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

    @Column(nullable = false)
    private Integer periodeN1;

    @Column(nullable = false)
    private Integer periodeN;

    @Column(nullable = false)
    private Double valeurN1;

    @Column(nullable = false)
    private Double valeurN;

    @Column(nullable = false)
    private Double variationAbsolue;

    @Column(nullable = false)
    private Double variationRelative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauVariation niveauVariation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tendance tendance;

    @Column(nullable = false)
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QualityStatus qualityStatus;

    @Column(columnDefinition = "TEXT")
    private String analyseIa;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
