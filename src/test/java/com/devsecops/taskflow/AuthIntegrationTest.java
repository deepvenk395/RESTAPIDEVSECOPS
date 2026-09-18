package com.devsecops.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerReturnsCreatedWithToken() throws Exception {
        JsonNode response = register("carol", "carol@test.local");

        org.assertj.core.api.Assertions.assertThat(tokenOf(response)).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(response.get("user").get("role").asText())
                .isEqualTo("ROLE_USER");
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        register("dave", "dave1@test.local");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"email\":\"dave2@test.local\","
                                + "\"password\":\"Password123!\",\"fullName\":\"Dave\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        register("erin", "erin@test.local");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin2\",\"email\":\"erin@test.local\","
                                + "\"password\":\"Password123!\",\"fullName\":\"Erin\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsWeakPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"email\":\"not-an-email\","
                                + "\"password\":\"short\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        JsonNode registration = register("frank", "frank@test.local");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenOf(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("frank"))
                .andExpect(jsonPath("$.email").value("frank@test.local"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Authentication is required")));
    }

    @Test
    void blockedUserCannotLogin() throws Exception {
        JsonNode user = register("grace", "grace@test.local");
        long userId = userIdOf(user);
        String admin = adminToken();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"grace\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defaultHealthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}