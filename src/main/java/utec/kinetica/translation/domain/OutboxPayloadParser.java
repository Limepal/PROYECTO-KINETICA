package utec.kinetica.translation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OutboxPayloadParser {

    private final ObjectMapper objectMapper;

    public OutboxPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Long extractRequestId(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode requestId = root.get("requestId");
            if (requestId == null || !requestId.canConvertToLong()) {
                return null;
            }
            return requestId.longValue();
        } catch (Exception ignored) {
            return null;
        }
    }
}
