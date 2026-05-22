package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import utec.kinetica.support.PostgresContainerSupport;

import java.net.HttpURLConnection;
import java.net.URI;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityErrorEnvelopeIntegrationTest extends PostgresContainerSupport {

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnApiErrorEnvelopeWhenUnauthorizedRequestIsSent() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create("http://localhost:" + port + "/api/v1/translations").toURL().openConnection();
        conn.setRequestMethod("GET");
        int status = conn.getResponseCode();
        String body;
        try (InputStream errorStream = conn.getErrorStream(); Scanner scanner = new Scanner(errorStream)) {
            body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }

        assertEquals(401, status);
        assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""));
    }

}
