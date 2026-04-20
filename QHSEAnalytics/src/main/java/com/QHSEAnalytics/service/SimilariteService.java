package com.QHSEAnalytics.service;

import com.QHSEAnalytics.dto.response.ColonneDetecteeResponse;
import com.QHSEAnalytics.dto.response.KpiResponse;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimilariteService {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final double SEUIL_SUGGESTION_FORTE = 0.85d;
    private static final double SEUIL_SUGGESTION_MODEREE = 0.60d;

    private final KpiRepository kpiRepository;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public List<ColonneDetecteeResponse> detecterEtSuggerer(List<String> colonnes) {
        List<Kpi> kpis = kpiRepository.findAllByOrderByOrdreAsc();
        List<ColonneDetecteeResponse> result = new ArrayList<>();

        for (int i = 0; i < colonnes.size(); i++) {
            String colonne = colonnes.get(i);
            String normalizedCol = normaliser(colonne);

            Kpi best = null;
            double bestScore = -1.0d;

            for (Kpi kpi : kpis) {
                String normalizedKpi = normaliser(kpi.getNom());
                double score = similarity.apply(normalizedCol, normalizedKpi);
                if (score > bestScore) {
                    bestScore = score;
                    best = kpi;
                }
            }

            KpiResponse suggested = null;
            String noteSuggestion = null;
            if (best != null && bestScore >= SEUIL_SUGGESTION_MODEREE) {
                suggested = KpiResponse.builder()
                        .id(best.getId())
                        .nom(best.getNom())
                        .definition(best.getDefinition())
                        .unite(best.getUnite())
                        .categorieCode(best.getCategorieKpi().getCode())
                        .categorieLibelle(best.getCategorieKpi().getLibelle())
                        .seuilFaible(best.getSeuilFaible())
                        .seuilModere(best.getSeuilModere())
                        .seuilCritique(best.getSeuilCritique())
                        .ordre(best.getOrdre())
                        .isActive(best.isActive())
                        .createdAt(best.getCreatedAt())
                        .updatedAt(best.getUpdatedAt())
                        .build();
                if (bestScore < SEUIL_SUGGESTION_FORTE) {
                    noteSuggestion = "similarité modérée";
                }
            }

            result.add(ColonneDetecteeResponse.builder()
                    .nomColonne(colonne)
                    .indexColonne(i)
                    .kpiSuggere(suggested)
                    .scoreSimilarite(bestScore < 0 ? 0.0d : bestScore)
                    .noteSuggestion(noteSuggestion)
                    .build());
        }

        return result;
    }

    private String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String lowered = texte.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return NON_ASCII.matcher(normalized).replaceAll("").trim();
    }
}
