package utec.kinetica.auth.domain;

public record AuthTokens(
        Long userId,
        String email,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
