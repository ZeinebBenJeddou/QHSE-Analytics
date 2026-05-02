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
        // Use JdkClientHttpRequestFactory which handles modern TLS better than the default HttpURLConnection
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory = 
            new org.springframework.http.client.JdkClientHttpRequestFactory();
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateRaw(String prompt) {
        if (!isConfigured()) {
            return null;
        }
        
        int maxRetries = 2; // Try up to 2 times
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = baseUrl + model + ":generateContent?key=" + apiKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("User-Agent", "QHSEAnalytics-Backend/1.0"); // Add User-Agent

                com.fasterxml.jackson.databind.node.ObjectNode payloadNode = objectMapper.createObjectNode();
                payloadNode.set("contents", objectMapper.createArrayNode().add(
                        objectMapper.createObjectNode()
                                .set("parts", objectMapper.createArrayNode().add(
                                        objectMapper.createObjectNode().put("text", prompt)))));
                payloadNode.set("generationConfig", objectMapper.createObjectNode()
                        .put("response_mime_type", "application/json"));

                HttpEntity<String> entity = new HttpEntity<>(payloadNode.toString(), headers);
                String responseBody = restTemplate.postForObject(url, entity, String.class);

                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                if (attempt == maxRetries) {
                    log.error("Gemini API rate limit exceeded after retries.", e);
                    return null;
                }
                log.warn("Gemini API rate limit hit (429). Waiting 15s before retry (Attempt {}/{})...", attempt, maxRetries);
                try {
                    Thread.sleep(15000); // Wait 15 seconds as suggested by API
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                log.error("Error calling Gemini API (raw): {}", e.getMessage(), e); // Print stack trace too
                return null;
            }
        }
        return null;
    }

    public AiResponse generateAnalysis(String prompt) {
        String json = generateRaw(prompt);
        return json != null ? parseGeminiResponse(json) : null;
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
