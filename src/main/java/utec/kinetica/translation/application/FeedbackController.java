package utec.kinetica.translation.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.translation.application.dto.CreateFeedbackRequest;
import utec.kinetica.translation.application.dto.FeedbackResponse;
import utec.kinetica.translation.domain.Feedback;
import utec.kinetica.translation.domain.FeedbackService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/translations/{requestId}/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<FeedbackResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @Valid @RequestBody CreateFeedbackRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        Feedback feedback = feedbackService.create(requestId, userId, request.rating(), request.correctionText());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/translations/" + requestId + "/feedback/" + feedback.getId()))
                .body(new FeedbackResponse(
                feedback.getId(),
                feedback.getRequest().getId(),
                feedback.getUser().getId(),
                feedback.getRating(),
                feedback.getCorrectionText(),
                feedback.getCreatedAt()
        ));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<java.util.List<FeedbackResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(feedbackService.listByRequest(requestId, userId).stream().map(this::toResponse).toList());
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponse> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId, @PathVariable Long feedbackId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(toResponse(feedbackService.getById(requestId, userId, feedbackId)));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId, @PathVariable Long feedbackId) {
        Long userId = Long.valueOf(jwt.getSubject());
        feedbackService.delete(requestId, userId, feedbackId);
        return ResponseEntity.noContent().build();
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRequest().getId(),
                feedback.getUser().getId(),
                feedback.getRating(),
                feedback.getCorrectionText(),
                feedback.getCreatedAt()
        );
    }
}
