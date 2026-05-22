package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import utec.kinetica.auth.application.dto.AssignRoleRequest;
import utec.kinetica.auth.application.dto.CreateRoleRequest;
import utec.kinetica.auth.domain.Role;
import utec.kinetica.auth.domain.RoleAdminService;
import utec.kinetica.auth.domain.RoleName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAdminControllerTest {

    @Test
    void shouldHandleRoleAdminEndpointsWhenCalled() {
        RoleAdminService service = mock(RoleAdminService.class);
        RoleAdminController controller = new RoleAdminController(service);

        Role role = new Role();
        role.setId(5L);
        role.setName(RoleName.MANAGER);

        when(service.list()).thenReturn(List.of(role));
        when(service.getById(5L)).thenReturn(role);
        when(service.create("manager")).thenReturn(role);

        assertEquals(1, controller.list().getBody().size());
        assertEquals("MANAGER", controller.getById(5L).getBody().name());

        var created = controller.create(new CreateRoleRequest("manager"));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        assertEquals(HttpStatus.NO_CONTENT, controller.assignRole(10L, new AssignRoleRequest("manager")).getStatusCode());
        verify(service).assignRole(10L, "manager");

        assertEquals(HttpStatus.NO_CONTENT, controller.delete(5L).getStatusCode());
        verify(service).delete(5L);
    }
}
