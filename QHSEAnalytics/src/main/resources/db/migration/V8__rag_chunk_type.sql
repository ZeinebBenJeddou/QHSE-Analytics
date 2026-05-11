ALTER TABLE rag_knowledge DROP CONSTRAINT IF EXISTS rag_knowledge_kpi_name_key;
ALTER TABLE rag_knowledge ADD COLUMN IF NOT EXISTS chunk_type VARCHAR(50) DEFAULT 'full';
UPDATE rag_knowledge SET chunk_type = 'full' WHERE chunk_type IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_rag_kpi_chunk ON rag_knowledge(kpi_name, chunk_type);
DROP INDEX IF EXISTS rag_knowledge_embedding_idx;
CREATE INDEX IF NOT EXISTS rag_embedding_ivfflat ON rag_knowledge
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);
