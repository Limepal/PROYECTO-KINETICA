package utec.kinetica.auth.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.infrastructure.UserRepository;

import java.util.List;

@Service
public class UserAdminService {
    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Transactional
    public User updateEmail(Long id, String email) {
        User user = getById(id);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getById(id);
        userRepository.delete(user);
    }
}
