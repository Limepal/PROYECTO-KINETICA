package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import utec.kinetica.translation.application.dto.TranslationKpiResponse;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationKpiServiceTest {

    @Test
    void shouldCalculateKpisFromRecentData() {
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        TranslationResultRepository resultRepository = mock(TranslationResultRepository.class);

        when(requestRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);
        when(requestRepository.countByCreatedAtAfterAndStatus(any(Instant.class), org.mockito.ArgumentMatchers.eq(TranslationStatus.DONE))).thenReturn(7L);
        when(requestRepository.countByCreatedAtAfterAndStatus(any(Instant.class), org.mockito.ArgumentMatchers.eq(TranslationStatus.FAILED))).thenReturn(2L);

        TranslationResult r1 = new TranslationResult();
        r1.setLatencyMs(200L);
        r1.setConfidence(0.9);
        TranslationResult r2 = new TranslationResult();
        r2.setLatencyMs(400L);
        r2.setConfidence(0.6);
        TranslationResult r3 = new TranslationResult();
        r3.setLatencyMs(100L);
        r3.setConfidence(0.5);

        when(resultRepository.findByCreatedAtAfter(any(Instant.class))).thenReturn(List.of(r1, r2, r3));

        TranslationKpiService service = new TranslationKpiService(requestRepository, resultRepository, 0.65);
        TranslationKpiResponse summary = service.summarizeLastDays(7);

        assertEquals(10L, summary.totalRequests());
        assertEquals(7L, summary.completedRequests());
        assertEquals(2L, summary.failedRequests());
        assertEquals(0.7, summary.successRate(), 1e-9);
        assertNotNull(summary.averageLatencyMs());
        assertEquals(233.33333333333334, summary.averageLatencyMs(), 1e-9);
        assertEquals(400L, summary.p95LatencyMs());
        assertNotNull(summary.averageConfidence());
        assertEquals(2L, summary.lowConfidenceCount());
        assertTrue(summary.lowConfidenceRate() > 0.66 && summary.lowConfidenceRate() < 0.67);
    }
}
