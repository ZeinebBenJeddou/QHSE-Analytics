-- RAG Knowledge Base: ISO 9001 / ISO 14001 / ISO 45001 QHSE definitions
-- 55+ standard KPI definitions with thresholds and direction indicators
-- Uses ON CONFLICT DO NOTHING so re-runs are safe and existing entries are preserved.

INSERT INTO rag_knowledge (kpi_name, definition, category, direction, chunk_type) VALUES

-- ══════════════════════════════════════
-- SÉCURITÉ (S) — ISO 45001:2018
-- ══════════════════════════════════════
('Taux de Fréquence des Accidents (TF1)',
 'Nombre d''accidents avec arrêt de travail survenant pour 1 000 000 heures travaillées. '
 'Formule : (Nombre d''accidents avec arrêt × 1 000 000) / Nombre d''heures travaillées. '
 'Seuil ISO 45001 : viser TF1 < 5 dans les industries à risque modéré, < 2 dans les secteurs tertiaires. '
 'Une hausse du TF1 indique une dégradation des conditions de sécurité ou un sous-signalement antérieur.',
 'S', 'BAISSE', 'full'),

('Taux de Fréquence Toutes Accidents (TF2)',
 'Nombre total d''accidents du travail (avec et sans arrêt) pour 1 000 000 heures travaillées. '
 'Inclut les accidents bénins déclarés. Indicateur plus sensible que TF1 car capte les quasi-accidents. '
 'Un TF2 élevé avec un TF1 faible révèle souvent un problème de culture sécurité ou de sous-déclaration. '
 'Référence ISO 45001 clause 9.1.1 : surveiller les performances SST.',
 'S', 'BAISSE', 'full'),

('Taux de Gravité des Accidents (TG)',
 'Nombre de journées perdues pour incapacité temporaire pour 1 000 heures travaillées. '
 'Formule : (Journées perdues × 1 000) / Heures travaillées. '
 'Seuil cible : TG < 1 pour industries standard, TG < 0,5 pour secteur tertiaire. '
 'Complète TF1 : un TG élevé avec TF1 faible indique des accidents peu fréquents mais très graves.',
 'S', 'BAISSE', 'full'),

('Nombre d''accidents mortels',
 'Nombre de décès liés au travail sur la période. Indicateur de résultat critique. '
 'Tout accident mortel déclenche automatiquement une enquête approfondie selon ISO 45001 clause 10.2. '
 'Objectif absolu : zéro accident mortel. Tout écart doit faire l''objet d''une revue de direction immédiate.',
 'S', 'BAISSE', 'full'),

('Taux de Fréquence des Accidents de Trajet (TFAT)',
 'Accidents survenus sur le trajet domicile-travail pour 1 000 000 heures travaillées. '
 'Non inclus dans TF1 réglementaire mais traçable selon ISO 45001 clause 8.1.4. '
 'Un TFAT élevé peut révéler des problèmes d''aménagement du temps de travail ou de fatigue.',
 'S', 'BAISSE', 'full'),

('Taux de Presque-Accidents (Near Miss)',
 'Nombre de situations dangereuses signalées sans blessure, pour 100 salariés ou pour 1 000 heures. '
 'Un ratio élevé de near miss déclarés indique une bonne culture de signalement, pas un problème. '
 'ISO 45001 clause 10.2 exige l''investigation des incidents et des presque-accidents. '
 'Viser > 5 near miss déclarés par accident réel (pyramide de Bird).',
 'S', 'HAUSSE', 'full'),

('Taux d''Équipements de Protection Individuelle (EPI) conformes',
 'Pourcentage d''EPI vérifiés conformes lors des audits terrain. '
 'Formule : (EPI conformes / EPI contrôlés) × 100. '
 'Seuil minimum ISO 45001 clause 8.1.1 : 100 % des EPI obligatoires doivent être disponibles et conformes. '
 'En dessous de 95 %, risque d''exposition aux dangers inacceptable.',
 'S', 'HAUSSE', 'full'),

