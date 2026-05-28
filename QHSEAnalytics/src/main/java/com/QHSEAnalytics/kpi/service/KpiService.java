package com.QHSEAnalytics.kpi.service;

import com.QHSEAnalytics.shared.dto.request.CreateKpiRequest;
import com.QHSEAnalytics.shared.dto.request.UpdateKpiRequest;
import com.QHSEAnalytics.shared.dto.response.CategorieKpiResponse;
import com.QHSEAnalytics.shared.dto.response.KpiDeleteResponse;
import com.QHSEAnalytics.shared.dto.response.KpiResponse;
import com.QHSEAnalytics.shared.entity.CategorieKpi;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.exception.CategorieNotFoundException;
import com.QHSEAnalytics.shared.exception.InvalidSeuilException;
import com.QHSEAnalytics.shared.exception.KpiAlreadyExistsException;
import com.QHSEAnalytics.shared.exception.KpiNotFoundException;
import com.QHSEAnalytics.shared.repository.CategorieKpiRepository;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KpiService {

    private final KpiRepository kpiRepository;
    private final CategorieKpiRepository categorieKpiRepository;

    public Page<KpiResponse> getKpis(String categorie, Pageable pageable) {
        if (categorie != null && !categorie.isBlank()) {
            String normalizedCode = normalizeCode(categorie);
            return kpiRepository.findByCategorieKpiCodeAndIsActiveTrueOrderByNomAsc(normalizedCode, pageable)
                    .map(this::toKpiResponse);
        }
        return kpiRepository.findByIsActiveTrueOrderByNomAsc(pageable)
                .map(this::toKpiResponse);
    }

    public List<KpiResponse> getAllKpis() {
        log.info("Récupération de tous les KPIs actifs");
        return kpiRepository.findByIsActiveTrueOrderByNomAsc()
                .stream()
                .map(this::toKpiResponse)
                .toList();
    }

    public List<KpiResponse> getKpisByCategorie(String code) {
        String normalizedCode = normalizeCode(code);
        log.info("Récupération des KPIs actifs de catégorie {}", normalizedCode);
        return kpiRepository.findByCategorieKpiCodeAndIsActiveTrueOrderByNomAsc(normalizedCode)
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

        validateSeuils(request.getSeuilFaible(), request.getSeuilModere(), request.getSeuilCritique());

        // Si un KPI inactif existe déjà avec ce nom dans cette catégorie → le réactiver + màj
        Optional<Kpi> existing = kpiRepository.findByNomAndCategorieKpi(request.getNom().trim(), categorie);
        if (existing.isPresent()) {
            Kpi kpi = existing.get();
            if (kpi.isActive()) {
                throw new KpiAlreadyExistsException("Ce KPI est déjà actif dans la catégorie " + normalizedCode);
            }
            kpi.setActive(true);
            kpi.setDefinition(request.getDefinition().trim());
            kpi.setUnite(request.getUnite());
            kpi.setSeuilFaible(request.getSeuilFaible());
            kpi.setSeuilModere(request.getSeuilModere());
            kpi.setSeuilCritique(request.getSeuilCritique());
            if (request.getDirection() != null) kpi.setDirection(request.getDirection());
            Kpi saved = kpiRepository.save(kpi);
            log.info("KPI réactivé id={} catégorie={} nom={}", saved.getId(), normalizedCode, saved.getNom());
            return toKpiResponse(saved);
        }

        Kpi kpi = Kpi.builder()
                .nom(request.getNom().trim())
                .definition(request.getDefinition().trim())
                .unite(request.getUnite())
                .categorieKpi(categorie)
                .seuilFaible(request.getSeuilFaible())
                .seuilModere(request.getSeuilModere())
                .seuilCritique(request.getSeuilCritique())
                .direction(request.getDirection())
                .isActive(true)
                .build();

        Kpi saved = kpiRepository.save(kpi);
        log.info("KPI créé id={} catégorie={} nom={}", saved.getId(), normalizedCode, saved.getNom());

        return toKpiResponse(saved);
    }

    @Transactional
    public KpiResponse updateKpi(Long id, UpdateKpiRequest request) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new KpiNotFoundException("KPI introuvable avec id=" + id));

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
        if (request.getDirection() != null) {
            kpi.setDirection(request.getDirection());
        }

        Kpi saved = kpiRepository.save(kpi);
        log.info("KPI mis à jour id={}", saved.getId());

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
            return KpiDeleteResponse.builder()
                    .message("KPI supprimé définitivement")
                    .deleted(true)
                    .build();
        }

        kpi.setActive(false);
        kpiRepository.save(kpi);
        log.warn("KPI désactivé (données historiques) id={}", id);
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
        return toKpiResponse(saved);
    }

    private void validateSeuils(Double faible, Double modere, Double critique) {
        if (faible < 0 || modere < 0 || critique < 0) {
            throw new InvalidSeuilException("Les seuils ne peuvent pas être négatifs.");
        }
        if (faible >= modere) {
            throw new InvalidSeuilException("Le seuil faible doit être inférieur au seuil modéré.");
        }
        if (modere >= critique) {
            throw new InvalidSeuilException("Le seuil modéré doit être inférieur au seuil critique.");
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
                .isActive(kpi.isActive())
                .createdAt(kpi.getCreatedAt())
                .updatedAt(kpi.getUpdatedAt())
                .direction(kpi.getDirection())
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

}
