package utec.kinetica.auth.application.dto;

public record AuthResponse(
        Long userId,
        String email,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
