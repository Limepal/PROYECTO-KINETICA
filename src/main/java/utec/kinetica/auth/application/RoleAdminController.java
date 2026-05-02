package utec.kinetica.auth.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utec.kinetica.auth.application.dto.AssignRoleRequest;
import utec.kinetica.auth.application.dto.CreateRoleRequest;
import utec.kinetica.auth.application.dto.RoleResponse;
import utec.kinetica.auth.domain.Role;
import utec.kinetica.auth.domain.RoleAdminService;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleAdminController {
    private final RoleAdminService roleAdminService;

    public RoleAdminController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleAdminService.list().stream().map(this::toResponse).toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(roleAdminService.getById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleAdminService.create(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(role));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public ResponseEntity<Void> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        roleAdminService.assignRole(request.userId(), request.roleName());
        return ResponseEntity.noContent().build();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName().name());
    }
}
