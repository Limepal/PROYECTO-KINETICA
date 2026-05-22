package utec.kinetica.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.RefreshToken;
import utec.kinetica.auth.domain.User;
import utec.kinetica.support.PostgresContainerSupport;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void shouldFindActiveTokenForUpdateWhenTokenIsNotRevoked() {
        User user = new User();
        user.setEmail("refresh-repo@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("token-hash-1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(token);

        assertTrue(refreshTokenRepository.findActiveTokenForUpdate("token-hash-1").isPresent());
    }

    @Test
    void shouldReturnEmptyWhenFindingActiveTokenForUpdateAndTokenIsRevoked() {
        User user = new User();
        user.setEmail("refresh-revoked@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("token-hash-2");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        assertTrue(refreshTokenRepository.findActiveTokenForUpdate("token-hash-2").isEmpty());
        assertTrue(refreshTokenRepository.findByTokenHash("token-hash-2").isPresent());
    }
}
