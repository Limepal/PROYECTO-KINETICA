package utec.kinetica.translation.application.dto;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long requestId,
        Long userId,
        Integer rating,
        String correctionText,
        Instant createdAt
) {
}
