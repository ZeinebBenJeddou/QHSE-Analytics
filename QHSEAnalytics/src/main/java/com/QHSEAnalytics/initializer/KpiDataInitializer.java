package com.QHSEAnalytics.initializer;

import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.entity.UniteKpi;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class KpiDataInitializer implements ApplicationRunner {

    private final CategorieKpiRepository categorieKpiRepository;
    private final KpiRepository kpiRepository;
    private final RagKnowledgeRepository ragKnowledgeRepository;

    // Enriched RAG definitions: formula, direction, benchmark, root causes, actions, ISO norms.
    private static final Map<String, String> ENRICHED_DEFINITIONS = Map.ofEntries(
        Map.entry("Taux de Non-Conformité",
            "Formule: (Nb non-conformités / Nb total produits) × 100. Direction: lower_better. " +
            "Benchmark: <2% industrie manufacturière. Causes: défaut matière première, erreur opérateur, " +
            "machine mal calibrée. Actions: audit process, formation opérateurs, révision contrôles qualité. " +
            "Norme: ISO 9001:2015 §8.7."),
        Map.entry("Coût de la Non-Qualité",
            "Formule: Coûts rebuts + retouches + garanties + réclamations (k€). Direction: lower_better. " +
            "Benchmark: <3% du CA. Causes: non-conformités répétées, manque de prévention. " +
            "Actions: lean manufacturing, analyse causes racines (5 pourquoi, Ishikawa). " +
            "Norme: ISO 9001:2015 §10.2."),
        Map.entry("Taux de Satisfaction Client",
            "Formule: (Nb clients satisfaits / Nb clients interrogés) × 100. Direction: higher_better. " +
            "Benchmark: >85%. Causes de dégradation: délais non tenus, qualité produit, SAV insuffisant. " +
            "Actions: enquêtes NPS régulières, amélioration SAV, résolution rapide des réclamations. " +
            "Norme: ISO 9001:2015 §9.1.2."),
        Map.entry("Délai Moyen de Livraison",
            "Formule: Σ (date livraison réelle - date promise) / Nb commandes (jours). Direction: lower_better. " +
            "Benchmark: <5j e-commerce, <15j B2B. Causes: ruptures stock, aléas production, transport. " +
            "Actions: optimiser niveaux de stock, planification de production, suivi transport temps réel. " +
            "Norme: ISO 9001:2015 §8.5."),
        Map.entry("Taux de Rebuts",
            "Formule: (Nb pièces rebut / Nb pièces fabriquées) × 100. Direction: lower_better. " +
            "Benchmark: <1%. Causes: réglage machines défaillant, matière première défectueuse. " +
            "Actions: SPC (Statistical Process Control), AMDEC, maintenance préventive. " +
            "Norme: ISO 9001:2015 §8.7."),
        Map.entry("First Pass Yield",
            "Formule: (Nb pièces conformes au 1er contrôle / Nb total fabriquées) × 100. Direction: higher_better. " +
            "Benchmark: >95%. Causes de dégradation: process instable, mauvais paramétrage, opérateurs non formés. " +
            "Actions: SMED, poka-yoke, standardisation des réglages. " +
            "Norme: ISO 9001:2015 §8.5.1."),
        Map.entry("Nombre de Réclamations Clients",
            "Formule: Nb total réclamations reçues sur la période. Direction: lower_better. " +
            "Benchmark: <5/mois pour PME. Causes: qualité produit non conforme, livraison tardive, communication. " +
            "Actions: traitement 8D, CAPA, mise en place NPS trimestriel. " +
            "Norme: ISO 9001:2015 §8.2.1."),
        Map.entry("Taux de Réussite des Audits",
            "Formule: (Nb points conformes / Nb points audités) × 100. Direction: higher_better. " +
            "Benchmark: >85%. Causes de dégradation: non-maîtrise des processus, documentation obsolète. " +
            "Actions: plan d'actions post-audit, formation aux exigences normatives. " +
            "Norme: ISO 9001:2015 §9.2."),
        Map.entry("Taux d'Absentéisme",
            "Formule: (Heures d'absence / Heures théoriques) × 100. Direction: lower_better. " +
            "Benchmark: <3.5% (France). Causes: TMS, stress, mauvaises conditions de travail. " +
            "Actions: mise à jour DUERP, programme ergonomie, démarche QVT. " +
            "Norme: ISO 45001:2018 §6.1.2."),
        Map.entry("Taux de Maladies Professionnelles",
            "Formule: (Nb maladies professionnelles déclarées × 1000) / Nb salariés. Direction: lower_better. " +
            "Benchmark: <1 cas/1000 salariés. Causes: expositions chimiques (CMR), TMS répétitifs. " +
            "Actions: substitution agents CMR, EPC prioritaires sur EPI, surveillance médicale renforcée. " +
            "Norme: ISO 45001:2018 §8.1.1."),
        Map.entry("Conformité Ergonomique",
            "Formule: (Nb postes conformes aux critères ergonomiques / Nb postes audités) × 100. Direction: higher_better. " +
            "Benchmark: >90%. Causes: aménagement inadapté, équipements vétustes, formation insuffisante. " +
            "Actions: analyse de poste de travail, acquisition matériel adapté, formation gestes et postures. " +
            "Norme: ISO 45001:2018 §6.1.2."),
        Map.entry("Taux de Visites Médicales",
            "Formule: (Nb salariés à jour de leur visite médicale / Nb total salariés) × 100. Direction: higher_better. " +
            "Benchmark: 100% (obligation réglementaire). Causes: planification insuffisante, refus salariés. " +
            "Actions: relances automatiques, organisation avec le service de santé au travail. " +
            "Norme: ISO 45001:2018 §8.6, Code du travail Art. R4624-10."),
        Map.entry("Qualité de l'Air (CO2)",
            "Formule: Mesure en ppm via capteurs CO2. Direction: lower_better. " +
            "Benchmark: <800 ppm (excellente), <1000 ppm (bonne), >1500 ppm (mauvaise). " +
            "Causes: ventilation insuffisante, effectif élevé dans espace fermé. " +
            "Actions: maintenance VMC, aération régulière, capteurs temps réel. " +
            "Norme: ISO 45001:2018 §8.1.4, Décret 2011-1728."),
        Map.entry("Taux de Renouvellement d'Air",
            "Formule: Débit d'air neuf introduit (m³/h) / Volume du local. Direction: higher_better. " +
            "Benchmark: >20 volumes/heure pour locaux de travail. " +
            "Causes de dégradation: ventilation défaillante, filtres colmatés. " +
            "Actions: maintenance préventive VMC, mesures de débit annuelles. " +
            "Norme: ISO 45001:2018 §8.1.4, Arrêté du 8 octobre 1987."),
        Map.entry("Indice d'Exposition au Bruit",
            "Formule: Leq mesuré en dB(A) sur période de référence 8h. Direction: lower_better. " +
            "Benchmark: <80 dB(A) sans EPI, seuil d'alerte 85 dB(A), limite réglementaire 87 dB(A). " +
            "Causes: machines bruyantes, acoustique locale déficiente. " +
            "Actions: capotage des sources, protections auditives, zones calmes, rotation des postes. " +
            "Norme: ISO 45001:2018 §8.1.1, Directive 2003/10/CE."),
        Map.entry("Usage des Équipements de Repos",
            "Formule: (Nb utilisations zones de repos observées / Nb salariés éligibles) × 100. Direction: higher_better. " +
            "Benchmark: >60%. Causes de sous-utilisation: manque de sensibilisation, localisation inadaptée. " +
            "Actions: aménagement attractif des zones, sensibilisation management, pauses planifiées. " +
            "Norme: ISO 45001:2018 §8.1."),
        Map.entry("Taux de Fréquence (TF1)",
            "Formule: (Nb accidents avec arrêt × 1 000 000) / Nb heures travaillées. Direction: lower_better. " +
            "Benchmark: <5 industrie manufacturière, <2 secteur tertiaire. " +
            "Causes: manque EPI, formation insuffisante, fatigue, non-respect consignes. " +
            "Actions: audit terrain, renforcer formations sécurité, révision des procédures, VMS. " +
            "Norme: ISO 45001:2018 §6.1.2."),
        Map.entry("Taux de Gravité (TG)",
            "Formule: (Nb jours d'arrêt de travail × 1 000) / Nb heures travaillées. Direction: lower_better. " +
            "Benchmark: <0.5. Causes: gravité des lésions, délai de retour au travail prolongé. " +
            "Actions: premiers secours efficaces, aménagement de poste, suivi RQTH, retour progressif. " +
            "Norme: ISO 45001:2018 §9.1.1."),
        Map.entry("Nombre de Presque-accidents",
            "Formule: Nb near-miss déclarés sur la période. Direction: higher_better (indique bonne culture sécurité). " +
            "Benchmark: >10 déclarations/mois encourage la culture de signalement. " +
            "Causes de sous-déclaration: peur des sanctions, manque de culture juste, procédure complexe. " +
            "Actions: culture juste et non-punitive, anonymat possible, communication des suites données. " +
            "Norme: ISO 45001:2018 §10.2."),
        Map.entry("Heures de Formation Sécurité",
            "Formule: Σ heures de formation sécurité / Nb employés. Direction: higher_better. " +
            "Benchmark: >8 heures/an/salarié (hors accueil nouveaux entrants). " +
            "Causes de déficit: contraintes budget, planification insuffisante. " +
            "Actions: e-learning, exercices pratiques terrain, habilitations réglementaires. " +
            "Norme: ISO 45001:2018 §7.2."),
        Map.entry("Taux de Port des EPI",
            "Formule: (Nb observations conformes EPI / Nb observations totales) × 100. Direction: higher_better. " +
            "Benchmark: >98%. Causes: inconfort, EPI inadaptés, manque de sensibilisation ou de supervision. " +
            "Actions: sélection EPI ergonomiques, formation, rappels visuels, VMS. " +
            "Norme: ISO 45001:2018 §8.1.1, Directive 89/686/CEE."),
        Map.entry("Nombre de Situations Dangereuses",
            "Formule: Nb situations dangereuses identifiées et signalées sur la période. Direction: lower_better (tendance). " +
            "Causes: dérive des process, équipements défaillants, non-respect des consignes. " +
            "Actions: analyse des risques actualisée, mise en conformité immédiate, traçabilité des actions. " +
            "Norme: ISO 45001:2018 §6.1.2."),
        Map.entry("Délai Levée des Non-Conformités",
            "Formule: Moyenne de (date de levée - date de détection) en jours. Direction: lower_better. " +
            "Benchmark: <7 jours pour NC critiques, <30 jours pour NC modérées. " +
            "Causes: manque de ressources, absence de priorisation, suivi défaillant. " +
            "Actions: responsabilisation des pilotes, tableau de bord de suivi, escalade automatique. " +
            "Norme: ISO 45001:2018 §10.2."),
        Map.entry("Nombre de Visites Sécurité (VMS)",
            "Formule: Nb de visites managériales sécurité terrain réalisées sur la période. Direction: higher_better. " +
            "Benchmark: >2 visites/semaine par manager de proximité. " +
            "Causes de déficit: manque d'engagement, pas de programme formalisé. " +
            "Actions: programme VMS structuré, formation des managers, suivi indicateur d'engagement. " +
            "Norme: ISO 45001:2018 §5.1."),
        Map.entry("Consommation Électricité",
            "Formule: kWh consommés / tonne de produit fini. Direction: lower_better. " +
            "Benchmark: objectif -3%/an, référence sectorielle ADEME. " +
            "Causes de surconsommation: équipements vétustes, process énergivores, comportements. " +
            "Actions: audit énergétique ISO 50002, variateurs de vitesse, LED, suivi temps réel. " +
            "Norme: ISO 14001:2015 §6.1.3, ISO 50001:2018."),
        Map.entry("Consommation Eau",
            "Formule: m³ d'eau consommés sur la période. Direction: lower_better. " +
            "Benchmark: défini par référentiel sectoriel (ADEME, FCD). " +
            "Causes: fuites non détectées, procédés humides, absence de sous-comptage. " +
            "Actions: audit eau, détection de fuites, récupération eaux de pluie, recyclage process. " +
            "Norme: ISO 14001:2015 §6.1.3, ISO 14046."),
        Map.entry("Taux de Valorisation Déchets",
            "Formule: (Masse déchets valorisés / Masse déchets totaux) × 100. Direction: higher_better. " +
            "Benchmark: >70%. Causes de sous-performance: tri à la source insuffisant, absence de débouchés. " +
            "Actions: sensibilisation tri sélectif, partenariats recycleurs, réduction à la source. " +
            "Norme: ISO 14001:2015 §8.1, Directive 2008/98/CE."),
        Map.entry("Émissions CO2 (Scope 1&2)",
            "Formule: Σ émissions directes (combustion, process) + indirectes énergie (tCO2eq). Direction: lower_better. " +
            "Benchmark: trajectoire -1.5°C Accord de Paris. " +
            "Causes: consommation énergie fossile, transport, process industriels. " +
            "Actions: efficacité énergétique, transition ENR, compensation carbone certifiée. " +
            "Norme: ISO 14001:2015 §6.1.3, ISO 14064, GHG Protocol."),
        Map.entry("Volume Déchets Dangereux",
            "Formule: Masse totale de déchets dangereux (DIS) produits sur la période (kg). Direction: lower_better. " +
            "Benchmark: minimisation réglementaire, objectif -5%/an. " +
            "Causes: process chimiques, produits de nettoyage, maintenance. " +
            "Actions: substitution produits dangereux, réduction à la source, partenariats éliminateurs agréés. " +
            "Norme: ISO 14001:2015 §8.1, Réglementation ICPE."),
        Map.entry("Consommation de Papier",
            "Formule: Nb rames de papier consommées / Nb collaborateurs. Direction: lower_better. " +
            "Benchmark: <2 rames/personne/an. Causes: manque de digitalisation, habitudes d'impression. " +
            "Actions: dématérialisation des processus, impression recto-verso par défaut, politique sans papier. " +
            "Norme: ISO 14001:2015 §6.1.3."),
        Map.entry("Incidents Environnementaux",
            "Formule: Nb de déversements, fuites ou rejets accidentels déclarés sur la période. Direction: lower_better. " +
            "Benchmark: objectif cible = 0. Causes: stockage inadapté, erreur opérateur, absence de procédures. " +
            "Actions: bacs de rétention, consignes d'urgence, exercices de simulation annuels. " +
            "Norme: ISO 14001:2015 §8.2, Directive SEVESO."),
        Map.entry("Part Énergie Renouvelable",
            "Formule: (Consommation ENR / Consommation énergie totale) × 100. Direction: higher_better. " +
            "Benchmark: >50% à horizon 2030 (objectif REpowerEU). " +
            "Causes de faiblesse: contrats d'approvisionnement classiques, absence d'investissement ENR. " +
            "Actions: Power Purchase Agreement (PPA), installation photovoltaïque en autoconsommation. " +
            "Norme: ISO 14001:2015 §6.1.3, ISO 50001:2018.")
    );

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

        log.info("Référentiel complet initialisé.");
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
                upsertRagKnowledge(nom, categorie.getCode(), seuilFaible, seuilModere, seuilCritique);
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

            upsertRagKnowledge(savedKpi.getNom(), categorie.getCode(), seuilFaible, seuilModere, seuilCritique);
        } catch (Exception ex) {
            log.error("Échec création KPI seed catégorie={} nom={} cause={}", categorie.getCode(), nom, ex.getMessage());
        }
    }

    private void upsertRagKnowledge(String kpiName, String categoryCode,
                                    Double seuilFaible, Double seuilModere, Double seuilCritique) {
        try {
            String enrichedDef = ENRICHED_DEFINITIONS.getOrDefault(kpiName,
                "KPI QHSE catégorie " + categoryCode + ". " +
                "Seuils: faible=" + seuilFaible + ", modéré=" + seuilModere + ", critique=" + seuilCritique + ".");

            String thresholds = String.format(java.util.Locale.US,
                "{\"faible\":%f, \"modere\":%f, \"critique\":%f}",
                seuilFaible, seuilModere, seuilCritique);

            ragKnowledgeRepository.findByKpiName(kpiName).ifPresentOrElse(
                existing -> {
                    // Update if definition was never enriched (short placeholder)
                    if (existing.getDefinition() == null || existing.getDefinition().length() < 100) {
                        existing.setDefinition(enrichedDef);
                        existing.setThresholds(thresholds);
                        existing.setUpdatedAt(LocalDateTime.now());
                        ragKnowledgeRepository.save(existing);
                        log.debug("RAG knowledge enrichi pour KPI: {}", kpiName);
                    }
                },
                () -> {
                    RagKnowledge rag = RagKnowledge.builder()
                            .kpiName(kpiName)
                            .definition(enrichedDef)
                            .category(categoryCode)
                            .thresholds(thresholds)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    ragKnowledgeRepository.save(rag);
                    log.info("RAG knowledge créé pour KPI: {}", kpiName);
                }
            );
        } catch (Exception ex) {
            log.error("Échec upsert RAG knowledge KPI={} cause={}", kpiName, ex.getMessage());
        }
    }
}
