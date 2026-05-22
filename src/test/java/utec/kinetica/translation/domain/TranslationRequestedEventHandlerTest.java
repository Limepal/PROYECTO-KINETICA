package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationRequestedEventHandlerTest {

    @Test
    void shouldConvertSpanishToGlossBeforeAiWhenDirectionIsTextToSign() {
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        TranslationResultRepository resultRepository = mock(TranslationResultRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        AiInferenceClient aiInferenceClient = mock(AiInferenceClient.class);
        GlossConversionService glossConversionService = mock(GlossConversionService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        TranslationRequest request = new TranslationRequest();
        request.setId(20L);
        request.setDirection(TranslationDirection.TEXT_TO_SIGN);
        request.setSourceText("yo quiero arroz");
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(77L);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(request));
        when(outboxEventRepository.findById(77L)).thenReturn(Optional.of(outbox));
        when(glossConversionService.spanishToGloss("yo quiero arroz"))
                .thenReturn(new GlossConversionResult("YO QUERER ARROZ", 0.9, List.of(), List.of(), List.of(), "g1"));
        when(aiInferenceClient.translateTextToSign(20L, "YO QUERER ARROZ"))
                .thenReturn(new AiInferenceResponse(TranslationStatus.DONE, null, "sign://seq/1", 0.82, 120, "ai-1", null));

        TranslationRequestedEventHandler handler = new TranslationRequestedEventHandler(
                requestRepository,
                resultRepository,
                outboxEventRepository,
                aiInferenceClient,
                glossConversionService,
                eventPublisher
        );

        handler.onTranslationRequested(new TranslationRequestedEvent(20L, 77L));

        ArgumentCaptor<TranslationResult> resultCaptor = ArgumentCaptor.forClass(TranslationResult.class);
        verify(resultRepository).save(resultCaptor.capture());
        assertEquals("YO QUERER ARROZ", resultCaptor.getValue().getGlossOutput());
        assertEquals("sign://seq/1", resultCaptor.getValue().getSignOutputRef());
        assertNull(resultCaptor.getValue().getTextOutput());
    }

    @Test
    void shouldNormalizeGlossToNaturalSpanishWhenDirectionIsSignToText() {
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        TranslationResultRepository resultRepository = mock(TranslationResultRepository.class);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        AiInferenceClient aiInferenceClient = mock(AiInferenceClient.class);
        GlossConversionService glossConversionService = mock(GlossConversionService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        TranslationRequest request = new TranslationRequest();
        request.setId(30L);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setSourceText("sign://input");
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(88L);

        when(requestRepository.findById(30L)).thenReturn(Optional.of(request));
        when(outboxEventRepository.findById(88L)).thenReturn(Optional.of(outbox));
        when(aiInferenceClient.translateSignToText(30L, "sign://input"))
                .thenReturn(new AiInferenceResponse(TranslationStatus.DONE, "YO QUERER ARROZ", null, 0.8, 140, "ai-1", null));
        when(glossConversionService.glossToSpanish("YO QUERER ARROZ"))
                .thenReturn(new GlossConversionResult("Yo quiero arroz.", 0.95, List.of(), List.of(), List.of(), "g1"));

        TranslationRequestedEventHandler handler = new TranslationRequestedEventHandler(
                requestRepository,
                resultRepository,
                outboxEventRepository,
                aiInferenceClient,
                glossConversionService,
                eventPublisher
        );

        handler.onTranslationRequested(new TranslationRequestedEvent(30L, 88L));

        ArgumentCaptor<TranslationResult> resultCaptor = ArgumentCaptor.forClass(TranslationResult.class);
        verify(resultRepository).save(resultCaptor.capture());
        assertEquals("YO QUERER ARROZ", resultCaptor.getValue().getGlossOutput());
        assertEquals("Yo quiero arroz.", resultCaptor.getValue().getTextOutput());
    }
}
