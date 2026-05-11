

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE rag_knowledge ADD COLUMN IF NOT EXISTS embedding vector(768);

CREATE INDEX IF NOT EXISTS rag_knowledge_embedding_idx
    ON rag_knowledge USING ivfflat (embedding vector_cosine_ops) WITH (lists=10);
