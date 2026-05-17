package utec.kinetica.translation.application.dto;

import java.util.List;

public record GlossConversionResponse(
        String inputText,
        String outputText,
        double confidence,
        List<String> notes,
        List<String> alternatives,
        List<String> flags,
        String modelVersion
) {
}
