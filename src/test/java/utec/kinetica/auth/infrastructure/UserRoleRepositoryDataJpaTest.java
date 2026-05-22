package utec.kinetica.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.Role;
import utec.kinetica.auth.domain.RoleName;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.domain.UserRole;
import utec.kinetica.support.PostgresContainerSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRoleRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void shouldFindByUserIdWhenRolesAreAssigned() {
        User user = new User();
        user.setEmail("user-role@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        Role role = roleRepository.findByName(RoleName.USER).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(RoleName.USER);
            return roleRepository.save(newRole);
        });

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        assertEquals(1, userRoleRepository.findByUser_Id(user.getId()).size());
    }

    @Test
    void shouldReturnEmptyWhenFindingByUserIdAndUserHasNoRoles() {
        User user = new User();
        user.setEmail("user-no-role@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        assertEquals(0, userRoleRepository.findByUser_Id(user.getId()).size());
    }
}
