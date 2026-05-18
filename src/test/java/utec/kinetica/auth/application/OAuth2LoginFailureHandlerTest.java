package utec.kinetica.auth.application;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2LoginFailureHandlerTest {

    @Test
    void shouldClearCookiesAndRedirectWithGenericError() throws ServletException, IOException {
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler("http://localhost:3000/auth/error");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad oauth"));

        String redirectedUrl = response.getRedirectedUrl();
        assertTrue(redirectedUrl != null && redirectedUrl.contains("error=oauth_login_failed"));

        List<String> setCookieHeaders = response.getHeaders("Set-Cookie").stream().toList();
        assertTrue(setCookieHeaders.stream().anyMatch(h -> h.startsWith("kinetica_access_token=") && h.contains("Max-Age=0")));
        assertTrue(setCookieHeaders.stream().anyMatch(h -> h.startsWith("kinetica_refresh_token=") && h.contains("Max-Age=0")));
    }
}
