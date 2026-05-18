package utec.kinetica.auth.application;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private final String failureRedirectUrl;

    public OAuth2LoginFailureHandler(@Value("${app.oauth2.failure-redirect:http://localhost:3000/auth/error}") String failureRedirectUrl) {
        this.failureRedirectUrl = failureRedirectUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        boolean secure = request.isSecure();
        response.addHeader("Set-Cookie", clearCookie("kinetica_access_token", secure));
        response.addHeader("Set-Cookie", clearCookie("kinetica_refresh_token", secure));
        response.addHeader("Set-Cookie", clearCookie("kinetica_token_type", secure));
        response.addHeader("Set-Cookie", clearCookie("kinetica_user_id", secure));
        response.addHeader("Set-Cookie", clearCookie("kinetica_user_email", secure));

        String redirect = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", "oauth_login_failed")
                .build(true)
                .toUriString();
        response.sendRedirect(redirect);
    }

    private String clearCookie(String name, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build()
                .toString();
    }
}
