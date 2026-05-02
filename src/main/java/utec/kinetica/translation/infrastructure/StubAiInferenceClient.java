package utec.kinetica.translation.infrastructure;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import utec.kinetica.translation.domain.AiInferenceClient;
import utec.kinetica.translation.domain.AiInferenceResponse;
import utec.kinetica.translation.domain.TranslationStatus;

@Component
@Profile({"default", "dev", "test"})
public class StubAiInferenceClient implements AiInferenceClient {
    @Override
    public AiInferenceResponse translateSignToText(Long requestId, String sourceText) {
        return new AiInferenceResponse(
                TranslationStatus.DONE,
                "Traducción simulada de seña a texto",
                null,
                0.78,
                180,
                "stub-v1",
                null
        );
    }

    @Override
    public AiInferenceResponse translateTextToSign(Long requestId, String sourceText) {
        double confidence = 0.62;
        return new AiInferenceResponse(
                TranslationStatus.DONE,
                null,
                "sign://catalog/basic-sequence",
                confidence,
                210,
                "stub-v1",
                confidence < 0.65 ? "LOW_CONFIDENCE" : null
        );
    }
}
