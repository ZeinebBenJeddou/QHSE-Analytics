# OllamaClientService.java - Refactored File Structure

## File Organization

```
PACKAGE & IMPORTS (Lines 1-30)
├── Package: com.QHSEAnalytics.service.processing
├── 11 Java imports
├── 8 Spring framework imports
└── 3 Jackson/utility imports

CLASS DEFINITION & CONSTANTS (Lines 31-77)
├── @Service @Slf4j annotations
├── Static final constants:
│   ├── GENERATE_PATH = "/api/generate"
│   ├── MAX_RETRY_ATTEMPTS = 2
│   ├── DEFAULT_CONNECT_TIMEOUT_MS = 60_000
│   ├── DEFAULT_READ_TIMEOUT_MS = 180_000
│   ├── UNAVAILABLE_SUMMARY
│   ├── UNAVAILABLE_RECOMMENDATION
│   └── OLLAMA_SYSTEM_PROMPT (multiline string)
└── Instance fields:
    ├── restTemplate
    ├── endpointUrl
    ├── objectMapper
    ├── model
    └── fallbackModel

CONSTRUCTOR (Lines 78-95)
├── RestTemplateBuilder injection
├── ObjectMapper injection
├── Configuration properties:
│   ├── ollama.url
│   ├── ollama.model
│   ├── ollama.fallback-model
│   ├── ollama.connect-timeout-ms
│   └── ollama.read-timeout-ms
├── Endpoint normalization
└── RestTemplate configuration

PUBLIC METHODS (Lines 96-169)
├── generateKpiAnalysis(List<KpiCalculatedDTO>)
│   └── Returns: String (JSON serialized response)
├── generateWithPrompt(String)
│   └── Returns: String (JSON serialized response)
├── generateWithPromptObject(String)
│   └── Returns: OllamaResponse
│   └── Uses: retryOperation<OllamaResponse>()
├── generateStrictAiResponseObject(String)
│   └── Returns: AiResponse
│   └── Uses: retryOperation<AiResponse>()
└── generateKpiAnalysisObject(List<KpiCalculatedDTO>)
    └── Returns: OllamaResponse
    └── Uses: retryOperation<OllamaResponse>()

CONSOLIDATED RETRY LOGIC (Lines 170-239)
├── retryOperation<T>(String, Class<T>)
│   ├── Handles fallback model logic
│   ├── Catches 404 errors
│   └── Dispatches to attemptRetry<T>()
├── attemptRetry<T>(String, String, Class<T>, HttpHeaders, String)
│   ├── MAX_RETRY_ATTEMPTS loop (up to 2)
│   ├── Second attempt with strict prompt
│   ├── Exception handling:
│   │   ├── AnalyseGenerationException → rethrow or retry
│   │   ├── HttpStatusCodeException → rethrow or retry
│   │   └── ResourceAccessException → timeout detection
│   └── Returns: <T> response object

CONSOLIDATED JSON PARSING (Lines 240-315)
├── parseJsonResponse<T>(String, Class<T>)
│   ├── Null/blank validation
│   ├── Wrapper extraction (get "response" field)
│   ├── JSON object extraction from markdown
│   ├── Payload parsing and validation
│   ├── Type-specific mapping:
│   │   ├── if OllamaResponse.class → mapJsonNodeToResponse()
│   │   └── if AiResponse.class → mapJsonNodeToAiResponse()
│   └── Returns: <T> response object
└── extractJsonObject(String)
    ├── Markdown cleanup (```, ```json, backticks)
    ├── JSON boundary detection
    └── Returns: extracted JSON string

HTTP CALL (Lines 316-340)
└── callOllamaApi(String, String, HttpHeaders)
    ├── Request building (OllamaRequest.builder())
    ├── HTTP POST exchange
    ├── Response validation (not null/blank)
    └── Returns: raw response body string

RESPONSE VALIDATION (Lines 341-380)
└── isValidStrictResponsePayload(JsonNode)
    ├── Object type check
    ├── Support for 2 formats:
    │   ├── Old: {overallScore, summary, recommendation}
    │   └── New: {overallScore, summary, kpis, recommendations}
    ├── Type validation for each field
    ├── Array validation for kpis/recommendations
    └── Returns: boolean

RESPONSE MAPPING (Lines 381-460)
├── mapJsonNodeToResponse(JsonNode) → OllamaResponse
│   ├── Extract overallScore, summary
│   ├── Handle both recommendation formats
│   ├── Set empty collections for backward compatibility
│   └── Returns: OllamaResponse
└── mapJsonNodeToAiResponse(JsonNode) → AiResponse
    ├── Extract overallScore, summary
    ├── Parse kpis array → List<KpiInsight>
    ├── Extract recommendations
    └── Returns: AiResponse

HELPER METHODS (Lines 461-550)
├── getStringList(JsonNode)
│   ├── Handles null, array, text, object
│   └── Returns: List<String>
├── nodeItemAsString(JsonNode)
│   ├── Safe conversion to string
│   └── Returns: String
├── getTextValue(JsonNode, String)
│   ├── Extract field with fallback to empty string
│   └── Returns: String
├── toJson(OllamaResponse)
│   ├── ObjectMapper serialization
│   ├── Error fallback JSON
│   └── Returns: String
├── buildPromptKpiAnalysis(List<KpiCalculatedDTO>)
│   ├── Serialize KPIs to JSON
│   ├── Inject into system prompt template
│   └── Returns: String
├── buildUnavailableResponse(String)
│   ├── Create error OllamaResponse
│   └── Returns: OllamaResponse
└── buildUnavailableAiResponse(String)
    ├── Create error AiResponse
    └── Returns: AiResponse

UTILITY METHODS (Lines 551-565)
├── isFallbackAvailable()
│   ├── Check fallback model configured and different
│   └── Returns: boolean
├── isTimeoutException(Throwable)
│   ├── Walk exception chain for SocketTimeoutException
│   └── Returns: boolean
├── safe(String)
│   ├── Null-safe string handler
│   └── Returns: String (or "-")
└── normalizeEndpoint(String)
    ├── Remove trailing slash from URL
    └── Returns: String
```

