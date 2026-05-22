package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.support.PostgresContainerSupport;
import utec.kinetica.translation.domain.OutboxEvent;
import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationResult;
import utec.kinetica.translation.domain.TranslationStatus;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class TranslationRepositoryQueryIntegrationTest extends PostgresContainerSupport {

    @Autowired
    private TranslationRequestRepository translationRequestRepository;

    @Autowired
    private TranslationResultRepository translationResultRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnOnlyMatchingResultsWhenFindingByRequestIdIn() {
        User user = new User();
        user.setEmail("translation-query@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest r1 = new TranslationRequest();
        r1.setUser(user);
        r1.setDirection(TranslationDirection.SIGN_TO_TEXT);
        r1.setStatus(TranslationStatus.DONE);
        r1.setSourceText("uno");
        r1 = translationRequestRepository.save(r1);

        TranslationRequest r2 = new TranslationRequest();
        r2.setUser(user);
        r2.setDirection(TranslationDirection.TEXT_TO_SIGN);
        r2.setStatus(TranslationStatus.DONE);
        r2.setSourceText("dos");
        r2 = translationRequestRepository.save(r2);

        TranslationResult result1 = new TranslationResult();
        result1.setRequest(r1);
        result1.setModelVersion("test-model");
        result1.setTextOutput("ok-1");
        translationResultRepository.save(result1);

        TranslationResult result2 = new TranslationResult();
        result2.setRequest(r2);
        result2.setModelVersion("test-model");
        result2.setTextOutput("ok-2");
        translationResultRepository.save(result2);

        List<TranslationResult> found = translationResultRepository.findByRequestIdIn(List.of(r2.getId()));

        assertEquals(1, found.size());
        assertEquals(r2.getId(), found.get(0).getRequest().getId());
        assertEquals("ok-2", found.get(0).getTextOutput());
    }

    @Test
    void shouldFilterByWindowAndRetryBudgetWhenFindingRetryableByStatus() {
        OutboxEvent retryable = new OutboxEvent();
        retryable.setEventType("TRANSLATION_REQUESTED");
        retryable.setPayload("{\"requestId\":1}");
        retryable.setStatus("FAILED");
        retryable.setRetryCount(1);
        retryable.setMaxRetries(5);
        retryable.setNextRetryAt(Instant.now().minusSeconds(10));
        outboxEventRepository.save(retryable);

        OutboxEvent notDueYet = new OutboxEvent();
        notDueYet.setEventType("TRANSLATION_REQUESTED");
        notDueYet.setPayload("{\"requestId\":2}");
        notDueYet.setStatus("FAILED");
        notDueYet.setRetryCount(1);
        notDueYet.setMaxRetries(5);
        notDueYet.setNextRetryAt(Instant.now().plusSeconds(120));
        outboxEventRepository.save(notDueYet);

        OutboxEvent exhausted = new OutboxEvent();
        exhausted.setEventType("TRANSLATION_REQUESTED");
        exhausted.setPayload("{\"requestId\":3}");
        exhausted.setStatus("FAILED");
        exhausted.setRetryCount(5);
        exhausted.setMaxRetries(5);
        exhausted.setNextRetryAt(Instant.now().minusSeconds(10));
        outboxEventRepository.save(exhausted);

        List<OutboxEvent> found = outboxEventRepository.findRetryableByStatus(
                "FAILED",
                Instant.now(),
                PageRequest.of(0, 50)
        );

        assertEquals(1, found.size());
        assertEquals(retryable.getId(), found.get(0).getId());
    }
}
