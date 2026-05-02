package utec.kinetica.auth.application;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityErrorHandlersTest {

    private final SecurityErrorHandlers handlers = new SecurityErrorHandlers();

    @Test
    void shouldWriteUnauthorizedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlers.commence(null, response, new BadCredentialsException("bad"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"UNAUTHORIZED\""));
    }

    @Test
    void shouldWriteForbiddenEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlers.handle(null, response, new AccessDeniedException("denied"));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"FORBIDDEN\""));
    }
}
