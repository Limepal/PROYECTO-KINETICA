package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import utec.kinetica.auth.application.dto.UpdateUserRequest;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.domain.UserAdminService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminControllerTest {

    @Test
    void shouldHandleUserAdminEndpointsWhenCalled() {
        UserAdminService service = mock(UserAdminService.class);
        UserAdminController controller = new UserAdminController(service);

        User user = new User();
        user.setId(3L);
        user.setEmail("user@test.com");
        user.setCreatedAt(Instant.now());

        when(service.list()).thenReturn(List.of(user));
        when(service.getById(3L)).thenReturn(user);
        when(service.updateEmail(3L, "new@test.com")).thenReturn(user);

        assertEquals(1, controller.list().getBody().size());
        assertEquals(3L, controller.getById(3L).getBody().id());
        assertEquals(HttpStatus.OK, controller.updateEmail(3L, new UpdateUserRequest("new@test.com")).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(3L).getStatusCode());
        verify(service).delete(3L);
    }
}
