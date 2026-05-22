package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, message);
    }
}
