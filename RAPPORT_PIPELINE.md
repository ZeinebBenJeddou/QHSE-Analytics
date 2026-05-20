# Rapport Technique — Pipeline QHSE Analytics

> **Périmètre** : Import → Traitement → Calcul → Classification → Module IA (RAG, Few-shot, Prompt engineering)
> **Date** : 2026-05-20

---

## Table des matières

1. [Vue d'ensemble du pipeline](#1-vue-densemble-du-pipeline)
2. [Import — Mode 1 : Fichier unique](#2-import--mode-1--fichier-unique)
3. [Import — Mode 2 : Double fichier (Dual)](#3-import--mode-2--double-fichier-dual)
4. [Traitement — ExtractionAgent](#4-traitement--extractionagent)
5. [Traitement — CleaningAgent](#5-traitement--cleaningagent)
6. [Calcul — ComparativeCalculator](#6-calcul--comparativecalculator)
7. [Calcul — CalculationAgent & Score par catégorie](#7-calcul--calculationagent--score-par-catégorie)
8. [Classification — ClassificationEngine](#8-classification--classificationengine)
9. [Détection des risques — RiskDetectionAgent](#9-détection-des-risques--riskdetectionagent)
10. [Module IA — Architecture LLM](#10-module-ia--architecture-llm)
11. [Module IA — RAG (Retrieval-Augmented Generation)](#11-module-ia--rag-retrieval-augmented-generation)
12. [Module IA — Prompt Engineering & Few-Shot](#12-module-ia--prompt-engineering--few-shot)
13. [Module IA — AnalysisAgent (orchestration)](#13-module-ia--analysisagent-orchestration)
14. [Persistance & Machine à états](#14-persistance--machine-à-états)
15. [Schéma base de données](#15-schéma-base-de-données)

---

## 1. Vue d'ensemble du pipeline

```
Fichier Excel (upload)
        │
        ▼
┌───────────────────┐
│  ImportProcessing │  ← contrôleur REST
│  Controller       │
└────────┬──────────┘
         │
    ┌────▼────┐
    │ Mode 1  │  POST /api/import/manual          (1 fichier, colonnes N et N-1 dans le même fichier)
    │ Mode 2  │  POST /api/import/manual/dual      (2 fichiers séparés : année N et année N-1)
    └────┬────┘
         │
    ┌────▼──────────┐
    │ ExtractionAgent│  → détecte la feuille, l'en-tête, mappe les colonnes
    └────┬───────────┘
         │  List<KpiRawDataDTO>
    ┌────▼──────────┐
    │ CleaningAgent  │  → normalise, déduplique, détecte outliers
    └────┬───────────┘
         │  List<KpiRawDataDTO> nettoyés
    ┌────▼────────────┐
    │ CalculationAgent│  → matching KPI, calcul variation, score
    └────┬────────────┘
         │  List<KpiCalculatedDTO>
    ┌────▼────────────────┐
    │ ClassificationEngine │  → 6 niveaux (EXCELLENT → CRITIQUE)
    └────┬────────────────┘
         │
    ┌────▼──────────────┐
    │ RiskDetectionAgent │  → filtre les KPIs critiques
    └────┬───────────────┘
         │
    ┌────▼────────────────────────────────────┐
    │ AnalyseIaService (async, après commit)  │
    │   → RAG search → LLM (Groq / Gemini)   │
    └─────────────────────────────────────────┘
```

---

## 2. Import — Mode 1 : Fichier unique

### Concept théorique

L'import en mode fichier unique correspond au cas le plus courant : l'utilisateur dispose d'un tableau Excel qui contient **dans le même fichier** les valeurs de l'année N et de l'année N-1, sur des colonnes séparées. C'est le mode standard d'un tableau de bord QHSE périodique.

### Implémentation

**Fichier** : `QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/controller/ImportProcessingController.java`

**Endpoint** :
```
POST /api/import/manual
Content-Type: multipart/form-data
Params  : file (MultipartFile), mapping (JSON optionnel), periodeN (int), periodeN1 (int)
```

**Contraintes techniques** :
- Format accepté : `.xlsx` et `.xls` uniquement (validé par `ExcelFileValidator`)
- Taille maximale : **15 MB** (configuré dans `application.properties`)
- Bibliothèque de lecture : **Apache POI** (`WorkbookFactory.create(inputStream)`)
- Les formules Excel sont évaluées via `FormulaEvaluator` avant extraction

**Flux côté service** :
1. `ImportProcessingService.processImport(file, mapping, periodeN, periodeN1, userId)`
2. Création de la session `ImportSession` avec statut `INITIAL`
3. Appel chaîné : Extraction → Nettoyage → Calcul → Sauvegarde
4. Déclenchement asynchrone de l'analyse IA après commit

**Options supplémentaires disponibles pour ce mode** :
- `POST /api/import/manual/preview` — extrait sans persister, retourne un aperçu
- `POST /api/import/manual/profile` — profile les colonnes sans calculer

---

## 3. Import — Mode 2 : Double fichier (Dual)

### Concept théorique

Le mode dual sépare les deux périodes dans deux fichiers distincts. Ce cas d'usage survient quand les données N et N-1 proviennent de sources différentes (exports de deux exercices), ou quand le tableau de bord de chaque année est exporté indépendamment. Le système doit alors **fusionner** les deux fichiers par correspondance de noms de KPIs.

### Implémentation

**Endpoint** :
```
POST /api/import/manual/dual
Params : fileN (MultipartFile), fileN1 (MultipartFile), periodeN, periodeN1, userId
```

**Différence clé avec le Mode 1** :

Dans le mode dual, `ExtractionAgent` est appelé **deux fois** (une fois par fichier). Chaque appel produit une `List<KpiRawDataDTO>` avec uniquement les valeurs de sa période. Ensuite, une étape de **fusion par nom de KPI** (join côté service) assemble les deux listes en une seule, en associant valeurN et valeurN1 pour chaque indicateur.

```java
// Principe de fusion (simplifié)
Map<String, KpiRawDataDTO> mapN  = extractFromFile(fileN,  mappingN,  "N");
Map<String, KpiRawDataDTO> mapN1 = extractFromFile(fileN1, mappingN1, "N-1");

// Pour chaque KPI présent dans N, on cherche son équivalent dans N-1
for (KpiRawDataDTO kpiN : mapN.values()) {
    KpiRawDataDTO kpiN1 = mapN1.get(normalize(kpiN.getNomKpi()));
    merged.add(merge(kpiN, kpiN1));  // kpiN1 peut être null → valeurN1 = null
}
```

La correspondance utilise le même algorithme de normalisation (suppression des accents, lowercase) que dans `CleaningAgent`.

---

## 4. Traitement — ExtractionAgent

### Concept théorique

L'extraction est la phase de **lecture brute** du fichier Excel. Le défi principal est l'hétérogénéité des fichiers clients : certains ont des en-têtes à la ligne 1, d'autres à la ligne 3, certains appellent la colonne KPI « Indicateur », d'autres « Libellé ». L'agent doit être robuste à ces variations sans nécessiter de configuration manuelle systématique.

### Implémentation

**Fichier** : [ExtractionAgent.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/ExtractionAgent.java)

#### 4.1 Sélection de la feuille de données

```java
// Sélection heuristique : cherche la feuille dont le nom contient un mot-clé QHSE
Optional<Sheet> selectDataSheet(Workbook workbook, ...) {
    for (Sheet sheet : workbook) {
        String name = normalize(sheet.getSheetName());
        if (containsAny(name, "donn", "data", "kpi", "qhse")) return Optional.of(sheet);
    }
    return Optional.of(workbook.getSheetAt(0)); // fallback : première feuille
}
```

#### 4.2 Détection de la ligne d'en-tête

L'agent scanne les premières lignes du fichier jusqu'à trouver une ligne qui contient au moins un synonyme de colonne KPI. Si un `mapping` personnalisé est fourni par l'utilisateur (via l'interface de mapping de colonnes), il est utilisé directement en mode `CUSTOM`.

#### 4.3 Synonymes de colonnes

| Champ cible    | Synonymes acceptés                                                              |
|----------------|---------------------------------------------------------------------------------|
| Nom KPI        | `kpi`, `indicateur`, `libelle`, `nom indicateur`, `metric`, `designation`       |
| Valeur N       | `n`, `annee n`, `valeur n`, `current`, `valeur actuelle`, `n courant`           |
| Valeur N-1     | `n-1`, `annee n-1`, `valeur n-1`, `previous`, `n1`, `n moins 1`                |
| Catégorie      | `categorie`, `domaine`, `famille`, `axe`, `qhse`, `type`                        |
| Unité          | `unite`, `unit`, `mesure`, `unites`                                             |

#### 4.4 Lecture et parsing des cellules

Apache POI fournit un `DataFormatter` qui convertit toute cellule en `String`, y compris les formules (grâce au `FormulaEvaluator`). Les valeurs numériques ambiguës (ex. `"12 accidents"`, `"2/3"`) sont détectées via regex :

```java
// Détecte les valeurs qui mélangent chiffres et texte
private static final Pattern AMBIGUOUS_PATTERN = Pattern.compile("^-?\\d[\\d\\s.,]*[a-zA-ZÀ-ÿ%°/]+.*$");

// Détecte les fractions
private static final Pattern FRACTION_PATTERN = Pattern.compile("^\\d+/\\d+$");
```

Ces cas sont marqués comme `WARNING` dans la liste des `ImportIssue`, mais n'empêchent pas le traitement.

#### 4.5 Sortie

Chaque ligne du fichier devient un objet `KpiRawDataDTO` contenant :
- `nomKpi`, `categorieRaw`, `uniteRaw`
- `rawValueN`, `rawValueN1` (String avant parsing numérique)
- `valeurN`, `valeurN1` (Double après tentative de conversion)
- `issues` : liste des avertissements détectés sur cette ligne

---

## 5. Traitement — CleaningAgent

### Concept théorique

Le nettoyage normalise les données brutes pour garantir la cohérence avant les calculs. Deux problèmes sont traités : (1) la **variabilité linguistique** (accents, casse, espaces superflus) et (2) la **qualité des données** (doublons, valeurs aberrantes).

### Implémentation

**Fichier** : [CleaningAgent.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/CleaningAgent.java)

#### 5.1 Normalisation Unicode

```java
// Normalisation NFD : décompose les caractères accentués
// puis supprime les diacritiques (classe \p{M})
String normalize(String input) {
    return Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                     .replaceAll("\\p{M}", "")
                     .toLowerCase();
}
```

Exemple : `"Taux d'Accidents"` → `"taux d'accidents"` (sans accent)

#### 5.2 Détection des outliers

Un KPI est marqué outlier si la variation entre N-1 et N dépasse **500%** en valeur absolue. Seuil configurable dans `application.properties` :

```properties
app.import.outlier.threshold=500
```

Ce KPI reçoit un `ImportIssue` de type `WARNING` mais n'est pas exclu du calcul.

#### 5.3 Déduplication

**Clé de déduplication** : `normalize(nomKpi) + "|" + normalize(categorie) + "|" + valeurN + "|" + valeurN1`

- Si deux lignes ont la même clé → **doublon exact** : la deuxième est supprimée
- Si deux lignes ont le même nom KPI mais des valeurs différentes → **conflit** : les deux sont gardées avec un flag `DUPLICATE_CONFLICT`

---

## 6. Calcul — ComparativeCalculator

### Concept théorique

La comparaison entre deux périodes est le cœur analytique du pipeline. Pour que cette comparaison soit **interprétable**, il faut connaître la **direction** du KPI : une hausse de `Taux de Fréquence des Accidents` est mauvaise, alors qu'une hausse de `Taux de Conformité` est bonne. Le calculateur détermine cette direction automatiquement à partir du nom et de la catégorie du KPI.

### Implémentation

**Fichier** : [ComparativeCalculator.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/ComparativeCalculator.java)

#### 6.1 Calcul de la variation

```java
// Écart absolu
double absoluteGap = valN - valN1;

// Écart relatif (en %)
double relativePercentage = ((valN - valN1) / Math.abs(valN1)) * 100.0;
```

#### 6.2 Cas spéciaux

| Condition                          | Code `specialCase`    | Signification                         |
|------------------------------------|-----------------------|---------------------------------------|
| `valN1 == null` ou `valN == null`  | `NA`                  | Comparaison impossible                |
| `valN1 == 0` et `valN == 0`        | `STABLE`              | Aucun changement, base nulle          |
| `valN1 == 0` et `valN ≠ 0` (LOWER)| `EMERGING_RISK`       | Nouveau risque apparu                 |
| `valN1 == 0` et `valN ≠ 0` (HIGHER)| `STRONG_IMPROVEMENT` | Performance émergente positive        |
| `valN == 0`                        | `STRONG_IMPROVEMENT`  | Problème résolu (ex: 0 accident)      |

#### 6.3 Résolution de la direction

Le système détermine `Direction` via une cascade de règles :

```
1. Direction explicite sur l'entité Kpi en base de données
        ↓ (si null)
2. Direction déduite de la catégorie KPI (label + code)
        ↓ (si null)
3. Direction déduite du nom du KPI (mots-clés)
        ↓ (si null)
4. Défaut : HIGHER_IS_BETTER
```

**Mots-clés utilisés pour la déduction** :

```java
// Cibles → TARGET_IS_BEST
containsAny(name, "cible", "objectif", "target")

// Indicateurs négatifs → LOWER_IS_BETTER
containsAny(name, "accident", "incident", "nc", "nonconform",
            "reclamation", "rejet", "defaut", "retard", "anomal")

// Indicateurs positifs → HIGHER_IS_BETTER
containsAny(name, "conformite", "reussite", "disponibilite",
            "satisfaction", "performance")
```

#### 6.4 Score de confiance du calcul

Le score de confiance (`calcConfidence`, 0–100) mesure la fiabilité du résultat :

```java
int confidence = 60;              // base
if (kpi != null)              confidence += 20;  // KPI connu en base
if (MISSING_CONTEXT)          confidence -= 25;  // valeur manquante
if (LOW_BASE)                 confidence -= 20;  // valN1 ≈ 0
if (OUTLIER)                  confidence -= 10;  // variation > 300%
```

---

## 7. Calcul — CalculationAgent & Score par catégorie

### Concept théorique

Le `CalculationAgent` orchestre le pipeline de calcul pour **l'ensemble des lignes** importées. Il inclut une étape de **matching flou** entre le nom du KPI importé et les KPIs existants en base de données, ce qui permet d'enrichir le résultat avec les seuils, la direction et l'historique déjà connus.

### Implémentation

**Fichier** : [CalculationAgent.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/CalculationAgent.java)

#### 7.1 Matching fuzzy des KPIs

```java
// Ordre de priorité du matching
1. Exact match  : normalize(importedName).equals(normalize(dbKpiName))          → confiance 100%
2. Substring    : l'un contient l'autre                                         → confiance 90%
3. Jaro-Winkler : similarité ≥ 0.82 (seuil config : fuzzy-threshold=0.85)      → confiance proportionnelle
4. Fallback     : aucun match → nouveau KPI créé avec catégorie AUTO
```

L'algorithme **Jaro-Winkler** est une mesure de distance entre chaînes de caractères. Il accorde plus de poids aux correspondances en début de mot, ce qui est adapté aux noms de KPIs (`"Taux Fréquence"` vs `"Taux de Fréquence"`).

#### 7.2 Chargement de l'historique

Avant de boucler sur les lignes, l'agent charge en **une seule requête** l'historique de variation de tous les KPIs actifs :

```java
// Requête groupée pour éviter N+1
Map<Long, List<Double>> historyCache =
    resultatKpiRepository.findVariationHistoryByKpiIds(kpiIds)
        .stream()
        .collect(Collectors.groupingBy(
            VariationHistoryProjection::getKpiId,
            Collectors.mapping(VariationHistoryProjection::getVariation, Collectors.toList())
        ));
```

Cet historique est utilisé par `ClassificationEngine` pour le calcul des percentiles et du Z-score robuste.

#### 7.3 Tendance

La tendance (`Tendance`) est calculée par comparaison simple :

| Condition              | Tendance      |
|------------------------|---------------|
| `variationPct > +2%`   | `CROISSANCE`  |
| `variationPct < -2%`   | `DIMINUTION`  |
| `variationPct ∈ [-2,+2]` | `STABLE`    |
| données manquantes     | `NA`          |

#### 7.4 Score composite par catégorie

**Formule** :

```
Score(catégorie) = (excellent×100 + faible×75 + indetermine×45
                    + modere×30 + preEscalade×15 + critique×0) / nbKPIs_catégorie
```

**Interprétation** :

| Score     | Label          |
|-----------|----------------|
| ≥ 90      | Excellent       |
| ≥ 75      | Bon             |
| ≥ 55      | Acceptable      |
| ≥ 30      | À surveiller   |
| < 30      | Critique        |

```java
// Code exact — CalculationAgent.java lignes 96-105
double score = total == 0 ? 0 :
    (excellent * 100.0 + faible * 75.0 + indetermine * 45.0
    + modere * 30.0 + preEscalade * 15.0 + critique * 0.0) / total;
```

---

## 8. Classification — ClassificationEngine

### Concept théorique

La classification est le moteur de décision qui transforme un résultat numérique en un **jugement qualitatif** à 6 niveaux. Elle combine plusieurs approches selon les données disponibles :

1. **Classification absolue par seuils** : si le KPI a des seuils configurés, on compare la valeur courante directement aux seuils
2. **Classification relative historique (SPC)** : si 10+ points historiques sont disponibles, on utilise les percentiles (p25, p75, p95)
3. **Z-score robuste** : si 5–9 points historiques sont disponibles, on utilise la médiane et la MAD (Median Absolute Deviation), plus robustes que la moyenne/écart-type face aux outliers

### Implémentation

**Fichier** : [ClassificationEngine.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/ClassificationEngine.java)

#### 8.1 Les 6 niveaux de classification

| Niveau         | Signification                                     |
|----------------|---------------------------------------------------|
| `EXCELLENT`    | Performance optimale, aucune dégradation          |
| `FAIBLE`       | Niveau acceptable, risque minimal                 |
| `MODERE`       | Attention requise, dégradation notable            |
| `PRE_ESCALADE` | Proche du seuil critique, surveillance immédiate  |
| `CRITIQUE`     | Seuil critique franchi, action urgente            |
| `INDETERMINE`  | Données insuffisantes pour classer                |

#### 8.2 Algorithme de décision — cas avec seuils configurés

```
1. Calculer la position absolue de la valeur courante :
   - LOWER_IS_BETTER : v ≤ seuil_faible → EXCELLENT, v ≤ seuil_modere → FAIBLE, etc.
   - HIGHER_IS_BETTER : v ≥ seuil_critique → EXCELLENT (inversé), etc.

2. Si pas de dégradation :
   - Retourner directement la position absolue

3. Si dégradation détectée :
   - La position absolue est prioritaire (critique absolu > score composite)
   - Sinon, calculer le degradationScore (0-100) :
       score = min(55, variationPct × 0.55)
             + min(30, log10(1+absoluteGap) × 15)
             + min(15, log10(1+degradationMagnitude) × 10)
   - degradationScore ≥ 85 ou magnitude ≥ seuil_critique → CRITIQUE
   - degradationScore ≥ 72 ou magnitude ≥ seuil_critique × 0.75 → PRE_ESCALADE
   - degradationScore ≥ 60 ou magnitude ≥ seuil_modere → MODERE
   - degradationScore ≥ 35 ou magnitude ≥ seuil_faible → FAIBLE
```

#### 8.3 Algorithme de décision — cas sans seuils (SPC historique)

**Avec ≥ 10 points historiques** : utilise les percentiles

```java
double p95 = percentile(historyDegradations, 95);
double p75 = percentile(historyDegradations, 75);

if (degradationMagnitude > p95) → CRITIQUE    // hors distribution
if (degradationMagnitude > p75) → MODERE      // queue haute
if (degradationMagnitude > p75 * 0.85) → PRE_ESCALADE
else → FAIBLE
```

**Avec 5–9 points historiques** : utilise le Z-score robuste

```java
// Z robuste = |valeur - médiane| / (1.4826 × MAD)
// (1.4826 est le facteur de normalisation pour approximer l'écart-type)
double robustZ = Math.abs(value - median) / (1.4826 * mad);

if (robustZ >= 3.0) → CRITIQUE       // 3 MAD-écarts-types au-dessus
if (robustZ >= 2.4) → PRE_ESCALADE
if (robustZ >= 1.8) → MODERE
else → FAIBLE
```

**Avec < 5 points** : retourne `INDETERMINE`

#### 8.4 Direction `TARGET_IS_BEST`

Pour les KPIs cibles (ex: objectif de production à atteindre), la dégradation est mesurée comme **distance à la cible** :

```java
double prevDistance = |prev - target|;
double currDistance = |curr - target|;
degradationMagnitude = max(0, currDistance - prevDistance);
// Si on s'éloigne de la cible → dégradation
```

---

## 9. Détection des risques — RiskDetectionAgent

### Concept théorique

Après classification, tous les KPIs ont un niveau. Le `RiskDetectionAgent` filtre pour ne retenir que les **risques prioritaires** à soumettre à l'analyse IA, en évitant de surcharger le prompt LLM avec des KPIs stables.

### Implémentation

**Fichier** : [RiskDetectionAgent.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/processing/RiskDetectionAgent.java)

**Critères de sélection** :
- Classification = `CRITIQUE`
- Classification = `PRE_ESCALADE`
- `riskScore ≥ 12` (probabilité × impact)

**Tri** : par valeur absolue de la variation en pourcentage (décroissant)
**Limite** : 20 KPIs maximum → envoyés au module IA

---

## 10. Module IA — Architecture LLM

### Concept théorique

Le module IA utilise une architecture **provider chain** avec fallback automatique. Deux LLM sont configurés : Groq comme primaire (plus rapide) et Gemini comme secondaire. Un système de **cooldown** évite les appels répétés à un provider en erreur ou en rate-limit. Un **cache SHA-256** évite de ré-analyser le même contenu deux fois.

### Implémentation

**Fichier** : [LlmProviderChain.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/analytics/service/LlmProviderChain.java)

#### 10.1 Providers LLM

| Provider | Modèles configurés                                              | Température (JSON) | Température (texte) |
|----------|-----------------------------------------------------------------|--------------------|---------------------|
| **Groq** | `llama-3.3-70b-versatile`, `meta-llama/llama-4-scout-17b-16e-instruct` | 0.1 | 0.5 |
| **Gemini** | `gemini-2.0-flash`                                            | 0.1                | 0.5                 |

Groq est préféré car il expose des modèles Llama très rapides via une infrastructure GPU dédiée. Gemini sert de fallback fiable.

#### 10.2 Chaîne de fallback

```java
// LlmProviderChain.java — méthode callProviders()
if (cooldownManager.isAvailable("groq")) {
    try {
        result = groqService.generate(prompt);
        if (result != null) return new ProviderResult(result, "groq");
    } catch (ProviderUnavailableException e) { ... }
}

if (cooldownManager.isAvailable("gemini")) {
    result = geminiService.generateRaw(prompt);
    if (result != null) return new ProviderResult(result, "gemini");
}
```

#### 10.3 Cache SHA-256

La clé de cache est construite à partir du **contenu** du prompt (pas du sessionId), ce qui permet des cache hits entre sessions analysant les mêmes KPIs :

```java
// La clé exclut "session=XXX" pour être stable entre sessions
String stablePrefix = cacheKeyPrefix.replaceAll("(?:^|\\|)session=[^|]*", "");
String fullCacheKey = "llm:" + toHexString(sha256(stablePrefix + "|" + prompt));
```

#### 10.4 Timeout et retry

```java
// Timeout global : 30 secondes par appel
Future<ProviderResult> future = sharedExecutor.submit(() -> callProviders(prompt));
return future.get(30, TimeUnit.SECONDS);  // TimeoutException → retry

// 3 tentatives au total
for (int attempt = 1; attempt <= 3; attempt++) { ... }
```

#### 10.5 Rotation de clés API Groq

`GroqKeyRotator` maintient une liste de clés API (configurées dans `.env`) et les alterne en round-robin pour répartir la charge et éviter les rate-limits par clé.

---

## 11. Module IA — RAG (Retrieval-Augmented Generation)

### Concept théorique

Le RAG (Retrieval-Augmented Generation) est une technique qui **enrichit le prompt LLM** avec des informations pertinentes extraites d'une base de connaissances locale, plutôt que de dépendre uniquement de la mémoire paramétrique du modèle. Dans QHSE Analytics, la base RAG contient les définitions normatives des KPIs QHSE (ISO 9001, 14001, 45001), leurs seuils de référence, et leurs directions métier.

**Avantage** : le LLM peut citer des seuils précis et des normes ISO spécifiques sans hallucination, car ces informations lui sont fournies explicitement dans le contexte.

### Architecture RAG

```
Requête (noms des KPIs à analyser)
        │
        ▼
┌─────────────────┐
│ EmbeddingService│  → Gemini Embedding API → vecteur float[768]
└────────┬────────┘
         │ vecteur
    ┌────▼────────────────────────────┐
    │ RagSearchService.findRelevant() │
    │                                  │
    │  1. vectorSearch (threshold 0.72)│ → SQL: embedding <=> ?::vector
    │  2. vectorSearch (threshold 0.50)│ → fallback si vide
    │  3. keywordSearch (ILIKE)        │ → fallback si embedding indispo
    └────────────────┬────────────────┘
                     │ List<RagKnowledge>
                     ▼
         Injecté dans le prompt LLM
```

### Implémentation

#### 11.1 Génération des embeddings

**Fichier** : [EmbeddingService.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/analytics/service/EmbeddingService.java)

Le service appelle l'API Gemini Embedding avec une dimension fixée à **768** :

```java
// EmbeddingService.java — méthode callApi()
String body = String.format(
    "{\"model\":\"models/%s\",\"content\":{\"parts\":[{\"text\":%s}]},\"outputDimensionality\":768}",
    embeddingModel,
    objectMapper.writeValueAsString(text)  // sérialisation JSON sécurisée
);

// Réponse : tableau de 768 floats
float[] vector = new float[values.size()];
for (int i = 0; i < values.size(); i++) {
    vector[i] = (float) values.get(i).asDouble();
}
```

**Gestion du rate-limit** :
- Si HTTP 429 → lecture du header `Retry-After` → cooldown du provider
- 3 tentatives avec backoff exponentiel (1s → 2s → 4s → max 30s)
- Cache Spring (`@Cacheable`) pour éviter de ré-embedder les mêmes textes

**Découverte automatique du modèle** : au démarrage, `discoverEmbeddingModel()` interroge l'API Gemini pour lister les modèles disponibles avec `embedContent`. Si le modèle configuré n'est pas disponible, il bascule automatiquement sur le premier modèle compatible.

#### 11.2 Recherche vectorielle

**Fichier** : [RagSearchService.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/analytics/service/RagSearchService.java)

La recherche utilise l'opérateur `<=>` de pgvector (distance cosinus) :

```sql
-- Recherche vectorielle dans PostgreSQL + pgvector
SELECT id, kpi_name, chunk_type, definition, thresholds, category
FROM rag_knowledge
WHERE embedding IS NOT NULL
  AND (1 - (embedding <=> ?::vector)) >= 0.72   -- similarité cosinus ≥ 72%
ORDER BY embedding <=> ?::vector                  -- plus proche en premier
LIMIT 15
```

**Stratégie en cascade** :
```java
public List<RagKnowledge> findRelevant(String query, int topK, String category, double threshold) {
    float[] vector = embeddingService.embed(query);
    if (vector != null) {
        // 1. Recherche stricte (similarité ≥ 0.72)
        List<RagKnowledge> results = vectorSearch(vectorLiteral, topK, category, 0.72);
        if (!results.isEmpty()) return results;

        // 2. Recherche relâchée (similarité ≥ 0.50)
        results = vectorSearch(vectorLiteral, topK, category, 0.50);
        if (!results.isEmpty()) return results;
    }
    // 3. Fallback par mots-clés (ILIKE)
    return keywordSearch(query, topK);
}
```

#### 11.3 Base de connaissances RAG

**Table** : `rag_knowledge`

| Colonne       | Type           | Contenu                                              |
|---------------|----------------|------------------------------------------------------|
| `kpi_name`    | VARCHAR        | Nom de l'indicateur                                  |
| `definition`  | TEXT           | Définition normative (ISO, INRS, etc.)               |
| `thresholds`  | JSONB          | `{"faible": X, "modere": Y, "critique": Z}`          |
| `category`    | VARCHAR        | Domaine QHSE (Q, H, S, E)                           |
| `direction`   | VARCHAR        | `HIGHER_IS_BETTER` / `LOWER_IS_BETTER` / `TARGET_IS_BEST` |
| `embedding`   | `vector(768)`  | Vecteur dense calculé par Gemini                    |
| `chunk_type`  | VARCHAR        | `"full"` ou `"partial"` (pour les longues définitions) |

**Population automatique** : quand un nouveau KPI est créé lors de l'import (aucun match trouvé), `ImportProcessingService.createRagKnowledgeForKpi()` crée automatiquement une entrée dans `rag_knowledge` avec la définition générée par l'IA (via `MetadataEnrichmentAgent`).

#### 11.4 Migrations SQL pgvector

```sql
-- V3__pgvector_rag_embedding.sql (extrait)
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE rag_knowledge
    ADD COLUMN embedding vector(768);

CREATE INDEX idx_rag_knowledge_embedding
    ON rag_knowledge
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

L'index **IVFFlat** (Inverted File Flat) divise l'espace vectoriel en 100 clusters (lists=100) pour accélérer la recherche approximative. C'est un compromis entre vitesse et précision adapté à des bases de quelques milliers de KPIs.

---

## 12. Module IA — Prompt Engineering & Few-Shot

### Concept théorique

Le **prompt engineering** est la discipline qui consiste à concevoir les instructions données au LLM pour obtenir des réponses de qualité professionnelle, structurées et fiables. Dans QHSE Analytics, trois techniques sont combinées :

1. **System prompt** : définit le persona, les règles absolues et le format de sortie
2. **Few-shot examples** : fournit des exemples concrets du niveau de qualité attendu
3. **Context injection** : injecte les données réelles (scores pré-calculés, résultats RAG, KPIs)

### Implémentation

**Fichier** : [StructuredAnalysisPromptBuilder.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/analytics/service/processing/StructuredAnalysisPromptBuilder.java)

#### 12.1 System Prompt — Persona et règles

```
"Tu es un consultant QHSE expert de niveau senior (15 ans d'expérience),
auditeur certifié ISO 9001, ISO 14001 et ISO 45001,
maîtrisant les référentiels DREAL, INRS, et les méthodes d'analyse 8D, 5 Pourquoi
et diagramme d'Ishikawa."
```

**12 règles absolues** incluent notamment :
- **Règle 1** : Répondre UNIQUEMENT avec du JSON valide — aucun texte avant/après, aucun markdown
- **Règle 2** : Chaque champ texte doit être spécifique, chiffré et actionnable (interdiction des `"N/A"`, texte < 15 mots)
- **Règle 3** : `insight` doit analyser la variation chiffrée (valeur N-1 → N, écart %, seuil franchi) et l'impact QHSE réel
- **Règle 4** : `probableCauses` classé dans une catégorie Ishikawa : `[Homme]`, `[Machine]`, `[Méthode]`, `[Milieu]`, `[Matière]`
- **Règle 5** : `actionImmediate` doit être SMART — verbe d'action fort, responsable nommé, horizon temporel

#### 12.2 Few-Shot Examples

Les examples sont intégrés **dans le prompt** (pas comme messages séparés) pour être compatibles avec l'API Groq :

**Exemple de `globalSummary` attendu** (extrait du code) :
```
"L'analyse QHSE portant sur la période 2025 → 2026, couvrant 12 indicateurs répartis
sur 4 catégories (...), révèle un score global de 42/100 (À SURVEILLER) avec 8 KPIs
classés CRITIQUE. La catégorie Qualité est la plus dégradée (score 20/100, 3 critiques),
notamment le First Pass Yield en chute de −18,5 % (91 % → 74,2 %), franchissant le seuil
critique ISO 9001 §8.7 (...)"
```

**Exemple de `kpiInsights`** montrant le niveau de détail attendu :
```json
{
  "kpiId": 1547,
  "kpiName": "Taux de Fréquence des Accidents (TF1)",
  "insight": "Le TF1 a progressé de +28 % entre N-1 et N (2,5 → 3,2), dépassant
              le seuil critique fixé à 3,0 selon le référentiel ISO 45001 §6.1.2...",
  "probableCauses": [
    "[Méthode] Absence de révision des analyses de risques lors de l'augmentation de cadence",
    "[Homme] Déficit de formation sécurité pour les opérateurs récemment embauchés"
  ],
  "actionImmediate": "Organiser une revue sécurité d'urgence dans les 48h...",
  "urgency": "HIGH",
  "successMetric": "TF1 revient sous le seuil de 2,5 dans les 3 prochains mois"
}
```

#### 12.3 Injection des données contextuelles

Le prompt est construit dynamiquement avec 4 sources de contexte :

**Source 1 — Scores pré-calculés** (vérité ground-truth, non générée par LLM) :
```
=== SCORES CALCULÉS (à citer tels quels dans globalSummary) ===
Période analysée : 2024 → 2025
Score global : 42/100 (À SURVEILLER) | KPIs totaux : 12 | Critiques : 5 | Modérés : 3
  • Qualité : score 20/100 | 3 critique(s), 1 modéré(s), 2 OK
  • Sécurité : score 55/100 | 1 critique(s), 2 modéré(s), 3 OK
KPI le plus dégradé : First Pass Yield (-18.5%, 91.0→74.2, CRITIQUE)
```

**Source 2 — Base de connaissances RAG** :
```
=== BASE DE CONNAISSANCES QHSE (RAG vectoriel) ===
sourceId: rag_knowledge | relevanceScore: 0.95
• Taux de Fréquence [full]: Nombre d'accidents avec arrêt × 10^6 / heures travaillées...
  | Seuils: {"faible": 1.5, "modere": 2.5, "critique": 4.0}
```

**Source 3 — Périmètre global** (pour `globalSummary` dans le mode chunked) :
```
=== PÉRIMÈTRE GLOBAL DE LA SESSION ===
⚠️ Cette liste sert UNIQUEMENT à rédiger le champ globalSummary.
Noms des KPIs de la session : Taux Fréquence, FPY, Délai Livraison, ...
```

**Source 4 — Données KPI** (cœur du prompt) :
```
=== KPI CONTEXT & RAG SOURCES ===
⚠️ RÈGLE ABSOLUE : kpiId DOIT être exactement l'entier indiqué après KPI_ID=.
KPI_ID=147 | KPI: First Pass Yield | Categorie: Qualité |
  N-1=91.0 | N=74.2 | Variation=-18.5% | Classification=CRITIQUE
```

#### 12.4 Output Schema

Le LLM est contraint à produire un JSON précis :

```json
{
  "globalSummary": "string",
  "confidence": { "overall": 85, "sections": {...} },
  "kpiInsights": [ { "kpiId": ..., "insight": ..., "probableCauses": [...], ... } ],
  "probableCauses": ["[Homme] ...", "[Méthode] ..."],
  "recommendations": [ { "title": ..., "rationale": ..., "expectedBenefit": ... } ],
  "actionPlan": [ { "action": ..., "ownerRole": ..., "dueHorizon": ... } ],
  "rootCauseAnalysis": [ { "method": "5_whys", "whyChain": [...], "ishikawaCategory": ... } ],
  "predictiveAlerts": [ { "projection": ..., "severity": "HIGH", "estimatedHorizonMonths": 3 } ],
  "traceability": { "modelName": ..., "generatedAt": ..., "contextSourcesUsed": [...] }
}
```

#### 12.5 Mécanisme de retry sur JSON invalide

Si la réponse LLM ne parse pas en JSON valide, le système régénère avec un prompt de correction :

```java
// AnalysisAgent — jusqu'à 2 tentatives de régénération
String retryPrompt = buildRetryPrompt(originalPrompt, validationErrors);
// Le retryPrompt contient :
// "ATTENTION : ta réponse précédente était invalide.
//  Erreurs de validation : [champs vides, format incorrect].
//  Régénère le JSON avec exactement le même schéma."
```

Un suffixe générique est également disponible :

```java
public static final String GENERIC_RETRY_SUFFIX =
    "ATTENTION : ta réponse précédente contenait des champs vides ou génériques. " +
    "Cette fois, chaque champ DOIT contenir une analyse réelle et détaillée. " +
    "Les valeurs 'N/A', vides ou inférieures à 20 mots sont refusées.";
```

---

## 13. Module IA — AnalysisAgent (orchestration)

### Concept théorique

L'`AnalysisAgent` est le chef d'orchestre du module IA. Il gère deux modes d'analyse :
- **Mode strict** (fast path) : envoi direct de tous les KPIs en un seul prompt
- **Mode structuré** (slow path, par défaut) : découpage en chunks de 5 KPIs pour ne pas dépasser la fenêtre de contexte du LLM, puis fusion des résultats

### Implémentation

**Fichier** : [AnalysisAgent.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/analytics/service/processing/AnalysisAgent.java)

#### 13.1 Mode structuré — chunking

```java
// Configuration : app.analysis.batch-size=5
int batchSize = 5;
List<List<KpiCalculatedDTO>> chunks = partition(kpis, batchSize);

for (List<KpiCalculatedDTO> chunk : chunks) {
    String prompt = promptBuilder.buildPrompt(chunk, kpis.size(), kpis);
    ProviderResult result = llmProviderChain.generate(prompt, cacheKey, false);
    AiAnalysisStructuredResponse chunkResponse = parseAndValidate(result.response());
    merge(globalResponse, chunkResponse);
}
```

**Pourquoi chunker à 5 ?** Les modèles Llama sur Groq ont une fenêtre de contexte limitée. Un KPI avec ses données + la réponse attendue occupe ~500 tokens. Avec 5 KPIs, plus les instructions + le RAG, on reste sous ~4000 tokens/requête, ce qui garantit une réponse complète sans troncature.

**Fusion des chunks** : le `globalSummary` est produit une seule fois (premier chunk ou chunk de synthèse). Les `kpiInsights`, `recommendations`, et `actionPlan` sont concaténés.

#### 13.2 Score de confiance global

`StructuredAnalysisPromptBuilder.computeScores()` calcule le score **avant** d'envoyer le prompt, en pondérant les KPIs CRITIQUE avec un poids double :

```java
// Score pondéré (CRITIQUE compte double)
double base   = classification.contains("CRITIQUE") ? 20 : 
                classification.contains("MODERE")   ? 60 : 100;
double weight = classification.contains("CRITIQUE") ? 2 : 1;
weightedSum += base * weight;
totalWeight += weight;

globalScore = (int) Math.round(weightedSum / totalWeight);
```

Interprétation du score global :

| Score | Label          |
|-------|----------------|
| ≥ 80  | EXCELLENT      |
| ≥ 60  | SATISFAISANT   |
| ≥ 40  | À SURVEILLER   |
| < 40  | CRITIQUE       |

---

## 14. Persistance & Machine à états

### Concept théorique

L'import est un processus long qui peut être interrompu. Une machine à états (`ImportStatut`) garantit la cohérence : on ne déclenche l'IA qu'après avoir sauvegardé tous les résultats en base, et on utilise `TransactionSynchronization` pour lancer l'IA **après le commit** de la transaction principale, évitant ainsi les conditions de course.

### Implémentation

**Service** : [ImportProcessingService.java](QHSEAnalytics/src/main/java/com/QHSEAnalytics/importer/service/ImportProcessingService.java)

#### États de la session

```
INITIAL
  → PROCESSING  (calcul en cours)
  → CALCULATED  (résultats sauvegardés, en attente d'IA)
  → READY_FOR_AI (IA déclenchée)
  → ERREUR      (échec bloquant)
```

#### Déclenchement asynchrone de l'IA

```java
// TransactionSynchronizationManager garantit que l'IA ne démarre
// qu'après le COMMIT de la transaction qui sauvegarde les résultats
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            analyseIaService.triggerAnalyseAsync(importId, userId);
        }
    }
);
```

#### Rapport qualité

`QualityReportBuilder` calcule un score de qualité global (0-100) basé sur les issues collectées :

```java
// Calcul du score par ligne
int rowScore = hasErrors ? 0 : hasWarnings ? 70 : 100;

// Score global
double qualityScore = rows.stream()
    .mapToInt(r -> r.getScore())
    .average()
    .orElse(0.0);
```

---

## 15. Schéma base de données

### Migrations Flyway (ordre chronologique)

| Migration | Contenu                                                              |
|-----------|----------------------------------------------------------------------|
| `V1`      | Index de performance sur les tables principales                       |
| `V2`      | Colonnes stockage fichier (`path`, `bucket`, `checksum`, `size`)     |
| `V3`      | Extension pgvector + colonne `embedding vector(768)` + index IVFFLAT |
| `V4`      | Élargissement de `categorie_kpi.code`                               |
| `V5`      | Audit admin (logs d'actions)                                         |
| `V6`      | Table `ai_config`, colonne `overall_confidence` sur `analyse_globale`|
| `V7`      | Colonne `direction` sur `rag_knowledge`                             |
| `V8`      | Colonne `chunk_type` sur `rag_knowledge`                            |
| `V18`     | Population initiale de la base RAG (définitions ISO par défaut)      |

### Tables principales

```
import_sessions         → métadonnées de chaque import (statut, périodes, userId)
resultat_kpi            → résultats calculés par KPI et par session
kpi                     → référentiel des KPIs (seuils, direction, définition)
categorie_kpi           → catégories QHSE (Q, H, S, E, ...)
rag_knowledge           → base de connaissances vectorielle
analyse_globales        → analyse IA globale par session
analyse_categories      → analyse IA par catégorie
kpi_analysis            → insight IA par KPI
```

---

*Rapport généré le 2026-05-20 — Version du prompt builder : `structured-qhse-v8`*
