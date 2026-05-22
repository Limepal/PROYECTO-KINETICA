package utec.kinetica.translation.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import utec.kinetica.translation.application.dto.GlossConversionRequest;
import utec.kinetica.translation.domain.GlossConversionResult;
import utec.kinetica.translation.domain.GlossConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlossControllerTest {

    @Test
    void shouldConvertBothDirectionsWhenEndpointsAreCalled() {
        GlossConversionService service = mock(GlossConversionService.class);
        GlossController controller = new GlossController(service);

        GlossConversionResult result = new GlossConversionResult("OUT", 0.91, List.of("n"), List.of("a"), List.of(), "model");
        when(service.spanishToGloss("hola")).thenReturn(result);
        when(service.glossToSpanish("HOLA")).thenReturn(result);

        var esToGloss = controller.convertSpanishToGloss(new GlossConversionRequest("hola"));
        assertEquals(HttpStatus.OK, esToGloss.getStatusCode());
        assertEquals("OUT", esToGloss.getBody().outputText());

        var glossToEs = controller.convertGlossToSpanish(new GlossConversionRequest("HOLA"));
        assertEquals(HttpStatus.OK, glossToEs.getStatusCode());
        assertEquals("OUT", glossToEs.getBody().outputText());
    }
}
