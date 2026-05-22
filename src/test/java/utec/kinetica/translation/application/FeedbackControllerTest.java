package utec.kinetica.translation.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import utec.kinetica.auth.domain.User;
import utec.kinetica.translation.application.dto.CreateFeedbackRequest;
import utec.kinetica.translation.domain.Feedback;
import utec.kinetica.translation.domain.FeedbackService;
import utec.kinetica.translation.domain.TranslationRequest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackControllerTest {

    @Test
    void shouldHandleFeedbackEndpointsWhenCalled() {
        FeedbackService service = mock(FeedbackService.class);
        FeedbackController controller = new FeedbackController(service);
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("7").build();

        Feedback feedback = buildFeedback(5L, 11L, 7L, 4, "detalle");

        when(service.create(11L, 7L, 4, "detalle")).thenReturn(feedback);
        when(service.listByRequest(11L, 7L)).thenReturn(List.of(feedback));
        when(service.getById(11L, 7L, 5L)).thenReturn(feedback);

        var created = controller.create(jwt, 11L, new CreateFeedbackRequest(4, "detalle"));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        var list = controller.list(jwt, 11L);
        assertEquals(1, list.getBody().size());

        var byId = controller.getById(jwt, 11L, 5L);
        assertEquals(5L, byId.getBody().id());

        var deleted = controller.delete(jwt, 11L, 5L);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        verify(service).delete(11L, 7L, 5L);
    }

    private static Feedback buildFeedback(Long id, Long requestId, Long userId, int rating, String text) {
        TranslationRequest request = new TranslationRequest();
        request.setId(requestId);
        User user = new User();
        user.setId(userId);
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setRequest(request);
        feedback.setUser(user);
        feedback.setRating(rating);
        feedback.setCorrectionText(text);
        feedback.setCreatedAt(Instant.now());
        return feedback;
    }
}
