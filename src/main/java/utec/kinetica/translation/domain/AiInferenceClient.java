package utec.kinetica.translation.domain;

public interface AiInferenceClient {
    AiInferenceResponse translateSignToText(Long requestId, String sourceText);
    AiInferenceResponse translateTextToSign(Long requestId, String sourceText);
}
