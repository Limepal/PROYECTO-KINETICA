package utec.kinetica.translation.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import utec.kinetica.translation.domain.AiInferenceClient;
import utec.kinetica.translation.domain.AiInferenceResponse;
import utec.kinetica.translation.domain.TranslationStatus;

@Component
@Profile("prod")
public class HttpAiInferenceClient implements AiInferenceClient {
    private final RestClient restClient;

    public HttpAiInferenceClient(
            RestClient.Builder builder,
            @Value("${app.ai.base-url:http://localhost:8000}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public AiInferenceResponse translateSignToText(Long requestId, String sourceText) {
        return invokeWithSingleRetry("/infer/sign-to-text", requestId, sourceText);
    }

    @Override
    public AiInferenceResponse translateTextToSign(Long requestId, String sourceText) {
        return invokeWithSingleRetry("/infer/text-to-sign", requestId, sourceText);
    }

    private AiInferenceResponse invokeWithSingleRetry(String path, Long requestId, String sourceText) {
        int attempts = 0;
        while (attempts < 2) {
            try {
                AiHttpResponse response = restClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AiHttpRequest(requestId, sourceText))
                        .retrieve()
                        .body(AiHttpResponse.class);

                if (response == null) {
                    throw new RestClientException("AI service empty response");
                }

                return new AiInferenceResponse(
                        TranslationStatus.valueOf(response.status()),
                        response.textOutput(),
                        response.signOutputRef(),
                        response.confidence(),
                        response.latencyMs(),
                        response.modelVersion(),
                        response.warning()
                );
            } catch (Exception ex) {
                attempts++;
                if (attempts >= 2) {
                    return new AiInferenceResponse(
                            TranslationStatus.FAILED,
                            null,
                            null,
                            0.0,
                            0,
                            "http-client",
                            "AI_SERVICE_ERROR"
                    );
                }
            }
        }
        return new AiInferenceResponse(TranslationStatus.FAILED, null, null, 0.0, 0, "http-client", "AI_SERVICE_ERROR");
    }

    private record AiHttpRequest(Long requestId, String sourceText) {
    }

    private record AiHttpResponse(
            String status,
            String textOutput,
            String signOutputRef,
            double confidence,
            long latencyMs,
            String modelVersion,
            String warning
    ) {
    }
}
