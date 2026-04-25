package com.QHSEAnalytics.importengine;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.entity.ImportSession;
import com.QHSEAnalytics.entity.Kpi;
import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.ImportMode;
import com.QHSEAnalytics.enums.ImportStatut;
import com.QHSEAnalytics.enums.QualityStatus;
import com.QHSEAnalytics.repository.ImportSessionRepository;
import com.QHSEAnalytics.repository.KpiRepository;
import com.QHSEAnalytics.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoImportEngine {

    private final ExcelStructureDetector excelStructureDetector;
    private final KpiMatcher kpiMatcher;
    private final DataCleaner dataCleaner;
    private final ConfidenceEngine confidenceEngine;
    private final VariationCalculator variationCalculator;
    private final ClassificationService classificationService;
    private final ResultatKpiRepository resultatKpiRepository;
    private final ImportSessionRepository importSessionRepository;
    private final KpiRepository kpiRepository;
    private final UserRepository userRepository;

    @Transactional
    public AutoImportResult process(MultipartFile file, Long userId, int periodeN1, int periodeN) {
        validateInput(file, periodeN1, periodeN);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));

        ImportSession session = ImportSession.builder()
                .user(user)
                .mode(ImportMode.AUTO)
                .nomFichier(file.getOriginalFilename())
                .periodeN1(periodeN1)
                .periodeN(periodeN)
                .statut(ImportStatut.EN_TRAITEMENT)
                .build();
        session = importSessionRepository.save(session);

        List<ResultatKpi> savedResults = new ArrayList<>();
        int rejectedCount = 0;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            ExcelStructureDetector.ExcelStructure structure = excelStructureDetector.detectStructure(sheet);
            List<ExcelStructureDetector.RawKpiRow> rawRows = excelStructureDetector.extractRows(sheet, structure);
            List<Kpi> availableKpis = kpiRepository.findByIsActiveTrueOrderByOrdreAsc();

            for (ExcelStructureDetector.RawKpiRow rawRow : rawRows) {
                if (rawRow.label() == null || rawRow.label().isBlank()) {
                    log.debug("Omitting row without KPI label rowIndex={}", rawRow.rowIndex());
                    rejectedCount++;
                    continue;
                }
                Optional<KpiMatcher.KpiMatch> matchOptional = kpiMatcher.match(rawRow.label(), availableKpis);
                if (matchOptional.isEmpty()) {
                    log.debug("KPI non associé rowIndex={} label={}", rawRow.rowIndex(), rawRow.label());
                    rejectedCount++;
                    continue;
                }

                KpiMatcher.KpiMatch match = matchOptional.get();
                DataCleaner.CleanedRow cleaned = dataCleaner.clean(rawRow, match.kpi());
                if (!cleaned.isValid()) {
                    log.debug("Données invalides rowIndex={} label={} statusN1={} statusN={}", rawRow.rowIndex(), rawRow.label(), cleaned.statusN1(), cleaned.statusN());
                    rejectedCount++;
                    continue;
                }

                double absoluteVariation = variationCalculator.calculateAbsolute(cleaned.valeurN1(), cleaned.valeurN());
                double relativeVariation = variationCalculator.calculateRelative(cleaned.valeurN1(), cleaned.valeurN());
                var niveauVariation = classificationService.classify(match.kpi(), relativeVariation);
                var tendance = variationCalculator.determineTendance(relativeVariation);
                double businessCoherence = confidenceEngine.computeBusinessCoherence(match.kpi(), cleaned.valeurN1(), cleaned.valeurN());
                double confidenceScore = confidenceEngine.computeConfidence(match.similarity(), cleaned.validityScore(), businessCoherence);
                QualityStatus qualityStatus = confidenceEngine.assessQualityStatus(confidenceScore);

                if (qualityStatus == QualityStatus.REJECTED) {
                    log.debug("KPI rejeté par confiance rowIndex={} label={} score={} ", rawRow.rowIndex(), rawRow.label(), confidenceScore);
                    rejectedCount++;
                    continue;
                }

                ResultatKpi resultatKpi = ResultatKpi.builder()
                        .importSession(session)
                        .kpi(match.kpi())
                        .user(user)
                        .periodeN1(periodeN1)
                        .periodeN(periodeN)
                        .valeurN1(cleaned.valeurN1())
                        .valeurN(cleaned.valeurN())
                        .variationAbsolue(absoluteVariation)
                        .variationRelative(relativeVariation)
                        .niveauVariation(niveauVariation)
                        .tendance(tendance)
                        .confidenceScore(confidenceScore)
                        .qualityStatus(qualityStatus)
                        .analyseIa(buildAnalyseMessage(match, cleaned, confidenceScore, businessCoherence))
                        .build();

                savedResults.add(resultatKpi);
            }

            resultatKpiRepository.saveAll(savedResults);
            session.setStatut(ImportStatut.TRAITE);
            if (savedResults.isEmpty()) {
                session.setMessageErreur("Aucun KPI fiable détecté après traitement automatique.");
            }
            session = importSessionRepository.save(session);

            return new AutoImportResult(session, savedResults, rejectedCount);
        } catch (Exception ex) {
            resultatKpiRepository.deleteByImportSessionId(session.getId());
            session.setStatut(ImportStatut.ERREUR);
            session.setMessageErreur("Échec traitement import automatique: " + ex.getMessage());
            importSessionRepository.save(session);
            log.error("Erreur import automatique sessionId={} cause={}", session.getId(), ex.getMessage(), ex);
            throw new IllegalStateException("Impossible de traiter le fichier Excel automatiquement.");
        }
    }

    private void validateInput(MultipartFile file, int periodeN1, int periodeN) {
        if (periodeN != periodeN1 + 1) {
            throw new IllegalArgumentException("La période N doit être égale à N-1 + 1.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Seuls les fichiers .xlsx sont autorisés.");
        }
    }

    private String buildAnalyseMessage(KpiMatcher.KpiMatch match, DataCleaner.CleanedRow cleaned, double confidenceScore, double businessCoherence) {
        return String.format("Import automatique — KPI=%s, scoreMatching=%.2f, validité=%.2f, cohérence=%.2f, scoreConfiance=%.2f",
                match.kpi().getNom(), match.similarity(), cleaned.validityScore(), businessCoherence, confidenceScore);
    }

    public record AutoImportResult(ImportSession session, List<ResultatKpi> records, int rejectedCount) {}
}
