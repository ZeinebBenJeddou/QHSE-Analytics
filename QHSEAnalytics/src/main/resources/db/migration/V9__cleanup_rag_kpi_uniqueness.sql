DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class tbl ON tbl.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
        WHERE tbl.relname = 'rag_knowledge'
          AND ns.nspname = current_schema()
          AND con.contype = 'u'
          AND (
              SELECT array_agg(att.attname::text ORDER BY cols.ordinality)
              FROM unnest(con.conkey) WITH ORDINALITY AS cols(attnum, ordinality)
              JOIN pg_attribute att
                ON att.attrelid = tbl.oid
               AND att.attnum = cols.attnum
          ) = ARRAY['kpi_name']::text[]
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
            current_schema(),
            'rag_knowledge',
            constraint_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    index_name TEXT;
BEGIN
    FOR index_name IN
        SELECT idx.relname
        FROM pg_index ind
        JOIN pg_class tbl ON tbl.oid = ind.indrelid
        JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
        JOIN pg_class idx ON idx.oid = ind.indexrelid
        WHERE tbl.relname = 'rag_knowledge'
          AND ns.nspname = current_schema()
          AND ind.indisunique
          AND pg_get_indexdef(ind.indexrelid) ILIKE '%(kpi_name)%'
          AND pg_get_indexdef(ind.indexrelid) NOT ILIKE '%chunk_type%'
    LOOP
        EXECUTE format(
            'DROP INDEX IF EXISTS %I.%I',
            current_schema(),
            index_name
        );
    END LOOP;
END $$;

UPDATE rag_knowledge
SET chunk_type = 'full'
WHERE chunk_type IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_rag_kpi_chunk
    ON rag_knowledge (kpi_name, chunk_type);
