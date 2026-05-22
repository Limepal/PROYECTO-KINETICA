package utec.kinetica.translation.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OutboxPayloadParserTest {

    private final OutboxPayloadParser parser = new OutboxPayloadParser(new ObjectMapper());

    @Test
    void shouldExtractRequestIdWhenPayloadContainsValidNumericField() {
        assertEquals(99L, parser.extractRequestId("{\"requestId\":99}"));
    }

    @Test
    void shouldReturnNullWhenPayloadIsMalformedOrFieldIsMissing() {
        assertNull(parser.extractRequestId("not-json"));
        assertNull(parser.extractRequestId("{}"));
        assertNull(parser.extractRequestId("{\"requestId\":\"abc\"}"));
    }
}
