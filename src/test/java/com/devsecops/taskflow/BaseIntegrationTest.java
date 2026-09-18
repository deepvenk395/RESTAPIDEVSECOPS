package com.devsecops.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected JsonNode register(String username, String email) throws Exception {
        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"Password123!\","
                + "\"fullName\":\"" + username + " User\"}";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    protected String login(String identifier, String password) throws Exception {
        String body = "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    protected String adminToken() throws Exception {
        return login("admin", "Admin@123456");
    }

    protected String tokenOf(JsonNode authResponse) {
        return authResponse.get("token").asText();
    }

    protected long userIdOf(JsonNode authResponse) {
        return authResponse.get("user").get("id").asLong();
    }

    protected long createTask(String token, String title, String assigneeIdJson) throws Exception {
        String body = "{"
                + "\"title\":\"" + title + "\","
                + "\"description\":\"Integration test task\","
                + "\"status\":\"TODO\","
                + "\"priority\":\"MEDIUM\""
                + (assigneeIdJson == null ? "" : ",\"assigneeId\":" + assigneeIdJson)
                + "}";
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}