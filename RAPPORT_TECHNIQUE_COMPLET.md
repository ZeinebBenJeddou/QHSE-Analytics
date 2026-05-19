# RAPPORT TECHNIQUE COMPLET — QHSE ANALYTICS
## Plateforme d'analyse des indicateurs QHSE avec IA

> **Document de révision technique pour soutenance**
> Projet : QHSE Analytics | Stack : Java 21 + Spring Boot 3.3.5 + Angular 19 + PostgreSQL 16 + pgvector

---

# PARTIE 1 — ARCHITECTURE GLOBALE

## 1.1 Vue d'ensemble

QHSE Analytics est une plateforme web full-stack qui automatise l'import, le traitement, la classification et l'analyse par IA des indicateurs de performance QHSE (Qualité, Hygiène, Sécurité, Environnement). Elle repose sur une architecture en couches strictement séparées.

```
┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND Angular 19                       │
│   import | dashboard | analyse-ia | auth | historique       │
│   Standalone Components | HttpClient | SSE | JWT Cookie     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST + SSE (EventSource)
┌────────────────────────▼────────────────────────────────────┐
│              API Spring Boot 3.3.5 (Java 21)                │
│   Controllers → Services → Repositories (Spring Data JPA)  │
│   JwtAuthFilter | RateLimitingFilter | CORS | BCrypt        │
└────────────────────────┬────────────────────────────────────┘
                         │ JPA/Hibernate (batch_size=50)
┌────────────────────────▼────────────────────────────────────┐
│          PostgreSQL 16 + extension pgvector                 │
│  import_sessions | resultat_kpis | rag_knowledge (vector)   │
│  analyse_globales | kpis | users | refresh_tokens           │
└──────────────┬─────────────────────────┬────────────────────┘
               │ HTTP REST API            │ HTTP REST API
┌──────────────▼──────────┐   ┌──────────▼──────────────────┐
│    Groq Cloud API        │   │   Google Gemini API         │
│  llama-3.3-70b-versatile │   │  gemini-2.0-flash (texte)   │
│  llama-4-scout (fallback)│   │  gemini-embedding-2 (vect.) │
│  qwen3-32b (fallback)    │   │                             │
└─────────────────────────┘   └─────────────────────────────┘
```

**Flux principal :** L'utilisateur (analyste) importe un fichier Excel → le pipeline de traitement Java extrait, nettoie, calcule, classifie les KPIs → un module IA (LLM) génère des analyses textuelles contextualisées par une base de connaissances vectorielle (RAG) → le dashboard affiche les résultats interactifs.

---

## 1.2 Packages backend

| Package | Rôle |
|---------|------|
| `com.QHSEAnalytics` | Classe principale `@SpringBootApplication` |
| `auth/` | Inscription, login, OTP, JWT, refresh token, profil utilisateur |
| `analytics/` | Module IA : LLM, RAG, embeddings, dashboard analyste, dashboard admin |
| `importer/` | Pipeline import Excel : extraction → nettoyage → calcul → classification |
| `kpi/` | Gestion des mappings colonnes (templates réutilisables) |
| `export/` | Génération PDF (iText7) et Excel |
| `config/` | Spring Security, CORS, Cache Caffeine, Async, pgvector init |
| `security/` | Filtres JWT, rate limiting in-memory |
| `shared/` | Entités JPA, DTOs, enums, repositories, exceptions communes |

---

## 1.3 Modèle de données

| Table | Description | Relations clés | Colonnes importantes |
|-------|-------------|----------------|---------------------|
| `users` | Comptes utilisateurs | — | email, password_hash, role, is_active, is_verified, is_system_admin |
| `kpis` | Référentiel KPI | → categorie_kpis | nom, definition, unite, direction, seuil_faible/modere/critique, target_value |
| `categorie_kpis` | Catégories Q/H/S/E | ← kpis | code (Q/H/S/E), libelle |
| `import_sessions` | Sessions d'import | → users | statut, periode_n1, periode_n, mode (SINGLE/DUAL), message_erreur |
| `resultat_kpis` | Résultats calculés | → import_sessions, kpis | valeur_n1, valeur_n, variation_absolue, variation_relative, niveau_variation, tendance, analyse_ia |
| `analyse_globales` | Analyse IA globale | → import_sessions | synthese, plan_actions, structured_response_json, overall_confidence |
| `analyse_categories` | Analyse IA par catégorie | → import_sessions | categorie_code, contenu |
| `rag_knowledge` | Base de connaissances RAG | — | kpi_name, chunk_type, definition, thresholds, category, embedding (vector(768)) |
| `refresh_tokens` | Tokens de session | → users | token, expires_at, revoked |
| `otp_codes` | Codes OTP 2FA | → users | code, expires_at, used |
| `mapping_configs` | Templates de mapping | → users | nom_colonne_source, index_colonne, type_mapping |
| `kpi_raw_data` | Données brutes Excel | → import_sessions | kpi_name, valeur_n_raw, valeur_n1_raw, statut_nettoyage |
| `staging_donnees` | Données nettoyées | → import_sessions | valeur_n, valeur_n1, score_qualite |
| `ai_config` | Config LLM runtime | — | config_key, config_value (température, timeout) |
| `admin_audit_log` | Journal d'audit admin | → users | action, target_user_id, created_at |

---

## 1.4 Technologies et versions

| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 | Langage backend, records, sealed classes |
| Spring Boot | 3.3.5 | Framework REST, sécurité, async |
| Angular | 19 | Frontend standalone components |
| PostgreSQL | 16 | Base de données principale |
| pgvector | 0.5+ | Extension vecteurs pour la recherche RAG |
| Groq API | — | LLM principal (llama-3.3-70b, llama-4-scout, qwen3-32b) |
| Google Gemini | gemini-2.0-flash | LLM fallback + embeddings 768d |
| Apache POI | 5.x | Lecture fichiers Excel (.xlsx/.xls) |
| iText7 | 7.x | Génération PDF |
| Flyway | 10.x | Migrations versionnées (V1→V17) |
| Caffeine Cache | 3.x | Cache LRU in-memory (24h TTL analyses) |
| jjwt | 0.12.x | Génération/validation JWT HS256 |
| BCrypt | — | Hachage mots de passe |
| Micrometer/Prometheus | — | Métriques observabilité |

---

# PARTIE 2 — PIPELINE D'IMPORT ET TRAITEMENT

## 2.1 Vue d'ensemble du pipeline

```
Fichier Excel uploadé (.xlsx / .xls)
          │
          ▼
  ┌───────────────────┐
  │  ExcelFileValidator│  ← Taille ≤15MB, magic bytes, extension
  └────────┬──────────┘
           │ fichier valide
           ▼
  ┌───────────────────┐
  │   ExtractionAgent │  ← Détection en-tête, synonymes colonnes, CUSTOM/AUTO
  └────────┬──────────┘
           │ List<KpiRawDataDTO>
           ▼
  ┌───────────────────┐
  │    CleaningAgent  │  ← Normalisation, détection doublons, outliers
  └────────┬──────────┘
           │ données nettoyées
           ▼
  ┌────────────────────────┐
  │   QualityReportBuilder │  ← Score qualité, rapport des anomalies
  └────────┬───────────────┘
           │
           ▼
  ┌────────────────────┐
  │  CalculationAgent  │  ← Variation absolue/relative, Jaro-Winkler matching
  └────────┬───────────┘
           │ ComparativeResult
           ▼
  ┌────────────────────────┐
  │  ClassificationEngine  │  ← 6 niveaux : EXCELLENT/FAIBLE/MODERE/
  └────────┬───────────────┘    PRE_ESCALADE/CRITIQUE/INDETERMINE
           │
           ▼
  ┌──────────────────────────┐
  │ MetadataEnrichmentAgent  │  ← Catégorie, unité, définition depuis référentiel
  └────────┬─────────────────┘
           │
           ▼
  ┌─────────────────────┐
  │  RiskDetectionAgent │  ← Probabilité × Impact = score risque
  └────────┬────────────┘
           │
           ▼
  ┌──────────────────────┐
  │  VisualizationAgent  │  ← Préparation données graphiques (bar, pie)
  └────────┬─────────────┘
           │
           ▼
  ┌──────────────────┐
  │  AnalysisAgent   │  ← Appel LLM → analyse textuelle par KPI
  └────────┬─────────┘
           │
           ▼
  Persistance PostgreSQL (ResultatKpi, AnalyseGlobale, AnalyseCategorie)
  Statut import : TRAITE
```

---

## 2.2 ExcelFileValidator

Valide le fichier avant tout traitement. Trois niveaux de contrôle :

```java
// ExcelFileValidator.java
private static final long MAX_SIZE_BYTES = 15L * 1024 * 1024; // 15 MB

// Magic bytes : signature binaire du format réel
private static final byte[] XLSX_MAGIC = {0x50, 0x4B};           // PK (ZIP)
private static final byte[] XLS_MAGIC  = {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0}; // OLE2

public void validate(MultipartFile file) {
    // 1. Fichier non vide
    if (file == null || file.isEmpty()) {
        throw new InvalidFileFormatException("Aucun fichier fourni.");
    }
    // 2. Taille ≤ 15 MB
    if (file.getSize() > MAX_SIZE_BYTES) {
        throw new FileTooLargeException(
            String.format("Fichier trop volumineux : %.1f MB (max 15 MB)",
                file.getSize() / (1024.0 * 1024.0)));
    }
    // 3. Extension .xlsx ou .xls
    String originalName = file.getOriginalFilename();
    if (originalName == null ||
        !(originalName.toLowerCase().endsWith(".xlsx") || originalName.toLowerCase().endsWith(".xls"))) {
        throw new InvalidFileFormatException("Seuls les fichiers .xlsx et .xls sont acceptés.");
    }
    // 4. Magic bytes (anti-spoofing : renommer un PDF en .xlsx échoue ici)
    try (InputStream is = file.getInputStream()) {
        byte[] header = is.readNBytes(8);
        if (!matchesMagic(header, XLSX_MAGIC) && !matchesMagic(header, XLS_MAGIC)) {
            throw new InvalidFileFormatException(
                "Le fichier n'est pas un fichier Excel valide (signature incorrecte).");
        }
    } catch (IOException e) {
        throw new InvalidFileFormatException("Impossible de lire le fichier uploadé.");
    }
}
```

**Pourquoi les magic bytes ?** Un attaquant peut renommer `malware.pdf` en `data.xlsx`. La vérification des premiers octets détecte la vraie nature du fichier : `.xlsx` est un ZIP (commence par `PK` = `50 4B`), `.xls` est un format OLE2 (commence par `D0 CF 11 E0`).

---

## 2.3 ExtractionAgent

Lit le fichier Excel et détecte automatiquement la structure des colonnes.

**Méthodes d'extraction :**
- `CUSTOM` : l'utilisateur a fourni un mapping explicite (indices de colonnes)
- `AUTO` : détection automatique par synonymes d'en-têtes

