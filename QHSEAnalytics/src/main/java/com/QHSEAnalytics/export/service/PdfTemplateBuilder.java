package com.QHSEAnalytics.export.service;

import com.QHSEAnalytics.shared.dto.response.AdminAnalysteItemResponse;
import com.QHSEAnalytics.shared.dto.response.AdminKpiCritiqueResponse;
import com.QHSEAnalytics.shared.dto.response.AdminRepartitionResponse;
import com.QHSEAnalytics.shared.dto.response.AdminStatsResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseCategorieResponse;
import com.QHSEAnalytics.shared.dto.response.AnalyseGlobaleResponse;
import com.QHSEAnalytics.shared.dto.response.LigneComparatifResponse;
import com.QHSEAnalytics.shared.dto.response.ResumeCategorieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfTemplateBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String getCommonStyles() {
        return """
                :root {
                  --blue-dark: #1F3864;
                  --blue-med: #2E75B6;
                  --teal: #1D7874;
                  --orange: #C55A11;
                  --red: #C00000;
                  --green: #375623;
                  --gray-light: #F2F2F2;
                }
                body {
                  margin: 0;
                  padding: 20px;
                  font-family: Arial, sans-serif;
                  color: #222;
                }
                .page-break {
                  page-break-after: always;
                }
                .header {
                  background: #1F3864;
                  color: white;
                  padding: 20px;
                  border-radius: 8px;
                  margin-bottom: 24px;
                }
                .card {
                  border: 1px solid #BDD7EE;
                  border-radius: 8px;
                  padding: 16px;
                  margin-bottom: 16px;
                }
                .card-title {
                  font-weight: bold;
                  color: #1F3864;
                  font-size: 14px;
                  margin-bottom: 8px;
                }
                .badge-critique {
                  background: #C00000;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                .badge-modere {
                  background: #C55A11;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                .badge-faible {
                  background: #375623;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                .badge-hausse {
                  color: #C00000;
                  font-weight: bold;
                }
                .badge-baisse {
                  color: #375623;
                  font-weight: bold;
                }
                .badge-stable {
                  color: #555555;
                }
                .badge-ok {
                  background: #375623;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                .badge-alerte {
                  background: #C00000;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                .badge-inactif {
                  background: #777;
                  color: white;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 11px;
                  font-weight: bold;
                }
                table {
                  width: 100%;
                  border-collapse: collapse;
                  font-size: 11px;
                }
                th {
                  background: #1F3864;
                  color: white;
                  padding: 8px;
                  text-align: left;
                }
                td {
                  padding: 6px 8px;
                  border-bottom: 1px solid #E0E0E0;
                }
                tr:nth-child(even) {
                  background: #F2F2F2;
                }
                .section-title {
                  font-size: 16px;
                  font-weight: bold;
                  color: #1F3864;
                  border-left: 4px solid #2E75B6;
                  padding-left: 12px;
                  margin: 24px 0 12px 0;
                }
                .analyse-ia {
                  background: #EBF3FB;
                  border-radius: 6px;
                  padding: 12px;
                  margin-bottom: 8px;
                  font-size: 11px;
                }
                .plan-critique {
                  background: #FFF2F2;
                  border-left: 4px solid #C00000;
                  padding: 12px;
                  margin-bottom: 8px;
                  border-radius: 4px;
                }
                .plan-modere {
                  background: #FFF8F2;
                  border-left: 4px solid #C55A11;
                  padding: 12px;
                  margin-bottom: 8px;
                  border-radius: 4px;
                }
                .plan-surveillance {
                  background: #F2FFF2;
                  border-left: 4px solid #375623;
                  padding: 12px;
                  margin-bottom: 8px;
                  border-radius: 4px;
                }
                .footer {
                  text-align: center;
                  font-size: 10px;
                  color: #888;
                  margin-top: 32px;
                  padding-top: 12px;
                  border-top: 1px solid #CCCCCC;
                }
                .muted {
                  color: #666;
                  font-size: 11px;
                }
                .category-row {
                  background: #DDEBF7 !important;
                  font-weight: bold;
                }
                .eightd-table {
                  width: 100%;
                  margin-top: 10px;
                  border: 1px solid #BDD7EE;
                  font-size: 10px;
                }
                .eightd-table th {
                  background: #DEEAF6;
                  color: #1F3864;
                  width: 20%;
                  font-weight: bold;
                }
                .eightd-table td {
                  background: white;
                }
                .action-box {
                  border-left: 3px solid #2E75B6;
                  padding-left: 8px;
                  margin-top: 5px;
                }
                ul {
                  margin: 8px 0 0 16px;
                  padding: 0;
                }
                li {
                  margin-bottom: 6px;
                }
                """;
    }

    public String buildAnalysteTemplate(
            String nomAnalyste,
            String prenomAnalyste,
            int periodeN1,
            int periodeN,
            LocalDateTime dateGeneration,
            List<ResumeCategorieResponse> resumeCategories,
            List<LigneComparatifResponse> lignesComparatif,
            List<AnalyseCategorieResponse> analysesCategories,
            AnalyseGlobaleResponse analyseGlobale
    ) {
        StringBuilder html = new StringBuilder(16_384);
        appendDocumentStart(html, "Rapport Comparatif QHSE");

        String analysteFullName = escapeHtml((prenomAnalyste == null ? "" : prenomAnalyste) + " " + (nomAnalyste == null ? "" : nomAnalyste)).trim();
        String dateFormatted = formatDateTime(dateGeneration);

        html.append("<div class=\"page-break\">")
                .append("<div class=\"header\">")
                .append("<h1 style=\"margin:0 0 8px 0;font-size:30px;\">QHSE Analytics</h1>")
                .append("<div style=\"font-size:18px;\">Rapport Comparatif QHSE</div>")
                .append("<div style=\"margin-top:10px;font-size:13px;\">Periode : ").append(periodeN1).append(" vs ").append(periodeN).append("</div>")
                .append("</div>")
                .append("<div class=\"card\">")
                .append("<div><strong>Analyste :</strong> ").append(analysteFullName).append("</div>")
                .append("<div><strong>Date de generation :</strong> ").append(dateFormatted).append("</div>")
                .append("<div><strong>Genere par :</strong> QHSE Analytics Platform</div>")
                .append("</div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<ResumeCategorieResponse> safeResume = resumeCategories == null ? List.of() : resumeCategories;
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Resume par Categorie</div>");
        if (safeResume.isEmpty()) {
            html.append("<div class=\"card\">Aucune donnee de resume disponible.</div>");
        } else {
            safeResume.stream()
                    .sorted(Comparator.comparing(ResumeCategorieResponse::getCategorieCode, Comparator.nullsLast(String::compareTo)))
                    .forEach(item -> html.append("<div class=\"card\" style=\"background:")
                            .append(categoryBackground(item.getCouleur()))
                            .append(";\">")
                            .append("<div class=\"card-title\">")
                            .append(escapeHtml(item.getCategorieLibelle()))
                            .append(" (")
                            .append(escapeHtml(item.getCategorieCode()))
                            .append(")</div>")
                            .append("<div><strong>Variation moyenne :</strong> ")
                            .append(formatPercent(item.getVariationMoyenne()))
                            .append("</div>")
                            .append("<div><strong>KPIs critiques :</strong> ")
                            .append(item.getNombreKpisCritiques())
                            .append(" | <strong>Moderes :</strong> ")
                            .append(item.getNombreKpisModeres())
                            .append(" | <strong>Faibles :</strong> ")
                            .append(item.getNombreKpisFaibles())
                            .append("</div>")
                            .append("</div>"));
        }
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<LigneComparatifResponse> safeLignes = lignesComparatif == null ? List.of() : lignesComparatif;
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Tableau Comparatif N-1 / N</div>")
                .append("<table>")
                .append("<thead><tr>")
                .append("<th>Categorie</th><th>KPI</th><th>Unite</th><th>").append(periodeN1).append("</th><th>").append(periodeN).append("</th><th>Variation</th><th>Niveau</th><th>Tendance</th>")
                .append("</tr></thead><tbody>");

        String currentCategorie = null;
        for (LigneComparatifResponse ligne : safeLignes) {
            String categorieCode = nullSafe(ligne.getCategorieCode(), "N/A");
            String categorieLabel = nullSafe(ligne.getCategorieLibelle(), "Non categorisee") + " (" + categorieCode + ")";

            if (!categorieCode.equals(currentCategorie)) {
                currentCategorie = categorieCode;
                html.append("<tr class=\"category-row\"><td colspan=\"8\">")
                        .append(escapeHtml(categorieLabel))
                        .append("</td></tr>");
            }

            html.append("<tr>")
                    .append("<td>").append(escapeHtml(categorieCode)).append("</td>")
                    .append("<td>").append(escapeHtml(ligne.getKpiNom())).append("</td>")
                    .append("<td>").append(escapeHtml(ligne.getUnite())).append("</td>")
                    .append("<td>").append(formatNumber(ligne.getValeurN1())).append("</td>")
                    .append("<td>").append(formatNumber(ligne.getValeurN())).append("</td>")
                    .append("<td>").append(formatPercent(ligne.getVariationRelative())).append("</td>")
                    .append("<td>").append(renderNiveauBadge(ligne.getNiveauVariation())).append("</td>")
                    .append("<td>").append(renderTendance(ligne.getTendance())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        Map<String, List<LigneComparatifResponse>> byCategorie = safeLignes.stream()
                .collect(Collectors.groupingBy(
                        ligne -> nullSafe(ligne.getCategorieCode(), "N/A"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Analyses IA par KPI</div>");
        if (byCategorie.isEmpty()) {
            html.append("<div class=\"card\">Aucune analyse IA disponible.</div>");
        } else {
            byCategorie.forEach((categorieCode, lignes) -> {
                String label = lignes.isEmpty() ? categorieCode : nullSafe(lignes.get(0).getCategorieLibelle(), categorieCode);
                html.append("<div class=\"card-title\" style=\"font-size:16px;margin-top:10px;\">")
                        .append(escapeHtml(label))
                        .append(" (")
                        .append(escapeHtml(categorieCode))
                        .append(")</div>");

                for (LigneComparatifResponse ligne : lignes) {
                    html.append("<div class=\"card\">")
                            .append("<div class=\"card-title\">")
                            .append(escapeHtml(ligne.getKpiNom()))
                            .append("</div>")
                            .append("<div><strong>Variation :</strong> ")
                            .append(formatPercent(ligne.getVariationRelative()))
                            .append(" - <strong>Risque :</strong> ")
                            .append(renderNiveauBadge(ligne.getRiskLevel() != null ? ligne.getRiskLevel() : ligne.getNiveauVariation()))
                            .append("</div>");

                    if (ligne.getRiskJustification() != null) {
                        html.append("<div class=\"analyse-ia\"><strong>Analyse :</strong> ")
                                .append(nl2br(ligne.getRiskJustification()))
                                .append("</div>");
                    } else if (ligne.getAnalyseIa() != null) {
                        html.append("<div class=\"analyse-ia\">")
                                .append(nl2br(ligne.getAnalyseIa()))
                                .append("</div>");
                    }

                    if (ligne.getCorrectiveAction() != null || ligne.getPreventiveAction() != null) {
                        html.append("<div style=\"margin-top:10px;\">")
                                .append("<div class=\"action-box\"><strong>Action Corrective :</strong> ")
                                .append(escapeHtml(ligne.getCorrectiveAction())).append("</div>")
                                .append("<div class=\"action-box\"><strong>Action Preventive :</strong> ")
                                .append(escapeHtml(ligne.getPreventiveAction())).append("</div>")
                                .append("</div>");
                    }

                    if (ligne.getImmediateAction() != null) {
                        html.append("<div class=\"plan-modere\" style=\"margin-top:8px;font-size:10px;\">")
                                .append("<strong>Action Immediate (").append(escapeHtml(ligne.getImmediatePriority())).append(") :</strong> ")
                                .append(escapeHtml(ligne.getImmediateAction()))
                                .append("</div>");
                    }

                    if (ligne.getRequires8d() != null && ligne.getRequires8d()) {
                        html.append("<div style=\"margin-top:12px;\">")
                                .append("<div style=\"font-weight:bold;font-size:11px;color:#1F3864;\">Méthodologie 8D</div>")
                                .append(renderEightDTable(ligne.getEightDDetails()))
                                .append("</div>");
                    }

                    html.append("</div>");
                }
            });
        }
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<AnalyseCategorieResponse> safeAnalyses = analysesCategories == null ? List.of() : analysesCategories;
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Analyses par Categorie</div>");
        if (safeAnalyses.isEmpty()) {
            html.append("<div class=\"card\">Aucune analyse par categorie disponible.</div>");
        } else {
            safeAnalyses.forEach(analyse -> html.append("<div class=\"card\">")
                    .append("<div class=\"card-title\">")
                    .append(escapeHtml(analyse.getCategorieLibelle()))
                    .append(" (")
                    .append(escapeHtml(analyse.getCategorieCode()))
                    .append(")</div>")
                    .append("<div>")
                    .append(nl2br(nullSafe(analyse.getContenu(), "Analyse indisponible.")))
                    .append("</div>")
                    .append("</div>"));
        }
        appendFooter(html, dateFormatted);
        html.append("</div>");

        String synthese = analyseGlobale == null ? "Synthese indisponible." : nullSafe(analyseGlobale.getSynthese(), "Synthese indisponible.");
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Synthese Globale</div>")
                .append("<div class=\"card\">")
                .append(nl2br(synthese))
                .append("</div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        PlanSections sections = extractPlanSections(analyseGlobale == null ? null : analyseGlobale.getPlanActions());
        html.append("<div>")
                .append("<div class=\"section-title\">Plan d'Actions Priorise</div>")
                .append("<div class=\"plan-critique\"><strong>ACTIONS CRITIQUES</strong><br/>")
                .append(nl2br(sections.critique))
                .append("</div>")
                .append("<div class=\"plan-modere\"><strong>ACTIONS IMPORTANTES</strong><br/>")
                .append(nl2br(sections.modere))
                .append("</div>")
                .append("<div class=\"plan-surveillance\"><strong>SURVEILLANCE</strong><br/>")
                .append(nl2br(sections.surveillance))
                .append("</div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        appendDocumentEnd(html);
        return html.toString();
    }

    public String buildAdminTemplate(
            int periodeN1,
            int periodeN,
            LocalDateTime dateGeneration,
            AdminStatsResponse stats,
            List<AdminAnalysteItemResponse> analystes,
          List<AdminKpiCritiqueResponse> kpisCritiques,
          AdminRepartitionResponse repartitionComplete
    ) {
        StringBuilder html = new StringBuilder(12_288);
        appendDocumentStart(html, "Rapport Global QHSE Analytics");

        String dateFormatted = formatDateTime(dateGeneration);
        String periodeLabel = (periodeN1 > 0 && periodeN > 0)
                ? (periodeN1 + " vs " + periodeN)
                : "Global consolide";

        html.append("<div class=\"page-break\">")
                .append("<div class=\"header\">")
                .append("<h1 style=\"margin:0 0 8px 0;font-size:28px;\">Rapport Global QHSE Analytics</h1>")
                .append("<div style=\"font-size:14px;\">Periode : ").append(escapeHtml(periodeLabel)).append("</div>")
                .append("<div style=\"margin-top:8px;font-size:12px;\">Date de generation : ").append(dateFormatted).append("</div>")
                .append("</div>")
                .append("<div class=\"card\"><strong>Genere par :</strong> Administrateur</div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        AdminStatsResponse safeStats = stats == null ? AdminStatsResponse.builder().build() : stats;
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Statistiques Globales</div>")
                .append("<div class=\"card\"><div class=\"card-title\">Total analyses</div><div style=\"font-size:22px;\">")
                .append(safeStats.getTotalAnalyses())
                .append("</div></div>")
                .append("<div class=\"card\"><div class=\"card-title\">Total KPIs critiques</div><div style=\"font-size:22px;\">")
                .append(safeStats.getTotalKpisCritiques())
                .append("</div></div>")
                .append("<div class=\"card\"><div class=\"card-title\">Analystes actifs</div><div style=\"font-size:22px;\">")
                .append(safeStats.getNombreAnalystesActifs())
                .append("</div></div>")
                .append("<div class=\"card\"><div class=\"card-title\">Nombre admins</div><div style=\"font-size:22px;\">")
                .append(safeStats.getNombreAdmins())
                .append("</div></div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<AdminAnalysteItemResponse> safeAnalystes = analystes == null ? List.of() : analystes;
        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Vue par Analyste</div>")
                .append("<table><thead><tr>")
                .append("<th>Analyste</th><th>Derniere periode</th><th>KPIs critiques</th><th>Statut</th>")
                .append("</tr></thead><tbody>");
        for (AdminAnalysteItemResponse analyste : safeAnalystes) {
            String fullName = nullSafe(analyste.getPrenom(), "") + " " + nullSafe(analyste.getNom(), "");
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(fullName.trim())).append("</td>")
                    .append("<td>").append(escapeHtml(nullSafe(analyste.getDernierePeriode(), "N/A"))).append("</td>")
                    .append("<td>").append(analyste.getNombreKpisCritiques()).append("</td>")
                    .append("<td>").append(renderStatutBadge(analyste.getStatut())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<AdminKpiCritiqueResponse> safeKpis = (kpisCritiques == null ? List.<AdminKpiCritiqueResponse>of() : kpisCritiques).stream()
                .sorted(Comparator.comparingInt(AdminKpiCritiqueResponse::getNombreAnalystesAvecCritique).reversed())
                .limit(10)
                .toList();

        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Top 10 KPIs Critiques</div>")
                .append("<table><thead><tr>")
                .append("<th>KPI</th><th>Categorie</th><th>Nb analystes concernes</th><th>Variation moyenne</th>")
                .append("</tr></thead><tbody>");
        for (AdminKpiCritiqueResponse item : safeKpis) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(item.getKpiNom())).append("</td>")
                    .append("<td>").append(escapeHtml(item.getCategorieLibelle())).append(" (")
                    .append(escapeHtml(item.getCategorieCode())).append(")</td>")
                    .append("<td>").append(item.getNombreAnalystesAvecCritique()).append("</td>")
                    .append("<td>").append(formatPercent(item.getVariationMoyenne())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        List<AdminRepartitionResponse.RepartitionCategorieItem> repartitionItems =
          repartitionComplete == null || repartitionComplete.getCategories() == null
            ? List.of()
            : repartitionComplete.getCategories();

        html.append("<div class=\"page-break\">")
                .append("<div class=\"section-title\">Repartition des Niveaux par Categorie</div>")
                .append("<table><thead><tr>")
          .append("<th>Categorie</th><th>Faibles</th><th>Moderes</th><th>Critiques</th><th>Total</th>")
                .append("</tr></thead><tbody>");
        if (repartitionItems.isEmpty()) {
            html.append("<tr><td colspan=\"5\">Aucune donnee disponible.</td></tr>");
        } else {
            repartitionItems.stream()
              .sorted(Comparator.comparing(AdminRepartitionResponse.RepartitionCategorieItem::getCategorieCode, Comparator.nullsLast(String::compareTo)))
              .forEach(item -> html.append("<tr>")
              .append("<td>")
              .append(escapeHtml(nullSafe(item.getCategorieLibelle(), item.getCategorieCode())))
              .append(" (")
              .append(escapeHtml(item.getCategorieCode()))
              .append(")</td>")
              .append("<td>").append(item.getNombreFaibles()).append("</td>")
              .append("<td>").append(item.getNombreModeres()).append("</td>")
              .append("<td>").append(item.getNombreCritiques()).append("</td>")
              .append("<td>").append(item.getTotal()).append("</td>")
                    .append("</tr>"));
        }
        html.append("</tbody></table>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        html.append("<div>")
                .append("<div class=\"section-title\">Plan d'Actions Consolide Global</div>")
                .append("<div class=\"plan-critique\"><strong>ACTIONS CRITIQUES</strong>")
                .append(buildAdminCriticalActions(safeKpis))
                .append("</div>")
                .append("<div class=\"plan-modere\"><strong>ACTIONS IMPORTANTES</strong>")
                .append(buildAdminModerateActions(safeKpis))
                .append("</div>")
                .append("<div class=\"plan-surveillance\"><strong>SURVEILLANCE</strong>")
                .append(buildAdminSurveillanceActions(safeAnalystes))
                .append("</div>");
        appendFooter(html, dateFormatted);
        html.append("</div>");

        appendDocumentEnd(html);
        return html.toString();
    }

    private void appendDocumentStart(StringBuilder html, String title) {
        html.append("<!DOCTYPE html>")
                .append("<html lang=\"fr\"><head>")
                .append("<meta charset=\"UTF-8\"/>")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>")
                .append("<title>").append(escapeHtml(title)).append("</title>")
                .append("<style>").append(getCommonStyles()).append("</style>")
                .append("</head><body>");
    }

    private void appendDocumentEnd(StringBuilder html) {
        html.append("</body></html>");
    }

    private void appendFooter(StringBuilder html, String dateFormatted) {
        html.append("<div class=\"footer\">QHSE Analytics - Rapport genere le ")
                .append(escapeHtml(dateFormatted))
                .append(" - Confidentiel</div>");
    }

    private String buildAdminCriticalActions(List<AdminKpiCritiqueResponse> kpisCritiques) {
        List<AdminKpiCritiqueResponse> top = kpisCritiques.stream().limit(3).toList();
        if (top.isEmpty()) {
            return "<br/>Aucune action critique identifiee.";
        }
        StringBuilder html = new StringBuilder("<ul>");
        for (AdminKpiCritiqueResponse item : top) {
            html.append("<li>Traiter en priorite le KPI ")
                    .append(escapeHtml(item.getKpiNom()))
                    .append(" (categorie ")
                    .append(escapeHtml(item.getCategorieCode()))
                    .append(") impactant ")
                    .append(item.getNombreAnalystesAvecCritique())
                    .append(" analyste(s).</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String buildAdminModerateActions(List<AdminKpiCritiqueResponse> kpisCritiques) {
        List<AdminKpiCritiqueResponse> next = kpisCritiques.stream().skip(3).limit(4).toList();
        if (next.isEmpty()) {
            return "<br/>Aucune action importante supplementaire.";
        }
        StringBuilder html = new StringBuilder("<ul>");
        for (AdminKpiCritiqueResponse item : next) {
            html.append("<li>Mettre en place un plan correctif sur ")
                    .append(escapeHtml(item.getKpiNom()))
                    .append(" avec suivi mensuel de la variation moyenne (")
                    .append(formatPercent(item.getVariationMoyenne()))
                    .append(").</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String buildAdminSurveillanceActions(List<AdminAnalysteItemResponse> analystes) {
        List<String> inactifs = analystes.stream()
                .filter(a -> "INACTIF".equalsIgnoreCase(nullSafe(a.getStatut(), "")))
                .map(a -> (nullSafe(a.getPrenom(), "") + " " + nullSafe(a.getNom(), "")).trim())
                .filter(name -> !name.isBlank())
                .limit(5)
                .toList();

        List<String> actions = new ArrayList<>();
        actions.add("Maintenir un suivi trimestriel des KPIs non critiques pour detecter toute derive.");
        if (!inactifs.isEmpty()) {
            actions.add("Reactiver et accompagner les analystes inactifs : " + String.join(", ", inactifs) + ".");
        }
        actions.add("Conserver un reporting centralise et un point d'avancement hebdomadaire.");

        StringBuilder html = new StringBuilder("<ul>");
        for (String action : actions) {
            html.append("<li>").append(escapeHtml(action)).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String renderNiveauBadge(String niveauVariation) {
        String niveau = nullSafe(niveauVariation, "N/A").toUpperCase(Locale.ROOT);
        String cssClass = switch (niveau) {
            case "CRITIQUE" -> "badge-critique";
            case "MODERE" -> "badge-modere";
            case "FAIBLE" -> "badge-faible";
            default -> "badge-stable";
        };
        return "<span class=\"" + cssClass + "\">" + escapeHtml(niveau) + "</span>";
    }

    private String renderEightDTable(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            // Very simple JSON parsing for D1-D8 since we can't easily include Jackson here
            // or we use a Map if we pass it already parsed. 
            // For now, I'll assume it's a simple JSON string and do basic cleaning.
            String clean = json.replace("{", "").replace("}", "").replace("\"", "");
            String[] pairs = clean.split(",");
            Map<String, String> steps = new LinkedHashMap<>();
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    steps.put(kv[0].trim(), kv[1].trim());
                }
            }

            StringBuilder sb = new StringBuilder("<table class=\"eightd-table\">");
            String[] dSteps = {"D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8"};
            String[] dLabels = {"Equipe", "Problème", "Confinement", "Cause Racine", "Correctives", "Validation", "Prévention", "Clôture"};
            
            for (int i = 0; i < dSteps.length; i++) {
                String d = dSteps[i];
                String val = steps.getOrDefault(d, "N/A");
                sb.append("<tr><th>").append(d).append(" - ").append(dLabels[i]).append("</th><td>")
                  .append(escapeHtml(val)).append("</td></tr>");
            }
            sb.append("</table>");
            return sb.toString();
        } catch (Exception e) {
            return "<div class=\"muted\">Données 8D indisponibles.</div>";
        }
    }

    private String renderTendance(String tendance) {
        String value = nullSafe(tendance, "STABLE").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HAUSSE" -> "<span class=\"badge-hausse\">&#8593; HAUSSE</span>";
            case "BAISSE" -> "<span class=\"badge-baisse\">&#8595; BAISSE</span>";
            default -> "<span class=\"badge-stable\">&#8594; STABLE</span>";
        };
    }

    private String renderStatutBadge(String statut) {
        String value = nullSafe(statut, "INACTIF").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "ALERTE" -> "<span class=\"badge-alerte\">ALERTE</span>";
            case "OK" -> "<span class=\"badge-ok\">OK</span>";
            default -> "<span class=\"badge-inactif\">INACTIF</span>";
        };
    }

    private String categoryBackground(String couleur) {
        String value = nullSafe(couleur, "").toUpperCase(Locale.ROOT);
        return switch (value) {
            case "ROUGE" -> "#FFF2F2";
            case "ORANGE" -> "#FFF8F2";
            case "VERT" -> "#F2FFF2";
            default -> "#FFFFFF";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return DATE_TIME_FORMAT.format(dateTime);
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f%%", value);
    }

    private String nullSafe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String nl2br(String raw) {
        return escapeHtml(nullSafe(raw, "")).replace("\n", "<br/>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private PlanSections extractPlanSections(String planActions) {
        if (planActions == null || planActions.isBlank()) {
            return new PlanSections(
                    "Aucune action critique detectee.",
                    "Aucune action importante proposee.",
                    "Aucune action de surveillance proposee."
            );
        }

        StringBuilder critique = new StringBuilder();
        StringBuilder modere = new StringBuilder();
        StringBuilder surveillance = new StringBuilder();

        String currentSection = "MODERE";
        String[] lines = planActions.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            if (containsAny(trimmed, "ACTIONS CRITIQUES", "CRITIQUES", "CRITIQUE")) {
                currentSection = "CRITIQUE";
                continue;
            }
            if (containsAny(trimmed, "ACTIONS IMPORTANTES", "IMPORTANTES", "MODEREES", "MODERE")) {
                currentSection = "MODERE";
                continue;
            }
            if (containsAny(trimmed, "SURVEILLANCE", "SUIVI")) {
                currentSection = "SURVEILLANCE";
                continue;
            }

            if ("CRITIQUE".equals(currentSection)) {
                appendLine(critique, trimmed);
            } else if ("SURVEILLANCE".equals(currentSection)) {
                appendLine(surveillance, trimmed);
            } else {
                appendLine(modere, trimmed);
            }
        }

        if (critique.isEmpty() && modere.isEmpty() && surveillance.isEmpty()) {
            modere.append(planActions.trim());
        }

        return new PlanSections(
                critique.isEmpty() ? "Aucune action critique detectee." : critique.toString(),
                modere.isEmpty() ? "Aucune action importante proposee." : modere.toString(),
                surveillance.isEmpty() ? "Aucune action de surveillance proposee." : surveillance.toString()
        );
    }

    private boolean containsAny(String source, String... terms) {
        String up = source.toUpperCase(Locale.ROOT);
        for (String term : terms) {
            if (up.contains(term.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void appendLine(StringBuilder builder, String line) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private record PlanSections(String critique, String modere, String surveillance) {
    }
}
