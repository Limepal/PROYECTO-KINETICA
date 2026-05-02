package utec.kinetica.auth.domain;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    @Value("${app.security.jwt-ttl-seconds:3600}")
    private long ttlSeconds;

    @Value("${app.security.jwt-issuer:kinetica}")
    private String issuer;

    @Value("${app.security.jwt-audience:kinetica-clients}")
    private String audience;

    public String generateToken(User user, List<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .audience(List.of(audience))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJWT.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }
}
