package com.QHSEAnalytics.shared.service;

import com.QHSEAnalytics.shared.enums.Direction;

import java.util.Optional;


public interface KpiKnowledgeLookup {

    Optional<KnowledgeMatch> findBestMatch(String kpiName, String categoryCode, Double currentValue, Double previousValue);

    static KpiKnowledgeLookup noop() {
        return (kpiName, categoryCode, currentValue, previousValue) -> Optional.empty();
    }

    record KnowledgeMatch(
            String matchedKpiName,
            String categoryCode,
            String definition,
            Double seuilFaible,
            Double seuilModere,
            Double seuilCritique,
            Direction direction,
            Double similarity
    ) {
        public boolean hasThresholds() {
            return seuilFaible != null && seuilModere != null && seuilCritique != null;
        }
    }
}
