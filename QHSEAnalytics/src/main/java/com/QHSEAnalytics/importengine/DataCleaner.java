package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.enums.QualityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataCleaner {

    private final NormalizationService normalizationService;

    public CleanedRow clean(ExcelStructureDetector.RawKpiRow rawRow, Kpi kpi) {
        NormalizationService.NormalizationResult normalizedN1 = normalizationService.normalize(rawRow.valeurBruteN1(), kpi.getUnite());
        NormalizationService.NormalizationResult normalizedN = normalizationService.normalize(rawRow.valeurBruteN(), kpi.getUnite());

        double scoreN1 = mapStatusToScore(normalizedN1.status());
        double scoreN = mapStatusToScore(normalizedN.status());
        double validityScore = Math.min(scoreN1, scoreN);
        boolean valid = normalizedN1.valid() && normalizedN.valid();

        return new CleanedRow(rawRow.rowIndex(), rawRow.label(), normalizedN1.value(), normalizedN.value(), validityScore, normalizedN1.status(), normalizedN.status(), valid);
    }

    private double mapStatusToScore(NormalizationService.DataQuality status) {
        return switch (status) {
            case OK -> 1.0;
            case CORRECTED -> 0.9;
            case SUSPECT -> 0.7;
            case MISSING, INVALID -> 0.0;
        };
    }

    public record CleanedRow(
            int rowIndex,
            String label,
            Double valeurN1,
            Double valeurN,
            double validityScore,
            NormalizationService.DataQuality statusN1,
            NormalizationService.DataQuality statusN,
            boolean valid
    ) {
        public boolean isValid() {
            return valid && valeurN1 != null && valeurN != null;
        }
    }
}
