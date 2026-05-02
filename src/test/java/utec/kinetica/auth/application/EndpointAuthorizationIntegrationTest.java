package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signsShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/signs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signsCreateShouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(post("/signs")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER"))))
                        .contentType("application/json")
                        .content("{\"label\":\"hola\",\"mediaRef\":\"m1\",\"locale\":\"es-PE\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void translationsShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/translations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void translationsShouldReturn403ForAuthenticatedWithoutRoles() throws Exception {
        mockMvc.perform(get("/translations")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersShouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/users")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesShouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolesShouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/roles")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden());
    }
}
