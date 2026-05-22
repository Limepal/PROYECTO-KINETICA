package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlossConversionServiceTest {

    @Test
    void shouldDelegateSpanishToGlossWhenConvertingEsToGloss() {
        GlossConversionClient client = mock(GlossConversionClient.class);
        when(client.spanishToGloss("quiero comer arroz")).thenReturn(
                new GlossConversionResult("YO QUERER COMER ARROZ", 0.9, List.of(), List.of(), List.of(), "m1")
        );

        GlossConversionService service = new GlossConversionService(client);
        GlossConversionResult result = service.spanishToGloss("quiero comer arroz");

        assertEquals("YO QUERER COMER ARROZ", result.outputText());
        assertEquals(0.9, result.confidence());
    }

    @Test
    void shouldDelegateGlossToSpanishWhenConvertingGlossToEs() {
        GlossConversionClient client = mock(GlossConversionClient.class);
        when(client.glossToSpanish("YO QUERER ARROZ")).thenReturn(
                new GlossConversionResult("Yo quiero arroz.", 0.88, List.of(), List.of(), List.of(), "m1")
        );

        GlossConversionService service = new GlossConversionService(client);
        GlossConversionResult result = service.glossToSpanish("YO QUERER ARROZ");

        assertEquals("Yo quiero arroz.", result.outputText());
        assertEquals(0.88, result.confidence());
    }
}
