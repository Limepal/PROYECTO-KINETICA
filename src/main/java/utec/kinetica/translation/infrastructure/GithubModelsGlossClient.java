package utec.kinetica.translation.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import utec.kinetica.translation.domain.GlossConversionClient;
import utec.kinetica.translation.domain.GlossConversionResult;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.gloss.provider", havingValue = "github")
public class GithubModelsGlossClient implements GlossConversionClient {
    private static final String SYSTEM_ES_TO_GLOSS = """
            Eres un asistente lingüístico especializado en conversión de español peruano a glosa funcional para Lengua de Señas Peruana (LSP) en contexto educativo/tecnológico.
            Objetivo: convertir texto en español a glosa intermedia consistente para sistemas computacionales.

            Reglas:
            1) Responde SOLO JSON válido, sin texto adicional.
            2) Usa MAYÚSCULAS en 'output_text'.
            3) Prioriza estructura semántica clara (agente, acción, objeto, tiempo, negación, pregunta).
            4) Reduce artículos y preposiciones no esenciales.
            5) Explicita deixis/pronombres cuando ayude a la claridad.
            6) Si hay término no mapeable, usa {D-A-C-T-I-L-O}.
            7) Si hay ambigüedad, conserva mejor opción en output_text y adicionales en alternatives.

            Esquema JSON:
            {
              "output_text": "...",
              "confidence": 0.0,
              "notes": ["..."],
              "alternatives": ["..."],
              "flags": ["..."]
            }
            """;

    private static final String SYSTEM_GLOSS_TO_ES = """
            Eres un asistente lingüístico especializado en reconversión de glosa LSP a español natural peruano.
            Objetivo: convertir glosa simplificada a español claro, conservando significado.

            Reglas:
            1) Responde SOLO JSON válido, sin texto adicional.
            2) Reconstruye conjugación, conectores y puntuación natural.
            3) Respeta negación, tiempo y modalidad interrogativa.
            4) Si hay ambigüedad, entrega una principal y variantes en alternatives.
            5) No sobreinterpretes ni inventes información.

            Esquema JSON:
            {
              "output_text": "...",
              "confidence": 0.0,
              "notes": ["..."],
              "alternatives": ["..."],
              "flags": ["..."]
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String modelId;

    public GithubModelsGlossClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.gloss.github.base-url:https://models.github.ai/inference}") String baseUrl,
            @Value("${app.gloss.github.model-id:openai/gpt-4o-mini}") String modelId,
            @Value("${app.gloss.github.token:${GITHUB_TOKEN:}}") String token
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.token = token == null ? "" : token;
        this.modelId = modelId;
    }

    @Override
    public GlossConversionResult spanishToGloss(String spanishText) {
        return invokeModel(SYSTEM_ES_TO_GLOSS, spanishText);
    }

    @Override
    public GlossConversionResult glossToSpanish(String glossText) {
        return invokeModel(SYSTEM_GLOSS_TO_ES, glossText);
    }

    private GlossConversionResult invokeModel(String systemPrompt, String inputText) {
        if (token.isBlank()) {
            return failed("Missing GITHUB_TOKEN");
        }
        try {
            ChatCompletionsResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(new ChatCompletionsRequest(
                            modelId,
                            List.of(new Message("system", systemPrompt), new Message("user", inputText)),
                            0.2
                    ))
                    .retrieve()
                    .body(ChatCompletionsResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return failed("Empty model response");
            }

            String content = response.choices().get(0).message().content();
            ParsedPayload payload = parsePayload(content, objectMapper);
            return new GlossConversionResult(
                    payload.outputText(),
                    payload.confidence(),
                    payload.notes(),
                    payload.alternatives(),
                    payload.flags(),
                    response.model() == null ? modelId : response.model()
            );
        } catch (Exception ex) {
            return failed(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    static ParsedPayload parsePayload(String modelContent, ObjectMapper objectMapper) throws Exception {
        String cleaned = stripMarkdownFence(modelContent).trim();
        JsonNode node = objectMapper.readTree(cleaned);
        String outputText = node.path("output_text").asText("");
        double confidence = node.path("confidence").asDouble(0.0);

        List<String> notes = toStringList(node.path("notes"));
        List<String> alternatives = toStringList(node.path("alternatives"));
        List<String> flags = toStringList(node.path("flags"));

        return new ParsedPayload(outputText, confidence, notes, alternatives, flags);
    }

    private static List<String> toStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private static String stripMarkdownFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        String withoutStart = trimmed.replaceFirst("^```[a-zA-Z]*", "").trim();
        if (withoutStart.endsWith("```")) {
            return withoutStart.substring(0, withoutStart.length() - 3).trim();
        }
        return withoutStart;
    }

    private GlossConversionResult failed(String message) {
        return new GlossConversionResult(
                "",
                0.0,
                List.of(),
                List.of(),
                List.of("MODEL_ERROR", message.toUpperCase(Locale.ROOT)),
                modelId
        );
    }

    record ParsedPayload(
            String outputText,
            double confidence,
            List<String> notes,
            List<String> alternatives,
            List<String> flags
    ) {
    }

    private record ChatCompletionsRequest(
            String model,
            List<Message> messages,
            double temperature
    ) {
    }

    private record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionsResponse(
            String model,
            List<Choice> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {
    }
}
