package utec.kinetica.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull Long userId,
        @NotBlank String roleName
) {
}
