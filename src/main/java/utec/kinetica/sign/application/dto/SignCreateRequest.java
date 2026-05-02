package utec.kinetica.sign.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SignCreateRequest(
        @NotBlank String label,
        @NotBlank String mediaRef,
        @NotBlank String locale,
        Boolean active
) {
}
