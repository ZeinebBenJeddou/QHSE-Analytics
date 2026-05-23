-- ============================================
-- QHSE Analytics — Schéma consolidé
-- Version 1.0 — Mai 2026
--
-- Ce fichier remplace les migrations V1 à V19.
-- Schéma final après suppression complète du RAG.
-- ============================================

-- Supprimer l'extension pgvector si elle existe (héritée d'une ancienne base)
DROP EXTENSION IF EXISTS vector CASCADE;

-- ===========================================
-- TABLES : base utilisateurs
-- ===========================================

CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    nom            VARCHAR(255) NOT NULL,
    prenom         VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    role           VARCHAR(50)  NOT NULL,
    is_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    is_system_admin BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE otp_codes (
    id         BIGSERIAL  PRIMARY KEY,
    code       VARCHAR(6) NOT NULL,
    user_id    BIGINT     NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP  NOT NULL,
    used       BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP  NOT NULL
);

CREATE TABLE email_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    type       VARCHAR(50)  NOT NULL,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE admin_audit_log (
    id             BIGSERIAL    PRIMARY KEY,
    admin_email    VARCHAR(255) NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    target_user_id BIGINT,
    target_email   VARCHAR(255),
    details        VARCHAR(512),
    timestamp      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLES : référentiel KPI
-- ===========================================

CREATE TABLE categorie_kpi (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    libelle     VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT ck_categorie_kpi_code CHECK (code ~ '^[A-Z][A-Z0-9]{0,9}$')
);

CREATE TABLE kpi (
    id             BIGSERIAL        PRIMARY KEY,
    nom            VARCHAR(150)     NOT NULL,
    definition     VARCHAR(1000)    NOT NULL,
    unite          VARCHAR(20)      NOT NULL,
    categorie_id   BIGINT           NOT NULL REFERENCES categorie_kpi(id),
    seuil_faible   DOUBLE PRECISION NOT NULL,
    seuil_modere   DOUBLE PRECISION NOT NULL,
    seuil_critique DOUBLE PRECISION NOT NULL,
    ordre          INTEGER          NOT NULL,
    is_active      BOOLEAN          NOT NULL DEFAULT TRUE,
    direction      VARCHAR(50),
    target_value   DOUBLE PRECISION,
    created_at     TIMESTAMP        NOT NULL,
    updated_at     TIMESTAMP        NOT NULL,
    CONSTRAINT uk_kpi_nom_categorie UNIQUE (nom, categorie_id)
);

-- ===========================================
-- TABLES : imports et données
-- ===========================================

CREATE TABLE import_sessions (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id),
    mode                VARCHAR(30)  NOT NULL,
    nom_fichier         VARCHAR(255) NOT NULL,
    template_version    VARCHAR(20),
    file_storage_path   TEXT,
    file_storage_bucket VARCHAR(100),
    file_size_bytes     BIGINT,
    file_checksum       VARCHAR(64),
    periode_n1          INTEGER      NOT NULL,
    periode_n           INTEGER      NOT NULL,
    statut              VARCHAR(30)  NOT NULL,
    message_erreur      VARCHAR(1000),
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT import_sessions_mode_check   CHECK (mode   = 'MANUAL'),
    CONSTRAINT import_sessions_statut_check CHECK (statut IN (
        'EN_ATTENTE','EN_TRAITEMENT','IMPORTED','CALCULATED',
        'READY_FOR_AI','TRAITE','ERREUR'
    ))
);

CREATE TABLE kpi_raw_data (
    id                 BIGSERIAL        PRIMARY KEY,
    import_session_id  BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_nom            VARCHAR(1000)    NOT NULL,
    valeur_n1          VARCHAR(255),
    valeur_n           VARCHAR(255),
    methode_extraction VARCHAR(20)      NOT NULL,
    score_confiance    DOUBLE PRECISION NOT NULL,
    ligne_fichier      INTEGER          NOT NULL
);

