package utec.kinetica.translation.domain;

import org.springframework.stereotype.Service;

@Service
public class GlossConversionService {
    private final GlossConversionClient client;

    public GlossConversionService(GlossConversionClient client) {
        this.client = client;
    }

    public GlossConversionResult spanishToGloss(String spanishText) {
        return client.spanishToGloss(spanishText);
    }

    public GlossConversionResult glossToSpanish(String glossText) {
        return client.glossToSpanish(glossText);
    }
}
