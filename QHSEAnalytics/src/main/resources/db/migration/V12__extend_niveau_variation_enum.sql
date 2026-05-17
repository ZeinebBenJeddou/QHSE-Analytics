-- Extend niveau_variation column in resultat_kpis to support all ClassificationEngine levels.
-- PostgreSQL stores enum values as VARCHAR(20); we widen the check constraint rather than
-- using a native ENUM type so that adding values is a non-breaking DDL operation.

DO $$
BEGIN
    -- Widen the column to VARCHAR(20) if it is still a shorter VARCHAR
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name   = 'resultat_kpis'
          AND column_name  = 'niveau_variation'
          AND character_maximum_length < 20
    ) THEN
        ALTER TABLE resultat_kpis ALTER COLUMN niveau_variation TYPE VARCHAR(20);
    END IF;
END
$$;

-- Drop the old check constraint if it exists and only allowed FAIBLE/MODERE/CRITIQUE
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'resultat_kpis'::regclass
      AND contype  = 'c'
      AND pg_get_constraintdef(oid) LIKE '%niveau_variation%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE resultat_kpis DROP CONSTRAINT %I', constraint_name);
    END IF;
END
$$;

-- Re-add constraint with the full set of ClassificationEngine levels
ALTER TABLE resultat_kpis
    ADD CONSTRAINT chk_niveau_variation
    CHECK (niveau_variation IN ('EXCELLENT','FAIBLE','MODERE','PRE_ESCALADE','CRITIQUE','INDETERMINE'));

-- Migrate legacy rows that have values not in the new set
UPDATE resultat_kpis
SET niveau_variation = 'INDETERMINE'
WHERE niveau_variation NOT IN ('EXCELLENT','FAIBLE','MODERE','PRE_ESCALADE','CRITIQUE','INDETERMINE');
