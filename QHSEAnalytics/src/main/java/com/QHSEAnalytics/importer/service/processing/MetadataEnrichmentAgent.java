package com.QHSEAnalytics.importer.service.processing;

import com.QHSEAnalytics.shared.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.analytics.service.LlmProviderChain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataEnrichmentAgent {

    private final LlmProviderChain llmProviderChain;
    private final ObjectMapper objectMapper;

    public List<KpiCalculatedDTO> enrichMetadata(List<KpiCalculatedDTO> data) {
        List<KpiCalculatedDTO> toEnrich = data.stream()
                .filter(k -> k.getDefinition() == null || k.getDefinition().isBlank() ||
                        k.getCategorie() == null || k.getCategorie().isBlank() || "AUTO".equals(k.getCategorieCode()) ||
                        k.getUnite() == null || k.getUnite().isBlank() ||
                        k.getDirection() == null || k.getDirection().isBlank())
                .toList();

        if (toEnrich.isEmpty()) {
            return data;
        }

        log.info("Enriching metadata for {} KPIs using AI", toEnrich.size());
        String prompt = buildEnrichmentPrompt(toEnrich);

        try {
            String aiResponseJson = llmProviderChain.generate(prompt);

            if (aiResponseJson != null) {
                parseAndApplyMetadata(aiResponseJson, data);
            }
        } catch (Exception e) {
            log.error("Failed to enrich metadata using AI: {}", e.getMessage());

        }

        return data;
    }

    private String cleanGroqResponse(String rawResponse) {
        return rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }

    private void parseAndApplyMetadata(String json, List<KpiCalculatedDTO> data) {
        try {
            String cleaned = cleanGroqResponse(json);
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode items = root.isArray() ? root : root.path("items");
            if (items.isArray()) {
                for (JsonNode node : items) {
                    String name = node.path("name").asText();
                    String category = node.path("category").asText();
                    String definition = node.path("definition").asText();
                    String unite = node.path("unite").asText();
                    Double seuilFaible   = node.path("seuilFaible").isNull()   ? null : node.path("seuilFaible").asDouble();
                    Double seuilModere   = node.path("seuilModere").isNull()   ? null : node.path("seuilModere").asDouble();
                    Double seuilCritique = node.path("seuilCritique").isNull() ? null : node.path("seuilCritique").asDouble();
                    String direction = node.path("direction").isNull() ? null : node.path("direction").asText();
                    boolean validDirection = "HIGHER_IS_BETTER".equals(direction)
                            || "LOWER_IS_BETTER".equals(direction)
                            || "TARGET_IS_BEST".equals(direction);

                    data.stream()
                        .filter(k -> k.getKpiName().equalsIgnoreCase(name))
                        .forEach(k -> {
                            if (k.getDefinition() == null || k.getDefinition().isBlank()) k.setDefinition(definition);
                            if (k.getCategorie() == null || k.getCategorie().isBlank() || "AUTO".equals(k.getCategorieCode())) k.setCategorie(category);
                            if (k.getUnite() == null || k.getUnite().isBlank() || "ND".equals(k.getUnite())) k.setUnite(unite);
                            if (seuilFaible != null && seuilModere != null && seuilCritique != null
                                    && seuilFaible < seuilModere && seuilModere < seuilCritique) {
                                if (k.getSeuilFaible() == null)   k.setSeuilFaible(seuilFaible);
                                if (k.getSeuilModere() == null)   k.setSeuilModere(seuilModere);
                                if (k.getSeuilCritique() == null) k.setSeuilCritique(seuilCritique);
                            }
                            if (validDirection && (k.getDirection() == null || k.getDirection().isBlank())) {
                                k.setDirection(direction);
                            }
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error parsing AI metadata response: {}", e.getMessage());
        }
    }

    private String buildEnrichmentPrompt(List<KpiCalculatedDTO> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a QHSE expert. For the following KPI names, provide a business category, a professional QHSE definition, the exact appropriate unit of measurement (unite), and threshold values for QHSE variation monitoring.\n");
        sb.append("IMPORTANT: You MUST choose the unit ONLY from the following list based on the KPI type:\n");
        sb.append("- For PERCENTAGE KPIs: %, pts, ‰, ratio\n");
        sb.append("- For NUMBER KPIs: U, pcs, KWH, MWH, m³, L, T, KG, j, h, min, DT, €, $\n");
        sb.append("- For BOOLEAN KPIs (Yes/No, True/False): —\n");
        sb.append("KPIs: ");
        data.forEach(k -> sb.append(k.getKpiName()).append(", "));
        sb.append("\nRéponds UNIQUEMENT en français pour les champs category et definition.");
        sb.append("\nFor the threshold fields (seuilFaible, seuilModere, seuilCritique):");
        sb.append("\n- They are positive decimal numbers representing variation thresholds for this QHSE indicator.");
        sb.append("\n- seuilFaible < seuilModere < seuilCritique (strictly increasing).");
        sb.append("\n- For PERCENTAGE KPIs: typical values are 5/15/30 or 2/5/10.");
        sb.append("\n- For ABSOLUTE NUMBER KPIs: use values adapted to the QHSE context.");
        sb.append("\n- If uncertain, return null for all three threshold fields.");
        sb.append("\nFor the direction field:");
        sb.append("\n- Choose EXACTLY one of: HIGHER_IS_BETTER (improvement = increase, e.g. satisfaction rate),");
        sb.append(" LOWER_IS_BETTER (improvement = decrease, e.g. accident count),");
        sb.append(" TARGET_IS_BEST (improvement = reaching a target, e.g. target rate).");
        sb.append("\n- If uncertain, return null for direction.");
        sb.append("\nReturn ONLY a valid JSON object with exactly this structure: "
                + "{\"items\":[{\"name\":string,\"category\":string,\"definition\":string,\"unite\":string,"
                + "\"seuilFaible\":number|null,\"seuilModere\":number|null,\"seuilCritique\":number|null,"
                + "\"direction\":\"HIGHER_IS_BETTER|LOWER_IS_BETTER|TARGET_IS_BEST\"|null}]}. No other text.");
        return sb.toString();
    }
}
