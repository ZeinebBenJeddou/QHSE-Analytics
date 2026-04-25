package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.entity.UniteKpi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NormalizationService {

    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9,\\.\\-+eE]");

    public NormalizationResult normalize(String rawValue, UniteKpi unit) {
        if (rawValue == null) {
            return new NormalizationResult(null, DataQuality.MISSING);
        }

        String cleaned = rawValue.trim();
        if (cleaned.isBlank()) {
            return new NormalizationResult(null, DataQuality.MISSING);
        }

        boolean hasPercent = cleaned.contains("%");
        cleaned = cleaned.replace("%", "").replace("\u00A0", " ").trim();

        String value = stripUnits(cleaned);
        if (value.isBlank()) {
            return new NormalizationResult(null, DataQuality.INVALID);
        }

        value = value.replace(" ", "");
        value = removeNonNumericCharacters(value);
        value = normalizeDecimalSeparators(value);

        Double parsed = parseDouble(value);
        if (parsed == null) {
            return new NormalizationResult(null, DataQuality.INVALID);
        }

        DataQuality quality = DataQuality.OK;
        if (hasPercent || shouldNormalizePercentage(parsed, unit)) {
            parsed = parsed > 1.0 ? parsed : parsed * 100.0;
            quality = DataQuality.CORRECTED;
        }

        if (isSuspicious(parsed, unit)) {
            quality = DataQuality.SUSPECT;
        }

        if (isInvalidValue(parsed, unit)) {
            return new NormalizationResult(null, DataQuality.INVALID);
        }

        parsed = roundValue(parsed, unit);
        return new NormalizationResult(parsed, quality);
    }

    private String stripUnits(String candidate) {
        return candidate.replaceAll("(?i)kwh|kg|unite|unit|pcs|pcs$", "").trim();
    }

    private String removeNonNumericCharacters(String candidate) {
        return NON_NUMERIC.matcher(candidate).replaceAll("");
    }

    private String normalizeDecimalSeparators(String candidate) {
        if (candidate.contains(",") && candidate.contains(".")) {
            int lastComma = candidate.lastIndexOf(',');
            int lastDot = candidate.lastIndexOf('.');
            if (lastComma > lastDot) {
                candidate = candidate.replace(".", "").replace(',', '.');
            } else {
                candidate = candidate.replace(",", "");
            }
        } else if (candidate.contains(",")) {
            candidate = candidate.replace(',', '.');
        }
        return candidate;
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            log.debug("Échec parsing valeur : {}", value);
            return null;
        }
    }

    private boolean shouldNormalizePercentage(Double parsed, UniteKpi unit) {
        return unit == UniteKpi.POURCENTAGE && parsed >= 0.0 && parsed <= 1.0;
    }

    private boolean isInvalidValue(Double parsed, UniteKpi unit) {
        if (parsed == null) {
            return true;
        }
        if (parsed.isNaN() || parsed.isInfinite()) {
            return true;
        }
        if (unit == UniteKpi.POURCENTAGE && parsed < 0.0) {
            return true;
        }
        return false;
    }

    private boolean isSuspicious(Double parsed, UniteKpi unit) {
        if (parsed == null) {
            return false;
        }
        if (unit == UniteKpi.POURCENTAGE) {
            return parsed > 100.0 || parsed < 0.0;
        }
        return false;
    }

    private Double roundValue(Double parsed, UniteKpi unit) {
        if (parsed == null) {
            return null;
        }
        if (unit == UniteKpi.NOMBRE) {
            return (double) Math.round(parsed);
        }
        return Math.round(parsed * 100.0) / 100.0;
    }

    public enum DataQuality {
        OK,
        CORRECTED,
        SUSPECT,
        INVALID,
        MISSING
    }

    public record NormalizationResult(Double value, DataQuality status) {
        public boolean valid() {
            return value != null && status != DataQuality.INVALID && status != DataQuality.MISSING;
        }
    }
}
