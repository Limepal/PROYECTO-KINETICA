package utec.kinetica.auth.application.dto;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(
        @Email String email
) {
}
