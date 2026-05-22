package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends AppException {
    public ExternalServiceException(String message) {
        super("EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
