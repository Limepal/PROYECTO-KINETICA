package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.support.PostgresContainerSupport;
import utec.kinetica.translation.domain.Feedback;
import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeedbackRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TranslationRequestRepository translationRequestRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void shouldFindByRequestIdWhenFeedbackExistsForRequest() {
        User user = new User();
        user.setEmail("feedback-user@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.TEXT_TO_SIGN);
        request.setStatus(TranslationStatus.DONE);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setRequest(request);
        feedback.setRating(5);
        feedback.setCorrectionText("ok");
        feedbackRepository.save(feedback);

        assertEquals(1, feedbackRepository.findByRequestId(request.getId()).size());
    }

    @Test
    void shouldReturnEmptyWhenFindingByRequestIdAndFeedbackDoesNotExist() {
        assertEquals(0, feedbackRepository.findByRequestId(Long.MAX_VALUE).size());
    }
}
