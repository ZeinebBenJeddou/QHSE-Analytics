-- Stocke la réponse structurée IA (JSON) pour éviter de relancer le LLM à chaque consultation
ALTER TABLE analyse_globales ADD COLUMN IF NOT EXISTS structured_response_json TEXT;
