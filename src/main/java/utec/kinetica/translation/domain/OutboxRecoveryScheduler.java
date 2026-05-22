package utec.kinetica.translation.domain;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxRecoveryScheduler {
    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxPayloadParser outboxPayloadParser;

    public OutboxRecoveryScheduler(
            OutboxEventRepository outboxEventRepository,
            ApplicationEventPublisher eventPublisher,
            OutboxPayloadParser outboxPayloadParser
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.outboxPayloadParser = outboxPayloadParser;
    }

    @Scheduled(fixedDelayString = "${app.outbox.recovery-delay-ms:15000}")
    @Transactional
    public void recoverPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pending) {
            Long requestId = outboxPayloadParser.extractRequestId(event.getPayload());
            if (requestId == null) {
                event.setStatus("FAILED");
                event.setRetryCount(safeIncrement(event.getRetryCount()));
                event.setLastError("Invalid outbox payload: missing requestId");
                event.setNextRetryAt(calculateNextRetryAt(event.getRetryCount()));
                outboxEventRepository.save(event);
                continue;
            }
            eventPublisher.publishEvent(new TranslationRequestedEvent(requestId, event.getId()));
        }

        List<OutboxEvent> retryableFailed = outboxEventRepository.findRetryableByStatus(
                "FAILED",
                Instant.now(),
                PageRequest.of(0, 50)
        );
        for (OutboxEvent event : retryableFailed) {
            Long requestId = outboxPayloadParser.extractRequestId(event.getPayload());
            if (requestId == null) {
                event.setRetryCount(safeIncrement(event.getRetryCount()));
                event.setLastError("Invalid outbox payload: missing requestId");
                event.setNextRetryAt(calculateNextRetryAt(event.getRetryCount()));
                outboxEventRepository.save(event);
                continue;
            }
            eventPublisher.publishEvent(new TranslationRequestedEvent(requestId, event.getId()));
        }
    }

    private int safeIncrement(Integer value) {
        return value == null ? 1 : value + 1;
    }

    private Instant calculateNextRetryAt(Integer retryCount) {
        int attempts = retryCount == null ? 1 : Math.max(retryCount, 1);
        long baseDelaySeconds = 15L;
        long factor = 1L << Math.min(attempts - 1, 6);
        return Instant.now().plusSeconds(baseDelaySeconds * factor);
    }

}
