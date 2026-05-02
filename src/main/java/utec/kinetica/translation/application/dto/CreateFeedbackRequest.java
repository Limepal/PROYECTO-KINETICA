package utec.kinetica.translation.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateFeedbackRequest(
        Long userId,
        @Min(1) @Max(5) Integer rating,
        String correctionText
) {
}
