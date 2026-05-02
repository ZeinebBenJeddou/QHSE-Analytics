package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.ollama.AiResponse;
import com.QHSEAnalytics.dto.ollama.KpiInsight;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GeminiClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-3-flash-preview}")
    private String model;

    @Value("${app.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String baseUrl;

    public GeminiClientService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public AiResponse generateAnalysis(String prompt) {
        if (!isConfigured()) {
            log.warn("Gemini API key is not configured.");
            return null;
        }

        try {
            String url = baseUrl + model + ":generateContent?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gemini Request Structure
            com.fasterxml.jackson.databind.node.ObjectNode payloadNode = objectMapper.createObjectNode();
            payloadNode.set("contents", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode()
                            .set("parts", objectMapper.createArrayNode().add(
                                    objectMapper.createObjectNode().put("text", prompt)))));
            payloadNode.set("generationConfig", objectMapper.createObjectNode()
                    .put("response_mime_type", "application/json"));

            String payload = payloadNode.toString();

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            String responseBody = restTemplate.postForObject(url, entity, String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return parseGeminiResponse(candidateText);
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return null;
        }
    }

    private AiResponse parseGeminiResponse(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(jsonText);

            AiResponse response = new AiResponse();
            response.setOverallScore(root.path("overallScore").asDouble(0.0));
            response.setSummary(root.path("summary").asText("Analyse indisponible"));

            List<String> recs = new ArrayList<>();
            root.path("recommendations").forEach(n -> recs.add(n.asText()));
            response.setRecommendations(recs);

            List<KpiInsight> kpis = new ArrayList<>();
            root.path("kpis").forEach(n -> {
                KpiInsight insight = new KpiInsight();
                insight.setName(n.path("name").asText());
                insight.setScore(n.path("score").asDouble(0.0));
                insight.setInsight(n.path("insight").asText());
                kpis.add(insight);
            });
            response.setKpis(kpis);

            return response;
        } catch (Exception e) {
            log.error("Error parsing Gemini JSON response: {}", e.getMessage());
            return null;
        }
    }
}