**Synonymes reconnus automatiquement :**

```java
private static final Set<String> KPI_SYNONYMS = Set.of(
    "kpi","indicateur","indicateur qhse","libelle",
    "nom indicateur","metric","nom kpi","designation");

private static final Set<String> VALUE_N_SYNONYMS = Set.of(
    "n","annee n","valeur n","resultat n","current",
    "annee actuelle","valeur actuelle","n courant");

private static final Set<String> VALUE_N1_SYNONYMS = Set.of(
    "n-1","annee n-1","valeur n-1","resultat n-1","previous",
    "annee precedente","valeur precedente","n1","n moins 1");
```

**Détection de la ligne d'en-tête :** fenêtre de 50 premières lignes, cherche la première ligne avec ≥2 cellules non vides contenant des synonymes connus.

**Méthode `extract()` (résumé logique) :**
```java
public ExtractionResult extract(MultipartFile file, Map<String, Integer> mapping) {
    excelFileValidator.validate(file);
    // Ouvre le workbook Apache POI
    Sheet sheet = selectDataSheet(workbook, formatter, evaluator);
    // Détermine la méthode : CUSTOM si mapping fourni, AUTO sinon
    ExtractionMethod method = determineExtractionMethod();
    // Détecte la ligne d'en-tête dans les 50 premières lignes
    int headerRowIndex = determineHeaderRow(sheet, normalizedMapping, evaluator, formatter, method);
    // Construit le mapping effectif (index réels de chaque colonne)
    MappingResult mappingResult = buildEffectiveMapping(sheet, normalizedMapping, headerRowIndex, ...);
    // Extrait toutes les lignes de données
    List<KpiRawDataDTO> rows = extractRows(sheet, effectiveMapping, headerRowIndex, ...);
    return new ExtractionResult(rows, method.name(), detectedHeaders, extractionIssues);
}
```

---

## 2.4 CleaningAgent

Nettoie et valide chaque ligne extraite :
- Normalisation des noms KPI : minuscules, suppression accents, trim
- Détection doublons (même nom normalisé → flag `DOUBLON`)
- Validation des valeurs numériques (format, plage)
- Détection outliers : variation relative > 300% → flag `DataFlag.OUTLIER`

---

## 2.5 CalculationAgent

Orchestre le calcul des KPIs et délègue à `ComparativeCalculator`.

**Calcul de variation (dans `ComparativeCalculator.compute()`) :**

```java
public ComparativeResult compute(Kpi kpi, Double valN1, Double valN) {
    // Variation absolue
    double absGap = (valN != null && valN1 != null) ? (valN - valN1) : 0.0;

    // Variation relative : ((N - N-1) / |N-1|) × 100
    Double rel = null;
    if (valN1 == null && valN == null) {
        special = "NA";                  // Aucune valeur → indéterminé
    } else if (valN1 == null || valN == null) {
        special = "NA";                  // Une valeur manquante → indéterminé
    } else if (valN1 == 0d) {
        if (valN == 0d) {
            special = "STABLE";          // 0 → 0 : stable
        } else if (direction == HIGHER_IS_BETTER) {
            special = "STRONG_IMPROVEMENT"; // 0 → positif (sens positif)
        } else {
            special = "EMERGING_RISK";   // 0 → positif (sens négatif) = risque émergent
            flags.add(DataFlag.LOW_BASE);
        }
    } else {
        rel = ((valN - valN1) / Math.abs(valN1)) * 100.0; // Formule standard
        if (valN == 0d) special = "STRONG_IMPROVEMENT";   // X → 0 (LOWER_IS_BETTER)
    }

    // Outlier si variation > 300%
    if (rel != null && Math.abs(rel) >= 300d) {
        flags.add(DataFlag.OUTLIER);
    }
    ...
}
```

**Formule de variation relative :**
```
ΔR (%) = ((N - N-1) / |N-1|) × 100
```

**Cas spéciaux :**
| Cas | N-1 | N | Résultat |
|-----|-----|---|----------|
| STABLE | 0 | 0 | tendance = STABLE, niveau FAIBLE |
| EMERGING_RISK | 0 | >0 (négatif) | niveau MODERE, review requis |
| STRONG_IMPROVEMENT | 0 | >0 (positif) | niveau FAIBLE |
| NA | null | quelconque | INDETERMINE |
| OUTLIER | x | y | |Δ%| ≥ 300% |

**Score de confiance de calcul :**
```java
int confidence = 60;
if (kpi != null) confidence += 20;              // KPI reconnu dans le référentiel
if (flags.contains(MISSING_CONTEXT)) confidence -= 25;
if (flags.contains(LOW_BASE)) confidence -= 20;
if (flags.contains(OUTLIER)) confidence -= 10;
confidence = Math.max(0, Math.min(100, confidence));
```

**Matching KPI (Jaro-Winkler) dans `CalculationAgent` :**
```java
private MatchResult findMatchingKpi(KpiRawDataDTO row, Map<String, Kpi> byName) {
    // 1. Correspondance exacte normalisée
    if (byName.containsKey(normalized)) return new MatchResult(byName.get(normalized), 1.0);
    // 2. Inclusion (l'un contient l'autre)
    for (Map.Entry<String, Kpi> entry : byName.entrySet()) {
        if (normalized.contains(key) || key.contains(normalized))
            return new MatchResult(entry.getValue(), 0.9);
    }
    // 3. Jaro-Winkler fuzzy matching, seuil 0.82
    Kpi best = null;
    double bestScore = 0.0;
    for (Map.Entry<String, Kpi> entry : byName.entrySet()) {
        double score = jaroWinkler(normalized, entry.getKey());
        if (score > bestScore) { bestScore = score; best = entry.getValue(); }
    }
    if (bestScore >= 0.82) return new MatchResult(best, bestScore);
    return new MatchResult(null, null);
}
```

---

## 2.6 ClassificationEngine

Le moteur de classification est le composant le plus sophistiqué du pipeline. Il attribue à chaque KPI un niveau parmi 6.

### Les 6 niveaux de classification

| Niveau | Signification | Score catégorie |
|--------|--------------|-----------------|
| `EXCELLENT` | KPI en zone optimale, amélioration notable | 100 pts |
| `FAIBLE` | Dégradation légère ou dans les seuils | 75 pts |
| `MODERE` | Dégradation modérée ou en zone d'alerte | 30 pts |
| `PRE_ESCALADE` | Dégradation pré-critique, surveillance immédiate | 15 pts |
| `CRITIQUE` | Dégradation critique, action urgente | 0 pts |
| `INDETERMINE` | Données insuffisantes ou cas spéciaux | 45 pts |

### Logique de classification (code exact)

```java
public ClassificationResult classify(Kpi kpi, ComparativeCalculator.ComparativeResult comp,
                                      List<Double> historical) {
    // 1. Cas spéciaux issus du ComparativeCalculator
    if ("NA".equals(comp.getSpecialCase())) {
        r.setClassification(INDETERMINE);
        return r;
    }
    if ("STABLE".equals(comp.getSpecialCase())) {
        r.setClassification(FAIBLE); // 0→0 = pas de risque
        return r;
    }
    if ("EMERGING_RISK".equals(comp.getSpecialCase())) {
        r.setClassification(MODERE); // Risque émergent
        return r;
    }

    // 2. Si des seuils KPI sont définis (priorité absolue)
    if (hasThresholds(kpi)) {
        Direction dir = resolveDirection(kpi, comp);
        String absolutePosition = computeAbsolutePosition(dir, currentValue, faible, modere, critique);

        // LA POSITION ABSOLUE PRIME SUR LA VARIATION RELATIVE
        if (!hasDegradation) {
            if ("EXCELLENT".equals(absolutePosition)) return EXCELLENT;
            if ("MODERE".equals(absolutePosition))   return MODERE;   // amélioration mais encore en zone modérée
            if ("CRITIQUE".equals(absolutePosition)) return CRITIQUE; // amélioration mais encore en zone critique
            return FAIBLE;
        }

        // En cas de dégradation, la position absolue prime aussi
        if ("CRITIQUE".equals(absolutePosition)) return CRITIQUE;
        if ("MODERE".equals(absolutePosition))   return MODERE;
        if ("EXCELLENT".equals(absolutePosition)) return FAIBLE; // légère dégradation dans zone optimale

        // Score composite (dégradation hors position absolue)
        if (degradationMagnitude >= critique || degradationScore >= 85) return CRITIQUE;
        if (degradationScore >= 72 || ...) return PRE_ESCALADE;
        if (degradationMagnitude >= modere || degradationScore >= 60)   return MODERE;
        if (degradationMagnitude >= faible || degradationScore >= 35)   return FAIBLE;
        return FAIBLE;
    }

    // 3. Sans seuils : analyse statistique historique
    if (history.size() >= 10) {
        // Percentiles p95/p75/p25 sur l'historique
        if (degradationMagnitude > p95) return CRITIQUE;
        if (degradationMagnitude > p75) return MODERE;
        ...
    }
    if (history.size() >= 5) {
        // Robust Z-Score (MAD) : résistant aux outliers
        double robustZ = robustZScore(historyDegradations, degradationMagnitude);
        if (robustZ >= 3.0)  return CRITIQUE;
        if (robustZ >= 2.4)  return PRE_ESCALADE;
        if (robustZ >= 1.8)  return MODERE;
        return FAIBLE;
    }

    // 4. Pas de seuils ni historique suffisant
    return INDETERMINE;
}
```

**Calcul de la position absolue :**
```java
private String computeAbsolutePosition(Direction dir, Double currentValue,
                                        double faible, double modere, double critique) {
    if (dir == Direction.LOWER_IS_BETTER) {
        // Ex: taux accidents → plus c'est bas, mieux c'est
        if (v <= faible)   return "EXCELLENT";
        if (v <= modere)   return "FAIBLE";
        if (v <= critique) return "MODERE";
        return "CRITIQUE";
    }
    if (dir == Direction.HIGHER_IS_BETTER) {
        // Ex: taux conformité → plus c'est haut, mieux c'est
        if (v >= critique) return "EXCELLENT";
        if (v >= modere)   return "FAIBLE";
        if (v >= faible)   return "MODERE";
        return "CRITIQUE";
    }
    return "UNKNOWN";
}
```

