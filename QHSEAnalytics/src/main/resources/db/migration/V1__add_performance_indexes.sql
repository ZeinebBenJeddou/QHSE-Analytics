-- ============================================================
-- QHSE Analytics — V1 : Schéma initial complet
-- Crée toutes les tables si elles n'existent pas (idempotent).
-- En développement le schéma était géré par Hibernate ddl-auto=update ;
-- cette migration le formalise pour Flyway + ddl-auto=validate.
-- ============================================================

-- ─── Auth ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    prenom      VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    is_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id         BIGSERIAL   PRIMARY KEY,
    code       VARCHAR(6)  NOT NULL,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS email_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    type       VARCHAR(50)  NOT NULL,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

-- ─── KPI catalogue ───────────────────────────────────────────

CREATE TABLE IF NOT EXISTS categorie_kpi (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(1)   NOT NULL UNIQUE,
    libelle     VARCHAR(100) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS kpi (
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

-- ─── Import pipeline ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS import_sessions (
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
    updated_at          TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS kpi_raw_data (
    id                 BIGSERIAL        PRIMARY KEY,
    import_session_id  BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_nom            VARCHAR(1000)    NOT NULL,
    valeur_n1          VARCHAR(255),
    valeur_n           VARCHAR(255),
    methode_extraction VARCHAR(20)      NOT NULL,
    score_confiance    DOUBLE PRECISION NOT NULL,
    ligne_fichier      INTEGER          NOT NULL
);

CREATE TABLE IF NOT EXISTS kpi_import_preview (
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

CREATE TABLE IF NOT EXISTS staging_donnees (
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

CREATE TABLE IF NOT EXISTS resultat_kpis (
    id                 BIGSERIAL        PRIMARY KEY,
    import_session_id  BIGINT           NOT NULL REFERENCES import_sessions(id),
    kpi_id             BIGINT           NOT NULL REFERENCES kpi(id),
    user_id            BIGINT           NOT NULL REFERENCES users(id),
    periode_n1         INTEGER          NOT NULL,
    periode_n          INTEGER          NOT NULL,
    valeur_n1          DOUBLE PRECISION NOT NULL,
    valeur_n           DOUBLE PRECISION NOT NULL,
    variation_absolue  DOUBLE PRECISION NOT NULL,
    variation_relative DOUBLE PRECISION NOT NULL,
    niveau_variation   VARCHAR(20)      NOT NULL,
    tendance           VARCHAR(20)      NOT NULL,
    confidence_score   DOUBLE PRECISION NOT NULL,
    quality_status     VARCHAR(20)      NOT NULL,
    analyse_ia         TEXT,
    status             VARCHAR(50),
    commentaire        TEXT,
    created_at         TIMESTAMP        NOT NULL
);

CREATE TABLE IF NOT EXISTS mapping_config (
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

-- ─── AI analysis ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS kpi_analysis (
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

CREATE TABLE IF NOT EXISTS analyse_globales (
    id                BIGSERIAL PRIMARY KEY,
    import_session_id BIGINT    NOT NULL REFERENCES import_sessions(id),
    user_id           BIGINT    NOT NULL REFERENCES users(id),
    synthese          TEXT      NOT NULL,
    plan_actions      TEXT      NOT NULL,
    created_at        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS analyse_categories (
    id                BIGSERIAL    PRIMARY KEY,
    import_session_id BIGINT       NOT NULL REFERENCES import_sessions(id),
    user_id           BIGINT       NOT NULL REFERENCES users(id),
    categorie_code    VARCHAR(1)   NOT NULL,
    categorie_libelle VARCHAR(100) NOT NULL,
    contenu           TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);

-- ─── RAG knowledge base ──────────────────────────────────────

CREATE TABLE IF NOT EXISTS rag_knowledge (
    id         BIGSERIAL    PRIMARY KEY,
    kpi_name   VARCHAR(255) NOT NULL UNIQUE,
    definition TEXT,
    thresholds JSONB,
    category   VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
