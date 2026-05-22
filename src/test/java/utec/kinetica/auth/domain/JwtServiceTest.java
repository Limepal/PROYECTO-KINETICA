package utec.kinetica.auth.domain;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    @Test
    void shouldGenerateSignedJwtWhenUserAndRolesAreProvided() throws Exception {
        JwtService service = new JwtService();
        setField(service, "jwtSecret", "12345678901234567890123456789012");
        setField(service, "ttlSeconds", 3600L);
        setField(service, "issuer", "kinetica-test");
        setField(service, "audience", "kinetica-clients");

        User user = new User();
        user.setId(10L);
        user.setEmail("jwt-user@test.com");

        String token = service.generateToken(user, List.of("USER"));
        SignedJWT parsed = SignedJWT.parse(token);

        assertNotNull(parsed.getSignature());
        assertEquals("10", parsed.getJWTClaimsSet().getSubject());
        assertEquals("jwt-user@test.com", parsed.getJWTClaimsSet().getStringClaim("email"));
    }

    @Test
    void shouldThrowWhenGeneratingJwtAndSecretIsTooShort() throws Exception {
        JwtService service = new JwtService();
        setField(service, "jwtSecret", "short-secret");
        setField(service, "ttlSeconds", 3600L);
        setField(service, "issuer", "kinetica-test");
        setField(service, "audience", "kinetica-clients");

        User user = new User();
        user.setId(11L);
        user.setEmail("jwt-short@test.com");

        try {
            service.generateToken(user, List.of("USER"));
        } catch (IllegalStateException ex) {
            assertNotNull(ex.getMessage());
            return;
        }
        throw new AssertionError("Expected IllegalStateException for short secret");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
