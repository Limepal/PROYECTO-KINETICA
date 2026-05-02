package utec.kinetica.common.application;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnShortNotFoundMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new EntityNotFoundException("Sign not found: 99"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals("Resource not found.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnShortBadRequestMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Email already registered"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("Request could not be processed.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnShortInternalErrorMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Unexpected server error.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }
}
