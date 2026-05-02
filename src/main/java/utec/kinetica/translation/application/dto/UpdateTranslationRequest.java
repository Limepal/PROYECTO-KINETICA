package utec.kinetica.translation.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTranslationRequest(
        @NotBlank String sourceText
) {
}
