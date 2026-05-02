package utec.kinetica.auth.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.application.dto.AuthResponse;
import utec.kinetica.auth.application.dto.RefreshRequest;
import utec.kinetica.auth.infrastructure.RefreshTokenRepository;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role role = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.USER);
                    return roleRepository.save(newRole);
                });

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        User saved = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(saved);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        String token = jwtService.generateToken(saved, List.of("USER"));
        String refresh = issueRefreshToken(saved);
        return new AuthResponse(saved.getId(), saved.getEmail(), token, refresh, "Bearer");
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        List<String> roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        String token = jwtService.generateToken(user, roles);
        String refresh = issueRefreshToken(user);
        return new AuthResponse(user.getId(), user.getEmail(), token, refresh, "Bearer");
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findActiveTokenForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        List<String> roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        String accessToken = jwtService.generateToken(user, roles);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        String newRefresh = issueRefreshToken(user);

        return new AuthResponse(user.getId(), user.getEmail(), accessToken, newRefresh, "Bearer");
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        String tokenHash = hashToken(refreshTokenRaw);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    private String issueRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(raw));
        token.setExpiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 7));
        refreshTokenRepository.save(token);
        return raw;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
