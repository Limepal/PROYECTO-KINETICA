package utec.kinetica.translation.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import utec.kinetica.translation.domain.GlossConversionClient;
import utec.kinetica.translation.domain.GlossConversionResult;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.gloss.provider", havingValue = "stub", matchIfMissing = true)
public class StubGlossConversionClient implements GlossConversionClient {
    @Override
    public GlossConversionResult spanishToGloss(String spanishText) {
        return new GlossConversionResult(
                "YO QUERER ARROZ",
                0.72,
                List.of("stub-response"),
                List.of(),
                List.of(),
                "stub-gloss-v1"
        );
    }

    @Override
    public GlossConversionResult glossToSpanish(String glossText) {
        return new GlossConversionResult(
                "Yo quiero arroz.",
                0.72,
                List.of("stub-response"),
                List.of(),
                List.of(),
                "stub-gloss-v1"
        );
    }
}
