package com.QHSEAnalytics.analytics.service.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqPromptBuilder {

    private final PromptSanitizer promptSanitizer;

    public String buildKpiPrompt(
            String kpiNom,
            String definition,
            String unite,
            String categorieLibelle,
            double valeurN1,
            double valeurN,
            double variationRelative,
            int periodeN1,
            int periodeN
    ) {

                String safeName = safe(kpiNom);
                if (safeName.length() > 100) {
                        safeName = safeName.substring(0, 100);
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Tu es un expert QHSE. Réponds UNIQUEMENT avec un objet JSON valide. ");
                sb.append("Aucune explication, aucun markdown, aucun bloc de code. Commence ta réponse par { et termine par }.");
                sb.append("\n\nFournis exactement la structure JSON suivante (tous les champs doivent être présents):\n");
                sb.append("{\n");
                sb.append("  \"risqueIa\": \"Élevé|Modéré|Faible\",\n");
                sb.append("  \"noteIa\": \"short summary text\",\n");
                sb.append("  \"identificationRisque\": \"text\",\n");
                sb.append("  \"problemeDetecte\": \"text\",\n");
                sb.append("  \"actionsCorrectives\": \"text\",\n");
                sb.append("  \"actionsPreventives\": \"text\",\n");
                sb.append("  \"actionImmediate\": \"text\",\n");
                sb.append("  \"prioriteAction\": \"HAUTE|MOYENNE|FAIBLE\",\n");
                sb.append("  \"methode8D\": {\n");
                sb.append("    \"D1\": \"text\", \"D2\": \"text\", \"D3\": \"text\", \"D4\": \"text\",\n");
                sb.append("    \"D5\": \"text\", \"D6\": \"text\", \"D7\": \"text\", \"D8\": \"text\"\n");
                sb.append("  },\n");
                sb.append("  \"noteFinale\": \"text\"\n");
                sb.append("}\n\n");


                sb.append("Contexte KPI (seulement les champs suivants) :\n");
                sb.append("- nom: ").append(safeName).append("\n");
                sb.append("- valeur_n: ").append(formatValue(valeurN)).append("\n");
                sb.append("- valeur_n_1: ").append(formatValue(valeurN1)).append("\n");
                sb.append("- variation_percent: ").append(format(variationRelative)).append("\n");
                sb.append("- unite: ").append(safe(unite)).append("\n");
                sb.append("- categorie: ").append(safe(categorieLibelle)).append("\n\n");

                sb.append("Utilise le contexte QHSE ci-dessus pour enrichir ton analyse (formule, direction, benchmarks, causes, normes ISO).\n");
                sb.append("Réponds en français. Les champs texte doivent être brefs et factuels.\n");

                return sb.toString();
    }

    public String buildColumnMappingPrompt(List<String> headers) {
        String headerList = headers.stream()
                .map(header -> "- " + safe(header))
                .collect(Collectors.joining("\n"));

        return "Tu es un expert QHSE responsable de la normalisation des données d'import Excel.\n\n"
                + "Voici les en-têtes trouvées dans un fichier Excel :\n"
                + headerList + "\n\n"
                + "Fournis uniquement un objet JSON valide avec les clés suivantes :\n"
                + "{\n"
                + "  \"kpi\": \"nom de la colonne KPI\",\n"
                + "  \"categorie\": \"nom de la colonne catégorie\",\n"
                + "  \"unite\": \"nom de la colonne unité\",\n"
                + "  \"annee_n\": \"nom de la colonne année N\",\n"
                + "  \"annee_n_1\": \"nom de la colonne année N-1\",\n"
                + "  \"valeur_n\": \"nom de la colonne valeur N\",\n"
                + "  \"valeur_n_1\": \"nom de la colonne valeur N-1\"\n"
                + "}\n\n"
                + "Ne fournis aucun texte supplémentaire, uniquement l'objet JSON.";
    }

    public String buildKpiMatchingPrompt(String rawLabel, String rawCategorie, List<com.QHSEAnalytics.shared.entity.Kpi> candidates) {
        String candidateList = candidates.stream()
                .map(kpi -> "- " + safe(kpi.getNom()) + " (" + safe(kpi.getCategorieKpi().getLibelle()) + ")")
                .collect(Collectors.joining("\n"));

        return "Tu es un expert QHSE. En te basant sur le label suivant et la catégorie éventuellement fournie, retourne exactement le nom du KPI correspondant parmi la liste ci-dessous.\n\n"
                + "Label brut : \"" + safe(rawLabel) + "\"\n"
                + "Categorie : \"" + safe(rawCategorie) + "\"\n\n"
                + "Liste des KPI disponibles :\n"
                + candidateList + "\n\n"
                + "Réponds uniquement par le nom exact du KPI correspondant.\n";
    }

    private String safe(String value) {
        return promptSanitizer.sanitize(value);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

        private String formatValue(Double value) {
                if (value == null) {
                        return "N/A";
                }
                return String.format(Locale.ROOT, "%.2f", value);
        }
}
