package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.ColumnProfileService;
import com.QHSEAnalytics.importer.service.processing.ExcelFileValidator;
import com.QHSEAnalytics.shared.dto.response.ColumnProfileDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnProfileServiceTest {

    private final ColumnProfileService service =
            new ColumnProfileService(new ExcelFileValidator());

    private MockMultipartFile buildXlsx(String[] headers, double[][] rows) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet("Data");
        var headerRow = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            headerRow.createCell(c).setCellValue(headers[c]);
        }
        for (int r = 0; r < rows.length; r++) {
            var dataRow = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                dataRow.createCell(c).setCellValue(rows[r][c]);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());
    }

    // ── Tests limite colonnes ──────────────────────────────────────────────────

    // Test colonne-limite 1 : 10 colonnes → 10 profils, aucun warningMessage
    @Test
    void dixColonnes_retourne_dixProfils_sansWarning() throws Exception {
        String[] headers = new String[10];
        double[][] rows = new double[3][10];
        for (int i = 0; i < 10; i++) {
            headers[i] = "Col" + i;
            for (int r = 0; r < 3; r++) rows[r][i] = i * 1.0 + r;
        }
        List<ColumnProfileDTO> profiles = service.profile(buildXlsx(headers, rows));

        assertEquals(10, profiles.size(), "10 colonnes → 10 profils attendus");
        assertTrue(profiles.stream().allMatch(p -> p.getWarningMessage() == null),
                "Aucun warningMessage attendu pour 10 colonnes");
    }

    // Test colonne-limite 2 : 60 colonnes → exactement 50 profils + warningMessage sur le premier
    @Test
    void soixanteColonnes_retourne_cinquanteProfils_avecWarning() throws Exception {
        int total = 60;
        String[] headers = new String[total];
        double[][] rows = new double[2][total];
        for (int i = 0; i < total; i++) {
            headers[i] = "Col" + i;
            for (int r = 0; r < 2; r++) rows[r][i] = i * 1.0;
        }
        List<ColumnProfileDTO> profiles = service.profile(buildXlsx(headers, rows));

        assertEquals(50, profiles.size(),
                "60 colonnes → 50 profils attendus (MAX_COLUMNS_TO_PROFILE)");

        String warning = profiles.get(0).getWarningMessage();
        assertNotNull(warning, "warningMessage doit être présent sur le premier profil");
        assertTrue(warning.contains("60"),
                "Le warningMessage doit mentionner le nombre total (60), obtenu : " + warning);
    }

    // ── Tests headers dupliqués ───────────────────────────────────────────────

    // Test doublon 1 : "KPI | Réalisé 2026 | Réalisé 2026" → warningMessage contient "réalisé 2026"
    @Test
    void headersDupliques_emitWarningMessage() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"KPI", "Réalisé 2026", "Réalisé 2026"},
                new double[][]{{3.2, 2.8}, {0.45, 0.38}, {12.0, 9.0}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        String warning = profiles.stream()
                .map(ColumnProfileDTO::getWarningMessage)
                .filter(w -> w != null)
                .findFirst()
                .orElse(null);

        assertNotNull(warning, "Un warningMessage doit être présent pour des headers dupliqués");
        assertTrue(warning.toLowerCase().contains("realise 2026") || warning.contains("réalisé 2026"),
                "Le warningMessage doit mentionner le header dupliqué, obtenu : " + warning);
    }

    // Test doublon 2 : "KPI | Réalisé 2025 | Réalisé 2026" → pas de doublon, pas de warningMessage
    @Test
    void headersUniques_pasDeWarning() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"KPI", "Réalisé 2025", "Réalisé 2026"},
                new double[][]{{3.2, 2.8}, {0.45, 0.38}, {12.0, 9.0}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        boolean hasDuplicateWarning = profiles.stream()
                .map(ColumnProfileDTO::getWarningMessage)
                .filter(w -> w != null)
                .anyMatch(w -> w.contains("dupliqués") || w.contains("dupliqu"));

        assertFalse(hasDuplicateWarning,
                "Aucun warningMessage de doublon attendu pour des headers uniques");
    }

    // Test doublon 3 : "KPI | N | N | Objectif" → warningMessage contient "n"
    @Test
    void headerNDuplique_emitWarningMessage() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"KPI", "N", "N", "Objectif"},
                new double[][]{{3.2, 2.8, 0.0}, {0.45, 0.38, 0.0}, {12.0, 9.0, 0.0}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        String warning = profiles.stream()
                .map(ColumnProfileDTO::getWarningMessage)
                .filter(w -> w != null)
                .findFirst()
                .orElse(null);

        assertNotNull(warning, "Un warningMessage doit être présent pour le header 'N' dupliqué");
        assertTrue(warning.contains(" n") || warning.contains("\"n\"") || warning.toLowerCase().contains(": n"),
                "Le warningMessage doit mentionner 'n', obtenu : " + warning);
    }

    // ── Tests confidence score ────────────────────────────────────────────────

    // Test 1 : confidenceScore élevé (0.95) préservé après setSemanticOnProfile
    // "Indicateur | Réalisé 2024 | Réalisé 2025" — "Réalisé 2025" résout VALUE_N
    // avec un score initial de 0.85 (via ColumnSemanticResolver), la résolution
    // par année l'écrase en VALUE_N mais doit conserver le score ≥ 0.85.
    @Test
    void confidenceScore_eleve_preserve_apres_resolution_annee() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"Indicateur", "Réalisé 2024", "Réalisé 2025"},
                new double[][]{{3.2, 2.8}, {0.45, 0.38}, {12, 9}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        ColumnProfileDTO valueN = profiles.stream()
                .filter(p -> "VALUE_N".equals(p.getLikelySemantic()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucune colonne VALUE_N trouvée"));

        assertTrue(valueN.getConfidenceScore() >= 0.85,
                "Le confidenceScore de VALUE_N doit être >= 0.85, obtenu : " + valueN.getConfidenceScore());
    }

    // Test 2 : colonne résolue par fallback NUMERIC avec score initial 0.0
    // → setSemanticOnProfile doit assigner 0.50 (badge "Douteux")
    // "Donnée A | Donnée B | Donnée C" — headers opaques, résolution NUMERIC
    @Test
    void confidenceScore_zero_remplace_par_0_50_apres_fallback_numeric() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"Donnée A", "Donnée B", "Donnée C"},
                new double[][]{{3.2, 2.8}, {0.45, 0.38}, {12, 9}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        // La colonne TEXT (Donnée A) reste UNKNOWN — seules les colonnes NUMERIC
        // résolues par fallback (VALUE_N1 et VALUE_N) doivent avoir score >= 0.50.
        long resolvedCount = profiles.stream()
                .filter(p -> "VALUE_N".equals(p.getLikelySemantic())
                          || "VALUE_N1".equals(p.getLikelySemantic()))
                .peek(p ->
                    assertTrue(p.getConfidenceScore() >= 0.50,
                            "La colonne '" + p.getDetectedHeader() + "' résolue par fallback NUMERIC " +
                            "doit avoir confidenceScore >= 0.50, obtenu : " + p.getConfidenceScore())
                )
                .count();
        assertEquals(2, resolvedCount, "Deux colonnes NUMERIC doivent être résolues (VALUE_N et VALUE_N1)");
    }

    // Test 3 : badge "Douteux" — score >= 0.50 et < 0.85
    // Vérifie que la résolution fallback NUMERIC ne produit pas un badge "Inconnu"
    @Test
    void fallback_numeric_produit_badge_douteux_pas_inconnu() throws Exception {
        MockMultipartFile file = buildXlsx(
                new String[]{"Donnée A", "Donnée B", "Donnée C"},
                new double[][]{{3.2, 2.8}, {0.45, 0.38}, {12, 9}}
        );

        List<ColumnProfileDTO> profiles = service.profile(file);

        profiles.stream()
                .filter(p -> "VALUE_N1".equals(p.getLikelySemantic()))
                .forEach(p -> {
                    double score = p.getConfidenceScore();
                    assertTrue(score >= 0.50,
                            "Score doit être >= 0.50 (badge Douteux), obtenu : " + score);
                    assertTrue(score < 0.85,
                            "Score doit être < 0.85 (pas badge Sûr), obtenu : " + score);
                });
    }
}
