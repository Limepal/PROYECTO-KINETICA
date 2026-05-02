package utec.kinetica.translation.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import utec.kinetica.translation.domain.MediaAssetKind;

public record CreateMediaAssetRequest(
        @NotNull MediaAssetKind kind,
        @NotBlank String storageUrl,
        @NotBlank String mimeType,
        Long durationMs,
        Long sizeBytes
) {
}
