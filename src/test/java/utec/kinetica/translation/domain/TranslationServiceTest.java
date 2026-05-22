package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    @Test
    void shouldStorePendingRequestAndOutboxWhenCreatingRequest() {
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        TranslationResultRepository resultRepository = mock(TranslationResultRepository.class);
        OutboxEventRepository outboxRepository = mock(OutboxEventRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        TranslationService service = new TranslationService(
                requestRepository,
                resultRepository,
                outboxRepository,
                userRepository,
                publisher
        );

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        TranslationRequest persisted = new TranslationRequest();
        persisted.setId(55L);
        persisted.setDirection(TranslationDirection.TEXT_TO_SIGN);
        persisted.setSourceText("hola");
        persisted.setStatus(TranslationStatus.PENDING);
        when(requestRepository.save(any(TranslationRequest.class))).thenReturn(persisted);
        OutboxEvent savedOutbox = new OutboxEvent();
        savedOutbox.setId(77L);
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(savedOutbox);

        TranslationRequest result = service.createRequest(1L, TranslationDirection.TEXT_TO_SIGN, "hola");

        assertEquals(55L, result.getId());
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("TRANSLATION_REQUESTED", captor.getValue().getEventType());
        verify(publisher).publishEvent(any(TranslationRequestedEvent.class));
    }
}
