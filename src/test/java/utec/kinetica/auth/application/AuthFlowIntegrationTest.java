package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import utec.kinetica.auth.domain.RefreshToken;
import utec.kinetica.auth.infrastructure.RefreshTokenRepository;
import utec.kinetica.support.PostgresContainerSupport;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest extends PostgresContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void shouldHandleRegisterLoginRefreshAndLogoutWhenFlowIsValid() throws Exception {
        HttpResponse weakPassword = postJson("/api/v1/auth/register", "{\"email\":\"weak@test.com\",\"password\":\"abcdefg\"}", null);
        assertTrue(weakPassword.status == 400);
        assertTrue(weakPassword.body.contains("\"code\":\"VALIDATION_ERROR\""));

        String email = "flow-" + System.nanoTime() + "@test.com";
        String password = "Secret-123!";

        HttpResponse register = postJson("/api/v1/auth/register", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null);
        assertTrue(register.status == 201);

        HttpResponse login = postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null);
        assertTrue(login.status == 200);
        String accessToken = extractJsonString(login.body, "accessToken");
        String refreshToken = extractJsonString(login.body, "refreshToken");

        HttpResponse refresh = postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}", null);
        assertTrue(refresh.status == 200);
        String rotatedRefresh = extractJsonString(refresh.body, "refreshToken");

        HttpResponse logout = postJson("/api/v1/auth/logout", "{\"refreshToken\":\"" + rotatedRefresh + "\"}", accessToken);
        assertTrue(logout.status == 204);

        HttpResponse revokedRefresh = postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + rotatedRefresh + "\"}", null);
        assertTrue(revokedRefresh.status == 401);
        assertTrue(revokedRefresh.body.contains("\"code\":\"UNAUTHORIZED\""));

        HttpResponse invalidRefresh = postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"invalid-token-value\"}", null);
        assertTrue(invalidRefresh.status == 401);
        assertTrue(invalidRefresh.body.contains("\"code\":\"UNAUTHORIZED\""));

        HttpResponse secondLogin = postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null);
        assertTrue(secondLogin.status == 200);
        String expiringRefresh = extractJsonString(secondLogin.body, "refreshToken");

        RefreshToken token = refreshTokenRepository.findByTokenHash(hashToken(expiringRefresh)).orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(token);

        HttpResponse expiredRefresh = postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + expiringRefresh + "\"}", null);
        assertTrue(expiredRefresh.status == 401);
        assertTrue(expiredRefresh.body.contains("\"code\":\"TOKEN_EXPIRED\""));
    }

    private HttpResponse postJson(String path, String body, String bearerToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create("http://localhost:" + port + path).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String responseBody = "";
        if (stream != null) {
            responseBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            stream.close();
        }
        return new HttpResponse(status, responseBody);
    }

    private String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart, valueEnd);
    }

    private record HttpResponse(int status, String body) {}

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
