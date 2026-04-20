package com.QHSEAnalytics.entity;

import com.QHSEAnalytics.enums.TypeValeur;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_mapping_colonnes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMappingColonne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_template_id", nullable = false)
    private UserMappingTemplate mappingTemplate;

    @Column(nullable = false, length = 255)
    private String nomColonne;

    @Column(nullable = false)
    private Integer indexColonne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_id")
    private Kpi kpi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeValeur typeValeur;
}
