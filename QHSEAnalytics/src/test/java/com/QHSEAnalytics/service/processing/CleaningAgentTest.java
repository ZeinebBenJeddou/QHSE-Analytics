package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.importer.service.processing.CleaningAgent;
import com.QHSEAnalytics.shared.dto.request.KpiRawDataDTO;
import com.QHSEAnalytics.shared.dto.response.ImportIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CleaningAgentTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private final CleaningAgent agent = new CleaningAgent();

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(agent, "outlierThresholdPercent", 500.0);
        ReflectionTestUtils.setField(agent, "scoreBase",               100);
        ReflectionTestUtils.setField(agent, "penaltyInvalid",           50);
        ReflectionTestUtils.setField(agent, "penaltyError",             30);
        ReflectionTestUtils.setField(agent, "penaltyWarning",           10);
        ReflectionTestUtils.setField(agent, "penaltyInfo",               5);
    }

    private KpiRawDataDTO invokeSanitizeRow(KpiRawDataDTO row) throws Exception {
        Method method = CleaningAgent.class.getDeclaredMethod("sanitizeRow", KpiRawDataDTO.class);
        method.setAccessible(true);
        return (KpiRawDataDTO) method.invoke(agent, row);
    }

    private KpiRawDataDTO.KpiRawDataDTOBuilder validRow(int index) {
        return KpiRawDataDTO.builder()
                .rowIndex(index)
                .kpiName("Taux de fréquence")
                .valid(true)
                .valeurN1(10.0)
                .valeurN(12.0);
    }

    private KpiRawDataDTO.KpiRawDataDTOBuilder invalidRow(int index) {
        return KpiRawDataDTO.builder()
                .rowIndex(index)
                .kpiName("Taux de fréquence")
                .valid(false);
    }

    private ImportIssue issueOf(ImportIssue.Severity severity, String code) {
        return ImportIssue.builder()
                .rowIndex(1).column("KPI")
                .severity(severity).code(code)
                .message("message test")
                .build();
    }

    // ── Groupe 1 — Nettoyage textuel ─────────────────────────────────────────

    @Nested
    class NettoyageTextuelTests {

        @Test
        void kpiName_espacesAvantApres_sontsupprimes() throws Exception {
            KpiRawDataDTO row = validRow(1).kpiName("  Taux de fréquence  ").build();
            assertEquals("Taux de fréquence", invokeSanitizeRow(row).getKpiName());
        }

        @Test
        void kpiName_espacesMultiplesInternes_reduits() throws Exception {
            KpiRawDataDTO row = validRow(2).kpiName("Taux   de   fréquence").build();
            assertEquals("Taux de fréquence", invokeSanitizeRow(row).getKpiName());
        }

        @Test
        void kpiName_sautDeLigneInterne_remplaceParEspace() throws Exception {
            KpiRawDataDTO row = validRow(3).kpiName("Taux\nde fréquence").build();
            assertEquals("Taux de fréquence", invokeSanitizeRow(row).getKpiName());
        }

        @Test
        void kpiName_null_resteNull_sansnpe() throws Exception {
            KpiRawDataDTO row = validRow(4).kpiName(null).build();
            assertDoesNotThrow(() -> invokeSanitizeRow(row));
            assertNull(invokeSanitizeRow(row).getKpiName());
        }

        @Test
        void categorie_et_unite_sontnettoyees() throws Exception {
            KpiRawDataDTO row = validRow(5)
                    .categorie("  Sécurité  ")
                    .unite("  %  ")
                    .build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            assertEquals("Sécurité", result.getCategorie());
            assertEquals("%", result.getUnite());
        }

        @Test
        void normalizedKpiName_calculesurlenom_nettoye_pasSurOriginal() throws Exception {
            // Original avec espaces et majuscules — normalizedKpiName doit être basé
            // sur le nom après trim, sans accents, en minuscules
            KpiRawDataDTO row = validRow(6)
                    .kpiName("  TAUX DE FRÉQUENCE  ")
                    .normalizedKpiName(null)
                    .build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            // trimAndNormalize → "TAUX DE FRÉQUENCE" → toTitleCaseIfAllCaps → "Taux De Fréquence"
            // normalizeForMatching → supprime accents, minuscules → "taux de frequence"
            assertEquals("taux de frequence", result.getNormalizedKpiName());
        }

        // ── Tests title case ────────────────────────────────────────────────

        @Test
        void titleCase_toutEnMajuscules_convertiEnTitleCase() throws Exception {
            KpiRawDataDTO row = validRow(20).kpiName("TAUX DE FRÉQUENCE").build();
            assertEquals("Taux De Fréquence", invokeSanitizeRow(row).getKpiName(),
                    "Nom entièrement en majuscules doit être converti en title case");
        }

        @Test
        void titleCase_dejaMixte_inchange() throws Exception {
            KpiRawDataDTO row = validRow(21).kpiName("Taux de fréquence").build();
            assertEquals("Taux de fréquence", invokeSanitizeRow(row).getKpiName(),
                    "Nom déjà en casse mixte ne doit pas être modifié");
        }

        @Test
        void titleCase_toutEnMinuscules_inchange() throws Exception {
            KpiRawDataDTO row = validRow(22).kpiName("taux de fréquence").build();
            assertEquals("taux de fréquence", invokeSanitizeRow(row).getKpiName(),
                    "Nom entièrement en minuscules ne doit pas être modifié (pas all-caps)");
        }

        @Test
        void titleCase_mixteMajMinAvecSigle_inchange() throws Exception {
            KpiRawDataDTO row = validRow(23).kpiName("Taux TF1").build();
            assertEquals("Taux TF1", invokeSanitizeRow(row).getKpiName(),
                    "Nom avec casse mixte (lettre minuscule présente) ne doit pas être modifié");
        }

        @Test
        void titleCase_siglePurLettesMajuscules_convertiEnTitleCase() throws Exception {
            // "TF1" : letters = "TF" → all-caps → title case appliqué → "Tf1"
            // Comportement attendu : conversion acceptée (mieux que "TF1" tout en maj
            // dans un contexte où d'autres KPIs sont en title case)
            KpiRawDataDTO row = validRow(24).kpiName("TF1").build();
            assertEquals("Tf1", invokeSanitizeRow(row).getKpiName(),
                    "Sigle court purement en majuscules : converti en title case (comportement documenté)");
        }
    }

    // ── Groupe 2 — Immuabilité ───────────────────────────────────────────────

    @Nested
    class ImmutabiliteTests {

        @Test
        void original_kpiName_inchangeapresappel() throws Exception {
            KpiRawDataDTO original = invalidRow(7).kpiName("  Taux de fréquence  ").build();
            invokeSanitizeRow(original);
            assertEquals("  Taux de fréquence  ", original.getKpiName());
        }

        @Test
        void retourne_nouvelObjet_paslOriginal() throws Exception {
            KpiRawDataDTO original = validRow(8).build();
            KpiRawDataDTO result = invokeSanitizeRow(original);
            assertNotSame(original, result);
        }

        @Test
        void liste_issues_originale_inchangee_memeAvecOutlier() throws Exception {
            ImportIssue existing = issueOf(ImportIssue.Severity.WARNING, ImportIssue.CODE_LOW_CONFIDENCE);
            // valeurN1=1, valeurN=20 → variation 1900% → outlier déclenché
            KpiRawDataDTO original = KpiRawDataDTO.builder()
                    .rowIndex(9).kpiName("Taux de fréquence")
                    .valid(true).valeurN1(1.0).valeurN(20.0)
                    .issue(existing)
                    .build();

            int countBefore = original.getIssues().size();
            invokeSanitizeRow(original);

            assertEquals(countBefore, original.getIssues().size(),
                    "La liste originale ne doit pas avoir été agrandie");
            assertSame(existing, original.getIssues().get(0),
                    "L'issue originale doit être la même référence");
        }
    }

    // ── Groupe 3 — Détection outlier ─────────────────────────────────────────

    @Nested
    class OutlierTests {

        @Test
        void variation_normale_aucunWarningOutlier() throws Exception {
            // variation = |70-10|/10 * 100 = 600% > 500 → outlier
            // ici variation = |12-10|/10 * 100 = 20% → pas d'outlier
            KpiRawDataDTO row = validRow(10).valeurN1(10.0).valeurN(12.0).build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            boolean hasOutlier = result.getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));
            assertFalse(hasOutlier, "Variation normale (20%) ne doit pas produire de WARNING outlier");
        }

        @Test
        void variation_extreme_ajouteWarningOutlier() throws Exception {
            // variation = |70-10|/10 * 100 = 600% > seuil 500%
            KpiRawDataDTO row = validRow(11).valeurN1(10.0).valeurN(70.0).build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            boolean hasOutlier = result.getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));
            assertTrue(hasOutlier, "Variation de 600% doit produire un WARNING CODE_OUTLIER_VARIATION");
        }

        @Test
        void valeurN1_egale_zero_aucunOutlierCheck() throws Exception {
            // COMPORTEMENT ACTUEL : la branche `if (v1 != 0)` court-circuite silencieusement.
            // valeurN1=0 et valeurN=1000 → variation infinie non signalée.
            // Pas de WARNING ajouté — lacune connue documentée dans l'audit.
            KpiRawDataDTO row = validRow(12).valeurN1(0.0).valeurN(1000.0).build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            boolean hasOutlier = result.getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));
            assertFalse(hasOutlier,
                    "Comportement actuel : valeurN1=0 court-circuite l'outlier check sans warning " +
                    "(lacune connue — variation infinie non détectée)");
        }

        @Test
        void ligne_invalide_pasDeOutlierCheck() throws Exception {
            // valid=false → le bloc outlier n'est pas exécuté
            KpiRawDataDTO row = KpiRawDataDTO.builder()
                    .rowIndex(13).kpiName("Taux de fréquence")
                    .valid(false).valeurN1(1.0).valeurN(1000.0)
                    .build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            boolean hasOutlier = result.getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_OUTLIER_VARIATION.equals(i.getCode()));
            assertFalse(hasOutlier, "Ligne invalide ne doit pas déclencher l'outlier check");
        }
    }

    // ── Groupe 4 — rowQualityScore ───────────────────────────────────────────

    @Nested
    class QualityScoreTests {

        @Test
        void ligneValide_sansIssue_score100() throws Exception {
            KpiRawDataDTO result = invokeSanitizeRow(validRow(14).build());
            assertEquals(100, result.getRowQualityScore());
        }

        @Test
        void ligneInvalide_sansIssue_score50() throws Exception {
            KpiRawDataDTO result = invokeSanitizeRow(invalidRow(15).build());
            assertEquals(50, result.getRowQualityScore());
        }

        @Test
        void ligneInvalide_avecError_score20() throws Exception {
            KpiRawDataDTO row = invalidRow(16)
                    .issue(issueOf(ImportIssue.Severity.ERROR, ImportIssue.CODE_MISSING_VALUE_N))
                    .build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            assertEquals(20, result.getRowQualityScore(), "100 - 50 (invalid) - 30 (ERROR) = 20");
        }

        @Test
        void ligneValide_avecOutlierWarning_score90() throws Exception {
            // valeurN1=1, valeurN=20 → 1900% → outlier WARNING ajouté dans sanitizeRow
            KpiRawDataDTO row = validRow(17).valeurN1(1.0).valeurN(20.0).build();
            KpiRawDataDTO result = invokeSanitizeRow(row);
            assertEquals(90, result.getRowQualityScore(), "100 - 10 (WARNING outlier) = 90");
        }
    }

    // ── Groupe 5 — Déduplication ─────────────────────────────────────────────

    @Nested
    class DeduplicationTests {

        private KpiRawDataDTO rowWithNorm(int index, String kpiName, String norm,
                                          Double valN1, Double valN) {
            return KpiRawDataDTO.builder()
                    .rowIndex(index).kpiName(kpiName).normalizedKpiName(norm)
                    .valid(true).valeurN1(valN1).valeurN(valN)
                    .build();
        }

        @Test
        void deuxLignesIdentiques_uneSeuleConservee_infoSurPremiere() {
            KpiRawDataDTO r1 = rowWithNorm(1, "Taux de fréquence", "taux de frequence", 10.0, 12.0);
            KpiRawDataDTO r2 = rowWithNorm(2, "Taux de fréquence", "taux de frequence", 10.0, 12.0);

            List<KpiRawDataDTO> result = agent.clean(List.of(r1, r2));

            assertEquals(1, result.size(), "Doublon exact → 1 seule ligne retournée");
            boolean hasInfo = result.get(0).getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode())
                            && i.getSeverity() == ImportIssue.Severity.INFO);
            assertTrue(hasInfo, "La 1ère ligne doit avoir une issue INFO CODE_DUPLICATE_KPI");
        }

        @Test
        void deuxLignesMemeNom_valeursDifferentes_warningConflict() {
            KpiRawDataDTO r1 = rowWithNorm(1, "Taux de fréquence", "taux de frequence", 10.0, 12.0);
            KpiRawDataDTO r2 = rowWithNorm(2, "Taux de fréquence", "taux de frequence", 10.0, 99.0);

            List<KpiRawDataDTO> result = agent.clean(List.of(r1, r2));

            assertEquals(1, result.size(), "Doublon conflictuel → 1 seule ligne (1ère occurrence)");
            boolean hasConflict = result.get(0).getIssues().stream()
                    .anyMatch(i -> ImportIssue.CODE_DUPLICATE_CONFLICT.equals(i.getCode())
                            && i.getSeverity() == ImportIssue.Severity.WARNING);
            assertTrue(hasConflict, "La 1ère ligne doit avoir un WARNING CODE_DUPLICATE_CONFLICT");
        }

        @Test
        void casseDifferente_traiteeCommeDoublon() {
            // "Taux" et "taux" → normalizedKpiName identique → même clé de dédup
            KpiRawDataDTO r1 = rowWithNorm(1, "Taux", "taux", 10.0, 12.0);
            KpiRawDataDTO r2 = rowWithNorm(2, "taux", "taux", 10.0, 12.0);

            List<KpiRawDataDTO> result = agent.clean(List.of(r1, r2));

            assertEquals(1, result.size(),
                    "\"Taux\" et \"taux\" ont le même normalizedKpiName → traités comme doublons");
        }

        @Test
        void troisLignes_deuxDoublons_deuxLignesRetournees() {
            KpiRawDataDTO r1 = rowWithNorm(1, "Taux de fréquence", "taux de frequence", 10.0, 12.0);
            KpiRawDataDTO r2 = rowWithNorm(2, "Taux de fréquence", "taux de frequence", 10.0, 12.0);
            KpiRawDataDTO r3 = rowWithNorm(3, "Taux de gravité",   "taux de gravite",   5.0,  3.0);

            List<KpiRawDataDTO> result = agent.clean(List.of(r1, r2, r3));

            assertEquals(2, result.size(), "La 1ère et la 3e (unique) doivent être conservées");
            assertEquals("Taux de fréquence", result.get(0).getKpiName());
            assertEquals("Taux de gravité",   result.get(1).getKpiName());
        }

        @Test
        void aucunDoublon_toutesLesLignesConservees() {
            KpiRawDataDTO r1 = rowWithNorm(1, "Taux de fréquence", "taux de frequence", 10.0, 12.0);
            KpiRawDataDTO r2 = rowWithNorm(2, "Taux de gravité",   "taux de gravite",   5.0,  3.0);
            KpiRawDataDTO r3 = rowWithNorm(3, "Incidents",          "incidents",          2.0,  1.0);

            List<KpiRawDataDTO> result = agent.clean(List.of(r1, r2, r3));

            assertEquals(3, result.size(), "Aucun doublon → 3 lignes conservées");
        }
    }

    // ── Groupe 6 — Robustesse ─────────────────────────────────────────────────

    @Nested
    class RobustnessTests {

        @Test
        void listeVide_retourneListeVide() {
            List<KpiRawDataDTO> result = agent.clean(List.of());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void listeAvecNull_nullFiltre_pasDenpe() {
            List<KpiRawDataDTO> input = new ArrayList<>();
            input.add(null);
            input.add(KpiRawDataDTO.builder()
                    .rowIndex(1).kpiName("Taux").normalizedKpiName("taux")
                    .valid(true).valeurN1(10.0).valeurN(12.0)
                    .build());

            assertDoesNotThrow(() -> {
                List<KpiRawDataDTO> result = agent.clean(input);
                assertEquals(1, result.size(), "Le null doit être filtré, 1 ligne valide conservée");
            });
        }

        @Test
        void centLignesIdentiques_uneSeuleRetournee_infoDoublon() {
            List<KpiRawDataDTO> input = IntStream.rangeClosed(1, 100)
                    .mapToObj(i -> KpiRawDataDTO.builder()
                            .rowIndex(i).kpiName("Taux de fréquence")
                            .normalizedKpiName("taux de frequence")
                            .valid(true).valeurN1(10.0).valeurN(12.0)
                            .build())
                    .toList();

            List<KpiRawDataDTO> result = agent.clean(input);

            assertEquals(1, result.size(), "100 doublons exacts → 1 seule ligne retournée");
            long infoCount = result.get(0).getIssues().stream()
                    .filter(i -> ImportIssue.CODE_DUPLICATE_KPI.equals(i.getCode()))
                    .count();
            assertEquals(99, infoCount,
                    "La 1ère ligne doit avoir 99 issues INFO (une par doublon ignoré)");
        }
    }
}
