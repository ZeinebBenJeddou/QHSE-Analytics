package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.Tendance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private static final String FALLBACK_MESSAGE = "Analyse IA temporairement indisponible.";

    private final ObjectMapper objectMapper;
    private final GroqPromptBuilder groqPromptBuilder;

    @Value("${app.groq.api-key:}")
    private String apiKey;

    @Value("${app.groq.model:llama3-8b-8192}")
    private String model;

    @Value("${app.groq.timeout:30}")
    private int timeoutSeconds;

    public String appeler(String prompt, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return FALLBACK_MESSAGE;
        }

        try {
            RestTemplate restTemplate = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String payload = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0.3d)
                    .put("max_tokens", maxTokens)
                    .set("messages", objectMapper.createArrayNode().add(
                            objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)
                    ))
                    .toString();

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
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
        } catch (RestClientException | IOException ex) {
            log.warn("Groq indisponible : {}", ex.getMessage());
            return FALLBACK_MESSAGE;
        }
    }

    public String analyserKpi(String prompt) {
        return appeler(prompt, 400);
    }

    public String analyserCategorie(String prompt) {
        return appeler(prompt, 600);
    }

    public String genererSynthese(String prompt) {
        return appeler(prompt, 800);
    }

    public String genererPlanActions(String prompt) {
        return appeler(prompt, 1000);
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
                periodeN
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
        int timeoutMillis = Math.max(timeoutSeconds, 1) * 1000;
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }
}