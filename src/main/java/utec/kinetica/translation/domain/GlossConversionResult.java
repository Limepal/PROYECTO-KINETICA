package utec.kinetica.translation.domain;

import java.util.List;

public record GlossConversionResult(
        String outputText,
        double confidence,
        List<String> notes,
        List<String> alternatives,
        List<String> flags,
        String modelVersion
) {
}
