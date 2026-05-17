package utec.kinetica.translation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.translation.domain.MediaAsset;

import java.time.Instant;
import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findByRequestId(Long requestId);
    List<MediaAsset> findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant now);
}
