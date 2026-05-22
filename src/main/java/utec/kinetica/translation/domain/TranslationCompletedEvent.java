package utec.kinetica.translation.domain;

public record TranslationCompletedEvent(Long requestId, Long outboxId) {
}
