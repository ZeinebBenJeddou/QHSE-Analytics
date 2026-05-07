// java
package com.QHSEAnalytics.analytics.service.processing;

import com.QHSEAnalytics.shared.dto.llm.AiResponse;
import com.QHSEAnalytics.shared.dto.llm.KpiInsight;
import com.QHSEAnalytics.analytics.service.ProviderCooldownManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProviderCooldownManager cooldownManager;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-3-flash-preview}")
    private String model;

    @Value("${app.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String baseUrl;

    @Value("${app.gemini.max-retries:5}")
    private int maxRetries;

    @Value("${app.gemini.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    public GeminiClientService(ObjectMapper objectMapper, ProviderCooldownManager cooldownManager) {
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory();
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = objectMapper;
        this.cooldownManager = cooldownManager;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateRaw(String prompt) {
        if (!isConfigured()) {
            return null;
        }

        if (!cooldownManager.isAvailable("gemini")) {
            return null;
        }

        long backoff = initialBackoffMs;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = baseUrl + model + ":generateContent?key=" + apiKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("User-Agent", "QHSEAnalytics-Backend/1.0");

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
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    long waitSeconds = extractRetryDelaySeconds(e);
                    if (waitSeconds <= 0) {
                        waitSeconds = 60L;
                    }
                    cooldownManager.setCooldown("gemini", waitSeconds);
                    log.warn("Gemini API 429 (attempt {}/{}). Cooling down for {}s.", attempt, maxRetries, waitSeconds);
                    return null;
                }
                // non-429 client error -> log and abort
                log.error("Gemini API client error: {} - {}", e.getStatusCode(), e.getStatusText());
                return null;
            } catch (Exception e) {
                log.error("Error calling Gemini API (raw): {}", e.getMessage());
                if (attempt == maxRetries) return null;
                log.warn("Transient error, backing off {} ms (attempt {}/{})", backoff, attempt, maxRetries);
                sleep(backoff);
                backoff = Math.min(backoff * 2, 60_000L);
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

    private long extractRetryDelaySeconds(HttpClientErrorException e) {
        // 1) Retry-After header (seconds)
        if (e.getResponseHeaders() != null) {
            String ra = e.getResponseHeaders().getFirst("Retry-After");
            if (StringUtils.hasText(ra)) {
                try {
                    return Long.parseLong(ra.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 2) Parse JSON body for error.details[*].retryDelay or error.retryDelay
        String body = e.getResponseBodyAsString();
        if (StringUtils.hasText(body)) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode details = root.path("error").path("details");
                if (details.isArray()) {
                    for (JsonNode d : details) {
                        if (d.has("retryDelay")) {
                            long seconds = parseDurationToSeconds(d.path("retryDelay").asText());
                            if (seconds > 0) return seconds;
                        }
                    }
                }
                if (root.path("error").has("retryDelay")) {
                    long seconds = parseDurationToSeconds(root.path("error").path("retryDelay").asText());
                    if (seconds > 0) return seconds;
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private long parseDurationToSeconds(String s) {
        if (!StringUtils.hasText(s)) return -1;
        String v = s.trim().toLowerCase();
        try {
            if (v.endsWith("ms")) {
                return Math.max(1L, Long.parseLong(v.substring(0, v.length() - 2)) / 1000L);
            } else if (v.endsWith("s")) {
                return Long.parseLong(v.substring(0, v.length() - 1));
            } else if (v.endsWith("m")) {
                return Long.parseLong(v.substring(0, v.length() - 1)) * 60L;
            } else {
                return Long.parseLong(v);
            }
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}