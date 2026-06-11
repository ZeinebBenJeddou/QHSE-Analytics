package com.QHSEAnalytics.config;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialise les catégories QHSE et les KPIs standard au démarrage.
 * Idempotent : ne recrée pas ce qui existe déjà.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class KpiDataInitializer implements CommandLineRunner {

    private final CategorieKpiRepository categorieRepo;
    private final KpiRepository kpiRepo;

    @Override
    public void run(String... args) {
        initCategories();
        initKpis();
    }

    // ─────────────────────────────────────────────────────────────
    // Catégories
    // ─────────────────────────────────────────────────────────────

    private void initCategories() {
        seedCategorie("Q", "Qualité",         "Maîtrise opérationnelle et satisfaction");
        seedCategorie("H", "Hygiène & Santé", "Préservation de la santé des collaborateurs");
        seedCategorie("S", "Sécurité",        "Prévention des risques et accidents");
        seedCategorie("E", "Environnement",   "Gestion des impacts et durabilité");
        log.info("Catégories QHSE initialisées");
    }

    private void seedCategorie(String code, String libelle, String description) {
        if (!categorieRepo.existsByCode(code)) {
            categorieRepo.save(CategorieKpi.builder()
                    .code(code).libelle(libelle).description(description).build());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // KPIs
    // ─────────────────────────────────────────────────────────────

    private void initKpis() {
        CategorieKpi q = categorieRepo.findByCode("Q").orElseThrow();
        CategorieKpi h = categorieRepo.findByCode("H").orElseThrow();
        CategorieKpi s = categorieRepo.findByCode("S").orElseThrow();
        CategorieKpi e = categorieRepo.findByCode("E").orElseThrow();

        List<Kpi> kpis = List.of(

            // ── Qualité ───────────────────────────────────────────────
            kpi("Taux de Non-Conformité",         "Ratio produits non conformes / total produit",   UniteKpi.POURCENTAGE, q,   2.0,    5.0,   10.0, Direction.LOWER_IS_BETTER),
            kpi("Coût de la Non-Qualité",         "Coûts des rebuts et retouches en k€",            UniteKpi.NOMBRE,      q, 500.0, 2000.0, 5000.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Satisfaction Client",    "Indice de satisfaction global",                  UniteKpi.POURCENTAGE, q,  70.0,   80.0,   90.0, Direction.HIGHER_IS_BETTER),
            kpi("Délai Moyen de Livraison",       "Respect des délais promis (jours)",              UniteKpi.NOMBRE,      q,   2.0,    5.0,   10.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Rebuts",                 "Pourcentage de perte matière brute",             UniteKpi.POURCENTAGE, q,   1.0,    3.0,    5.0, Direction.LOWER_IS_BETTER),
            kpi("First Pass Yield",               "Produits conformes dès le premier essai",        UniteKpi.POURCENTAGE, q,  90.0,   95.0,   98.0, Direction.HIGHER_IS_BETTER),
            kpi("Nombre de Réclamations Clients", "Total des plaintes enregistrées",                UniteKpi.NOMBRE,      q,   2.0,   10.0,   25.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Réussite des Audits",    "Score moyen des audits qualité internes",        UniteKpi.POURCENTAGE, q,  75.0,   85.0,   95.0, Direction.HIGHER_IS_BETTER),

            // ── Hygiène & Santé ───────────────────────────────────────
            kpi("Taux d'Absentéisme",               "Heures d'absence / Heures théoriques",        UniteKpi.POURCENTAGE, h,   3.0,    6.0,   10.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Maladies Professionnelles","Cas déclarés pour 1000 salariés",              UniteKpi.NOMBRE,      h,   0.0,    1.0,    2.0, Direction.LOWER_IS_BETTER),
            kpi("Conformité Ergonomique",           "Postes de travail adaptés aux normes",         UniteKpi.POURCENTAGE, h,  80.0,   90.0,  100.0, Direction.HIGHER_IS_BETTER),
            kpi("Taux de Visites Médicales",        "Salariés à jour de leur suivi médical",        UniteKpi.POURCENTAGE, h,  90.0,   95.0,  100.0, Direction.HIGHER_IS_BETTER),
            kpi("Qualité de l'Air (CO2)",           "Niveau moyen de CO2 en ppm",                   UniteKpi.NOMBRE,      h, 600.0, 1000.0, 1500.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Renouvellement d'Air",     "Volume d'air renouvelé par heure",             UniteKpi.NOMBRE,      h,  20.0,   25.0,   30.0, Direction.HIGHER_IS_BETTER),
            kpi("Indice d'Exposition au Bruit",     "Moyenne des niveaux sonores en dB(A)",         UniteKpi.NOMBRE,      h,  80.0,   85.0,   90.0, Direction.LOWER_IS_BETTER),
            kpi("Usage des Équipements de Repos",   "Fréquence d'utilisation des zones de pause",   UniteKpi.POURCENTAGE, h,  40.0,   60.0,   80.0, Direction.HIGHER_IS_BETTER),

            // ── Sécurité ─────────────────────────────────────────────
            kpi("Taux de Fréquence (TF1)",          "Accidents avec arrêt / million d'heures",      UniteKpi.NOMBRE,      s,   5.0,   15.0,   30.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Gravité (TG)",             "Jours perdus / millier d'heures",              UniteKpi.NOMBRE,      s,   0.5,    1.0,    2.0, Direction.LOWER_IS_BETTER),
            kpi("Nombre de Presque-accidents",      "Near-miss signalés (vigilance)",               UniteKpi.NOMBRE,      s,   5.0,   10.0,   20.0, Direction.HIGHER_IS_BETTER),
            kpi("Heures de Formation Sécurité",     "Total heures formation par employé",           UniteKpi.NOMBRE,      s,   5.0,   10.0,   20.0, Direction.HIGHER_IS_BETTER),
            kpi("Taux de Port des EPI",             "Conformité observée lors des rondes",          UniteKpi.POURCENTAGE, s,  90.0,   95.0,  100.0, Direction.HIGHER_IS_BETTER),
            kpi("Nombre de Situations Dangereuses", "Situations à risque signalées",                UniteKpi.NOMBRE,      s,  10.0,   25.0,   50.0, Direction.LOWER_IS_BETTER),
            kpi("Délai de Levée des Non-Conformités",  "Temps pour corriger une faille sécurité (j)",  UniteKpi.NOMBRE,      s,   2.0,    7.0,   15.0, Direction.LOWER_IS_BETTER),
            kpi("Nombre de Visites Sécurité (VMS)", "Total des visites managériales terrain",       UniteKpi.NOMBRE,      s,   4.0,    8.0,   12.0, Direction.HIGHER_IS_BETTER),

            // ── Environnement ────────────────────────────────────────
            kpi("Consommation Électricité",         "kWh consommés par tonne produite",             UniteKpi.KWH,         e, 100.0,  200.0,  500.0, Direction.LOWER_IS_BETTER),
            kpi("Consommation Eau",                 "Mètres cubes d'eau consommés",                 UniteKpi.NOMBRE,      e,  50.0,  150.0,  300.0, Direction.LOWER_IS_BETTER),
            kpi("Taux de Valorisation Déchets",     "Déchets recyclés / Déchets totaux",            UniteKpi.POURCENTAGE, e,  50.0,   70.0,   85.0, Direction.HIGHER_IS_BETTER),
            kpi("Émissions CO2",                     "Tonnes de CO2 équivalent",                     UniteKpi.KG,          e,1000.0, 5000.0,10000.0, Direction.LOWER_IS_BETTER),
            kpi("Volume Déchets Dangereux",         "Total déchets toxiques ou polluants",          UniteKpi.KG,          e,  50.0,  200.0,  500.0, Direction.LOWER_IS_BETTER),
            kpi("Consommation de Papier",           "Nombre de rames par collaborateur",            UniteKpi.NOMBRE,      e,   1.0,    3.0,    5.0, Direction.LOWER_IS_BETTER),
            kpi("Incidents Environnementaux",       "Déversements ou fuites accidentelles",         UniteKpi.NOMBRE,      e,   0.0,    1.0,    2.0, Direction.LOWER_IS_BETTER),
            kpi("Part d'Énergie Renouvelable",      "Pourcentage d'énergie propre utilisée",        UniteKpi.POURCENTAGE, e,  10.0,   30.0,   50.0, Direction.HIGHER_IS_BETTER)
        );

        int created = 0;
        for (Kpi k : kpis) {
            if (!kpiRepo.existsByNomAndCategorieKpi(k.getNom(), k.getCategorieKpi())) {
                kpiRepo.save(k);
                created++;
            }
        }
        log.info("KPIs initialisés : {} créés", created);
    }

    private Kpi kpi(String nom, String definition, UniteKpi unite, CategorieKpi cat,
                    double sf, double sm, double sc, Direction direction) {
        return Kpi.builder()
                .nom(nom)
                .definition(definition)
                .unite(unite)
                .categorieKpi(cat)
                .seuilFaible(sf)
                .seuilModere(sm)
                .seuilCritique(sc)
                .direction(direction)
                .isActive(true)
                .build();
    }
}
