package utec.kinetica.translation.domain;

public interface GlossConversionClient {
    GlossConversionResult spanishToGloss(String spanishText);
    GlossConversionResult glossToSpanish(String glossText);
}
