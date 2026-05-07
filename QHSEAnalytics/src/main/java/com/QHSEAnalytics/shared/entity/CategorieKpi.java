package com.QHSEAnalytics.shared.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorie_kpi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "categorieKpi", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Kpi> kpis = new ArrayList<>();
}
