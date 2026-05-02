package utec.kinetica.translation.domain;

public record AiInferenceResponse(
        TranslationStatus status,
        String textOutput,
        String signOutputRef,
        double confidence,
        long latencyMs,
        String modelVersion,
        String warning
) {
}
