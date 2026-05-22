package utec.kinetica.auth.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.auth.application.dto.AuthResponse;
import utec.kinetica.auth.application.dto.LoginRequest;
import utec.kinetica.auth.application.dto.RefreshRequest;
import utec.kinetica.auth.application.dto.RegisterRequest;
import utec.kinetica.auth.domain.AuthService;
import utec.kinetica.auth.domain.AuthTokens;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/users/" + tokens.userId()))
                .body(toResponse(tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(toResponse(authService.login(request.email(), request.password())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(toResponse(authService.refresh(request.refreshToken())));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(AuthTokens tokens) {
        return new AuthResponse(
                tokens.userId(),
                tokens.email(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType()
        );
    }
}
