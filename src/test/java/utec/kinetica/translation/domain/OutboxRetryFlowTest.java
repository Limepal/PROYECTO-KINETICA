package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRetryFlowTest {

    @Test
    void handlerShouldMoveOutboxFromProcessingToProcessed() {
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        TranslationResultRepository resultRepository = mock(TranslationResultRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        AiInferenceClient aiInferenceClient = mock(AiInferenceClient.class);
        GlossConversionService glossConversionService = mock(GlossConversionService.class);

        TranslationRequestedEventHandler handler = new TranslationRequestedEventHandler(
                requestRepository,
                resultRepository,
                outboxEventRepository,
                aiInferenceClient,
                glossConversionService
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(5L);
        outboxEvent.setStatus("PENDING");
        outboxEvent.setRetryCount(0);
        outboxEvent.setMaxRetries(5);

        TranslationRequest request = new TranslationRequest();
        request.setId(9L);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.PENDING);
        request.setSourceText("hola");

        when(outboxEventRepository.findById(5L)).thenReturn(Optional.of(outboxEvent));
        when(requestRepository.findById(9L)).thenReturn(Optional.of(request));
        when(aiInferenceClient.translateSignToText(9L, "hola")).thenReturn(
                new AiInferenceResponse(TranslationStatus.DONE, "ok", null, 0.95, 120L, "stub", null)
        );

        handler.onTranslationRequested(new TranslationRequestedEvent(9L, 5L));

        assertEquals("PROCESSED", outboxEvent.getStatus());
        assertEquals(TranslationStatus.DONE, request.getStatus());
        verify(resultRepository, times(1)).save(any(TranslationResult.class));
        verify(requestRepository, times(1)).save(request);
    }

    @Test
    void schedulerShouldFailInvalidPendingAndRetryFailedEvents() {
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OutboxRecoveryScheduler scheduler = new OutboxRecoveryScheduler(outboxEventRepository, eventPublisher);

        OutboxEvent invalidPending = new OutboxEvent();
        invalidPending.setId(1L);
        invalidPending.setStatus("PENDING");
        invalidPending.setPayload("{}");
        invalidPending.setRetryCount(0);
        invalidPending.setMaxRetries(5);

        OutboxEvent retryableFailed = new OutboxEvent();
        retryableFailed.setId(2L);
        retryableFailed.setStatus("FAILED");
        retryableFailed.setPayload("{\"requestId\":99}");
        retryableFailed.setRetryCount(1);
        retryableFailed.setMaxRetries(5);
        retryableFailed.setNextRetryAt(Instant.now().minusSeconds(5));

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(invalidPending));
        when(outboxEventRepository.findRetryableByStatus(eq("FAILED"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(retryableFailed));

        scheduler.recoverPendingEvents();

        assertEquals("FAILED", invalidPending.getStatus());
        assertEquals(1, invalidPending.getRetryCount());
        assertNotNull(invalidPending.getNextRetryAt());
        assertTrue(invalidPending.getLastError().contains("Invalid outbox payload"));

        verify(eventPublisher, times(1)).publishEvent(new TranslationRequestedEvent(99L, 2L));
    }
}
