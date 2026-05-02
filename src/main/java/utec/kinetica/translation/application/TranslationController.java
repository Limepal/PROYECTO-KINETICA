package utec.kinetica.translation.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.translation.application.dto.CreateTranslationRequest;
import utec.kinetica.translation.application.dto.TranslationResponse;
import utec.kinetica.translation.application.dto.UpdateTranslationRequest;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationResult;
import utec.kinetica.translation.domain.TranslationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/translations")
public class TranslationController {
    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TranslationResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTranslationRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        TranslationRequest created = translationService.createRequest(userId, request.direction(), request.sourceText());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(created, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TranslationResponse> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Long userId = Long.valueOf(jwt.getSubject());
        TranslationRequest translationRequest = translationService.getRequest(id, userId);
        TranslationResult result = translationService.getResult(id, userId);
        return ResponseEntity.ok(toResponse(translationRequest, result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<java.util.List<TranslationResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<TranslationRequest> requests = translationService.listRequests(userId);
        Map<Long, TranslationResult> resultsByRequestId = translationService.listResultsByRequestIds(
                requests.stream().map(TranslationRequest::getId).toList()
        );
        return ResponseEntity.ok(requests.stream()
                .map(request -> toResponse(request, resultsByRequestId.get(request.getId())))
                .toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TranslationResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTranslationRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        TranslationRequest updated = translationService.updateRequest(id, userId, request.sourceText());
        return ResponseEntity.ok(toResponse(updated, translationService.getResult(updated.getId(), userId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Long userId = Long.valueOf(jwt.getSubject());
        translationService.deleteRequest(id, userId);
        return ResponseEntity.noContent().build();
    }

    private TranslationResponse toResponse(TranslationRequest request, TranslationResult result) {
        return new TranslationResponse(
                request.getId(),
                request.getStatus(),
                request.getDirection(),
                result != null ? result.getTextOutput() : null,
                result != null ? result.getSignOutputRef() : null,
                result != null ? result.getConfidence() : null,
                result != null ? result.getWarning() : null,
                result != null ? result.getModelVersion() : null,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