CREATE TABLE kpi_import_preview (
    id                BIGSERIAL        PRIMARY KEY,
    import_session_id BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_name          VARCHAR(200)     NOT NULL,
    category          VARCHAR(100),
    unit              VARCHAR(50),
    definition        TEXT,
    value_n           DOUBLE PRECISION,
    value_n1          DOUBLE PRECISION,
    status            VARCHAR(50),
    commentaire       TEXT,
    variation_percent DOUBLE PRECISION,
    ecart             DOUBLE PRECISION,
    created_at        TIMESTAMP        NOT NULL
);

CREATE TABLE staging_donnees (
    id                BIGSERIAL        PRIMARY KEY,
    import_session_id BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_id            BIGINT           NOT NULL REFERENCES kpi(id),
    valeur_brute_n1   VARCHAR(255),
    valeur_brute_n    VARCHAR(255),
    valeur_n1         DOUBLE PRECISION,
    valeur_n          DOUBLE PRECISION,
    statut_nettoyage  VARCHAR(20)      NOT NULL,
    note_nettoyage    VARCHAR(500),
    created_at        TIMESTAMP        NOT NULL
);

CREATE TABLE resultat_kpis (
    id                 BIGSERIAL        PRIMARY KEY,
    import_session_id  BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_id             BIGINT           NOT NULL REFERENCES kpi(id),
    user_id            BIGINT           NOT NULL REFERENCES users(id),
    periode_n1         INTEGER          NOT NULL,
    periode_n          INTEGER          NOT NULL,
    valeur_n1          DOUBLE PRECISION,
    valeur_n           DOUBLE PRECISION,
    variation_absolue  DOUBLE PRECISION,
    variation_relative DOUBLE PRECISION,
    niveau_variation   VARCHAR(20)      NOT NULL,
    tendance           VARCHAR(20)      NOT NULL,
    confidence_score   DOUBLE PRECISION NOT NULL,
    quality_status     VARCHAR(20)      NOT NULL,
    analyse_ia         TEXT,
    status             VARCHAR(50),
    commentaire        TEXT,
    created_at         TIMESTAMP        NOT NULL,
    CONSTRAINT chk_niveau_variation CHECK (niveau_variation IN (
        'EXCELLENT','FAIBLE','MODERE','PRE_ESCALADE','CRITIQUE','INDETERMINE'
    )),
    CONSTRAINT chk_tendance CHECK (tendance IN ('HAUSSE','BAISSE','STABLE','NA'))
);

-- ===========================================
-- TABLES : analyses IA
-- ===========================================

CREATE TABLE kpi_analysis (
    id                    BIGSERIAL    PRIMARY KEY,
    import_session_id     BIGINT       NOT NULL REFERENCES import_sessions(id),
    kpi_name              VARCHAR(200) NOT NULL,
    risk_level            VARCHAR(20),
    risk_justification    TEXT,
    identification_risque TEXT,
    objective_reached     BOOLEAN,
    improvement_detected  BOOLEAN,
    issue_detected        TEXT,
    probleme_detecte      TEXT,
    corrective_action     TEXT,
    preventive_action     TEXT,
    actions_preventives   TEXT,
    immediate_action      TEXT,
    action_immediate      TEXT,
    immediate_priority    VARCHAR(20),
    priorite_action       VARCHAR(20),
    requires_8d           BOOLEAN      NOT NULL DEFAULT FALSE,
    eight_d_details       TEXT,
    methode_8d            TEXT,
    ai_note               TEXT,
    note_finale           TEXT,
    created_at            TIMESTAMP    NOT NULL
);

CREATE TABLE analyse_globales (
    id                      BIGSERIAL PRIMARY KEY,
    import_session_id       BIGINT    NOT NULL REFERENCES import_sessions(id),
    user_id                 BIGINT    NOT NULL REFERENCES users(id),
    synthese                TEXT      NOT NULL,
    plan_actions            TEXT      NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    overall_confidence      INTEGER,
    structured_response_json TEXT
);

