package utec.kinetica.translation.application.dto;

import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationStatus;

import java.time.Instant;

public record TranslationResponse(
        Long requestId,
        TranslationStatus status,
        TranslationDirection direction,
        String textOutput,
        String glossOutput,
        String signOutputRef,
        Double confidence,
        String warning,
        String modelVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
