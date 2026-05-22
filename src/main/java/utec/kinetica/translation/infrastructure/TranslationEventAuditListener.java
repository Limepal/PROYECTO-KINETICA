package utec.kinetica.translation.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import utec.kinetica.translation.domain.MediaAssetUploadedEvent;
import utec.kinetica.translation.domain.TranslationCompletedEvent;
import utec.kinetica.translation.domain.TranslationFailedEvent;

@Component
public class TranslationEventAuditListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationEventAuditListener.class);

    @Async("appTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationCompleted(TranslationCompletedEvent event) {
        LOGGER.info("Translation completed for requestId={} outboxId={}", event.requestId(), event.outboxId());
    }

    @Async("appTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationFailed(TranslationFailedEvent event) {
        LOGGER.warn("Translation failed for requestId={} outboxId={} error={}", event.requestId(), event.outboxId(), event.error());
    }

    @Async("appTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMediaAssetUploaded(MediaAssetUploadedEvent event) {
        LOGGER.info("Media asset uploaded for requestId={} mediaAssetId={}", event.requestId(), event.mediaAssetId());
    }
}
