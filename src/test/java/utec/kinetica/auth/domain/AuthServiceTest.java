package utec.kinetica.auth.domain;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import utec.kinetica.auth.infrastructure.RefreshTokenRepository;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void shouldCreateUserTokenWhenRegisteringWithValidCredentials() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                refreshTokenHasher
        );
        when(refreshTokenHasher.hash(any(String.class))).thenReturn("hash-token");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        Role role = new Role();
        role.setName(RoleName.USER);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
        User saved = new User();
        saved.setId(1L);
        saved.setEmail("a@b.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(any(User.class), any())).thenReturn("jwt");

        AuthTokens response = service.register("a@b.com", "secret");

        assertEquals(1L, response.userId());
        assertEquals("jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void shouldNotFailRegisterWhenWelcomeEmailFails() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                refreshTokenHasher
        );
        when(refreshTokenHasher.hash(any(String.class))).thenReturn("hash-token-2");

        when(userRepository.findByEmail("b@c.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        Role role = new Role();
        role.setName(RoleName.USER);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
        User saved = new User();
        saved.setId(2L);
        saved.setEmail("b@c.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(any(User.class), any())).thenReturn("jwt-2");
        AuthTokens response = service.register("b@c.com", "secret");

        assertEquals(2L, response.userId());
        assertEquals("jwt-2", response.accessToken());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void shouldReturnNewAccessTokenWhenRefreshTokenIsValid() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                refreshTokenHasher
        );
        when(refreshTokenHasher.hash(any(String.class))).thenReturn("refresh-hash");

        User user = new User();
        user.setId(7L);
        user.setEmail("u@test.com");
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(300));

        when(refreshTokenRepository.findActiveTokenForUpdate(any(String.class))).thenReturn(Optional.of(token));
        Role role = new Role();
        role.setName(RoleName.USER);
        UserRole userRole = new UserRole();
        userRole.setRole(role);
        when(userRoleRepository.findByUser_Id(7L)).thenReturn(List.of(userRole));
        when(jwtService.generateToken(any(User.class), any())).thenReturn("new-access");

        AuthTokens response = service.refresh("refresh-raw");

        assertEquals("new-access", response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void shouldRevokeRefreshTokenWhenLogoutIsCalled() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                refreshTokenHasher
        );
        when(refreshTokenHasher.hash(any(String.class))).thenReturn("logout-hash");

        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(token));

        service.logout("refresh-raw");

        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldCreateUserWhenOAuthLoginAndUserDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                eventPublisher,
                refreshTokenHasher
        );
        when(refreshTokenHasher.hash(any(String.class))).thenReturn("oauth-hash-token");

        when(userRepository.findByEmail("oauth@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("oauth-hash");
        Role role = new Role();
        role.setName(RoleName.USER);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));

        User saved = new User();
        saved.setId(33L);
        saved.setEmail("oauth@test.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(any(User.class), any())).thenReturn("oauth-jwt");
        when(userRoleRepository.findByUser_Id(33L)).thenReturn(List.of());

        AuthTokens response = service.loginWithOAuth("oauth@test.com");

        assertEquals(33L, response.userId());
        assertEquals("oauth-jwt", response.accessToken());
        assertEquals("oauth@test.com", response.email());
    }
}
