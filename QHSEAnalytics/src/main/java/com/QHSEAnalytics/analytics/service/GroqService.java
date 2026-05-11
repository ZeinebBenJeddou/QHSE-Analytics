package com.QHSEAnalytics.analytics.service;

import com.QHSEAnalytics.shared.entity.ResultatKpi;
import com.QHSEAnalytics.shared.enums.NiveauVariation;
import com.QHSEAnalytics.shared.enums.Tendance;
import com.QHSEAnalytics.shared.exception.ProviderUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    public static final String FALLBACK_MESSAGE = "Analyse IA temporairement indisponible.";
    private static final String PROVIDER_NAME = "groq";
    private static final int MAX_TOKENS = 2800;

    private final ObjectMapper objectMapper;
    private final GroqPromptBuilder groqPromptBuilder;
    private final GroqKeyRotator groqKeyRotator;
    private final ProviderCooldownManager cooldownManager;
    private final AiConfigService aiConfigService;

    @Value("${app.groq.models:}")
    private String models;

    @Value("${app.groq.timeout:30}")
    private int timeoutSeconds;

    @Value("${app.groq.temperature.json:0.1}")
    private double temperatureJson;

    @Value("${app.groq.temperature.text:0.5}")
    private double temperatureText;

    public String generate(String prompt) {
        String response = appelerJson(prompt, MAX_TOKENS);
        if (response == null || response.isBlank() || FALLBACK_MESSAGE.equals(response)) {
            throw new ProviderUnavailableException(PROVIDER_NAME, "No Groq model/key combination succeeded");
        }
        return response;
    }

    public String appeler(String prompt, int maxTokens) {
        try {
            return appelerInternal(prompt, maxTokens, false);
        } catch (ProviderUnavailableException ex) {
            return FALLBACK_MESSAGE;
        }
    }

    public String appelerJson(String prompt, int maxTokens) {
        try {
            return appelerInternal(prompt, maxTokens, true);
        } catch (ProviderUnavailableException ex) {
            return FALLBACK_MESSAGE;
        }
    }

    private String appelerInternal(String prompt, int maxTokens, boolean jsonMode) {
        if (!cooldownManager.isAvailable(PROVIDER_NAME)) {
            throw new ProviderUnavailableException(PROVIDER_NAME, "provider on cooldown");
        }

        List<String> candidateModels = resolveCandidateModels();
        if (candidateModels.isEmpty()) {
            throw new ProviderUnavailableException(PROVIDER_NAME, "no Groq models configured");
        }

        RestTemplate restTemplate = buildRestTemplate();
        int attempts = Math.max(1, groqKeyRotator.keyCount());

        for (String candidateModel : candidateModels) {
            for (int attempt = 0; attempt < attempts; attempt++) {
                String apiKey = groqKeyRotator.next();
                try {
                    String response = callGroqModel(restTemplate, apiKey, candidateModel, prompt, maxTokens, jsonMode);
                    if (response != null && !response.isBlank()) {
                        log.info("Groq response received with model {}", candidateModel);
                        cooldownManager.clearCooldown(PROVIDER_NAME);
                        return response;
                    }
                } catch (HttpStatusCodeException ex) {
                    if (ex.getStatusCode() == HttpStatusCode.valueOf(429)) {
                        long retryAfterSeconds = extractRetryAfterSeconds(ex);
                        cooldownManager.setCooldown(PROVIDER_NAME, retryAfterSeconds > 0 ? retryAfterSeconds : 60L);
                        log.warn("Groq key hit 429 on model {}. Rotating key.", candidateModel);
                        continue;
                    }
                    log.warn("Groq model {} unavailable: {}", candidateModel, ex.getMessage());
                } catch (IOException ex) {
                    log.warn("Groq model {} returned invalid content: {}", candidateModel, ex.getMessage());
                }
            }
        }

        throw new ProviderUnavailableException(PROVIDER_NAME, "all Groq keys/models exhausted");
    }

    private String callGroqModel(
            RestTemplate restTemplate,
            String apiKey,
            String candidateModel,
            String prompt,
            int maxTokens,
            boolean jsonMode
    ) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        double temperature = jsonMode
                ? aiConfigService.getDouble("groq.temperature.json", temperatureJson)
                : aiConfigService.getDouble("groq.temperature.text", temperatureText);
        com.fasterxml.jackson.databind.node.ObjectNode payloadNode = objectMapper.createObjectNode()
                .put("model", candidateModel)
                .put("temperature", temperature)
                .put("max_tokens", maxTokens)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", jsonMode
                                        ? "Tu réponds uniquement avec du JSON valide. Aucun texte avant ou après, aucun markdown, aucune explication, aucun bloc de code."
                                        : "Tu es un assistant QHSE professionnel."))
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", prompt)));

        if (jsonMode) {
            payloadNode.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        }

        HttpEntity<String> request = new HttpEntity<>(payloadNode.toString(), headers);
        String responseBody = restTemplate.exchange(
                "https://api.groq.com/openai/v1/chat/completions",
                HttpMethod.POST,
                request,
                String.class
        ).getBody();

        JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
            throw new IOException("Réponse Groq invalide");
        }

        return content.asText();
    }

    private long extractRetryAfterSeconds(HttpStatusCodeException ex) {
        if (ex.getResponseHeaders() != null) {
            String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
            if (retryAfter != null && !retryAfter.isBlank()) {
                try {
                    return Long.parseLong(retryAfter.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String body = ex.getResponseBodyAsString();
        if (!body.isBlank()) {
            Matcher matcher = Pattern.compile("(?i)retry[- ]after[^0-9]{0,20}(\\d+)").matcher(body);
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return -1;
    }

    private List<String> resolveCandidateModels() {
        return Arrays.stream(models == null ? new String[0] : models.split(","))
                .map(String::trim)
                .filter(modelName -> !modelName.isBlank())
                .toList();
    }

    public String analyserKpi(String prompt) {
        return appeler(prompt, 700);
    }

    public String analyserCategorie(String prompt) {
        return appeler(prompt, 900);
    }

    public String genererSynthese(String prompt) {
        return appeler(prompt, 1200);
    }

    public String genererPlanActions(String prompt) {
        return appeler(prompt, 1400);
    }

    public String analyserKpi(
            String kpiNom,
            String definition,
            String unite,
            String categorie,
            double valeurN1,
            double valeurN,
            double variationRelative,
            NiveauVariation niveau,
            Tendance tendance,
            int periodeN1,
            int periodeN
    ) {
        String prompt = groqPromptBuilder.buildKpiPrompt(
                kpiNom,
                definition,
                unite,
                categorie,
                valeurN1,
                valeurN,
                variationRelative,
                niveau,
                tendance,
                periodeN1,
                periodeN,
                null
        );
        return analyserKpi(prompt);
    }

    public String genererSyntheseGlobale(List<ResultatKpi> resultats, int periodeN1, int periodeN) {
        String prompt = groqPromptBuilder.buildSynthesePrompt(resultats, periodeN1, periodeN);
        return genererSynthese(prompt);
    }

    public String genererAnalyseCategorie(String categorieLibelle, List<ResultatKpi> resultats, int periodeN1, int periodeN) {
        String prompt = groqPromptBuilder.buildCategoriePrompt(categorieLibelle, resultats, periodeN1, periodeN);
        return analyserCategorie(prompt);
    }

    public String genererPlanActions(List<ResultatKpi> resultats, int periodeN1, int periodeN) {
        String prompt = groqPromptBuilder.buildPlanActionsPrompt(resultats, periodeN1, periodeN);
        return genererPlanActions(prompt);
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int effectiveTimeout = aiConfigService.getInt("groq.timeout.seconds", timeoutSeconds);
        int timeoutMillis = Math.max(effectiveTimeout, 1) * 1000;
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }
}
