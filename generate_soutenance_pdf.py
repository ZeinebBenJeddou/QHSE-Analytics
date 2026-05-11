# -*- coding: utf-8 -*-
from fpdf import FPDF
from fpdf.enums import XPos, YPos

FONT_REGULAR = r"C:\Windows\Fonts\arial.ttf"
FONT_BOLD    = r"C:\Windows\Fonts\arialbd.ttf"
FONT_ITALIC  = r"C:\Windows\Fonts\ariali.ttf"

BLUE_DARK   = (26,  57,  96)
BLUE_MID    = (41,  98,  158)
BLUE_LIGHT  = (219, 234, 254)
ACCENT      = (200, 70,  10)
GREEN       = (22,  163, 74)
GRAY_DARK   = (51,  51,  51)
GRAY_LIGHT  = (240, 242, 245)
WHITE       = (255, 255, 255)
BORDER_GRAY = (200, 200, 200)

L_MARGIN = 15   # left margin mm
R_MARGIN = 15   # right margin mm
PAGE_W   = 210
CONTENT_W = PAGE_W - L_MARGIN - R_MARGIN   # 180 mm


class PDF(FPDF):
    def __init__(self):
        super().__init__()
        self.set_margins(L_MARGIN, 22, R_MARGIN)
        self.add_font("Arial",  "",  FONT_REGULAR)
        self.add_font("Arial",  "B", FONT_BOLD)
        self.add_font("Arial",  "I", FONT_ITALIC)
        self.set_auto_page_break(auto=True, margin=20)
        self._chapter = ""

    # ── HEADER / FOOTER ────────────────────────────────────────
    def header(self):
        if self.page_no() == 1:
            return
        self.set_fill_color(*BLUE_DARK)
        self.rect(0, 0, PAGE_W, 13, "F")
        self.set_font("Arial", "B", 8)
        self.set_text_color(*WHITE)
        self.set_xy(L_MARGIN, 2.5)
        self.cell(CONTENT_W / 2, 8, "QHSE Analytics - Guide Technique Soutenance",
                  new_x=XPos.RIGHT, new_y=YPos.TOP)
        self.cell(CONTENT_W / 2, 8, self._chapter, align="R",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*GRAY_DARK)

    def footer(self):
        if self.page_no() == 1:
            return
        self.set_y(-14)
        self.set_draw_color(*BORDER_GRAY)
        self.set_line_width(0.3)
        self.line(L_MARGIN, self.get_y(), PAGE_W - R_MARGIN, self.get_y())
        self.set_font("Arial", "", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f"Page {self.page_no()}", align="C",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # ── LAYOUT HELPERS ─────────────────────────────────────────
    def chapter_title(self, num, title):
        self._chapter = title
        self.add_page()
        self.set_fill_color(*BLUE_DARK)
        self.rect(0, 13, PAGE_W, 26, "F")
        self.set_font("Arial", "B", 17)
        self.set_text_color(*WHITE)
        self.set_xy(L_MARGIN, 19)
        self.cell(CONTENT_W, 12, f"{num}.  {title}",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*GRAY_DARK)
        self.ln(12)

    def section(self, title):
        self.set_font("Arial", "B", 12)
        self.set_text_color(*BLUE_MID)
        self.cell(CONTENT_W, 8, title,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_draw_color(*BLUE_MID)
        self.set_line_width(0.5)
        self.line(L_MARGIN, self.get_y(), PAGE_W - R_MARGIN, self.get_y())
        self.ln(4)
        self.set_text_color(*GRAY_DARK)

    def subsection(self, title):
        self.set_font("Arial", "B", 10.5)
        self.set_text_color(*ACCENT)
        self.cell(CONTENT_W, 7, f">> {title}",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*GRAY_DARK)
        self.ln(1)

    def body(self, text):
        self.set_font("Arial", "", 10)
        self.set_text_color(*GRAY_DARK)
        self.multi_cell(CONTENT_W, 6, text)
        self.ln(2)

    def bullet(self, items):
        self.set_font("Arial", "", 10)
        self.set_text_color(*GRAY_DARK)
        indent = 5
        bullet_w = 5
        text_w = CONTENT_W - indent - bullet_w
        for item in items:
            self.set_x(L_MARGIN + indent)
            self.cell(bullet_w, 6, "-", new_x=XPos.RIGHT, new_y=YPos.TOP)
            self.multi_cell(text_w, 6, item)
        self.ln(2)

    def numbered(self, items):
        self.set_font("Arial", "", 10)
        self.set_text_color(*GRAY_DARK)
        indent = 5
        num_w = 8
        text_w = CONTENT_W - indent - num_w
        for i, item in enumerate(items, 1):
            self.set_x(L_MARGIN + indent)
            self.cell(num_w, 6, f"{i}.", new_x=XPos.RIGHT, new_y=YPos.TOP)
            self.multi_cell(text_w, 6, item)
        self.ln(2)

    def code_block(self, lines):
        self.set_font("Arial", "", 8.5)
        self.set_text_color(30, 30, 30)
        line_h = 5.2
        padding = 4
        total_h = len(lines) * line_h + padding * 2
        y0 = self.get_y()
        # Check page break
        if y0 + total_h > self.page_break_trigger:
            self.add_page()
            y0 = self.get_y()
        self.set_fill_color(*GRAY_LIGHT)
        self.set_draw_color(*BORDER_GRAY)
        self.set_line_width(0.3)
        self.rect(L_MARGIN, y0, CONTENT_W, total_h, "FD")
        self.set_xy(L_MARGIN + 3, y0 + padding)
        for line in lines:
            self.cell(CONTENT_W - 6, line_h, line,
                      new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_x(L_MARGIN + 3)
        self.set_text_color(*GRAY_DARK)
        self.ln(4)

    def info_box(self, title, text):
        self.set_fill_color(*BLUE_LIGHT)
        self.set_draw_color(*BLUE_MID)
        self.set_line_width(0.5)
        self.set_font("Arial", "B", 9.5)
        self.set_text_color(*BLUE_DARK)
        self.multi_cell(CONTENT_W, 6, f"  {title}", fill=True)
        self.set_font("Arial", "", 9.5)
        self.set_text_color(*GRAY_DARK)
        self.set_fill_color(*BLUE_LIGHT)
        self.multi_cell(CONTENT_W, 6, f"  {text}", fill=True)
        self.ln(3)

    def qa_block(self, question, answer):
        self.set_font("Arial", "B", 10)
        self.set_fill_color(255, 244, 230)
        self.multi_cell(CONTENT_W, 6, f"  Q : {question}", fill=True)
        self.set_font("Arial", "", 10)
        self.set_fill_color(240, 248, 255)
        self.multi_cell(CONTENT_W, 6, f"  R : {answer}", fill=True)
        self.ln(3)


# ══════════════════════════════════════════════════════════════
def build():
    pdf = PDF()

    # ── COVER ──────────────────────────────────────────────────
    pdf.add_page()
    pdf.set_fill_color(*BLUE_DARK)
    pdf.rect(0, 0, PAGE_W, 297, "F")

    pdf.set_font("Arial", "B", 34)
    pdf.set_text_color(*WHITE)
    pdf.set_xy(0, 65)
    pdf.multi_cell(PAGE_W, 15, "QHSE Analytics", align="C")

    pdf.set_font("Arial", "B", 17)
    pdf.set_text_color(180, 210, 255)
    pdf.multi_cell(PAGE_W, 10, "Guide Technique Complet - Soutenance", align="C")

    pdf.set_font("Arial", "", 12)
    pdf.set_text_color(200, 220, 255)
    pdf.ln(8)
    pdf.multi_cell(PAGE_W, 7, "Architecture  *  Backend Spring Boot  *  Frontend Angular", align="C")
    pdf.multi_cell(PAGE_W, 7, "LLMs (Groq / Gemini)  *  RAG (pgvector)  *  Securite", align="C")

    pdf.set_font("Arial", "I", 11)
    pdf.set_text_color(150, 180, 220)
    pdf.ln(18)
    pdf.multi_cell(PAGE_W, 8, "Zeineb Ben Jeddou  -  2026", align="C")

    pdf.set_fill_color(*ACCENT)
    pdf.rect(0, 284, PAGE_W, 13, "F")
    pdf.set_text_color(*WHITE)
    pdf.set_font("Arial", "B", 9)
    pdf.set_xy(0, 287)
    pdf.cell(PAGE_W, 8, "Confidentiel - Usage interne soutenance", align="C",
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # ── SOMMAIRE ───────────────────────────────────────────────
    pdf._chapter = "Sommaire"
    pdf.add_page()
    pdf.set_fill_color(*BLUE_DARK)
    pdf.rect(0, 13, PAGE_W, 22, "F")
    pdf.set_font("Arial", "B", 15)
    pdf.set_text_color(*WHITE)
    pdf.set_xy(L_MARGIN, 17)
    pdf.cell(CONTENT_W, 14, "Table des matieres",
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_text_color(*GRAY_DARK)
    pdf.ln(8)

    chapters = [
        ("1",  "Vue d'ensemble du projet"),
        ("2",  "Architecture globale et stack technique"),
        ("3",  "Base de donnees - Schema et migrations Flyway"),
        ("4",  "Securite - JWT, OTP, Rate Limiting"),
        ("5",  "Pipeline d'import Excel (8 etapes)"),
        ("6",  "Calcul KPI et Classification"),
        ("7",  "Integration LLM - Groq"),
        ("8",  "Chaine LLM avec Fallback (LlmProviderChain)"),
        ("9",  "Google Gemini - Fallback et Embeddings"),
        ("10", "Systeme RAG - pgvector et Recherche Vectorielle"),
        ("11", "Prompt Engineering et Reponse Structuree"),
        ("12", "Frontend Angular - Architecture et Signals"),
        ("13", "Composant Analyse IA (654 lignes)"),
        ("14", "Cache, Performance et Metriques"),
        ("15", "Deploiement Docker"),
        ("16", "Questions-Reponses Soutenance"),
    ]
    for num, title in chapters:
        pdf.set_font("Arial", "", 11)
        pdf.set_x(L_MARGIN)
        pdf.cell(12, 8, num + ".", new_x=XPos.RIGHT, new_y=YPos.TOP)
        pdf.cell(CONTENT_W - 12, 8, title,
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.set_draw_color(*BORDER_GRAY)
        pdf.set_line_width(0.2)
        pdf.line(L_MARGIN, pdf.get_y(), PAGE_W - R_MARGIN, pdf.get_y())

    # ══════════════════════════════════════════════════════════
    # CH 1 — VUE D'ENSEMBLE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("1", "Vue d'ensemble du projet")

    pdf.section("Objectif")
    pdf.body(
        "QHSE Analytics est une plateforme SaaS d'analyse des indicateurs QHSE "
        "(Qualite, Hygiene, Securite, Environnement) pour les entreprises industrielles. "
        "Elle automatise l'analyse des donnees de performance operationnelle en combinant "
        "un pipeline de traitement de fichiers Excel, des algorithmes de classification, "
        "et des modeles de langage (LLMs) enrichis par un systeme RAG."
    )

    pdf.section("Problemes resolus")
    pdf.bullet([
        "Import manuel fastidieux de fichiers Excel → automatisation complete du pipeline",
        "Calcul manuel des variations N vs N-1 → calcul automatise, classe et visualise",
        "Absence d'analyse intelligente des KPIs → insights IA avec causes racines et plans d'action",
        "LLM sans contexte metier → RAG injecte les definitions et seuils QHSE officiels",
        "Gestion multi-utilisateurs avec roles → admin vs analyste avec authentification securisee",
    ])

    pdf.section("Fonctionnalites principales")
    pdf.bullet([
        "Upload et parsing automatique de fichiers Excel multi-periodes",
        "Detection automatique des colonnes et fuzzy matching des KPIs (Levenshtein, seuil 0.85)",
        "Calcul des variations (%), tendances (HAUSSE/BAISSE/STABLE) et niveaux (FAIBLE/MODERE/CRITIQUE)",
        "Analyse IA structuree : insights, causes racines, alertes predictives, plans d'action",
        "Base de connaissances RAG vectorielle (pgvector + embeddings Gemini 768D)",
        "Tableaux de bord interactifs, export PDF (iText7) et Excel (Apache POI)",
        "Gestion des utilisateurs, audit log, configuration IA a chaud (sans redemarrage)",
    ])

    pdf.section("Acteurs du systeme")
    pdf.bullet([
        "Analyste QHSE : importe les fichiers Excel, visualise les KPIs, consulte les analyses IA",
        "Administrateur : gere les utilisateurs, le catalogue KPI, la base RAG, la config IA",
        "Systeme IA : Groq (LLM primaire) + Gemini (fallback LLM + embeddings 768D)",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 2 — ARCHITECTURE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("2", "Architecture globale et stack technique")

    pdf.section("Schema d'architecture")
    pdf.code_block([
        "  Angular SPA (Nginx en prod)",
        "       |  /api  (proxy dev :4200→:8080, Nginx en prod)",
        "  Spring Boot REST API  (:8080)",
        "       |",
        "  PostgreSQL 15  (pgvector extension)",
        "       |              |",
        "  Groq API       Google Gemini API",
        "  (LLM primaire) (fallback LLM + embeddings 768D)",
    ])

    pdf.section("Stack Backend")
    pdf.bullet([
        "Spring Boot 3.3.5 (Java 17) - framework principal REST API",
        "Spring Security + JWT cookie HttpOnly - authentification et autorisation",
        "Spring Data JPA / Hibernate - acces base de donnees ORM",
        "PostgreSQL 15 avec extension pgvector - stockage relationnel + vectoriel",
        "Flyway - versionning et migration du schema DB (V1-V7)",
        "Caffeine Cache - cache en memoire (analyses IA 24h TTL, embeddings)",
        "Apache POI - generation Excel ; iText7 - generation PDF",
        "Micrometer + Prometheus - metriques IA (cache hits, provider selectionne)",
        "JavaMail - emails (Gmail SMTP) pour OTP et verification de compte",
    ])

    pdf.section("Stack Frontend")
    pdf.bullet([
        "Angular 21 - Standalone Components (sans NgModules)",
        "Angular Signals - etat reactif (signal(), computed(), effect())",
        "Angular Material - composants UI (tables, dialogs, tabs, chips)",
        "RxJS - gestion des flux asynchrones HTTP",
        "TypeScript strict mode",
        "Nginx - serveur web + proxy inverse en production",
    ])

    pdf.section("Stack IA / LLM / RAG")
    pdf.bullet([
        "Groq API - LLM primaire : llama-3.3-70b-versatile, meta-llama-4-scout, qwen3-32b",
        "Google Gemini API - gemini-2.0-flash (fallback) + text-embedding-2 (vecteurs 768D)",
        "pgvector - stockage et recherche vectorielle cosinus dans PostgreSQL",
        "Index IVFFlat - Approximate Nearest Neighbor pour performances vectorielles",
    ])

    pdf.section("Packages backend")
    pdf.code_block([
        "com/QHSEAnalytics/",
        "  auth/         JWT, OTP, email verification, refresh tokens, audit log",
        "  analytics/    Orchestration IA, Groq, Gemini, RAG, dashboards (27 classes)",
        "  kpi/          Catalogue KPI CRUD et enrichissement",
        "  importer/     Upload → nettoyage → classification → calcul (24 classes)",
        "  export/       Generation PDF (iText7) et Excel (Apache POI)",
        "  shared/       Entites JPA, repositories, DTOs, enums, exceptions (120 classes)",
        "  security/     JwtAuthFilter, rate-limiting filter",
        "  config/       Security, CORS, async, Caffeine, OpenAPI, pgvector init",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 3 — DATABASE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("3", "Base de donnees - Schema et migrations Flyway")

    pdf.section("Pourquoi Flyway ?")
    pdf.body(
        "Flyway garantit que le schema de base de donnees est toujours synchronise avec le code. "
        "Chaque migration est un script SQL versionne et immuable. "
        "Au demarrage de Spring Boot, Flyway verifie la table 'flyway_schema_history' "
        "et applique uniquement les migrations non encore executees. "
        "Regle absolue : ne JAMAIS modifier un script existant - creer un nouveau Vn+1."
    )

    pdf.section("Migrations V1 a V7")
    pdf.bullet([
        "V1 - Index de performance initiaux (import_sessions, resultat_kpi, users)",
        "V2 - Colonnes stockage fichier (chemin, nom original, taille) pour les imports",
        "V3 - Extension pgvector + colonne embedding vector(768) + index IVFFlat sur rag_knowledge",
        "V4 - Agrandissement colonne code categorie_kpi (contrainte VARCHAR trop courte)",
        "V5 - Table admin_audit_log (id, user_id, action, timestamp, details JSON)",
        "V6 - Champ confidence sur analyse_globale + table ai_config (cle/valeur runtime)",
        "V7 - Champ direction sur rag_knowledge (LOWER_IS_BETTER / HIGHER_IS_BETTER)",
    ])

    pdf.section("Tables principales")
    pdf.subsection("users")
    pdf.body("id, email, password (bcrypt), role (ANALYSTE/ADMIN), enabled, emailVerified, createdAt")

    pdf.subsection("import_sessions")
    pdf.body("id, user_id (FK), periode, fichierNom, statut (INITIAL/VALIDATED/TRAITE/ERREUR), mode (NORMAL/PREVIEW), createdAt")

    pdf.subsection("kpi + categorie_kpi")
    pdf.body("kpi : id, nom, unite, seuilBas, seuilHaut, direction, categorieId (FK). categorie_kpi : id, code (Q/H/S/E), libelle")

    pdf.subsection("resultat_kpi (table centrale)")
    pdf.body(
        "id, importSessionId (FK), kpiId (FK), valeurN1, valeurN, "
        "variationPct, tendance, niveauVariation, analyseIa (texte legacy), "
        "statutNettoyage, createdAt"
    )

    pdf.subsection("analyse_globale + analyse_categorie")
    pdf.body(
        "analyse_globale : id, importSessionId, synthese, planActions (JSON), overallConfidence (float). "
        "analyse_categorie : id, importSessionId, categorieCode, contenu (texte IA par categorie)"
    )

    pdf.subsection("rag_knowledge (table RAG - critique)")
    pdf.body(
        "id, kpiName, definition, thresholds, category, direction, embedding vector(768). "
        "La colonne embedding est un tableau de 768 floats genere par Gemini text-embedding-2. "
        "L'index IVFFlat (lists=100) accelere la recherche par similarite cosinus."
    )
    pdf.code_block([
        "-- Migration V3",
        "CREATE EXTENSION IF NOT EXISTS vector;",
        "ALTER TABLE rag_knowledge ADD COLUMN embedding vector(768);",
        "CREATE INDEX ON rag_knowledge",
        "  USING ivfflat (embedding vector_cosine_ops)",
        "  WITH (lists = 100);",
    ])

    pdf.subsection("ai_config")
    pdf.body(
        "Table cle/valeur pour la configuration IA runtime (modifiable sans redemarrage). "
        "Exemples : groq.temperature.json=0.1, rag.threshold=0.72, cache.ttl.hours=24"
    )

    # ══════════════════════════════════════════════════════════
    # CH 4 — SECURITE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("4", "Securite - JWT, OTP, Rate Limiting")

    pdf.section("JWT cookie HttpOnly")
    pdf.body(
        "Le JWT est stocke dans un cookie HttpOnly (inaccessible depuis JavaScript), "
        "ce qui elimine les attaques XSS ciblant le token. "
        "Contrairement au localStorage, le cookie est envoye automatiquement "
        "par le navigateur a chaque requete vers le meme domaine."
    )
    pdf.bullet([
        "Access token : TTL 30 minutes",
        "Refresh token : TTL 1 heure (7 jours avec remember-me), stocke en base",
        "CookieTokenService : gere la creation, le rafraichissement et la suppression des cookies",
        "JwtAuthFilter : filtre Spring Security qui valide le JWT a chaque requete",
        "JwtService : genere et valide les tokens (cle secrete >= 256 bits dans .env)",
    ])

    pdf.section("Flux d'authentification complet")
    pdf.numbered([
        "POST /api/auth/login → verification email+password (bcrypt)",
        "Si OTP active → envoi code 6 chiffres par Gmail SMTP (TTL 5 min)",
        "POST /api/auth/verify-otp → validation code",
        "Emission JWT access + refresh tokens en cookies HttpOnly",
        "Frontend relaie les cookies automatiquement (withCredentials: true)",
        "Token expire → ErrorInterceptor appelle POST /api/auth/refresh",
        "Refresh expire ou invalide → redirection login",
    ])

    pdf.section("Rate Limiting - protection brute force")
    pdf.body(
        "RateLimitingFilter : filtre Servlet sur /api/auth/**. "
        "Limite : 5 tentatives par adresse IP sur 300 secondes. "
        "Depassement → HTTP 429 (Too Many Requests). "
        "Implementation : HashMap<IP, (compteur, timestamp)> en memoire."
    )

    pdf.section("Autorisation par roles")
    pdf.bullet([
        "ANALYSTE : acces /api/import/**, /api/ia/**, /api/dashboard/**",
        "ADMIN : acces /api/admin/**, /api/rag/**, /api/ia-config/**",
        "Annotations @PreAuthorize sur les controleurs Spring",
        "Guards Angular (auth.guard, admin.guard, analyste.guard) cote frontend",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 5 — IMPORT PIPELINE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("5", "Pipeline d'import Excel (8 etapes)")

    pdf.body(
        "Le pipeline est orchestre par KpiProcessingOrchestratorService. "
        "Il transforme un fichier Excel brut en resultats KPI calcules, nettoyes et classes."
    )

    pdf.section("Etape 1 - Upload et creation de session")
    pdf.body(
        "ImportSessionController.upload() recoit le fichier multipart. "
        "LocalFileStorageService sauvegarde dans ./storage/imports/{sessionId}/. "
        "ImportSession cree en base : statut=INITIAL, mode=PREVIEW ou NORMAL."
    )

    pdf.section("Etape 2 - Parsing Excel")
    pdf.body(
        "ExcelFileValidator verifie le format (.xlsx/.xls) et la structure minimale. "
        "ExcelParserUtil (Apache POI) lit toutes les feuilles et cellules, "
        "construit une liste de lignes brutes (Map<String, String>)."
    )

    pdf.section("Etape 3 - Detection des en-tetes")
    pdf.body(
        "HeaderDetectionUtil analyse la premiere ligne : identifie automatiquement "
        "les colonnes nom KPI, valeur N, valeur N-1, unite. "
        "Utilise des heuristiques basees sur des mots-cles ('valeur', 'n-1', 'objectif', etc.)."
    )

    pdf.section("Etape 4 - Fuzzy matching KPI")
    pdf.body(
        "KpiMatchingUtil compare le nom de chaque ligne Excel avec le catalogue KPI en base. "
        "Algorithme : distance de Levenshtein normalisee (similarite = 1 - distance/maxLen). "
        "Seuil : 0.85 (85% de similarite requise). "
        "Si match → association kpiId. Sinon → KPI inconnu (ignore ou cree selon config)."
    )

    pdf.section("Etape 5 - Extraction")
    pdf.body(
        "ExtractionAgent cree les entites StagingDonnee (donnees brutes) : "
        "kpiId, valeurN, valeurN1, unite, periode. Associees a la session d'import."
    )

    pdf.section("Etape 6 - Nettoyage (CleaningAgent)")
    pdf.body(
        "Normalisation des nombres (virgule → point, suppression espaces), "
        "detection des valeurs aberrantes (IQR : valeurs au-dela Q3+1.5*IQR), "
        "traitement des valeurs manquantes. "
        "StatutNettoyage : PROPRE, CORRIGE, SUSPECT, INCOMPLET."
    )

    pdf.section("Etape 7 - Calcul (CalculationAgent + ComparativeCalculator)")
    pdf.code_block([
        "variationPct = ((valeurN - valeurN1) / |valeurN1|) * 100",
        "",
        "tendance :",
        "  si |variationPct| < 5%  → STABLE",
        "  si variationPct > 0     → HAUSSE",
        "  sinon                   → BAISSE",
        "",
        "direction LOWER_IS_BETTER si le nom KPI contient :",
        "  'incident', 'accident', 'defaut', 'erreur', 'retard', 'absenteisme'",
    ])

    pdf.section("Etape 8 - Classification et enrichissement (ClassificationEngine)")
    pdf.code_block([
        "Si direction = LOWER_IS_BETTER :",
        "  HAUSSE forte  → CRITIQUE  (ex: +20% accidents = tres mauvais)",
        "  HAUSSE moderee → MODERE",
        "  BAISSE/STABLE  → FAIBLE",
        "",
        "Si direction = HIGHER_IS_BETTER :",
        "  BAISSE forte   → CRITIQUE  (ex: -20% conformite = tres mauvais)",
        "  BAISSE moderee → MODERE",
        "  HAUSSE/STABLE  → FAIBLE",
    ])
    pdf.body(
        "Ensuite : MetadataEnrichmentAgent, RiskDetectionAgent (anomalies combinées), "
        "VisualizationAgent (donnees graphiques). "
        "ResultatKpi persistes en base. Session passe en statut TRAITE."
    )

    pdf.section("Mode PREVIEW vs NORMAL")
    pdf.body(
        "PREVIEW : calculs stockes temporairement pour validation utilisateur. "
        "Validation → conversion NORMAL (statut VALIDATED puis TRAITE). "
        "Abandon → suppression automatique apres 15 jours (ImportDataRetentionService)."
    )

    # ══════════════════════════════════════════════════════════
    # CH 6 — KPI CALCUL
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("6", "Calcul KPI et Classification")

    pdf.section("Formules et exemple complet")
    pdf.code_block([
        "Variation (%) = ((valeurN - valeurN1) / |valeurN1|) x 100",
        "",
        "Exemple concret :",
        "  KPI : Taux d'accidents",
        "  valeurN1 = 5  |  valeurN = 6",
        "  Variation = ((6 - 5) / 5) x 100 = +20%",
        "  Direction : LOWER_IS_BETTER (mot 'accident' dans le nom)",
        "  Tendance : HAUSSE",
        "  Niveau : CRITIQUE  (hausse sur indicateur a minimiser)",
        "",
        "Cas division par zero : si valeurN1 = 0 → variationPct = null",
        "  → KPI exclu de l'analyse IA (filtre dans AnalysisAgent)",
    ])

    pdf.section("Rapport qualite des donnees")
    pdf.body(
        "QualityReportBuilder produit : nb lignes totales, nb corrigees, nb suspectes, "
        "nb incompletes, taux de completude, distribution FAIBLE/MODERE/CRITIQUE. "
        "Affiche a l'utilisateur avant validation PREVIEW → NORMAL."
    )

    pdf.info_box(
        "Point cle soutenance",
        "ClassificationEngine depend de ComparativeCalculator.resolveDirection() qui "
        "INFERE LOWER_IS_BETTER depuis les mots-cles dans le nom du KPI. "
        "Ce n'est pas configure explicitement - c'est une heuristique automatique. "
        "Implication : les tests doivent inclure ces mots-cles pour obtenir le niveau attendu."
    )

    # ══════════════════════════════════════════════════════════
    # CH 7 — GROQ
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("7", "Integration LLM - Groq")

    pdf.section("Pourquoi Groq ?")
    pdf.bullet([
        "Inference ultra-rapide via LPU (Language Processing Unit) proprietaire",
        "Latence 5-10x inferieure a OpenAI pour les memes modeles llama",
        "API compatible OpenAI → migration simplifiee si besoin",
        "Acces a des modeles open-source puissants : llama-3.3-70b, qwen-3-32b",
        "Tier gratuit genereux pour le developpement et les tests",
    ])

    pdf.section("Modeles configures")
    pdf.bullet([
        "llama-3.3-70b-versatile : modele principal (70B parametres, excellent raisonnement)",
        "meta-llama/llama-4-scout-17b-16e-instruct : modele leger et rapide (fallback interne)",
        "qwen/qwen3-32b : modele Alibaba (diversite de raisonnement)",
    ])

    pdf.section("Rotation de cles API (GroqKeyRotator)")
    pdf.body(
        "Groq impose des limites de debit (RPM/TPM) par cle API. "
        "GroqKeyRotator contient une liste de cles provenant de .env (GROQ_API_KEYS, separees par virgule). "
        "Selection en round-robin : chaque appel prend la cle suivante dans la liste. "
        "Si une cle recoit 429 → ProviderCooldownManager la met en cooldown "
        "et GroqKeyRotator passe a la suivante disponible."
    )

    pdf.section("Parametres d'appel Groq")
    pdf.code_block([
        "POST https://api.groq.com/openai/v1/chat/completions",
        "",
        "  model          : llama-3.3-70b-versatile",
        "  messages       : [{role:system, content:...}, {role:user, content:prompt}]",
        "  temperature    : 0.1 (JSON structure) ou 0.5 (texte libre)",
        "  max_tokens     : 2800",
        "  response_format: {type: json_object}   // garantit JSON valide en sortie",
        "  timeout        : 30 secondes",
    ])

    pdf.section("Pourquoi temperature 0.1 pour JSON et 0.5 pour texte ?")
    pdf.body(
        "Temperature = degre de creativite/aleatoire du LLM. "
        "0.0 = deterministe. 1.0 = tres creatif. "
        "Pour les analyses JSON structurees → 0.1 : precision maximale, "
        "respect strict du format, pas de variations inattendues. "
        "Pour les syntheses textuelles (plans d'action, recommandations) → 0.5 : "
        "formulations plus naturelles et variees."
    )

    pdf.section("Methodes de GroqService")
    pdf.bullet([
        "analyserKpi(kpiName, variation, context) : insight par KPI en JSON",
        "analyserCategorie(categorie, kpis[]) : synthese d'une categorie QHSE",
        "genererSynthese(kpisResume) : synthese globale de tous les KPIs",
        "genererPlanActions(kpisAlerte) : plans d'action pour les KPIs CRITIQUES",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 8 — PROVIDER CHAIN
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("8", "Chaine LLM avec Fallback (LlmProviderChain)")

    pdf.section("Architecture de la chaine")
    pdf.code_block([
        "LlmProviderChain.generate(prompt, cacheKey, bypassCache)",
        "  |",
        "  +-- 1. Verifier cache Caffeine (cle SHA-256)",
        "  |      Si hit → retourner immediatement (0ms LLM)",
        "  |",
        "  +-- 2. Tenter Groq API (provider primaire)",
        "  |      ProviderCooldownManager.isAvailable('groq') ?",
        "  |      Oui → appel HTTP, timeout 30s",
        "  |      429/timeout → cooldown groq + passer a etape 3",
        "  |",
        "  +-- 3. Tenter Gemini API (fallback)",
        "  |      ProviderCooldownManager.isAvailable('gemini') ?",
        "  |      Oui → appel HTTP, timeout 30s",
        "  |      429/timeout → cooldown gemini",
        "  |",
        "  +-- 4. Les deux indisponibles → ProviderUnavailableException",
        "         → AnalysisAgent retourne statut=FAILED + fallbackReason",
    ])

    pdf.section("Cache Caffeine dans la chaine LLM")
    pdf.body(
        "Cle de cache : SHA-256 du cacheKey fourni par AnalysisAgent. "
        "Format cle source : importSessionId=X|mode=structured|promptVersion=Y|schemaVersion=Z|chunk=N. "
        "TTL : 24 heures (configurable via ai_config). "
        "bypassCache=true lors d'une regeneration explicite par l'utilisateur."
    )

    pdf.section("ProviderCooldownManager")
    pdf.body(
        "Gere les periodes de refroidissement apres un 429. "
        "Stocke par provider : {lastErrorTime, cooldownDuration}. "
        "Cooldown initial : 60 secondes. Augmente exponentiellement si erreurs repetees. "
        "isAvailable() retourne false si (now - lastErrorTime) < cooldownDuration."
    )

    pdf.section("Metriques Micrometer")
    pdf.bullet([
        "ai.cache.hit : nb de reponses servies depuis le cache",
        "ai.cache.miss : nb d'appels LLM reels effectues",
        "ai.provider.selected (tag: groq/gemini) : quel provider a repondu",
        "ai.retry.count : nb de tentatives de retry JSON",
        "ai.parse.error.count : JSON invalides recus des LLMs",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 9 — GEMINI
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("9", "Google Gemini - Fallback et Embeddings")

    pdf.section("Deux roles distincts de Gemini")
    pdf.bullet([
        "Role 1 - LLM fallback : generation de texte quand Groq est indisponible (gemini-2.0-flash)",
        "Role 2 - Embeddings RAG : vecteurs 768D pour la recherche semantique (text-embedding-2)",
    ])

    pdf.section("GeminiClientService - LLM fallback")
    pdf.code_block([
        "POST https://generativelanguage.googleapis.com/v1beta",
        "     /models/gemini-2.0-flash:generateContent",
        "",
        "Corps :",
        "  contents: [{role:user, parts:[{text: prompt}]}]",
        "  generationConfig:",
        "    responseMimeType: application/json",
        "    maxOutputTokens: 2800",
        "",
        "Retry : max 5, backoff exponentiel (1s, 2s, 4s, 8s, 60s max)",
        "Gestion 429 : cooldown dynamique via ProviderCooldownManager",
    ])

    pdf.section("EmbeddingService - vecteurs 768D")
    pdf.code_block([
        "POST https://generativelanguage.googleapis.com/v1beta",
        "     /models/text-embedding-002:embedContent",
        "",
        "Corps : {content: {parts: [{text: texte}]}}",
        "Retourne : {embedding: {values: [768 floats]}}",
        "",
        "Troncature : 6000 caracteres max avant envoi",
        "Cache Caffeine : une entree par texte (evite appels API redondants)",
        "Gestion 429 : cooldown dynamique, retourne null si indisponible",
    ])

    pdf.section("Qu'est-ce qu'un embedding 768D ?")
    pdf.body(
        "Un embedding est une representation numerique d'un texte sous forme de vecteur. "
        "text-embedding-2 de Gemini projette n'importe quel texte dans un espace a 768 dimensions. "
        "Textes semantiquement similaires → vecteurs proches dans cet espace. "
        "Exemple : 'taux d'accidents du travail' et 'frequence des blessures' "
        "auront des vecteurs tres proches meme sans mot en commun."
    )

    # ══════════════════════════════════════════════════════════
    # CH 10 — RAG
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("10", "Systeme RAG - pgvector et Recherche Vectorielle")

    pdf.section("Pourquoi le RAG ?")
    pdf.body(
        "Probleme : un LLM generaliste ne connait pas les definitions precises des KPIs QHSE "
        "de l'entreprise, ni ses seuils acceptables, ni la direction d'amelioration. "
        "Sans contexte → l'IA produit des insights generiques et potentiellement incorrects."
    )
    pdf.body(
        "Solution RAG (Retrieval-Augmented Generation) : "
        "avant chaque appel LLM, on recupere depuis la base de connaissances "
        "la definition exacte du KPI analyse, ses seuils et sa direction. "
        "Ces informations sont injectees dans le prompt. "
        "L'IA dispose d'un contexte metier precis et produit des insights pertinents."
    )

    pdf.section("Pipeline RAG - Phase 1 : Indexation (admin)")
    pdf.numbered([
        "Admin cree une entree via RagAdminController (nom KPI, definition, seuils, categorie, direction)",
        "RagAdminService appelle EmbeddingService.embed(definition)",
        "Gemini text-embedding-2 retourne un vecteur float[768]",
        "Le vecteur est stocke dans rag_knowledge.embedding (type vector(768))",
        "L'index IVFFlat se met a jour automatiquement",
    ])

    pdf.section("Pipeline RAG - Phase 2 : Recherche lors de l'analyse")
    pdf.numbered([
        "AnalysisAgent prepare l'analyse d'un KPI (ex: 'Taux d'accidents')",
        "Appelle RagKpiKnowledgeLookupService.findRelevant(kpiName, category)",
        "EmbeddingService.embed(kpiName) → vecteur de requete float[768]",
        "RagSearchService.searchBySimilarity(queryVector, category, topK=3, threshold=0.72)",
        "Si resultats insuffisants → searchByKeyword() (ILIKE, threshold=0.50)",
        "Contexte RAG injecte dans le prompt LLM",
    ])

    pdf.section("Requete SQL pgvector")
    pdf.code_block([
        "SELECT id, kpi_name, definition, thresholds, direction,",
        "       1 - (embedding <=> :queryVector) AS similarity",
        "FROM rag_knowledge",
        "WHERE category = :category",
        "  AND 1 - (embedding <=> :queryVector) >= :threshold",
        "ORDER BY embedding <=> :queryVector ASC",
        "LIMIT :topK",
        "",
        "Operateur <=>  : distance cosinus entre vecteurs",
        "1 - distance   : similarite cosinus (0.0=opposes, 1.0=identiques)",
    ])

    pdf.section("Similarite cosinus - explication")
    pdf.body(
        "La similarite cosinus mesure l'angle entre deux vecteurs dans l'espace. "
        "Elle est independante de la norme (longueur) des vecteurs - uniquement la direction compte. "
        "Formule : cos(theta) = (A . B) / (||A|| x ||B||). "
        "Valeur 1.0 → vecteurs identiques (textes tres similaires). "
        "Valeur 0.0 → vecteurs orthogonaux (textes sans rapport). "
        "Seuil 0.72 choisi empiriquement pour equilibrer precision et rappel sur les KPIs QHSE."
    )

    pdf.section("Index IVFFlat vs HNSW")
    pdf.bullet([
        "IVFFlat : partitionne l'espace en listes (lists=100). Rapide, faible memoire. "
          "Adapte pour < 100k vecteurs. Utilise ici car base RAG QHSE reste petite.",
        "HNSW : graphe hierarchique. Plus precis, mais plus gourmand en memoire. "
          "Adapte pour millions de vecteurs. A envisager si la base RAG depasse 50k entrees.",
    ])

    pdf.section("Injection du contexte RAG dans le prompt")
    pdf.code_block([
        "Contexte metier (base de connaissances QHSE) :",
        "  KPI : Taux de frequence des accidents (TF)",
        "  Definition : Nb accidents x 10^6 / heures travaillees",
        "  Seuils : Excellent < 2 | Acceptable 2-5 | Critique > 5",
        "  Direction : LOWER_IS_BETTER",
        "  Categorie : Securite",
        "",
        "Donnees observees :",
        "  Valeur N-1 : 3.2  |  Valeur N : 4.8  |  Variation : +50%",
        "  Tendance : HAUSSE  |  Niveau : CRITIQUE",
        "",
        "Analyse demandee : causes racines, recommandations, alertes predictives",
    ])

    pdf.section("Fallback keyword si RAG indisponible")
    pdf.body(
        "Si Gemini Embedding API est indisponible ou similarite < 0.50 : "
        "RagSearchService.searchByKeyword() utilise ILIKE pour chercher le nom du KPI "
        "dans rag_knowledge.kpi_name. Moins precis mais toujours fonctionnel."
    )

    # ══════════════════════════════════════════════════════════
    # CH 11 — PROMPT ENGINEERING
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("11", "Prompt Engineering et Reponse Structuree")

    pdf.section("StructuredAnalysisPromptBuilder")
    pdf.body(
        "Construit le prompt systeme + utilisateur envoye au LLM. "
        "Prompt systeme : 'Tu es un expert QHSE senior. Analyse les KPIs fournis "
        "et retourne UNIQUEMENT un JSON valide au format specifie.' "
        "Prompt utilisateur contient :"
    )
    pdf.bullet([
        "Le schema JSON exact attendu (avec types et descriptions de chaque champ)",
        "Les donnees des KPIs du batch (nom, variation, niveau, tendance)",
        "Le contexte RAG enrichi pour chaque KPI (definition, seuils, direction)",
        "Instructions explicites : 'Ne depasse pas 2800 tokens. JSON uniquement.'",
    ])

    pdf.section("Chunking - pourquoi 5 KPIs par batch ?")
    pdf.body(
        "Un prompt trop long (100 KPIs) depasse le max_tokens et degrade la qualite. "
        "Batch de 5 KPIs : prompt court (~1200 tokens), reponse JSON precise (~800 tokens). "
        "AnalysisAgent trie d'abord par |variation| descendant et garde les top 20 "
        "pour limiter le nombre de chunks et donc le cout en appels LLM."
    )

    pdf.section("StructuredAnalysisValidator - gestion JSON invalide")
    pdf.numbered([
        "Parse JSON (Jackson ObjectMapper). Si exception → tentative de reparation (extraction substr JSON)",
        "Verification champs obligatoires : kpiInsights non vide, chaque insight a kpiName + insight",
        "Si invalide → log + retry (max 2 fois). Si echec final → statut=FAILED",
    ])

    pdf.section("Format de reponse structuree (AiAnalysisStructuredResponse)")
    pdf.code_block([
        "{",
        '  "status": "SUCCESS | PARTIAL | FAILED",',
        '  "confidence": 0.87,',
        '  "kpiInsights": [',
        "    {",
        '      "kpiName": "Taux d\'accidents",',
        '      "variation": -12.5,',
        '      "level": "CRITIQUE",',
        '      "trend": "BAISSE",',
        '      "insight": "Description de la situation...",',
        '      "rootCauses": ["Manque EPI", "Formation insuffisante"],',
        '      "recommendations": ["Audit securite", "Formation obligatoire"],',
        '      "ragContext": "Def: ... Seuil: <2%  Direction: LOWER_IS_BETTER"',
        "    }",
        "  ],",
        '  "categoryAnalyses": {"S": "...", "Q": "..."},',
        '  "globalSynthesis": "Vision globale...",',
        '  "actionPlans": [{"priority":"HIGH","action":"...","deadline":"..."}],',
        '  "predictiveAlerts": [{"alertType":"RISK","probability":0.75}]',
        "}",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 12 — FRONTEND
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("12", "Frontend Angular - Architecture et Signals")

    pdf.section("Standalone Components - pourquoi ?")
    pdf.body(
        "Angular 15+ permet les composants autonomes sans NgModule. "
        "Chaque composant declare ses propres imports (Material, RouterModule, etc.). "
        "Avantages : tree-shaking plus efficace, code plus lisible, lazy loading facilite. "
        "Tous les composants du projet suivent ce pattern."
    )

    pdf.section("Angular Signals - etat reactif")
    pdf.code_block([
        "// Signal : valeur reactive",
        "selectedSessionId = signal<number | null>(null);",
        "",
        "// Computed : derive automatiquement",
        "hasData = computed(() => this.kpiInsights().length > 0);",
        "",
        "// Effect : reagit aux changements de signal",
        "effect(() => {",
        "  const id = this.selectedSessionId();",
        "  if (id) this.loadAnalysis(id);",
        "});",
        "",
        "// ATTENTION : les primitives dans computed() ne triggent PAS",
        "// Faux  : filterText = computed(() => '');",
        "// Correct : filterText = signal('');",
    ])

    pdf.section("Structure feature analyste")
    pdf.bullet([
        "import/ : upload fichier Excel, suivi de progression",
        "import-mapping/ : mapping colonnes Excel → KPIs catalogue",
        "dashboard/ : graphiques KPIs (radar, bar, pie charts)",
        "historique/ : liste des sessions d'import passees",
        "comparatif/ : comparaison entre deux periodes",
        "analyse-ia/ : page principale analyse IA (654 lignes) - cf. ch.13",
        "export-pdf/ : generation et telechargement PDF",
    ])

    pdf.section("Services principaux")
    pdf.bullet([
        "AiAnalysisService : GET/POST /api/ia/{id}, /api/ia/{id}/structured, /api/ia/{id}/regenerer",
        "ImportService : POST /api/import/upload, GET /api/import/sessions, POST /api/import/{id}/validate",
        "DashboardService : GET /api/dashboard/analyste, GET /api/dashboard/admin",
        "AuthService : login, logout, register, refreshToken, OTP, forgot/reset password",
    ])

    pdf.section("Intercepteurs HTTP")
    pdf.bullet([
        "AuthInterceptor : ajoute withCredentials:true a chaque requete (envoie les cookies JWT)",
        "ErrorInterceptor : affiche un toast Material sur toute reponse 4xx/5xx",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 13 — COMPOSANT ANALYSE IA
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("13", "Composant Analyse IA (654 lignes)")

    pdf.section("Logique de chargement des donnees")
    pdf.code_block([
        "// Effect : charge les donnees a chaque changement de session",
        "effect(() => {",
        "  const id = this.sessionId();",
        "  if (id) {",
        "    this.loadStructured(id);   // tente le format structure",
        "    this.loadLegacy(id);       // charge aussi le format legacy",
        "  }",
        "});",
        "",
        "// Fusion des deux formats",
        "mergedInsights = computed(() => {",
        "  const structured = this.structuredResponse();",
        "  const legacy = this.legacyResponse();",
        "  return this.merge(structured, legacy);",
        "});",
    ])

    pdf.section("Fonctionnalites affichees")
    pdf.bullet([
        "Badge de confiance globale (vert >= 0.8, orange 0.5-0.8, rouge < 0.5)",
        "Liste KPIs avec niveau (FAIBLE/MODERE/CRITIQUE) et variation en %",
        "Insights par KPI avec contexte RAG enrichi affiche en accordeon",
        "Causes racines (diagramme Ishikawa textuel)",
        "Plans d'action (tableau Kanban : HIGH/MEDIUM/LOW priority)",
        "Alertes predictives (RISK/OPPORTUNITY avec probabilite en %)",
        "Synthese par categorie (Q, H, S, E) et synthese globale",
        "Bouton 'Regenerer' qui bypasse le cache (bypassCache=true)",
    ])

    pdf.section("Gestion des etats")
    pdf.bullet([
        "isLoading signal : affiche un spinner Material pendant le chargement",
        "error signal : affiche un message d'erreur si l'API echoue",
        "Status SUCCESS : tout s'est bien passe",
        "Status PARTIAL : analyse incomplete (certains chunks ont echoue) mais affichee quand meme",
        "Status FAILED : echec total (les deux providers indisponibles)",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 14 — CACHE ET PERFORMANCE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("14", "Cache, Performance et Metriques")

    pdf.section("Cache Caffeine")
    pdf.bullet([
        "kpiAnalysis : analyse complete par session (TTL 24h) - evite reappels LLM couteux",
        "embeddings : vecteurs Gemini par texte - evite reappels embedding redondants",
        "CacheConfig.java : chaque cache nomme avec TTL et taille max configures",
        "Invalidation explicite lors d'une regeneration (evict par sessionId)",
    ])

    pdf.section("@Async - pourquoi ?")
    pdf.body(
        "L'analyse IA peut prendre 10-60 secondes (plusieurs appels LLM en serie). "
        "Si synchrone → le thread HTTP est bloque → timeout navigateur. "
        "@Async('aiAnalysisExecutor') decharge le calcul sur un pool de threads dedie "
        "(AsyncConfig : core=4, max=8 threads). "
        "Le front fait du polling ou utilise un endpoint de statut pour savoir quand c'est pret."
    )

    pdf.section("Optimisations de performance")
    pdf.bullet([
        "Top-20 KPIs : trie par |variation| descendant, garde les 20 plus anomaliques → limite les chunks",
        "Batch de 5 : chaque chunk = 5 KPIs → prompt court, reponse rapide et precise",
        "Cache SHA-256 : meme analyse demandee 2 fois = 0ms la 2eme (cache hit)",
        "Index IVFFlat : recherche vectorielle en O(log n) au lieu de O(n)",
        "EmbeddingService cache : vecteur deja calcule n'est jamais recalcule dans la meme session",
    ])

    # ══════════════════════════════════════════════════════════
    # CH 15 — DOCKER
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("15", "Deploiement Docker")

    pdf.section("Architecture Docker Compose")
    pdf.code_block([
        "services:",
        "  postgres:",
        "    image: pgvector/pgvector:pg15",
        "    volumes: [postgres_data:/var/lib/postgresql/data]",
        "    env: POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD",
        "",
        "  backend:",
        "    build: ./QHSEAnalytics",
        "    depends_on: [postgres]  (condition: service_healthy)",
        "    env: DB_*, JWT_SECRET, GROQ_API_KEYS, GEMINI_API_KEY, MAIL_*",
        "    expose: [8080]  (pas expose publiquement, reseau interne Docker)",
        "",
        "  frontend:",
        "    build: ./frontend   (Angular build + Nginx)",
        "    ports: [80:80]",
        "    depends_on: [backend]",
        "",
        "volumes:",
        "  postgres_data:  (persistance des donnees entre redemarrages)",
    ])

    pdf.section("Role de Nginx en production")
    pdf.bullet([
        "Sert les fichiers statiques Angular (dist/frontend/browser/) sur port 80",
        "Proxyfie /api/* vers http://backend:8080 (reseau Docker interne)",
        "Gzip compression sur les assets JS/CSS (gain 60-80% de taille)",
        "Cache-Control headers immutable pour les chunks haches (performance navigateur)",
    ])

    pdf.section("Variables d'environnement (.env)")
    pdf.body(
        "Toutes les valeurs sensibles sont externalisees dans .env (jamais en dur dans le code). "
        "Docker Compose charge .env automatiquement. "
        "Spring Boot lit les variables via ${VAR_NAME} dans application.properties. "
        "Exemple : app.groq.api-keys=${GROQ_API_KEYS}"
    )

    pdf.section("Flyway en production")
    pdf.body(
        "Au demarrage du backend Docker, Flyway s'execute automatiquement. "
        "Si la DB est vide → applique V1-V7 dans l'ordre. "
        "Si la DB existe deja → applique uniquement les nouvelles migrations. "
        "Le container backend attend que postgres soit healthy (healthcheck + depends_on condition)."
    )

    # ══════════════════════════════════════════════════════════
    # CH 16 — Q&R SOUTENANCE
    # ══════════════════════════════════════════════════════════
    pdf.chapter_title("16", "Questions-Reponses Soutenance")

    pdf.section("Architecture et choix technologiques")

    pdf.qa_block(
        "Pourquoi Spring Boot plutot que FastAPI (Python) pour un projet IA ?",
        "Le coeur metier (import, calcul KPI, gestion utilisateurs) est une application d'entreprise "
        "classique avec JPA, securite, transactions. Spring Boot excelle ici. L'IA est un composant "
        "parmi d'autres, pas le coeur. Groq et Gemini sont appeles via HTTP - n'importe quel langage "
        "peut faire ca. La limite : Python aurait donne acces a LangChain, mais on a implemente "
        "notre propre chaine LLM avec fallback et cache."
    )

    pdf.qa_block(
        "Pourquoi PostgreSQL et pas une base vectorielle dediee (Pinecone, Weaviate) ?",
        "pgvector dans PostgreSQL evite d'ajouter un service supplementaire dans l'infrastructure. "
        "Pour < 100k vecteurs, les performances sont excellentes avec l'index IVFFlat. "
        "Avantage cle : les vecteurs et les donnees relationnelles (KPI, sessions) sont dans la meme "
        "transaction ACID. Limite : pour des millions de vecteurs, Pinecone serait plus scalable."
    )

    pdf.qa_block(
        "Pourquoi Groq comme LLM primaire et pas OpenAI ?",
        "Groq utilise des LPU (Language Processing Units) specialises qui offrent une latence "
        "5-10x inferieure a OpenAI pour des modeles equivalents. Les modeles llama sont open-source. "
        "Tier gratuit genereux pour le developpement. API compatible OpenAI : migration facile si besoin."
    )

    pdf.section("Pipeline d'import")

    pdf.qa_block(
        "Comment gerez-vous les Excel avec des structures differentes ?",
        "HeaderDetectionUtil analyse la premiere ligne avec des heuristiques (mots-cles 'valeur', 'n-1'). "
        "KpiMatchingUtil fait du fuzzy matching (Levenshtein, seuil 0.85) pour associer les colonnes "
        "aux KPIs du catalogue. L'utilisateur peut aussi mapper manuellement via import-mapping si la "
        "detection automatique echoue. Les templates de mapping sont sauvegardes pour les prochains imports."
    )

    pdf.qa_block(
        "Que se passe-t-il si valeurN1 est zero (division par zero) ?",
        "ComparativeCalculator verifie |valeurN1| > 0 avant le calcul. Si valeurN1 = 0, "
        "la variation est marquee null (non calculable) et le KPI est exclu du batch "
        "d'analyse IA (AnalysisAgent filtre les KPIs avec variation null)."
    )

    pdf.section("LLM et prompt engineering")

    pdf.qa_block(
        "Que se passe-t-il si le LLM retourne un JSON malforme ?",
        "StructuredAnalysisValidator detecte l'exception Jackson. Il tente de reparer le JSON "
        "(extraction de sous-chaine entre { et }). Si echec, AnalysisAgent retente avec le meme "
        "prompt (max 2 retries). Si toujours invalide, le chunk est marque FAILED et les autres "
        "chunks continuent. La reponse finale peut etre PARTIAL. L'utilisation de "
        "response_format:json_object chez Groq reduit tres fortement ce probleme."
    )

    pdf.qa_block(
        "Comment garantissez-vous la qualite des analyses IA ?",
        "(1) RAG injecte le contexte metier precis pour guider le LLM. "
        "(2) Temperature 0.1 minimise les hallucinations sur les donnees chiffrees. "
        "(3) response_format:json_object garantit la structure JSON. "
        "(4) StructuredAnalysisValidator verifie les champs obligatoires. "
        "(5) Le score de confiance (overallConfidence) est affiche a l'utilisateur. "
        "Limite honnete : le LLM peut toujours halluciner des causes racines non verifiees."
    )

    pdf.section("RAG")

    pdf.qa_block(
        "En quoi le RAG ameliore-t-il concretement les resultats ?",
        "Sans RAG : 'Le taux d'accidents a augmente, il faudrait ameliorer la securite.' "
        "Avec RAG (seuil injecte : excellent <2, critique >5) : "
        "'Le TF passe de 3.2 a 4.8. Il approche le seuil critique (>5) de votre referentiel. "
        "Causes prioritaires : formation EPI insuffisante, absence de permis de travail. "
        "Action urgente : audit securite avant depassement du seuil reglementaire.' "
        "Le RAG ancre le LLM dans la realite metier de l'entreprise."
    )

    pdf.qa_block(
        "Quelle est la difference entre similarite cosinus et distance euclidienne ?",
        "Distance euclidienne mesure la distance geometrique - depend de la norme du vecteur. "
        "Similarite cosinus mesure l'angle - independante de la longueur. "
        "Pour les embeddings textuels, deux textes similaires ont la meme direction meme si "
        "l'un est plus long (norme plus grande). Cosinus est plus adapte pour la semantique textuelle."
    )

    pdf.section("Securite")

    pdf.qa_block(
        "Pourquoi JWT en cookie HttpOnly plutot qu'en localStorage ?",
        "localStorage est accessible par JavaScript - vulnerable aux attaques XSS "
        "(un script injecte peut voler le token). "
        "Cookie HttpOnly : inaccessible par JavaScript, envoye automatiquement par le navigateur. "
        "Vulnerabilite restante : CSRF. Mitigation via SameSite=Strict sur le cookie "
        "et validation de l'origine dans Spring Security."
    )

    pdf.qa_block(
        "Comment gerez-vous la revocation des tokens ?",
        "Les access tokens (30 min) ne sont pas revocables - c'est la limitation du JWT stateless. "
        "Les refresh tokens sont stockes en base (table refresh_token) avec un flag 'revoked'. "
        "Logout → marque le refresh token comme revoqu. "
        "L'access token reste valide jusqu'a expiration (max 30 min) - "
        "c'est le trade-off classique JWT : stateless vs revocabilite immediate."
    )

    pdf.section("Flow utilisateur complet")

    pdf.qa_block(
        "Decrivez le flux complet d'utilisation de la plateforme de A a Z.",
        "1) Register (email, password) → 2) Email de verification (token 24h) "
        "→ 3) Clic lien verification → compte active → 4) Login → 5) OTP si configure (5 min) "
        "→ 6) Cookie JWT emis → 7) Redirection /analyste/dashboard "
        "→ 8) Upload Excel → 9) Detection auto colonnes + fuzzy matching KPIs "
        "→ 10) Nettoyage + Calcul variation% + Classification FAIBLE/MODERE/CRITIQUE "
        "→ 11) Validation PREVIEW → commit NORMAL "
        "→ 12) Declenchement analyse IA (async, pool 4-8 threads) "
        "→ 13) RAG recherche vecteurs Gemini → contexte injecte dans prompt Groq "
        "→ 14) JSON structure valide → stockage ResultatKpi + AnalyseGlobale "
        "→ 15) Frontend /analyse-ia : insights, causes racines, plans d'action, alertes predictives "
        "→ 16) Export PDF ou Excel si besoin."
    )

    # ── PAGE FINALE - MOTS CLES ────────────────────────────────
    pdf._chapter = "Recapitulatif"
    pdf.add_page()
    pdf.set_fill_color(*BLUE_DARK)
    pdf.rect(0, 13, PAGE_W, 22, "F")
    pdf.set_font("Arial", "B", 15)
    pdf.set_text_color(*WHITE)
    pdf.set_xy(L_MARGIN, 17)
    pdf.cell(CONTENT_W, 14, "Mots-cles a maitriser",
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_text_color(*GRAY_DARK)
    pdf.ln(8)

    keywords = [
        ("LLM / Groq",
         "Large Language Model. Groq = inference rapide via LPU. "
         "Parametres cles: temperature, max_tokens, response_format:json_object, rotation de cles API."),
        ("RAG",
         "Retrieval-Augmented Generation. Enrichit le prompt LLM avec des donnees "
         "extraites d'une base de connaissances par recherche semantique vectorielle."),
        ("pgvector",
         "Extension PostgreSQL pour stocker et interroger des vecteurs flottants. "
         "Operateur <=> = distance cosinus. Index IVFFlat pour ANN rapide."),
        ("Embedding",
         "Representation numerique d'un texte en vecteur dense (768D ici via Gemini text-embedding-2). "
         "Textes semantiquement proches = vecteurs proches dans l'espace."),
        ("Similarite cosinus",
         "Mesure l'angle entre 2 vecteurs. 1.0 = identiques, 0.0 = orthogonaux. "
         "Independante de la longueur. Formule: (A.B)/(||A||x||B||)"),
        ("Fallback chain",
         "Groq (primaire) → Gemini (fallback). ProviderCooldownManager gere les "
         "cooldowns apres 429. Cache SHA-256 evite les appels redondants."),
        ("Flyway",
         "Migration DB versionnee (V1→V7). Immuable : ne jamais modifier un script "
         "existant, toujours creer un Vn+1."),
        ("JWT HttpOnly",
         "Token d'authentification en cookie inaccessible JS. Elimine XSS. "
         "Access 30min + Refresh 1h (7j remember-me) stocke en base."),
        ("Standalone Angular",
         "Composants sans NgModule. Imports declares par composant. "
         "Tree-shaking optimal. Signals pour la reactivite."),
        ("ClassificationEngine",
         "FAIBLE/MODERE/CRITIQUE selon direction du KPI "
         "(LOWER_IS_BETTER infere par mots-cles) et amplitude de variation."),
    ]

    for kw, definition in keywords:
        pdf.set_font("Arial", "B", 10)
        pdf.set_text_color(*BLUE_MID)
        pdf.set_x(L_MARGIN)
        pdf.cell(45, 6, kw, new_x=XPos.RIGHT, new_y=YPos.TOP)
        pdf.set_font("Arial", "", 9.5)
        pdf.set_text_color(*GRAY_DARK)
        pdf.multi_cell(CONTENT_W - 45, 6, definition)
        pdf.set_draw_color(*BORDER_GRAY)
        pdf.set_line_width(0.2)
        pdf.line(L_MARGIN, pdf.get_y(), PAGE_W - R_MARGIN, pdf.get_y())
        pdf.ln(1)

    # ── OUTPUT ─────────────────────────────────────────────────
    out = r"c:\Users\zeine\Desktop\QHSE-Analytics\QHSE_Analytics_Soutenance_Technique.pdf"
    pdf.output(out)
    print(f"PDF generated: {out}")


if __name__ == "__main__":
    build()
