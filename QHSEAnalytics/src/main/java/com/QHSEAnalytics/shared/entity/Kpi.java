package com.QHSEAnalytics.shared.entity;

import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.exception.InvalidSeuilException;

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

    public static final String SOURCE_REFERENTIEL = "REFERENTIEL";
    public static final String SOURCE_ADMIN = "ADMIN";
    public static final String SOURCE_IMPORT = "IMPORT";

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

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "source", length = 120)
    @Builder.Default
    private String source = SOURCE_REFERENTIEL;

    @Column(name = "ai_enriched", nullable = false)
    @Builder.Default
    private boolean aiEnriched = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 50, nullable = true)
    private Direction direction;

    @PrePersist
    public void onCreate() {
        validateSeuils();
        source = normalizeSource(source);
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
            throw new InvalidSeuilException("Règle invalide : seuilFaible < seuilModere < seuilCritique");
        }
    }

    private String normalizeSource(String value) {
        if (value == null || value.isBlank()) {
            return SOURCE_REFERENTIEL;
        }

        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("Référentiel") || trimmed.equalsIgnoreCase("Referentiel")) {
            return SOURCE_REFERENTIEL;
        }
        if (trimmed.equalsIgnoreCase("LLM")) {
            return SOURCE_IMPORT;
        }
        if (trimmed.equalsIgnoreCase("Admin") || trimmed.toUpperCase().startsWith("ADMIN")) {
            return SOURCE_ADMIN;
        }
        if (trimmed.equalsIgnoreCase(SOURCE_REFERENTIEL)
                || trimmed.equalsIgnoreCase(SOURCE_ADMIN)
                || trimmed.equalsIgnoreCase(SOURCE_IMPORT)) {
            return trimmed.toUpperCase();
        }
        return trimmed;
    }
}
