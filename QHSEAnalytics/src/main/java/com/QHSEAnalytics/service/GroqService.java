package com.QHSEAnalytics.service;

import com.QHSEAnalytics.entity.ResultatKpi;
import com.QHSEAnalytics.enums.NiveauVariation;
import com.QHSEAnalytics.enums.Tendance;
import com.QHSEAnalytics.exception.GroqApiException;
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
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key:}")
    private String apiKey;

    @Value("${app.groq.model:llama3-8b-8192}")
    private String model;

    @Value("${app.groq.timeout:30}")
    private int timeoutSeconds;

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
        String prompt = "Tu es un expert QHSE. Analyse le KPI suivant en restant factuel et professionnel :\n"
                + "- Nom KPI : " + kpiNom + "\n"
                + "- Définition : " + definition + "\n"
                + "- Unité : " + unite + "\n"
                + "- Catégorie : " + categorie + "\n"
                + "- Période N-1 : " + periodeN1 + "\n"
                + "- Valeur N-1 : " + valeurN1 + "\n"
                + "- Période N : " + periodeN + "\n"
                + "- Valeur N : " + valeurN + "\n"
                + "- Variation % : " + String.format(Locale.ROOT, "%.2f", variationRelative) + "\n"
                + "- Niveau : " + niveau + "\n"
                + "- Tendance : " + tendance + "\n"
                + "Rédige 3 à 4 phrases avec un constat, une interprétation et une recommandation.";

        try {
            return callGroqApi(prompt);
        } catch (GroqApiException ex) {
            log.warn("Analyse IA KPI indisponible pour {}", kpiNom);
            return "Analyse IA temporairement indisponible.";
        }
    }

    public String genererSyntheseGlobale(List<ResultatKpi> resultats, int periodeN1, int periodeN) {
        String details = resultats.stream()
            .map(r -> "- " + r.getKpi().getNom() + " : " + String.format(Locale.ROOT, "%.2f", r.getVariationRelative())
                        + "% (" + r.getNiveauVariation() + ")")
                .collect(Collectors.joining("\n"));

        String prompt = "Tu es un expert QHSE. Voici les résultats comparatifs N-1/N :\n"
                + details + "\n"
                + "Périodes : " + periodeN1 + " -> " + periodeN + "\n"
                + "Génère une synthèse globale de 5-6 phrases :\n"
                + "1. Bilan général\n"
                + "2. Points forts\n"
                + "3. Points critiques\n"
                + "4. Tendances émergentes\n"
                + "5. Plan d'actions prioritaires\n"
                + "Réponds en français, professionnel.";

        try {
            return callGroqApi(prompt);
        } catch (GroqApiException ex) {
            log.warn("Synthèse IA indisponible");
            return "Analyse IA temporairement indisponible.";
        }
    }

    private String callGroqApi(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GroqApiException("Analyse IA temporairement indisponible");
        }

        try {
            RestTemplate restTemplate = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String payload = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0.3)
                    .put("max_tokens", 500)
                    .set("messages", objectMapper.createArrayNode().add(
                            objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)
                    ))
                    .toString();

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            String body = restTemplate.exchange(
                    "https://api.groq.com/openai/v1/chat/completions",
                    HttpMethod.POST,
                    request,
                    String.class
            ).getBody();

            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new GroqApiException("Analyse IA temporairement indisponible");
            }
            return content.asText();
        } catch (RestClientException | IOException ex) {
            throw new GroqApiException("Analyse IA temporairement indisponible");
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(timeoutSeconds, 1) * 1000;
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return new RestTemplate(factory);
    }
}
