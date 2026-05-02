package com.QHSEAnalytics.service.processing;

import com.QHSEAnalytics.dto.ollama.AiResponse;
import com.QHSEAnalytics.dto.ollama.KpiInsight;
import com.QHSEAnalytics.dto.ollama.OllamaRequest;
import com.QHSEAnalytics.dto.ollama.OllamaResponse;
import com.QHSEAnalytics.dto.response.KpiCalculatedDTO;
import com.QHSEAnalytics.exception.AnalyseGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service client pour intégration avec Ollama avec retry et fallback.
 * Gère les appels API, parsing JSON, et stratégie de fallback avec retry.
 */
@Service
@Slf4j
public class OllamaClientService {

    private static final String GENERATE_PATH = "/api/generate";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 180_000;
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final String UNAVAILABLE_SUMMARY = "IA indisponible";
    private static final String UNAVAILABLE_RECOMMENDATION = "Analyse IA indisponible. Veuillez réessayer ultérieurement.";
    private static final String OLLAMA_SYSTEM_PROMPT = """
You are a senior QHSE data analyst AI expert in ISO 9001, ISO 14001, and ISO 45001.

Analyze the provided KPI data and return ONLY a valid JSON.
Your analysis must consider the business definitions and categories provided for each KPI.

STRICT RULES:
* Return ONLY JSON
* No explanation, no markdown, no comments
* Overall score is from 0 to 100

EXPECTED FORMAT:
{
  "overallScore": number,
  "summary": "Professional summary of performance and ISO compliance",
  "kpis": [
    {
      "name": "KPI name",
      "score": number,
      "insight": "Business analysis based on variation and definition"
    }
  ],
  "recommendations": ["Actionable advice 1", "Actionable advice 2"]
}

DATA:
{KPI_DATA}
""";

    private final RestTemplate restTemplate;
    private final String endpointUrl;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String fallbackModel;

    /**
     * Constructeur du service Ollama avec configuration des timeouts.
     */
    public OllamaClientService(RestTemplateBuilder restTemplateBuilder,
                                ObjectMapper objectMapper,
                                @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
                                @Value("${ollama.model:llama3\\:latest}") String model,
                                @Value("${ollama.fallback-model:mistral\\:latest}") String fallbackModel,
                                @Value("${ollama.connect-timeout-ms:" + DEFAULT_CONNECT_TIMEOUT_MS + "}") int connectTimeoutMs,
                                @Value("${ollama.read-timeout-ms:" + DEFAULT_READ_TIMEOUT_MS + "}") int readTimeoutMs) {
        this.endpointUrl = normalizeEndpoint(ollamaUrl) + GENERATE_PATH;
        this.model = Objects.requireNonNull(model, "Le nom du modèle Ollama ne peut pas être nul.");
        this.fallbackModel = Objects.requireNonNull(fallbackModel, "Le nom du modèle de fallback Ollama ne peut pas être nul.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper ne peut pas être nul.");
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Génère un texte d'analyse QHSE pour une liste de KPI calculés.
     * Si Ollama échoue, une exception explicite est levée.
     */
    public String generateKpiAnalysis(List<KpiCalculatedDTO> kpiData) {
        return toJson(generateKpiAnalysisObject(kpiData));
    }

    /**
     * Génère une analyse IA à partir d'un prompt déjà construit.
     * Utilise le système de retry existant.
     */
    public String generateWithPrompt(String prompt) {
        return toJson(generateWithPromptObject(prompt));
    }

    /**
     * Génère une réponse OllamaResponse à partir d'un prompt.
     * Retourne une réponse indisponible en cas d'erreur.
     */
    public OllamaResponse generateWithPromptObject(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return buildUnavailableResponse("Prompt vide.");
        }
        try {
            return retryOperation(prompt, OllamaResponse.class);
        } catch (AnalyseGenerationException ex) {
            log.warn("Analyse IA indisponible : {}", ex.getMessage());
            return buildUnavailableResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("Erreur inattendue Ollama : {}", ex.getMessage(), ex);
            return buildUnavailableResponse("Erreur de communication avec Ollama.");
        }
    }

