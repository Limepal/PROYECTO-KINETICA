package utec.kinetica.translation.domain;

public record TranslationFailedEvent(Long requestId, Long outboxId, String error) {
}
