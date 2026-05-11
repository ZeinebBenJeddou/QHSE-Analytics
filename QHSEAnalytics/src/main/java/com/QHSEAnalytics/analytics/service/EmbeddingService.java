package com.QHSEAnalytics.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ProviderCooldownManager cooldownManager;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String baseUrl;

    @Value("${app.gemini.embedding-model:gemini-embedding-exp-03-07}")
    private String embeddingModel;

    private static final String PROVIDER_KEY = "gemini-embedding";
    private static final int    MAX_RETRIES     = 3;
    private static final long   INITIAL_BACKOFF = 1_000L;

    private static final int    MAX_TEXT_CHARS  = 6_000;

    public EmbeddingService(ObjectMapper objectMapper, ProviderCooldownManager cooldownManager) {
        this.objectMapper    = objectMapper;
        this.cooldownManager = cooldownManager;
        this.restTemplate    = new RestTemplate(new SimpleClientHttpRequestFactory());
    }

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            log.info("[Embedding] No Gemini API key — embedding disabled, RAG will use keyword fallback.");
            return;
        }
        CompletableFuture.runAsync(this::discoverEmbeddingModel);
    }

    private void discoverEmbeddingModel() {
        try {
            URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey);
            ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), String.class);

            JsonNode models = objectMapper.readTree(resp.getBody()).path("models");
            List<String> available = new ArrayList<>();
            if (models.isArray()) {
                for (JsonNode m : models) {
                    JsonNode methods = m.path("supportedGenerationMethods");
                    if (methods.isArray()) {
                        for (JsonNode method : methods) {
                            if ("embedContent".equals(method.asText())) {
                                available.add(m.path("name").asText()); // e.g. "models/text-embedding-004"
                                break;
                            }
                        }
                    }
                }
            }

            if (available.isEmpty()) {
                log.warn("[Embedding] No models supporting embedContent found for this API key. RAG will use keyword fallback.");
                return;
            }

            log.info("[Embedding] Models supporting embedContent for this key: {}", available);

            String configuredPath = "models/" + embeddingModel;
            boolean configured = available.stream().anyMatch(configuredPath::equals);
            if (!configured) {
                String autoModel = available.get(0).replace("models/", "");
                log.warn("[Embedding] Configured model '{}' not available. Auto-switching to '{}'.",
                        embeddingModel, autoModel);
                embeddingModel = autoModel;
            } else {
                log.info("[Embedding] Configured model '{}' is available.", embeddingModel);
            }

        } catch (Exception e) {
            log.warn("[Embedding] Could not query ListModels: {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }


    @Cacheable(value = "embeddings", unless = "#result == null")
    public float[] embed(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return null;
        }
        if (!cooldownManager.isAvailable(PROVIDER_KEY)) {
            log.debug("[Embedding] Provider on cooldown — skipping.");
            return null;
        }


        String truncated = text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;

        long backoff = INITIAL_BACKOFF;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                float[] result = callApi(truncated);
                if (result != null) return result;

            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    long waitSeconds = extractRetryAfter(ex);
                    if (waitSeconds <= 0) waitSeconds = 60L;
                    cooldownManager.setCooldown(PROVIDER_KEY, waitSeconds);
                    log.warn("[Embedding] 429 rate-limit — cooldown {}s.", waitSeconds);
                    return null;
                }
                log.warn("[Embedding] HTTP error (attempt {}/{}): {} {} — body: {}", attempt, MAX_RETRIES,
                        ex.getStatusCode(), ex.getStatusText(), ex.getResponseBodyAsString());
                if (attempt == MAX_RETRIES) return null;

            } catch (Exception ex) {
                log.warn("[Embedding] Error (attempt {}/{}): {}", attempt, MAX_RETRIES, ex.getMessage());
                if (attempt == MAX_RETRIES) return null;
            }

            sleep(backoff);
            backoff = Math.min(backoff * 2, 30_000L);
        }
        return null;
    }

    private float[] callApi(String text) throws Exception {

        URI uri = URI.create(baseUrl + embeddingModel + ":embedContent?key=" + apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        String body = String.format(
            "{\"model\":\"models/%s\",\"content\":{\"parts\":[{\"text\":%s}]},\"outputDimensionality\":768}",
            embeddingModel,
            objectMapper.writeValueAsString(text)
        );

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        String responseBody = response.getBody();

        JsonNode values = objectMapper.readTree(responseBody).path("embedding").path("values");
        if (!values.isArray() || values.size() == 0) {
            log.warn("[Embedding] Empty 'values' array in response.");
            return null;
        }

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = (float) values.get(i).asDouble();
        }
        return vector;
    }

    public static String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) return null;
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            sj.add(Float.toString(v));
        }
        return sj.toString();
    }

    private long extractRetryAfter(HttpClientErrorException ex) {
        try {
            if (ex.getResponseHeaders() != null) {
                String ra = ex.getResponseHeaders().getFirst("Retry-After");
                if (ra != null) return Long.parseLong(ra.trim());
            }
        } catch (NumberFormatException parseException) {
            log.warn("[Embedding] Invalid Retry-After header: {}", parseException.getMessage());
        }
        return -1L;
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
