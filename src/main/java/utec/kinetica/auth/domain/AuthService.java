package utec.kinetica.auth.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.infrastructure.RefreshTokenRepository;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;
import utec.kinetica.common.domain.exception.DuplicateResourceException;
import utec.kinetica.common.domain.exception.InvalidOperationException;
import utec.kinetica.common.domain.exception.TokenExpiredException;
import utec.kinetica.common.domain.exception.UnauthorizedOperationException;

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
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenHasher refreshTokenHasher;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ApplicationEventPublisher eventPublisher,
            RefreshTokenHasher refreshTokenHasher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.refreshTokenHasher = refreshTokenHasher;
    }

    @Transactional
    public AuthTokens register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Email already registered");
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

        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmail()));

        String token = jwtService.generateToken(saved, List.of("USER"));
        String refresh = issueRefreshToken(saved);
        return new AuthTokens(saved.getId(), saved.getEmail(), token, refresh, "Bearer");
    }

    @Transactional
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedOperationException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedOperationException("Invalid credentials");
        }

        List<String> roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        String token = jwtService.generateToken(user, roles);
        String refresh = issueRefreshToken(user);
        return new AuthTokens(user.getId(), user.getEmail(), token, refresh, "Bearer");
    }

    @Transactional
    public AuthTokens loginWithOAuth(String email) {
        User user = userRepository.findByEmail(email).orElseGet(() -> createOAuthUser(email));
        List<String> roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        if (roles.isEmpty()) {
            Role role = ensureUserRole();
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
            roles = List.of("USER");
        }

        String token = jwtService.generateToken(user, roles);
        String refresh = issueRefreshToken(user);
        return new AuthTokens(user.getId(), user.getEmail(), token, refresh, "Bearer");
    }

    @Transactional
    public AuthTokens refresh(String refreshTokenRaw) {
        String tokenHash = refreshTokenHasher.hash(refreshTokenRaw);
        RefreshToken refreshToken = refreshTokenRepository.findActiveTokenForUpdate(tokenHash)
                .orElseThrow(() -> new UnauthorizedOperationException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        List<String> roles = userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        String accessToken = jwtService.generateToken(user, roles);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        String newRefresh = issueRefreshToken(user);

        return new AuthTokens(user.getId(), user.getEmail(), accessToken, newRefresh, "Bearer");
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        String tokenHash = refreshTokenHasher.hash(refreshTokenRaw);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidOperationException("Invalid refresh token"));
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    private String issueRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(refreshTokenHasher.hash(raw));
        token.setExpiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 7));
        refreshTokenRepository.save(token);
        return raw;
    }

    private Role ensureUserRole() {
        return roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.USER);
                    return roleRepository.save(newRole);
                });
    }

    private User createOAuthUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        User saved = userRepository.save(user);

        Role role = ensureUserRole();
        UserRole userRole = new UserRole();
        userRole.setUser(saved);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
        return saved;
    }

}
