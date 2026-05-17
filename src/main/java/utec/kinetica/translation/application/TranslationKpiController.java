package utec.kinetica.translation.application;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.translation.application.dto.TranslationKpiResponse;
import utec.kinetica.translation.domain.TranslationKpiService;

@RestController
@RequestMapping("/kpis/translations")
public class TranslationKpiController {
    private final TranslationKpiService kpiService;

    public TranslationKpiController(TranslationKpiService kpiService) {
        this.kpiService = kpiService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<TranslationKpiResponse> getSummary(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(kpiService.summarizeLastDays(days));
    }
}
