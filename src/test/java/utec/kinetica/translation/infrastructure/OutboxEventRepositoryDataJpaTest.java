package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import utec.kinetica.support.PostgresContainerSupport;
import utec.kinetica.translation.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxEventRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldReturnRetryableEventsWhenStatusAndTimeWindowMatch() {
        OutboxEvent retryable = new OutboxEvent();
        retryable.setEventType("TRANSLATION_REQUESTED");
        retryable.setPayload("{\"requestId\":1}");
        retryable.setStatus("FAILED");
        retryable.setRetryCount(1);
        retryable.setMaxRetries(5);
        retryable.setNextRetryAt(Instant.now().minusSeconds(5));
        outboxEventRepository.save(retryable);

        OutboxEvent nonRetryable = new OutboxEvent();
        nonRetryable.setEventType("TRANSLATION_REQUESTED");
        nonRetryable.setPayload("{\"requestId\":2}");
        nonRetryable.setStatus("FAILED");
        nonRetryable.setRetryCount(5);
        nonRetryable.setMaxRetries(5);
        nonRetryable.setNextRetryAt(Instant.now().minusSeconds(5));
        outboxEventRepository.save(nonRetryable);

        List<OutboxEvent> results = outboxEventRepository.findRetryableByStatus(
                "FAILED",
                Instant.now(),
                PageRequest.of(0, 10)
        );

        assertEquals(1, results.size());
        assertEquals(retryable.getId(), results.get(0).getId());
    }

    @Test
    void shouldReturnPendingEventsOrderedByCreatedAtWhenFindingTop50ByStatus() {
        OutboxEvent first = new OutboxEvent();
        first.setEventType("TRANSLATION_REQUESTED");
        first.setPayload("{\"requestId\":11}");
        first.setStatus("PENDING");
        first.setRetryCount(0);
        first.setMaxRetries(3);
        outboxEventRepository.save(first);

        OutboxEvent second = new OutboxEvent();
        second.setEventType("TRANSLATION_REQUESTED");
        second.setPayload("{\"requestId\":12}");
        second.setStatus("PENDING");
        second.setRetryCount(0);
        second.setMaxRetries(3);
        outboxEventRepository.save(second);

        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        assertEquals(2, pending.size());
    }
}
