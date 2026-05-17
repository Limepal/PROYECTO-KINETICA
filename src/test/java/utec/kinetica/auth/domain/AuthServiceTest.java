package utec.kinetica.auth.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import utec.kinetica.auth.application.dto.AuthResponse;
import utec.kinetica.auth.application.dto.RefreshRequest;
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
    void registerCreatesUserToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        RegistrationNotifier registrationNotifier = mock(RegistrationNotifier.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                registrationNotifier
        );

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

        AuthResponse response = service.register("a@b.com", "secret");

        assertEquals(1L, response.userId());
        assertEquals("jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(registrationNotifier).notifyWelcome("a@b.com");
    }

    @Test
    void registerShouldNotFailWhenWelcomeEmailFails() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        RegistrationNotifier registrationNotifier = mock(RegistrationNotifier.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                registrationNotifier
        );

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
        org.mockito.Mockito.doThrow(new RuntimeException("smtp failed")).when(registrationNotifier).notifyWelcome("b@c.com");

        AuthResponse response = service.register("b@c.com", "secret");

        assertEquals(2L, response.userId());
        assertEquals("jwt-2", response.accessToken());
    }

    @Test
    void refreshReturnsNewAccessToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        RegistrationNotifier registrationNotifier = mock(RegistrationNotifier.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                registrationNotifier
        );

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

        AuthResponse response = service.refresh(new RefreshRequest("refresh-raw"));

        assertEquals("new-access", response.accessToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void logoutRevokesRefreshToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        RegistrationNotifier registrationNotifier = mock(RegistrationNotifier.class);

        AuthService service = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                registrationNotifier
        );

        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(token));

        service.logout("refresh-raw");

        verify(refreshTokenRepository).save(token);
    }
}