**Exemple concret :**
- KPI : Taux d'accidents (LOWER_IS_BETTER), seuils : Faible=2, Modéré=5, Critique=10
- Valeur N-1 = 8, Valeur N = 6 → Amélioration (+25%)
- Position absolue de 6 : entre modéré(5) et critique(10) → zone "MODERE"
- **Résultat : MODERE** (malgré l'amélioration, la valeur reste en zone dangereuse)

---

## 2.7 Mode Dual File

Le mode Dual permet d'importer deux fichiers distincts : un pour la période N-1 et un pour la période N, puis de les fusionner.

```java
// DualFileImportService.java
public MergeResult mergeFiles(MultipartFile fileN1, MultipartFile fileN,
                               int anneeN1, int anneeN,
                               Map<String, Integer> mappingN1, Map<String, Integer> mappingN) {
    // 1. Extraire les valeurs de chaque fichier (Map<nomNormalisé, Double>)
    Map<String, Double> valeursN1 = extraireValeurs(fileN1, mappingN1);
    Map<String, Double> valeursN  = extraireValeurs(fileN,  mappingN);

    // 2. Union de tous les KPIs (ceux présents dans l'un ou l'autre)
    Set<String> tousLesKpis = new LinkedHashSet<>();
    tousLesKpis.addAll(valeursN1.keySet());
    tousLesKpis.addAll(valeursN.keySet());

    for (String kpiName : tousLesKpis) {
        Double valN1 = valeursN1.get(kpiName);
        Double valN  = valeursN.get(kpiName);

        // KPI absent d'un fichier → INDETERMINE (pas de comparaison possible)
        boolean valid = valN1 != null && valN != null;
        KpiRawDataDTO dto = KpiRawDataDTO.builder()
            .kpiName(kpiName)
            .valeurN1(valN1)  // null si absent
            .valeurN(valN)    // null si absent
            .valid(valid)
            .validationMessage(valid ? null : "KPI absent d'un des fichiers — INDETERMINE")
            .methodeExtraction("DUAL_FILE")
            .scoreConfiance(valid ? 0.85 : 0.0)
            .build();
        result.add(dto);
    }
    return new MergeResult(result, warnings);
}
```

**Normalisation des noms pour la fusion :**
```java
static String normaliserNom(String value) {
    String s = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
    s = s.replaceAll("\\p{M}", "");          // Supprime les diacritiques
    s = s.toLowerCase(Locale.ROOT);
    s = s.replaceAll("[^a-z0-9\\s]", " ");   // Remplace les caractères spéciaux
    s = s.replaceAll("\\s+", " ").trim();
    return s;
}
```

**Garde anti-inversion colonnes :** Si la colonne KPI-Name contient un nombre (ex: la valeur numérique), la ligne est ignorée avec un warning :
```java
if (parseDouble(kpiRaw) != null) {
    log.warn("[DualFile] Ligne {} ignorée : colonne KPI contient un nombre ('{}') ...", r, kpiRaw);
    continue;
}
```

---

## 2.8 Optimisations pipeline

### Hibernate Batch
```properties
# application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```
Permet d'envoyer 50 INSERT/UPDATE en une seule requête SQL au lieu de 50 requêtes individuelles. Gain estimé : ~60% de réduction des requêtes SQL lors de la persistance des `ResultatKpi`.

### Préchargement KPIs en lot (élimination N+1)
```java
// CalculationAgent.java — chargement en une seule requête
List<Long> kpiIds = activeKpis.stream().map(Kpi::getId)...;
Map<Long, List<Double>> historyCache = resultatKpiRepository
    .findVariationHistoryByKpiIds(kpiIds)  // UN seul appel SQL
    .stream()
    .collect(Collectors.groupingBy(...));
```
Sans cette optimisation : 1 requête SQL par KPI pour récupérer l'historique → N+1 queries. Avec cette optimisation : 1 seule requête pour tous les KPIs.

### Cache LLM SHA-256
```java
// LlmProviderChain.java
String fullCacheKey = "llm:" + toHexString(sha256(stablePrefix + "|" + prompt));
Cache.ValueWrapper cached = cache.get(fullCacheKey);
if (cached != null) return (ProviderResult) cached.get(); // Pas d'appel API
```
TTL : 24 heures (configuré Caffeine). Si le même KPI est ré-analysé avec les mêmes données, le résultat LLM est servi depuis le cache sans aucun appel réseau.

---

# PARTIE 3 — MODULE IA

## 3.1 Architecture LLM

```
LlmProviderChain (orchestrateur)
├── Groq Cloud (prioritaire)
│   ├── Modèle 1 : llama-3.3-70b-versatile
│   ├── Modèle 2 : meta-llama/llama-4-scout-17b-16e-instruct
│   ├── Modèle 3 : qwen/qwen3-32b
│   └── GroqKeyRotator (rotation des clés API sur erreur 429)
└── Google Gemini (fallback global)
    └── gemini-2.0-flash (génération texte)

ProviderCooldownManager
└── ConcurrentHashMap<provider, Instant>
    └── Délai configuré dynamiquement (header Retry-After ou 60s par défaut)

Cache Caffeine "kpiAnalysis"
└── TTL 24h, clé = SHA-256(prefix + prompt)
```

---

## 3.2 LlmProviderChain — logique generate()

```java
// LlmProviderChain.java
public ProviderResult generate(String prompt, String cacheKeyPrefix, boolean bypassCache) {
    // 1. Clé stable : on retire sessionId pour permettre le cache hit sur re-analyses
    String stablePrefix = cacheKeyPrefix.replaceAll("(?:^|\\|)session=[^|]*", "")...;
    String fullCacheKey = "llm:" + toHexString(sha256(stablePrefix + "|" + prompt));

    // 2. Vérification cache
    if (!bypassCache && cache != null) {
        Cache.ValueWrapper cached = cache.get(fullCacheKey);
        if (cached != null) return (ProviderResult) cached.get(); // cache HIT
    }

    // 3. Jusqu'à 3 tentatives
    int maxAttempts = 3;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            // Timeout de 30 secondes par appel
            ProviderResult result = executeWithTimeout(prompt, Duration.ofSeconds(30));
            if (result != null && result.response() != null) {
                cacheResult(cache, fullCacheKey, result); // Mise en cache
                return result;
            }
        } catch (TimeoutException | ExecutionException | Exception e) {
            log.warn("[LlmChain] provider error on attempt {}: {}", attempt, e.getMessage());
        }
    }
    return new ProviderResult(null, "none"); // Tous les providers épuisés
}

// Sélection du provider
private ProviderResult callProviders(String prompt) {
    if (cooldownManager.isAvailable("groq")) {
        try {
            String result = groqService.generate(prompt);
            if (result != null) return new ProviderResult(result, "groq");
        } catch (ProviderUnavailableException e) {
            log.warn("[LlmChain] Groq unavailable: {}", e.getMessage());
        }
    }
    // Fallback Gemini
    if (cooldownManager.isAvailable("gemini")) {
        String result = geminiService.generateRaw(prompt);
        if (result != null) return new ProviderResult(result, "gemini");
    }
    return new ProviderResult(null, "none");
}
```

---

## 3.3 GroqService

```java
// GroqService.java
private String appelerInternal(String prompt, int maxTokens, boolean jsonMode) {
    List<String> candidateModels = resolveCandidateModels(); // llama-3.3-70b, llama-4-scout, qwen3-32b
    RestTemplate restTemplate = buildRestTemplate(); // timeout configuré (défaut 30s)

    for (String candidateModel : candidateModels) {
        for (int attempt = 0; attempt < groqKeyRotator.keyCount(); attempt++) {
            String apiKey = groqKeyRotator.next(); // Rotation clés API
            try {
                // Construction payload JSON OpenAI-compatible
                ObjectNode payload = objectMapper.createObjectNode()
                    .put("model", candidateModel)
                    .put("temperature", jsonMode ? 0.1 : 0.5) // JSON = déterministe
                    .put("max_tokens", 2800);

                if (jsonMode) {
                    payload.set("response_format",
                        objectMapper.createObjectNode().put("type", "json_object"));
                }
                // Messages system + user
                payload.set("messages", ...);

                String responseBody = restTemplate.exchange(
                    "https://api.groq.com/openai/v1/chat/completions",
                    HttpMethod.POST, request, String.class).getBody();

                return objectMapper.readTree(responseBody)
                    .path("choices").path(0).path("message").path("content").asText();

            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode() == 429) {
                    // Extraire Retry-After depuis le header ou le body
                    long retryAfterSeconds = extractRetryAfterSeconds(ex);
                    cooldownManager.setCooldown(PROVIDER_NAME,
                        retryAfterSeconds > 0 ? retryAfterSeconds : 60L);
                    // Rotation vers la prochaine clé API
                    continue;
                }
            }
        }
    }
    throw new ProviderUnavailableException(PROVIDER_NAME, "all Groq keys/models exhausted");
}
```

**Températures :**
- `groq.temperature.json = 0.1` → Très faible, pour JSON structuré déterministe
- `groq.temperature.text = 0.5` → Modérée, pour texte analytique

---

## 3.4 ProviderCooldownManager

```java
// ProviderCooldownManager.java
@Component
public class ProviderCooldownManager {
    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public boolean isAvailable(String provider) {
        Instant until = cooldowns.get(provider);
        if (until == null) return true;
        if (Instant.now().isAfter(until)) {
            cooldowns.remove(provider); // Cooldown expiré → disponible
            return true;
        }
        return false; // Encore en cooldown
    }

    public void setCooldown(String provider, long seconds) {
        cooldowns.put(provider, Instant.now().plusSeconds(seconds));
        log.warn("[Cooldown] Provider '{}' paused for {}s", provider, seconds);
    }
}
```

**Durées de cooldown :**
- Extrait du header `Retry-After` de la réponse 429 si présent
- Sinon défaut : **60 secondes**
- La durée est dynamique selon ce que l'API Groq retourne

---

## 3.5 Construction des prompts

### GroqPromptBuilder (analyse par KPI, mode `analyzeStrict`)

Le prompt injecte :
1. **Contexte métier RAG** (chunks de la base de connaissances vectorielle)
2. **Historique des plans d'actions** (10 dernières analyses)
3. **Données KPI actuelles** (nom, catégorie, N-1, N, Δabs, Δrel%, classification, seuils)
4. **Instructions strictes** (format JSON attendu, champs obligatoires)

```java
// AnalysisAgent.java — buildPrompt()
private String buildPrompt(List<KpiCalculatedDTO> data) {
    // Injection RAG
    List<RagKnowledge> ragResults = ragSearchService.findRelevant(combinedQuery, 10, null, 0.65);
    ragResults.forEach(r -> prompt.append("• ").append(r.getKpiName())
        .append(": ").append(r.getDefinition())
        .append(" | Seuils: ").append(r.getThresholds()).append("\n"));

    // Données KPI
    data.forEach(kpi -> {
        prompt.append(String.format(
            "KPI: %s | Catégorie: %s | Direction: %s | N-1=%s | N=%s | " +
            "Δ_abs=%s | Δ_rel=%s%% | Classification=%s | NiveauRisque=%s",
            kpi.getKpiName(), kpi.getCategorie(), kpi.getDirection(),
            kpi.getValeurN1(), kpi.getValeurN(),
            kpi.getAbsoluteGap(), kpi.getVariationPercentage(),
            kpi.getClassification(), kpi.getRiskLevel()));
        // Seuils si disponibles
        if (kpi.getSeuilFaible() != null ...)
            prompt.append(String.format(" | Seuils[F=%s,M=%s,C=%s]", ...));
    });

    // Format JSON attendu
    prompt.append("{ \"overallScore\": number, \"summary\": string, \"kpis\": [...], " +
                  "\"recommendations\": [...] }");
}
```

### StructuredAnalysisPromptBuilder (analyse structurée complète)

Version enrichie avec :
- Few-shot learning (exemple concret de réponse attendue)
- Schema JSON complet : `globalSummary`, `confidence`, `kpiInsights`, `probableCauses`, `recommendations`, `actionPlan`, `rootCauseAnalysis`, `predictiveAlerts`, `traceability`
- Instructions spécifiques : méthode 5 Pourquoi, catégories Ishikawa, alertes prédictives
- Prompt version : `"structured-qhse-v7"`

```java
// StructuredAnalysisPromptBuilder.java
private static final String SYSTEM_PROMPT =
    "Tu es un expert QHSE senior maîtrisant ISO 9001, ISO 14001 et ISO 45001. ...\n" +
    "RÈGLES ABSOLUES :\n" +
    "1. Réponds UNIQUEMENT avec du JSON valide — aucun texte avant ou après.\n" +
    "2. Chaque champ texte doit contenir une analyse réelle et spécifique au KPI " +
    "   (minimum 20 mots). Les valeurs génériques comme 'N/A' sont INTERDITES.\n" +
    "3. actionImmediate doit décrire une action concrète avec un verbe d'action.\n";
```

---

## 3.6 Base de connaissances RAG

### Pourquoi le RAG ?

Les LLMs peuvent "halluciner" des seuils KPI inexistants, des normes erronées, ou des recommandations génériques sans fondement QHSE. Le RAG (Retrieval-Augmented Generation) ancre les réponses dans une base de connaissances réelle et vérifiée.

### Structure des chunks

Pour chaque KPI QHSE, 4 types de chunks sont générés par `KpiDataInitializer` :

| `chunk_type` | Contenu |
|-------------|---------|
| `formule` | Formule de calcul de l'indicateur |
| `interpretation` | Comment interpréter les valeurs (seuils, niveaux) |
| `action` | Actions correctives recommandées |
| `reglementation` | Normes ISO applicables (ISO 45001, ISO 14001, ISO 9001) |

**Exemple de chunks pour "Taux de Fréquence des Accidents" :**
- `formule` : "TF1 = (Nombre d'accidents avec arrêt × 1 000 000) / Nombre d'heures travaillées"
- `interpretation` : "TF1 < 2 = excellent, 2-5 = modéré, > 5 = critique selon référentiel INRS"
- `action` : "Analyse des causes racines, formation sécurité, audit des postes à risque"
- `reglementation` : "ISO 45001:2018 §10.2 — Incident, nonconformity and corrective action"

### EmbeddingService

```java
// EmbeddingService.java
// Modèle : gemini-embedding-2 (Google)
// Dimension vectorielle : 768 dimensions
// Stockage : colonne "embedding" de type vector(768) dans rag_knowledge
```

### RagSearchService — méthode search()

```java
// RagSearchService.java
public List<RagKnowledge> findRelevant(String query, int topK, String category, double threshold) {
    float[] vector = embeddingService.embed(query); // Embed la requête en 768d
    if (vector != null) {
        String vectorLiteral = EmbeddingService.toVectorLiteral(vector);
        try {
            // 1. Recherche vectorielle avec seuil principal (0.72 par défaut)
            List<RagKnowledge> results = vectorSearch(vectorLiteral, topK, category, threshold);
            if (!results.isEmpty()) return results;

            // 2. Seuil fallback (0.50) si pas de résultat au seuil principal
            results = vectorSearch(vectorLiteral, topK, category, fallbackThreshold);
            if (!results.isEmpty()) return results;
        } catch (DataAccessException ex) {
            log.warn("[RAG] pgvector unavailable, falling back to keyword search");
        }
    }
    // 3. Recherche par mots-clés si pgvector indisponible
    return keywordSearch(query, topK);
}

// Requête SQL pgvector (similarité cosinus)
private List<RagKnowledge> vectorSearch(String vectorLiteral, int topK,
                                         String category, double threshold) {
    return jdbcTemplate.query(
        "SELECT id, kpi_name, chunk_type, definition, thresholds, category " +
        "FROM rag_knowledge " +
        "WHERE embedding IS NOT NULL " +
        "AND (1 - (embedding <=> ?::vector)) >= ? " +  // similarité cosinus >= seuil
        "ORDER BY embedding <=> ?::vector LIMIT ?",    // tri par distance cosinus
        ROW_MAPPER, vectorLiteral, threshold, vectorLiteral, topK
    );
}
```

**Paramètres RAG (application.properties) :**
```properties
app.rag.embedding.threshold=0.72        # Seuil principal similarité cosinus
app.rag.embedding.top-k=3              # Nombre de chunks retournés
app.rag.embedding.fallback-threshold=0.50  # Seuil assoupli si pas de résultat
```

**Opérateur pgvector `<=>` :** Distance cosinus entre deux vecteurs. `1 - distance = similarité` (0=opposé, 1=identique).

---

## 3.7 AnalysisAgent

Deux modes d'analyse :

### `analyzeStrict()` — analyse par KPI

Sélectionne les 20 KPIs avec la plus grande variation absolue, construit un prompt, appelle le LLM, parse la réponse JSON.

```java
public AiResponse analyzeStrict(List<KpiCalculatedDTO> calculatedData) {
    List<KpiCalculatedDTO> topKpis = calculatedData.stream()
        .filter(k -> k.getVariationPercentage() != null)
        .sorted(Comparator.comparingDouble(k -> -Math.abs(k.getVariationPercentage())))
        .limit(20)
        .collect(Collectors.toList());

    String prompt = buildPrompt(topKpis);
    String responseJson = llmProviderChain.generate(prompt);
    return parseAiResponse(responseJson);
}
```

### `analyzeStructured()` — analyse globale structurée

Découpe les KPIs en chunks de 5, analyse chaque chunk, fusionne les résultats, retente si validation échoue.

```java
public AiAnalysisStructuredResponse analyzeStructured(...) {
    int chunkSize = 5; // app.analysis.batch-size=5
    List<List<KpiCalculatedDTO>> chunks = partition(topKpis, chunkSize);

    for (List<KpiCalculatedDTO> chunk : chunks) {
        ChunkAnalysisResult result = analyzeStructuredChunkWithRetries(chunk, ...);
        if (result.response() != null) successfulChunkResponses.add(result.response());
    }

    // Fusion des réponses de tous les chunks
    return mergeChunkResponses(successfulChunkResponses);
}
```

**Système de retry (MAX 2 retries de validation) :**
```java
while (true) {
    LlmProviderChain.ProviderResult result = llmProviderChain.generate(currentPrompt, ...);
    AiAnalysisStructuredResponse response = parseStructuredResponse(result.response());

    // Détection réponse générique (champs vides/courts)
    if (isGenericResponse(response)) {
        currentPrompt = basePrompt + GENERIC_RETRY_SUFFIX;
        continue; // retry avec instructions renforcées
    }

    List<String> errors = structuredAnalysisValidator.validate(response, chunkKpis);
    if (errors.isEmpty()) return SUCCESS;

    if (validationRetryCount >= MAX_VALIDATION_RETRY_ATTEMPTS) return FAILED;

    // Retry avec prompt corrigé
    currentPrompt = structuredAnalysisPromptBuilder.buildRetryPrompt(basePrompt, errors);
    validationRetryCount++;
}
```

---

## 3.8 AnalyseIaService

Orchestre tout le flux d'analyse IA après un import.

### `genererToutesLesAnalyses()` — flux complet

```java
@Transactional
public void genererToutesLesAnalyses(Long importSessionId, Long userId, boolean bypassCache) {
    ImportSession session = loadSessionWithOwnership(importSessionId, userId, false);
    List<ResultatKpi> resultats = resultatKpiRepository.findByImportSessionId(importSessionId)
        .stream().filter(r -> r.getKpi() != null && r.getKpi().getCategorieKpi() != null).toList();

    // 1. Analyse stricte (analyzeStrict) → texte par KPI
    AiResponse response = analysisAgent.analyzeStrict(cleanedData);

    // 2. Persistance des analyses par KPI dans ResultatKpi.analyseIa
    for (ResultatKpi resultat : resultats) {
        String analyseIa = insightsByKpiName.get(normalizedKpiName);
        resultat.setAnalyseIa(analyseIa);
        resultatKpiRepository.save(resultat);
    }

    // 3. Persistance AnalyseGlobale (synthèse + plan d'actions)
    analyseGlobale.setSynthese(globalSummary);
    analyseGlobale.setPlanActions(recommendations);
    analyseGlobaleRepository.save(analyseGlobale);

    // 4. Persistance AnalyseCategorie (analyse par catégorie Q/H/S/E)
    for (Map.Entry<String, List<ResultatKpi>> entry : byCategorie.entrySet()) {
        AnalyseCategorie ac = ...;
        analyseCategorieRepository.save(ac);
    }

    // 5. Analyse structurée → persiste JSON dans analyse_globales.structured_response_json
    AiAnalysisStructuredResponse structured = analysisAgent.analyzeStructured(cleanedData, ...);
    analyseGlobale.setStructuredResponseJson(objectMapper.writeValueAsString(structured));
    analyseGlobaleRepository.save(analyseGlobale);

    session.setStatut(ImportStatut.TRAITE);
    importSessionRepository.save(session);
}
```

### `triggerAnalyseAsync()` — déclenchement asynchrone

```java
@Async("aiAnalysisExecutor")
public CompletableFuture<Void> triggerAnalyseAsync(Long importSessionId, Long userId) {
    ImportSession session = loadSessionForAsync(importSessionId);

    // Guard : session doit être en READY_FOR_AI
    if (session.getStatut() != ImportStatut.READY_FOR_AI) return CompletableFuture.completedFuture(null);

    // Guard anti race-condition : si l'analyse existe déjà, skip
    boolean alreadyStored = analyseGlobaleRepository.findByImportSessionId(importSessionId).isPresent()
        || !analyseCategorieRepository.findByImportSessionId(importSessionId).isEmpty()
        || resultats.stream().anyMatch(r -> r.getAnalyseIa() != null);

    if (alreadyStored) {
        log.info("[IA-Async] Analyse IA déjà présente pour la session {}, skip.", importSessionId);
        return CompletableFuture.completedFuture(null);
    }

    genererToutesLesAnalyses(importSessionId, userId);
    return CompletableFuture.completedFuture(null);
}
```

### `getAnalyseStructured()` — lecture avec cache JSON

```java
@Transactional
public AiAnalysisStructuredResponse getAnalyseStructured(Long importSessionId, ...) {
    // Si encore en cours d'analyse → ne pas bloquer
    if (session.getStatut() == ImportStatut.READY_FOR_AI) return null;

    // Lire depuis la base si déjà générée (évite appel LLM)
    var analyseGlobaleOpt = analyseGlobaleRepository.findByImportSessionId(importSessionId);
    if (analyseGlobaleOpt.isPresent()) {
        String json = analyseGlobaleOpt.get().getStructuredResponseJson();
        if (json != null && !json.isBlank()) {
            return objectMapper.readValue(json, AiAnalysisStructuredResponse.class); // CACHE
        }
    }
    // Sinon générer et persister
    AiAnalysisStructuredResponse result = analysisAgent.analyzeStructured(cleanedData, importSessionId);
    ag.setStructuredResponseJson(objectMapper.writeValueAsString(result));
    analyseGlobaleRepository.save(ag);
    return result;
}
```

**Deux méthodes de chargement de session :**
- `loadSessionWithOwnership()` : vérifie que la session appartient à l'utilisateur (sécurité analyste)
- `loadSessionForAsync()` : chargement sans vérification d'ownership (thread async = pas de contexte utilisateur)

---

# PARTIE 4 — SÉCURITÉ

## 4.1 Flux d'authentification complet

```
[1] POST /api/auth/login (email + password)
    │
    ▼
AuthService.login()
    ├── Vérifie credentials (BCrypt)
    ├── Vérifie account actif (is_active = true)
    └── Vérifie email vérifié (is_verified = true)
    │
    ▼
OtpService.generateAndSendOtp()
    ├── Génère OTP 6 chiffres (Random.nextInt(900000) + 100000)
    ├── Durée validité : 10 minutes (app.otp.expiration-minutes=10)
    ├── Stocke en base (otp_codes) avec expires_at
    └── Envoie par email (Gmail SMTP TLS)
    │
    ▼
[2] POST /api/auth/verify-otp (email + code)
    │
    ▼
OtpService.verifyOtp()
    ├── Vérifie OTP non expiré
    ├── Vérifie OTP non déjà utilisé (used = false)
    └── Marque used = true
    │
    ▼
JwtService.generateAccessToken()
    ├── Access Token JWT HS256 (durée : 1 800 000 ms = 30 min)
    └── Claim "role" : ROLE_ANALYSTE ou ROLE_ADMIN

RefreshTokenService.createRefreshToken()
    ├── Refresh Token standard : 3 600 000 ms = 1 heure
    └── Refresh Token rememberMe : 604 800 000 ms = 7 jours
```

## 4.2 JwtService

```java
// JwtService.java
public String generateAccessToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
    return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
}

private String buildToken(Map<String, Object> claims, String subject, long expiration) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)           // email de l'utilisateur
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration)) // +30min
        .signWith(getSigningKey(), Jwts.SIG.HS256) // Algorithme HMAC-SHA256
        .compact();
}

public boolean isTokenValid(String token, UserDetails userDetails) {
    final String email = extractEmail(token);
    return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
}
```

**Paramètres JWT (application.properties) :**
```properties
app.jwt.secret=${JWT_SECRET}                     # Variable d'env, clé secrète HMAC
app.jwt.access-token-expiration=1800000          # 30 minutes en ms
app.jwt.refresh-token-expiration=3600000         # 1 heure en ms
app.jwt.refresh-token-remember-expiration=604800000  # 7 jours en ms
```

---

## 4.3 InMemoryRateLimiter

```java
// InMemoryRateLimiter.java
@Value("${app.rate-limit.auth.max-attempts:5}")
private int maxAttempts; // 5 tentatives max

@Value("${app.rate-limit.auth.window-seconds:300}")
private long windowSeconds; // Fenêtre de 300 secondes (5 minutes)

public boolean allowRequest(String ip, String endpoint) {
    String key = ip + "|" + endpoint;
    Instant now = Instant.now();

    buckets.compute(key, (k, existing) -> {
        // Nouvelle fenêtre si expirée
        if (existing == null || now.isAfter(existing.windowStart().plusSeconds(windowSeconds))) {
            return new BucketEntry(new AtomicInteger(1), now);
        }
        existing.count().incrementAndGet();
        return existing;
    });

    BucketEntry entry = buckets.get(key);
    return entry == null || entry.count().get() <= maxAttempts; // false → HTTP 429
}

// Nettoyage périodique toutes les 10 minutes
@Scheduled(fixedDelay = 600_000)
public void evictExpiredEntries() {
    Instant cutoff = Instant.now().minusSeconds(windowSeconds);
    buckets.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
}
```

**Endpoints protégés par le rate limiter :**
```java
// RateLimitingFilter.java
private static final Set<String> PROTECTED_PATHS = Set.of(
    "/api/auth/login",
    "/api/auth/verify-otp",
    "/api/auth/resend-otp",
    "/api/auth/forgot-password",
    "/api/auth/reset-password"
);
```

---

## 4.4 Spring Security Config

```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    // Routes publiques
    .requestMatchers(
        "/api/auth/register", "/api/auth/verify", "/api/auth/login",
        "/api/auth/verify-otp", "/api/auth/refresh", "/api/auth/forgot-password",
        "/api/auth/reset-password", "/actuator/health"
    ).permitAll()
    // Routes admin uniquement
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    // Routes analyste uniquement
    .requestMatchers("/api/dashboard/analyste/**").hasRole("ANALYSTE")
    .requestMatchers("/api/export/analyste/**").hasRole("ANALYSTE")
    // Routes communes
    .requestMatchers("/api/profil/**").hasAnyRole("ADMIN", "ANALYSTE")
    // Tout le reste : authentifié
    .anyRequest().authenticated()
)
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(jwtAuthFilter,      UsernamePasswordAuthenticationFilter.class);
```

---

# PARTIE 5 — API REST

## Tableau complet des endpoints

| Endpoint | Méthode | Rôle requis | Description |
|----------|---------|-------------|-------------|
| `/api/auth/register` | POST | Public | Inscription utilisateur |
| `/api/auth/verify` | GET | Public | Vérification email (token lien) |
| `/api/auth/login` | POST | Public | Connexion → déclenche OTP |
| `/api/auth/verify-otp` | POST | Public | Validation OTP → JWT |
| `/api/auth/resend-otp` | POST | Public | Renvoyer OTP |
| `/api/auth/refresh` | POST | Public | Renouveler Access Token |
| `/api/auth/logout` | POST | Authentifié | Révocation Refresh Token |
| `/api/auth/forgot-password` | POST | Public | Demande réinitialisation |
| `/api/auth/reset-password` | POST | Public | Réinitialisation avec token |
| `/api/profil/me` | GET | ANALYSTE/ADMIN | Profil utilisateur connecté |
| `/api/profil/update` | PUT | ANALYSTE/ADMIN | Mise à jour profil |
| `/api/import/manual/profile` | POST | ANALYSTE | Profiler fichier Excel (aperçu) |
| `/api/import/manual/preview` | POST | ANALYSTE | Prévisualiser import |
| `/api/import/manual/process` | POST | ANALYSTE | Lancer import complet |
| `/api/import/dual/profile` | POST | ANALYSTE | Profiler deux fichiers |
| `/api/import/dual/preview` | POST | ANALYSTE | Prévisualiser import dual |
| `/api/import/dual/process` | POST | ANALYSTE | Lancer import dual |
| `/api/import/manual/progress` | GET | Public (SSE) | Flux SSE progression |
| `/api/dashboard/analyste/comparatif/{id}` | GET | ANALYSTE | Tableau comparatif KPIs |
| `/api/dashboard/analyste/graphiques/{id}` | GET | ANALYSTE | Données graphiques |
| `/api/dashboard/analyste/alertes/{id}` | GET | ANALYSTE | Alertes KPIs critiques |
| `/api/analyse-ia/complet/{id}` | GET | ANALYSTE | Analyse IA complète |
| `/api/analyse-ia/globale/{id}` | GET | ANALYSTE | Synthèse globale |
| `/api/analyse-ia/categories/{id}` | GET | ANALYSTE | Analyses par catégorie |
| `/api/analyse-ia/structured/{id}` | GET | ANALYSTE | Analyse structurée JSON |
| `/api/analyse-ia/regenerer/{id}` | POST | ANALYSTE | Régénérer analyse IA |
| `/api/export/analyste/excel/{id}` | GET | ANALYSTE | Export Excel résultats |
| `/api/export/analyste/pdf/{id}` | GET | ANALYSTE | Export PDF rapport |
| `/api/admin/users` | GET | ADMIN | Liste utilisateurs |
| `/api/admin/rag` | GET/POST/DELETE | ADMIN | Gestion base RAG |
| `/api/admin/dashboard` | GET | ADMIN | Dashboard administrateur |

---

# PARTIE 6 — CONCEPTS THÉORIQUES

## 6.1 ETL (Extract, Transform, Load)

**Définition :** Processus en 3 phases pour intégrer des données depuis une source externe vers un système cible.

**Dans ce projet :**
- **Extract** : `ExtractionAgent` lit le fichier Excel (Apache POI), détecte les colonnes, produit des `KpiRawDataDTO`
- **Transform** : `CleaningAgent` normalise, `CalculationAgent` calcule les variations, `ClassificationEngine` enrichit la classification
- **Load** : Persistance en base via `ResultatKpiRepository.saveAll()` avec Hibernate batch

---

## 6.2 Pipeline en agents

**Définition :** Découpage d'un traitement complexe en agents indépendants et chaînés, chacun responsable d'une transformation précise.

**Dans ce projet :** 7 agents séquentiels orchestrés par `KpiProcessingOrchestratorService`. Chaque agent a une responsabilité unique (Single Responsibility Principle), peut être testé isolément, et produit un résultat passé à l'agent suivant.

---

## 6.3 Fuzzy Matching / Distance de Jaro-Winkler

**Définition :** Mesure de similarité entre deux chaînes de caractères, tolérante aux fautes de frappe et variations orthographiques.

**Jaro-Winkler :** Extension de Jaro qui bonus les correspondances en début de chaîne.

```java
// CalculationAgent.java — implémentation Jaro-Winkler
private double jaroWinkler(String s1, String s2) {
    double j = jaro(s1, s2);
    int prefix = 0;
    int limit = Math.min(Math.min(s1.length(), s2.length()), 4);
    for (int i = 0; i < limit; i++) {
        if (s1.charAt(i) == s2.charAt(i)) prefix++;
        else break;
    }
    return j + prefix * 0.1 * (1 - j); // Bonus préfixe commun
}
```

**Seuil utilisé :** `0.82` dans `CalculationAgent`, `0.82` semble être la valeur de production (la propriété `app.import.matching.fuzzy-threshold=0.85` est utilisée dans le pre-profiling).

**Exemple :** "Taux accidents travail" ↔ "Taux accident travail" → score ~0.97 → correspondance.

---

## 6.4 LLM (Large Language Models)

**Définition :** Modèles de langage entraînés sur de très grands corpus de texte, capables de générer du texte, analyser, résumer, raisonner.

**Dans ce projet :** Les LLMs ne calculent PAS les chiffres. Ils reçoivent les valeurs déjà calculées par le pipeline Java et génèrent des analyses textuelles, causes probables, recommandations et plans d'actions. Les LLMs utilisés :
- **Groq / LLaMA-3.3-70B** : 70 milliards de paramètres, très performant en JSON structuré
- **Groq / LLaMA-4-Scout** : modèle de recours
- **Google Gemini 2.0 Flash** : fallback global + génération d'embeddings

---

## 6.5 RAG (Retrieval-Augmented Generation)

**Définition :** Technique qui enrichit le prompt envoyé au LLM avec des documents pertinents récupérés depuis une base de connaissances externe, réduisant les hallucinations et ancrant les réponses dans des faits vérifiés.

**Flux :**
```
Question utilisateur (ex: "analyser TF1")
        │
        ▼
   EmbeddingService → vecteur 768d
        │
        ▼
   pgvector (SQL: SELECT ... ORDER BY embedding <=> query LIMIT 3)
        │
        ▼
   Chunks pertinents (formule TF1, seuils, réglementation ISO 45001)
        │
        ▼
   Injection dans le prompt LLM :
   "=== CONTEXTE MÉTIER QHSE ===\n• TF1: formule... | Seuils: ..."
        │
        ▼
   LLM génère réponse ancrée dans les faits
```

---

## 6.6 Embeddings vectoriels

**Définition :** Représentation numérique d'un texte sous forme d'un vecteur de réels (ici 768 dimensions). Des textes sémantiquement similaires produisent des vecteurs proches dans l'espace vectoriel.

**Dans ce projet :** `EmbeddingService` appelle l'API `gemini-embedding-2` de Google pour convertir chaque chunk RAG en un vecteur de 768 dimensions. Ces vecteurs sont stockés dans la colonne `embedding vector(768)` de `rag_knowledge`.

---

## 6.7 Similarité cosinus

**Définition :** Mesure l'angle entre deux vecteurs. Valeur entre -1 et 1 : 1 = identiques, 0 = orthogonaux, -1 = opposés.

**Formule :**
```
cosine_similarity(A, B) = (A · B) / (||A|| × ||B||)
```

**Dans pgvector :** L'opérateur `<=>` calcule la **distance cosinus** = `1 - similarité cosinus`. Donc `1 - (A <=> B)` donne la similarité. Le seuil `0.72` signifie : retourner uniquement les chunks avec similarité cosinus ≥ 72%.

---

## 6.8 pgvector

**Définition :** Extension PostgreSQL open-source ajoutant le type de données `vector` et des opérateurs de calcul de distance (cosinus, euclidienne, produit scalaire).

**Dans ce projet :**
```sql
-- Migration V3 : installation
CREATE EXTENSION IF NOT EXISTS vector;

-- Colonne vectorielle
ALTER TABLE rag_knowledge ADD COLUMN embedding vector(768);

-- Index IVFFlat pour accélérer les recherches de voisins approchés
CREATE INDEX IF NOT EXISTS rag_knowledge_embedding_idx
ON rag_knowledge USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

**Index IVFFlat :** Partitionne l'espace vectoriel en `lists=100` clusters (Inverted File). La recherche ne parcourt que les clusters les plus proches → beaucoup plus rapide qu'une recherche exhaustive sur de grands volumes.

---

## 6.9 JWT (JSON Web Token)

**Définition :** Standard RFC 7519 pour transmettre des informations de manière sécurisée entre parties sous forme de token JSON signé. Structure : `header.payload.signature` encodé en Base64.

**Dans ce projet :**
- Algorithme : **HS256** (HMAC-SHA256) avec clé secrète depuis `${JWT_SECRET}`
- Claims : `sub` (email), `role` (ROLE_ANALYSTE/ROLE_ADMIN), `iat` (émission), `exp` (expiration)
- Durée : **30 minutes** (access token)
- Transport : cookie HttpOnly ou header `Authorization: Bearer <token>`

---

## 6.10 OTP (One-Time Password)

**Définition :** Mot de passe à usage unique, valide pendant une durée limitée, envoyé sur un canal secondaire (email/SMS) pour la double authentification (2FA).

**Dans ce projet :**
```java
// Code OTP 6 chiffres
int code = random.nextInt(900000) + 100000; // Entre 100000 et 999999
// Validité : 10 minutes (app.otp.expiration-minutes=10)
// Stocké haché en base avec flag "used" pour éviter la réutilisation
```

---

## 6.11 SSE (Server-Sent Events)

**Définition :** Protocole HTTP qui permet au serveur de pousser des événements vers le client en continu via une connexion HTTP persistante (unidirectionnelle serveur→client).

**Dans ce projet :** Le endpoint `/api/import/manual/progress` utilise SSE pour notifier le frontend de l'avancement du pipeline d'import en temps réel (0% → 25% extraction → 50% calcul → 100% terminé).

---

## 6.12 Async Java (@Async, CompletableFuture)

**Définition :** `@Async` délègue l'exécution d'une méthode à un thread pool séparé, libérant le thread HTTP principal.

**Dans ce projet :**
```java
// AsyncConfig.java — deux pools dédiés
// aiAnalysisExecutor : 3 threads core, 10 max — pour les appels LLM (lents)
// importProcessingExecutor : 4 threads core, 8 max — pour le pipeline Excel

@Async("aiAnalysisExecutor")
public CompletableFuture<Void> triggerAnalyseAsync(Long importSessionId, Long userId) {
    genererToutesLesAnalyses(importSessionId, userId);
    return CompletableFuture.completedFuture(null);
}
```

L'import Excel retourne immédiatement une réponse HTTP au frontend, l'analyse IA démarre en arrière-plan dans `aiAnalysisExecutor`.

---

## 6.13 Hibernate Batch Processing

**Définition :** Regroupement de plusieurs opérations SQL INSERT/UPDATE en une seule requête (batch), réduisant drastiquement les allers-retours réseau avec la base de données.

**Configuration :**
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50  # Groupes de 50
spring.jpa.properties.hibernate.order_inserts=true   # Réordonner pour maximiser les batchs
spring.jpa.properties.hibernate.order_updates=true
```

**Impact :** Au lieu de 50 requêtes `INSERT INTO resultat_kpis ...`, Hibernate envoie 1 seule requête batch. Gain ~60% sur les temps de persistance post-import.

---

## 6.14 Flyway (migrations)

**Définition :** Outil de gestion des migrations de schéma de base de données versionnées. Chaque migration est un fichier SQL nommé `V{n}__{description}.sql`.

**17 migrations dans ce projet :**

| Migration | Description |
|-----------|-------------|
| V1 | Tables principales + 40+ index de performance |
| V2 | Colonnes stockage fichier (path, checksum, taille) |
| V3 | Extension pgvector, colonne embedding, index IVFFlat |
| V4 | Élargissement code categorie_kpi à VARCHAR(10) |
| V5 | Table admin_audit_log |
| V6 | Table ai_config (température LLM runtime) |
| V7 | Colonne direction dans rag_knowledge |
| V8 | Colonne chunk_type dans rag_knowledge |
| V9 | Index composé (kpi_name, chunk_type) sur rag_knowledge |
| V10 | Backfill periodes dans resultat_kpis |
| V11 | Normalisation noms colonnes legacy |
| V12 | Extension enum niveau_variation (EXCELLENT, PRE_ESCALADE) |
| V15 | Nullable valeur_n1/valeur_n pour import dual |
| V16 | Flag is_system_admin sur users |
| V17 | Colonne structured_response_json sur analyse_globales |

---

## 6.15 CRISP-DM (méthodologie)

**Définition :** Cross-Industry Standard Process for Data Mining — méthodologie en 6 phases pour les projets data : Compréhension métier → Compréhension données → Préparation données → Modélisation → Évaluation → Déploiement.

**Correspondance dans ce projet :**
- **Compréhension métier** : domaine QHSE, normes ISO 9001/14001/45001
- **Compréhension données** : profiling Excel (ExtractionAgent), rapport qualité
- **Préparation** : CleaningAgent, normalisation, détection outliers
- **Modélisation** : ClassificationEngine (règles métier), module IA (LLM+RAG)
- **Évaluation** : StructuredAnalysisValidator, score de confiance, coverage KPIs
- **Déploiement** : API REST + Docker Compose

---

# PARTIE 7 — PRÉPARATION SOUTENANCE

## 7.1 Questions techniques probables + réponses

---

### ARCHITECTURE

**Q : Pourquoi Java Spring Boot et pas Python (FastAPI/Django) ?**
R : Spring Boot offre un écosystème mature pour les applications enterprise avec une gestion robuste des transactions (`@Transactional`), un framework de sécurité complet (Spring Security), et une intégration native avec les bases de données relationnelles (Hibernate/JPA). Python est excellent pour le data science, mais pour une API REST transactionnelle avec authentification, rôles, et gestion d'état complexe, Spring Boot est plus adapté. De plus, le traitement Apache POI des fichiers Excel est nativement Java.

**Q : Pourquoi Angular et pas React ou Vue ?**
R : Angular est un framework "opinionated" qui impose une architecture claire (modules, services, composants standalone), ce qui est bénéfique pour un projet structuré. Il offre un typage fort avec TypeScript natif, un système de gestion des formulaires réactifs (pour le mapping des colonnes), et un HttpClient puissant. Angular 19 avec les standalone components allège considérablement l'architecture.

**Q : Pourquoi PostgreSQL et pas MongoDB ?**
R : Les données QHSE sont fortement relationnelles (KPIs → catégories, résultats → sessions, analyses → résultats). PostgreSQL avec JPA garantit la cohérence transactionnelle (ACID). De plus, l'extension **pgvector** permet de stocker les embeddings directement dans PostgreSQL sans déployer une base vectorielle séparée (Pinecone, Weaviate...).

**Q : Qu'est-ce qu'un DTO ? Pourquoi les utiliser ?**
R : DTO (Data Transfer Object) est un objet dont le seul rôle est de transporter des données entre couches. Il découple le modèle de domaine (entité JPA) de la représentation API (JSON). Avantages : évite d'exposer des champs sensibles (mot de passe haché), permet d'adapter la forme des données selon le contexte, et protège contre les attaques de masse assignment. Exemple : `KpiCalculatedDTO` contient les données calculées à afficher, pas les entités JPA complètes.

---

### PIPELINE

**Q : Expliquez le pipeline de traitement de A à Z.**
R : 1) **Validation** (ExcelFileValidator) : taille ≤15MB, extension, magic bytes. 2) **Extraction** (ExtractionAgent) : Apache POI lit le fichier, détecte les en-têtes par synonymes, produit des `KpiRawDataDTO`. 3) **Nettoyage** (CleaningAgent) : normalisation, doublons, outliers. 4) **Calcul** (CalculationAgent + ComparativeCalculator) : variation absolue et relative, Jaro-Winkler matching sur le référentiel KPI. 5) **Classification** (ClassificationEngine) : 6 niveaux basés sur les seuils absolus et l'historique statistique. 6) **Enrichissement** (MetadataEnrichmentAgent) : catégorie, unité, définition. 7) **Risque** (RiskDetectionAgent) : probabilité × impact. 8) **Visualisation** (VisualizationAgent) : données graphiques. 9) **Analyse IA** (AnalysisAgent) : appel LLM avec contexte RAG. 10) **Persistance** en base PostgreSQL.

