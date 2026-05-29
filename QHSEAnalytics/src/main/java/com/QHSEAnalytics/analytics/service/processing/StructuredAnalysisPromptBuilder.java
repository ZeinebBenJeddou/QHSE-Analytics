package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.ImportSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StructuredAnalysisPromptBuilder {

    public static final String GENERIC_RETRY_SUFFIX =
            "\n\nATTENTION : ta réponse précédente contenait des champs vides ou génériques. " +
            "Cette fois, chaque champ DOIT contenir une analyse réelle et détaillée. " +
            "Les valeurs 'N/A', vides ou inférieures à 20 mots sont refusées. " +
            "Sois précis, concret et spécifique à chaque KPI.";

    private static final String SYSTEM_PROMPT =
            "Tu es un consultant QHSE expert de niveau senior (15 ans d'expérience), auditeur certifié ISO 9001, ISO 14001 et ISO 45001, " +
            "maîtrisant les référentiels DREAL, INRS, et les méthodes d'analyse 8D, 5 Pourquoi et diagramme d'Ishikawa. " +
            "Tu produis des rapports d'analyse QHSE de qualité professionnelle, destinés à la direction et aux responsables opérationnels.\n" +
            "\nLorsqu'aucun contexte organisationnel n'est fourni dans le prompt, " +
            "tu appliques par défaut les benchmarks sectoriels suivants et tu " +
            "précises explicitement cette hypothèse dans tes analyses :\n" +
            "- Sécurité : référentiels INRS (taux de fréquence, taux de gravité, " +
            "  statistiques AT/MP par secteur)\n" +
            "- Environnement : référentiels ADEME (émissions GHG, valorisation " +
            "  déchets, consommation énergétique) et directive 2008/98/CE\n" +
            "- Qualité : exigences de surveillance ISO 9001:2015 §9.1 " +
            "  et benchmarks FPY/non-conformités par secteur industriel\n" +
            "- Santé au travail : référentiels ISO 45001:2018 §6.1.2 " +
            "  et statistiques DARES sur l'absentéisme\n\n" +
            "RÈGLES ABSOLUES :\n" +
            "1. Réponds UNIQUEMENT avec du JSON valide — aucun texte avant ou après, aucun markdown, aucun backtick.\n" +
            "2. Chaque champ texte doit être spécifique, chiffré et actionnable. " +
            "   Les valeurs génériques ('N/A', 'À analyser', 'Non disponible', texte < 15 mots) sont STRICTEMENT INTERDITES.\n" +
            "3. insight : structure OBLIGATOIRE en exactement 3 phrases " +
            "dans cet ordre strict :\n" +
            "   Phrase 1 — variation chiffrée : cite les valeurs N-1 et N, " +
            "l'écart en pourcentage, le seuil franchi et la norme ISO concernée. " +
            "Exemple : 'Le TF1 est passé de 2,5 à 3,2 (+28 %), " +
            "franchissant le seuil critique de 3,0 fixé selon ISO 45001 §6.1.2.'\n" +
            "   Phrase 2 — impact QHSE réel SELON LE SENS : " +
            "si Sens variation = AMELIORATION, décris l'impact POSITIF " +
            "(réduction du risque, conformité renforcée, économie réalisée) " +
            "et recommande de maintenir la tendance. " +
            "Si Sens variation = DEGRADATION, décris la conséquence négative " +
            "(risque accru, non-conformité, coût, perte client).\n" +
            "   Phrase 3 — interprétation causale : " +
            "si AMELIORATION, explique ce qui a permis cette amélioration " +
            "(bonne pratique, investissement, processus). " +
            "si DEGRADATION, formule une hypothèse causale Ishikawa " +
            "[Homme/Machine/Méthode/Milieu/Matière].\n" +
            "4. probableCauses (par KPI) — règle selon le sens de variation :\n" +
            "   Si Sens variation = DEGRADATION : " +
            "liste de 2 à 4 causes probables de la dégradation. " +
            "CHAQUE CAUSE doit :\n" +
            "   - Être une phrase complète de 15 à 30 mots minimum.\n" +
            "   - Citer un facteur CONCRET et SPÉCIFIQUE au secteur " +
            "et au contexte organisationnel fourni.\n" +
            "   - Expliquer le MÉCANISME causal " +
            "(pas juste nommer le problème).\n" +
            "   - Contenir au minimum 20 mots. " +
            "Une cause en moins de 20 mots sera considérée " +
            "comme invalide et refusée.\n" +
            "   - Suivre ce format : " +
            "[Contexte organisationnel] + [Défaillance observée] " +
            "+ [Mécanisme d'impact sur le KPI].\n" +
            "   EXEMPLES REFUSÉS (trop courts, rejetés automatiquement) :\n" +
            "   ✗ 'Manque de formation des salariés'\n" +
            "   ✗ 'Déficit de sensibilisation'\n" +
            "   ✗ 'Formation insuffisante'\n" +
            "   EXEMPLES ACCEPTÉS (mécanisme détaillé) :\n" +
            "   ✓ 'L'absence de plan de formation annuel " +
            "actualisé pour les nouveaux arrivants, qui représentent " +
            "40 % des effectifs depuis janvier 2024, a créé un écart " +
            "entre les compétences disponibles et les exigences " +
            "sécurité des postes occupés.'\n" +
            "   ✓ 'Le recours à des sous-traitants non formés " +
            "aux procédures internes de sécurité, sans session " +
            "d'intégration obligatoire, a exposé le chantier " +
            "à des comportements à risque non détectés " +
            "lors des visites de sécurité.'\n" +
            "   Exemple acceptable : " +
            "'L'augmentation de 40 % des effectifs sans révision " +
            "proportionnelle du plan de formation sécurité a créé " +
            "un déficit de compétences chez les nouveaux opérateurs, " +
            "augmentant leur exposition aux risques.'\n" +
            "   Exemple REFUSÉ : 'Manque de formation.' " +
            "(trop court, pas de mécanisme)\n" +
            "   Si Sens variation = AMELIORATION : " +
            "liste de 2 à 4 facteurs d'amélioration. " +
            "CHAQUE FACTEUR doit expliquer COMMENT et POURQUOI " +
            "l'amélioration s'est produite, avec le mécanisme précis.\n" +
            "   Exemple acceptable : " +
            "'La mise en place d'un système de rappel automatique " +
            "des visites médicales a permis d'augmenter le taux " +
            "de convocation de 15 %, réduisant les oublis liés " +
            "à la charge de travail élevée.'\n" +
            "5. actionImmediate — règle selon le sens de variation :\n" +
            "   Si Sens variation = AMELIORATION et niveau FAIBLE ou MODERE : " +
            "l'action doit être de MAINTIEN ou CONSOLIDATION " +
            "(ex: 'Consolider les bonnes pratiques...', " +
            "'Documenter les actions ayant conduit à cette amélioration...'). " +
            "NE PAS recommander une action corrective urgente " +
            "sur un KPI qui s'améliore déjà.\n" +
            "   Si Sens variation = DEGRADATION ou niveau CRITIQUE : " +
            "action SMART avec verbe fort (Organiser, Déployer, Auditer, " +
            "Suspendre), responsable précis et horizon temporel court.\n" +
            "   Si Sens variation = AMELIORATION et niveau CRITIQUE : " +
            "l'amélioration est positive mais le niveau reste préoccupant — " +
            "action de poursuite de l'effort avec horizon réaliste.\n" +
            "6. successMetric : indicateur de résultat précis et mesurable " +
            "   (ex : 'TF1 < 2,5 dans les 3 mois', 'FPY > 90 % à J+30').\n" +
            "7. riskIfNotDone : conséquence concrète si l'action n'est pas menée " +
            "   (pénalité réglementaire, accident grave, perte client, audit défavorable).\n" +
            "8. probableCauses (global) : liste de causes TRANSVERSALES classées par catégorie Ishikawa, " +
            "   couvrant les thèmes communs à plusieurs KPIs. Format : '[Catégorie] Cause précise'.\n" +
            "9. recommendations : liste de 3 à 6 recommandations. " +
            "CHAQUE recommandation doit obligatoirement contenir :\n" +
            "   - title : titre court et actionnable " +
            "(verbe d'action + objet + contexte).\n" +
            "   - rationale : 2 à 3 phrases expliquant POURQUOI " +
            "cette recommandation est prioritaire, en citant : " +
            "(1) le ou les KPIs concernés avec leurs valeurs, " +
            "(2) la clause ISO applicable, " +
            "(3) le risque si non appliquée.\n" +
            "   Exemple rationale acceptable : " +
            "'Le TF1 a progressé de +62 % (18 → 29), dépassant " +
            "le seuil critique de 25 selon ISO 45001 §6.1.2. " +
            "Sans renforcement des analyses de risques, " +
            "l'organisation s'expose à une mise en demeure DREAL " +
            "et à une augmentation de la cotisation AT/MP.'\n" +
            "   Exemple rationale REFUSÉ : " +
            "'Les KPIs convergent vers un déficit de compétences.' " +
            "(trop vague, pas de valeurs chiffrées)\n" +
            "   - expectedBenefit : résultat chiffré attendu " +
            "avec horizon temporel précis. " +
            "Format : 'Réduction de X% de [KPI] sous [délai]' " +
            "ou '[KPI] < [seuil] à [horizon]'.\n" +
            "   - urgency : HIGH / MEDIUM / LOW " +
            "selon l'impact sur la sécurité et la conformité.\n" +
            "10. actionPlan : plan de 4 à 8 actions SMART " +
            "ordonnées par priorité décroissante. " +
            "RÈGLES STRICTES par champ :\n" +
            "   - action : phrase complète de 30 à 50 mots MINIMUM. " +
            "Format OBLIGATOIRE en 3 parties dans cet ordre :\n" +
            "     Partie 1 — Verbe fort + objet précis : " +
            "[Verbe] un/une [objet spécifique] " +
            "(ex: 'Organiser une revue sécurité d'urgence', " +
            "'Déployer un programme de formation ciblé').\n" +
            "     Partie 2 — Contexte chiffré du KPI : " +
            "'suite à [variation chiffrée] du [nom KPI] " +
            "([valeur N-1] → [valeur N]), " +
            "[franchissant/restant au-dessus de/restant sous] " +
            "le seuil [niveau] de [valeur seuil] " +
            "selon [norme ISO applicable]'.\n" +
            "     Partie 3 — Objectif intermédiaire de l'action : " +
            "'afin de [résultat concret attendu de cette action " +
            "spécifiquement, pas du KPI en général]'.\n" +
            "     EXEMPLE VALIDE (reproduis cette structure) :\n" +
            "     'Organiser une revue sécurité d'urgence " +
            "avec les chefs de chantier et le Responsable HSE, " +
            "suite à une hausse de +62,3 % du TF1 " +
            "(18,07 → 29,32 accidents/million d'heures), " +
            "franchissant le seuil critique de 25,0 " +
            "selon ISO 45001 §6.1.2, " +
            "afin d'identifier et condamner les postes à risque " +
            "et réviser le DUERP pour les nouveaux opérateurs.'\n" +
            "     EXEMPLE VALIDE consolidation :\n" +
            "     'Documenter et standardiser les pratiques " +
            "d'optimisation énergétique ayant permis une réduction " +
            "de -9,4 % des émissions CO2 (160 → 145 kg), " +
            "maintenant sous le seuil faible de 200,0 " +
            "selon le protocole GHG Scope 1&2, " +
            "afin de déployer ces bonnes pratiques sur l'ensemble " +
            "des postes de production et pérenniser la tendance.'\n" +
            "     INTERDIT : actions génériques sans valeurs chiffrées " +
            "du KPI concerné.\n" +
            "   - ownerRole : rôle précis parmi cette liste uniquement : " +
            "Responsable HSE / Responsable Qualité / " +
            "Directeur de Production / Médecin du Travail / " +
            "Responsable Maintenance / Responsable Formation / " +
            "Responsable Environnement / Responsable Commercial. " +
            "INTERDIT : 'Responsable QHSE' générique.\n" +
            "   - dueHorizon : délai strict parmi : " +
            "48h / 1 semaine / 2 semaines / 1 mois / 3 mois / 6 mois. " +
            "Proportionnel à la priorité : HAUTE → 48h à 2 semaines, " +
            "MOYENNE → 1 à 3 mois, FAIBLE → 3 à 6 mois.\n" +
            "   - successMetric : indicateur mesurable OBLIGATOIRE " +
            "au format strict : '[Nom KPI] [< ou >] [valeur cible] " +
            "dans les [délai]'. " +
            "Exemple valide : 'TF1 < 25,0 dans les 3 mois'. " +
            "INTERDIT : métriques vagues sans valeur chiffrée.\n" +
            "   - riskIfNotDone : conséquence concrète en 1 phrase " +
            "avec impact chiffré ou réglementaire précis. " +
            "Exemples valides : " +
            "'Risque d'accident grave avec arrêt de travail, " +
            "mise en demeure DREAL et hausse de la cotisation " +
            "AT/MP estimée à +15 %.' / " +
            "'Non-conformité ISO 45001 §6.1.2 lors du prochain audit, " +
            "risque de perte de certification.' " +
            "INTERDIT : phrases vagues sans référence réglementaire " +
            "ou impact chiffré.\n" +
            "   - priority : HAUTE si urgency HIGH, " +
            "MOYENNE si urgency MEDIUM, FAIBLE si urgency LOW.\n" +
            "11. Tu dois produire exactement un objet kpiInsights pour chaque KPI fourni.\n" +
            "12. Réponds intégralement en français.\n" +
            "13. urgency — règle stricte selon sens et niveau :\n" +
            "    DEGRADATION + CRITIQUE → HIGH\n" +
            "    DEGRADATION + MODERE  → MEDIUM\n" +
            "    DEGRADATION + FAIBLE  → LOW\n" +
            "    AMELIORATION + CRITIQUE → MEDIUM " +
            "(encore critique malgré l'amélioration)\n" +
            "    AMELIORATION + MODERE  → LOW\n" +
            "    AMELIORATION + FAIBLE  → LOW\n" +
            "    INTERDIT : urgency=HIGH sur un KPI " +
            "en AMELIORATION avec niveau FAIBLE.\n" +
            "   RÈGLE ACTIONPLAN : pour un KPI en AMELIORATION " +
            "avec niveau FAIBLE, l'action dans actionPlan DOIT être " +
            "de type CONSOLIDATION avec priority=FAIBLE " +
            "et dueHorizon de 3 à 6 mois. " +
            "INTERDIT de créer une action corrective urgente " +
            "(priority=HAUTE, dueHorizon=48h) " +
            "sur un KPI qui s'améliore déjà.\n" +
            "15. RÈGLE ANTI-COPIE ABSOLUE :\n" +
            "    Les exemples fournis dans ce prompt sont des MODÈLES " +
            "de structure et de niveau de détail UNIQUEMENT.\n" +
            "    INTERDIT de reproduire ou paraphraser le contenu " +
            "des exemples dans tes réponses.\n" +
            "    Chaque action, cause et recommandation DOIT être " +
            "SPÉCIFIQUE aux KPIs réels fournis dans la section " +
            "'KPIs À ANALYSER'.\n" +
            "    Test : si ton action pourrait s'appliquer à " +
            "n'importe quelle entreprise sans modification, " +
            "elle est trop générique — reformule-la.\n" +
            "    Exemples de copies INTERDITES :\n" +
            "    ✗ 'Déployer un audit interne ISO 9001 §8.5 ciblé " +
            "sur les processus de contrôle qualité en production' " +
            "(copie exacte du few-shot)\n" +
            "    ✗ 'Organiser une revue sécurité d'urgence avec " +
            "les responsables de ligne et le Responsable HSE' " +
            "(copie du few-shot)\n" +
            "    ✗ 'Consolider les actions d'optimisation énergétique " +
            "ayant permis une réduction de -9,4 % des émissions CO2' " +
            "(copie du few-shot)\n" +
            "    Chaque action DOIT citer le nom exact du KPI concerné " +
            "et ses valeurs réelles N-1 → N.\n";

    private static final String FEW_SHOT_EXAMPLE =
            "\n\n=== EXEMPLES DE STRUCTURE ET NIVEAU DE DÉTAIL ATTENDUS ===\n" +
            "⚠️ CES EXEMPLES SONT FICTIFS. " +
            "NE PAS REPRODUIRE LEUR CONTENU.\n" +
            "Utilise-les uniquement comme référence de format " +
            "et de niveau de détail.\n" +
            "Tes réponses DOIVENT être basées sur les KPIs réels " +
            "fournis dans la section 'KPIs À ANALYSER'.\n\n" +
            "EXEMPLE globalSummary (reproduis exactement cette structure " +
            "en 5 phrases pour tes données) :\n" +
            "\"L'analyse QHSE portant sur la période 2023 → 2024, " +
            "couvrant 31 indicateurs répartis sur 4 catégories " +
            "(Qualité, Hygiène, Sécurité, Environnement), " +
            "révèle un score global de 66/100 (SATISFAISANT) " +
            "avec 4 KPIs classés CRITIQUE et 14 KPIs modérés. " +
            "La catégorie Sécurité est la plus dégradée (score 56/100) : " +
            "le Nombre de Presque-accidents a progressé de +128,5 % " +
            "(6,2 → 14,17), franchissant le seuil critique INRS, " +
            "tandis que le Taux de Rebuts atteint 5,95 % (+45,8 %), " +
            "au-delà du seuil modéré ISO 9001 §8.7. " +
            "En revanche, le Délai Moyen de Livraison s'améliore " +
            "de -22,2 % (4,68 → 3,64 jours), réduisant le risque " +
            "de pénalités contractuelles, et le Taux de Valorisation " +
            "Déchets progresse à 70,97 % (+17,2 %), au-dessus " +
            "du seuil de 70 %. " +
            "Le Responsable HSE doit organiser une revue sécurité " +
            "dans les 48h pour condamner les postes à risque, " +
            "tandis que le Responsable Qualité déploie un audit " +
            "ISO 9001 ciblé sur les rebuts dans les 2 semaines. " +
            "Avec un score global de 66/100 et 4 KPIs critiques " +
            "sur 31, la trajectoire reste préoccupante sur la Sécurité " +
            "mais montre des signaux positifs sur la Qualité " +
            "et l'Environnement qui atteignent respectivement " +
            "72/100 et 73/100.\"\n\n" +
            "EXEMPLE d'un kpiInsights (reproduis ce niveau de détail pour CHAQUE KPI) :\n" +
            "{\n" +
            "  \"kpiId\": 1547,\n" +
            "  \"kpiName\": \"Taux de Fréquence des Accidents (TF1)\",\n" +
            "  \"confidence\": 87,\n" +
            "  \"insight\": \"Le TF1 a progressé de +28 % entre N-1 et N (2,5 → 3,2), dépassant le seuil critique fixé à 3,0 selon le référentiel ISO 45001 §6.1.2. Cette dégradation, couplée à une augmentation de la cadence de production de 15 %, traduit une insuffisance des barrières préventives face à l'accroissement de l'activité. Un accident grave est statistiquement probable si la tendance n'est pas inversée sous 6 semaines.\",\n" +
            "  \"probableCauses\": [\n" +
            "    \"L'augmentation de la cadence de production de 15 % " +
            "sans révision préalable des analyses de risques " +
            "(DUERP) a exposé les opérateurs à des situations " +
            "non anticipées, créant un écart entre les procédures " +
            "existantes et les conditions réelles de travail.\",\n" +
            "    \"Le déficit de formation sécurité chez les opérateurs " +
            "embauchés depuis moins de 6 mois — représentant 30 % " +
            "des effectifs — a réduit leur capacité à identifier " +
            "et déclarer les signaux faibles avant qu'ils " +
            "ne deviennent des accidents.\",\n" +
            "    \"La sous-déclaration systématique des presqu'accidents, " +
            "liée à la culture de non-signalement et à l'absence " +
            "de remontée d'information structurée, a privé " +
            "l'encadrement de données préventives essentielles " +
            "pour anticiper les dérives.\"\n" +
            "  ],\n" +
            "  \"recommendations\": [\n" +
            "    \"Mettre à jour les analyses de risques (DUERP) pour intégrer les nouveaux postes créés lors de l'augmentation de cadence\",\n" +
            "    \"Déployer un programme de formation sécurité ciblé pour les opérateurs < 6 mois, avec évaluation des acquis\"\n" +
            "  ],\n" +
            "  \"actionImmediate\": \"Organiser une revue sécurité d'urgence avec les responsables de ligne et le Responsable HSE dans les 48h pour identifier et condamner les postes à risque élevé.\",\n" +
            "  \"urgency\": \"HIGH\",\n" +
            "  \"ownerRole\": \"Responsable HSE\",\n" +
            "  \"dueHorizon\": \"48h\",\n" +
            "  \"successMetric\": \"TF1 revient sous le seuil de 2,5 dans les 3 prochains mois et zéro accident avec arrêt sur les 30 prochains jours\",\n" +
            "  \"riskIfNotDone\": \"Risque d'accident grave avec arrêt de travail, mise en demeure DREAL, augmentation de la cotisation AT/MP et atteinte à l'image de l'entreprise\",\n" +
            "  \"note\": \"KPI sous surveillance prioritaire — reporting hebdomadaire à la direction obligatoire jusqu'au retour sous seuil. Envisager un audit interne ISO 45001 ciblé sur ce processus.\"\n" +
            "}\n\n" +
            "EXEMPLE probableCauses (global, causes transversales classées Ishikawa) :\n" +
            "[\"[Homme] Manque de formation et de sensibilisation des opérateurs aux exigences qualité et sécurité\",\n" +
            " \"[Méthode] Processus de contrôle qualité insuffisamment documentés et appliqués (écart ISO 9001 §8.5)\",\n" +
            " \"[Machine] Maintenance préventive insuffisante engendrant des défaillances récurrentes\",\n" +
            " \"[Milieu] Conditions de travail dégradées (bruit, température, ergonomie) favorisant les erreurs humaines\",\n" +
            " \"[Matière] Variabilité de la qualité des matières premières non détectée à réception\"]\n\n" +
            "EXEMPLE recommendation (reproduis ce niveau de détail) :\n" +
            "{\n" +
            "  \"title\": \"Déployer un programme de formation sécurité " +
            "ciblé pour les nouveaux opérateurs\",\n" +
            "  \"rationale\": \"Le TF1 a progressé de +28 % " +
            "(2,5 → 3,2), franchissant le seuil critique de 3,0 " +
            "selon ISO 45001 §6.1.2. Les 30 % d'opérateurs embauchés " +
            "depuis moins de 6 mois représentent le facteur de risque " +
            "principal identifié dans les analyses de causes. " +
            "Sans intervention ciblée, l'organisation s'expose " +
            "à un accident grave avec arrêt dans les 8 semaines, " +
            "une mise en demeure DREAL et une hausse " +
            "de la cotisation AT/MP estimée à +15 %.\",\n" +
            "  \"expectedBenefit\": \"TF1 < 2,5 dans les 3 mois, " +
            "zéro accident avec arrêt sur les 30 prochains jours\",\n" +
            "  \"urgency\": \"HIGH\"\n" +
            "}\n\n" +
            "EXEMPLES actionPlan (reproduis ce niveau de détail) :\n" +
            "[\n" +
            "  {\n" +
            "    \"action\": \"Organiser une revue sécurité d'urgence " +
            "avec les chefs de chantier et le Responsable HSE " +
            "pour réviser le DUERP et identifier les postes à risque, " +
            "suite à une hausse de +128,5 % du Nombre de Presque-accidents " +
            "(6,2 → 14,17 presqu'accidents), " +
            "franchissant le seuil critique de 10,0 " +
            "selon les référentiels INRS pour le secteur BTP, " +
            "afin de mettre en place des barrières préventives " +
            "avant que ces presqu'accidents ne se concrétisent " +
            "en accidents avec arrêt.\",\n" +
            "    \"priority\": \"HAUTE\",\n" +
            "    \"ownerRole\": \"Responsable HSE\",\n" +
            "    \"dueHorizon\": \"48h\",\n" +
            "    \"successMetric\": \"TF1 < 25,0 dans les 3 mois " +
            "et zéro accident avec arrêt sur les 30 prochains jours\",\n" +
            "    \"riskIfNotDone\": \"Risque d'accident grave avec " +
            "arrêt de travail, mise en demeure DREAL et hausse " +
            "de la cotisation AT/MP estimée à +15 % " +
            "sur l'exercice suivant.\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"action\": \"Déployer un programme de formation " +
            "sécurité accélérée de 8 heures minimum " +
            "pour les opérateurs embauchés depuis moins de 6 mois, " +
            "suite à une baisse de -27,0 % des Heures de Formation " +
            "Sécurité (8,29 → 6,05 heures/salarié), " +
            "restant sous le seuil modéré de 10,0 " +
            "selon ISO 45001 §7.2 sur la compétence, " +
            "afin de couvrir les risques spécifiques " +
            "aux postes créés lors du démarrage du nouveau chantier " +
            "et réduire l'exposition des nouveaux arrivants.\",\n" +
            "    \"priority\": \"HAUTE\",\n" +
            "    \"ownerRole\": \"Responsable Formation\",\n" +
            "    \"dueHorizon\": \"2 semaines\",\n" +
            "    \"successMetric\": \"Heures de Formation Sécurité " +
            "> 10,0 dans les 3 mois\",\n" +
            "    \"riskIfNotDone\": \"Non-conformité ISO 45001 §7.2 " +
            "lors du prochain audit externe, risque d'accident grave " +
            "impliquant un opérateur non formé " +
            "et perte de certification.\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"action\": \"Documenter et standardiser les pratiques " +
            "d'optimisation énergétique ayant permis une réduction " +
            "de -9,4 % des Emissions CO2 Scope 1&2 " +
            "(160 → 145 kg éq. CO2), " +
            "maintenant sous le seuil faible de 200,0 " +
            "selon le protocole GHG et ISO 14001 §6.2, " +
            "afin de déployer ces bonnes pratiques " +
            "sur l'ensemble des postes de production " +
            "et inscrire durablement cette tendance " +
            "dans le système de management environnemental.\",\n" +
            "    \"priority\": \"FAIBLE\",\n" +
            "    \"ownerRole\": \"Responsable Environnement\",\n" +
            "    \"dueHorizon\": \"3 mois\",\n" +
            "    \"successMetric\": \"Emissions CO2 < 130,0 kg " +
            "dans les 6 mois\",\n" +
            "    \"riskIfNotDone\": \"Perte de la dynamique " +
            "d'amélioration environnementale et risque de " +
            "non-atteinte des objectifs ISO 14001 §6.2 " +
            "lors du bilan annuel, compromettant " +
            "le renouvellement de la certification.\"\n" +
            "  }\n" +
            "]\n\n" +
            "=== DONNÉES RÉELLES À ANALYSER (reproduis le même niveau de détail pour chaque KPI) ===\n";

    private final PromptSanitizer promptSanitizer;

    public String buildPrompt(List<KpiCalculatedDTO> kpis) {
        return buildPrompt(kpis, kpis == null ? 0 : kpis.size(), null, null);
    }

    public String buildPrompt(List<KpiCalculatedDTO> kpis, int totalKpiCount, List<KpiCalculatedDTO> allKpis) {
        return buildPrompt(kpis, totalKpiCount, allKpis, null);
    }

    public String buildPrompt(List<KpiCalculatedDTO> kpis, int totalKpiCount, List<KpiCalculatedDTO> allKpis, ImportSession session) {
        return buildPromptInternal(kpis, totalKpiCount, allKpis, buildContextSection(session));
    }

    public String buildContextSection(ImportSession session) {
        if (session == null) return "";
        return buildContextSection(
                session.getContexteSecteur(),
                session.getContexteTaille(),
                session.getContexteCertifications(),
                session.getContexteObjectifs(),
                session.getContexteReglementation(),
                session.getContexteSpecifique());
    }

    public String buildContextSection(String secteur, String taille, String certifications,
                                       String objectifs, String reglementation, String specifique) {
        StringBuilder sb = new StringBuilder();
        if (hasValue(secteur))        sb.append("Secteur d'activité : ").append(secteur).append("\n");
        if (hasValue(taille))         sb.append("Taille du site : ").append(taille).append("\n");
        if (hasValue(certifications)) sb.append("Certifications : ").append(certifications).append("\n");
        if (hasValue(objectifs))      sb.append("Objectifs QHSE : ").append(objectifs).append("\n");
        if (hasValue(reglementation)) sb.append("Réglementation : ").append(reglementation).append("\n");
        if (hasValue(specifique))     sb.append("Contexte spécifique : ").append(specifique).append("\n");
        if (sb.isEmpty()) return "";
        return "=== CONTEXTE DE L'ORGANISATION ===\n" + sb + "===================================\n\n";
    }

    /**
     * @param kpis            KPIs in this chunk (what the LLM must analyse)
     * @param totalKpiCount   total number of KPIs in the session (used only for globalSummary context)
     * @param allKpis         full list used to compute session-wide scores; falls back to kpis when null
     */
    private String buildPromptInternal(List<KpiCalculatedDTO> kpis, int totalKpiCount, List<KpiCalculatedDTO> allKpis, String contextBlock) {
        if (kpis == null || kpis.isEmpty()) {
            throw new IllegalArgumentException("At least one KPI is required to build a structured IA prompt.");
        }

        List<KpiCalculatedDTO> orderedKpis = kpis.stream()
                .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage() == null ? 0.0 : k.getVariationPercentage())))
                .collect(Collectors.toList());

        // Compute scores on the full session KPI list so globalSummary always reflects the real totals
        List<KpiCalculatedDTO> scoreSource = (allKpis != null && !allKpis.isEmpty()) ? allKpis : orderedKpis;
        ScoreSummary scores = computeScores(scoreSource);
        int reportedTotal = totalKpiCount > 0 ? totalKpiCount : orderedKpis.size();

        StringBuilder prompt = new StringBuilder();
        prompt.append("SYSTEM:\n");
        prompt.append(SYSTEM_PROMPT);
        if (contextBlock != null && !contextBlock.isBlank()) {
            prompt.append(contextBlock);
        }
        prompt.append(String.format("Nombre exact d'objets kpiInsights attendus : %d.\n\n", orderedKpis.size()));

        prompt.append("=== ANALYSE QHSE — DONNÉES À ANALYSER ===\n\n");

        // Resolve actual years from KPI data
        String periodeN1Str = scoreSource.stream()
                .map(KpiCalculatedDTO::getPeriodeN1).filter(p -> p != null && p > 0)
                .findFirst().map(String::valueOf).orElse("N-1");
        String periodeNStr = scoreSource.stream()
                .map(KpiCalculatedDTO::getPeriodeN).filter(p -> p != null && p > 0)
                .findFirst().map(String::valueOf).orElse("N");

        // Inject pre-computed scores as ground truth for globalSummary
        prompt.append("=== SCORES CALCULÉS (à citer tels quels dans globalSummary) ===\n");
        prompt.append(String.format("Période analysée : %s → %s\n", periodeN1Str, periodeNStr));
        prompt.append(String.format("Score global : %d/100 (%s) | KPIs totaux session : %d | Critiques : %d | Modérés : %d | Faibles/OK : %d\n",
                scores.globalScore, scores.globalLabel, reportedTotal,
                scores.critiques, scores.moderes, scores.faibledOk));
        for (ScoreSummary.CatScore cs : scores.categories) {
            prompt.append(String.format("  • %s : score %d/100 | %d critique(s), %d modéré(s), %d OK\n",
                    cs.libelle, cs.score, cs.critiques, cs.moderes, cs.ok));
        }
        if (scores.worstKpi != null) {
            prompt.append(String.format("KPI le plus dégradé : %s (%+.1f%%, %s→%s, %s)\n",
                    scores.worstKpi.getKpiName(), scores.worstVariation,
                    safeNumber(scores.worstKpi.getValeurN1()), safeNumber(scores.worstKpi.getValeurN()),
                    safeText(scores.worstKpi.getClassification())));
        }
        prompt.append("\n");

        // Names-only overview of all session KPIs so the LLM can write a globalSummary that covers
        // all KPIs, not just the chunk being analyzed. Full data is intentionally omitted to prevent
        // the LLM from generating kpiInsights for KPIs outside the chunk.
        if (allKpis != null && allKpis.size() > orderedKpis.size()) {
            prompt.append("=== PÉRIMÈTRE GLOBAL DE LA SESSION (référence pour globalSummary UNIQUEMENT) ===\n");
            prompt.append("⚠️ IMPORTANT : Cette liste sert UNIQUEMENT à rédiger le champ globalSummary.\n");
            prompt.append("Tu NE DOIS PAS produire de kpiInsight pour ces KPIs — seulement pour ceux de la section KPI CONTEXT ci-dessous.\n");
            prompt.append("Noms des KPIs de la session (tous) : ");
            List<String> allNames = allKpis.stream().map(k -> safeText(k.getKpiName())).collect(Collectors.toList());
            prompt.append(String.join(", ", allNames)).append("\n");
            prompt.append("Scores pré-calculés disponibles dans la section SCORES ci-dessus — utilise-les dans globalSummary.\n\n");
        }

        prompt.append("=== INSTRUCTIONS DE SORTIE ===\n");
        prompt.append("Renvoie un unique objet JSON valide respectant exactement le schéma ci-dessous.\n");
        prompt.append("Aucun texte avant ou après, aucun markdown, aucun backtick.\n\n");
        prompt.append("QUALITÉ REQUISE PAR SECTION :\n");
        prompt.append("• insight (par KPI) : 3 phrases minimum — (1) variation chiffrée + seuil franchi, (2) impact QHSE réel + norme ISO concernée, (3) interprétation causale préliminaire.\n");
        prompt.append("• probableCauses (par KPI) : 2 à 4 causes, chacune préfixée par sa catégorie Ishikawa entre crochets : [Homme], [Machine], [Méthode], [Milieu] ou [Matière].\n");
        prompt.append("• probableCauses (global) : 4 à 8 causes TRANSVERSALES couvrant plusieurs KPIs, classées Ishikawa, format '[Catégorie] Cause précise'.\n");
        prompt.append("• recommendations : 3 à 6 recommandations. Chaque title cite le(s) KPI(s) concerné(s). rationale mentionne la clause ISO applicable. expectedBenefit est chiffré si possible.\n");
        prompt.append("• actionPlan : 4 à 8 actions SMART ordonnées par priorité décroissante. ownerRole = rôle précis (pas 'Responsable' générique). dueHorizon = délai réaliste. riskIfNotDone = conséquence concrète.\n");
        prompt.append("• successMetric : indicateur mesurable avec valeur cible et horizon temporel (ex: 'KPI X < seuil Y dans les Z mois').\n");
        prompt.append("• note (par KPI) : observation experte complémentaire — tendance préoccupante, lien avec un autre KPI, recommandation de surveillance renforcée.\n");
        prompt.append("Les valeurs confidence sont des entiers entre 0 et 100.\n\n");

        prompt.append("OUTPUT SCHEMA:\n");
        prompt.append("{\n");
        prompt.append("  \"globalSummary\": string,  // OBLIGATOIRE : un seul paragraphe narratif fluide (5 à 8 phrases), rédigé comme un rapport d'expert QHSE senior destiné à la direction.\n");
        prompt.append("  // Doit couvrir dans l'ordre, sans sauts de ligne : (1) période et périmètre analysés, (2) score global chiffré et bilan par catégorie, (3) les 2-3 KPIs les plus critiques avec valeurs N-1→N et impact réel ISO/réglementaire, (4) les 2 actions prioritaires avec horizon temporel, (5) conclusion sur la trajectoire globale.\n");
        prompt.append("  // INTERDIT : sauts de ligne (\\n), puces, titres de section, texte générique sans valeurs chiffrées, répétitions.\n");
        prompt.append("  \"confidence\": {\n");
        prompt.append("    \"overall\": number,\n");
        prompt.append("    \"sections\": {\n");
        prompt.append("      \"summary\": number,\n");
        prompt.append("      \"probableCauses\": number,\n");
        prompt.append("      \"recommendations\": number,\n");
        prompt.append("      \"actionPlan\": number\n");
        prompt.append("    }\n");
        prompt.append("  },\n");
        prompt.append("  \"kpiInsights\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"kpiId\": number,\n");
        prompt.append("      \"kpiName\": string,\n");
        prompt.append("      \"confidence\": number,\n");
        prompt.append("      \"insight\": string,\n");
        prompt.append("      \"probableCauses\": [string],\n");
        prompt.append("      \"recommendations\": [string],\n");
        prompt.append("      \"actionImmediate\": string,\n");
        prompt.append("      \"urgency\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string,\n");
        prompt.append("      \"note\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"probableCauses\": [string],\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": string,\n");
        prompt.append("      \"rationale\": string,\n");
        prompt.append("      \"expectedBenefit\": string,\n");
        prompt.append("      \"urgency\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"actionPlan\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"action\": string,\n");
        prompt.append("      \"priority\": string,\n");
        prompt.append("      \"ownerRole\": string,\n");
        prompt.append("      \"dueHorizon\": string,\n");
        prompt.append("      \"successMetric\": string,\n");
        prompt.append("      \"riskIfNotDone\": string\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"traceability\": {\n");
        prompt.append("    \"generatedAt\": string\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append(FEW_SHOT_EXAMPLE);
        prompt.append("=== KPIs À ANALYSER ===\n");
        prompt.append("⚠️ RÈGLE ABSOLUE : le champ \"kpiId\" dans kpiInsights DOIT être exactement l'entier indiqué après KPI_ID=. Ne génère jamais 1, 2, 3... séquentiellement — utilise UNIQUEMENT les IDs fournis.\n\n");
        for (KpiCalculatedDTO kpi : orderedKpis) {
            String realId = safeId(kpi.getMatchedKpiId(), kpi.getKpiName());
            prompt.append(String.format("KPI_ID=%s | KPI: %s | Categorie: %s | Definition: %s\n",
                    realId,
                    safeText(kpi.getKpiName()), safeText(kpi.getCategorie()), safeText(kpi.getDefinition())));
            prompt.append(String.format("Seuils: Faible<%s, Modéré<%s, Critique<%s | N-1=%s | N=%s | Variation=%s%% | Niveau=%s\n",
                    safeNumber(kpi.getSeuilFaible()), safeNumber(kpi.getSeuilModere()), safeNumber(kpi.getSeuilCritique()),
                    safeNumber(kpi.getValeurN1()), safeNumber(kpi.getValeurN()), safeNumber(kpi.getVariationPercentage()),
                    safeText(kpi.getClassification())));
            prompt.append("\n");
        }

        prompt.append("Make sure the output is valid JSON.\n");
        prompt.append("If you cannot provide a structured section, return an empty array/object for it instead of text.\n");


        prompt.append("Remember: the structured JSON is the single source of truth. Do not output other prose.\n");

        return prompt.toString();
    }

    public String buildRetryPrompt(String originalPrompt, String validationErrors) {
        StringBuilder retryPrompt = new StringBuilder(originalPrompt);
        retryPrompt.append("\n\nATTENTION : ta réponse précédente était invalide.\n");
        retryPrompt.append("Erreurs de validation : ").append(safeText(validationErrors)).append(".\n");
        retryPrompt.append("Régénère le JSON avec exactement le même schéma et corrige toutes les erreurs ci-dessus.\n");
        retryPrompt.append("Réponds uniquement avec du JSON valide.\n");
        return retryPrompt.toString();
    }

    public String getPromptVersion() {
        return "structured-qhse-v8";
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String normalized = TextNormalizer.normalizeForPrompt(value);
        return promptSanitizer.sanitize(normalized, 1000);
    }

    private String safeNumber(Number value) {
        if (value == null) return "0";
        return value.toString();
    }

    private String safeId(Long id, String name) {
        if (id != null) {
            return String.valueOf(id);
        }
        String safeName = TextNormalizer.normalizeForSearch(name);
        return safeName.isBlank() ? "unknown" : safeName.replaceAll("\\s+", "_");
    }

    // ── Score pre-computation ─────────────────────────────────────────────────

    public static class ScoreSummary {
        public int globalScore;
        public String globalLabel;
        public int critiques;
        public int moderes;
        public int faibledOk;
        public KpiCalculatedDTO worstKpi;
        public double worstVariation;
        public List<CatScore> categories = new ArrayList<>();

        public static class CatScore {
            public String libelle;
            public int score;
            public int critiques;
            public int moderes;
            public int ok;
        }
    }

    public ScoreSummary computeScoresPublic(List<KpiCalculatedDTO> kpis) {
        return computeScores(kpis);
    }

    private ScoreSummary computeScores(List<KpiCalculatedDTO> kpis) {
        ScoreSummary s = new ScoreSummary();

        // Global counts
        for (KpiCalculatedDTO k : kpis) {
            String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
            if (cls.contains("CRITIQUE")) s.critiques++;
            else if (cls.contains("MODERE") || cls.contains("MODERÉ")) s.moderes++;
            else s.faibledOk++;
        }

        // Weighted global score (CRITIQUE counts double)
        double weightedSum = 0;
        double totalWeight = 0;
        for (KpiCalculatedDTO k : kpis) {
            String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
            double base   = cls.contains("CRITIQUE") ? 20 : cls.contains("MODERE") || cls.contains("MODERÉ") ? 60 : 100;
            double weight = cls.contains("CRITIQUE") ? 2 : 1;
            weightedSum += base * weight;
            totalWeight += weight;
        }
        s.globalScore = totalWeight > 0 ? (int) Math.round(weightedSum / totalWeight) : 0;
        if (s.globalScore >= 80)      s.globalLabel = "EXCELLENT";
        else if (s.globalScore >= 60) s.globalLabel = "SATISFAISANT";
        else if (s.globalScore >= 40) s.globalLabel = "À SURVEILLER";
        else                          s.globalLabel = "CRITIQUE";

        // Worst KPI by absolute variation
        kpis.stream()
            .filter(k -> k.getVariationPercentage() != null)
            .max(Comparator.comparingDouble(k -> Math.abs(((KpiCalculatedDTO) k).getVariationPercentage())))
            .ifPresent(k -> {
                s.worstKpi = k;
                s.worstVariation = k.getVariationPercentage();
            });

        // Per-category scores
        Map<String, List<KpiCalculatedDTO>> byCat = kpis.stream()
            .filter(k -> k.getCategorie() != null)
            .collect(Collectors.groupingBy(KpiCalculatedDTO::getCategorie, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<KpiCalculatedDTO>> entry : byCat.entrySet()) {
            ScoreSummary.CatScore cs = new ScoreSummary.CatScore();
            cs.libelle = entry.getKey();
            List<KpiCalculatedDTO> catKpis = entry.getValue();
            double cw = 0, cws = 0;
            for (KpiCalculatedDTO k : catKpis) {
                String cls = k.getClassification() == null ? "" : k.getClassification().toUpperCase();
                if (cls.contains("CRITIQUE")) { cs.critiques++; cws += 20 * 2; cw += 2; }
                else if (cls.contains("MODERE") || cls.contains("MODERÉ")) { cs.moderes++; cws += 60; cw += 1; }
                else { cs.ok++; cws += 100; cw += 1; }
            }
            cs.score = cw > 0 ? (int) Math.round(cws / cw) : 0;
            s.categories.add(cs);
        }
        return s;
    }
}
