package utec.kinetica.translation.application.dto;

import utec.kinetica.translation.domain.MediaAssetKind;

import java.time.Instant;

public record MediaAssetResponse(
        Long id,
        Long requestId,
        MediaAssetKind kind,
        String storageUrl,
        String mimeType,
        Long durationMs,
        Long sizeBytes,
        Instant createdAt,
        Instant expiresAt
) {
}
