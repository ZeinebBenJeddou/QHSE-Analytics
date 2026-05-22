package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.ColumnSemanticResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ColumnSemanticResolverTest {

    // ── KPI_NAME ──────────────────────────────────────────────────────────────

    @Test
    void indicator_anglais_singulier_est_kpi_name() {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve("Indicator");
        assertEquals(ColumnSemanticResolver.Semantic.KPI_NAME, result.semantic());
        assertTrue(result.confidence() >= 0.85,
                "confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Indicators", "Measure", "Measures", "Metric", "Metrics", "Item", "Items" })
    void synonymes_kpi_anglais_sont_kpi_name(String header) {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve(header);
        assertEquals(ColumnSemanticResolver.Semantic.KPI_NAME, result.semantic(),
                "Header '" + header + "' devrait être KPI_NAME");
        assertTrue(result.confidence() >= 0.85,
                "Header '" + header + "' : confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    // ── VALUE_N ───────────────────────────────────────────────────────────────

    @Test
    void current_year_est_value_n() {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve("Current Year");
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N, result.semantic());
        assertTrue(result.confidence() >= 0.85,
                "confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    @ParameterizedTest
    @ValueSource(strings = { "This Year", "Actual", "Current Value", "Value N" })
    void synonymes_value_n_anglais_sont_value_n(String header) {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve(header);
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N, result.semantic(),
                "Header '" + header + "' devrait être VALUE_N");
        assertTrue(result.confidence() >= 0.85,
                "Header '" + header + "' : confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    // ── VALUE_N1 ──────────────────────────────────────────────────────────────

    @Test
    void previous_year_est_value_n1() {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve("Previous Year");
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N1, result.semantic());
        assertTrue(result.confidence() >= 0.85,
                "confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Last Year", "Prior", "Prior Year", "Baseline", "Value N-1" })
    void synonymes_value_n1_anglais_sont_value_n1(String header) {
        ColumnSemanticResolver.Result result = ColumnSemanticResolver.resolve(header);
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N1, result.semantic(),
                "Header '" + header + "' devrait être VALUE_N1");
        assertTrue(result.confidence() >= 0.85,
                "Header '" + header + "' : confidence attendue >= 0.85, obtenue : " + result.confidence());
    }

    // ── Non-régression FR ─────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = { "Indicateur", "KPI", "Libellé", "Désignation" })
    void synonymes_kpi_francais_toujours_reconnus(String header) {
        assertEquals(ColumnSemanticResolver.Semantic.KPI_NAME,
                ColumnSemanticResolver.resolve(header).semantic(),
                "Header FR '" + header + "' ne devrait pas régresser");
    }

    @ParameterizedTest
    @ValueSource(strings = { "Réalisé N", "Valeur actuelle", "Performance" })
    void synonymes_value_n_francais_toujours_reconnus(String header) {
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N,
                ColumnSemanticResolver.resolve(header).semantic(),
                "Header FR '" + header + "' ne devrait pas régresser");
    }

    @ParameterizedTest
    @ValueSource(strings = { "N-1", "Réalisé N-1", "Année N-1", "Référence" })
    void synonymes_value_n1_francais_toujours_reconnus(String header) {
        assertEquals(ColumnSemanticResolver.Semantic.VALUE_N1,
                ColumnSemanticResolver.resolve(header).semantic(),
                "Header FR '" + header + "' ne devrait pas régresser");
    }

    // ── Headers opaques restent UNKNOWN ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = { "Score A", "Score B", "Colonne1", "Data" })
    void headers_opaques_restent_unknown(String header) {
        assertEquals(ColumnSemanticResolver.Semantic.UNKNOWN,
                ColumnSemanticResolver.resolve(header).semantic(),
                "Header opaque '" + header + "' devrait rester UNKNOWN");
    }

    // ── Colonnes cibles/objectif ne sont plus KPI_NAME ────────────────────────

    @Test
    void objectif_singulier_est_unknown_pas_kpi_name() {
        assertEquals(ColumnSemanticResolver.Semantic.UNKNOWN,
                ColumnSemanticResolver.resolve("Objectif").semantic(),
                "'Objectif' ne doit pas être classé KPI_NAME");
    }

    @Test
    void objectifs_pluriel_est_unknown_pas_kpi_name() {
        assertEquals(ColumnSemanticResolver.Semantic.UNKNOWN,
                ColumnSemanticResolver.resolve("Objectifs").semantic(),
                "'Objectifs' ne doit pas être classé KPI_NAME");
    }

    @Test
    void description_est_unknown_pas_kpi_name() {
        assertEquals(ColumnSemanticResolver.Semantic.UNKNOWN,
                ColumnSemanticResolver.resolve("Description").semantic(),
                "'Description' ne doit pas être classé KPI_NAME");
    }
}
