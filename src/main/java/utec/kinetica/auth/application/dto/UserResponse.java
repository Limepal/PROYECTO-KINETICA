package utec.kinetica.auth.application.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        Instant createdAt
) {
}
