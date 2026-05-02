package utec.kinetica.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.auth.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
