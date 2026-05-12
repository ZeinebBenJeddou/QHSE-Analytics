ALTER TABLE IF EXISTS import_sessions
    ADD COLUMN IF NOT EXISTS periode_n1 INTEGER,
    ADD COLUMN IF NOT EXISTS periode_n INTEGER;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'periode_n1'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'resultat_kpis'
          AND column_name = 'periode_n'
    ) THEN
        EXECUTE $update$
            UPDATE import_sessions session
            SET periode_n1 = source.periode_n1,
                periode_n = source.periode_n
            FROM (
                SELECT import_session_id,
                       MIN(periode_n1) AS periode_n1,
                       MIN(periode_n) AS periode_n
                FROM resultat_kpis
                GROUP BY import_session_id
            ) source
            WHERE session.id = source.import_session_id
              AND (session.periode_n1 IS NULL OR session.periode_n IS NULL)
        $update$;
    END IF;
END
$$;

UPDATE import_sessions
SET periode_n = EXTRACT(YEAR FROM COALESCE(updated_at, created_at))::INTEGER
WHERE periode_n IS NULL;

UPDATE import_sessions
SET periode_n1 = periode_n - 1
WHERE periode_n1 IS NULL
  AND periode_n IS NOT NULL;

ALTER TABLE IF EXISTS import_sessions
    ALTER COLUMN periode_n1 SET NOT NULL,
    ALTER COLUMN periode_n SET NOT NULL;
