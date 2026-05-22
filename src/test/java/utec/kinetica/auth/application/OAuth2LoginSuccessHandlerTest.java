package utec.kinetica.auth.application;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import utec.kinetica.auth.domain.AuthService;
import utec.kinetica.auth.domain.AuthTokens;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    @Test
    void shouldSetHttpOnlyCookiesAndRedirectWithoutTokensInQueryWhenOAuth2LoginSucceeds() throws ServletException, IOException {
        AuthService authService = mock(AuthService.class);
        when(authService.loginWithOAuth("oauth@test.com"))
                .thenReturn(new AuthTokens(5L, "oauth@test.com", "access-123", "refresh-123", "Bearer"));

        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
                authService,
                "http://localhost:3000/auth/callback",
                "http://localhost:3000/auth/error"
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "oauth@test.com"),
                "email"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(oauth2User, null, List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        String redirectedUrl = response.getRedirectedUrl();
        assertTrue(redirectedUrl != null && redirectedUrl.contains("oauth=success"));
        assertTrue(!redirectedUrl.contains("accessToken=") && !redirectedUrl.contains("refreshToken="));

        List<String> setCookieHeaders = response.getHeaders("Set-Cookie").stream().toList();
        assertTrue(setCookieHeaders.stream().anyMatch(h -> h.startsWith("kinetica_access_token=") && h.contains("HttpOnly")));
        assertTrue(setCookieHeaders.stream().anyMatch(h -> h.startsWith("kinetica_refresh_token=") && h.contains("HttpOnly")));
    }
}
