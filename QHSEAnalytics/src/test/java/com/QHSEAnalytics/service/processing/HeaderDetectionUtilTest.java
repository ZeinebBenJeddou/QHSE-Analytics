package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.HeaderDetectionUtil;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HeaderDetectionUtilTest {

    private Workbook buildWorkbook(String... headers) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Data");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        // ligne de données pour que la feuille ne soit pas vide
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("KPI test");
        data.createCell(1).setCellValue(42.0);
        data.createCell(2).setCellValue(38.0);
        return wb;
    }

    private DataFormatter formatter() {
        return new DataFormatter(java.util.Locale.ROOT);
    }

    // Test 1 : "KPI | 2024 | 2025" → 2025 = VALUE_N, 2024 = VALUE_N1
    @Test
    void anneesCroissantes_2024_2025_retourneN1puisN() throws Exception {
        try (Workbook wb = buildWorkbook("KPI", "2024", "2025")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();

            assertEquals(1, r.getValeurN1ColumnIndex(), "2024 doit être VALUE_N1 (colonne 1)");
            assertEquals(2, r.getValeurNColumnIndex(),  "2025 doit être VALUE_N  (colonne 2)");
        }
    }

    // Test 2 : "KPI | 2025 | 2026" → 2026 = VALUE_N, 2025 = VALUE_N1
    @Test
    void anneesCroissantes_2025_2026_retourneN1puisN() throws Exception {
        try (Workbook wb = buildWorkbook("KPI", "2025", "2026")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();

            assertEquals(1, r.getValeurN1ColumnIndex(), "2025 doit être VALUE_N1 (colonne 1)");
            assertEquals(2, r.getValeurNColumnIndex(),  "2026 doit être VALUE_N  (colonne 2)");
        }
    }

    // Test 3 : "KPI | Réalisé N | Réalisé N-1" → détection par mots-clés, pas d'exception
    @Test
    void headersTextuels_sansAnnee_detectionParMotsCles() throws Exception {
        try (Workbook wb = buildWorkbook("Indicateur", "Réalisé N", "Réalisé N-1")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            assertDoesNotThrow(() -> HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval));

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté via mots-clés");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(0, r.getKpiColumnIndex(), "Colonne KPI doit être à l'index 0");
        }
    }

    // Test 4 : "Indicateur | Réalisé 2026 | Réalisé 2025"
    // "Réalisé" connu de ColumnSemanticResolver mais absent des anciennes listes HDU
    // → 2026 doit être VALUE_N, 2025 doit être VALUE_N1
    @Test
    void realise_avecAnnee_classifieCorrectement() throws Exception {
        try (Workbook wb = buildWorkbook("Indicateur", "Réalisé 2026", "Réalisé 2025")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(0, r.getKpiColumnIndex(),    "Indicateur doit être KPI (colonne 0)");
            assertEquals(1, r.getValeurNColumnIndex(), "Réalisé 2026 doit être VALUE_N (colonne 1)");
            assertEquals(2, r.getValeurN1ColumnIndex(),"Réalisé 2025 doit être VALUE_N1 (colonne 2)");
        }
    }

    // Test 5 : "Indicateur | Performance N-1 | Performance N"
    // "performance" et "precedent" connus de ColumnSemanticResolver mais absents des anciennes listes HDU
    // → VALUE_N1 doit être détecté malgré l'absence d'année explicite
    @Test
    void performance_N1_classifieCorrectement() throws Exception {
        try (Workbook wb = buildWorkbook("Indicateur", "Performance N-1", "Performance N")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(0, r.getKpiColumnIndex(),    "Indicateur doit être KPI (colonne 0)");
            assertEquals(2, r.getValeurNColumnIndex(), "Performance N doit être VALUE_N (colonne 2)");
            assertEquals(1, r.getValeurN1ColumnIndex(),"Performance N-1 doit être VALUE_N1 (colonne 1)");
        }
    }

    // --- Nouveaux tests étape 2 ---

    // Test 6 : ordre naturel — "Indicateur | Valeur actuelle | Valeur précédente"
    // Mots-clés sémantiques connus → detectHeaderRow doit réussir sans fallback positionnel
    // → col 1 = VALUE_N, col 2 = VALUE_N1
    @Test
    void valeurActuelle_avant_valeurPrecedente_classifieCorrectement() throws Exception {
        try (Workbook wb = buildWorkbook("Indicateur", "Valeur actuelle", "Valeur précédente")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté via mots-clés");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(0, r.getKpiColumnIndex(),     "Indicateur doit être KPI");
            assertEquals(1, r.getValeurNColumnIndex(),  "Valeur actuelle doit être VALUE_N");
            assertEquals(2, r.getValeurN1ColumnIndex(), "Valeur précédente doit être VALUE_N1");
        }
    }

    // Test 7 : ordre inversé — "Indicateur | Valeur précédente | Valeur actuelle"
    // Même résultat que le test 6 — l'ordre physique ne doit pas changer l'assignation sémantique
    @Test
    void valeurPrecedente_avant_valeurActuelle_classifieCorrectement() throws Exception {
        try (Workbook wb = buildWorkbook("Indicateur", "Valeur précédente", "Valeur actuelle")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté via mots-clés");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(0, r.getKpiColumnIndex(),     "Indicateur doit être KPI");
            assertEquals(2, r.getValeurNColumnIndex(),  "Valeur actuelle doit être VALUE_N (col 2)");
            assertEquals(1, r.getValeurN1ColumnIndex(), "Valeur précédente doit être VALUE_N1 (col 1)");
        }
    }

    // Test 8 : headers totalement opaques — "KPI | Colonne A | Colonne B"
    // Fallback positionnel activé, aucun mot-clé reconnu
    // → buildFallbackHeader utilisé → isPositionalFallback() doit être true
    @Test
    void headersOpaques_fallbackPositionnel_flagPositionnel() throws Exception {
        try (Workbook wb = buildWorkbook("KPI", "Colonne A", "Colonne B")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            // detectHeaderRow échoue (aucun mot-clé VALUE_N reconnu pour "Colonne A"/"Colonne B")
            // → buildFallbackHeader prend le relais
            Optional<HeaderDetectionUtil.HeaderDetectionResult> primary =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);
            Optional<HeaderDetectionUtil.HeaderDetectionResult> fallback =
                    HeaderDetectionUtil.buildFallbackHeader(sheet, fmt, eval);

            assertTrue(fallback.isPresent(), "buildFallbackHeader doit retourner un résultat");
            HeaderDetectionUtil.HeaderDetectionResult r = fallback.get();
            assertTrue(r.isPositionalFallback(), "isPositionalFallback() doit être true pour des headers opaques");
            // assignation positionnelle : première valeur → N-1, deuxième → N
            assertEquals(1, r.getValeurN1ColumnIndex(), "Colonne A (col 1) doit être VALUE_N1 (premier positionnel)");
            assertEquals(2, r.getValeurNColumnIndex(),  "Colonne B (col 2) doit être VALUE_N (deuxième positionnel)");
        }
    }

    // Test 9 : régression années — "KPI | 2026 | 2025"
    // Les corrections précédentes ne doivent pas être cassées
    @Test
    void regression_annees_2026_2025_nonCassees() throws Exception {
        try (Workbook wb = buildWorkbook("KPI", "2026", "2025")) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = formatter();
            var eval = wb.getCreationHelper().createFormulaEvaluator();

            Optional<HeaderDetectionUtil.HeaderDetectionResult> result =
                    HeaderDetectionUtil.detectHeaderRow(sheet, fmt, eval);

            assertTrue(result.isPresent(), "L'en-tête doit être détecté");
            HeaderDetectionUtil.HeaderDetectionResult r = result.get();
            assertEquals(1, r.getValeurNColumnIndex(),  "2026 doit être VALUE_N (col 1)");
            assertEquals(2, r.getValeurN1ColumnIndex(), "2025 doit être VALUE_N1 (col 2)");
        }
    }
}