**Q : Comment détectez-vous automatiquement les colonnes dans un fichier Excel ?**
R : L'`ExtractionAgent` parcourt les 50 premières lignes du fichier. Pour chaque ligne, il normalise le texte de chaque cellule (minuscules, sans accents) et le compare à des sets de synonymes prédéfinis : `KPI_SYNONYMS` (kpi, indicateur, libelle...), `VALUE_N_SYNONYMS` (n, valeur n, annee n...), `VALUE_N1_SYNONYMS` (n-1, valeur precedente...). Quand suffisamment de synonymes sont trouvés, cette ligne est identifiée comme l'en-tête. Si la détection automatique échoue, l'utilisateur peut fournir un mapping manuel (indices de colonnes).

**Q : Qu'est-ce que le fuzzy matching ? Quel algorithme ? Quel seuil ?**
R : Le fuzzy matching permet de reconnaître deux chaînes comme similaires malgré des différences mineures (fautes de frappe, abréviations). L'algorithme utilisé est **Jaro-Winkler**, qui mesure la similarité en tenant compte des transpositions de caractères et bonus les correspondances en début de chaîne. Le seuil de validation est `0.82` dans le `CalculationAgent`. Exemple : "taux accident travail" vs "tauxaccidenttravail" → score 0.94 → correspondance acceptée.

