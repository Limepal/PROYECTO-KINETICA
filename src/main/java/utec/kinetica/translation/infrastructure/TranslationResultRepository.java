package utec.kinetica.translation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.translation.domain.TranslationResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TranslationResultRepository extends JpaRepository<TranslationResult, Long> {
    Optional<TranslationResult> findByRequestId(Long requestId);
    List<TranslationResult> findByRequestIdIn(List<Long> requestIds);
    List<TranslationResult> findByCreatedAtAfter(Instant from);
}
