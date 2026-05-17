package utec.kinetica.translation.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import utec.kinetica.translation.infrastructure.OutboxEventRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;
import utec.kinetica.translation.infrastructure.TranslationResultRepository;

import java.time.Instant;

@Component
public class TranslationRequestedEventHandler {
    private final TranslationRequestRepository requestRepository;
    private final TranslationResultRepository resultRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AiInferenceClient aiInferenceClient;
    private final GlossConversionService glossConversionService;

    public TranslationRequestedEventHandler(
            TranslationRequestRepository requestRepository,
            TranslationResultRepository resultRepository,
            OutboxEventRepository outboxEventRepository,
            AiInferenceClient aiInferenceClient,
            GlossConversionService glossConversionService
    ) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.aiInferenceClient = aiInferenceClient;
        this.glossConversionService = glossConversionService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationRequested(TranslationRequestedEvent event) {
        outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
            outbox.setStatus("PROCESSING");
            outbox.setLastError(null);
            outboxEventRepository.save(outbox);
        });
        try {
            TranslationRequest request = requestRepository.findById(event.requestId())
                    .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + event.requestId()));

            ProcessingOutcome outcome = request.getDirection() == TranslationDirection.SIGN_TO_TEXT
                    ? processSignToText(request)
                    : processTextToSign(request);

            TranslationResult result = new TranslationResult();
            result.setRequest(request);
            result.setTextOutput(outcome.aiResponse().textOutput());
            result.setGlossOutput(outcome.glossOutput());
            result.setSignOutputRef(outcome.aiResponse().signOutputRef());
            result.setConfidence(outcome.aiResponse().confidence());
            result.setLatencyMs(outcome.aiResponse().latencyMs());
            result.setModelVersion(outcome.aiResponse().modelVersion());
            result.setWarning(mergeWarnings(outcome.aiResponse().warning(), outcome.glossWarning()));
            resultRepository.save(result);

            request.setStatus(outcome.aiResponse().status());
            requestRepository.save(request);

            outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
                outbox.setStatus("PROCESSED");
                outbox.setNextRetryAt(null);
                outbox.setLastError(null);
                outboxEventRepository.save(outbox);
            });
        } catch (Exception ex) {
            outboxEventRepository.findById(event.outboxId()).ifPresent(outbox -> {
                int retryCount = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount();
                retryCount++;
                outbox.setStatus("FAILED");
                outbox.setRetryCount(retryCount);
                outbox.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                outbox.setNextRetryAt(calculateNextRetryAt(retryCount));
                outboxEventRepository.save(outbox);
            });
            throw ex;
        }
    }

    private Instant calculateNextRetryAt(int retryCount) {
        long baseDelaySeconds = 15L;
        long factor = 1L << Math.min(Math.max(retryCount - 1, 0), 6);
        return Instant.now().plusSeconds(baseDelaySeconds * factor);
    }

    private ProcessingOutcome processTextToSign(TranslationRequest request) {
        GlossConversionResult gloss = glossConversionService.spanishToGloss(request.getSourceText());
        String glossInput = gloss.outputText() == null || gloss.outputText().isBlank() ? request.getSourceText() : gloss.outputText();
        AiInferenceResponse response = aiInferenceClient.translateTextToSign(request.getId(), glossInput);
        String glossWarning = firstFlag(gloss.flags());
        return new ProcessingOutcome(response, glossInput, glossWarning);
    }

    private ProcessingOutcome processSignToText(TranslationRequest request) {
        AiInferenceResponse response = aiInferenceClient.translateSignToText(request.getId(), request.getSourceText());
        String candidate = response.textOutput();
        if (candidate == null || candidate.isBlank()) {
            return new ProcessingOutcome(response, null, null);
        }
        if (looksLikeGloss(candidate)) {
            GlossConversionResult conversion = glossConversionService.glossToSpanish(candidate);
            AiInferenceResponse normalized = new AiInferenceResponse(
                    response.status(),
                    conversion.outputText(),
                    response.signOutputRef(),
                    response.confidence(),
                    response.latencyMs(),
                    response.modelVersion(),
                    response.warning()
            );
            return new ProcessingOutcome(normalized, candidate, firstFlag(conversion.flags()));
        }
        return new ProcessingOutcome(response, candidate, null);
    }

    private boolean looksLikeGloss(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lettersOnly = trimmed.replaceAll("[^A-Za-zÁÉÍÓÚÑáéíóúñ ]", "").trim();
        if (lettersOnly.isEmpty()) {
            return false;
        }
        String[] tokens = lettersOnly.split("\\s+");
        if (tokens.length < 2) {
            return false;
        }
        int uppercaseTokens = 0;
        for (String token : tokens) {
            if (!token.isBlank() && token.equals(token.toUpperCase())) {
                uppercaseTokens++;
            }
        }
        return uppercaseTokens >= Math.max(2, tokens.length - 1);
    }

    private String mergeWarnings(String aiWarning, String glossWarning) {
        if (aiWarning == null || aiWarning.isBlank()) {
            return glossWarning;
        }
        if (glossWarning == null || glossWarning.isBlank()) {
            return aiWarning;
        }
        return aiWarning + ";" + glossWarning;
    }

    private String firstFlag(java.util.List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return null;
        }
        String value = flags.get(0);
        return value == null || value.isBlank() ? null : value;
    }

    private record ProcessingOutcome(
            AiInferenceResponse aiResponse,
            String glossOutput,
            String glossWarning
    ) {
    }
}
