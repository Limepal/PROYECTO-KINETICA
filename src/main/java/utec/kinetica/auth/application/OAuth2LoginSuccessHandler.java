package utec.kinetica.auth.application;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.UriComponentsBuilder;
import utec.kinetica.auth.domain.AuthService;
import utec.kinetica.auth.domain.AuthTokens;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final long ACCESS_TOKEN_TTL_SECONDS = 15L * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7L * 24 * 60 * 60;

    private final AuthService authService;
    private final String successRedirectUrl;
    private final String failureRedirectUrl;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            @Value("${app.oauth2.success-redirect:http://localhost:3000/auth/callback}") String successRedirectUrl,
            @Value("${app.oauth2.failure-redirect:http://localhost:3000/auth/error}") String failureRedirectUrl
    ) {
        this.authService = authService;
        this.successRedirectUrl = successRedirectUrl;
        this.failureRedirectUrl = failureRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oauth2User)) {
            response.sendRedirect(failureRedirectUrl + "?error=invalid_principal");
            return;
        }

        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            response.sendRedirect(failureRedirectUrl + "?error=missing_email");
            return;
        }

        AuthTokens tokens = authService.loginWithOAuth(email);

        boolean secure = request.isSecure();
        response.addHeader("Set-Cookie", buildCookie("kinetica_access_token", tokens.accessToken(), ACCESS_TOKEN_TTL_SECONDS, secure));
        response.addHeader("Set-Cookie", buildCookie("kinetica_refresh_token", tokens.refreshToken(), REFRESH_TOKEN_TTL_SECONDS, secure));
        response.addHeader("Set-Cookie", buildCookie("kinetica_token_type", tokens.tokenType(), ACCESS_TOKEN_TTL_SECONDS, secure));
        response.addHeader("Set-Cookie", buildCookie("kinetica_user_id", String.valueOf(tokens.userId()), REFRESH_TOKEN_TTL_SECONDS, secure));
        response.addHeader("Set-Cookie", buildCookie("kinetica_user_email", tokens.email(), REFRESH_TOKEN_TTL_SECONDS, secure));

        String redirect = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("oauth", "success")
                .build(true)
                .toUriString();

        response.sendRedirect(redirect);
    }

    private String buildCookie(String name, String value, long maxAgeSeconds, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }
}
