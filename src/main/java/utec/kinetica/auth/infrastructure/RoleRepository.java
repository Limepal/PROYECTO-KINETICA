package utec.kinetica.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.auth.domain.Role;
import utec.kinetica.auth.domain.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
