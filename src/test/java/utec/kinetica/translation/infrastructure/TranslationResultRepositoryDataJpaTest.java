package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.support.PostgresContainerSupport;
import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationResult;
import utec.kinetica.translation.domain.TranslationStatus;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranslationResultRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TranslationRequestRepository translationRequestRepository;
    @Autowired
    private TranslationResultRepository translationResultRepository;

    @Test
    void shouldFindByRequestIdAndRequestIdInWhenResultsExist() {
        User user = new User();
        user.setEmail("result-user@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.DONE);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        TranslationResult result = new TranslationResult();
        result.setRequest(request);
        result.setTextOutput("hola");
        result.setModelVersion("stub");
        translationResultRepository.save(result);

        assertTrue(translationResultRepository.findByRequestId(request.getId()).isPresent());
        assertEquals(1, translationResultRepository.findByRequestIdIn(List.of(request.getId())).size());
    }

    @Test
    void shouldFindByCreatedAtAfterWhenResultIsRecent() {
        User user = new User();
        user.setEmail("result-recent@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.DONE);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        TranslationResult result = new TranslationResult();
        result.setRequest(request);
        result.setTextOutput("hola");
        result.setModelVersion("stub");
        translationResultRepository.save(result);

        assertEquals(1, translationResultRepository.findByCreatedAtAfter(Instant.now().minusSeconds(60)).size());
        assertEquals(0, translationResultRepository.findByCreatedAtAfter(Instant.now().plusSeconds(60)).size());
    }
}
