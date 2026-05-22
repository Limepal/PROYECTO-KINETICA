package utec.kinetica.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import utec.kinetica.support.PostgresContainerSupport;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointAuthorizationIntegrationTest extends PostgresContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WhenGettingSignsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/signs"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    @Test
    void shouldReturn403WhenCreatingSignWithUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/signs")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER"))))
                        .contentType("application/json")
                        .content("{\"label\":\"hola\",\"mediaRef\":\"m1\",\"locale\":\"es-PE\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenGettingTranslationsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/translations"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    @Test
    void shouldReturn403WhenGettingTranslationsWithoutRoles() throws Exception {
        mockMvc.perform(get("/api/v1/translations")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowGettingTranslationsWhenRoleIsManager() throws Exception {
        mockMvc.perform(get("/api/v1/translations")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("MANAGER")))
                                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenGettingUsersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    @Test
    void shouldReturn403WhenGettingUsersWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowGettingUsersWhenRoleIsManager() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(user("manager@test.com").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenGettingRolesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    @Test
    void shouldReturn403WhenGettingRolesWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowGettingRolesWhenRoleIsManager() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .with(user("manager@test.com").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowCorsPreflightWhenOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void shouldReturn401WhenCallingGlossEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/conversions/es-to-gloss")
                        .contentType("application/json")
                        .content("{\"text\":\"quiero comer arroz\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/conversions/gloss-to-es")
                        .contentType("application/json")
                        .content("{\"text\":\"YO QUERER ARROZ\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