('Taux de Réalisation des Causticités / Exercices de Sécurité',
 'Pourcentage d''exercices de sécurité et simulations d''urgence réalisés vs planifiés. '
 'Formule : (Exercices réalisés / Exercices planifiés) × 100. '
 'ISO 45001 clause 8.2 impose des exercices périodiques pour tester les plans d''urgence. '
 'Objectif : taux de réalisation ≥ 90 %.',
 'S', 'HAUSSE', 'full'),

('Nombre de Situations de Travail Dangereuses (STD) identifiées',
 'Situations présentant un risque grave identifiées lors des inspections terrain. '
 'ISO 45001 clause 6.1.2 impose une identification systématique des dangers et évaluation des risques. '
 'Un nombre croissant de STD détectées est positif s''il reflète une meilleure vigilance terrain, '
 'négatif s''il traduit une dégradation réelle des conditions.',
 'S', 'BAISSE', 'full'),

('Taux de Conformité HSE aux Exigences Légales',
 'Pourcentage d''exigences légales HSE applicables respectées. '
 'Formule : (Exigences conformes / Exigences totales applicables) × 100. '
 'ISO 45001 clause 6.1.3 : obligation de veille réglementaire et conformité. '
 'Seuil : 100 % de conformité légale obligatoire. Tout écart constitue un risque juridique.',
 'S', 'HAUSSE', 'full'),

-- ══════════════════════════════════════
-- QUALITÉ (Q) — ISO 9001:2015
-- ══════════════════════════════════════
('Taux de Non-Conformités (NC)',
 'Nombre de non-conformités détectées en production ou service pour 100 unités produites ou contrôlées. '
 'ISO 9001 clause 8.7 impose la maîtrise des éléments de sortie non conformes. '
 'Seuil cible selon secteur : < 1 % en industrie générale, < 0,1 % en secteur médical ou aéronautique. '
 'Distinguer NC internes (détectées avant livraison) et NC externes (réclamations clients).',
 'Q', 'BAISSE', 'full'),

('Taux de Non-Conformités Internes',
 'Non-conformités détectées avant livraison / expédition, pour 1 000 unités fabriquées. '
 'Indicateur de l''efficacité des contrôles qualité internes. '
 'ISO 9001 clause 9.1 : surveiller les performances des processus. '
 'Une hausse soudaine révèle souvent un problème de matières premières, de machine ou de compétence.',
 'Q', 'BAISSE', 'full'),

('Taux de Réclamations Clients',
 'Nombre de réclamations reçues de clients pour 100 livraisons ou 1 000 unités vendues. '
 'ISO 9001 clause 9.1.2 : mesurer la satisfaction client. '
 'Seuil cible : < 0,5 % des livraisons génèrent une réclamation. '
 'Analyser par catégorie : délai, qualité produit, facturation, service après-vente.',
 'Q', 'BAISSE', 'full'),

('Indice de Satisfaction Client (ISC)',
 'Score moyen de satisfaction client mesuré par enquête (échelle 0-100 ou NPS). '
 'ISO 9001 clause 9.1.2 : la satisfaction client est une exigence fondamentale. '
 'Seuil cible : ISC ≥ 80 / 100 ou NPS ≥ +30. '
 'Mesurer à chaud (post-livraison) et à froid (relation long terme). '
 'Corrélé aux indicateurs de fidélisation et de part de marché.',
 'Q', 'HAUSSE', 'full'),

('Taux de Produits / Services Conformes du Premier Coup (First Pass Yield)',
 'Pourcentage d''unités conformes sans retouche ni reprise lors du premier passage en contrôle. '
 'Formule : (Unités conformes sans retouche / Unités produites totales) × 100. '
 'Objectif lean quality : FPY ≥ 95 %. En dessous de 90 %, les coûts de non-qualité sont significatifs. '
 'ISO 9001 clause 8.5 : maîtrise de la production et de la prestation de service.',
 'Q', 'HAUSSE', 'full'),

('Taux de Retours Clients',
 'Pourcentage de produits retournés par les clients sur les livraisons de la période. '
 'Formule : (Quantités retournées / Quantités livrées) × 100. '
 'Seuil critique : > 2 % de retours indique un problème systémique. '
 'ISO 9001 clause 8.7.1 : traitement des non-conformités détectées après livraison.',
 'Q', 'BAISSE', 'full'),

