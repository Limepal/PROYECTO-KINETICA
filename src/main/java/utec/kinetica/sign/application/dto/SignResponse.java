package utec.kinetica.sign.application.dto;

public record SignResponse(
        Long id,
        String label,
        String normalizedLabel,
        String mediaRef,
        String locale,
        boolean active
) {
}
