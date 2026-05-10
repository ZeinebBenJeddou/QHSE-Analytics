package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.entity.RagKnowledge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagSearchService {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.rag.embedding.fallback-threshold:0.50}")
    private double fallbackThreshold;

    private static final RowMapper<RagKnowledge> ROW_MAPPER = (rs, rowNum) -> {
        RagKnowledge r = new RagKnowledge();
        r.setId(rs.getLong("id"));
        r.setKpiName(rs.getString("kpi_name"));
        r.setDefinition(rs.getString("definition"));
        r.setThresholds(rs.getString("thresholds"));
        r.setCategory(rs.getString("category"));
        return r;
    };

    /**
     * Find the top-K most relevant RAG entries for a query.
     * Uses cosine similarity via pgvector if available; falls back to ILIKE keyword search.
     */
    public List<RagKnowledge> findRelevant(String query, int topK, String category, double threshold) {
        float[] vector = embeddingService.embed(query);
        if (vector != null) {
            String vectorLiteral = EmbeddingService.toVectorLiteral(vector);
            try {
                List<RagKnowledge> results = vectorSearch(vectorLiteral, topK, category, threshold);
                if (!results.isEmpty()) return results;
                // retry with lower threshold before falling back to keyword
                results = vectorSearch(vectorLiteral, topK, category, fallbackThreshold);
                if (!results.isEmpty()) return results;
            } catch (DataAccessException ex) {
                log.warn("[RAG] pgvector unavailable, falling back to keyword search: {}", ex.getMessage());
            }
        }
        return keywordSearch(query, topK);
    }

    private List<RagKnowledge> vectorSearch(String vectorLiteral, int topK, String category, double threshold) {
        if (category != null && !category.isBlank()) {
            return jdbcTemplate.query(
                "SELECT id, kpi_name, definition, thresholds, category FROM rag_knowledge " +
                "WHERE category = ? AND embedding IS NOT NULL " +
                "AND (1 - (embedding <=> ?::vector)) >= ? " +
                "ORDER BY embedding <=> ?::vector LIMIT ?",
                ROW_MAPPER,
                category, vectorLiteral, threshold, vectorLiteral, topK
            );
        }
        return jdbcTemplate.query(
            "SELECT id, kpi_name, definition, thresholds, category FROM rag_knowledge " +
            "WHERE embedding IS NOT NULL " +
            "AND (1 - (embedding <=> ?::vector)) >= ? " +
            "ORDER BY embedding <=> ?::vector LIMIT ?",
            ROW_MAPPER,
            vectorLiteral, threshold, vectorLiteral, topK
        );
    }

    /**
     * Keyword fallback using ILIKE.
     * When the query is a comma-separated list (e.g. from AnalysisAgent combined query),
     * search for each individual term with OR so every term gets a chance to match.
     * Capped at 5 terms to keep the query cheap.
     */
    private List<RagKnowledge> keywordSearch(String query, int topK) {
        String[] terms = query.contains(",")
            ? query.split(",")
            : new String[]{query};

        StringBuilder sql = new StringBuilder(
            "SELECT id, kpi_name, definition, thresholds, category FROM rag_knowledge WHERE ");
        List<Object> params = new ArrayList<>();
        int added = 0;
        for (String raw : terms) {
            String term = raw.trim();
            if (term.isBlank()) continue;
            if (term.length() > 100) term = term.substring(0, 100);
            if (added > 0) sql.append(" OR ");
            sql.append("(kpi_name ILIKE ? OR definition ILIKE ?)");
            params.add("%" + term + "%");
            params.add("%" + term + "%");
            if (++added == 5) break;
        }
        if (added == 0) return List.of();

        sql.append(" LIMIT ?");
        params.add(topK);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }
}
