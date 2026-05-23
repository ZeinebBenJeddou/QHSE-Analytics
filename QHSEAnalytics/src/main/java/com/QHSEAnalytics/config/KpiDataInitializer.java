package com.QHSEAnalytics.config;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class KpiDataInitializer implements ApplicationRunner {

    private final CategorieKpiRepository categorieKpiRepository;
    private final KpiRepository kpiRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initialisation des categories KPI et des donnees de base");

        CategorieKpi q = ensureCategory("Q", "Qualité", "Maîtrise opérationnelle et satisfaction");
        CategorieKpi h = ensureCategory("H", "Hygiène & Santé", "Préservation de la santé des collaborateurs");
        CategorieKpi s = ensureCategory("S", "Sécurité", "Prévention des risques et accidents");
        CategorieKpi e = ensureCategory("E", "Environnement", "Gestion des impacts et durabilité");

        // Qualité
        ensureKpi(q, "Taux de Non-Conformité",        "Ratio produits non conformes / total produit",    UniteKpi.POURCENTAGE,  2.0,    5.0,    10.0,   1, Direction.LOWER_IS_BETTER);
        ensureKpi(q, "Coût de la Non-Qualité",         "Coûts des rebuts et retouches en k€",             UniteKpi.NOMBRE,     500.0, 2000.0, 5000.0,  2, Direction.LOWER_IS_BETTER);
        ensureKpi(q, "Taux de Satisfaction Client",    "Indice de satisfaction global",                   UniteKpi.POURCENTAGE,  70.0,   80.0,   90.0,  3, Direction.HIGHER_IS_BETTER);
        ensureKpi(q, "Délai Moyen de Livraison",       "Respect des délais promis (jours)",               UniteKpi.NOMBRE,       2.0,    5.0,   10.0,   4, Direction.LOWER_IS_BETTER);
        ensureKpi(q, "Taux de Rebuts",                 "Pourcentage de perte matière brute",              UniteKpi.POURCENTAGE,  1.0,    3.0,    5.0,   5, Direction.LOWER_IS_BETTER);
        ensureKpi(q, "First Pass Yield",               "Produits conformes dès le premier essai",         UniteKpi.POURCENTAGE, 90.0,   95.0,   98.0,   6, Direction.HIGHER_IS_BETTER);
        ensureKpi(q, "Nombre de Réclamations Clients", "Total des plaintes enregistrées",                 UniteKpi.NOMBRE,       2.0,   10.0,   25.0,   7, Direction.LOWER_IS_BETTER);
        ensureKpi(q, "Taux de Réussite des Audits",    "Score moyen des audits qualité internes",         UniteKpi.POURCENTAGE, 75.0,   85.0,   95.0,   8, Direction.HIGHER_IS_BETTER);

        // Hygiène & Santé
        ensureKpi(h, "Taux d'Absentéisme",             "Heures d'absence / Heures théoriques",            UniteKpi.POURCENTAGE,  3.0,    6.0,   10.0,   1, Direction.LOWER_IS_BETTER);
        ensureKpi(h, "Taux de Maladies Professionnelles", "Cas déclarés pour 1000 salariés",              UniteKpi.NOMBRE,       0.0,    1.0,    2.0,   2, Direction.LOWER_IS_BETTER);
        ensureKpi(h, "Conformité Ergonomique",         "Postes de travail adaptés aux normes",            UniteKpi.POURCENTAGE, 80.0,   90.0,  100.0,   3, Direction.HIGHER_IS_BETTER);
        ensureKpi(h, "Taux de Visites Médicales",      "Salariés à jour de leur suivi médical",           UniteKpi.POURCENTAGE, 90.0,   95.0,  100.0,   4, Direction.HIGHER_IS_BETTER);
        ensureKpi(h, "Qualité de l'Air (CO2)",         "Niveau moyen de CO2 en ppm",                      UniteKpi.NOMBRE,     600.0, 1000.0, 1500.0,   5, Direction.LOWER_IS_BETTER);
        ensureKpi(h, "Taux de Renouvellement d'Air",   "Volume d'air renouvelé par heure",                UniteKpi.NOMBRE,      20.0,   25.0,   30.0,   6, Direction.HIGHER_IS_BETTER);
        ensureKpi(h, "Indice d'Exposition au Bruit",   "Moyenne des niveaux sonores en dB(A)",            UniteKpi.NOMBRE,      80.0,   85.0,   90.0,   7, Direction.LOWER_IS_BETTER);
        ensureKpi(h, "Usage des Équipements de Repos", "Fréquence d'utilisation des zones de pause",      UniteKpi.POURCENTAGE, 40.0,   60.0,   80.0,   8, Direction.HIGHER_IS_BETTER);

        // Sécurité
        ensureKpi(s, "Taux de Fréquence (TF1)",        "Accidents avec arrêt / million d'heures",         UniteKpi.NOMBRE,       5.0,   15.0,   30.0,   1, Direction.LOWER_IS_BETTER);
        ensureKpi(s, "Taux de Gravité (TG)",            "Jours perdus / millier d'heures",                 UniteKpi.NOMBRE,       0.5,    1.0,    2.0,   2, Direction.LOWER_IS_BETTER);
        ensureKpi(s, "Nombre de Presque-accidents",     "Near-miss signalés (vigilance)",                  UniteKpi.NOMBRE,       5.0,   10.0,   20.0,   3, Direction.HIGHER_IS_BETTER);
        ensureKpi(s, "Heures de Formation Sécurité",    "Total heures formation par employé",              UniteKpi.NOMBRE,       5.0,   10.0,   20.0,   4, Direction.HIGHER_IS_BETTER);
        ensureKpi(s, "Taux de Port des EPI",            "Conformité observée lors des rondes",             UniteKpi.POURCENTAGE, 90.0,   95.0,  100.0,   5, Direction.HIGHER_IS_BETTER);
        ensureKpi(s, "Nombre de Situations Dangereuses","Situations à risque signalées",                   UniteKpi.NOMBRE,      10.0,   25.0,   50.0,   6, Direction.LOWER_IS_BETTER);
        ensureKpi(s, "Délai Levée des Non-Conformités", "Temps pour corriger une faille sécurité (jours)",UniteKpi.NOMBRE,       2.0,    7.0,   15.0,   7, Direction.LOWER_IS_BETTER);
        ensureKpi(s, "Nombre de Visites Sécurité (VMS)","Total des visites managériales terrain",          UniteKpi.NOMBRE,       4.0,    8.0,   12.0,   8, Direction.HIGHER_IS_BETTER);

        // Environnement
        ensureKpi(e, "Consommation Électricité",        "kWh consommés par tonne produite",               UniteKpi.KWH,        100.0,  200.0,  500.0,   1, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Consommation Eau",                "Mètres cubes d'eau consommés",                   UniteKpi.NOMBRE,      50.0,  150.0,  300.0,   2, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Taux de Valorisation Déchets",    "Déchets recyclés / Déchets totaux",              UniteKpi.POURCENTAGE, 50.0,   70.0,   85.0,   3, Direction.HIGHER_IS_BETTER);
        ensureKpi(e, "Émissions CO2 (Scope 1&2)",       "Tonnes de CO2 équivalent",                       UniteKpi.KG,        1000.0, 5000.0,10000.0,   4, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Volume Déchets Dangereux",        "Total déchets toxiques ou polluants",             UniteKpi.KG,          50.0,  200.0,  500.0,   5, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Consommation de Papier",          "Nombre de rames par collaborateur",               UniteKpi.NOMBRE,       1.0,    3.0,    5.0,   6, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Incidents Environnementaux",      "Déversements ou fuites accidentelles",            UniteKpi.NOMBRE,       0.0,    1.0,    2.0,   7, Direction.LOWER_IS_BETTER);
        ensureKpi(e, "Part Énergie Renouvelable",       "Pourcentage d'énergie propre utilisée",           UniteKpi.POURCENTAGE, 10.0,   30.0,   50.0,   8, Direction.HIGHER_IS_BETTER);

        log.info("Referentiel complet initialise.");
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
                log.info("Categorie KPI creee code={}", code);
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
        Integer ordre,
        Direction direction
    ) {
        try {
            if (!kpiRepository.existsByNomAndCategorieKpi(nom, categorie)) {
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
                    .direction(direction)
                    .build();
                kpiRepository.save(kpi);
                log.info("KPI seed cree categorie={} nom={} direction={}", categorie.getCode(), nom, direction);
            } else {
                kpiRepository.findByNom(nom)
                    .ifPresent(existing -> {
                        if (existing.getDirection() == null && direction != null) {
                            existing.setDirection(direction);
                            kpiRepository.save(existing);
                            log.info("Direction mise a jour KPI={} direction={}", nom, direction);
                        }
                    });
            }
        } catch (Exception ex) {
            log.error("Echec creation KPI seed categorie={} nom={} cause={}", categorie.getCode(), nom, ex.getMessage());
        }
    }
}