('Délai de Traitement des Non-Conformités',
 'Délai moyen en jours entre la détection d''une NC et la clôture de l''action corrective. '
 'ISO 9001 clause 10.2 impose des actions correctives efficaces et documentées. '
 'Seuil cible : < 30 jours pour NC standard, < 5 jours pour NC critiques. '
 'Un délai trop long augmente le risque de récidive et de propagation du problème.',
 'Q', 'BAISSE', 'full'),

('Taux de Réalisation du Plan d''Audit Qualité',
 'Pourcentage d''audits qualité internes réalisés par rapport au programme d''audit annuel. '
 'ISO 9001 clause 9.2 : les audits internes sont une exigence système. '
 'Seuil minimum : ≥ 90 % des audits planifiés réalisés dans les délais. '
 'Un taux faible révèle des problèmes de ressources ou de priorités managériales.',
 'Q', 'HAUSSE', 'full'),

('Taux d''Actions Correctives Efficaces',
 'Pourcentage d''actions correctives vérifiées efficaces lors du suivi / re-audit. '
 'ISO 9001 clause 10.2.1 : vérification de l''efficacité des actions correctives. '
 'Seuil cible : ≥ 85 % d''efficacité à 90 jours. '
 'Un faible taux indique une analyse des causes insuffisante ou des actions superficielles.',
 'Q', 'HAUSSE', 'full'),

('Coût de Non-Qualité (CNQ)',
 'Coût total des non-conformités : rebuts, retouches, garanties, réclamations, retours. '
 'Exprimé en valeur absolue ou en % du chiffre d''affaires. '
 'Seuil ISO 9001 de référence : CNQ < 5 % du CA est considéré acceptable ; < 2 % est l''objectif lean. '
 'Se décompose en coûts de défaillance internes + externes et coûts de prévention.',
 'Q', 'BAISSE', 'full'),

('Nombre d''Audits de Fournisseurs Réalisés',
 'Nombre d''audits de qualification et de surveillance réalisés chez les fournisseurs stratégiques. '
 'ISO 9001 clause 8.4 : maîtrise des processus, produits et services fournis de l''extérieur. '
 'Objectif : 100 % des fournisseurs critiques audités selon le plan annuel. '
 'Un audit fournisseur évite la propagation de NC externes détectées trop tard.',
 'Q', 'HAUSSE', 'full'),

('Taux de Conformité Documentaire',
 'Pourcentage de documents qualité (procédures, instructions, enregistrements) à jour et approuvés. '
 'ISO 9001 clause 7.5 : informations documentées maîtrisées. '
 'Seuil cible : 100 % des documents critiques à jour. '
 'Les documents obsolètes exposent l''organisation à des pratiques non conformes.',
 'Q', 'HAUSSE', 'full'),

-- ══════════════════════════════════════
-- ENVIRONNEMENT (E) — ISO 14001:2015
-- ══════════════════════════════════════
('Consommation d''Énergie Totale (kWh)',
 'Consommation totale d''énergie (électricité + gaz + fioul + autres) en kWh sur la période. '
 'ISO 14001 clause 6.1.2 : identifier les aspects environnementaux significatifs dont la consommation énergétique. '
 'Normaliser par unité produite pour isoler l''effet volume. '
 'Seuil européen RE2020 et objectif Accord de Paris : réduire l''intensité énergétique de 3 % / an.',
 'E', 'BAISSE', 'full'),

('Intensité Énergétique (kWh/unité)',
 'Consommation d''énergie rapportée à l''unité de production (kWh par tonne, m², pièce, etc.). '
 'Permet de séparer l''amélioration de l''efficacité énergétique de l''effet volume. '
 'ISO 14001 clause 9.1.1 + ISO 50001 : surveiller la performance énergétique. '
 'Objectif : réduction continue de 2 à 5 % par an selon engagement RSE de l''organisation.',
 'E', 'BAISSE', 'full'),

