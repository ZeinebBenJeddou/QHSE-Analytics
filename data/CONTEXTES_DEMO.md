# Profils Contexte QHSE — Fiches Démo

Ces 4 fiches correspondent aux 4 fichiers Excel de démo.
À saisir dans **Profil QHSE** (`/analyste/profil-qhse`) avant chaque import.

---

## FICHIER 1 — `QHSE_DEMO_2024_2025.xlsx`
**Scénario : Entreprise industrielle mixte — tous axes, tous cas de figure**

| Champ | Valeur à saisir |
|---|---|
| **Secteur d'activité** | `Industrie manufacturière` |
| **Taille du site** | `200-500` |
| **Certifications** | `ISO 9001, ISO 14001, ISO 45001` |
| **Objectifs QHSE** | `Réduire le taux de non-conformité de 50% d'ici fin 2025, atteindre zéro accident avec arrêt, maintenir la certification ISO 45001 lors du prochain audit de renouvellement` |
| **Réglementation applicable** | `Code du travail tunisien, Norme ISO 9001:2015, ISO 14001:2015, ISO 45001:2018` |
| **Contexte spécifique** | `Passage à une nouvelle ligne de production en janvier 2025. Turnover élevé sur les postes opérateurs (15%). Audit de surveillance ISO prévu en juin 2025.` |

**Pourquoi ce contexte :**
L'IA va ancrer ses recommandations sur la prochaine ligne de production, expliquer les NC par le turnover, et calibrer les actions sur l'audit de juin. Les 3 certifications activent les références ISO 9001/14001/45001 dans le prompt.

---

## FICHIER 2 — `QHSE_DEMO_BTP_2023_2024.xlsx`
**Scénario : Chantier BTP — sécurité dégradée, urgence terrain**

| Champ | Valeur à saisir |
|---|---|
| **Secteur d'activité** | `BTP / Génie civil` |
| **Taille du site** | `50-200` |
| **Certifications** | `ISO 45001, MASE` |
| **Objectifs QHSE** | `Ramener le TF1 sous 15 d'ici fin 2024, éliminer les accidents graves, atteindre 100% de port des EPI sur tous les postes, renforcer les visites sécurité managériales à 12 par an` |
| **Réglementation applicable** | `Code du travail tunisien, Décret 2006-1114 relatif aux chantiers, OPPBTP, normes NF EN 13374 pour les protections collectives` |
| **Contexte spécifique** | `Chantier de construction d'un complexe hôtelier — phase gros œuvre active. Effectif en pic saisonnier (+40% intérimaires depuis septembre). Accident grave survenu en octobre 2023 ayant déclenché une inspection de l'IIST.` |

**Pourquoi ce contexte :**
L'IA va relier la hausse du TF1 et du TG au pic d'intérimaires et à l'accident d'octobre. Les recommandations cibleront spécifiquement le gros œuvre, l'OPPBTP et le retour post-inspection.

---

## FICHIER 3 — `QHSE_DEMO_Agroalimentaire_2023_2024.xlsx`
**Scénario : Industrie agroalimentaire — plan d'action réussi, résultats positifs**

| Champ | Valeur à saisir |
|---|---|
| **Secteur d'activité** | `Industrie agroalimentaire` |
| **Taille du site** | `200-500` |
| **Certifications** | `ISO 9001, ISO 22000, IFS Food` |
| **Objectifs QHSE** | `Consolider les gains obtenus en 2024 (réduction NC de 75%), maintenir le taux de satisfaction client au-dessus de 90%, viser la certification BRC Grade A pour 2025` |
| **Réglementation applicable** | `Règlement CE 852/2004 sur l'hygiène alimentaire, Code du travail, normes HACCP, IFS Food v8` |
| **Contexte spécifique** | `Plan d'amélioration QHSE lancé en janvier 2023 sur 24 mois — bilan intermédiaire positif. Investissement de 800k DT dans l'automatisation des lignes en 2023. Préparation audit IFS Food prévu T1 2025.` |

**Pourquoi ce contexte :**
L'IA va valoriser les résultats du plan d'amélioration, ancrer les quelques MODERE restants (délai livraison, délai levée NC) sur les axes encore en cours d'optimisation, et orienter les recommandations vers la consolidation BRC/IFS.

---

## FICHIER 4 — `QHSE_DEMO_Crise_2023_2024.xlsx`
**Scénario : Entreprise en situation de crise — alerte maximale tous axes**

| Champ | Valeur à saisir |
|---|---|
| **Secteur d'activité** | `Industrie chimique / Pétrochimie` |
| **Taille du site** | `500+` |
| **Certifications** | `ISO 9001 (suspendue), ISO 14001` |
| **Objectifs QHSE** | `Rétablir la conformité ISO 9001 d'urgence, réduire le TF1 sous 30 avant fin T1 2024, éviter toute sanction réglementaire suite aux incidents environnementaux signalés en 2023` |
| **Réglementation applicable** | `Code du travail, Loi n°2001-14 sur les établissements dangereux, normes ICPE, arrêté préfectoral de mise en demeure du 15/11/2023` |
| **Contexte spécifique** | `Certification ISO 9001 suspendue suite à un audit de surveillance défavorable en septembre 2023. Deux incidents de déversement chimique signalés aux autorités. Direction générale sous pression des actionnaires. Comité de crise QHSE activé depuis novembre 2023.` |

**Pourquoi ce contexte :**
C'est le contexte le plus percutant pour la démo : l'IA va générer un plan d'action d'urgence structuré autour de la mise en demeure, relier chaque CRITIQUE à la situation de crise, et prioriser les actions selon la pression réglementaire. Le score global sera très bas (< 30/100) avec un globalSummary alarmant et des recommandations immédiates chiffrées.

---

## Procédure démo en 3 étapes

1. **Avant l'import** → aller dans `Profil QHSE` et saisir le contexte du fichier choisi → Enregistrer
2. **Import** → uploader le fichier Excel correspondant → confirmer les années
3. **Résultats** → Dashboard comparatif → Analyse IA → Export PDF

> Le contexte est automatiquement injecté dans le prompt IA à chaque import. Sans contexte, l'IA applique des benchmarks génériques — avec contexte, elle cite l'arrêté préfectoral, le pic d'intérimaires, ou l'audit IFS selon le scénario.
