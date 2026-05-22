package utec.kinetica.common.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import utec.kinetica.common.domain.exception.AppException;
import utec.kinetica.common.domain.exception.ConflictException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException exception, HttpServletRequest request) {
        return build(exception.getStatus(), exception.getCode(), exception.getMessage(), List.of(exception.getMessage()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request data.", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request body.", List.of("Request body could not be parsed."), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.", List.of("Unexpected internal failure."), request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied.", List.of("You do not have permission to access this resource."), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request could not be processed.", List.of("Invalid request."), request);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiErrorResponse> handleMailFailure(MailException exception, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SERVICE_ERROR", "Email service is currently unavailable.", List.of("Could not send notification email at this time."), request);
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", "Resource conflict detected.", List.of("Operation violates a uniqueness or integrity constraint."), request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, List<String> details, HttpServletRequest request) {
        ApiErrorResponse apiError = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                details,
                request != null ? request.getRequestURI() : "",
                Instant.now()
        );
        return ResponseEntity.status(status).body(apiError);
    }
}
