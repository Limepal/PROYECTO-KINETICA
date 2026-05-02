package utec.kinetica.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utec.kinetica.auth.domain.RefreshToken;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash and rt.revokedAt is null")
    Optional<RefreshToken> findActiveTokenForUpdate(@Param("tokenHash") String tokenHash);
}
