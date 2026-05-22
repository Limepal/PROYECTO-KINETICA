package utec.kinetica.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank String roleName
) {
}
