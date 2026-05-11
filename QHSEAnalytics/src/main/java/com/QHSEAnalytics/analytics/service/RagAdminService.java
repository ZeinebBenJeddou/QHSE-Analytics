package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.dto.request.RagKnowledgeRequest;
import com.QHSEAnalytics.shared.dto.request.RagSearchTestRequest;
import com.QHSEAnalytics.shared.dto.response.RagKnowledgeResponse;
import com.QHSEAnalytics.shared.dto.response.RagSearchTestResultItem;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagAdminService {

    private final RagKnowledgeRepository repository;
    private final EmbeddingService embeddingService;
    private final RagSearchService ragSearchService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<RagKnowledgeResponse> getAll() {
        List<RagKnowledge> entities = repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (entities.isEmpty()) return List.of();

        Set<Long> embeddedIds = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT id FROM rag_knowledge WHERE embedding IS NOT NULL", Long.class
        ));

        return entities.stream()
                .map(e -> toResponse(e, embeddedIds.contains(e.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public RagKnowledgeResponse create(RagKnowledgeRequest request) {
        String kpiName = request.getKpiName().trim();
        if (repository.findByKpiNameAndChunkType(kpiName, "full").isPresent()) {
            throw new DataIntegrityViolationException("Une entrée RAG avec ce nom existe déjà : " + kpiName);
        }

        RagKnowledge entity = RagKnowledge.builder()
                .kpiName(kpiName)
                .chunkType("full")
                .definition(request.getDefinition())
                .thresholds(request.getThresholds())
                .category(request.getCategory())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entity = repository.save(entity);
        embedAndStore(entity);
        boolean hasEmbedding = hasEmbedding(entity.getId());
        return toResponse(entity, hasEmbedding);
    }

    @Transactional
    public RagKnowledgeResponse update(Long id, RagKnowledgeRequest request) {
        RagKnowledge entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrée RAG introuvable : " + id));

        String newName = request.getKpiName().trim();
        if (!newName.equals(entity.getKpiName())) {
            repository.findByKpiNameAndChunkType(newName, "full").ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new DataIntegrityViolationException("Une entrée RAG avec ce nom existe déjà : " + newName);
                }
            });
        }

        entity.setKpiName(newName);
        entity.setDefinition(request.getDefinition());
        entity.setThresholds(request.getThresholds());
        entity.setCategory(request.getCategory());
        entity = repository.save(entity);
        embedAndStore(entity);
        boolean hasEmbedding = hasEmbedding(entity.getId());
        return toResponse(entity, hasEmbedding);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Entrée RAG introuvable : " + id);
        }
        repository.deleteById(id);
    }

    public List<RagSearchTestResultItem> testSearch(RagSearchTestRequest request) {
        int topK = request.getTopK() > 0 ? request.getTopK() : 5;
        double threshold = request.getThreshold() > 0 ? request.getThreshold() : 0.50;
        List<RagKnowledge> results = ragSearchService.findRelevant(request.getQuery(), topK, null, threshold);
        return results.stream()
                .map(r -> RagSearchTestResultItem.builder()
                        .id(r.getId())
                        .kpiName(r.getKpiName())
                        .category(r.getCategory())
                        .definition(r.getDefinition())
                        .build())
                .collect(Collectors.toList());
    }

    private void embedAndStore(RagKnowledge entity) {
        String text = entity.getKpiName() + ". " +
                      (entity.getDefinition() != null ? entity.getDefinition() : "");
        float[] vector = embeddingService.embed(text);
        if (vector != null) {
            jdbcTemplate.update(
                "UPDATE rag_knowledge SET embedding = ?::vector WHERE id = ?",
                EmbeddingService.toVectorLiteral(vector), entity.getId()
            );
            log.info("[RagAdmin] Embedding calculé pour id={}", entity.getId());
        } else {
            log.warn("[RagAdmin] Embedding indisponible pour id={} (API non configurée ou rate-limitée)", entity.getId());
        }
    }

    private boolean hasEmbedding(Long id) {
        Boolean result = jdbcTemplate.queryForObject(
            "SELECT embedding IS NOT NULL FROM rag_knowledge WHERE id = ?", Boolean.class, id
        );
        return Boolean.TRUE.equals(result);
    }

    private RagKnowledgeResponse toResponse(RagKnowledge e, boolean hasEmbedding) {
        return RagKnowledgeResponse.builder()
                .id(e.getId())
                .kpiName(e.getKpiName())
                .definition(e.getDefinition())
                .thresholds(e.getThresholds())
                .category(e.getCategory())
                .hasEmbedding(hasEmbedding)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
