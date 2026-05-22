package utec.kinetica.translation.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import utec.kinetica.translation.application.dto.CreateTranslationRequest;
import utec.kinetica.translation.application.dto.UpdateTranslationRequest;
import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationResult;
import utec.kinetica.translation.domain.TranslationService;
import utec.kinetica.translation.domain.TranslationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationControllerTest {

    @Test
    void shouldHandleCrudFlowWhenTranslationEndpointsAreCalled() {
        TranslationService service = mock(TranslationService.class);
        TranslationController controller = new TranslationController(service);
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("10").build();

        TranslationRequest request = buildRequest(1L, TranslationDirection.TEXT_TO_SIGN, TranslationStatus.PENDING, "hola");
        TranslationResult result = buildResult(request);

        when(service.createRequest(10L, TranslationDirection.TEXT_TO_SIGN, "hola")).thenReturn(request);
        when(service.getRequest(1L, 10L)).thenReturn(request);
        when(service.getResult(1L, 10L)).thenReturn(result);
        when(service.listRequests(10L)).thenReturn(List.of(request));
        when(service.listResultsByRequestIds(List.of(1L))).thenReturn(Map.of(1L, result));
        when(service.updateRequest(1L, 10L, "hola editada")).thenReturn(request);

        var created = controller.create(jwt, new CreateTranslationRequest(TranslationDirection.TEXT_TO_SIGN, "hola"));
        assertEquals(HttpStatus.ACCEPTED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        var byId = controller.getById(jwt, 1L);
        assertEquals(HttpStatus.OK, byId.getStatusCode());
        assertEquals(1L, byId.getBody().requestId());

        var listed = controller.list(jwt);
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertEquals(1, listed.getBody().size());

        var updated = controller.update(jwt, 1L, new UpdateTranslationRequest("hola editada"));
        assertEquals(HttpStatus.OK, updated.getStatusCode());

        var deleted = controller.delete(jwt, 1L);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        verify(service).deleteRequest(1L, 10L);
    }

    private static TranslationRequest buildRequest(Long id, TranslationDirection direction, TranslationStatus status, String sourceText) {
        TranslationRequest request = new TranslationRequest();
        request.setId(id);
        request.setDirection(direction);
        request.setStatus(status);
        request.setSourceText(sourceText);
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        return request;
    }

    private static TranslationResult buildResult(TranslationRequest request) {
        TranslationResult result = new TranslationResult();
        result.setRequest(request);
        result.setTextOutput("texto");
        result.setGlossOutput("GLOSS");
        result.setSignOutputRef("media://1");
        result.setConfidence(0.9);
        result.setModelVersion("v1");
        return result;
    }
}
