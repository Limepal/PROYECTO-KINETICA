package utec.kinetica.translation.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.common.domain.exception.ResourceNotFoundException;
import utec.kinetica.translation.infrastructure.FeedbackRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final TranslationRequestRepository requestRepository;
    private final UserRepository userRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            TranslationRequestRepository requestRepository,
            UserRepository userRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Feedback create(Long requestId, Long userId, Integer rating, String correctionText) {
        TranslationRequest request = requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Feedback feedback = new Feedback();
        feedback.setRequest(request);
        feedback.setUser(user);
        feedback.setRating(rating);
        feedback.setCorrectionText(correctionText);
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public java.util.List<Feedback> listByRequest(Long requestId, Long userId) {
        requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        return feedbackRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Feedback getById(Long requestId, Long userId, Long id) {
        requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
        if (!feedback.getRequest().getId().equals(requestId)) {
            throw new ResourceNotFoundException("Feedback not found for request: " + requestId);
        }
        return feedback;
    }

    @Transactional
    public void delete(Long requestId, Long userId, Long id) {
        Feedback feedback = getById(requestId, userId, id);
        feedbackRepository.delete(feedback);
    }
}
