package utec.kinetica.translation.application.dto;

import utec.kinetica.translation.domain.MediaAssetKind;

public record MediaAssetResponse(
        Long id,
        Long requestId,
        MediaAssetKind kind,
        String storageUrl,
        String mimeType,
        Long durationMs,
        Long sizeBytes
) {
}
