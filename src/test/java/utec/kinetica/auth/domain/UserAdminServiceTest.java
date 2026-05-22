package utec.kinetica.auth.domain;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import utec.kinetica.auth.infrastructure.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {

    @Test
    void shouldUpdateEmailWhenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAdminService service = new UserAdminService(userRepository);

        User user = new User();
        user.setId(1L);
        user.setEmail("old@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = service.updateEmail(1L, "new@test.com");

        assertEquals("new@test.com", updated.getEmail());
    }

    @Test
    void shouldDeleteWhenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAdminService service = new UserAdminService(userRepository);

        User user = new User();
        user.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        service.delete(3L);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowWhenGetByIdAndUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        UserAdminService service = new UserAdminService(userRepository);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getById(99L));
    }
}
