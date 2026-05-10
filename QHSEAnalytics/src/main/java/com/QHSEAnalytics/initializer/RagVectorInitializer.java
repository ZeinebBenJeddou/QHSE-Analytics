package com.QHSEAnalytics.initializer;

import com.QHSEAnalytics.analytics.service.EmbeddingService;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RagVectorInitializer implements ApplicationRunner {

    private final EmbeddingService embeddingService;
    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!embeddingService.isConfigured()) {
            log.info("[RagVectorInit] Gemini API key not configured — skipping vector initialization.");
            return;
        }

        List<Long> ids;
        try {
            ids = jdbcTemplate.queryForList(
                "SELECT id FROM rag_knowledge WHERE embedding IS NULL",
                Long.class
            );
        } catch (Exception ex) {
            log.warn("[RagVectorInit] Cannot query rag_knowledge: {}", ex.getMessage());
            return;
        }

        if (ids.isEmpty()) {
            log.info("[RagVectorInit] All RAG entries already have embeddings.");
            return;
        }

        log.info("[RagVectorInit] Embedding {} RAG entries without vectors...", ids.size());
        int success = 0;
        int failed = 0;

        for (Long id : ids) {
            try {
                RagKnowledge rag = ragKnowledgeRepository.findById(id).orElse(null);
                if (rag == null) continue;

                String text = buildEmbeddingText(rag);
                float[] vector = embeddingService.embed(text);
                if (vector == null) {
                    failed++;
                    continue;
                }

                String vectorLiteral = EmbeddingService.toVectorLiteral(vector);
                jdbcTemplate.update(
                    "UPDATE rag_knowledge SET embedding = ?::vector WHERE id = ?",
                    vectorLiteral, id
                );
                success++;
                Thread.sleep(200);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[RagVectorInit] Interrupted — stopping early.");
                break;
            } catch (Exception ex) {
                log.warn("[RagVectorInit] Failed to embed entry id={}: {}", id, ex.getMessage());
                failed++;
            }
        }

        log.info("[RagVectorInit] Done — {} embedded, {} failed.", success, failed);
    }

    private String buildEmbeddingText(RagKnowledge rag) {
        StringBuilder sb = new StringBuilder();
        if (rag.getKpiName() != null) sb.append(rag.getKpiName()).append(". ");
        if (rag.getDefinition() != null) sb.append(rag.getDefinition());
        if (rag.getCategory() != null) sb.append(" Catégorie: ").append(rag.getCategory()).append(".");
        return sb.toString().trim();
    }
}