('Émissions de CO2 (Scope 1 + 2)',
 'Émissions directes (Scope 1 : combustion, procédés) et indirectes liées à l''énergie achetée (Scope 2) '
 'en tonnes équivalent CO2 (tCO2e). '
 'GHG Protocol + ISO 14064 : base de calcul reconnue internationalement. '
 'Objectif Accord de Paris : neutralité carbone avant 2050. '
 'Indicateur clé des rapports extra-financiers (CSRD, DPEF). '
 'Une hausse doit être corrélée à la production pour identifier si c''est structurel ou conjoncturel.',
 'E', 'BAISSE', 'full'),

('Volume de Déchets Produits (tonnes)',
 'Volume total de déchets générés sur la période (Déchets Industriels Banals + Dangereux). '
 'ISO 14001 clause 8.2 : préparation et réponse aux situations d''urgence environnementale. '
 'Objectif hiérarchie des déchets EU : réduire à la source en priorité. '
 'Seuil : toute hausse > 10 % doit déclencher une analyse des causes.',
 'E', 'BAISSE', 'full'),

('Taux de Valorisation des Déchets (%)',
 'Pourcentage de déchets valorisés (recyclage, réemploi, valorisation énergétique) '
 'sur le total des déchets produits. '
 'Formule : (Déchets valorisés / Déchets totaux) × 100. '
 'Objectif règlement européen : ≥ 55 % de recyclage des déchets municipaux d''ici 2025, '
 'objectifs sectoriels industriels souvent supérieurs à 80 %. '
 'ISO 14001 clause 8.1 : maîtrise opérationnelle des aspects environnementaux.',
 'E', 'HAUSSE', 'full'),

('Consommation d''Eau (m³)',
 'Volume d''eau consommé (réseau + captage propre) en m³ sur la période. '
 'ISO 14001 clause 6.1.2 : consommation d''eau identifiée comme aspect environnemental significatif. '
 'Normaliser par unité produite pour calculer l''intensité eau. '
 'Dans les zones de stress hydrique, toute hausse est critique. '
 'Objectif : réduire l''intensité eau de 2 % par an.',
 'E', 'BAISSE', 'full'),

('Nombre de Déversements / Incidents Environnementaux',
 'Nombre de déversements accidentels, fuites ou pollutions survenant sur la période. '
 'ISO 14001 clause 8.2 : préparation et réponse aux urgences environnementales. '
 'Objectif absolu : zéro déversement. Tout incident > seuil réglementaire déclenche déclaration DREAL. '
 'Un nombre croissant d''incidents mineurs signale une dégradation du management environnemental.',
 'E', 'BAISSE', 'full'),

('Taux de Conformité Environnementale Légale',
 'Pourcentage d''obligations réglementaires environnementales applicables respectées. '
 'ISO 14001 clause 9.1.2 : évaluation de la conformité aux exigences légales et autres exigences. '
 'Seuil : 100 % de conformité obligatoire. '
 'Tout écart expose à des sanctions administratives (ICPE) ou pénales.',
 'E', 'HAUSSE', 'full'),

('Taux de Réalisation des Objectifs Environnementaux',
 'Pourcentage des objectifs environnementaux annuels atteints. '
 'ISO 14001 clause 6.2 : objectifs environnementaux et planification des actions. '
 'Formule : (Objectifs atteints / Objectifs planifiés) × 100. '
 'Seuil cible : ≥ 80 % des objectifs atteints. '
 'Les objectifs non atteints doivent être reconduits avec analyse des obstacles.',
 'E', 'HAUSSE', 'full'),

('Émissions de Composés Organiques Volatils (COV)',
 'Masse totale de COV émis en kg ou tonnes par an. '
 'Réglementé par la Directive COV 1999/13/CE pour les installations industrielles. '
 'ISO 14001 clause 6.1.2 : émissions atmosphériques aspect environnemental significatif. '
 'Seuil selon seuils d''application de la directive et autorisations préfectorales ICPE. '
 'Une hausse peut indiquer des fuites d''équipements ou des changements de formulation produit.',
 'E', 'BAISSE', 'full'),

