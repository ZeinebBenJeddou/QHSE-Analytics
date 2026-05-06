package com.QHSEAnalytics.initializer;

import com.QHSEAnalytics.entity.CategorieKpi;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.RagKnowledge;
import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.repository.CategorieKpiRepository;
import com.QHSEAnalytics.repository.KpiRepository;
import com.QHSEAnalytics.repository.RagKnowledgeRepository;
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
    private final RagKnowledgeRepository ragKnowledgeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initialisation des catégories KPI et des données de base");

        CategorieKpi q = ensureCategory("Q", "Qualité", "Maîtrise opérationnelle et satisfaction");
        CategorieKpi h = ensureCategory("H", "Hygiène & Santé", "Préservation de la santé des collaborateurs");
        CategorieKpi s = ensureCategory("S", "Sécurité", "Prévention des risques et accidents");
        CategorieKpi e = ensureCategory("E", "Environnement", "Gestion des impacts et durabilité");

        // --- QUALITÉ (Q) ---
        ensureKpi(q, "Taux de Non-Conformité", "Ratio produits non conformes / total produit", UniteKpi.POURCENTAGE, 2.0, 5.0, 10.0, 1);
        ensureKpi(q, "Coût de la Non-Qualité", "Coûts des rebuts et retouches en k€", UniteKpi.NOMBRE, 500.0, 2000.0, 5000.0, 2);
        ensureKpi(q, "Taux de Satisfaction Client", "Indice de satisfaction global", UniteKpi.POURCENTAGE, 70.0, 80.0, 90.0, 3);
        ensureKpi(q, "Délai Moyen de Livraison", "Respect des délais promis (jours)", UniteKpi.NOMBRE, 2.0, 5.0, 10.0, 4);
        ensureKpi(q, "Taux de Rebuts", "Pourcentage de perte matière brute", UniteKpi.POURCENTAGE, 1.0, 3.0, 5.0, 5);
        ensureKpi(q, "First Pass Yield", "Produits conformes dès le premier essai", UniteKpi.POURCENTAGE, 90.0, 95.0, 98.0, 6);
        ensureKpi(q, "Nombre de Réclamations Clients", "Total des plaintes enregistrées", UniteKpi.NOMBRE, 2.0, 10.0, 25.0, 7);
        ensureKpi(q, "Taux de Réussite des Audits", "Score moyen des audits qualité internes", UniteKpi.POURCENTAGE, 75.0, 85.0, 95.0, 8);

        // --- HYGIÈNE (H) ---
        ensureKpi(h, "Taux d'Absentéisme", "Heures d'absence / Heures théoriques", UniteKpi.POURCENTAGE, 3.0, 6.0, 10.0, 1);
        ensureKpi(h, "Taux de Maladies Professionnelles", "Cas déclarés pour 1000 salariés", UniteKpi.NOMBRE, 0.0, 1.0, 2.0, 2);
        ensureKpi(h, "Conformité Ergonomique", "Postes de travail adaptés aux normes", UniteKpi.POURCENTAGE, 80.0, 90.0, 100.0, 3);
        ensureKpi(h, "Taux de Visites Médicales", "Salariés à jour de leur suivi médical", UniteKpi.POURCENTAGE, 90.0, 95.0, 100.0, 4);
        ensureKpi(h, "Qualité de l'Air (CO2)", "Niveau moyen de CO2 en ppm", UniteKpi.NOMBRE, 600.0, 1000.0, 1500.0, 5);
        ensureKpi(h, "Taux de Renouvellement d'Air", "Volume d'air renouvelé par heure", UniteKpi.NOMBRE, 20.0, 25.0, 30.0, 6);
        ensureKpi(h, "Indice d'Exposition au Bruit", "Moyenne des niveaux sonores en dB(A)", UniteKpi.NOMBRE, 80.0, 85.0, 90.0, 7);
        ensureKpi(h, "Usage des Équipements de Repos", "Fréquence d'utilisation des zones de pause", UniteKpi.POURCENTAGE, 40.0, 60.0, 80.0, 8);

        // --- SÉCURITÉ (S) ---
        ensureKpi(s, "Taux de Fréquence (TF1)", "Accidents avec arrêt / million d'heures", UniteKpi.NOMBRE, 5.0, 15.0, 30.0, 1);
        ensureKpi(s, "Taux de Gravité (TG)", "Jours perdus / millier d'heures", UniteKpi.NOMBRE, 0.5, 1.0, 2.0, 2);
        ensureKpi(s, "Nombre de Presque-accidents", "Near-miss signalés (vigilance)", UniteKpi.NOMBRE, 5.0, 10.0, 20.0, 3);
        ensureKpi(s, "Heures de Formation Sécurité", "Total heures formation par employé", UniteKpi.NOMBRE, 5.0, 10.0, 20.0, 4);
        ensureKpi(s, "Taux de Port des EPI", "Conformité observée lors des rondes", UniteKpi.POURCENTAGE, 90.0, 95.0, 100.0, 5);
        ensureKpi(s, "Nombre de Situations Dangereuses", "Situations à risque signalées", UniteKpi.NOMBRE, 10.0, 25.0, 50.0, 6);
        ensureKpi(s, "Délai Levée des Non-Conformités", "Temps pour corriger une faille sécurité (jours)", UniteKpi.NOMBRE, 2.0, 7.0, 15.0, 7);
        ensureKpi(s, "Nombre de Visites Sécurité (VMS)", "Total des visites managériales terrain", UniteKpi.NOMBRE, 4.0, 8.0, 12.0, 8);

        // --- ENVIRONNEMENT (E) ---
        ensureKpi(e, "Consommation Électricité", "kWh consommés par tonne produite", UniteKpi.KWH, 100.0, 200.0, 500.0, 1);
        ensureKpi(e, "Consommation Eau", "Mètres cubes d'eau consommés", UniteKpi.NOMBRE, 50.0, 150.0, 300.0, 2);
        ensureKpi(e, "Taux de Valorisation Déchets", "Déchets recyclés / Déchets totaux", UniteKpi.POURCENTAGE, 50.0, 70.0, 85.0, 3);
        ensureKpi(e, "Émissions CO2 (Scope 1&2)", "Tonnes de CO2 équivalent", UniteKpi.KG, 1000.0, 5000.0, 10000.0, 4);
        ensureKpi(e, "Volume Déchets Dangereux", "Total déchets toxiques ou polluants", UniteKpi.KG, 50.0, 200.0, 500.0, 5);
        ensureKpi(e, "Consommation de Papier", "Nombre de rames par collaborateur", UniteKpi.NOMBRE, 1.0, 3.0, 5.0, 6);
        ensureKpi(e, "Incidents Environnementaux", "Déversements ou fuites accidentelles", UniteKpi.NOMBRE, 0.0, 1.0, 2.0, 7);
        ensureKpi(e, "Part Énergie Renouvelable", "Pourcentage d'énergie propre utilisée", UniteKpi.POURCENTAGE, 10.0, 30.0, 50.0, 8);

        log.info(" Référentiel complet initialisé.");
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

            Kpi savedKpi = kpiRepository.save(kpi);
            log.info("KPI seed créé catégorie={} nom={}", categorie.getCode(), nom);
            
            // Enrich RAG knowledge base
            createRagKnowledgeForKpi(savedKpi, categorie);
        } catch (Exception ex) {
            log.error("Échec création KPI seed catégorie={} nom={} cause={}", categorie.getCode(), nom, ex.getMessage());
        }
    }

    private void createRagKnowledgeForKpi(Kpi kpi, CategorieKpi categorie) {
        try {
            if (ragKnowledgeRepository.findByKpiName(kpi.getNom()).isPresent()) {
                return;
            }

            RagKnowledge ragKnowledge = RagKnowledge.builder()
                    .kpiName(kpi.getNom())
                    .definition(kpi.getDefinition())
                    .category(categorie.getCode())
                    .thresholds(String.format(java.util.Locale.US, "{\"faible\":%f, \"modere\":%f, \"critique\":%f}",
                            kpi.getSeuilFaible(), kpi.getSeuilModere(), kpi.getSeuilCritique()))
                    .build();

            ragKnowledgeRepository.save(ragKnowledge);
            log.info("RAG knowledge seed créé pour KPI: {}", kpi.getNom());
        } catch (Exception ex) {
            log.error("Échec création RAG knowledge seed KPI {} cause={}", kpi.getNom(), ex.getMessage());
        }
    }
}