**Q : Comment fonctionne la classification ? Pourquoi "position absolue prime sur variation" ?**
R : La classification utilise un arbre de décision. Si des seuils KPI sont définis, la **position absolue** de la valeur N dans les zones (excellent/faible/modéré/critique) prime sur la variation relative. La raison : une amélioration de 20% peut sembler bonne, mais si la valeur reste en zone critique (ex: 8 accidents avec seuil critique à 10), elle est toujours critique. Inversement, une légère dégradation dans la zone optimale reste "faible". Sans seuils, on utilise les percentiles historiques (p95/p75) ou le Robust Z-Score (MAD) pour détecter les anomalies statistiques.

**Q : Qu'est-ce que le mode Dual File ?**
R : Le mode Dual permet d'importer deux fichiers séparés : un pour N-1, un pour N. Le `DualFileImportService` extrait les valeurs de chaque fichier, normalise les noms de KPIs (minuscules, sans accents, sans caractères spéciaux), puis fusionne par nom normalisé. Si un KPI est présent dans un seul fichier, il est marqué `valid=false` et classé `INDETERMINE`. Une garde anti-inversion détecte si la colonne KPI-Name contient des nombres (inversion accidentelle des colonnes).

**Q : Comment gérez-vous les valeurs manquantes ?**
R : Trois niveaux. 1) **Valeur N ou N-1 null** → `ComparativeCalculator` retourne `specialCase="NA"` → `ClassificationEngine` classe `INDETERMINE`. 2) **Les deux nulles** → idem `NA`. 3) **N-1=0 et N>0** → selon la direction du KPI : `STRONG_IMPROVEMENT` (amélioration) ou `EMERGING_RISK` (risque émergent). Dans tous les cas, le flag `MISSING_CONTEXT` est ajouté et le score de confiance baisse de 25 points.

