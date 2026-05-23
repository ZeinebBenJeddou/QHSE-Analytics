package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.CategoryScoreDTO;
import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.shared.entity.Kpi;
import com.QHSEAnalytics.shared.enums.UniteKpi;
import com.QHSEAnalytics.shared.enums.Direction;
import com.QHSEAnalytics.shared.enums.Tendance;
import com.QHSEAnalytics.shared.repository.KpiRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.Collections;
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


        List<Long> kpiIds = activeKpis.stream()
                .map(Kpi::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<Double>> historyCache;
        if (!kpiIds.isEmpty()) {
            historyCache = resultatKpiRepository.findVariationHistoryByKpiIds(kpiIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            ResultatKpiRepository.VariationHistoryProjection::getKpiId,
                            Collectors.mapping(
                                    ResultatKpiRepository.VariationHistoryProjection::getVariation,
                                    Collectors.toList()
                            )
                    ));
        } else {
            historyCache = Collections.emptyMap();
        }

        return rawData.stream()
                .map(row -> mapCalculatedRow(row, byName, historyCache))
                .toList();
    }


    public List<CategoryScoreDTO> computeCategoryScores(List<KpiCalculatedDTO> calculated) {
        Map<String, List<KpiCalculatedDTO>> byCode = calculated.stream()
                .filter(k -> k.getCategorieCode() != null)
                .collect(Collectors.groupingBy(KpiCalculatedDTO::getCategorieCode));

        return byCode.entrySet().stream().map(entry -> {
            String code = entry.getKey();
            List<KpiCalculatedDTO> items = entry.getValue();
            String libelle = items.stream()
                    .map(KpiCalculatedDTO::getCategorie)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(code);

            int excellent = count(items, "EXCELLENT");
            int faible = count(items, "FAIBLE");
            int indetermine = count(items, "INDETERMINE");
            int modere = count(items, "MODERE");
            int preEscalade = count(items, "PRE_ESCALADE");
            int critique = count(items, "CRITIQUE");
            int total = items.size();


            double score = total == 0 ? 0 :
                (excellent * 100.0 + faible * 75.0 + indetermine * 45.0
                + modere * 30.0 + preEscalade * 15.0 + critique * 0.0) / total;

            String label;
            if (score >= 90) label = "Excellent";
            else if (score >= 75) label = "Bon";
            else if (score >= 55) label = "Acceptable";
            else if (score >= 30) label = "À surveiller";
            else label = "Critique";

            return CategoryScoreDTO.builder()
                    .categoryCode(code)
                    .categoryLibelle(libelle)
                    .kpiCount(total)
                    .excellentCount(excellent)
                    .faibleCount(faible)
                    .indetermineCount(indetermine)
                    .modereCount(modere)
                    .preEscaladeCount(preEscalade)
                    .critiqueCount(critique)
                    .compositeScore(Math.round(score * 10.0) / 10.0)
                    .compositeLabel(label)
                    .build();
        }).sorted(Comparator.comparingDouble(CategoryScoreDTO::getCompositeScore)).toList();
    }



    private Map<String, Kpi> buildKpiLookup(List<Kpi> activeKpis) {
        Map<String, Kpi> lookup = new HashMap<>();
        for (Kpi kpi : activeKpis) {
            String normalized = normalize(kpi.getNom());
            lookup.putIfAbsent(normalized, kpi);
        }
        return lookup;
    }

    private KpiCalculatedDTO mapCalculatedRow(KpiRawDataDTO row, Map<String, Kpi> byName, Map<Long, List<Double>> historyCache) {
        MatchResult matchResult = findMatchingKpi(row, byName);
        Kpi matchedKpi = matchResult.kpi();
        Double matchConf = matchResult.confidence();

        boolean isBoolean = (matchedKpi != null && matchedKpi.getUnite() == UniteKpi.BOOLEAN) ||
                            isBooleanValue(row.getValeurNRaw()) ||
                            isBooleanValue(row.getValeurN1Raw());

        Double valN  = row.getValeurN();
        Double valN1 = row.getValeurN1();
        Kpi effectiveKpi = matchedKpi;

        Double absoluteGap = null;
        Double variationPercentage = null;
        String status = "Inconnu";
        String statusColor = "gray";

        ComparativeCalculator.ComparativeResult comp = null;
        ClassificationEngine.ClassificationResult classRes = null;
        List<Double> historicalSeries = List.of();

        if (row.isValid()) {
            absoluteGap = (valN != null && valN1 != null) ? (valN - valN1) : null;

            if (isBoolean) {
                if      (valN1 == 0 && valN == 1) { status = "Acquis";      statusColor = "green"; }
                else if (valN1 == 1 && valN == 0) { status = "Perdu";       statusColor = "red"; }
                else if (valN1 == 1 && valN == 1) { status = "Maintenu";    statusColor = "green"; }
                else                              { status = "Non atteint"; statusColor = "yellow"; }
                variationPercentage = null;
            } else {
                comp = comparativeCalculator.compute(effectiveKpi, valN1, valN);
                variationPercentage = comp.getRelativePercentage();
                if (comp.getSpecialCase() != null) {
                    status = translateSpecialCase(comp.getSpecialCase());
                    statusColor = "STRONG_IMPROVEMENT".equals(comp.getSpecialCase()) ? "green" : "yellow";
                } else if (variationPercentage == null) {
                    status = "Nouveau"; statusColor = "blue";
                } else {
                    Direction dir = comp.getDirection() != null ? comp.getDirection() : Direction.HIGHER_IS_BETTER;
                    if (variationPercentage > 0) {
                        status = "Augmentation";
                        statusColor = dir == Direction.LOWER_IS_BETTER ? "red" : "green";
                    } else if (variationPercentage < 0) {
                        status = "Diminution";
                        statusColor = dir == Direction.LOWER_IS_BETTER ? "green" : "red";
                    } else {
                        status = "Stable"; statusColor = "yellow";
                    }
                }
                historicalSeries = findHistoricalSeries(effectiveKpi, historyCache);
                classRes = classificationEngine.classify(effectiveKpi, comp, historicalSeries);
            }
        } else {
            status = "Invalide"; statusColor = "gray";
        }

        String definition    = effectiveKpi != null ? effectiveKpi.getDefinition() : null;
        String categorie     = effectiveKpi != null && effectiveKpi.getCategorieKpi() != null
                               ? effectiveKpi.getCategorieKpi().getLibelle() : row.getCategorie();
        String categorieCode = effectiveKpi != null && effectiveKpi.getCategorieKpi() != null
                               ? effectiveKpi.getCategorieKpi().getCode() : "AUTO";

        String classification = classRes != null ? classRes.getClassification() : "INDETERMINE";
        String tendance = isBoolean
                ? getBooleanTendance(valN1, valN)
                : (variationPercentage != null ? computeTendance(variationPercentage).name() : "NA");


        Double spcMean = null, spcStd = null, spcUcl = null, spcLcl = null;
        Boolean spcOutOfControl = null;
        if (!isBoolean && historicalSeries.size() >= 5) {
            spcMean = mean(historicalSeries);
            spcStd  = stdDev(historicalSeries, spcMean);
            spcUcl  = spcMean + 3 * spcStd;
            spcLcl  = spcMean - 3 * spcStd;
            spcOutOfControl = valN != null && (valN > spcUcl || valN < spcLcl);
        }


        int[] risk = computeRisk(classification, tendance, categorieCode, comp != null ? comp.getDirection() : null);

        KpiCalculatedDTO.KpiCalculatedDTOBuilder builder = KpiCalculatedDTO.builder()
                .rowIndex(row.getRowIndex())
                .kpiName(row.getKpiName())
                .categorie(categorie)
                .categorieCode(categorieCode)
                .unite(row.getUnite())
                .valeurN1(valN1)
                .valeurN(valN)
                .seuilFaible(effectiveKpi != null ? effectiveKpi.getSeuilFaible() : null)
                .seuilModere(effectiveKpi != null ? effectiveKpi.getSeuilModere() : null)
                .seuilCritique(effectiveKpi != null ? effectiveKpi.getSeuilCritique() : null)
                .definition(definition)
                .variationAbsolute(absoluteGap)
                .absoluteGap(absoluteGap)
                .variationPercentage(variationPercentage)
                .status(status)
                .statusColor(statusColor)
                .commentaire(row.getValidationMessage() != null
                        ? row.getValidationMessage()
                        : (row.isValid() ? "Données validées" : "Données non validées"))
                .isBoolean(isBoolean)
                .matchedKpi(Optional.ofNullable(matchedKpi).map(Kpi::getNom).orElse(null))
                .matchedKpiId(Optional.ofNullable(matchedKpi).map(Kpi::getId).orElse(null))
                .matchConfidence(matchConf)
                .spcMean(spcMean).spcStd(spcStd).spcUcl(spcUcl).spcLcl(spcLcl)
                .spcOutOfControl(spcOutOfControl)
                .riskProbability(risk[0]).riskImpact(risk[1])
                .riskScore(risk[0] * risk[1])
                .riskLevel(riskLevel(risk[0] * risk[1]));

        if (row.isValid() && !isBoolean && comp != null) {
            builder.direction(comp.getDirection().name())
                    .calcConfidence(comp.getCalcConfidence())
                    .classification(classification)
                    .classificationReason(classRes != null ? classRes.getReason() : "Classification indisponible")
                    .reviewRequired(classRes != null ? classRes.isReviewRequired() : true)
                    .dataFlags(comp.getDataFlags() == null ? null
                            : comp.getDataFlags().stream().map(Enum::name).collect(Collectors.toList()))
                    .tendance(tendance);
        } else {
            builder.tendance(tendance);
        }

        return builder.build();
    }



    private record MatchResult(Kpi kpi, Double confidence) {}

    private MatchResult findMatchingKpi(KpiRawDataDTO row, Map<String, Kpi> byName) {
        if (row.getKpiName() == null || row.getKpiName().isBlank()) return new MatchResult(null, null);
        String normalized = normalize(row.getKpiName());

        if (byName.containsKey(normalized)) return new MatchResult(byName.get(normalized), 1.0);

        for (Map.Entry<String, Kpi> entry : byName.entrySet()) {
            String key = entry.getKey();
            if (normalized.contains(key) || key.contains(normalized)) {
                return new MatchResult(entry.getValue(), 0.9);
            }
        }

        Kpi best = null;
        double bestScore = 0.0;
        for (Map.Entry<String, Kpi> entry : byName.entrySet()) {
            double score = jaroWinkler(normalized, entry.getKey());
            if (score > bestScore) { bestScore = score; best = entry.getValue(); }
        }
        if (bestScore >= 0.82) return new MatchResult(best, bestScore);
        return new MatchResult(null, null);
    }

    private double jaroWinkler(String s1, String s2) {
        double j = jaro(s1, s2);
        int prefix = 0;
        int limit = Math.min(Math.min(s1.length(), s2.length()), 4);
        for (int i = 0; i < limit; i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++; else break;
        }
        return j + prefix * 0.1 * (1 - j);
    }

    private double jaro(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        int window = Math.max(0, Math.max(s1.length(), s2.length()) / 2 - 1);
        boolean[] m1 = new boolean[s1.length()];
        boolean[] m2 = new boolean[s2.length()];
        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int lo = Math.max(0, i - window);
            int hi = Math.min(i + window + 1, s2.length());
            for (int j = lo; j < hi; j++) {
                if (m2[j] || s1.charAt(i) != s2.charAt(j)) continue;
                m1[i] = true; m2[j] = true; matches++; break;
            }
        }
        if (matches == 0) return 0.0;
        int t = 0, k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!m1[i]) continue;
            while (!m2[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) t++;
            k++;
        }
        return (matches / (double) s1.length()
                + matches / (double) s2.length()
                + (matches - t / 2.0) / matches) / 3.0;
    }



    private double mean(List<Double> data) {
        return data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double stdDev(List<Double> data, double avg) {
        if (data.size() < 2) return 0.0;
        double variance = data.stream()
                .mapToDouble(v -> (v - avg) * (v - avg))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }




    private int[] computeRisk(String classification, String tendance, String categoryCode, Direction direction) {
        int probability = switch (classification) {
            case "CRITIQUE"     -> 5;
            case "PRE_ESCALADE" -> 4;
            case "MODERE"       -> 3;
            case "FAIBLE"       -> 2;
            case "EXCELLENT"    -> 1;
            default             -> 2;
        };
        if (isWorseningTrend(tendance, direction)) probability = Math.min(5, probability + 1);

        int impact = switch (categoryCode != null ? categoryCode : "") {
            case "S"  -> 5;
            case "E"  -> 4;
            case "Q"  -> 3;
            case "H"  -> 3;
            default   -> 2;
        };

        if ("CRITIQUE".equals(classification))     impact = Math.min(5, impact + 1);
        if ("PRE_ESCALADE".equals(classification)) impact = Math.min(5, impact + 1);

        return new int[]{probability, impact};
    }

    private boolean isWorseningTrend(String tendance, Direction direction) {
        if (tendance == null || "STABLE".equals(tendance) || direction == null) {
            return false;
        }
        return switch (direction) {
            case HIGHER_IS_BETTER -> "BAISSE".equals(tendance);
            case LOWER_IS_BETTER -> "HAUSSE".equals(tendance);
            case TARGET_IS_BEST -> false;
        };
    }

    private String translateSpecialCase(String specialCase) {
        if (specialCase == null) return "Inconnu";
        return switch (specialCase) {
            case "EMERGING_RISK"      -> "Risque émergent";
            case "STRONG_IMPROVEMENT" -> "Amélioration forte";
            case "STABLE"             -> "Stable";
            default                   -> specialCase;
        };
    }

    private String riskLevel(int score) {
        if (score >= 20) return "Critique";
        if (score >= 12) return "Élevé";
        if (score >= 6)  return "Modéré";
        return "Faible";
    }



    private int count(List<KpiCalculatedDTO> items, String level) {
        return (int) items.stream()
                .filter(k -> level.equals(k.getClassification()))
                .count();
    }

    private Tendance computeTendance(double variationPercentage) {
        if (variationPercentage > 1.0)  return Tendance.HAUSSE;
        if (variationPercentage < -1.0) return Tendance.BAISSE;
        return Tendance.STABLE;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
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
        if (valN1 == 0 && valN == 1) return Tendance.HAUSSE.name();
        if (valN1 == 1 && valN == 0) return Tendance.BAISSE.name();
        return Tendance.STABLE.name();
    }

    private List<Double> findHistoricalSeries(Kpi kpi, Map<Long, List<Double>> historyCache) {
        if (kpi == null || kpi.getId() == null) return List.of();
        return historyCache.getOrDefault(kpi.getId(), List.of())
                .stream()
                .filter(v -> v != null && !v.isNaN() && !v.isInfinite())
                .limit(60)
                .toList();
    }

}
