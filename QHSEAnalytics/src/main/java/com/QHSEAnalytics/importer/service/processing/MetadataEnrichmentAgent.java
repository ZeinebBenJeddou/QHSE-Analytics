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
                        k.getUnite() == null || k.getUnite().isBlank())
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

                    data.stream()
                        .filter(k -> k.getKpiName().equalsIgnoreCase(name))
                        .forEach(k -> {
                            if (k.getDefinition() == null || k.getDefinition().isBlank()) k.setDefinition(definition);
                            if (k.getCategorie() == null || k.getCategorie().isBlank() || "AUTO".equals(k.getCategorieCode())) k.setCategorie(category);
                            if (k.getUnite() == null || k.getUnite().isBlank() || "ND".equals(k.getUnite())) k.setUnite(unite);
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error parsing AI metadata response: {}", e.getMessage());
        }
    }

    private String buildEnrichmentPrompt(List<KpiCalculatedDTO> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a QHSE expert. For the following KPI names, provide a business category, a professional QHSE definition, and the exact appropriate unit of measurement (unite).\n");
        sb.append("IMPORTANT: You MUST choose the unit ONLY from the following list based on the KPI type:\n");
        sb.append("- For PERCENTAGE KPIs: %, pts, ‰, ratio\n");
        sb.append("- For NUMBER KPIs: U, pcs, KWH, MWH, m³, L, T, KG, j, h, min, DT, €, $\n");
        sb.append("- For BOOLEAN KPIs (Yes/No, True/False): —\n");
        sb.append("KPIs: ");
        data.forEach(k -> sb.append(k.getKpiName()).append(", "));
        sb.append("\nRéponds UNIQUEMENT en français pour les champs category et definition.");
        sb.append("\nReturn ONLY a valid JSON object with exactly this structure: {\"items\":[{\"name\":string,\"category\":string,\"definition\":string,\"unite\":string}]}. No other text.");
        return sb.toString();
    }
}