---

### IA

**Q : L'IA calcule-t-elle les chiffres ?**
R : **Non**. L'IA ne fait aucun calcul arithmétique. Tous les chiffres (valeurs N/N-1, variations, classifications) sont calculés par le pipeline Java (ComparativeCalculator, ClassificationEngine). Le LLM reçoit ces chiffres déjà calculés et génère des analyses textuelles : interprétation, causes probables, recommandations, plans d'action. Cette séparation garantit la précision des données et évite les hallucinations sur les chiffres.

**Q : Qu'est-ce que le RAG ? Pourquoi l'avez-vous utilisé ?**
R : Le RAG (Retrieval-Augmented Generation) est une technique qui enrichit le prompt du LLM avec des documents pertinents récupérés depuis une base de connaissances. Sans RAG, le LLM pourrait inventer des seuils incorrects, citer de mauvaises normes ISO, ou donner des recommandations génériques. Avec notre base RAG de 32 KPIs × 4 chunks (formule, interprétation, action, réglementation), le LLM a accès aux définitions exactes, seuils réels et normes applicables à chaque indicateur.

**Q : Qu'est-ce qu'un embedding vectoriel ?**
R : Un embedding est une représentation numérique d'un texte sous forme d'un vecteur de réels en haute dimension (768 dans notre cas). Des textes sémantiquement proches produisent des vecteurs proches. Par exemple, "taux d'accidents" et "fréquence des accidents du travail" auraient des vecteurs très proches même si les mots exacts diffèrent. On utilise le modèle `gemini-embedding-2` de Google pour générer ces vecteurs.

