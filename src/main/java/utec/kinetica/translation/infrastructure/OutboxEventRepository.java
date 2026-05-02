package utec.kinetica.translation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utec.kinetica.translation.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);

    @Query("""
            select e from OutboxEvent e
            where e.status = :status
              and e.retryCount < e.maxRetries
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            order by e.createdAt asc
            """)
    List<OutboxEvent> findRetryableByStatus(@Param("status") String status, @Param("now") Instant now, Pageable pageable);
}
