package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.UniteKpi;
import com.QHSEAnalytics.enums.Tendance;
import com.QHSEAnalytics.repository.KpiRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CalculationAgent {

    private final KpiRepository kpiRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final ComparativeCalculator comparativeCalculator;
    private final ClassificationEngine classificationEngine;

    public CalculationAgent(KpiRepository kpiRepository,
                            ResultatKpiRepository resultatKpiRepository,
                            ComparativeCalculator comparativeCalculator,
                            ClassificationEngine classificationEngine) {
        this.kpiRepository = kpiRepository;
        this.resultatKpiRepository = resultatKpiRepository;
        this.comparativeCalculator = comparativeCalculator;
        this.classificationEngine = classificationEngine;
    }

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
        Kpi matchedKpi = findMatchingKpi(row, byName);
        boolean isBoolean = (matchedKpi != null && matchedKpi.getUnite() == UniteKpi.BOOLEAN) ||
                            isBooleanValue(row.getValeurNRaw()) || 
                            isBooleanValue(row.getValeurN1Raw());
        
        Double valN = row.getValeurN();
        Double valN1 = row.getValeurN1();
        
        double absoluteGap = 0.0;
        Double variationPercentage = 0.0;
        String status = "Inconnu";
        String statusColor = "gray";
        
        ComparativeCalculator.ComparativeResult comp = null;
        ClassificationEngine.ClassificationResult classRes = null;

        if (row.isValid()) {
            absoluteGap = (valN != null && valN1 != null) ? (valN - valN1) : 0.0;
            
            if (isBoolean) {
                // Boolean Logic (0/1)
                if (valN1 == 0 && valN == 1) { status = "Acquis"; statusColor = "green"; }
                else if (valN1 == 1 && valN == 0) { status = "Perdu"; statusColor = "red"; }
                else if (valN1 == 1 && valN == 1) { status = "Maintenu"; statusColor = "green"; }
                else { status = "Non atteint"; statusColor = "yellow"; }
                variationPercentage = null; // No variation for boolean
            } else {
                // Numeric Logic: delegate enhanced comparatives to ComparativeCalculator
                comp = comparativeCalculator.compute(matchedKpi, valN1, valN);
                variationPercentage = comp.getRelativePercentage();
                if (comp.getSpecialCase() != null) {
                    status = comp.getSpecialCase();
                    statusColor = "yellow";
                } else if (variationPercentage == null) {
                    status = "Nouveau";
                    statusColor = "blue";
                } else {
                    if (variationPercentage > 0) { status = "Augmentation"; statusColor = "green"; }
                    else if (variationPercentage < 0) { status = "Diminution"; statusColor = "red"; }
                    else { status = "Stable"; statusColor = "yellow"; }
                }

                List<Double> historicalSeries = findHistoricalSeries(matchedKpi);
                classRes = classificationEngine.classify(matchedKpi, comp, historicalSeries);
            }
        } else {
            status = "Invalide";
            statusColor = "gray";
        }

        String definition = matchedKpi != null ? matchedKpi.getDefinition() : null;
        String categorie = matchedKpi != null && matchedKpi.getCategorieKpi() != null ? matchedKpi.getCategorieKpi().getLibelle() : row.getCategorie();
        String categorieCode = matchedKpi != null && matchedKpi.getCategorieKpi() != null ? matchedKpi.getCategorieKpi().getCode() : "AUTO";

        KpiCalculatedDTO.KpiCalculatedDTOBuilder builder = KpiCalculatedDTO.builder()
                .rowIndex(row.getRowIndex())
                .kpiName(row.getKpiName())
                .categorie(categorie)
                .categorieCode(categorieCode)
                .unite(row.getUnite())
                .valeurN1(valN1)
                .valeurN(valN)
                .definition(definition)
                .variationAbsolute(absoluteGap) // mapping variationAbsolute to absoluteGap for compatibility or update field
                .absoluteGap(absoluteGap)
                .variationPercentage(variationPercentage)
                .status(status)
                .statusColor(statusColor)
                .commentaire(row.getValidationMessage() != null ? row.getValidationMessage() : (row.isValid() ? "Données validées" : "Données non validées"))
                .isBoolean(isBoolean)
            .matchedKpi(Optional.ofNullable(matchedKpi).map(Kpi::getNom).orElse(null))
            .matchedKpiId(Optional.ofNullable(matchedKpi).map(Kpi::getId).orElse(null));

        String tendance = isBoolean
                ? getBooleanTendance(valN1, valN)
                : (variationPercentage != null ? computeTendance(variationPercentage).name() : "STABLE");

        // if numeric and valid, enrich DTO with comparative and classification info
        if (row.isValid() && !isBoolean && comp != null) {
            builder.direction(comp.getDirection().name())
                .calcConfidence(comp.getCalcConfidence())
                .classification(classRes != null ? classRes.getClassification() : "INDETERMINE")
                .classificationReason(classRes != null ? classRes.getReason() : "Classification indisponible")
                .reviewRequired(classRes != null ? classRes.isReviewRequired() : true)
                .dataFlags(comp.getDataFlags() == null ? null : comp.getDataFlags().stream().map(Enum::name).collect(Collectors.toList()))
                .tendance(tendance);
        } else {
            builder.tendance(tendance);
        }

        return builder.build();
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

    private boolean isBooleanValue(String value) {
        if (value == null || value.isBlank()) return false;
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.equals("oui") || lower.equals("non") || 
               lower.equals("true") || lower.equals("false") || 
               lower.equals("vrai") || lower.equals("faux") ||
               lower.equals("yes") || lower.equals("no") ||
               lower.equals("acquis") || lower.equals("perdu");
    }

    private String getBooleanTendance(Double valN1, Double valN) {
        if (valN1 == null || valN == null) return "STABLE";
        if (valN1 == 0 && valN == 1) return Tendance.HAUSSE.name(); // Amélioration
        if (valN1 == 1 && valN == 0) return Tendance.BAISSE.name(); // Dégradation
        return Tendance.STABLE.name();
    }

    private List<Double> findHistoricalSeries(Kpi kpi) {
        if (kpi == null || kpi.getId() == null) {
            return List.of();
        }
        try {
            return resultatKpiRepository.findVariationHistoryByKpiId(kpi.getId())
                    .stream()
                    .filter(v -> v != null && !v.isNaN() && !v.isInfinite())
                    .limit(60)
                    .toList();
        } catch (Exception ex) {
            log.debug("Historique indisponible pour KPI {}: {}", kpi.getId(), ex.getMessage());
            return List.of();
        }
    }
}
