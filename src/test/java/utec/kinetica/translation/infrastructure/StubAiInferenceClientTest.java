package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import utec.kinetica.translation.domain.AiInferenceResponse;
import utec.kinetica.translation.domain.TranslationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StubAiInferenceClientTest {

    private final StubAiInferenceClient client = new StubAiInferenceClient();

    @Test
    void shouldReturnDoneResponseForSignToText() {
        AiInferenceResponse response = client.translateSignToText(1L, "SEÑA");

        assertEquals(TranslationStatus.DONE, response.status());
        assertEquals("Traducción simulada de seña a texto", response.textOutput());
        assertNull(response.signOutputRef());
        assertEquals(0.78, response.confidence());
        assertEquals(180L, response.latencyMs());
        assertEquals("stub-v1", response.modelVersion());
        assertNull(response.warning());
    }

    @Test
    void shouldReturnLowConfidenceWarningForTextToSign() {
        AiInferenceResponse response = client.translateTextToSign(2L, "hola");

        assertEquals(TranslationStatus.DONE, response.status());
        assertNull(response.textOutput());
        assertEquals("sign://catalog/basic-sequence", response.signOutputRef());
        assertEquals(0.62, response.confidence());
        assertEquals(210L, response.latencyMs());
        assertEquals("stub-v1", response.modelVersion());
        assertEquals("LOW_CONFIDENCE", response.warning());
    }
}
