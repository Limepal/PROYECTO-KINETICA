package utec.kinetica.translation.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import utec.kinetica.translation.application.dto.CreateMediaAssetRequest;
import utec.kinetica.translation.application.dto.MediaAssetResponse;
import utec.kinetica.translation.domain.MediaAsset;
import utec.kinetica.translation.domain.MediaAssetKind;
import utec.kinetica.translation.domain.MediaAssetService;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/translations/{requestId}/media")
public class MediaAssetController {
    private final MediaAssetService mediaAssetService;

    public MediaAssetController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/translations/" + requestId + "/media/" + media.getId()))
                .body(toResponse(media));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @RequestParam MediaAssetKind kind,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Long durationMs
    ) throws IOException {
        Long userId = Long.valueOf(jwt.getSubject());
        MediaAsset media = mediaAssetService.createManagedUpload(
                requestId,
                userId,
                kind,
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getBytes(),
                file.getOriginalFilename(),
                durationMs
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/translations/" + requestId + "/media/" + media.getId()))
                .body(toResponse(media));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<java.util.List<MediaAssetResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(mediaAssetService.listByRequest(requestId, userId).stream().map(this::toResponse).toList());
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaAssetResponse> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId, @PathVariable Long mediaId) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(toResponse(mediaAssetService.getById(requestId, userId, mediaId)));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN','MANAGER')")
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
                media.getSizeBytes(),
                media.getCreatedAt(),
                media.getExpiresAt()
        );
    }
}
