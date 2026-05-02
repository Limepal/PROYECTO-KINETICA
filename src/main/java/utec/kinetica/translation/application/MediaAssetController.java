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
import utec.kinetica.translation.application.dto.CreateMediaAssetRequest;
import utec.kinetica.translation.application.dto.MediaAssetResponse;
import utec.kinetica.translation.domain.MediaAsset;
import utec.kinetica.translation.domain.MediaAssetService;

@RestController
@RequestMapping("/translations/{requestId}/media")
public class MediaAssetController {
    private final MediaAssetService mediaAssetService;

    public MediaAssetController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<MediaAssetResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @Valid @RequestBody CreateMediaAssetRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        MediaAsset media = mediaAssetService.create(
                requestId,
                userId,
                request.kind(),
                request.storageUrl(),
                request.mimeType(),
                request.durationMs(),
                request.sizeBytes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new MediaAssetResponse(
                media.getId(),
                media.getRequest().getId(),
                media.getKind(),
                media.getStorageUrl(),
                media.getMimeType(),
                media.getDurationMs(),
                media.getSizeBytes()
        ));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public ResponseEntity<java.util.List<MediaAssetResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(mediaAssetService.listByRequest(requestId, userId).stream().map(this::toResponse).toList());
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaAssetResponse> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId, @PathVariable Long mediaId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(toResponse(mediaAssetService.getById(requestId, userId, mediaId)));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId, @PathVariable Long mediaId) {
        Long userId = Long.valueOf(jwt.getSubject());
        mediaAssetService.delete(requestId, userId, mediaId);
        return ResponseEntity.noContent().build();
    }

    private MediaAssetResponse toResponse(MediaAsset media) {
        return new MediaAssetResponse(
                media.getId(),
                media.getRequest().getId(),
                media.getKind(),
                media.getStorageUrl(),
                media.getMimeType(),
                media.getDurationMs(),
                media.getSizeBytes()
        );
    }
}
