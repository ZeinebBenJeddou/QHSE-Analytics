package com.QHSEAnalytics.shared.entity;

import com.QHSEAnalytics.shared.enums.StatutNettoyage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "staging_donnees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StagingDonnee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_session_id", nullable = false)
    private ImportSession importSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private Kpi kpi;

    @Column(name = "valeur_brute_n1", length = 255)
    private String valeurBruteN1;

    @Column(name = "valeur_brute_n", length = 255)
    private String valeurBruteN;

    @Column(name = "valeur_n1")
    private Double valeurN1;

    @Column(name = "valeur_n")
    private Double valeurN;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_nettoyage", nullable = false, length = 20)
    private StatutNettoyage statutNettoyage;

    @Column(name = "note_nettoyage", length = 500)
    private String noteNettoyage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
