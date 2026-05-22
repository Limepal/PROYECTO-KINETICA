package utec.kinetica.auth.domain;

public record UserRegisteredEvent(Long userId, String email) {
}
