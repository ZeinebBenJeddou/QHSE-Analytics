package com.QHSEAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kpi_raw_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiRawData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_session_id", nullable = false)
    private ImportSession importSession;

    @Column(name = "kpi_nom", nullable = false, length = 1000)
    private String kpiNom;

    @Column(name = "valeur_n1", length = 255)
    private String valeurN1;

    @Column(name = "valeur_n", length = 255)
    private String valeurN;

    @Column(name = "methode_extraction", nullable = false, length = 20)
    private String methodeExtraction;

    @Column(name = "score_confiance", nullable = false)
    private Double scoreConfiance;

    @Column(name = "ligne_fichier", nullable = false)
    private Integer ligneFichier;
}
