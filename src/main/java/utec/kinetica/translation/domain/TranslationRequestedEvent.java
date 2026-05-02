package utec.kinetica.translation.domain;

public record TranslationRequestedEvent(Long requestId, Long outboxId) {
}
