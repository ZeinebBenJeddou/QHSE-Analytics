package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.auth.entity.User;
import com.QHSEAnalytics.auth.repository.UserRepository;
import com.QHSEAnalytics.shared.dto.response.AdminAnalysteItemResponse;
import com.QHSEAnalytics.shared.dto.response.AdminGraphiquesDataResponse;
import com.QHSEAnalytics.shared.dto.response.AdminKpiCritiqueResponse;
import com.QHSEAnalytics.shared.dto.response.AdminRepartitionResponse;
import com.QHSEAnalytics.shared.dto.response.AdminStatsResponse;
import com.QHSEAnalytics.shared.dto.response.BarreGroupeeData;
import com.QHSEAnalytics.shared.dto.response.HistoriqueAnalysteResponse;
import com.QHSEAnalytics.shared.dto.response.HistoriqueItemResponse;
import com.QHSEAnalytics.shared.entity.AnalyseGlobale;
import com.QHSEAnalytics.shared.entity.ImportSession;
import com.QHSEAnalytics.shared.entity.ResultatKpi;
import com.QHSEAnalytics.shared.enums.ImportStatut;
import com.QHSEAnalytics.shared.enums.NiveauVariation;
import com.QHSEAnalytics.shared.repository.AnalyseGlobaleRepository;
import com.QHSEAnalytics.shared.repository.ImportSessionRepository;
import com.QHSEAnalytics.shared.repository.ResultatKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAdminService {

    private final ImportSessionRepository importSessionRepository;
    private final ResultatKpiRepository resultatKpiRepository;
    private final UserRepository userRepository;
    private final AnalyseGlobaleRepository analyseGlobaleRepository;


    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalAnalyses((int) importSessionRepository.countByStatutIn(List.of(ImportStatut.TRAITE, ImportStatut.READY_FOR_AI)))
                .totalKpisCritiques((int) resultatKpiRepository.countByNiveauVariation(NiveauVariation.CRITIQUE))
                .nombreAnalystesActifs((int) userRepository.countByRoleAndActiveTrue(User.Role.ANALYSTE))
                .nombreAdmins((int) userRepository.countByRole(User.Role.ADMIN))
                .build();
    }

    public List<AdminAnalysteItemResponse> getAnalystes() {
        List<User> analystes = userRepository.findByRoleOrderByNomAsc(User.Role.ANALYSTE);

        List<ImportSession> treatedSessions = importSessionRepository.findByStatutInOrderByCreatedAtDesc(List.of(ImportStatut.TRAITE, ImportStatut.READY_FOR_AI));
        Map<Long, ImportSession> latestByUser = treatedSessions.stream().collect(Collectors.toMap(
            session -> session.getUser().getId(),
            session -> session,
            (left, right) -> left,
            LinkedHashMap::new
        ));

        List<Long> latestImportIds = latestByUser.values().stream().map(ImportSession::getId).toList();

        Map<Long, Long> critiquesBySession = resultatKpiRepository
            .countByImportSessionIdsAndNiveauVariation(latestImportIds, NiveauVariation.CRITIQUE)
            .stream()
            .collect(Collectors.toMap(
                ResultatKpiRepository.ImportSessionCountView::getImportSessionId,
                view -> view.getTotal() == null ? 0L : view.getTotal(),
                (left, right) -> left,
                LinkedHashMap::new
            ));

        Map<Long, Integer> confidenceByImportId;
        if (!latestImportIds.isEmpty()) {
            confidenceByImportId = analyseGlobaleRepository.findByImportSessionIdIn(latestImportIds)
                    .stream()
                    .filter(ag -> ag.getOverallConfidence() != null)
                    .collect(Collectors.toMap(
                            ag -> ag.getImportSession().getId(),
                            AnalyseGlobale::getOverallConfidence,
                            (a, b) -> a,
                            HashMap::new));
        } else {
            confidenceByImportId = new HashMap<>();
        }

        return analystes.stream().map(user -> {
            ImportSession latest = latestByUser.get(user.getId());

            if (latest == null) {
                return AdminAnalysteItemResponse.builder()
                        .userId(user.getId())
                        .nom(user.getNom())
                        .prenom(user.getPrenom())
                        .email(user.getEmail())
                        .dernierImportId(null)
                        .dernierePeriode(null)
                        .nombreKpisCritiques(0)
                        .statut("INACTIF")
                        .build();
            }

            int critiques = critiquesBySession.getOrDefault(latest.getId(), 0L).intValue();
            String statut = critiques > 0 ? "ALERTE" : "OK";
            return AdminAnalysteItemResponse.builder()
                    .userId(user.getId())
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .email(user.getEmail())
                    .dernierImportId(latest.getId())
                    .dernierePeriode(latest.getPeriodeN1() + " → " + latest.getPeriodeN())
                    .dernierImportDate(latest.getCreatedAt())
                    .nombreKpisCritiques(critiques)
                    .statut(statut)
                    .confidenceScore(confidenceByImportId.get(latest.getId()))
                    .build();
        }).sorted(Comparator.comparingInt(item -> analysteStatusRank(item.getStatut()))).toList();
    }

    public List<AdminKpiCritiqueResponse> getTopKpisCritiques() {
        List<ResultatKpi> critiques = resultatKpiRepository.findByNiveauVariationWithKpi(NiveauVariation.CRITIQUE);
        Map<Long, List<ResultatKpi>> byKpi = critiques.stream().collect(Collectors.groupingBy(r -> r.getKpi().getId(), LinkedHashMap::new, Collectors.toList()));

        return byKpi.values().stream()
                .map(list -> {
                    ResultatKpi first = list.get(0);
                    int analysts = (int) list.stream().map(r -> r.getUser().getId()).distinct().count();
                    double moyenne = list.stream().mapToDouble(ResultatKpi::getVariationRelative).average().orElse(0d);

                    return AdminKpiCritiqueResponse.builder()
                            .kpiId(first.getKpi().getId())
                            .kpiNom(first.getKpi().getNom())
                            .categorieCode(first.getKpi().getCategorieKpi().getCode())
                            .categorieLibelle(first.getKpi().getCategorieKpi().getLibelle())
                            .nombreAnalystesAvecCritique(analysts)
                            .variationMoyenne(round2(moyenne))
                            .build();
                })
                .sorted(Comparator.comparingInt(AdminKpiCritiqueResponse::getNombreAnalystesAvecCritique).reversed())
                .limit(10)
                .toList();
    }

    public AdminGraphiquesDataResponse getGraphiques() {
        List<ResultatKpi> all = resultatKpiRepository.findAllByOrderByCreatedAtDesc();
        List<ResultatKpi> critiques = all.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).toList();

        Map<String, Integer> repartition = new LinkedHashMap<>();
        repartition.put("FAIBLE", 0);
        repartition.put("MODERE", 0);
        repartition.put("CRITIQUE", 0);
        repartition.put("FAIBLE", (int) all.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.FAIBLE).count());
        repartition.put("MODERE", (int) all.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.MODERE).count());
        repartition.put("CRITIQUE", (int) all.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count());

        Map<String, Integer> critiquesByCategorie = critiques.stream()
                .collect(Collectors.groupingBy(r -> r.getKpi().getCategorieKpi().getCode(), LinkedHashMap::new, Collectors.summingInt(value -> 1)));


        critiquesByCategorie.putIfAbsent("Q", 0);
        critiquesByCategorie.putIfAbsent("H", 0);
        critiquesByCategorie.putIfAbsent("S", 0);
        critiquesByCategorie.putIfAbsent("E", 0);

        Map<String, List<ResultatKpi>> byCategorie = all.stream()
                .collect(Collectors.groupingBy(r -> r.getKpi().getCategorieKpi().getCode(), LinkedHashMap::new, Collectors.toList()));

        List<BarreGroupeeData> evolution = byCategorie.values().stream()
                .map(list -> BarreGroupeeData.builder()
                        .categorie(list.get(0).getKpi().getCategorieKpi().getLibelle())
                        .valeurMoyenneN1(round2(list.stream().mapToDouble(ResultatKpi::getValeurN1).average().orElse(0d)))
                        .valeurMoyenneN(round2(list.stream().mapToDouble(ResultatKpi::getValeurN).average().orElse(0d)))
                        .variationMoyenne(round2(list.stream().mapToDouble(ResultatKpi::getVariationRelative).average().orElse(0d)))
                        .build())
                .toList();

        return AdminGraphiquesDataResponse.builder()
                .repartitionNiveaux(repartition)
                .kpisCritiquesByCategorie(critiquesByCategorie)
                .evolutionParCategorie(evolution)
                .build();
    }

    public HistoriqueAnalysteResponse getHistorique() {
        List<ImportSession> sessions = importSessionRepository.findAllByOrderByCreatedAtDesc();

        Map<Long, Long> critiquesBySession;
        if (sessions.isEmpty()) {
            critiquesBySession = Map.of();
        } else {
            List<ResultatKpiRepository.ImportSessionCountView> counts = resultatKpiRepository.countByImportSessionIdsAndNiveauVariation(
                sessions.stream().map(ImportSession::getId).toList(),
                NiveauVariation.CRITIQUE
            );

            critiquesBySession = counts.stream()
                .collect(Collectors.toMap(
                    ResultatKpiRepository.ImportSessionCountView::getImportSessionId,
                    view -> view.getTotal() == null ? 0L : view.getTotal()
                ));
        }

        List<HistoriqueItemResponse> items = sessions.stream().map(session ->
            HistoriqueItemResponse.builder()
                .importId(session.getId())
                .nomFichier(session.getNomFichier())
                .periodeN1(session.getPeriodeN1())
                .periodeN(session.getPeriodeN())
                .statut(session.getStatut().name())
                .messageErreur(session.getMessageErreur())
                .nombreKpisCritiques(critiquesBySession.getOrDefault(session.getId(), 0L).intValue())
                .dateImport(session.getCreatedAt())
                .utilisateurId(session.getUser().getId())
                .utilisateurNom(session.getUser().getPrenom() + " " + session.getUser().getNom())
                .utilisateurEmail(session.getUser().getEmail())
                .build()
        ).toList();

        int totalTraites = (int) sessions.stream()
            .filter(session -> session.getStatut() == ImportStatut.TRAITE || session.getStatut() == ImportStatut.READY_FOR_AI)
            .count();

        int totalErreurs = (int) sessions.stream()
            .filter(session -> session.getStatut() == ImportStatut.ERREUR)
            .count();

        return HistoriqueAnalysteResponse.builder()
                .items(items)
                .totalImports(items.size())
                .totalTraites(totalTraites)
                .totalErreurs(totalErreurs)
                .build();
    }

            @Transactional(readOnly = true)
            public AdminRepartitionResponse getRepartitionComplete() {
            List<ResultatKpi> all = resultatKpiRepository.findAllByOrderByCreatedAtDesc();

            Map<String, List<ResultatKpi>> byCategorie = all.stream()
                .filter(r -> r.getKpi() != null && r.getKpi().getCategorieKpi() != null)
                .collect(Collectors.groupingBy(
                    r -> r.getKpi().getCategorieKpi().getCode(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

            List<AdminRepartitionResponse.RepartitionCategorieItem> categories = byCategorie.values().stream()
                .map(list -> {
                    int faibles = (int) list.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.FAIBLE).count();
                    int moderes = (int) list.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.MODERE).count();
                    int critiques = (int) list.stream().filter(r -> r.getNiveauVariation() == NiveauVariation.CRITIQUE).count();
                    int total = faibles + moderes + critiques;

                    ResultatKpi first = list.get(0);
                    return AdminRepartitionResponse.RepartitionCategorieItem.builder()
                        .categorieCode(first.getKpi().getCategorieKpi().getCode())
                        .categorieLibelle(first.getKpi().getCategorieKpi().getLibelle())
                        .nombreFaibles(faibles)
                        .nombreModeres(moderes)
                        .nombreCritiques(critiques)
                        .total(total)
                        .build();
                })
                .sorted(Comparator.comparing(AdminRepartitionResponse.RepartitionCategorieItem::getCategorieCode, Comparator.nullsLast(String::compareTo)))
                .toList();

            return AdminRepartitionResponse.builder()
                .categories(categories)
                .build();
            }

    private int analysteStatusRank(String statut) {
        if ("ALERTE".equals(statut)) {
            return 0;
        }
        if ("OK".equals(statut)) {
            return 1;
        }
        return 2;
    }

    private double round2(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.2f", value));
    }
}