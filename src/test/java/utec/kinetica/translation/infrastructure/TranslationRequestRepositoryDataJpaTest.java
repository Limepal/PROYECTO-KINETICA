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
import utec.kinetica.translation.domain.TranslationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranslationRequestRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TranslationRequestRepository translationRequestRepository;

    @Test
    void shouldFindByUserIdAndIdWhenRequestBelongsToUser() {
        User user = new User();
        user.setEmail("tr-user@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.PENDING);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        assertEquals(1, translationRequestRepository.findByUser_Id(user.getId()).size());
        assertTrue(translationRequestRepository.findByIdAndUser_Id(request.getId(), user.getId()).isPresent());
    }

    @Test
    void shouldCountByCreatedAtAndStatusWhenRequestsExist() {
        User user = new User();
        user.setEmail("tr-count@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest done = new TranslationRequest();
        done.setUser(user);
        done.setDirection(TranslationDirection.SIGN_TO_TEXT);
        done.setStatus(TranslationStatus.DONE);
        done.setSourceText("one");
        translationRequestRepository.save(done);

        TranslationRequest pending = new TranslationRequest();
        pending.setUser(user);
        pending.setDirection(TranslationDirection.TEXT_TO_SIGN);
        pending.setStatus(TranslationStatus.PENDING);
        pending.setSourceText("two");
        translationRequestRepository.save(pending);

        java.time.Instant from = java.time.Instant.now().minusSeconds(60);
        assertEquals(2, translationRequestRepository.countByCreatedAtAfter(from));
        assertEquals(1, translationRequestRepository.countByCreatedAtAfterAndStatus(from, TranslationStatus.DONE));
    }
}
