package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import com.QHSEAnalytics.shared.service.KpiKnowledgeLookup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagKpiKnowledgeLookupService implements KpiKnowledgeLookup {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RagKnowledgeRepository ragKnowledgeRepository;

    @Value("${app.rag.embedding.threshold:0.72}")
    private double primaryThreshold;

    @Value("${app.rag.embedding.fallback-threshold:0.50}")
    private double fallbackThreshold;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<KnowledgeMatch> findBestMatch(String kpiName, String categoryCode, Double currentValue, Double previousValue) {
        if (kpiName == null || kpiName.isBlank()) {
            return Optional.empty();
        }

        String query = buildQuery(kpiName, categoryCode, currentValue, previousValue);
        float[] vector = embeddingService.embed(query);
        if (vector != null) {
            String vectorLiteral = EmbeddingService.toVectorLiteral(vector);
            try {
                Optional<KnowledgeMatch> primary = vectorSearch(vectorLiteral, categoryCode, primaryThreshold);
                if (primary.isPresent()) {
                    return primary;
                }
                Optional<KnowledgeMatch> fallback = vectorSearch(vectorLiteral, categoryCode, fallbackThreshold);
                if (fallback.isPresent()) {
                    return fallback;
                }
            } catch (DataAccessException ex) {
                log.warn("[RAG][Lookup] Vector search unavailable for '{}': {}", kpiName, ex.getMessage());
            }
        }

        return exactNameFallback(kpiName, categoryCode);
    }

    private Optional<KnowledgeMatch> vectorSearch(String vectorLiteral, String categoryCode, double threshold) {
        List<ScoredKnowledgeRow> rows;
        if (categoryCode != null && !categoryCode.isBlank()) {
            rows = jdbcTemplate.query(
                    "SELECT id, kpi_name, definition, thresholds, category, " +
                            "direction, " +
                            "(1 - (embedding <=> CAST(? AS vector))) AS similarity " +
                            "FROM rag_knowledge " +
                            "WHERE category = ? AND embedding IS NOT NULL " +
                            "AND (1 - (embedding <=> CAST(? AS vector))) >= ? " +
                            "ORDER BY embedding <=> CAST(? AS vector) " +
                            "LIMIT 1",
                    (rs, rowNum) -> new ScoredKnowledgeRow(
                            rs.getLong("id"),
                            rs.getString("kpi_name"),
                            rs.getString("definition"),
                            rs.getString("thresholds"),
                            rs.getString("category"),
                            rs.getString("direction"),
                            rs.getDouble("similarity")
                    ),
                    vectorLiteral, categoryCode, vectorLiteral, threshold, vectorLiteral
            );
        } else {
            rows = jdbcTemplate.query(
                    "SELECT id, kpi_name, definition, thresholds, category, " +
                            "direction, " +
                            "(1 - (embedding <=> CAST(? AS vector))) AS similarity " +
                            "FROM rag_knowledge " +
                            "WHERE embedding IS NOT NULL " +
                            "AND (1 - (embedding <=> CAST(? AS vector))) >= ? " +
                            "ORDER BY embedding <=> CAST(? AS vector) " +
                            "LIMIT 1",
                    (rs, rowNum) -> new ScoredKnowledgeRow(
                            rs.getLong("id"),
                            rs.getString("kpi_name"),
                            rs.getString("definition"),
                            rs.getString("thresholds"),
                            rs.getString("category"),
                            rs.getString("direction"),
                            rs.getDouble("similarity")
                    ),
                    vectorLiteral, threshold, vectorLiteral, vectorLiteral
            );
        }
        return rows.stream()
                .findFirst()
                .flatMap(this::toKnowledgeMatch);
    }

    private Optional<KnowledgeMatch> exactNameFallback(String kpiName, String categoryCode) {
        try {
            return exactNameFallbackInternal(kpiName, categoryCode);
        } catch (DataAccessException ex) {
            log.warn("[RAG][Lookup] Name fallback unavailable for '{}': {}", kpiName, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<KnowledgeMatch> exactNameFallbackInternal(String kpiName, String categoryCode) {
        Optional<RagKnowledge> exact = ragKnowledgeRepository.findByKpiNameIgnoreCase(kpiName);
        if (exact.isPresent() && categoryMatches(exact.get().getCategory(), categoryCode)) {
            return toKnowledgeMatch(new ScoredKnowledgeRow(
                    exact.get().getId(),
                    exact.get().getKpiName(),
                    exact.get().getDefinition(),
                    exact.get().getThresholds(),
                    exact.get().getCategory(),
                    exact.get().getDirection(),
                    null
            ));
        }

        return ragKnowledgeRepository.findByKpiNameContainingIgnoreCase(kpiName.trim())
                .stream()
                .filter(entry -> categoryMatches(entry.getCategory(), categoryCode))
                .findFirst()
                .flatMap(entry -> toKnowledgeMatch(new ScoredKnowledgeRow(
                        entry.getId(),
                        entry.getKpiName(),
                        entry.getDefinition(),
                        entry.getThresholds(),
                        entry.getCategory(),
                        entry.getDirection(),
                        null
                )));
    }

    private Optional<KnowledgeMatch> toKnowledgeMatch(ScoredKnowledgeRow row) {
        if (row == null) {
            return Optional.empty();
        }

        ThresholdValues thresholds = parseThresholds(row.thresholds());
        Direction direction = parseDirectionFromField(row.direction());
        if (direction == null) {
            direction = parseDirection(row.definition());
        }

        if (thresholds.isEmpty() && direction == null) {
            return Optional.empty();
        }

        return Optional.of(new KnowledgeMatch(
                row.kpiName(),
                row.category(),
                row.definition(),
                thresholds.faible(),
                thresholds.modere(),
                thresholds.critique(),
                direction,
                row.similarity()
        ));
    }

    private ThresholdValues parseThresholds(String rawThresholds) {
        if (rawThresholds == null || rawThresholds.isBlank()) {
            return ThresholdValues.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(rawThresholds);
            return new ThresholdValues(
                    readNullableDouble(node, "faible"),
                    readNullableDouble(node, "modere"),
                    readNullableDouble(node, "critique")
            );
        } catch (Exception ex) {
            log.warn("[RAG][Lookup] Cannot parse thresholds JSON '{}': {}", rawThresholds, ex.getMessage());
            return ThresholdValues.empty();
        }
    }

    private Double readNullableDouble(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.asDouble();
    }

    private Direction parseDirection(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }

        String normalized = normalize(definition);
        if (normalized.contains("directionlowerbetter") || normalized.contains("directionlowerisbetter")) {
            return Direction.LOWER_IS_BETTER;
        }
        if (normalized.contains("directionhigherbetter") || normalized.contains("directionhigherisbetter")) {
            return Direction.HIGHER_IS_BETTER;
        }
        if (normalized.contains("directiontargetisbest") || normalized.contains("directiontargetbest")) {
            return Direction.TARGET_IS_BEST;
        }
        return null;
    }

    private Direction parseDirectionFromField(String directionField) {
        if (directionField == null || directionField.isBlank()) return null;
        try {
            return Direction.valueOf(directionField.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildQuery(String kpiName, String categoryCode, Double currentValue, Double previousValue) {
        StringBuilder query = new StringBuilder(kpiName.trim());
        if (categoryCode != null && !categoryCode.isBlank()) {
            query.append(". Catégorie: ").append(categoryCode.trim());
        }
        if (currentValue != null) {
            query.append(". Valeur N: ").append(String.format(Locale.US, "%.4f", currentValue));
        }
        if (previousValue != null) {
            query.append(". Valeur N-1: ").append(String.format(Locale.US, "%.4f", previousValue));
        }
        return query.toString();
    }

    private boolean categoryMatches(String candidate, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        return candidate.trim().equalsIgnoreCase(expected.trim());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private record ScoredKnowledgeRow(
            Long id,
            String kpiName,
            String definition,
            String thresholds,
            String category,
            String direction,
            Double similarity
    ) {
    }

    private record ThresholdValues(Double faible, Double modere, Double critique) {
        static ThresholdValues empty() {
            return new ThresholdValues(null, null, null);
        }

        boolean isEmpty() {
            return faible == null && modere == null && critique == null;
        }
    }
}