**Q : Comment fonctionne la recherche sémantique ?**
R : 1) La requête (noms des KPIs à analyser) est convertie en vecteur 768d par `EmbeddingService`. 2) pgvector calcule la distance cosinus entre ce vecteur et tous les vecteurs de `rag_knowledge`. 3) Les chunks avec similarité ≥ 0.72 sont retournés (max 3 par défaut). 4) Si aucun résultat, le seuil est assoupli à 0.50. 5) Si pgvector est indisponible, recherche par mots-clés ILIKE. Les chunks pertinents sont injectés dans le prompt LLM.

**Q : Qu'est-ce que pgvector ? Quel type d'index avez-vous utilisé ?**
R : pgvector est une extension PostgreSQL qui ajoute le type `vector(n)` et des opérateurs de distance (cosinus `<=>`, euclidienne `<->`, produit scalaire `<#>`). Nous utilisons l'index **IVFFlat** (Inverted File Flat) avec `lists=100` : il partitionne l'espace vectoriel en 100 clusters. La recherche ne parcourt que les clusters les plus proches de la requête, ce qui est beaucoup plus rapide qu'une recherche exhaustive (O(n) → O(sqrt(n)) environ).

**Q : Pourquoi Groq et pas OpenAI ?**
R : Groq offre une inférence extrêmement rapide grâce à ses puces LPU (Language Processing Unit) dédiées, avec une latence ~10× inférieure à OpenAI pour des modèles équivalents. De plus, Groq propose un tier gratuit généreux, permettant le développement sans coût prohibitif. L'API est compatible OpenAI, donc la migration serait triviale. OpenAI GPT-4 est plus puissant mais nettement plus cher et plus lent pour notre cas d'usage.

**Q : Comment fonctionne le fallback Gemini ?**
R : `LlmProviderChain` essaie d'abord Groq. En cas d'échec (429 rate limit, timeout, erreur réseau), `ProviderCooldownManager` met Groq en "cooldown" pour N secondes (durée extraite du header `Retry-After` ou 60s par défaut). Pendant le cooldown, les appels vont directement à Gemini (`geminiService.generateRaw(prompt)`). Quand le cooldown expire, Groq redevient prioritaire.

**Q : Comment évitez-vous les hallucinations ?**
R : Plusieurs mécanismes : 1) **RAG** : ancre les analyses dans des données réelles (formules, seuils, normes). 2) **Données précalculées** : le LLM reçoit les chiffres exacts (N-1, N, Δ%), il ne les calcule pas. 3) **Temperature JSON = 0.1** : très basse pour des réponses structurées déterministes. 4) **StructuredAnalysisValidator** : valide le JSON retourné (champs obligatoires, valeurs numériques dans plages valides, cohérence). 5) **Retry avec prompt corrigé** : si la validation échoue, le LLM reçoit un prompt renforcé avec les erreurs spécifiques.

**Q : Qu'est-ce que le cache SHA-256 des analyses ?**
R : Avant d'appeler le LLM, `LlmProviderChain` calcule une clé de cache = `SHA-256(prefix + prompt)`. Si cette clé est en cache (Caffeine, TTL 24h), la réponse est retournée sans aucun appel réseau. Le prefix exclut le sessionId pour permettre les cache hits entre ré-analyses du même fichier. Le bypass est possible (`bypassCache=true`) lors de la régénération manuelle par l'utilisateur.

---

### SÉCURITÉ

**Q : Comment fonctionne l'authentification JWT ?**
R : Flux en deux étapes : 1) Login (email+password) → vérification BCrypt → génération OTP → envoi email. 2) Vérification OTP → génération Access Token JWT HS256 (30 min) + Refresh Token en base (1h ou 7j). Le JWT contient le claim `role`. À chaque requête, `JwtAuthFilter` extrait le token (cookie ou header Authorization), vérifie la signature HMAC, extrait l'email, charge l'utilisateur depuis la base, valide non-expiration. Architecture stateless : aucune session serveur.

**Q : Qu'est-ce qu'un refresh token ? Pourquoi la rotation ?**
R : Le refresh token est un token longue durée (1h ou 7j) stocké en base, utilisé pour obtenir de nouveaux access tokens sans re-saisir les credentials. La **rotation** signifie qu'à chaque utilisation, l'ancien refresh token est révoqué et un nouveau est émis. Cela permet de détecter le vol : si un attaquant vole et utilise un refresh token, la prochaine tentative légitime échouera (token révoqué), alertant l'utilisateur. La révocation à la déconnexion annule immédiatement la session.

**Q : Qu'est-ce que l'OTP ? Durée de validité ?**
R : OTP (One-Time Password) = code à 6 chiffres, généré aléatoirement (`Random.nextInt(900000) + 100000`), valide **10 minutes** (`app.otp.expiration-minutes=10`), à usage unique (flag `used=true` après vérification). Il constitue le 2ème facteur d'authentification envoyé par email. Même si le mot de passe est compromis, l'attaquant ne peut pas se connecter sans accès à la boîte email.

**Q : Comment protégez-vous contre le brute-force ?**
R : `InMemoryRateLimiter` + `RateLimitingFilter` : max **5 tentatives** par IP et par endpoint dans une fenêtre de **300 secondes** (5 minutes). Au-delà → HTTP 429 "Too Many Requests". Les endpoints protégés : `/login`, `/verify-otp`, `/resend-otp`, `/forgot-password`, `/reset-password`. Le bucket est indexé par `IP + endpoint`, stocké en `ConcurrentHashMap`, nettoyé toutes les 10 minutes.

---

### PERFORMANCE

**Q : Qu'est-ce que le Hibernate batch ? Quel gain de performance ?**
R : Sans batch, Hibernate envoie N requêtes SQL INSERT séparées pour N `ResultatKpi`. Avec `batch_size=50`, il regroupe les 50 premiers INSERT en une seule requête SQL multi-values, puis les 50 suivants, etc. Combiné avec `order_inserts=true` (tri par table pour maximiser les batchs), le gain est d'environ **60% de réduction du nombre de requêtes SQL** lors de la persistance post-import.