    /**
     * Génère une réponse AiResponse stricte à partir d'un prompt.
     * Retourne une réponse indisponible en cas d'erreur.
     */
    public AiResponse generateStrictAiResponseObject(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return buildUnavailableAiResponse("Prompt vide.");
        }
        try {
            return retryOperation(prompt, AiResponse.class);
        } catch (AnalyseGenerationException ex) {
            log.warn("Analyse IA indisponible : {}", ex.getMessage());
            return buildUnavailableAiResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("Erreur inattendue Ollama : {}", ex.getMessage(), ex);
            return buildUnavailableAiResponse("Erreur de communication avec Ollama.");
        }
    }

    /**
     * Génère une analyse KPI en OllamaResponse.
     * Construit le prompt KPI et effectue l'appel avec retry.
     */
    public OllamaResponse generateKpiAnalysisObject(List<KpiCalculatedDTO> kpiData) {
        if (kpiData == null || kpiData.isEmpty()) {
            return buildUnavailableResponse("Aucune donnée KPI disponible pour l'analyse IA.");
        }

        String prompt = buildPromptKpiAnalysis(kpiData);
        try {
            return retryOperation(prompt, OllamaResponse.class);
        } catch (AnalyseGenerationException ex) {
            log.warn("Analyse IA indisponible : {}", ex.getMessage());
            return buildUnavailableResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de l'appel Ollama : {}", ex.getMessage(), ex);
            return buildUnavailableResponse("Erreur de communication avec Ollama.");
        }
    }

    // ==================== CONSOLIDATED RETRY LOGIC ====================

    /**
     * Exécute une opération avec retry générique pour OllamaResponse et AiResponse.
     * Gère le fallback vers le modèle alternatif et les tentatives de retry.
     *
     * @param prompt le prompt à envoyer
     * @param responseType le type de réponse attendue (OllamaResponse.class ou AiResponse.class)
     * @return l'objet de réponse parsé
     * @throws AnalyseGenerationException si toutes les tentatives échouent
     */
    @SuppressWarnings("unchecked")
    private <T> T retryOperation(String prompt, Class<T> responseType) {
           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.APPLICATION_JSON);
           String strictRetryPrompt = prompt + "\n\nReturn ONLY valid JSON. Your previous response was invalid.";
           try {
               return attemptRetry(prompt, model, responseType, headers, strictRetryPrompt);
           } catch (HttpStatusCodeException ex) {
               if (ex.getStatusCode().value() == 404 && isFallbackAvailable()) {
                   log.warn("Modèle {} introuvable (404), bascule vers {}.", model, fallbackModel);
                   return attemptRetry(prompt, fallbackModel, responseType, headers, strictRetryPrompt);
               }
               throw new AnalyseGenerationException("Analyse IA échouée : modèle Ollama introuvable pour " + model + ".", ex);
           }
       }

       /**
        * Effectue les tentatives de retry avec un modèle donné.
        */
       @SuppressWarnings("unchecked")
       private <T> T attemptRetry(String prompt, String selectedModel, Class<T> responseType, 
                                  HttpHeaders headers, String strictRetryPrompt) {
        int attempt = 1;
        while (attempt <= MAX_RETRY_ATTEMPTS) {
            try {
                String currentPrompt = attempt == 2 ? strictRetryPrompt : prompt;
                String rawResponse = callOllamaApi(currentPrompt, selectedModel, headers);
                return parseJsonResponse(rawResponse, responseType);
            } catch (AnalyseGenerationException ex) {
                if (attempt == MAX_RETRY_ATTEMPTS || !"Invalid AI response structure".equals(ex.getMessage())) {
                    throw ex;
                }
                log.warn("Ollama returned invalid JSON on attempt {}. Retrying with stricter prompt.", attempt);
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().value() == 404 || attempt == MAX_RETRY_ATTEMPTS) {
                    throw ex;
                }
                log.warn("Erreur HTTP Ollama (tentative {}/{}): {}. Nouvelle tentative...",
                        attempt, MAX_RETRY_ATTEMPTS, ex.getStatusCode());
            } catch (ResourceAccessException ex) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new AnalyseGenerationException(isTimeoutException(ex.getCause())
                            ? "Analyse IA échouée : délai d'attente Ollama dépassé."
                            : "Analyse IA échouée : impossible de contacter Ollama.", ex);
                }
                log.warn("Impossible de contacter Ollama (tentative {}/{}). Nouvelle tentative... Cause: {}",
                        attempt, MAX_RETRY_ATTEMPTS, ex.getMessage());
            }
            attempt++;
        }
        throw new AnalyseGenerationException("Analyse IA échouée : échec de toutes les tentatives de connexion à Ollama.");
    }

    // ==================== CONSOLIDATED JSON PARSING ====================

    /**
     * Parse une réponse JSON brute en objet de type spécifié.
     * Gère extraction du champ "response", parsing JSON et validation.
     *
     * @param rawBody la réponse brute d'Ollama
     * @param responseType le type de réponse attendue
     * @return l'objet parsé
     * @throws AnalyseGenerationException si le parsing échoue
     */
    @SuppressWarnings("unchecked")
    private <T> T parseJsonResponse(String rawBody, Class<T> responseType) {
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("AI response is empty or blank.");
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        log.debug("Raw response from Ollama: {}", rawBody);
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            log.warn("Échec du parsing du wrapper Ollama.", ex);
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        if (root == null || !root.has("response") || !root.get("response").isTextual()) {
            log.warn("Ollama wrapper response field is missing or invalid.");
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        String responseField = root.get("response").asText();
        log.debug("Ollama response field extracted: {}", responseField);

        String extracted = extractJsonObject(responseField);
        log.debug("Extracted JSON from response field: {}", extracted);

        if (extracted == null || extracted.isBlank()) {
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(extracted);
        } catch (Exception ex) {
            log.warn("Échec du parsing du JSON extrait.", ex);
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        if (!isValidStrictResponsePayload(payload)) {
            log.warn("AI response failed strict validation.");
            throw new AnalyseGenerationException("Invalid AI response structure");
        }

        log.debug("AI response payload passed strict validation.");
        log.debug("Final parsed JSON payload: {}", payload.toString());

        // Map JSON to appropriate response type
        T response;
        if (responseType == OllamaResponse.class) {
            response = (T) mapJsonNodeToResponse(payload);
        } else if (responseType == AiResponse.class) {
            response = (T) mapJsonNodeToAiResponse(payload);
        } else {
            throw new AnalyseGenerationException("Unsupported response type: " + responseType.getSimpleName());
        }

        if (response == null) {
            throw new AnalyseGenerationException("Invalid AI response structure");
        }
        return response;
    }

    /**
     * Extrait un objet JSON valide d'une chaîne brute (nettoie markdown).
     */
    private String extractJsonObject(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String cleaned = rawText.trim()
                .replace("```json", "")
                .replace("```", "")
                .replace("`{", "{")
                .replace("}`", "}")
                .trim();

        int firstOpen = cleaned.indexOf('{');
        int lastClose = cleaned.lastIndexOf('}');
        if (firstOpen < 0 || lastClose <= firstOpen) {
            return null;
        }

        return cleaned.substring(firstOpen, lastClose + 1);
    }

    // ==================== HTTP CALL ====================

    /**
     * Effectue un appel HTTP à l'API Ollama.
     */
    private String callOllamaApi(String prompt, String selectedModel, HttpHeaders headers) {
        OllamaRequest request = OllamaRequest.builder()
                .model(selectedModel)
                .prompt(prompt)
                .stream(false)
                .format("json")
                .build();

        HttpEntity<OllamaRequest> requestEntity = new HttpEntity<>(request, headers);
        log.info("Envoi d'une requête à Ollama : {}, modèle={}, promptLength={}", endpointUrl, selectedModel, prompt.length());

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                endpointUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        String rawBody = responseEntity.getBody();
        log.info("Réponse Ollama reçue : status={}, bodyLength={}", responseEntity.getStatusCode().value(), rawBody == null ? 0 : rawBody.length());
        log.debug("Raw Ollama response body length {}", rawBody == null ? 0 : rawBody.length());
        log.debug("Raw Ollama response body: {}", rawBody);

        if (rawBody == null || rawBody.isBlank()) {
            throw new AnalyseGenerationException("Analyse IA échouée : réponse vide de Ollama.");
        }

        return rawBody;
    }

    // ==================== RESPONSE VALIDATION ====================

    /**
     * Valide si la charge JSON répond aux critères stricts.
     * Accepte deux formats : ancien (recommendation) et nouveau (kpis/recommendations).
     */
    private boolean isValidStrictResponsePayload(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }

        Set<String> requiredOldKeys = Set.of("overallScore", "summary", "recommendation");
        Set<String> requiredNewKeys = Set.of("overallScore", "summary", "kpis", "recommendations");
        Set<String> actualKeys = new java.util.HashSet<>();
        root.fieldNames().forEachRemaining(actualKeys::add);

        if (!(actualKeys.equals(requiredOldKeys) || actualKeys.equals(requiredNewKeys))) {
            log.warn("AI response payload contains invalid or extra keys: {}", actualKeys);
            return false;
        }

        if (!root.get("overallScore").isNumber()) {
            return false;
        }
        if (!root.get("summary").isTextual()) {
            return false;
        }

        if (actualKeys.equals(requiredOldKeys)) {
            return root.get("recommendation").isTextual();
        }

        if (!root.get("recommendations").isArray()) {
            return false;
        }
        if (!root.get("kpis").isArray()) {
            return false;
        }

        for (JsonNode item : root.get("kpis")) {
            if (item == null || !item.isObject()) {
                return false;
            }
            if (!item.has("name") || !item.has("score") || !item.has("insight")) {
                return false;
            }
            if (!item.get("name").isTextual() || !item.get("score").isNumber() || !item.get("insight").isTextual()) {
                return false;
            }
        }

        return true;
    }

    // ==================== RESPONSE MAPPING ====================

    /**
     * Map un JsonNode en OllamaResponse avec rétrocompatibilité.
     */
    private OllamaResponse mapJsonNodeToResponse(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }

        OllamaResponse response = new OllamaResponse();
        response.setOverallScore(root.has("overallScore") && root.get("overallScore").isNumber() ? root.get("overallScore").asDouble() : null);
        response.setGlobalSummary(getTextValue(root, "summary"));

        if (root.has("recommendation")) {
            response.setRecommendations(getStringList(root.get("recommendation")));
        } else {
            response.setRecommendations(getStringList(root.get("recommendations")));
        }

        response.setCriticalKPIs(Collections.emptyList());
        response.setCategories(Collections.emptyList());
        response.setKpisAnalyses(Collections.emptyList());

        // Rétrocompatibilité avec anciens consommateurs d'analyses
        response.setInterpretation(getTextValue(root, "summary"));
        response.setKpisCritiques(Collections.emptyList());
        response.setCauses(Collections.emptyList());
        response.setRecommandations(Collections.emptyList());
        response.setPlanAction(Collections.emptyList());
        return response;
    }

    /**
     * Map un JsonNode en AiResponse.
     */
    private AiResponse mapJsonNodeToAiResponse(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }

        AiResponse response = new AiResponse();
        response.setOverallScore(root.has("overallScore") && root.get("overallScore").isNumber() ? root.get("overallScore").asDouble() : 0.0);
        response.setSummary(getTextValue(root, "summary"));
        response.setRecommendations(getStringList(root.get("recommendations")));

        List<KpiInsight> kpiInsights = new java.util.ArrayList<>();
        if (root.has("kpis") && root.get("kpis").isArray()) {
            root.get("kpis").forEach(item -> {
                if (item != null && item.isObject()) {
                    KpiInsight insight = new KpiInsight();
                    insight.setName(getTextValue(item, "name"));
                    insight.setScore(item.has("score") && item.get("score").isNumber() ? item.get("score").asDouble() : 0.0);
                    insight.setInsight(getTextValue(item, "insight"));
                    kpiInsights.add(insight);
                }
            });
        }
        response.setKpis(kpiInsights);
        return response;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Convertit un JsonNode en liste de chaînes.
     */
    private List<String> getStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }

        if (node.isArray()) {
            List<String> values = new java.util.ArrayList<>();
            node.forEach(item -> values.add(nodeItemAsString(item)));
            return values;
        }

        if (node.isTextual()) {
            String text = node.asText();
            return text.isBlank() ? Collections.emptyList() : Collections.singletonList(text.trim());
        }

        return Collections.singletonList(node.toString());
    }

    /**
     * Convertit un item JsonNode en chaîne.
     */
    private String nodeItemAsString(JsonNode item) {
        if (item == null || item.isNull()) {
            return "";
        }
        if (item.isTextual()) {
            return item.asText();
        }
        return item.toString();
    }

    /**
     * Récupère la valeur texte d'un champ dans un JsonNode.
     */
    private String getTextValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Sérialise une OllamaResponse en JSON.
     */
    private String toJson(OllamaResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            log.error("Impossible de sérialiser la réponse Ollama en JSON.", ex);
            return "{\"overallScore\":0.0,\"summary\":\"Erreur interne de sérialisation.\",\"recommendation\":\"Erreur interne de sérialisation.\"}";
        }
    }

    /**
     * Construit un prompt d'analyse KPI à partir de données KPI.
     */
    private String buildPromptKpiAnalysis(List<KpiCalculatedDTO> kpiData) {
        try {
            StringBuilder prompt = new StringBuilder();
            kpiData.forEach(kpi -> prompt.append(String.format(
                "KPI: %s | Categorie: %s | Definition: %s | N-1=%s | N=%s | Δ=%s%% | Classification=%s\n",
                safe(kpi.getKpiName()),
                safe(kpi.getCategorie()),
                safe(kpi.getDefinition()),
                safeNumber(kpi.getValeurN1()),
                safeNumber(kpi.getValeurN()),
                safeNumber(kpi.getVariationPercentage()),
                safe(kpi.getClassification())
            )));
            return OLLAMA_SYSTEM_PROMPT.replace("{KPI_DATA}", prompt.toString());
        } catch (Exception ex) {
            log.warn("Impossible de sérialiser les KPI pour le prompt Ollama.", ex);
            return OLLAMA_SYSTEM_PROMPT.replace("{KPI_DATA}", "[]");
        }
    }

    /**
     * Construit une réponse indisponible en cas d'erreur Ollama.
     */
    private OllamaResponse buildUnavailableResponse(String reason) {
        String message = UNAVAILABLE_RECOMMENDATION;
        if (reason != null && !reason.isBlank() && !reason.contains(UNAVAILABLE_SUMMARY)) {
            message = String.format("%s (%s)", UNAVAILABLE_RECOMMENDATION, reason);
        }
        return OllamaResponse.builder()
                .overallScore(0.0)
                .globalSummary(UNAVAILABLE_SUMMARY)
                .recommendations(Collections.singletonList(message))
                .criticalKPIs(Collections.emptyList())
                .categories(Collections.emptyList())
                .kpisAnalyses(Collections.emptyList())
                .build();
    }

    /**
     * Construit une réponse AiResponse indisponible en cas d'erreur Ollama.
     */
    private AiResponse buildUnavailableAiResponse(String reason) {
        String message = UNAVAILABLE_RECOMMENDATION;
        if (reason != null && !reason.isBlank() && !reason.contains(UNAVAILABLE_SUMMARY)) {
            message = String.format("%s (%s)", UNAVAILABLE_RECOMMENDATION, reason);
        }
        AiResponse response = new AiResponse();
        response.setOverallScore(0.0);
        response.setSummary(UNAVAILABLE_SUMMARY);
        response.setRecommendations(Collections.singletonList(message));
        response.setKpis(Collections.emptyList());
        return response;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Vérifie si le fallback est disponible et différent du modèle principal.
     */
    private boolean isFallbackAvailable() {
        return fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equals(model);
    }

    /**
     * Vérifie si une exception est causée par un timeout de socket.
     */
    private boolean isTimeoutException(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Ramène une chaîne à une valeur sûre (jamais null ou blank).
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeNumber(Number value) {
        if (value == null) {
            return "0";
        }
        double d = value.doubleValue();
        return d == Math.floor(d) && !Double.isInfinite(d)
                ? String.valueOf((long) d)
                : String.format("%.2f", d);
    }

    /**
     * Normalise une URL de base Ollama en supprimant les slashes finales.
     */
    private static String normalizeEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("L'URL de base Ollama ne peut pas être vide.");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
