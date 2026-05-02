package utec.kinetica.translation.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import utec.kinetica.translation.domain.TranslationDirection;

public record CreateTranslationRequest(
        @NotNull TranslationDirection direction,
        @NotBlank String sourceText
) {
}