**Q : Comment fonctionne le cache des analyses IA ?**
R : Double cache : 1) **Cache LLM SHA-256** (Caffeine, 24h) : évite de rappeler l'API LLM pour les mêmes données. 2) **Cache JSON en base** (colonne `structured_response_json` dans `analyse_globales`) : la première consultation génère et persiste le JSON structuré ; les consultations suivantes désérialisent directement depuis la base sans appel LLM.

**Q : Pourquoi l'analyse structured est-elle persistée en base ?**
R : L'analyse structurée est coûteuse : appel LLM (latence 5-15s), plusieurs chunks, possible retry. Si elle n'était pas persistée, chaque consultation du dashboard IA rechargerait le LLM. Avec la persistance dans `structured_response_json`, la première génération (post-import ou première consultation) stocke le JSON, et toutes les consultations suivantes lisent depuis PostgreSQL en ~1ms.

**Q : Qu'est-ce que la race condition que vous avez résolue et comment ?**
R : Sans protection, deux threads pouvaient déclencher simultanément `genererToutesLesAnalyses` pour la même session : au retour du processus d'import ET depuis un appel manuel du frontend. La solution : dans `triggerAnalyseAsync()`, avant de lancer l'analyse, on vérifie si une analyse existe déjà :
```java
boolean alreadyStored = analyseGlobaleRepository.findByImportSessionId(id).isPresent()
    || !analyseCategorieRepository.findByImportSessionId(id).isEmpty()
    || resultats.stream().anyMatch(r -> r.getAnalyseIa() != null);
if (alreadyStored) { log.info("...skip."); return; }
```
Si déjà présente, le thread async s'arrête sans dupliquer les analyses.

---

### BASE DE DONNÉES

**Q : Expliquez le schéma de base de données.**
R : Le schéma est centré sur `import_sessions` (session d'import d'un analyste). Chaque session contient N `resultat_kpis` (un par KPI traité), une `analyse_globale` (synthèse IA), et plusieurs `analyse_categories` (Q/H/S/E). Les `kpis` forment le référentiel avec leurs `categorie_kpis`. La table `rag_knowledge` est indépendante et stocke les chunks vectoriels. `users` est référencé par import_sessions et refresh_tokens.

**Q : Qu'est-ce que Flyway ? Combien de migrations ?**
R : Flyway est un outil de migration de schéma de base de données. Il maintient une table `flyway_schema_history` traçant quelle migration a été appliquée. Au démarrage, Spring Boot vérifie les migrations non appliquées et les exécute dans l'ordre. Il y a **17 migrations** (V1 à V17) couvrant la création des tables, l'ajout de pgvector, les index de performance, et les évolutions du schéma.

**Q : Pourquoi avoir rendu certaines colonnes nullable ?**
R : La migration V15 rend `valeur_n1` et `valeur_n` nullable dans `resultat_kpis`. Cette évolution est nécessaire pour supporter le **mode Dual File** : quand un KPI est absent d'un des deux fichiers, sa valeur est null. La contrainte NOT NULL originale empêchait de persister ces cas INDETERMINE.

**Q : Comment gérez-vous la suppression en cascade ?**
R : Les entités enfants (ResultatKpi, AnalyseGlobale, AnalyseCategorie) utilisent `@ManyToOne` vers `ImportSession`. La suppression en cascade est gérée par des méthodes de repository (`deleteByImportSessionId`) plutôt que par `cascade = CascadeType.ALL`, pour éviter les suppressions accidentelles et contrôler précisément l'ordre de suppression.

---

### DÉPLOIEMENT

**Q : Comment déployer l'application ?**
R : L'application se déploie via Docker Compose : 3 services — `postgres` (PostgreSQL 16 + pgvector), `backend` (Spring Boot JAR), `frontend` (Angular buildé servi par Nginx). Les variables sensibles (JWT secret, clés API Groq/Gemini, credentials DB) sont injectées via `.env`. Flyway s'exécute automatiquement au démarrage du backend pour créer/migrer le schéma.

**Q : Qu'est-ce que Docker Compose ?**
R : Docker Compose est un outil qui permet de définir et exécuter des applications multi-conteneurs. Chaque service (postgres, backend, frontend) s'exécute dans son propre conteneur isolé, avec un réseau interne partagé. Le fichier `docker-compose.yml` décrit les images, ports, volumes, variables d'environnement et dépendances entre services (`depends_on: postgres`).

---

## 7.2 Points forts à mettre en avant

1. **Pipeline automatique complet** — 0 intervention manuelle de l'extraction à l'analyse IA, 9 agents chaînés
2. **Architecture dual-mode** — deux architectures d'import supportées (1 fichier / 2 fichiers) avec fusion intelligente
3. **Base de connaissances RAG QHSE** — 32 KPIs × 4 types de chunks = 128+ vecteurs ancrés dans les normes ISO
4. **Résilience IA automatique** — fallback Groq → Gemini transparent, rotation de clés API, cooldown dynamique
5. **Double cache intelligent** — SHA-256 in-memory (Caffeine 24h) + JSON persisté en base → zéro appel LLM redondant
6. **Classification par position absolue** — innovation vs classification par variation simple : cohérence métier garantie même lors d'améliorations en zone critique
7. **Sécurité enterprise** — JWT HS256 + OTP 2FA + refresh token rotation + rate limiting IP/endpoint
8. **Optimisations performance** — Hibernate batch 50, préchargement anti N+1, ~60% de réduction des requêtes SQL
9. **Clean architecture** — pattern Agent (SRP), découplage couches, DTO/entités séparés
10. **17 migrations Flyway versionnées** — traçabilité complète de l'évolution du schéma

---

## 7.3 Démonstration technique recommandée

### 1. ClassificationEngine.classify() — logique métier

Montrer le code de `computeAbsolutePosition()` et expliquer :
> "Ici, pour un KPI LOWER_IS_BETTER comme le taux d'accidents, si la valeur N est 7 et le seuil critique est 10 — même si on s'est amélioré de 30% — la valeur est encore en zone MODERE, donc la classification retourne MODERE. La position absolue prime sur la variation relative."

### 2. StructuredAnalysisPromptBuilder.buildPrompt() — ingénierie de prompt

Montrer le `SYSTEM_PROMPT` et le `FEW_SHOT_EXAMPLE`, expliquer :
> "Le système prompt impose des règles absolues : JSON uniquement, minimum 20 mots par champ, verbe d'action obligatoire. L'exemple few-shot montre au LLM exactement le niveau de détail attendu avec un exemple concret de TF1."

### 3. RagSearchService.findRelevant() — recherche vectorielle

Montrer la requête SQL pgvector :
```sql
WHERE (1 - (embedding <=> ?::vector)) >= 0.72
ORDER BY embedding <=> ?::vector LIMIT 3
```
> "L'opérateur `<=>` calcule la distance cosinus. On filtre les chunks avec similarité ≥ 72%. L'index IVFFlat rend ça rapide même sur des milliers de vecteurs."

### 4. LlmProviderChain.generate() — fallback IA

Montrer la logique `callProviders()` et `ProviderCooldownManager` :
> "Si Groq répond 429, setCooldown le met en pause 60 secondes. L'appel suivant va directement vers Gemini. Après 60 secondes, Groq redevient prioritaire. Tout est transparent pour l'application."

### 5. genererToutesLesAnalyses() — flux complet

Montrer les étapes dans l'ordre : analyzeStrict → persistance par KPI → AnalyseGlobale → AnalyseCategorie → analyzeStructured → persistance JSON → setStatut(TRAITE).

---

## 7.4 Glossaire technique

| Terme | Définition courte |
|-------|-------------------|
| **KPI** | Key Performance Indicator — indicateur chiffré mesurant la performance |
| **QHSE** | Qualité, Hygiène, Sécurité, Environnement — domaine de gestion des risques |
| **ETL** | Extract, Transform, Load — pipeline d'intégration de données |
| **LLM** | Large Language Model — modèle IA de traitement du langage naturel |
| **RAG** | Retrieval-Augmented Generation — génération IA enrichie par recherche documentaire |
| **Embedding** | Représentation vectorielle d'un texte (768 dimensions ici) |
| **pgvector** | Extension PostgreSQL pour le stockage et la recherche de vecteurs |
| **Similarité cosinus** | Mesure de l'angle entre deux vecteurs (0=différent, 1=identique) |
| **IVFFlat** | Index de recherche approximative par partitionnement en clusters |
| **JWT** | JSON Web Token — token signé pour l'authentification stateless |
| **OTP** | One-Time Password — code à usage unique pour la 2FA |
| **SSE** | Server-Sent Events — push serveur vers client via HTTP |
| **Fuzzy Matching** | Correspondance approximative de chaînes (tolère les fautes) |
| **Jaro-Winkler** | Algorithme de distance entre chaînes, bonus préfixe commun |
| **Levenshtein** | Nombre minimal d'opérations pour transformer une chaîne en une autre |
| **Hibernate** | ORM Java — mapping objet-relationnel avec gestion transactions |
| **JPA** | Java Persistence API — standard Java pour la persistance objet |
| **Flyway** | Outil de migration versionnée de schéma de base de données |
| **CRISP-DM** | Méthodologie standard pour les projets data mining en 6 phases |
| **Batch** | Traitement groupé de N opérations en une seule requête SQL |
| **BCrypt** | Algorithme de hachage de mots de passe avec sel intégré |
| **HS256** | HMAC-SHA256 — algorithme de signature symétrique pour JWT |
| **CORS** | Cross-Origin Resource Sharing — politique de partage de ressources inter-domaines |
| **DTO** | Data Transfer Object — objet de transport entre couches |
| **SRP** | Single Responsibility Principle — un composant = une responsabilité |
| **Caffeine** | Bibliothèque Java de cache in-memory haute performance (LRU/LFU) |
| **Micrometer** | Façade de métriques pour Spring Boot (Prometheus, Grafana) |
| **Apache POI** | Bibliothèque Java pour lire/écrire les fichiers Microsoft Office |
| **iText7** | Bibliothèque Java pour générer des PDFs |
| **Stateless** | Architecture sans état serveur — chaque requête est auto-suffisante |
| **Rate Limiting** | Limitation du nombre de requêtes par IP/unité de temps |
| **Magic Bytes** | Premiers octets d'un fichier identifiant son format réel |
| **N+1 Problem** | Anti-pattern : 1 requête pour la liste + N requêtes pour les détails |
| **MAD** | Median Absolute Deviation — mesure robuste de dispersion statistique |
| **Robust Z-Score** | Score standardisé résistant aux outliers (utilise la médiane et MAD) |
| **INDETERMINE** | Niveau de classification quand les données sont insuffisantes |
| **PRE_ESCALADE** | Niveau de classification précritique, surveillance immédiate requise |

---

*Document généré le 2026-05-19 — QHSE Analytics v1.0*
*Stack : Java 21 | Spring Boot 3.3.5 | Angular 19 | PostgreSQL 16 + pgvector | Groq LLaMA | Google Gemini*
