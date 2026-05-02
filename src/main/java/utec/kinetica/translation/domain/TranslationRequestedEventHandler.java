package utec.kinetica.translation.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.time.Instant;

@Component
public class TranslationRequestedEventHandler {
    private final TranslationRequestRepository requestRepository;
    private final TranslationResultRepository resultRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AiInferenceClient aiInferenceClient;

    public TranslationRequestedEventHandler(
            TranslationRequestRepository requestRepository,
            TranslationResultRepository resultRepository,
            OutboxEventRepository outboxEventRepository,
            AiInferenceClient aiInferenceClient
    ) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.aiInferenceClient = aiInferenceClient;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationRequested(TranslationRequestedEvent event) {
        outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
            outbox.setStatus("PROCESSING");
            outbox.setLastError(null);
            outboxEventRepository.save(outbox);
        });
        try {
            TranslationRequest request = requestRepository.findById(event.requestId())
                    .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + event.requestId()));

            AiInferenceResponse response = request.getDirection() == TranslationDirection.SIGN_TO_TEXT
                    ? aiInferenceClient.translateSignToText(request.getId(), request.getSourceText())
                    : aiInferenceClient.translateTextToSign(request.getId(), request.getSourceText());

            TranslationResult result = new TranslationResult();
            result.setRequest(request);
            result.setTextOutput(response.textOutput());
            result.setSignOutputRef(response.signOutputRef());
            result.setConfidence(response.confidence());
            result.setLatencyMs(response.latencyMs());
            result.setModelVersion(response.modelVersion());
            result.setWarning(response.warning());
            resultRepository.save(result);

            request.setStatus(response.status());
            requestRepository.save(request);

            outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
                outbox.setStatus("PROCESSED");
                outbox.setNextRetryAt(null);
                outbox.setLastError(null);
                outboxEventRepository.save(outbox);
            });
        } catch (Exception ex) {
            outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
                int retryCount = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount();
                retryCount++;
                outbox.setStatus("FAILED");
                outbox.setRetryCount(retryCount);
                outbox.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                outbox.setNextRetryAt(calculateNextRetryAt(retryCount));
                outboxEventRepository.save(outbox);
            });
            throw ex;
        }
    }

    private Instant calculateNextRetryAt(int retryCount) {
        long baseDelaySeconds = 15L;
        long factor = 1L << Math.min(Math.max(retryCount - 1, 0), 6);
        return Instant.now().plusSeconds(baseDelaySeconds * factor);
    }
}