('Consommation de Matières Premières Renouvelables (%)',
 'Part des matières premières renouvelables ou biosourcées dans les approvisionnements totaux. '
 'Indicateur de circularité et de réduction de la dépendance aux ressources fossiles. '
 'ISO 14001 clause 8.1 + objectifs RSE : favoriser l''économie circulaire. '
 'Objectif : augmenter progressivement selon engagement RSE de l''organisation.',
 'E', 'HAUSSE', 'full'),

('Nombre d''Espèces / Surfaces Protégées impactées',
 'Indicateur de biodiversité : surfaces naturelles protégées ou sensibles impactées par l''activité. '
 'ISO 14001 clause 6.1.2 : impacts sur la biodiversité parmi les aspects environnementaux. '
 'Objectif Kunming-Montréal : réduire à zéro les impacts nets sur la biodiversité. '
 'Tout impact sur zone Natura 2000 déclenche une étude d''incidence obligatoire.',
 'E', 'BAISSE', 'full'),

-- ══════════════════════════════════════
-- HYGIÈNE / SANTÉ AU TRAVAIL (H) — ISO 45001
-- ══════════════════════════════════════
('Taux d''Absentéisme',
 'Pourcentage de jours d''absence (toutes causes) par rapport aux jours théoriquement travaillés. '
 'Formule : (Jours d''absence / Jours théoriques) × 100. '
 'Seuil alerte : > 5 % est considéré élevé en France (moyenne nationale ≈ 4–5 %). '
 'ISO 45001 clause 9.1.1 : surveiller l''état de santé et le bien-être des travailleurs. '
 'Distinguer absentéisme court (< 3 jours) vs long terme pour des actions ciblées.',
 'H', 'BAISSE', 'full'),

('Taux de Maladies Professionnelles (MP) reconnues',
 'Nombre de maladies professionnelles officiellement reconnues pour 1 000 salariés. '
 'ISO 45001 clause 6.1.2 : les risques de santé au travail doivent être identifiés et maîtrisés. '
 'Une hausse des MP reconnues peut indiquer des expositions chroniques sous-estimées (TMS, bruit, produits chimiques). '
 'Délai typique de latence entre exposition et reconnaissance : 5 à 20 ans.',
 'H', 'BAISSE', 'full'),

('Taux de Restrictions Médicales (Restrictions d''aptitude)',
 'Pourcentage de salariés avec des restrictions médicales sur leurs postes de travail. '
 'Un taux croissant peut révéler des postes non ergonomiques ou des expositions excessives. '
 'ISO 45001 clause 8.1.4 : adapter les postes aux capacités des travailleurs. '
 'Objectif : réduire les restrictions par amélioration ergonomique des postes.',
 'H', 'BAISSE', 'full'),

('Taux de Réalisation des Visites Médicales Périodiques',
 'Pourcentage de salariés ayant passé leur visite médicale périodique dans les délais réglementaires. '
 'ISO 45001 clause 8.1.2 : gestion de la santé des travailleurs. '
 'Seuil réglementaire France : 100 % des salariés suivis selon périodicité fixée par le médecin du travail. '
 'Un taux < 90 % révèle des problèmes organisationnels ou des refus de salariés à traiter.',
 'H', 'HAUSSE', 'full'),

('Indice de Pénibilité Moyen',
 'Score moyen d''exposition aux facteurs de pénibilité (bruit, TMS, travail de nuit, etc.) '
 'selon le référentiel C2P (Compte Professionnel de Prévention). '
 'ISO 45001 clause 6.1.2 : identification et évaluation des risques professionnels. '
 'Réduction de la pénibilité = réduction des coûts long terme (MP, invalidité, retraite anticipée).',
 'H', 'BAISSE', 'full'),

('Nombre de Signalements RPS (Risques Psycho-Sociaux)',
 'Nombre de situations de RPS signalées (harcèlement, burn-out, conflits interpersonnels). '
 'ISO 45001 clause 6.1.2 : les RPS sont des risques SST à identifier et prévenir. '
 'Un faible nombre peut indiquer une sous-déclaration culturelle, pas l''absence de problème. '
 'Un outil d''évaluation anonyme (KARASEK, COPSOQ) est recommandé.',
 'H', 'BAISSE', 'full'),

