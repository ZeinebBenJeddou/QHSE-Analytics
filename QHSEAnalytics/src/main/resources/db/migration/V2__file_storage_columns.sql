
CREATE INDEX IF NOT EXISTS idx_import_sessions_user_id
    ON import_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_import_sessions_statut
    ON import_sessions(statut);

CREATE INDEX IF NOT EXISTS idx_import_sessions_user_statut
    ON import_sessions(user_id, statut);

CREATE INDEX IF NOT EXISTS idx_import_sessions_created_at
    ON import_sessions(created_at DESC);

-- Résultats KPI : jointures fréquentes
CREATE INDEX IF NOT EXISTS idx_resultat_kpis_import_session_id
    ON resultat_kpis(import_session_id);

CREATE INDEX IF NOT EXISTS idx_resultat_kpis_kpi_id
    ON resultat_kpis(kpi_id);

CREATE INDEX IF NOT EXISTS idx_resultat_kpis_user_id
    ON resultat_kpis(user_id);

CREATE INDEX IF NOT EXISTS idx_resultat_kpis_niveau_variation
    ON resultat_kpis(niveau_variation);

CREATE INDEX IF NOT EXISTS idx_resultat_kpis_import_kpi
    ON resultat_kpis(import_session_id, kpi_id);


CREATE INDEX IF NOT EXISTS idx_kpi_categorie_active
    ON kpi(categorie_id, is_active);

CREATE INDEX IF NOT EXISTS idx_kpi_active_ordre
    ON kpi(is_active, ordre ASC);


CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON refresh_tokens(token);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_email_tokens_token
    ON email_tokens(token);


CREATE INDEX IF NOT EXISTS idx_otp_codes_user_used_expires
    ON otp_codes(user_id, used, expires_at);


CREATE INDEX IF NOT EXISTS idx_analyse_globales_import_session
    ON analyse_globales(import_session_id);

CREATE INDEX IF NOT EXISTS idx_analyse_categories_import_session
    ON analyse_categories(import_session_id);

CREATE INDEX IF NOT EXISTS idx_analyse_categories_code
    ON analyse_categories(import_session_id, categorie_code);


CREATE INDEX IF NOT EXISTS idx_kpi_raw_data_import_session
    ON kpi_raw_data(import_session_id);

CREATE INDEX IF NOT EXISTS idx_kpi_import_preview_import_session
    ON kpi_import_preview(import_session_id);
