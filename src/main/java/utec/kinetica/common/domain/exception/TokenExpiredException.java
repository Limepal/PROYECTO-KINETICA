package utec.kinetica.common.domain.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AppException {
    public TokenExpiredException(String message) {
        super("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, message);
    }
}
