package com.QHSEAnalytics.shared.entity;

import com.QHSEAnalytics.shared.enums.Direction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "kpi",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kpi_nom_categorie", columnNames = {"nom", "categorie_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 1000)
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UniteKpi unite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private CategorieKpi categorieKpi;

    @Column(nullable = false)
    private Double seuilFaible;

    @Column(nullable = false)
    private Double seuilModere;

    @Column(nullable = false)
    private Double seuilCritique;

    @Column(nullable = false)
    private Integer ordre;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 50, nullable = true)
    private Direction direction;


    @Column(name = "target_value", nullable = true)
    private Double targetValue;

    @PrePersist
    public void onCreate() {
        validateSeuils();
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        validateSeuils();
        this.updatedAt = LocalDateTime.now();
    }

    private void validateSeuils() {
        if (seuilFaible == null || seuilModere == null || seuilCritique == null) {
            return;
        }

        if (!(seuilFaible < seuilModere && seuilModere < seuilCritique)) {
            throw new IllegalStateException("Règle invalide: seuilFaible < seuilModere < seuilCritique");
        }
    }
}