CREATE TABLE analyse_categories (
    id                BIGSERIAL    PRIMARY KEY,
    import_session_id BIGINT       NOT NULL REFERENCES import_sessions(id),
    user_id           BIGINT       NOT NULL REFERENCES users(id),
    categorie_code    VARCHAR(1)   NOT NULL,
    categorie_libelle VARCHAR(100) NOT NULL,
    contenu           TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);

-- ===========================================
-- TABLES : configuration
-- ===========================================

CREATE TABLE mapping_config (
    id             BIGSERIAL    PRIMARY KEY,
    template_name  VARCHAR(120) NOT NULL,
    excel_column   VARCHAR(255) NOT NULL,
    kpi_id         BIGINT       REFERENCES kpi(id),
    categorie_code VARCHAR(50),
    user_id        BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_mapping_config UNIQUE (user_id, template_name, excel_column)
);

CREATE TABLE ai_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       VARCHAR(500) NOT NULL,
    description VARCHAR(512),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===========================================
-- INDEX
-- ===========================================

CREATE INDEX idx_audit_log_timestamp        ON admin_audit_log    (timestamp DESC);
CREATE INDEX idx_audit_log_admin_email      ON admin_audit_log    (admin_email);

CREATE INDEX idx_refresh_tokens_token       ON refresh_tokens     (token);
CREATE INDEX idx_refresh_tokens_user_id     ON refresh_tokens     (user_id);

CREATE INDEX idx_email_tokens_token         ON email_tokens       (token);

CREATE INDEX idx_otp_codes_user_used_expires ON otp_codes         (user_id, used, expires_at);

CREATE INDEX idx_kpi_categorie_active       ON kpi                (categorie_id, is_active);
CREATE INDEX idx_kpi_active_ordre           ON kpi                (is_active, ordre ASC);

CREATE INDEX idx_import_sessions_user_id    ON import_sessions    (user_id);
CREATE INDEX idx_import_sessions_statut     ON import_sessions    (statut);
CREATE INDEX idx_import_sessions_user_statut ON import_sessions   (user_id, statut);
CREATE INDEX idx_import_sessions_created_at ON import_sessions    (created_at DESC);

CREATE INDEX idx_kpi_raw_data_import_session      ON kpi_raw_data      (import_session_id);
CREATE INDEX idx_kpi_import_preview_import_session ON kpi_import_preview (import_session_id);

CREATE INDEX idx_resultat_kpis_import_session_id ON resultat_kpis (import_session_id);
CREATE INDEX idx_resultat_kpis_kpi_id            ON resultat_kpis (kpi_id);
CREATE INDEX idx_resultat_kpis_user_id           ON resultat_kpis (user_id);
CREATE INDEX idx_resultat_kpis_niveau_variation  ON resultat_kpis (niveau_variation);
CREATE INDEX idx_resultat_kpis_import_kpi        ON resultat_kpis (import_session_id, kpi_id);

CREATE INDEX idx_analyse_globales_import_session    ON analyse_globales    (import_session_id);
CREATE INDEX idx_analyse_categories_import_session  ON analyse_categories  (import_session_id);
CREATE INDEX idx_analyse_categories_code            ON analyse_categories  (import_session_id, categorie_code);

-- ===========================================
-- DONNEES INITIALES : catégories KPI
-- ===========================================

INSERT INTO categorie_kpi (code, libelle, description) VALUES
    ('Q',  'Qualité',          'Maîtrise opérationnelle et satisfaction'),
    ('H',  'Hygiène & Santé',  'Préservation de la santé des collaborateurs'),
    ('S',  'Sécurité',         'Prévention des risques et accidents'),
    ('E',  'Environnement',    'Gestion des impacts et durabilité')
ON CONFLICT (code) DO NOTHING;

-- ===========================================
-- DONNEES INITIALES : KPIs standard QHSE
-- ===========================================

