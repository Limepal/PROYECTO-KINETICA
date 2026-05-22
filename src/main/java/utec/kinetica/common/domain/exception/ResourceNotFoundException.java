package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
