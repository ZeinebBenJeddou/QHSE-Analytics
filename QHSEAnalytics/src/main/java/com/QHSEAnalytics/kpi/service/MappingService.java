package com.QHSEAnalytics.kpi.service;

import com.QHSEAnalytics.shared.dto.request.MappingTemplateRequest;
import com.QHSEAnalytics.shared.dto.response.MappingTemplateResponse;
import com.QHSEAnalytics.shared.dto.response.MappingTemplateResponse.MappingItemResponse;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.entity.MappingConfig;
import com.QHSEAnalytics.shared.exception.KpiNotFoundException;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.MappingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MappingService {

    private final MappingConfigRepository mappingConfigRepository;
    private final KpiRepository kpiRepository;


    public List<MappingTemplateResponse> getTemplates(Long userId) {
        List<MappingConfig> configs = mappingConfigRepository
                .findByUserIdOrderByTemplateNameAscCreatedAtDesc(userId);


        Map<String, List<MappingConfig>> grouped = configs.stream()
                .collect(Collectors.groupingBy(MappingConfig::getTemplateName, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> toTemplateResponse(entry.getKey(), entry.getValue()))
                .toList();
    }


    @Transactional
    public MappingTemplateResponse saveTemplate(Long userId, MappingTemplateRequest request) {

        mappingConfigRepository.deleteByUserIdAndTemplateName(userId, request.getTemplateName().trim());

        List<MappingConfig> toSave = new ArrayList<>();
        for (MappingTemplateRequest.MappingConfigItemRequest item : request.getMappings()) {
            MappingConfig.MappingConfigBuilder builder = MappingConfig.builder()
                    .templateName(request.getTemplateName().trim())
                    .excelColumn(item.getExcelColumn().trim())
                    .userId(userId)
                    .categorieCode(item.getCategorieCode());

            if (item.getKpiId() != null) {
                Kpi kpi = kpiRepository.findById(item.getKpiId())
                        .orElseThrow(() -> new KpiNotFoundException("KPI introuvable id=" + item.getKpiId()));
                builder.kpi(kpi);
            }
            toSave.add(builder.build());
        }

        List<MappingConfig> saved = mappingConfigRepository.saveAll(toSave);
        log.info("Template '{}' sauvegardé pour userId={} ({} colonnes)", request.getTemplateName(), userId, saved.size());
        return toTemplateResponse(request.getTemplateName(), saved);
    }



    @Transactional
    public void deleteTemplate(Long id, Long userId) {

        MappingConfig config = mappingConfigRepository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Template introuvable ou accès refusé."));
        mappingConfigRepository.deleteByUserIdAndTemplateName(userId, config.getTemplateName());
        log.info("Template '{}' supprimé pour userId={}", config.getTemplateName(), userId);
    }


    public Map<String, Kpi> resolveMapping(Long userId, String templateName) {
        List<MappingConfig> configs = mappingConfigRepository
                .findByUserIdAndTemplateName(userId, templateName);
        Map<String, Kpi> result = new LinkedHashMap<>();
        for (MappingConfig c : configs) {
            if (c.getKpi() != null) {
                result.put(c.getExcelColumn().toLowerCase().trim(), c.getKpi());
            }
        }
        return result;
    }



    private MappingTemplateResponse toTemplateResponse(String name, List<MappingConfig> configs) {

        Long templateId = configs.isEmpty() ? null : configs.get(0).getId();
        return MappingTemplateResponse.builder()
                .id(templateId)
                .templateName(name)
                .createdAt(configs.isEmpty() ? null : configs.get(0).getCreatedAt())
                .mappings(configs.stream().map(this::toItemResponse).toList())
                .build();
    }

    private MappingItemResponse toItemResponse(MappingConfig config) {
        return MappingItemResponse.builder()
                .id(config.getId())
                .excelColumn(config.getExcelColumn())
                .kpiId(config.getKpi() != null ? config.getKpi().getId() : null)
                .kpiNom(config.getKpi() != null ? config.getKpi().getNom() : null)
                .categorieCode(config.getCategorieCode())
                .build();
    }
}
