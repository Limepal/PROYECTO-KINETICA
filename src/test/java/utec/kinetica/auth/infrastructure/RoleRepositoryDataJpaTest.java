package utec.kinetica.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.Role;
import utec.kinetica.auth.domain.RoleName;
import utec.kinetica.support.PostgresContainerSupport;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldFindByNameWhenRoleExists() {
        roleRepository.findByName(RoleName.ADMIN).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleName.ADMIN);
            return roleRepository.save(role);
        });

        assertTrue(roleRepository.findByName(RoleName.ADMIN).isPresent());
    }

    @Test
    void shouldReturnEmptyWhenFindingByIdAndRoleDoesNotExist() {
        assertTrue(roleRepository.findById(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void shouldDeleteRoleWhenRoleExists() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseGet(() -> {
            Role created = new Role();
            created.setName(RoleName.ADMIN);
            return roleRepository.save(created);
        });

        roleRepository.deleteById(role.getId());

        assertTrue(roleRepository.findById(role.getId()).isEmpty());
    }
}
