package com.QHSEAnalytics.initializer;

import com.QHSEAnalytics.entity.CategorieKpi;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.repository.CategorieKpiRepository;
import com.QHSEAnalytics.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KpiDataInitializer implements ApplicationRunner {

    private final CategorieKpiRepository categorieKpiRepository;
    private final KpiRepository kpiRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initialisation des catégories KPI et des données de base");

        CategorieKpi q = ensureCategory("Q", "Qualité", "Indicateurs qualité");
        CategorieKpi h = ensureCategory("H", "Hygiène", "Indicateurs hygiène");
        CategorieKpi s = ensureCategory("S", "Sécurité", "Indicateurs sécurité");
        CategorieKpi e = ensureCategory("E", "Environnement", "Indicateurs environnement");

        // Qualité (Q)
        ensureKpi(q, "Taux de conformité aux procédures", "Pourcentage des processus conformes aux procédures établies", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 1);
        ensureKpi(q, "Taux de défauts produits", "Pourcentage de produits non conformes", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 2);
        ensureKpi(q, "Taux de réclamations clients", "Pourcentage de réclamations par rapport au total des commandes", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 3);
        ensureKpi(q, "Taux de satisfaction client", "Pourcentage de clients satisfaits", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 4);

        // Hygiène (H)
        ensureKpi(h, "Taux d'absentéisme pour maladie", "Pourcentage de jours d'absence pour maladie par rapport au total des jours travaillés", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 1);
        ensureKpi(h, "Nombre d'accidents bénins", "Total incidents avec arrêt < 1 jour", UniteKpi.NOMBRE, 1.0, 3.0, 5.0, 2);
        ensureKpi(h, "Nombre d'accidents graves", "Total incidents avec arrêt > 1 jour", UniteKpi.NOMBRE, 1.0, 2.0, 3.0, 3);
        ensureKpi(h, "Taux de visites médicales réalisées", "Pourcentage de salariés ayant passé la visite annuelle", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 4);

        // Sécurité (S)
        ensureKpi(s, "Taux de conformité sécurité", "Pourcentage d'audits de sécurité conformes", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 1);
        ensureKpi(s, "Nombre d'incidents de sécurité", "Total incidents de sécurité", UniteKpi.NOMBRE, 1.0, 3.0, 5.0, 2);
        ensureKpi(s, "Nombre de formations sécurité réalisées", "Pourcentage de salariés formés", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 3);
        ensureKpi(s, "Nombre de non-conformités critiques", "Total non-conformités de sécurité critiques", UniteKpi.NOMBRE, 1.0, 2.0, 3.0, 4);

        // Environnement (E)
        ensureKpi(e, "Consommation énergétique", "kWh consommés par période", UniteKpi.KWH, 5.0, 10.0, 20.0, 1);
        ensureKpi(e, "Taux de recyclage déchets", "Pourcentage de déchets recyclés", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 2);
        ensureKpi(e, "Emissions CO2", "kg CO2 par période", UniteKpi.KG, 5.0, 10.0, 20.0, 3);
        ensureKpi(e, "Taux de conformité réglementaire", "Pourcentage du respect des lois et normes environnementales", UniteKpi.POURCENTAGE, 5.0, 10.0, 20.0, 4);

        log.info("Initialisation KPI terminée");
    }

    private CategorieKpi ensureCategory(String code, String libelle, String description) {
        return categorieKpiRepository.findByCode(code)
                .orElseGet(() -> {
                    CategorieKpi categorie = CategorieKpi.builder()
                            .code(code)
                            .libelle(libelle)
                            .description(description)
                            .build();
                    CategorieKpi saved = categorieKpiRepository.save(categorie);
                    log.info("Catégorie KPI créée code={}", code);
                    return saved;
                });
    }

    private void ensureKpi(
            CategorieKpi categorie,
            String nom,
            String definition,
            UniteKpi unite,
            Double seuilFaible,
            Double seuilModere,
            Double seuilCritique,
            Integer ordre
    ) {
        try {
            if (kpiRepository.existsByNomAndCategorieKpi(nom, categorie)) {
                return;
            }

            Kpi kpi = Kpi.builder()
                    .nom(nom)
                    .definition(definition)
                    .unite(unite)
                    .categorieKpi(categorie)
                    .seuilFaible(seuilFaible)
                    .seuilModere(seuilModere)
                    .seuilCritique(seuilCritique)
                    .ordre(ordre)
                    .isActive(true)
                    .build();

            kpiRepository.save(kpi);
            log.info("KPI seed créé catégorie={} nom={}", categorie.getCode(), nom);
        } catch (Exception ex) {
            log.error("Échec création KPI seed catégorie={} nom={} cause={}", categorie.getCode(), nom, ex.getMessage());
        }
    }
}
