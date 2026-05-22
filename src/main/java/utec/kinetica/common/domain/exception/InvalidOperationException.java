package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends AppException {
    public InvalidOperationException(String message) {
        super("INVALID_OPERATION", HttpStatus.BAD_REQUEST, message);
    }
}
