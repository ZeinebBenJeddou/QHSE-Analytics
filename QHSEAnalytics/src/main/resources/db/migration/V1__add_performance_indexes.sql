-- ============================================================
-- QHSE Analytics — Migration V1 : Indexes de performance
-- Ces indexes sont ajoutés SANS recréer les tables (Hibernate
-- gère le schéma). Activer Flyway en production en passant
-- spring.flyway.enabled=true et ddl-auto=validate.
-- ============================================================

-- Import sessions : filtrage par utilisateur et statut
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_import_sessions_user_id
    ON import_sessions(user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_import_sessions_statut
    ON import_sessions(statut);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_import_sessions_user_statut
    ON import_sessions(user_id, statut);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_import_sessions_created_at
    ON import_sessions(created_at DESC);

-- Résultats KPI : jointures fréquentes sur import_session et kpi
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_resultat_kpi_import_session_id
    ON resultat_kpi(import_session_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_resultat_kpi_kpi_id
    ON resultat_kpi(kpi_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_resultat_kpi_user_id
    ON resultat_kpi(user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_resultat_kpi_niveau_variation
    ON resultat_kpi(niveau_variation);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_resultat_kpi_import_kpi
    ON resultat_kpi(import_session_id, kpi_id);

-- KPI : filtrage par catégorie et statut actif
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_kpi_categorie_active
    ON kpi(categorie_id, is_active);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_kpi_active_ordre
    ON kpi(is_active, ordre ASC);

-- Authentification : lookup token (très fréquent)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_refresh_tokens_token
    ON refresh_tokens(token);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_email_tokens_token
    ON email_tokens(token);

-- OTP : lookup par user + statut (used=false)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_otp_codes_user_used_expires
    ON otp_codes(user_id, used, expires_at);

-- Analyses : recherche par session
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analyse_globale_import_session
    ON analyse_globale(import_session_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analyse_categorie_import_session
    ON analyse_categorie(import_session_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analyse_categorie_code
    ON analyse_categorie(import_session_id, categorie_code);

-- KPI raw data : purge quotidienne par date
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_kpi_raw_data_import_session
    ON kpi_raw_data(import_session_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_kpi_import_preview_import_session
    ON kpi_import_preview(import_session_id);