('Taux de Couverture des Analyses de Poste Ergonomiques',
 'Pourcentage de postes à risque TMS (Troubles Musculo-Squelettiques) ayant fait l''objet d''une analyse ergonomique. '
 'Formule : (Postes analysés / Postes identifiés à risque) × 100. '
 'ISO 45001 clause 6.1.2 + recommandations INRS. '
 'Seuil cible : 100 % des postes à risque TMS analysés dans les 12 mois suivant l''identification.',
 'H', 'HAUSSE', 'full'),

('Taux de Formation Sécurité et Hygiène',
 'Pourcentage de salariés ayant suivi les formations sécurité / hygiène obligatoires ou planifiées. '
 'ISO 45001 clause 7.2 : compétence et formation SST. '
 'Seuil minimum réglementaire : 100 % des formations initiales à l''embauche et au changement de poste. '
 'Un suivi annuel des habilitations à jour est obligatoire pour les postes à risque.',
 'H', 'HAUSSE', 'full'),

('Taux de Réalisation des Évaluations des Risques (DUERP)',
 'Pourcentage d''unités de travail couvertes par un DUERP (Document Unique d''Évaluation des Risques Professionnels) à jour. '
 'Obligation légale France (décret 2001-1016) + ISO 45001 clause 6.1.2. '
 'Le DUERP doit être mis à jour annuellement et à chaque changement significatif. '
 'Seuil : 100 % de couverture obligatoire.',
 'H', 'HAUSSE', 'full'),

('Taux d''Actions Préventives SST Réalisées',
 'Pourcentage d''actions préventives SST issues du DUERP ou des plans d''action réalisées vs planifiées. '
 'ISO 45001 clause 6.1 : actions pour traiter les risques et opportunités. '
 'Seuil cible : ≥ 85 % des actions préventives planifiées réalisées dans les délais. '
 'Un faible taux de réalisation maintient les travailleurs exposés à des risques identifiés.',
 'H', 'HAUSSE', 'full'),

-- ══════════════════════════════════════
-- KPIs TRANSVERSAUX QHSE
-- ══════════════════════════════════════
('Taux de Réalisation du Programme de Management QHSE',
 'Pourcentage d''actions du programme de management QHSE annuel réalisées. '
 'Référence multi-norme : ISO 9001 clause 6.2, ISO 14001 clause 6.2, ISO 45001 clause 6.2. '
 'Seuil cible : ≥ 80 % des actions réalisées dans les délais. '
 'Indicateur de pilotage du SMI (Système de Management Intégré).',
 'Q', 'HAUSSE', 'full'),

('Taux de Certification QHSE maintenu',
 'Statut du maintien des certifications ISO 9001, ISO 14001, ISO 45001. '
 'Valeur binaire ou taux de recommandations d''audit de certification sans écart majeur. '
 'Une non-reconduction de certification est un signal critique de défaillance du SMI. '
 'Préparer la revue de direction selon clause 9.3 des trois normes.',
 'Q', 'HAUSSE', 'full'),

('Nombre d''Incidents de Sécurité des Données (SI) liés aux opérations QHSE',
 'Nombre d''incidents affectant la confidentialité ou l''intégrité des données QHSE. '
 'ISO 27001 (si applicable) + RGPD : les données de santé et sécurité sont des données sensibles. '
 'Objectif : zéro incident de sécurité des données. '
 'Les données de surveillance HSE (santé au travail) nécessitent une protection renforcée.',
 'Q', 'BAISSE', 'full'),

('Taux de Sensibilisation QHSE des Nouveaux Embauchés',
 'Pourcentage de nouveaux salariés ayant reçu une sensibilisation QHSE complète dans les 30 jours suivant l''embauche. '
 'ISO 45001 clause 7.3 + ISO 14001 clause 7.3 : sensibilisation des personnes. '
 'Seuil cible : 100 % des nouveaux embauchés sensibilisés avant prise de poste effective. '
 'Une formation QHSE initiale réduit significativement les accidents en période d''essai.',
 'H', 'HAUSSE', 'full'),

