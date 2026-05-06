-- Fix thresholds column type to JSON with proper conversion
ALTER TABLE rag_knowledge ALTER COLUMN thresholds TYPE JSON USING thresholds::json;