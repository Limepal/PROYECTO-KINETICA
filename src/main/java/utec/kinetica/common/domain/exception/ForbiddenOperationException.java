package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends AppException {
    public ForbiddenOperationException(String message) {
        super("FORBIDDEN", HttpStatus.FORBIDDEN, message);
    }
}
