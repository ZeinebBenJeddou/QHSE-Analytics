package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataEnrichmentAgent {

    private final GeminiClientService geminiClientService;
    private final OllamaClientService ollamaClientService;
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
            String aiResponseJson = null;
            if (geminiClientService.isConfigured()) {
                aiResponseJson = callGeminiForMetadata(prompt);
            } else {
                aiResponseJson = ollamaClientService.generateWithPrompt(prompt);
            }

            if (aiResponseJson != null) {
                parseAndApplyMetadata(aiResponseJson, data);
            }
        } catch (Exception e) {
            log.error("Failed to enrich metadata using AI: {}", e.getMessage());
            // Fallback: keep existing or set defaults
        }
        
        return data;
    }

    private String callGeminiForMetadata(String prompt) {
        return geminiClientService.generateRaw(prompt);
    }

    private void parseAndApplyMetadata(String json, List<KpiCalculatedDTO> data) {
        try {
            if (json != null) {
                int start = json.indexOf('[');
                int end = json.lastIndexOf(']');
                if (start >= 0 && end >= start) {
                    json = json.substring(start, end + 1);
                }
            }
            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                for (JsonNode node : root) {
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
        sb.append("\nReturn ONLY a valid JSON array of objects with keys: \"name\", \"category\", \"definition\", \"unite\". No other text.");
        return sb.toString();
    }
}
