package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import utec.kinetica.translation.domain.MediaAssetUploadedEvent;
import utec.kinetica.translation.domain.TranslationCompletedEvent;
import utec.kinetica.translation.domain.TranslationFailedEvent;

class TranslationEventAuditListenerTest {

    private final TranslationEventAuditListener listener = new TranslationEventAuditListener();

    @Test
    void shouldHandleTranslationCompletedEventWithoutThrowing() {
        listener.onTranslationCompleted(new TranslationCompletedEvent(10L, 20L));
    }

    @Test
    void shouldHandleTranslationFailedEventWithoutThrowing() {
        listener.onTranslationFailed(new TranslationFailedEvent(11L, 21L, "timeout"));
    }

    @Test
    void shouldHandleMediaAssetUploadedEventWithoutThrowing() {
        listener.onMediaAssetUploaded(new MediaAssetUploadedEvent(12L, 22L));
    }
}
