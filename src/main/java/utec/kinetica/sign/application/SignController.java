package utec.kinetica.sign.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import utec.kinetica.sign.application.dto.SignCreateRequest;
import utec.kinetica.sign.application.dto.SignResponse;
import utec.kinetica.sign.application.dto.SignUpdateRequest;
import utec.kinetica.sign.domain.Sign;
import utec.kinetica.sign.domain.SignService;

import java.util.List;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/signs")
public class SignController {
    private final SignService service;

    public SignController(SignService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SignResponse>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SignResponse> create(@Valid @RequestBody SignCreateRequest request) {
        Sign created = service.create(request.label(), request.mediaRef(), request.locale(), request.active());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/signs/" + created.getId()))
                .body(toResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SignResponse> update(@PathVariable Long id, @Valid @RequestBody SignUpdateRequest request) {
        Sign updated = service.update(id, request.label(), request.mediaRef(), request.locale(), request.active());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private SignResponse toResponse(Sign sign) {
        return new SignResponse(
                sign.getId(),
                sign.getLabel(),
                sign.getNormalizedLabel(),
                sign.getMediaRef(),
                sign.getLocale(),
                sign.isActive()
        );
    }
}
