package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.enums.Tendance;
import com.QHSEAnalytics.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationAgent {

    private final KpiRepository kpiRepository;

    public List<KpiCalculatedDTO> calculate(List<KpiRawDataDTO> rawData) {
        List<Kpi> activeKpis = kpiRepository.findByIsActiveTrueOrderByOrdreAsc();
        Map<String, Kpi> byName = buildKpiLookup(activeKpis);

        return rawData.stream()
                .map(row -> mapCalculatedRow(row, byName))
                .toList();
    }

    private Map<String, Kpi> buildKpiLookup(List<Kpi> activeKpis) {
        Map<String, Kpi> lookup = new HashMap<>();
        for (Kpi kpi : activeKpis) {
            String normalized = normalize(kpi.getNom());
            lookup.putIfAbsent(normalized, kpi);
        }
        return lookup;
    }

    private KpiCalculatedDTO mapCalculatedRow(KpiRawDataDTO row, Map<String, Kpi> byName) {
        double variationAbsolute = 0d;
        double variationPercentage = 0d;
        String tendance = null;

        Kpi matchedKpi = findMatchingKpi(row, byName);
        double seuilFaible = 0d;
        double seuilModere = 0d;
        double seuilCritique = 0d;
        String categorieCode = "UNKNOWN";
        String definition = null;

        if (matchedKpi != null) {
            seuilFaible = Optional.ofNullable(matchedKpi.getSeuilFaible()).orElse(0d);
            seuilModere = Optional.ofNullable(matchedKpi.getSeuilModere()).orElse(0d);
            seuilCritique = Optional.ofNullable(matchedKpi.getSeuilCritique()).orElse(0d);
            categorieCode = Optional.ofNullable(matchedKpi.getCategorieKpi()).map(c -> c.getCode()).orElse("UNKNOWN");
            definition = matchedKpi.getDefinition();
        }

        if (row.isValid()) {
            variationAbsolute = calculateAbsolute(row.getValeurN1(), row.getValeurN());
            variationPercentage = calculatePercentage(row.getValeurN1(), row.getValeurN());
            tendance = computeTendance(variationPercentage).name();
        }

        return KpiCalculatedDTO.builder()
                .rowIndex(row.getRowIndex())
                .kpiName(row.getKpiName())
                .categorie(row.getCategorie())
                .categorieCode(categorieCode)
                .unite(row.getUnite())
                .valeurN1(row.getValeurN1())
                .valeurN(row.getValeurN())
                .seuilFaible(seuilFaible)
                .seuilModere(seuilModere)
                .seuilCritique(seuilCritique)
                .definition(definition)
                .variationAbsolute(variationAbsolute)
                .variationPercentage(variationPercentage)
                .classification("UNKNOWN")
                .tendance(tendance)
                .matchedKpi(Optional.ofNullable(matchedKpi).map(Kpi::getNom).orElse(null))
                .matchedKpiId(Optional.ofNullable(matchedKpi).map(Kpi::getId).orElse(null))
                .build();
    }

    private Kpi findMatchingKpi(KpiRawDataDTO row, Map<String, Kpi> byName) {
        if (row.getKpiName() == null || row.getKpiName().isBlank()) {
            return null;
        }
        String normalized = normalize(row.getKpiName());
        if (byName.containsKey(normalized)) {
            return byName.get(normalized);
        }
        return byName.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()) || entry.getKey().contains(normalized))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }


    private double calculateAbsolute(Double valeurN1, Double valeurN) {
        if (valeurN1 == null || valeurN == null) {
            return 0d;
        }
        return valeurN - valeurN1;
    }

    private double calculatePercentage(Double valeurN1, Double valeurN) {
        if (valeurN1 == null || valeurN == null) {
            return 0d;
        }
        if (valeurN1 == 0d) {
            return valeurN == 0d ? 0d : 100d;
        }
        return ((valeurN - valeurN1) / Math.abs(valeurN1)) * 100d;
    }

    private Tendance computeTendance(double variationPercentage) {
        if (variationPercentage > 1.0) {
            return Tendance.HAUSSE;
        }
        if (variationPercentage < -1.0) {
            return Tendance.BAISSE;
        }
        return Tendance.STABLE;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
        return normalized;
    }
}