---

## Method Call Flow

### Scenario 1: Successful KPI Analysis
```
generateKpiAnalysisObject(kpiData)
├── buildPromptKpiAnalysis(kpiData) → prompt
├── retryOperation(prompt, OllamaResponse.class)
│   ├── attemptRetry(prompt, model, ...)
│   │   ├── callOllamaApi(...) → rawResponse
│   │   └── parseJsonResponse(rawResponse, OllamaResponse.class)
│   │       ├── extract "response" field
│   │       ├── extractJsonObject() → json string
│   │       ├── objectMapper.readTree() → JsonNode
│   │       ├── isValidStrictResponsePayload() → true
│   │       ├── mapJsonNodeToResponse(node) → OllamaResponse
│   │       └── return OllamaResponse
│   └── return OllamaResponse
└── normalizeResponse() [if needed]
```

### Scenario 2: JSON Parsing Failure, Retry with Strict Prompt
```
attemptRetry(prompt, model, ...) [Attempt 1]
├── callOllamaApi(prompt, model, ...) → rawResponse
├── parseJsonResponse(rawResponse, OllamaResponse.class)
│   └── AnalyseGenerationException("Invalid AI response structure")
├── Log warning: "Ollama returned invalid JSON on attempt 1"
│
attemptRetry(prompt, model, ...) [Attempt 2]
├── currentPrompt = prompt + "\n\nReturn ONLY valid JSON..."
├── callOllamaApi(currentPrompt, model, ...) → rawResponse
├── parseJsonResponse(rawResponse, OllamaResponse.class)
│   └── return OllamaResponse [SUCCESS]
└── return OllamaResponse
```

### Scenario 3: Model Not Found, Fallback to Alternative
```
retryOperation(prompt, OllamaResponse.class)
├── attemptRetry(prompt, model, ...) → HttpStatusCodeException(404)
├── Catch 404 & isFallbackAvailable() == true
├── Log: "Model X not found, switching to Y"
├── attemptRetry(prompt, fallbackModel, ...)
│   └── return OllamaResponse [from fallback model]
└── return OllamaResponse
```

### Scenario 4: All Attempts Failed
```
attemptRetry(prompt, model, ...) [Attempt 1]
├── Exception caught → log warning
├── attempt++
│
attemptRetry(prompt, model, ...) [Attempt 2]
├── Exception caught (attempt == MAX_RETRY_ATTEMPTS)
├── throw AnalyseGenerationException
│
Public method catches exception
└── return buildUnavailableResponse(reason)
```

---

## Type Dispatch Pattern

The refactored code uses a **generic type dispatch** pattern:

```java
// Calling code specifies desired response type
private <T> T retryOperation(String prompt, Class<T> responseType)

// Public methods use different classes
public OllamaResponse generateWithPromptObject(String prompt) {
    return retryOperation(prompt, OllamaResponse.class);
}

public AiResponse generateStrictAiResponseObject(String prompt) {
    return retryOperation(prompt, AiResponse.class);
}

// Parsing layer checks type and dispatches
private <T> T parseJsonResponse(String rawBody, Class<T> responseType) {
    // ... extraction and validation ...
    
    if (responseType == OllamaResponse.class) {
        response = (T) mapJsonNodeToResponse(payload);
    } else if (responseType == AiResponse.class) {
        response = (T) mapJsonNodeToAiResponse(payload);
    }
    
    return response;
}
```

This allows **single method** to handle **multiple response types** without duplication.

---

## Error Handling Strategy

### Exception Hierarchy:
```
Exception
├── HttpStatusCodeException (4xx/5xx HTTP errors)
│   └── 404 → attempt fallback model
├── ResourceAccessException (network/timeout errors)
│   └── Check timeout, throw with appropriate message
└── AnalyseGenerationException (JSON parsing failures)
    └── Retry with strict prompt on first attempt
```

### Retry Strategy:
```
Attempt 1: prompt as-is
Attempt 2: prompt + strict JSON instruction
Attempt 3: Give up, throw exception

Result: Public method catches → buildUnavailable...Response()
```

---

## Configuration Properties

The service reads from `application.properties`:

```properties
# Primary Ollama model
ollama.model=llama3:latest

# Fallback model (used if primary returns 404)
ollama.fallback-model=mistral:latest

# Ollama API URL
ollama.url=http://localhost:11434

# HTTP connection timeout
ollama.connect-timeout-ms=60000

# HTTP read timeout
ollama.read-timeout-ms=180000
```

---

## Performance Characteristics

- **First Attempt Success:** Direct response + minimal logging
- **Retry (JSON failure):** +1 additional API call + warn log
- **Fallback (404):** +1 or 2 additional API calls (depends on failure point)
- **Complete Failure:** 2 attempts × 2 models = up to 4 API calls
- **Network Timeout Detection:** Exception chain inspection

---

## Extensibility Points

To add new response type:

1. Create new response DTO class
2. Create mapping method: `mapJsonNodeToNewType(JsonNode)`
3. Call existing methods with new class:
   ```java
   retryOperation(prompt, NewType.class)
   ```
4. Parsing layer automatically dispatches via type check

**No retry or parsing code changes needed!**
