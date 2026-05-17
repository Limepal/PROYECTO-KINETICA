package utec.kinetica.translation.application.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record TranslationKpiResponse(
        Instant from,
        Instant to,
        long totalRequests,
        long completedRequests,
        long failedRequests,
        double successRate,
        Double averageLatencyMs,
        Long p95LatencyMs,
        Double averageConfidence,
        double lowConfidenceRate,
        long lowConfidenceCount
) {
}
