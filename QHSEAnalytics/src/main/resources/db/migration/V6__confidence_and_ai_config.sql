-- Feature 3: persist AI confidence score per analyse
ALTER TABLE analyse_globales ADD COLUMN IF NOT EXISTS overall_confidence INTEGER;

-- Feature 5: runtime LLM configuration
CREATE TABLE IF NOT EXISTS ai_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       VARCHAR(500) NOT NULL,
    description VARCHAR(512),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO ai_config (key, value, description) VALUES
    ('groq.temperature.json',  '0.1',  'Température Groq pour les réponses JSON (0.0–2.0)'),
    ('groq.temperature.text',  '0.5',  'Température Groq pour les réponses texte (0.0–2.0)'),
    ('groq.timeout.seconds',   '30',   'Timeout des appels Groq en secondes'),
    ('rag.search.threshold',   '0.5',  'Seuil de similarité pour la recherche RAG (0.0–1.0)'),
    ('rag.search.top.k',       '5',    'Nombre de résultats retournés par la recherche RAG')
ON CONFLICT (key) DO NOTHING;
