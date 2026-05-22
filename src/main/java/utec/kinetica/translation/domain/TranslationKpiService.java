package utec.kinetica.translation.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class TranslationKpiService {
    private final TranslationRequestRepository requestRepository;
    private final TranslationResultRepository resultRepository;
    private final double lowConfidenceThreshold;

    public TranslationKpiService(
            TranslationRequestRepository requestRepository,
            TranslationResultRepository resultRepository,
            @Value("${kinetica.kpi.low-confidence-threshold:0.65}") double lowConfidenceThreshold
    ) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    @Transactional(readOnly = true)
    public TranslationKpiSummary summarizeLastDays(int days) {
        int normalizedDays = Math.max(days, 1);
        Instant to = Instant.now();
        Instant from = to.minus(normalizedDays, ChronoUnit.DAYS);

        long totalRequests = requestRepository.countByCreatedAtAfter(from);
        long completed = requestRepository.countByCreatedAtAfterAndStatus(from, TranslationStatus.DONE);
        long failed = requestRepository.countByCreatedAtAfterAndStatus(from, TranslationStatus.FAILED);

        List<TranslationResult> results = resultRepository.findByCreatedAtAfter(from);
        List<Long> latencies = results.stream()
                .map(TranslationResult::getLatencyMs)
                .filter(value -> value != null && value >= 0)
                .sorted(Comparator.naturalOrder())
                .toList();

        Double averageLatency = latencies.isEmpty()
                ? null
                : latencies.stream().mapToLong(Long::longValue).average().orElse(0);

        Long p95Latency = latencies.isEmpty() ? null : percentile95(latencies);

        List<Double> confidences = results.stream()
                .map(TranslationResult::getConfidence)
                .filter(value -> value != null && value >= 0)
                .toList();

        Double averageConfidence = confidences.isEmpty()
                ? null
                : confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        long lowConfidenceCount = confidences.stream()
                .filter(value -> value < lowConfidenceThreshold)
                .count();

        double successRate = totalRequests == 0 ? 0 : (double) completed / totalRequests;
        double lowConfidenceRate = confidences.isEmpty() ? 0 : (double) lowConfidenceCount / confidences.size();

        return new TranslationKpiSummary(
                from,
                to,
                totalRequests,
                completed,
                failed,
                successRate,
                averageLatency,
                p95Latency,
                averageConfidence,
                lowConfidenceRate,
                lowConfidenceCount
        );
    }

    private long percentile95(List<Long> sortedValues) {
        int index = (int) Math.ceil(0.95 * sortedValues.size()) - 1;
        int boundedIndex = Math.min(Math.max(index, 0), sortedValues.size() - 1);
        return sortedValues.get(boundedIndex);
    }
}
