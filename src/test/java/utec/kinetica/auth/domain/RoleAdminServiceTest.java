package utec.kinetica.auth.domain;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAdminServiceTest {

    @Test
    void shouldCreateRoleWhenValidNameProvided() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RoleAdminService service = new RoleAdminService(roleRepository, userRepository, userRoleRepository);

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role created = service.create("admin");

        assertEquals(RoleName.ADMIN, created.getName());
    }

    @Test
    void shouldAssignRoleWhenUserHasNotRoleYet() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RoleAdminService service = new RoleAdminService(roleRepository, userRepository, userRoleRepository);

        User user = new User();
        user.setId(7L);
        Role role = new Role();
        role.setName(RoleName.USER);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUser_Id(7L)).thenReturn(List.of());

        service.assignRole(7L, "user");

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void shouldNotAssignRoleWhenUserAlreadyHasRole() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RoleAdminService service = new RoleAdminService(roleRepository, userRepository, userRoleRepository);

        User user = new User();
        user.setId(9L);
        Role role = new Role();
        role.setName(RoleName.ADMIN);
        UserRole existing = new UserRole();
        existing.setRole(role);

        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUser_Id(9L)).thenReturn(List.of(existing));

        service.assignRole(9L, "admin");

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void shouldThrowWhenAssignRoleAndUserNotFound() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        RoleAdminService service = new RoleAdminService(roleRepository, userRepository, userRoleRepository);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.assignRole(99L, "user"));
    }
}
