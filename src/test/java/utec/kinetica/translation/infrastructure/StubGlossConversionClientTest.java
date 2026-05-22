package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import utec.kinetica.translation.domain.GlossConversionResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StubGlossConversionClientTest {

    private final StubGlossConversionClient client = new StubGlossConversionClient();

    @Test
    void shouldReturnExpectedStubGlossForSpanishInput() {
        GlossConversionResult result = client.spanishToGloss("Yo quiero arroz");

        assertEquals("YO QUERER ARROZ", result.outputText());
        assertEquals(0.72, result.confidence());
        assertEquals(1, result.notes().size());
        assertEquals("stub-response", result.notes().get(0));
        assertEquals("stub-gloss-v1", result.modelVersion());
    }

    @Test
    void shouldReturnExpectedStubSpanishForGlossInput() {
        GlossConversionResult result = client.glossToSpanish("YO QUERER ARROZ");

        assertEquals("Yo quiero arroz.", result.outputText());
        assertEquals(0.72, result.confidence());
        assertEquals(1, result.notes().size());
        assertEquals("stub-response", result.notes().get(0));
        assertEquals("stub-gloss-v1", result.modelVersion());
    }
}
