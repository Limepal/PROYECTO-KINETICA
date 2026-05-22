package utec.kinetica.translation.application;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.translation.application.dto.TranslationKpiResponse;
import utec.kinetica.translation.domain.TranslationKpiService;
import utec.kinetica.translation.domain.TranslationKpiSummary;

@RestController
@RequestMapping("/api/v1/kpis/translations")
public class TranslationKpiController {
    private final TranslationKpiService kpiService;

    public TranslationKpiController(TranslationKpiService kpiService) {
        this.kpiService = kpiService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<TranslationKpiResponse> getSummary(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(toResponse(kpiService.summarizeLastDays(days)));
    }

    private TranslationKpiResponse toResponse(TranslationKpiSummary summary) {
        return new TranslationKpiResponse(
                summary.from(),
                summary.to(),
                summary.totalRequests(),
                summary.completed(),
                summary.failed(),
                summary.successRate(),
                summary.averageLatencyMs(),
                summary.p95LatencyMs(),
                summary.averageConfidence(),
                summary.lowConfidenceRate(),
                summary.lowConfidenceCount()
        );
    }
}
