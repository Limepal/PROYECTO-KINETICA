package utec.kinetica.common.application;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String code,
        String message,
        List<String> details,
        String path,
        Instant timestamp
) {
}
