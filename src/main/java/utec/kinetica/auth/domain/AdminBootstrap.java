package utec.kinetica.auth.domain;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;

@Component
public class AdminBootstrap {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${app.security.bootstrap-admin-email:}")
    private String adminEmail;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @PostConstruct
    @Transactional
    public void bootstrapAdmin() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ADMIN);
                    return roleRepository.save(role);
                });

        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            boolean assigned = userRoleRepository.findByUser_Id(user.getId()).stream()
                    .anyMatch(ur -> ur.getRole().getName() == RoleName.ADMIN);
            if (!assigned) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(adminRole);
                userRoleRepository.save(userRole);
            }
        });
    }
}
