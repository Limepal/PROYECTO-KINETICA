package utec.kinetica.translation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TranslationRequestRepository extends JpaRepository<TranslationRequest, Long> {
    List<TranslationRequest> findByUserId(Long userId);
    Optional<TranslationRequest> findByIdAndUserId(Long id, Long userId);
    List<TranslationRequest> findByCreatedAtAfter(Instant from);
    long countByCreatedAtAfter(Instant from);
    long countByCreatedAtAfterAndStatus(Instant from, TranslationStatus status);
}
