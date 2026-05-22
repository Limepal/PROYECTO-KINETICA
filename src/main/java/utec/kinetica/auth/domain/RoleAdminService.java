package utec.kinetica.auth.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.auth.infrastructure.RoleRepository;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.auth.infrastructure.UserRoleRepository;

import java.util.List;

@Service
public class RoleAdminService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleAdminService(RoleRepository roleRepository, UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<Role> list() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Role create(String name) {
        Role role = new Role();
        role.setName(RoleName.valueOf(name.toUpperCase()));
        return roleRepository.save(role);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(Long id) {
        Role role = getById(id);
        roleRepository.delete(role);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Role role = roleRepository.findByName(RoleName.valueOf(roleName.toUpperCase()))
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        boolean alreadyAssigned = userRoleRepository.findByUser_Id(userId).stream()
                .anyMatch(ur -> ur.getRole().getName() == role.getName());
        if (!alreadyAssigned) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }
}
