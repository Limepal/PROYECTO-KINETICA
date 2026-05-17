package utec.kinetica.translation.application;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.translation.application.dto.GlossConversionRequest;
import utec.kinetica.translation.application.dto.GlossConversionResponse;
import utec.kinetica.translation.domain.GlossConversionResult;
import utec.kinetica.translation.domain.GlossConversionService;

@RestController
@RequestMapping("/linguistics")
public class GlossController {
    private final GlossConversionService glossConversionService;

    public GlossController(GlossConversionService glossConversionService) {
        this.glossConversionService = glossConversionService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/es-to-gloss")
    public ResponseEntity<GlossConversionResponse> spanishToGloss(@Valid @RequestBody GlossConversionRequest request) {
        GlossConversionResult result = glossConversionService.spanishToGloss(request.text());
        return ResponseEntity.ok(toResponse(request.text(), result));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/gloss-to-es")
    public ResponseEntity<GlossConversionResponse> glossToSpanish(@Valid @RequestBody GlossConversionRequest request) {
        GlossConversionResult result = glossConversionService.glossToSpanish(request.text());
        return ResponseEntity.ok(toResponse(request.text(), result));
    }

    private GlossConversionResponse toResponse(String input, GlossConversionResult result) {
        return new GlossConversionResponse(
                input,
                result.outputText(),
                result.confidence(),
                result.notes(),
                result.alternatives(),
                result.flags(),
                result.modelVersion()
        );
    }
}
