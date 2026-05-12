ALTER TABLE IF EXISTS resultat_kpis
    ADD COLUMN IF NOT EXISTS periode_n1 INTEGER,
    ADD COLUMN IF NOT EXISTS periode_n INTEGER,
    ADD COLUMN IF NOT EXISTS valeur_n1 DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS valeur_n DOUBLE PRECISION;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'perioden1'
    ) THEN
        EXECUTE '
            UPDATE resultat_kpis
            SET periode_n1 = COALESCE(periode_n1, perioden1)
            WHERE periode_n1 IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'perioden'
    ) THEN
        EXECUTE '
            UPDATE resultat_kpis
            SET periode_n = COALESCE(periode_n, perioden)
            WHERE periode_n IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'valeurn1'
    ) THEN
        EXECUTE '
            UPDATE resultat_kpis
            SET valeur_n1 = COALESCE(valeur_n1, valeurn1)
            WHERE valeur_n1 IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'valeurn'
    ) THEN
        EXECUTE '
            UPDATE resultat_kpis
            SET valeur_n = COALESCE(valeur_n, valeurn)
            WHERE valeur_n IS NULL
        ';
    END IF;
END
$$;

UPDATE resultat_kpis rk
SET periode_n1 = COALESCE(rk.periode_n1, sessions.periode_n1),
    periode_n = COALESCE(rk.periode_n, sessions.periode_n)
FROM import_sessions sessions
WHERE rk.import_session_id = sessions.id
  AND (rk.periode_n1 IS NULL OR rk.periode_n IS NULL);

ALTER TABLE IF EXISTS resultat_kpis
    ALTER COLUMN periode_n1 SET NOT NULL,
    ALTER COLUMN periode_n SET NOT NULL,
    ALTER COLUMN valeur_n1 SET NOT NULL,
    ALTER COLUMN valeur_n SET NOT NULL;

ALTER TABLE IF EXISTS staging_donnees
    ADD COLUMN IF NOT EXISTS valeur_brute_n1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS valeur_brute_n VARCHAR(255),
    ADD COLUMN IF NOT EXISTS valeur_n1 DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS valeur_n DOUBLE PRECISION;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'staging_donnees'
          AND column_name = 'valeur_bruten1'
    ) THEN
        EXECUTE '
            UPDATE staging_donnees
            SET valeur_brute_n1 = COALESCE(valeur_brute_n1, valeur_bruten1)
            WHERE valeur_brute_n1 IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'staging_donnees'
          AND column_name = 'valeur_bruten'
    ) THEN
        EXECUTE '
            UPDATE staging_donnees
            SET valeur_brute_n = COALESCE(valeur_brute_n, valeur_bruten)
            WHERE valeur_brute_n IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'staging_donnees'
          AND column_name = 'valeurn1'
    ) THEN
        EXECUTE '
            UPDATE staging_donnees
            SET valeur_n1 = COALESCE(valeur_n1, valeurn1)
            WHERE valeur_n1 IS NULL
        ';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'staging_donnees'
          AND column_name = 'valeurn'
    ) THEN
        EXECUTE '
            UPDATE staging_donnees
            SET valeur_n = COALESCE(valeur_n, valeurn)
            WHERE valeur_n IS NULL
        ';
    END IF;
END
$$;
