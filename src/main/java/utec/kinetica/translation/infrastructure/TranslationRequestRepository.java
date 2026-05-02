package utec.kinetica.translation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.translation.domain.TranslationRequest;

import java.util.List;
import java.util.Optional;

public interface TranslationRequestRepository extends JpaRepository<TranslationRequest, Long> {
    List<TranslationRequest> findByUserId(Long userId);
    Optional<TranslationRequest> findByIdAndUserId(Long id, Long userId);
}
