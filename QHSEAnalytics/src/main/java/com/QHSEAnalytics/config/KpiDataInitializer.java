package com.QHSEAnalytics.config;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class KpiDataInitializer implements ApplicationRunner {

    private static final String CHUNK_FORMULE = "formule";
    private static final String CHUNK_INTERPRETATION = "interpretation";
    private static final String CHUNK_ACTION = "action";
    private static final String CHUNK_REGLEMENTATION = "reglementation";
    private static final List<String> KPI_CHUNK_TYPES = List.of(
        CHUNK_FORMULE,
        CHUNK_INTERPRETATION,
        CHUNK_ACTION,
        CHUNK_REGLEMENTATION
    );

    private final CategorieKpiRepository categorieKpiRepository;
    private final KpiRepository kpiRepository;
    private final RagKnowledgeRepository ragKnowledgeRepository;

    private static final Map<String, Map<String, String>> ENRICHED_CHUNKS = Map.ofEntries(
        Map.entry("Taux de Non-Conformité", chunkSet(
            text("Formule: (Nb non-conformites / Nb total produits) x 100.",
                "Unite: %.", "Direction: lower_better.",
                "Mesure la part de production non conforme aux exigences internes ou client."),
            text("TNC < 1%: tres bon niveau.", "TNC 1-2%: maitrise correcte.",
                "TNC 2-5%: derive significative et cout cache en hausse.",
                "TNC > 5%: situation critique avec risque client immediat."),
            text("Actions si TNC eleve: isoler les lots, lancer 5 pourquoi et Ishikawa,",
                "recalibrer les postes, renforcer l'auto-controle et verifier matiere, machine et methode sous 48h."),
            text("ISO 9001:2015 section 8.7 sur la maitrise des sorties non conformes,",
                "section 9.1 pour la surveillance et section 10.2 pour les actions correctives.",
                "La tracabilite des NC et CAPA doit etre conservee.")
        )),
        Map.entry("Coût de la Non-Qualité", chunkSet(
            text("Formule: couts rebuts + retouches + garanties + retours + reclamations.",
                "Unite: kEUR ou % du chiffre d'affaires.", "Direction: lower_better.",
                "Le KPI chiffre l'impact economique de la non-qualite."),
            text("CNQ < 2% du CA: mature.", "2-4%: niveau de vigilance.",
                "4-8%: erosion de marge importante.", "> 8%: desequilibre structurel du systeme qualite."),
            text("Actions prioritaires: cartographier Pareto des couts, traiter les causes recurrentes,",
                "standardiser les controles critiques, lancer chantier lean et revue fournisseurs."),
            text("ISO 9001:2015 section 10.2 et section 10.3.",
                "Le cout de non-qualite alimente la revue de direction et les plans d'amelioration continue.")
        )),
        Map.entry("Taux de Satisfaction Client", chunkSet(
            text("Formule: (Nb clients satisfaits / Nb clients interroges) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Peut etre alimente par enquetes satisfaction, CSAT ou NPS transforme."),
            text("> 90%: excellent.", "85-90%: bon niveau.", "75-85%: experience client fragile.",
                "< 75%: risque de churn, reclamations et perte d'image."),
            text("Actions si baisse: analyser verbatim, traiter les irritants delai/qualite/SAV,",
                "fermer la boucle reclamation en moins de 5 jours et lancer plan d'amelioration customer journey."),
            text("ISO 9001:2015 section 9.1.2 sur la satisfaction client et section 8.2.1 sur la communication client.",
                "Les preuves d'ecoute et de traitement doivent etre conservees.")
        )),
        Map.entry("Délai Moyen de Livraison", chunkSet(
            text("Formule: somme(date livraison reelle - date promise) / Nb commandes.",
                "Unite: jours.", "Direction: lower_better.",
                "Un delai negatif indique une livraison en avance."),
            text("< 2 jours: excellent.", "2-5 jours: acceptable.", "5-10 jours: degrade.",
                "> 10 jours: risque fort de penalites, reclamations et rupture de service."),
            text("Actions si delai derive: fiabiliser le PDP, securiser les stocks critiques,",
                "revoir capacite machine, fiabilite fournisseurs et suivi transport en temps reel."),
            text("ISO 9001:2015 section 8.5 sur la production et la prestation de service,",
                "section 8.2 sur les exigences client. Les engagements de delai doivent etre maitrises.")
        )),
        Map.entry("Taux de Rebuts", chunkSet(
            text("Formule: (Nb pieces rebut / Nb pieces fabriquees) x 100.",
                "Unite: %.", "Direction: lower_better.",
                "Mesure la perte definitive de matiere ou de produit."),
            text("< 1%: process robuste.", "1-3%: niveau de vigilance.", "3-5%: derive industrielle.",
                "> 5%: process instable avec impact marge et capacite."),
            text("Actions: lancer SPC, verifier reglages, inspection premier article,",
                "revue AMDEC process, maintenance preventive et qualification matiere premiere."),
            text("ISO 9001:2015 section 8.5.1 et 8.7.",
                "Les rebuts doivent etre traces, analyses et integres aux CAPA.")
        )),
        Map.entry("First Pass Yield", chunkSet(
            text("Formule: (Nb pieces conformes au premier controle / Nb total fabriquees) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Mesure la capacite a produire juste du premier coup."),
            text("FPY > 98%: excellent.", "95-98%: bon niveau.", "90-95%: derive de process.",
                "< 90%: instabilite forte, retouches et retards probables."),
            text("Actions si FPY faible: standardiser reglages, mettre poka-yoke, renforcer formation poste,",
                "surveiller capabilite process et traiter les defauts au poste source."),
            text("ISO 9001:2015 section 8.5.1 sur la maitrise de production.",
                "Le FPY sert de preuve d'efficacite des processus et des actions de prevention.")
        )),
        Map.entry("Nombre de Réclamations Clients", chunkSet(
            text("Formule: nombre total de reclamations recues sur la periode.",
                "Unite: nombre.", "Direction: lower_better.",
                "A suivre avec segmentation produit, client, cause et delai de cloture."),
            text("< 5/mois pour PME: maitrise.", "5-10/mois: vigilance.", "10-20/mois: signal de derive.",
                "> 20/mois: perte de confiance et risque commercial eleve."),
            text("Actions: ouvrir 8D sur cas recurrent, informer SAV et production, analyser tendances hebdomadaires,",
                "renforcer traçabilite et communication proactive vers les clients touches."),
            text("ISO 9001:2015 section 8.2.1, 9.1.2 et 10.2.",
                "Chaque reclamation doit etre qualifiee, tracee et reliee a une action corrective.")
        )),
        Map.entry("Taux de Réussite des Audits", chunkSet(
            text("Formule: (Nb points conformes / Nb points audites) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Le KPI mesure la conformite systeme et terrain aux exigences QHSE."),
            text("> 95%: excellent.", "85-95%: bon niveau.", "75-85%: maturite fragile.",
                "< 75%: gouvernance et maitrise documentaire a renforcer."),
            text("Actions: prioriser ecarts majeurs, mettre a jour procedures, former les pilotes,",
                "suivre plan d'actions a 30-60-90 jours et verifier l'efficacite en re-audit."),
            text("ISO 9001:2015 section 9.2 pour les audits internes et section 10.2 pour le traitement des ecarts.",
                "Les preuves de cloture doivent etre disponibles.")
        )),
        Map.entry("Taux d'Absentéisme", chunkSet(
            text("Formule: (Heures d'absence / Heures theoriques) x 100.",
                "Unite: %.", "Direction: lower_better.",
                "Le KPI suit l'impact combine de la sante, de l'organisation et du climat social."),
            text("< 3.5%: sain.", "3.5-5%: vigilance.", "5-7%: derive significative.",
                "> 7%: critique avec tension RH et perte de productivite."),
            text("Actions: analyser motifs, croiser avec TMS et charge, mettre a jour DUERP,",
                "lancer audit ergonomique, QVT ciblee et suivi managers des absences longues."),
            text("ISO 45001:2018 section 6.1.2 sur l'identification des risques,",
                "section 9.1.1 sur la surveillance et Code du travail pour la prevention primaire.")
        )),
        Map.entry("Taux de Maladies Professionnelles", chunkSet(
            text("Formule: (Nb maladies professionnelles declarees x 1000) / Nb salaries.",
                "Unite: cas pour 1000 salaries.", "Direction: lower_better.",
                "Indicateur avance des expositions durables et des TMS."),
            text("< 0.5/1000: faible.", "0.5-1/1000: vigilance.", "1-2/1000: derive reelle.",
                "> 2/1000: exposition mal maitrisee et risque contentieux."),
            text("Actions: revoir substitutions CMR, protections collectives, gestes repetitifs,",
                "surveillance medicale renforcee et plans ergonomie sur postes exposes."),
            text("ISO 45001:2018 section 8.1.1 et section 9.1.1.",
                "Le suivi MP alimente le DUERP, la CSSCT et les echanges avec la medecine du travail.")
        )),
        Map.entry("Conformité Ergonomique", chunkSet(
            text("Formule: (Nb postes conformes aux criteres ergonomiques / Nb postes audites) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Mesure l'adaptation des postes aux contraintes humaines."),
            text("> 95%: tres bon niveau.", "90-95%: acceptable.", "80-90%: risque TMS modere.",
                "< 80%: plan de transformation ergonomique necessaire."),
            text("Actions: analyse de poste, reduction des ports de charge, aides a la manutention,",
                "adaptation hauteur/gestes et formation gestes et postures avec verification terrain."),
            text("ISO 45001:2018 section 6.1.2 et section 8.1.",
                "Le DUERP doit formaliser les risques ergonomiques et les mesures de maitrise.")
        )),
        Map.entry("Taux de Visites Médicales", chunkSet(
            text("Formule: (Nb salaries a jour de leur visite medicale / Nb total salaries) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Indicateur de conformite du suivi de sante au travail."),
            text("100%: conforme.", "95-99%: vigilance administrative.", "< 95%: non-conformite potentielle.",
                "< 90%: risque reglementaire et perte de maitrise des postes a suivi renforce."),
            text("Actions: relances automatiques, planification annuelle avec le service de prevention et de sante au travail,",
                "priorisation des postes a risques et tableau de bord RH mensuel."),
            text("Code du travail article R4624-10 et suivants.",
                "ISO 45001:2018 section 8.1 et 8.6. L'entreprise doit demontrer la tenue du suivi medical.")
        )),
        Map.entry("Qualité de l'Air (CO2)", chunkSet(
            text("Formule: concentration moyenne de CO2 mesuree par capteurs.",
                "Unite: ppm.", "Direction: lower_better.",
                "Le CO2 sert d'indicateur proxy de renouvellement d'air et de confinement."),
            text("< 800 ppm: excellent.", "800-1000 ppm: correct.", "1000-1500 ppm: degrade.",
                "> 1500 ppm: mauvaise qualite d'air avec inconfort et risque sanitaire accru."),
            text("Actions: controler VMC, augmenter debit d'air neuf, verifier occupation reelle,",
                "ouvrir campagnes de mesure en continu et corriger les zones sous-ventilees."),
            text("ISO 45001:2018 section 8.1.4 sur l'environnement de travail.",
                "En France, le suivi de la qualite de l'air interieur et des equipements de ventilation doit etre maitrise.")
        )),
        Map.entry("Taux de Renouvellement d'Air", chunkSet(
            text("Formule: debit d'air neuf introduit / volume du local.",
                "Unite: volumes par heure.", "Direction: higher_better.",
                "Mesure l'efficacite du systeme de ventilation."),
            text("> 20 vol/h: bon niveau pour locaux exigeants.", "15-20 vol/h: acceptable selon usage.",
                "10-15 vol/h: insuffisant.", "< 10 vol/h: risque de confinement et inconfort."),
            text("Actions: mesurer debits reels, remplacer filtres colmates, regler CTA/VMC,",
                "equilibrer reseau et programmer maintenance preventive avec verification annuelle."),
            text("ISO 45001:2018 section 8.1.4 et exigences techniques ventilation des locaux de travail.",
                "Les preuves de controle et maintenance doivent etre conservees.")
        )),
        Map.entry("Indice d'Exposition au Bruit", chunkSet(
            text("Formule: Leq ou LEX,8h mesure sur la periode de reference.",
                "Unite: dB(A).", "Direction: lower_better.",
                "Le KPI suit l'exposition reelle des salaries au bruit."),
            text("< 80 dB(A): maitrise.", "80-85 dB(A): vigilance et information.", "85-87 dB(A): action immediate.",
                "> 87 dB(A): depassement de la valeur limite apres prise en compte des EPI."),
            text("Actions: capotage, silencieux, reduction a la source, rotation de postes,",
                "protections auditives adaptees et campagne de mesures complementaires."),
            text("Directive 2003/10/CE et Code du travail sur le bruit.",
                "ISO 45001:2018 section 8.1.1. La surveillance de l'exposition et l'audiometrie doivent etre maitrisees.")
        )),
        Map.entry("Usage des Équipements de Repos", chunkSet(
            text("Formule: (Nb utilisations des zones de repos observees / Nb salaries eligibles) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Le KPI apprecie l'usage effectif des moyens de recuperation."),
            text("> 70%: bonne appropriation.", "50-70%: acceptable.", "30-50%: usage faible.",
                "< 30%: amenagement ou culture de pause probablement inadaptes."),
            text("Actions: revoir emplacement et confort des zones, planifier pauses, sensibiliser managers,",
                "croiser avec fatigue, bruit et temperature pour comprendre les freins d'usage."),
            text("ISO 45001:2018 section 7.3 et 8.1 sur le bien-etre au travail et les conditions de travail.",
                "Le dispositif doit contribuer a la prevention de la fatigue.")
        )),
        Map.entry("Taux de Fréquence (TF1)", chunkSet(
            text("Formule: (Nb accidents avec arret x 1 000 000) / Nb heures travaillees.",
                "Unite: accidents par million d'heures.", "Direction: lower_better.",
                "Variante TF2 integre aussi les accidents sans arret selon les pratiques internes."),
            text("TF1 < 2: excellent en tertiaire.", "TF1 2-5: acceptable en industrie maitrisée.",
                "TF1 5-15: degrade, action corrective requise.", "TF1 > 15: critique, arret de chantier possible."),
            text("Actions correctives TF1 eleve: audit terrain immediat, renforcement formations SST sous 30 jours,",
                "revision EPI, analyse des near-miss sur 90 jours et quart d'heure securite quotidien."),
            text("ISO 45001:2018 section 6.1.2, 9.1.1 et 10.2.",
                "Code du travail article L4121-1. Declaration AT CPAM sous 48h et reporting CSSCT.")
        )),
        Map.entry("Taux de Gravité (TG)", chunkSet(
            text("Formule: (Nb jours d'arret de travail x 1 000) / Nb heures travaillees.",
                "Unite: jours perdus pour 1 000 heures.", "Direction: lower_better.",
                "Le TG mesure la severite des accidents au-dela de leur frequence."),
            text("TG < 0.2: tres bon niveau.", "0.2-0.5: maitrise correcte.", "0.5-1: degrade.",
                "> 1: accidents lourds ou retours au travail mal prepares."),
            text("Actions: revue des accidents graves, coordination medecine du travail, adaptation de poste,",
                "plan retour progressif et verification des mesures de premiers secours."),
            text("ISO 45001:2018 section 9.1.1 et section 10.2.",
                "Le suivi TG complete le TF1 pour les analyses d'accidentologie et les plans de prevention.")
        )),
        Map.entry("Nombre de Presque-accidents", chunkSet(
            text("Formule: nombre de near-miss ou presqu'accidents declares sur la periode.",
                "Unite: nombre.", "Direction: higher_better.",
                "Un niveau eleve signale en general une culture de remontee mature."),
            text("> 10/mois: culture de signalement active.", "5-10/mois: acceptable.",
                "< 5/mois: possible sous-declaration.", "Une baisse simultanee avec hausse TF1 est un signal d'alerte fort."),
            text("Actions: simplifier declaration, anonymat possible, retour d'experience visible,",
                "analyse mensuelle de la pyramide de Bird et communication des actions prises."),
            text("ISO 45001:2018 section 10.2 sur les incidents, non-conformites et actions correctives.",
                "La declaration sans culture punitive est une bonne pratique cle.")
        )),
        Map.entry("Heures de Formation Sécurité", chunkSet(
            text("Formule: somme des heures de formation securite / Nb employes.",
                "Unite: heures par salarie et par an.", "Direction: higher_better.",
                "A suivre separement pour nouveaux entrants, habilitations et recyclages."),
            text("> 8 h/an/salarie: bon niveau.", "4-8 h: vigilance.", "< 4 h: insuffisant pour secteurs exposes.",
                "Une baisse continue precede souvent une derive TF1 et des ecarts comportementaux."),
            text("Actions: prioriser nouveaux entrants et postes critiques, combiner theorie et terrain,",
                "tenir un plan annuel d'habilitations et mesurer l'efficacite par observation terrain."),
            text("ISO 45001:2018 section 7.2 et 7.3.",
                "L'employeur doit demontrer competence, information et adequation des formations securite.")
        )),
        Map.entry("Taux de Port des EPI", chunkSet(
            text("Formule: (Nb observations conformes EPI / Nb observations totales) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Mesure la discipline terrain et l'adaptation des protections."),
            text("> 98%: excellent.", "95-98%: bon niveau.", "90-95%: fragile.", "< 90%: risque accidentel eleve."),
            text("Actions: verifier adequation et confort des EPI, rebriefer terrain,",
                "multiplier VMS, affichage visuel et management de proximite sur zones critiques."),
            text("ISO 45001:2018 section 8.1.1 et obligations de mise a disposition/utilisation des EPI.",
                "Les controles terrain doivent etre traces.")
        )),
        Map.entry("Nombre de Situations Dangereuses", chunkSet(
            text("Formule: nombre de situations dangereuses identifiees et signalees sur la periode.",
                "Unite: nombre.", "Direction: lower_better sur la tendance brute,",
                "mais une hausse ponctuelle peut aussi traduire une meilleure detection."),
            text("Une hausse isolee avec beaucoup de VMS peut etre positive.",
                "Une hausse durable avec accidents ou near-miss eleves indique une derive de maitrise.",
                "Le contexte de declaration doit toujours etre analyse."),
            text("Actions: qualifier gravite/probabilite, corriger immediatement les dangers majeurs,",
                "mettre a jour l'analyse de risques et suivre delai de levee des actions."),
            text("ISO 45001:2018 section 6.1.2 sur l'identification des dangers et section 8.1 sur la maitrise operationnelle.",
                "Le DUERP doit etre mis a jour si le risque evolue.")
        )),
        Map.entry("Délai Levée des Non-Conformités", chunkSet(
            text("Formule: moyenne(date de levee - date de detection).",
                "Unite: jours.", "Direction: lower_better.",
                "Mesure la reactivite du systeme de traitement des ecarts."),
            text("< 7 jours pour ecarts critiques: bon niveau.", "7-30 jours: acceptable selon gravite.",
                "> 30 jours: inertie organisationnelle.", "Une hausse continue degrade la credibilite du systeme QHSE."),
            text("Actions: prioriser par criticite, nommer un pilote par ecart,",
                "mettre escalade automatique, revue hebdomadaire et preuve de verification d'efficacite."),
            text("ISO 45001:2018 section 10.2 et ISO 9001:2015 section 10.2.",
                "Les delais de correction doivent etre maitrises et justifies.")
        )),
        Map.entry("Nombre de Visites Sécurité (VMS)", chunkSet(
            text("Formule: nombre de visites manageriales securite terrain realisees sur la periode.",
                "Unite: nombre.", "Direction: higher_better.",
                "Le KPI mesure la presence managériale prevention sur le terrain."),
            text("> 2 visites/semaine/manager: bon niveau.", "1-2: acceptable.", "< 1: engagement insuffisant.",
                "Une baisse durable reduit detection precoce des ecarts comportementaux."),
            text("Actions: planifier VMS hebdomadaires, former managers a l'observation,",
                "tracer themes, actions et delais de cloture pour ancrer la prevention dans le quotidien."),
            text("ISO 45001:2018 section 5.1 sur le leadership et section 7.4 sur la communication.",
                "Les VMS sont une preuve de presence terrain du management.")
        )),
        Map.entry("Consommation Électricité", chunkSet(
            text("Formule: kWh consommes / tonne de produit fini ou par unite d'oeuvre equivalente.",
                "Unite: kWh/t.", "Direction: lower_better.",
                "Le KPI mesure l'intensite energetique du process."),
            text("Objectif courant: -3%/an.", "Stable avec activite comparable: maitrise relative.",
                "Hausse > 5% a volume constant: derive a investiguer.", "Une derive durable impacte cout et Scope 2."),
            text("Actions: audit energetique, variateurs de vitesse, chasse aux veilles,",
                "pilotage horaire, sous-comptage par ligne et maintenance des moteurs/compresseurs."),
            text("ISO 14001:2015 section 6.1.3 et ISO 50001:2018.",
                "La consommation significative doit etre surveillee et integree au plan environnemental.")
        )),
        Map.entry("Consommation Eau", chunkSet(
            text("Formule: volume total d'eau consomme sur la periode ou par unite d'oeuvre.",
                "Unite: m3.", "Direction: lower_better.",
                "Le suivi doit distinguer process, sanitaire et utilites."),
            text("La reference depend du secteur.", "Stabilite a production comparable: maitrise relative.",
                "Hausse > 10% sans changement d'activite: suspicion de fuite ou derive process."),
            text("Actions: sous-comptage, recherche de fuites, optimisation lavages/CIP,",
                "recyclage interne, recuperation eaux pluviales et revue des usages non productifs."),
            text("ISO 14001:2015 section 6.1.3 et ISO 14046.",
                "La gestion de la ressource eau doit etre documentee et pilotee selon les impacts significatifs.")
        )),
        Map.entry("Taux de Valorisation Déchets", chunkSet(
            text("Formule: (Masse dechets valorises / Masse dechets totaux) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Le KPI suit la performance de tri et de recyclage/valorisation."),
            text("> 70%: bon niveau industriel.", "50-70%: correct mais perfectible.", "30-50%: faible.",
                "< 30%: organisation de tri insuffisante ou flux peu valorisables."),
            text("Actions: tri a la source, filieres recycleurs, reduction emballages,",
                "revue contrats prestataires et analyse des flux de dechets dangereux vs banals."),
            text("ISO 14001:2015 section 8.1 et Directive 2008/98/CE.",
                "La hierarchie des dechets privilegie prevention, reutilisation puis valorisation.")
        )),
        Map.entry("Émissions CO2 (Scope 1&2)", chunkSet(
            text("Formule: somme emissions directes combustibles/process + emissions indirectes energie achetee.",
                "Unite: tCO2eq.", "Direction: lower_better.",
                "Le KPI suit l'empreinte carbone sous controle direct de l'entreprise."),
            text("Une trajectoire compatible 1.5C vise environ -4.2%/an.",
                "Stagnation a activite stable: opportunites de decarbonation non captees.",
                "Hausse > 5%: derive significative a investiguer."),
            text("Actions: efficacite energetique, electrification, PPA/ENR, optimisation utilites,",
                "plan de reduction par usage et verification des facteurs d'emission."),
            text("ISO 14001:2015 section 6.1.3, ISO 14064 et GHG Protocol.",
                "Le bilan doit rester tracable, documente et coherent avec la strategie climat.")
        )),
        Map.entry("Volume Déchets Dangereux", chunkSet(
            text("Formule: masse totale de dechets dangereux produits sur la periode.",
                "Unite: kg ou t.", "Direction: lower_better.",
                "Inclut DIS, solvants, boues, emballages souilles et flux reglementes."),
            text("Objectif general: reduction continue.", "Stabilite peut etre acceptable si activite stable.",
                "Hausse > 10%: derive process ou substitution insuffisante.", "Le KPI conditionne cout et risque ICPE."),
            text("Actions: substitution produits, reduction a la source, tri rigoureux,",
                "stockage securise, revue fournisseurs et suivi des BSD/filieres agreées."),
            text("ISO 14001:2015 section 8.1 et reglementation dechets dangereux/ICPE.",
                "Traçabilite, conditionnement et elimination via filiere autorisee sont obligatoires.")
        )),
        Map.entry("Consommation de Papier", chunkSet(
            text("Formule: Nb rames de papier consommees / Nb collaborateurs ou par site.",
                "Unite: rames/personne/an.", "Direction: lower_better.",
                "Le KPI mesure la maturite de dematerialisation."),
            text("< 2 rames/personne/an: bon niveau.", "2-4: acceptable.", "4-6: usage eleve.",
                "> 6: dependance au papier et opportunite digitale forte."),
            text("Actions: impression recto-verso par defaut, workflows dematerialises, signature electronique,",
                "sensibilisation usages et suppression des impressions systematiques."),
            text("ISO 14001:2015 section 6.1.3.",
                "Le papier est un aspect environnemental indirect a maitriser via prevention et sobrieté.")
        )),
        Map.entry("Incidents Environnementaux", chunkSet(
            text("Formule: nombre de deversements, fuites, rejets accidentels ou ecarts environnementaux declares.",
                "Unite: nombre.", "Direction: lower_better.",
                "L'objectif cible reste zero incident significatif."),
            text("0: conforme a l'objectif.", "1 incident: vigilance et analyse complete.", ">= 2: derive serieuse.",
                "Tout incident majeur impose revue immediate des barrières de prevention."),
            text("Actions: securiser la zone, contenir le rejet, analyser causes,",
                "mettre a jour plans d'urgence, exercices et maintenance des stockages/retentions."),
            text("ISO 14001:2015 section 8.2 sur la preparation et reponse aux situations d'urgence.",
                "La declaration aux autorites depend de la nature et gravite de l'evenement.")
        )),
        Map.entry("Part Énergie Renouvelable", chunkSet(
            text("Formule: (Consommation energie renouvelable / Consommation energie totale) x 100.",
                "Unite: %.", "Direction: higher_better.",
                "Mesure la part d'energie bas carbone d'origine renouvelable."),
            text("> 50%: trajectoire ambitieuse.", "30-50%: niveau intermediaire.", "10-30%: debut de transition.",
                "< 10%: dependance forte aux approvisionnements conventionnels."),
            text("Actions: PPA, autoconsommation photovoltaïque, garanties d'origine,",
                "electrification des usages et revue des contrats d'achat energie."),
            text("ISO 14001:2015 section 6.1.3 et ISO 50001:2018.",
                "Le mix energetique doit etre suivi pour appuyer la trajectoire climat.")
        ))
    );

    private static final Map<String, String> CORRELATION_CHUNKS = Map.ofEntries(
        Map.entry("Corrélation: TF1 ↔ Port EPI", text(
            "Correlation forte (r~0.82): un TF1 > 5 est associe dans 78% des cas a un Taux de Port EPI < 90%.",
            "Diagnostic: si TF1 eleve et port EPI faible, le probleme est d'abord comportemental et managérial.",
            "Si TF1 eleve mais port EPI correct, investiguer plutot materiel, procedure ou exposition residuelle via AMDEC."
        )),
        Map.entry("Corrélation: TF1 ↔ Formation Sécurité", text(
            "Correlation moderee (r~0.65): chaque reduction de 2 h/an de formation securite est associee a +1.2 point de TF1 sur 12 mois.",
            "Seuil critique: < 4 h/an/salarie augmente fortement le risque de TF1 > 10.",
            "Action: prioriser nouveaux entrants, car la majorite des AT se concentre sur la premiere annee."
        )),
        Map.entry("Corrélation: Taux de Non-Conformité ↔ First Pass Yield", text(
            "Relation inverse directe: FPY ~ 100 - TNC dans une approximation simple.",
            "Un TNC > 3% implique generalement un FPY < 97%.",
            "Si TNC et FPY se degradent ensemble, le process est systemiquement instable.",
            "Si TNC monte mais FPY reste stable, suspecter un probleme de detection tardive plutot que de fabrication."
        )),
        Map.entry("Corrélation: Absentéisme ↔ Maladies Professionnelles", text(
            "Correlation forte (r~0.75) dans les secteurs exposes aux TMS.",
            "Un taux de MP > 1/1000 precede souvent une hausse d'absenteisme d'environ 1.5 point dans les 6 mois suivants.",
            "Action preventive: des MP > 0.5/1000, declencher un audit ergonomique complet et revoir le DUERP."
        )),
        Map.entry("Corrélation: Consommation Électricité ↔ Émissions CO2 Scope 2", text(
            "Relation lineaire directe: 1 kWh d'electricite France correspond environ a 0.052 kgCO2eq.",
            "Reduire la consommation electrique de 10% reduit mecaniquement le Scope 2 de 10% a facteur constant.",
            "Levier prioritaire: variateurs de vitesse sur moteurs avec gains usuels de 20 a 40%."
        )),
        Map.entry("Corrélation: Réclamations Clients ↔ Taux de Non-Conformité", text(
            "Decalage temporel de 3 a 8 semaines: une hausse du TNC se traduit ensuite par une hausse des reclamations.",
            "Seuil d'alerte: TNC > 2% peut annoncer +30% de reclamations a horizon 1 a 2 mois.",
            "Action: prevenir le SAV et renforcer les controles expedition des TNC > 1.5%."
        )),
        Map.entry("Corrélation: Taux de Valorisation Déchets ↔ Volume Déchets Dangereux", text(
            "Correlation negative: plus la part de dechets dangereux est elevee, plus le taux de valorisation global baisse.",
            "Chaque 10% de DIS dans le flux total fait perdre environ 7 points de valorisation maximale theorique.",
            "Strategie: reduire le dechet dangereux a la source avant d'optimiser les filieres de valorisation."
        )),
        Map.entry("Corrélation: Near-Miss ↔ Taux de Fréquence TF1", text(
            "Relation inverse contre-intuitive: beaucoup de near-miss declares signale souvent une bonne culture securite.",
            "Ratio sain: environ 30 near-miss pour 1 accident selon la pyramide de Bird.",
            "Alerte: near-miss < 5/mois avec TF1 > 3 traduit probablement une sous-declaration et un risque d'accident grave."
        ))
    );

    private static final Map<String, String> BENCHMARK_SECTORIEL_CHUNKS = Map.ofEntries(
        Map.entry("Benchmark sectoriel: TF1", text(
            "TF1 par secteur France source CNAM 2023: BTP=17.8, Industrie manufacturiere=8.2, Agroalimentaire=9.1, Logistique/Transport=12.4, Chimie/Pharma=4.3, Tertiaire=2.1, Sante=6.8.",
            "Interpretation: un TF1 de 8 est moyen dans le BTP mais critique dans le tertiaire.",
            "Toujours comparer au benchmark du secteur reel de l'entreprise analysee."
        )),
        Map.entry("Benchmark sectoriel: Taux de Non-Conformité", text(
            "TNC par secteur: Agroalimentaire <0.5% (GFSI), Automobile <0.1% (TS 16949), Pharmaceutique <0.01% (GMP), Industrie generale <2% (ISO 9001), E-commerce <1%.",
            "Le cout de non-qualite represente souvent 5 a 15% du CA selon secteur source ASQ.",
            "Objectif lean de reference: zero defaut."
        )),
        Map.entry("Benchmark sectoriel: Taux d'Absentéisme", text(
            "Absenteisme France 2023 source Ayming: Industrie=5.3%, BTP=4.8%, Sante=8.2%, Tertiaire=4.1%, Commerce=5.7%.",
            "Cout moyen estime: 3580 EUR/salarie/an.",
            "Seuil d'alerte general: > 5.5%, abaisse a 4.5% dans les secteurs tres exposes aux TMS."
        )),
        Map.entry("Benchmark sectoriel: Émissions CO2", text(
            "Intensite carbone par secteur source Bpifrance 2024 en tCO2eq/MEUR CA: Industrie lourde=250-800, Agroalimentaire=120-350, Chimie=180-500, Tertiaire=15-40, Logistique=200-600.",
            "Trajectoire SBTi 1.5C: environ -4.2%/an.",
            "CSRD obligatoire pour les entreprises >250 salaries a partir de 2025."
        )),
        Map.entry("Benchmark sectoriel: First Pass Yield", text(
            "FPY par secteur: Electronique=95-99%, Automobile=92-97%, Plastique injection=88-95%, Agroalimentaire=94-98%, Imprimerie=90-96%.",
            "World class manufacturing: FPY > 99.7% soit l'equivalent 3.4 DPMO.",
            "Un FPY < 85% traduit un processus instable exigeant une action urgente."
        ))
    );

    private static final Map<String, String> FEW_SHOT_EXAMPLES = Map.ofEntries(
        Map.entry("Exemple: analyse sécurité critique", text(
            "SITUATION: TF1=14.2 (critique, +35% vs N-1), Port EPI=81% (faible), Formation=3h/an.",
            "ANALYSE EXPERTE: convergence de trois signaux faibles indiquant une rupture de culture securite.",
            "Le ratio near-miss/accident est anormalement bas (4:1 au lieu de 30:1), signe de sous-declaration.",
            "CAUSES PROFONDES: pression production -> raccourcis securite -> incidents non declares -> absence de retour d'experience.",
            "PLAN D'ACTION: J1-J7 audit terrain VMS x5 et releve anonymise des near-miss; J8-J30 remise a niveau SST 1 jour et renouvellement EPI; J31-J90 mise en place culture juste et management visuel.",
            "SUCCES: TF1 < 7 a M+6 et near-miss declares > 15/mois a M+3."
        )),
        Map.entry("Exemple: analyse qualité dégradée", text(
            "SITUATION: TNC=4.8% (critique), FPY=89% (degrade), Reclamations=18/mois (+60%).",
            "ANALYSE EXPERTE: la degradation simultanee de TNC et FPY avec decalage de 6 semaines sur les reclamations pointe un probleme process apparu en debut de trimestre.",
            "ISHIKAWA: causes probables categorie Matiere et Machine, avec derive de calibration ou changement fournisseur.",
            "PLAN: J1 gel du lot et traçabilite, J3 recalibration et SPC, J14 audit fournisseur, J30 revue CAPA avec 8D formalise.",
            "PREDICTION: sans action, les reclamations peuvent atteindre 25/mois dans les 6 semaines."
        )),
        Map.entry("Exemple: analyse environnement positif", text(
            "SITUATION: Valorisation dechets=78% (+8 pts), CO2 Scope 1&2=-12% vs N-1, ENR=42%.",
            "ANALYSE EXPERTE: progression simultanee sur les trois axes environnementaux, avec preuve d'efficacite du PPA signe en janvier.",
            "Le taux de valorisation depasse le benchmark sectoriel de 70% grace au tri a la source.",
            "RECOMMANDATION: consolider via certification ISO 14001 sous 12 mois.",
            "PROCHAIN LEVIER: mesurer et reduire le Scope 3, souvent proche de 40% des emissions totales."
        )),
        Map.entry("Exemple: analyse hygiène multidimensionnelle", text(
            "SITUATION: Absenteisme=6.8% (critique), Maladies pro=2.1/1000, Ergonomie=72% (faible).",
            "ANALYSE EXPERTE: triangle TMS classique avec causalite ergonomie insuffisante -> maladies professionnelles -> absences longues.",
            "URGENCE REGLEMENTAIRE: DUERP a mettre a jour sans attendre.",
            "PLAN: semaine 1 analyse de poste par ergonome sur les trois postes les plus exposes; mois 1 acquisition d'equipements adaptatifs; mois 3 programme QVT formalise et suivi mensuel medecine du travail.",
            "ROI attendu: 18 mois environ via reduction AT/MP et absences."
        ))
    );

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initialisation des categories KPI et des donnees de base");

        CategorieKpi q = ensureCategory("Q", "Qualité", "Maîtrise opérationnelle et satisfaction");
        CategorieKpi h = ensureCategory("H", "Hygiène & Santé", "Préservation de la santé des collaborateurs");
        CategorieKpi s = ensureCategory("S", "Sécurité", "Prévention des risques et accidents");
        CategorieKpi e = ensureCategory("E", "Environnement", "Gestion des impacts et durabilité");

        ensureKpi(q, "Taux de Non-Conformité", "Ratio produits non conformes / total produit", UniteKpi.POURCENTAGE, 2.0, 5.0, 10.0, 1);
        ensureKpi(q, "Coût de la Non-Qualité", "Coûts des rebuts et retouches en k€", UniteKpi.NOMBRE, 500.0, 2000.0, 5000.0, 2);
        ensureKpi(q, "Taux de Satisfaction Client", "Indice de satisfaction global", UniteKpi.POURCENTAGE, 70.0, 80.0, 90.0, 3);
        ensureKpi(q, "Délai Moyen de Livraison", "Respect des délais promis (jours)", UniteKpi.NOMBRE, 2.0, 5.0, 10.0, 4);
        ensureKpi(q, "Taux de Rebuts", "Pourcentage de perte matière brute", UniteKpi.POURCENTAGE, 1.0, 3.0, 5.0, 5);
        ensureKpi(q, "First Pass Yield", "Produits conformes dès le premier essai", UniteKpi.POURCENTAGE, 90.0, 95.0, 98.0, 6);
        ensureKpi(q, "Nombre de Réclamations Clients", "Total des plaintes enregistrées", UniteKpi.NOMBRE, 2.0, 10.0, 25.0, 7);
        ensureKpi(q, "Taux de Réussite des Audits", "Score moyen des audits qualité internes", UniteKpi.POURCENTAGE, 75.0, 85.0, 95.0, 8);

        ensureKpi(h, "Taux d'Absentéisme", "Heures d'absence / Heures théoriques", UniteKpi.POURCENTAGE, 3.0, 6.0, 10.0, 1);
        ensureKpi(h, "Taux de Maladies Professionnelles", "Cas déclarés pour 1000 salariés", UniteKpi.NOMBRE, 0.0, 1.0, 2.0, 2);
        ensureKpi(h, "Conformité Ergonomique", "Postes de travail adaptés aux normes", UniteKpi.POURCENTAGE, 80.0, 90.0, 100.0, 3);
        ensureKpi(h, "Taux de Visites Médicales", "Salariés à jour de leur suivi médical", UniteKpi.POURCENTAGE, 90.0, 95.0, 100.0, 4);
        ensureKpi(h, "Qualité de l'Air (CO2)", "Niveau moyen de CO2 en ppm", UniteKpi.NOMBRE, 600.0, 1000.0, 1500.0, 5);
        ensureKpi(h, "Taux de Renouvellement d'Air", "Volume d'air renouvelé par heure", UniteKpi.NOMBRE, 20.0, 25.0, 30.0, 6);
        ensureKpi(h, "Indice d'Exposition au Bruit", "Moyenne des niveaux sonores en dB(A)", UniteKpi.NOMBRE, 80.0, 85.0, 90.0, 7);
        ensureKpi(h, "Usage des Équipements de Repos", "Fréquence d'utilisation des zones de pause", UniteKpi.POURCENTAGE, 40.0, 60.0, 80.0, 8);

        ensureKpi(s, "Taux de Fréquence (TF1)", "Accidents avec arrêt / million d'heures", UniteKpi.NOMBRE, 5.0, 15.0, 30.0, 1);
        ensureKpi(s, "Taux de Gravité (TG)", "Jours perdus / millier d'heures", UniteKpi.NOMBRE, 0.5, 1.0, 2.0, 2);
        ensureKpi(s, "Nombre de Presque-accidents", "Near-miss signalés (vigilance)", UniteKpi.NOMBRE, 5.0, 10.0, 20.0, 3);
        ensureKpi(s, "Heures de Formation Sécurité", "Total heures formation par employé", UniteKpi.NOMBRE, 5.0, 10.0, 20.0, 4);
        ensureKpi(s, "Taux de Port des EPI", "Conformité observée lors des rondes", UniteKpi.POURCENTAGE, 90.0, 95.0, 100.0, 5);
        ensureKpi(s, "Nombre de Situations Dangereuses", "Situations à risque signalées", UniteKpi.NOMBRE, 10.0, 25.0, 50.0, 6);
        ensureKpi(s, "Délai Levée des Non-Conformités", "Temps pour corriger une faille sécurité (jours)", UniteKpi.NOMBRE, 2.0, 7.0, 15.0, 7);
        ensureKpi(s, "Nombre de Visites Sécurité (VMS)", "Total des visites managériales terrain", UniteKpi.NOMBRE, 4.0, 8.0, 12.0, 8);

        ensureKpi(e, "Consommation Électricité", "kWh consommés par tonne produite", UniteKpi.KWH, 100.0, 200.0, 500.0, 1);
        ensureKpi(e, "Consommation Eau", "Mètres cubes d'eau consommés", UniteKpi.NOMBRE, 50.0, 150.0, 300.0, 2);
        ensureKpi(e, "Taux de Valorisation Déchets", "Déchets recyclés / Déchets totaux", UniteKpi.POURCENTAGE, 50.0, 70.0, 85.0, 3);
        ensureKpi(e, "Émissions CO2 (Scope 1&2)", "Tonnes de CO2 équivalent", UniteKpi.KG, 1000.0, 5000.0, 10000.0, 4);
        ensureKpi(e, "Volume Déchets Dangereux", "Total déchets toxiques ou polluants", UniteKpi.KG, 50.0, 200.0, 500.0, 5);
        ensureKpi(e, "Consommation de Papier", "Nombre de rames par collaborateur", UniteKpi.NOMBRE, 1.0, 3.0, 5.0, 6);
        ensureKpi(e, "Incidents Environnementaux", "Déversements ou fuites accidentelles", UniteKpi.NOMBRE, 0.0, 1.0, 2.0, 7);
        ensureKpi(e, "Part Énergie Renouvelable", "Pourcentage d'énergie propre utilisée", UniteKpi.POURCENTAGE, 10.0, 30.0, 50.0, 8);

        initCorrelationChunks();
        initBenchmarkSectorielChunks();
        initFewShotExamples();

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
        Integer ordre
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
                    .build();

                kpiRepository.save(kpi);
                log.info("KPI seed cree categorie={} nom={}", categorie.getCode(), nom);
            }

            upsertKpiChunks(nom, categorie.getCode(), seuilFaible, seuilModere, seuilCritique);
        } catch (Exception ex) {
            log.error("Echec creation KPI seed categorie={} nom={} cause={}", categorie.getCode(), nom, ex.getMessage());
        }
    }

    private void upsertKpiChunks(
        String kpiName,
        String categoryCode,
        Double seuilFaible,
        Double seuilModere,
        Double seuilCritique
    ) {
        Map<String, String> chunkDefinitions = ENRICHED_CHUNKS.get(kpiName);
        String thresholds = buildThresholdsJson(seuilFaible, seuilModere, seuilCritique);

        for (String chunkType : KPI_CHUNK_TYPES) {
            String definition = chunkDefinitions != null
                ? chunkDefinitions.get(chunkType)
                : buildDefaultChunkDefinition(kpiName, categoryCode, chunkType, seuilFaible, seuilModere, seuilCritique);
            upsertRagKnowledge(kpiName, categoryCode, chunkType, definition, thresholds);
        }
    }

    private void initCorrelationChunks() {
        CORRELATION_CHUNKS.forEach((kpiName, definition) ->
            upsertRagKnowledge(kpiName, null, "correlation", definition, null)
        );
    }

    private void initBenchmarkSectorielChunks() {
        BENCHMARK_SECTORIEL_CHUNKS.forEach((kpiName, definition) ->
            upsertRagKnowledge(kpiName, null, "benchmark_sectoriel", definition, null)
        );
    }

    private void initFewShotExamples() {
        FEW_SHOT_EXAMPLES.forEach((kpiName, definition) ->
            upsertRagKnowledge(kpiName, null, "exemple_analyse", definition, null)
        );
    }

    private void upsertRagKnowledge(
        String kpiName,
        String categoryCode,
        String chunkType,
        String definition,
        String thresholds
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            ragKnowledgeRepository.findByKpiNameAndChunkType(kpiName, chunkType).ifPresentOrElse(
                existing -> {
                    boolean changed = false;

                    if (!Objects.equals(existing.getDefinition(), definition)) {
                        existing.setDefinition(definition);
                        changed = true;
                    }
                    if (!Objects.equals(existing.getThresholds(), thresholds)) {
                        existing.setThresholds(thresholds);
                        changed = true;
                    }
                    if (!Objects.equals(existing.getCategory(), categoryCode)) {
                        existing.setCategory(categoryCode);
                        changed = true;
                    }
                    if (!Objects.equals(existing.getChunkType(), chunkType)) {
                        existing.setChunkType(chunkType);
                        changed = true;
                    }

                    if (changed) {
                        existing.setUpdatedAt(now);
                        ragKnowledgeRepository.save(existing);
                        log.debug("RAG knowledge mis a jour pour KPI={} chunk={}", kpiName, chunkType);
                    }
                },
                () -> {
                    RagKnowledge rag = RagKnowledge.builder()
                        .kpiName(kpiName)
                        .chunkType(chunkType)
                        .definition(definition)
                        .category(categoryCode)
                        .thresholds(thresholds)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                    ragKnowledgeRepository.save(rag);
                    log.info("RAG knowledge cree pour KPI={} chunk={}", kpiName, chunkType);
                }
            );
        } catch (Exception ex) {
            log.error("Echec upsert RAG knowledge KPI={} chunk={} cause={}", kpiName, chunkType, ex.getMessage());
        }
    }

    private String buildThresholdsJson(Double seuilFaible, Double seuilModere, Double seuilCritique) {
        return String.format(
            Locale.US,
            "{\"faible\":%f, \"modere\":%f, \"critique\":%f}",
            seuilFaible,
            seuilModere,
            seuilCritique
        );
    }

    private String buildDefaultChunkDefinition(
        String kpiName,
        String categoryCode,
        String chunkType,
        Double seuilFaible,
        Double seuilModere,
        Double seuilCritique
    ) {
        return switch (chunkType) {
            case CHUNK_FORMULE -> text(
                "KPI", kpiName + ".", "Categorie:", categoryCode + ".",
                "Seuils de reference:", "faible=" + seuilFaible + ",",
                "modere=" + seuilModere + ",", "critique=" + seuilCritique + "."
            );
            case CHUNK_INTERPRETATION -> text(
                "Interpretation attendue pour", kpiName + ":",
                "comparer la valeur au triplet faible/modere/critique et qualifier la derive."
            );
            case CHUNK_ACTION -> text(
                "Actions types pour", kpiName + ":",
                "verifier la donnee, traiter la cause racine et suivre un plan d'action date et pilote."
            );
            case CHUNK_REGLEMENTATION -> text(
                "Reglementation et referentiels a verifier pour", kpiName + ":",
                "ISO 9001, ISO 14001 ou ISO 45001 selon la categorie", categoryCode + "."
            );
            default -> text("KPI", kpiName, "categorie", categoryCode + ".");
        };
    }

    private static Map<String, String> chunkSet(
        String formule,
        String interpretation,
        String action,
        String reglementation
    ) {
        return Map.of(
            CHUNK_FORMULE, formule,
            CHUNK_INTERPRETATION, interpretation,
            CHUNK_ACTION, action,
            CHUNK_REGLEMENTATION, reglementation
        );
    }

    private static String text(String... parts) {
        return String.join(" ", parts);
    }
}
