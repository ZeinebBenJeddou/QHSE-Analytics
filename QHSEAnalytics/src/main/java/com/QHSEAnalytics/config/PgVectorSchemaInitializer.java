package com.QHSEAnalytics.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializePgVector() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("[pgvector] Extension 'vector' ready.");

            jdbcTemplate.execute(
                "ALTER TABLE rag_knowledge ADD COLUMN IF NOT EXISTS embedding vector(768)");
            log.info("[pgvector] Column 'embedding' ready on rag_knowledge.");

            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS rag_knowledge_embedding_idx " +
                "ON rag_knowledge USING ivfflat (embedding vector_cosine_ops) WITH (lists=10)");
            log.info("[pgvector] IVFFlat index ready on rag_knowledge.embedding.");

        } catch (DataAccessException ex) {
            log.warn("[pgvector] pgvector extension not available — vector search disabled, keyword fallback active. Reason: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("[pgvector] Schema initialization failed: {}", ex.getMessage());
        }
    }
}
