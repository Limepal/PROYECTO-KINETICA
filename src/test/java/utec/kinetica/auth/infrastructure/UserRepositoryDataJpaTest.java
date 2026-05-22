package utec.kinetica.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.User;
import utec.kinetica.support.PostgresContainerSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindByEmailWhenUserExists() {
        User user = new User();
        user.setEmail("repo-user@test.com");
        user.setPasswordHash("hash");
        userRepository.save(user);

        assertTrue(userRepository.findByEmail("repo-user@test.com").isPresent());
    }

    @Test
    void shouldReturnEmptyWhenFindingByEmailAndUserDoesNotExist() {
        assertTrue(userRepository.findByEmail("missing-user@test.com").isEmpty());
    }

    @Test
    void shouldDeleteUserWhenUserExists() {
        User user = new User();
        user.setEmail("repo-delete@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        userRepository.deleteById(user.getId());

        assertEquals(0, userRepository.findById(user.getId()).stream().count());
    }
}
