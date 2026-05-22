package utec.kinetica.translation.domain;

import java.time.Instant;

public record TranslationKpiSummary(
        Instant from,
        Instant to,
        long totalRequests,
        long completed,
        long failed,
        double successRate,
        Double averageLatencyMs,
        Long p95LatencyMs,
        Double averageConfidence,
        double lowConfidenceRate,
        long lowConfidenceCount
) {
}
