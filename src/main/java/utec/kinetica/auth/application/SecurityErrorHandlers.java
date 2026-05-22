package utec.kinetica.auth.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.", "Unauthorized request.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "You do not have permission.", "Access denied.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String error = status == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Forbidden";
        String path = request != null ? request.getRequestURI() : "";
        String json = "{\"code\":\"" + escape(code) + "\"," 
                + "\"status\":" + status + ","
                + "\"error\":\"" + escape(error) + "\"," 
                + "\"message\":\"" + escape(message) + "\"," 
                + "\"details\":[\"" + escape(detail) + "\"],"
                + "\"path\":\"" + escape(path) + "\"," 
                + "\"timestamp\":\"" + Instant.now() + "\"}";
        response.getOutputStream().write(json.getBytes());
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
