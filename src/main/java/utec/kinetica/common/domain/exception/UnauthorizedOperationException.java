package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends AppException {
    public UnauthorizedOperationException(String message) {
        super("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }
}
