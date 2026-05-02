package utec.kinetica.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank String name
) {
}
