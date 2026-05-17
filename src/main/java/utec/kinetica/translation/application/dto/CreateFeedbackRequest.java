package utec.kinetica.translation.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateFeedbackRequest(
        Long userId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String correctionText
) {
}
