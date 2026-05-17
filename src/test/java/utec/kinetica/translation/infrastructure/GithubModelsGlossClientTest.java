package utec.kinetica.translation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubModelsGlossClientTest {

    @Test
    void parsePayloadShouldHandlePlainJson() throws Exception {
        String payload = """
                {
                  "output_text":"YO QUERER ARROZ",
                  "confidence":0.91,
                  "notes":["ok"],
                  "alternatives":["YO ARROZ QUERER"],
                  "flags":[]
                }
                """;

        GithubModelsGlossClient.ParsedPayload parsed = GithubModelsGlossClient.parsePayload(payload, new ObjectMapper());
        assertEquals("YO QUERER ARROZ", parsed.outputText());
        assertEquals(0.91, parsed.confidence());
        assertEquals(1, parsed.notes().size());
    }

    @Test
    void parsePayloadShouldHandleMarkdownCodeFence() throws Exception {
        String payload = """
                ```json
                {
                  "output_text":"Yo quiero arroz.",
                  "confidence":0.87,
                  "notes":[],
                  "alternatives":[],
                  "flags":["LOW_CONTEXT"]
                }
                ```
                """;

        GithubModelsGlossClient.ParsedPayload parsed = GithubModelsGlossClient.parsePayload(payload, new ObjectMapper());
        assertEquals("Yo quiero arroz.", parsed.outputText());
        assertEquals(0.87, parsed.confidence());
        assertEquals("LOW_CONTEXT", parsed.flags().get(0));
    }
}
