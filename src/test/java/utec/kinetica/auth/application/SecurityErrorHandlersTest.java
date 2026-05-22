package utec.kinetica.auth.application;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityErrorHandlersTest {

    private final SecurityErrorHandlers handlers = new SecurityErrorHandlers();

    @Test
    void shouldWriteUnauthorizedEnvelopeWhenAuthenticationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/translations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlers.commence(request, response, new BadCredentialsException("bad"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"UNAUTHORIZED\""));
        assertTrue(response.getContentAsString().contains("\"status\":401"));
        assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/translations\""));
    }

    @Test
    void shouldWriteForbiddenEnvelopeWhenAccessIsDenied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handlers.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"FORBIDDEN\""));
        assertTrue(response.getContentAsString().contains("\"status\":403"));
        assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/users\""));
    }
}
