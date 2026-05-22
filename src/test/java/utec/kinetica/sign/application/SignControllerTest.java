package utec.kinetica.sign.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import utec.kinetica.sign.application.dto.SignCreateRequest;
import utec.kinetica.sign.application.dto.SignUpdateRequest;
import utec.kinetica.sign.domain.Sign;
import utec.kinetica.sign.domain.SignService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignControllerTest {

    @Test
    void shouldHandleSignCrudEndpointsWhenCalled() {
        SignService service = mock(SignService.class);
        SignController controller = new SignController(service);

        Sign sign = buildSign(1L, "hola", "hola", "media://1", "es-PE", true);
        when(service.list()).thenReturn(List.of(sign));
        when(service.create("hola", "media://1", "es-PE", true)).thenReturn(sign);
        when(service.getById(1L)).thenReturn(sign);
        when(service.update(1L, "hola", "media://1", "es-PE", true)).thenReturn(sign);

        var list = controller.list();
        assertEquals(1, list.getBody().size());

        var created = controller.create(new SignCreateRequest("hola", "media://1", "es-PE", true));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        var byId = controller.getById(1L);
        assertEquals(1L, byId.getBody().id());

        var updated = controller.update(1L, new SignUpdateRequest("hola", "media://1", "es-PE", true));
        assertEquals(HttpStatus.OK, updated.getStatusCode());

        var deleted = controller.delete(1L);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        verify(service).delete(1L);
    }

    private static Sign buildSign(Long id, String label, String normalizedLabel, String mediaRef, String locale, boolean active) {
        Sign sign = new Sign();
        sign.setId(id);
        sign.setLabel(label);
        sign.setNormalizedLabel(normalizedLabel);
        sign.setMediaRef(mediaRef);
        sign.setLocale(locale);
        sign.setActive(active);
        return sign;
    }
}