('Indice de Maturité du Système de Management QHSE',
 'Score d''auto-évaluation ou d''audit de maturité du SMI QHSE sur une échelle de 1 à 5. '
 'Niveau 1 : réactif. Niveau 3 : défini et documenté. Niveau 5 : optimisé en amélioration continue. '
 'ISO 9004 fournit un guide d''auto-évaluation complémentaire à ISO 9001. '
 'Objectif : progression d''au moins 0,5 point par exercice biennal.',
 'Q', 'HAUSSE', 'full'),

('Taux d''Actions d''Amélioration Continue Clôturées',
 'Pourcentage d''actions d''amélioration (issues d''audits, d''analyses de risques, de revues de direction) '
 'clôturées sur la période. '
 'ISO 9001 / 14001 / 45001 clause 10.3 : amélioration continue. '
 'Seuil cible : ≥ 75 % des actions clôturées dans les délais définis. '
 'Mesurer aussi l''efficacité des actions (pas seulement la clôture administrative).',
 'Q', 'HAUSSE', 'full'),

('Taux de Participation aux Causeries Sécurité',
 'Pourcentage de salariés ayant participé à au moins une causerie sécurité sur la période. '
 'Les causeries sécurité (toolbox meetings) sont un outil clé de culture sécurité. '
 'ISO 45001 clause 7.4 : communication et participation des travailleurs. '
 'Seuil cible : ≥ 90 % de participation mensuelle. '
 'Un taux faible révèle un engagement managérial insuffisant ou un planning inadapté.',
 'S', 'HAUSSE', 'full'),

('Taux d''Observations Terrain de Sécurité (OTS)',
 'Nombre d''observations terrain de sécurité (comportementales ou physiques) par rapport à l''objectif périodique. '
 'Les OTS (aussi appelées VSST — Visites Sécurité Sans Titre) mesurent l''engagement terrain des managers. '
 'ISO 45001 clause 5.1 : leadership et engagement en matière de SST. '
 'Objectif : 1 OTS / manager / semaine dans les secteurs à risque. '
 'Un programme OTS actif réduit le TF1 en moyenne de 20 à 30 % sur 3 ans.',
 'S', 'HAUSSE', 'full'),

('Coût Global de la Sinistralité (AT+MP)',
 'Coût total des accidents du travail et maladies professionnelles (charges patronales AT/MP, '
 'coûts directs de remplacement, coûts d''investigation). '
 'Exprimé en euros absolus ou en % de la masse salariale. '
 'ISO 45001 clause 9.1 : surveiller la performance SST y compris les coûts. '
 'Référence INRS : chaque accident avec arrêt coûte en moyenne 25 000 € directs + indirects. '
 'Indicateur à présenter en revue de direction pour justifier les investissements prévention.',
 'S', 'BAISSE', 'full'),

('Taux de Déclaration des Accidents (Taux de sous-déclaration estimé)',
 'Ratio entre accidents déclarés et accidents estimés (via enquêtes anonymes). '
 'Un taux de déclaration > 85 % indique une bonne culture de transparence. '
 'ISO 45001 clause 10.2 : les incidents doivent être signalés, enquêtés et enregistrés. '
 'La peur des sanctions est la principale barrière à la déclaration. '
 'Un programme non-punitif de déclaration augmente significativement ce taux.',
 'S', 'HAUSSE', 'full'),

('Taux de Dépassements des Valeurs Limites d''Exposition (VLE)',
 'Pourcentage de mesures d''exposition (bruit, poussières, COV, etc.) dépassant les VLE réglementaires. '
 'France : valeurs limites définies par le Code du Travail + directives européennes. '
 'ISO 45001 clause 8.1.3 : gestion du changement pour maîtriser les expositions. '
 'Objectif : 0 % de dépassements des VLE. Tout dépassement déclenche une action corrective immédiate.',
 'H', 'BAISSE', 'full')

ON CONFLICT (kpi_name, chunk_type) DO NOTHING;
