package utec.kinetica.translation.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.translation.infrastructure.FeedbackRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final TranslationRequestRepository requestRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, TranslationRequestRepository requestRepository) {
        this.feedbackRepository = feedbackRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public Feedback create(Long requestId, Long userId, Integer rating, String correctionText) {
        TranslationRequest request = requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        Feedback feedback = new Feedback();
        feedback.setRequest(request);
        feedback.setUserId(userId);
        feedback.setRating(rating);
        feedback.setCorrectionText(correctionText);
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public java.util.List<Feedback> listByRequest(Long requestId, Long userId) {
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        return feedbackRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Feedback getById(Long requestId, Long userId, Long id) {
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found: " + id));
        if (!feedback.getRequest().getId().equals(requestId)) {
            throw new EntityNotFoundException("Feedback not found for request: " + requestId);
        }
        return feedback;
    }

    @Transactional
    public void delete(Long requestId, Long userId, Long id) {
        Feedback feedback = getById(requestId, userId, id);
        feedbackRepository.delete(feedback);
    }
}
