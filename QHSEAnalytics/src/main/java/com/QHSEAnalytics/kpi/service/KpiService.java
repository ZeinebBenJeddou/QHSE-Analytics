package com.QHSEAnalytics.kpi.service;

import com.QHSEAnalytics.shared.dto.request.CreateKpiRequest;
import com.QHSEAnalytics.shared.dto.request.UpdateKpiRequest;
import com.QHSEAnalytics.shared.dto.response.CategorieKpiResponse;
import com.QHSEAnalytics.shared.dto.response.KpiDeleteResponse;
import com.QHSEAnalytics.shared.dto.response.KpiResponse;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.entity.RagKnowledge;
import com.QHSEAnalytics.shared.exception.CategorieNotFoundException;
import com.QHSEAnalytics.shared.exception.InvalidSeuilException;
import com.QHSEAnalytics.shared.exception.KpiAlreadyExistsException;
import com.QHSEAnalytics.shared.exception.KpiNotFoundException;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.RagKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.QHSEAnalytics.analytics.service.EmbeddingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KpiService {

    private final KpiRepository kpiRepository;
    private final CategorieKpiRepository categorieKpiRepository;
    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    public List<KpiResponse> getKpis(String categorie) {

        if (categorie != null && !categorie.isBlank()) {
            return getKpisByCategorie(categorie);
        }
        return getAllKpis();
    }

    public Page<KpiResponse> getKpis(String categorie, Pageable pageable) {
        if (categorie != null && !categorie.isBlank()) {
            String normalizedCode = normalizeCode(categorie);
            return kpiRepository.findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(normalizedCode, pageable)
                    .map(this::toKpiResponse);
        }
        return kpiRepository.findByIsActiveTrueOrderByOrdreAsc(pageable)
                .map(this::toKpiResponse);
    }

    public List<KpiResponse> getAllKpis() {
        log.info("Récupération de tous les KPIs actifs");
        return kpiRepository.findByIsActiveTrueOrderByOrdreAsc()
                .stream()
                .map(this::toKpiResponse)
                .toList();
    }

    public List<KpiResponse> getKpisByCategorie(String code) {
        String normalizedCode = normalizeCode(code);
        log.info("Récupération des KPIs actifs de catégorie {}", normalizedCode);
        return kpiRepository.findByCategorieKpiCodeAndIsActiveTrueOrderByOrdreAsc(normalizedCode)
                .stream()
                .map(this::toKpiResponse)
                .toList();
    }

    public KpiResponse getKpiById(Long id) {
        log.info("Récupération KPI par id={}", id);
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + id));
        return toKpiResponse(kpi);
    }

    public List<KpiResponse> getInactiveKpis() {
        log.info("Récupération des KPIs inactifs");
        return kpiRepository.findByIsActiveFalse()
                .stream()
                .map(this::toKpiResponse)
                .toList();
    }

    public List<CategorieKpiResponse> getAllCategories() {
        log.info("Récupération de toutes les catégories KPI");
        return categorieKpiRepository.findAll()
                .stream()
                .map(this::toCategorieResponse)
                .toList();
    }

    @Transactional
    public KpiResponse createKpi(CreateKpiRequest request) {
        String normalizedCode = normalizeCode(request.getCategorieCode());
        CategorieKpi categorie = categorieKpiRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new CategorieNotFoundException("Catégorie introuvable: " + normalizedCode));

        if (kpiRepository.existsByNomAndCategorieKpi(request.getNom().trim(), categorie)) {
            throw new KpiAlreadyExistsException("Ce KPI existe déjà dans la catégorie " + normalizedCode);
        }

        validateSeuils(request.getSeuilFaible(), request.getSeuilModere(), request.getSeuilCritique());

        Kpi kpi = Kpi.builder()
                .nom(request.getNom().trim())
                .definition(request.getDefinition().trim())
                .unite(request.getUnite())
                .categorieKpi(categorie)
                .seuilFaible(request.getSeuilFaible())
                .seuilModere(request.getSeuilModere())
                .seuilCritique(request.getSeuilCritique())
                .ordre(request.getOrdre())
                .direction(request.getDirection())
                .targetValue(request.getTargetValue())
                .isActive(true)
                .build();

        Kpi saved = kpiRepository.save(kpi);
        log.info("KPI créé id={} catégorie={} nom={}", saved.getId(), normalizedCode, saved.getNom());

        syncRagKnowledge(saved);
        embedRagEntryAsync(saved.getNom());

        return toKpiResponse(saved);
    }

    @Transactional
    public KpiResponse updateKpi(Long id, UpdateKpiRequest request) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + id));

        String oldName = kpi.getNom();

        CategorieKpi targetCategorie = kpi.getCategorieKpi();
        if (request.getCategorieCode() != null && !request.getCategorieCode().isBlank()) {
            String normalizedCode = normalizeCode(request.getCategorieCode());
            targetCategorie = categorieKpiRepository.findByCode(normalizedCode)
                    .orElseThrow(() -> new CategorieNotFoundException("Catégorie introuvable: " + normalizedCode));
            kpi.setCategorieKpi(targetCategorie);
        }

        String targetNom = kpi.getNom();
        if (request.getNom() != null && !request.getNom().isBlank()) {
            targetNom = request.getNom().trim();
        }

        if (kpiRepository.existsByNomAndCategorieKpiAndIdNot(targetNom, targetCategorie, id)) {
            throw new KpiAlreadyExistsException("Ce KPI existe déjà dans la catégorie " + targetCategorie.getCode());
        }

        Double seuilFaible = request.getSeuilFaible() != null ? request.getSeuilFaible() : kpi.getSeuilFaible();
        Double seuilModere = request.getSeuilModere() != null ? request.getSeuilModere() : kpi.getSeuilModere();
        Double seuilCritique = request.getSeuilCritique() != null ? request.getSeuilCritique() : kpi.getSeuilCritique();
        validateSeuils(seuilFaible, seuilModere, seuilCritique);

        if (request.getNom() != null && !request.getNom().isBlank()) {
            kpi.setNom(request.getNom().trim());
        }
        if (request.getDefinition() != null && !request.getDefinition().isBlank()) {
            kpi.setDefinition(request.getDefinition().trim());
        }
        if (request.getUnite() != null) {
            kpi.setUnite(request.getUnite());
        }
        if (request.getSeuilFaible() != null) {
            kpi.setSeuilFaible(request.getSeuilFaible());
        }
        if (request.getSeuilModere() != null) {
            kpi.setSeuilModere(request.getSeuilModere());
        }
        if (request.getSeuilCritique() != null) {
            kpi.setSeuilCritique(request.getSeuilCritique());
        }
        if (request.getOrdre() != null) {
            kpi.setOrdre(request.getOrdre());
        }
        if (request.getDirection() != null) {
            kpi.setDirection(request.getDirection());
        }
        if (request.getTargetValue() != null) {
            kpi.setTargetValue(request.getTargetValue());
        }

        Kpi saved = kpiRepository.save(kpi);
        log.info("KPI mis à jour id={}", saved.getId());

        updateRagKnowledge(oldName, saved);
        embedRagEntryAsync(saved.getNom());

        return toKpiResponse(saved);
    }

    @Transactional
    public KpiDeleteResponse deleteKpi(Long id) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + id));

        boolean hasLinkedData = kpiRepository.hasLinkedData(id);
        if (!hasLinkedData) {
            kpiRepository.deleteById(id);
            log.info("KPI supprimé définitivement id={}", id);
            deleteRagKnowledge(kpi.getNom());
            return KpiDeleteResponse.builder()
                    .message("KPI supprimé définitivement")
                    .deleted(true)
                    .build();
        }

        kpi.setActive(false);
        kpiRepository.save(kpi);
        log.warn("KPI désactivé (données historiques) id={}", id);
        deleteRagKnowledge(kpi.getNom());
        return KpiDeleteResponse.builder()
                .message("KPI désactivé car il possède des données historiques")
                .deleted(false)
                .build();
    }

    @Transactional
    public KpiResponse restoreKpi(Long id) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + id));

        if (kpi.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le KPI est déjà actif.");
        }

        kpi.setActive(true);
        Kpi saved = kpiRepository.save(kpi);
        log.info("KPI restauré id={}", id);
        syncRagKnowledge(saved);
        embedRagEntryAsync(saved.getNom());
        return toKpiResponse(saved);
    }

    private void validateSeuils(Double faible, Double modere, Double critique) {
        if (!(faible < modere && modere < critique)) {
            throw new InvalidSeuilException("Règle invalide: seuilFaible < seuilModere < seuilCritique");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        return code.trim().toUpperCase();
    }

    private KpiResponse toKpiResponse(Kpi kpi) {
        return KpiResponse.builder()
                .id(kpi.getId())
                .nom(kpi.getNom())
                .definition(kpi.getDefinition())
                .unite(kpi.getUnite())
                .categorieCode(kpi.getCategorieKpi().getCode())
                .categorieLibelle(kpi.getCategorieKpi().getLibelle())
                .seuilFaible(kpi.getSeuilFaible())
                .seuilModere(kpi.getSeuilModere())
                .seuilCritique(kpi.getSeuilCritique())
                .ordre(kpi.getOrdre())
                .isActive(kpi.isActive())
                .createdAt(kpi.getCreatedAt())
                .updatedAt(kpi.getUpdatedAt())
                .direction(kpi.getDirection())
                .targetValue(kpi.getTargetValue())
                .build();
    }

    private CategorieKpiResponse toCategorieResponse(CategorieKpi categorie) {
        int count = kpiRepository.countByCategorieKpiAndIsActiveTrue(categorie);
        return CategorieKpiResponse.builder()
                .id(categorie.getId())
                .code(categorie.getCode())
                .libelle(categorie.getLibelle())
                .description(categorie.getDescription())
                .nombreKpisActifs(count)
                .build();
    }

    private void syncRagKnowledge(Kpi kpi) {
        try {
            // Check existence on the "full" chunk specifically to avoid matching correlation/benchmark chunks
            if (ragKnowledgeRepository.findByKpiNameAndChunkType(kpi.getNom(), "full").isPresent()) {
                return;
            }
            RagKnowledge rag = RagKnowledge.builder()
                    .kpiName(kpi.getNom())
                    .chunkType("full")
                    .definition(kpi.getDefinition())
                    .category(kpi.getCategorieKpi().getCode())
                    .thresholds(buildThresholdJson(kpi))
                    .build();
            ragKnowledgeRepository.save(rag);
            log.info("RAG knowledge synchronisé (créé) pour KPI: {}", kpi.getNom());
        } catch (Exception ex) {
            log.error("Erreur lors de la synchronisation RAG pour KPI {}", kpi.getNom(), ex);
        }
    }

    private void updateRagKnowledge(String oldName, Kpi kpi) {
        try {
            List<RagKnowledge> allChunks = ragKnowledgeRepository.findAllByKpiName(oldName);
            if (allChunks.isEmpty()) {
                syncRagKnowledge(kpi);
                return;
            }
            String newThresholds = buildThresholdJson(kpi);
            for (RagKnowledge chunk : allChunks) {
                chunk.setKpiName(kpi.getNom());
                chunk.setCategory(kpi.getCategorieKpi().getCode());
                // Only update definition and thresholds on the "full" chunk to preserve specialized chunks
                if ("full".equals(chunk.getChunkType()) || chunk.getChunkType() == null) {
                    chunk.setDefinition(kpi.getDefinition());
                    chunk.setThresholds(newThresholds);
                }
            }
            ragKnowledgeRepository.saveAll(allChunks);
            log.info("RAG knowledge mis à jour ({} chunks) pour KPI: {} (ancien nom: {})",
                allChunks.size(), kpi.getNom(), oldName);
        } catch (Exception ex) {
            log.error("Erreur lors de la mise à jour RAG pour KPI {}", kpi.getNom(), ex);
        }
    }

    private void deleteRagKnowledge(String kpiName) {
        try {
            ragKnowledgeRepository.deleteAllByKpiName(kpiName);
            log.info("RAG knowledge supprimé (tous chunks) pour KPI: {}", kpiName);
        } catch (Exception ex) {
            log.error("Erreur lors de la suppression RAG pour KPI {}", kpiName, ex);
        }
    }

    private String buildThresholdJson(Kpi kpi) {
        return String.format(Locale.US, "{\"faible\":%f, \"modere\":%f, \"critique\":%f}",
                kpi.getSeuilFaible(), kpi.getSeuilModere(), kpi.getSeuilCritique());
    }

    private void embedRagEntryAsync(String kpiName) {
        if (!embeddingService.isConfigured()) return;
        CompletableFuture.runAsync(() -> {
            try {
                List<RagKnowledge> chunks = ragKnowledgeRepository.findAllByKpiName(kpiName);
                for (RagKnowledge rag : chunks) {
                    String text = kpiName + ". " + (rag.getDefinition() != null ? rag.getDefinition() : "")
                            + (rag.getCategory() != null ? " Catégorie: " + rag.getCategory() + "." : "");
                    float[] vector = embeddingService.embed(text);
                    if (vector != null) {
                        jdbcTemplate.update(
                            "UPDATE rag_knowledge SET embedding = ?::vector WHERE id = ?",
                            EmbeddingService.toVectorLiteral(vector), rag.getId()
                        );
                        log.debug("[Embed] RAG chunk embedded for KPI: {} chunkType={}", kpiName, rag.getChunkType());
                    }
                }
            } catch (Exception ex) {
                log.warn("[Embed] Failed to embed RAG entry for KPI '{}': {}", kpiName, ex.getMessage());
            }
        });
    }
}
