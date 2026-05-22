package utec.kinetica.common.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpServletRequest;
import utec.kinetica.common.domain.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    GlobalExceptionHandlerTest() {
        request.setRequestURI("/api/v1/translations/1");
    }

    @Test
    void shouldReturnShortNotFoundMessageWhenEntityIsMissing() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAppException(new ResourceNotFoundException("Sign not found: 99"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals("Sign not found: 99", response.getBody().message());
        assertEquals("/api/v1/translations/1", response.getBody().path());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnShortBadRequestMessageWhenInputIsInvalid() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Email already registered"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("Request could not be processed.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnShortInternalErrorMessageWhenUnexpectedExceptionOccurs() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Unexpected server error.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnServiceUnavailableWhenMailErrorOccurs() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMailFailure(new MailSendException("smtp down"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("EMAIL_SERVICE_ERROR", response.getBody().code());
        assertEquals("Email service is currently unavailable.", response.getBody().message());
        assertFalse(response.getBody().details().isEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenJsonBodyIsMalformed() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotReadable(new HttpMessageNotReadableException("broken json", new MockHttpInputMessage(new byte[0])), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MALFORMED_JSON", response.getBody().code());
        assertEquals("/api/v1/translations/1", response.getBody().path());
    }
}
