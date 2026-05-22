package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.common.domain.exception.ResourceNotFoundException;
import utec.kinetica.translation.infrastructure.FeedbackRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    @Test
    void shouldCreateFeedbackWhenRequestAndUserExist() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FeedbackService service = new FeedbackService(feedbackRepository, requestRepository, userRepository);

        Long requestId = 10L;
        Long userId = 20L;
        TranslationRequest request = new TranslationRequest();
        request.setId(requestId);
        User user = new User();
        user.setId(userId);

        when(requestRepository.findByIdAndUser_Id(requestId, userId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Feedback created = service.create(requestId, userId, 5, "todo bien");

        assertEquals(5, created.getRating());
        assertEquals("todo bien", created.getCorrectionText());
        assertEquals(requestId, created.getRequest().getId());
        assertEquals(userId, created.getUser().getId());
    }

    @Test
    void shouldThrowWhenCreateAndRequestDoesNotBelongToUser() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FeedbackService service = new FeedbackService(feedbackRepository, requestRepository, userRepository);

        when(requestRepository.findByIdAndUser_Id(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(1L, 2L, 3, "x"));
    }

    @Test
    void shouldDeleteFeedbackWhenOwnedByRequest() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FeedbackService service = new FeedbackService(feedbackRepository, requestRepository, userRepository);

        TranslationRequest request = new TranslationRequest();
        request.setId(50L);
        Feedback feedback = new Feedback();
        feedback.setId(99L);
        feedback.setRequest(request);

        when(requestRepository.findByIdAndUser_Id(50L, 7L)).thenReturn(Optional.of(request));
        when(feedbackRepository.findById(99L)).thenReturn(Optional.of(feedback));

        service.delete(50L, 7L, 99L);

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    void shouldListByRequestWhenRequestExistsForUser() {
        FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        FeedbackService service = new FeedbackService(feedbackRepository, requestRepository, userRepository);

        TranslationRequest request = new TranslationRequest();
        request.setId(11L);
        Feedback one = new Feedback();
        Feedback two = new Feedback();

        when(requestRepository.findByIdAndUser_Id(11L, 3L)).thenReturn(Optional.of(request));
        when(feedbackRepository.findByRequestId(11L)).thenReturn(List.of(one, two));

        List<Feedback> result = service.listByRequest(11L, 3L);

        assertEquals(2, result.size());
    }
}
