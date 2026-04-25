package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.entity.Kpi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KpiMatcher {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public Optional<KpiMatch> match(String rawLabel, List<Kpi> kpis) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return Optional.empty();
        }

        String normalizedLabel = normalize(rawLabel);
        if (normalizedLabel.isBlank()) {
            return Optional.empty();
        }

        return kpis.stream()
                .map(kpi -> new KpiMatch(kpi, similarity.apply(normalize(kpi.getNom()), normalizedLabel)))
                .max(Comparator.comparingDouble(KpiMatch::similarity))
                .filter(match -> match.similarity() >= 0.55);
    }

    private String normalize(String label) {
        if (label == null) {
            return "";
        }
        String cleaned = label.toLowerCase(Locale.ROOT).trim();
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD);
        cleaned = NON_ASCII.matcher(cleaned).replaceAll("");
        return cleaned.replaceAll("[\\s\u00A0]+", " ").trim();
    }

    public record KpiMatch(Kpi kpi, double similarity) {
    }
}