INSERT INTO kpi (nom, definition, unite, categorie_id, seuil_faible, seuil_modere, seuil_critique, ordre, is_active, direction, created_at, updated_at)
SELECT nom, definition, unite,
       (SELECT id FROM categorie_kpi WHERE code = cat),
       seuil_faible, seuil_modere, seuil_critique, ordre, TRUE, direction,
       NOW(), NOW()
FROM (VALUES
    -- Qualité                         nom                                               definition                                           unite           cat   sf       sm       sc       ord  direction
    ('Taux de Non-Conformité',         'Ratio produits non conformes / total produit',   'POURCENTAGE', 'Q',   2.0,    5.0,   10.0,   1, 'LOWER_IS_BETTER'),
    ('Coût de la Non-Qualité',         'Coûts des rebuts et retouches en k€',            'NOMBRE',      'Q', 500.0, 2000.0, 5000.0,  2, 'LOWER_IS_BETTER'),
    ('Taux de Satisfaction Client',    'Indice de satisfaction global',                  'POURCENTAGE', 'Q',  70.0,   80.0,   90.0,  3, 'HIGHER_IS_BETTER'),
    ('Délai Moyen de Livraison',       'Respect des délais promis (jours)',              'NOMBRE',      'Q',   2.0,    5.0,   10.0,  4, 'LOWER_IS_BETTER'),
    ('Taux de Rebuts',                 'Pourcentage de perte matière brute',             'POURCENTAGE', 'Q',   1.0,    3.0,    5.0,  5, 'LOWER_IS_BETTER'),
    ('First Pass Yield',               'Produits conformes dès le premier essai',        'POURCENTAGE', 'Q',  90.0,   95.0,   98.0,  6, 'HIGHER_IS_BETTER'),
    ('Nombre de Réclamations Clients', 'Total des plaintes enregistrées',                'NOMBRE',      'Q',   2.0,   10.0,   25.0,  7, 'LOWER_IS_BETTER'),
    ('Taux de Réussite des Audits',    'Score moyen des audits qualité internes',        'POURCENTAGE', 'Q',  75.0,   85.0,   95.0,  8, 'HIGHER_IS_BETTER'),
    -- Hygiène & Santé
    ('Taux d''Absentéisme',               'Heures d''absence / Heures théoriques',       'POURCENTAGE', 'H',   3.0,    6.0,   10.0,  1, 'LOWER_IS_BETTER'),
    ('Taux de Maladies Professionnelles', 'Cas déclarés pour 1000 salariés',             'NOMBRE',      'H',   0.0,    1.0,    2.0,  2, 'LOWER_IS_BETTER'),
    ('Conformité Ergonomique',            'Postes de travail adaptés aux normes',        'POURCENTAGE', 'H',  80.0,   90.0,  100.0,  3, 'HIGHER_IS_BETTER'),
    ('Taux de Visites Médicales',         'Salariés à jour de leur suivi médical',       'POURCENTAGE', 'H',  90.0,   95.0,  100.0,  4, 'HIGHER_IS_BETTER'),
    ('Qualité de l''Air (CO2)',           'Niveau moyen de CO2 en ppm',                  'NOMBRE',      'H', 600.0, 1000.0, 1500.0,  5, 'LOWER_IS_BETTER'),
    ('Taux de Renouvellement d''Air',     'Volume d''air renouvelé par heure',           'NOMBRE',      'H',  20.0,   25.0,   30.0,  6, 'HIGHER_IS_BETTER'),
    ('Indice d''Exposition au Bruit',     'Moyenne des niveaux sonores en dB(A)',        'NOMBRE',      'H',  80.0,   85.0,   90.0,  7, 'LOWER_IS_BETTER'),
    ('Usage des Équipements de Repos',    'Fréquence d''utilisation des zones de pause', 'POURCENTAGE', 'H',  40.0,   60.0,   80.0,  8, 'HIGHER_IS_BETTER'),
    -- Sécurité
    ('Taux de Fréquence (TF1)',           'Accidents avec arrêt / million d''heures',    'NOMBRE',      'S',   5.0,   15.0,   30.0,  1, 'LOWER_IS_BETTER'),
    ('Taux de Gravité (TG)',              'Jours perdus / millier d''heures',            'NOMBRE',      'S',   0.5,    1.0,    2.0,  2, 'LOWER_IS_BETTER'),
    ('Nombre de Presque-accidents',       'Near-miss signalés (vigilance)',              'NOMBRE',      'S',   5.0,   10.0,   20.0,  3, 'HIGHER_IS_BETTER'),
    ('Heures de Formation Sécurité',      'Total heures formation par employé',          'NOMBRE',      'S',   5.0,   10.0,   20.0,  4, 'HIGHER_IS_BETTER'),
    ('Taux de Port des EPI',              'Conformité observée lors des rondes',         'POURCENTAGE', 'S',  90.0,   95.0,  100.0,  5, 'HIGHER_IS_BETTER'),
    ('Nombre de Situations Dangereuses',  'Situations à risque signalées',               'NOMBRE',      'S',  10.0,   25.0,   50.0,  6, 'LOWER_IS_BETTER'),
    ('Délai Levée des Non-Conformités',   'Temps pour corriger une faille sécurité (jours)', 'NOMBRE', 'S',   2.0,    7.0,   15.0,  7, 'LOWER_IS_BETTER'),
    ('Nombre de Visites Sécurité (VMS)',  'Total des visites managériales terrain',      'NOMBRE',      'S',   4.0,    8.0,   12.0,  8, 'HIGHER_IS_BETTER'),
    -- Environnement
    ('Consommation Électricité',          'kWh consommés par tonne produite',            'KWH',         'E', 100.0,  200.0,  500.0,  1, 'LOWER_IS_BETTER'),
    ('Consommation Eau',                  'Mètres cubes d''eau consommés',               'NOMBRE',      'E',  50.0,  150.0,  300.0,  2, 'LOWER_IS_BETTER'),
    ('Taux de Valorisation Déchets',      'Déchets recyclés / Déchets totaux',           'POURCENTAGE', 'E',  50.0,   70.0,   85.0,  3, 'HIGHER_IS_BETTER'),
    ('Émissions CO2 (Scope 1&2)',         'Tonnes de CO2 équivalent',                    'KG',          'E', 1000.0, 5000.0, 10000.0, 4, 'LOWER_IS_BETTER'),
    ('Volume Déchets Dangereux',          'Total déchets toxiques ou polluants',         'KG',          'E',  50.0,  200.0,  500.0,  5, 'LOWER_IS_BETTER'),
    ('Consommation de Papier',            'Nombre de rames par collaborateur',           'NOMBRE',      'E',   1.0,    3.0,    5.0,  6, 'LOWER_IS_BETTER'),
    ('Incidents Environnementaux',        'Déversements ou fuites accidentelles',        'NOMBRE',      'E',   0.0,    1.0,    2.0,  7, 'LOWER_IS_BETTER'),
    ('Part Énergie Renouvelable',         'Pourcentage d''énergie propre utilisée',      'POURCENTAGE', 'E',  10.0,   30.0,   50.0,  8, 'HIGHER_IS_BETTER')
) AS v(nom, definition, unite, cat, seuil_faible, seuil_modere, seuil_critique, ordre, direction)
ON CONFLICT (nom, categorie_id) DO NOTHING;

-- ===========================================
-- DONNEES INITIALES : configuration IA
-- (les entrées RAG sont volontairement omises)
-- ===========================================

INSERT INTO ai_config (key, value, description) VALUES
    ('groq.temperature.json', '0.1', 'Température Groq pour les réponses JSON (0.0–2.0)'),
    ('groq.temperature.text', '0.5', 'Température Groq pour les réponses texte (0.0–2.0)'),
    ('groq.timeout.seconds',  '30',  'Timeout des appels Groq en secondes')
ON CONFLICT (key) DO NOTHING;

-- Note : le compte administrateur initial est créé au démarrage par AdminInitializer.java
-- (nécessite un hash bcrypt depuis les variables d'environnement app.admin.*)
