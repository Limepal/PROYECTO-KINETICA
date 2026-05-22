package utec.kinetica.translation.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TranslationService {
    private final TranslationRequestRepository requestRepository;
    private final TranslationResultRepository resultRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TranslationService(
            TranslationRequestRepository requestRepository,
            TranslationResultRepository resultRepository,
            OutboxEventRepository outboxEventRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TranslationRequest createRequest(Long userId, TranslationDirection direction, String sourceText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(direction);
        request.setSourceText(sourceText);
        request.setStatus(TranslationStatus.PENDING);
        TranslationRequest saved = requestRepository.save(request);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType("TRANSLATION_REQUESTED");
        outbox.setPayload("{\"requestId\":" + saved.getId() + "}");
        outbox.setStatus("PENDING");
        OutboxEvent savedOutbox = outboxEventRepository.save(outbox);

        eventPublisher.publishEvent(new TranslationRequestedEvent(saved.getId(), savedOutbox.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.List<TranslationRequest> listRequests(Long userId) {
        return requestRepository.findByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, TranslationResult> listResultsByRequestIds(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        return resultRepository.findByRequestIdIn(requestIds).stream()
                .collect(Collectors.toMap(result -> result.getRequest().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public TranslationRequest getRequest(Long requestId, Long userId) {
        return requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
    }

    @Transactional(readOnly = true)
    public TranslationResult getResult(Long requestId, Long userId) {
        TranslationRequest request = getRequest(requestId, userId);
        return resultRepository.findByRequestId(request.getId()).orElse(null);
    }

    @Transactional
    public TranslationRequest updateRequest(Long requestId, Long userId, String sourceText) {
        TranslationRequest request = getRequest(requestId, userId);
        request.setSourceText(sourceText);
        return requestRepository.save(request);
    }

    @Transactional
    public void deleteRequest(Long requestId, Long userId) {
        TranslationRequest request = getRequest(requestId, userId);
        resultRepository.findByRequestId(requestId).ifPresent(resultRepository::delete);
        requestRepository.delete(request);
    }
}
