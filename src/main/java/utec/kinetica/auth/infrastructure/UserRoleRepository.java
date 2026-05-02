package utec.kinetica.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.auth.domain.UserRole;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser_Id(Long userId);
}
