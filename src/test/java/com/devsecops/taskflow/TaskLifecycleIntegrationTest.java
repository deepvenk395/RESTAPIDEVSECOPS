package com.devsecops.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskLifecycleIntegrationTest extends BaseIntegrationTest {

    @Test
    void fullTaskLifecycle() throws Exception {
        String admin = adminToken();

        JsonNode carol = register("carol2", "carol2@test.local");
        String carolToken = tokenOf(carol);

        long taskId = createTask(admin, "Pay down tech debt", null);

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pay down tech debt"))
                .andExpect(jsonPath("$.status").value("TODO"));

        mockMvc.perform(post("/api/v1/tasks/{id}/assign", taskId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + userIdOf(carol) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.username").value("carol2"));

        mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + carolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"));

        mockMvc.perform(delete("/api/v1/tasks/{id}/assign", taskId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotTouchAnotherUsersTask() throws Exception {
        String aliceToken = tokenOf(register("alice2", "alice2@test.local"));
        String bobToken = tokenOf(register("bob2", "bob2@test.local"));

        long aliceTask = createTask(aliceToken, "Alice private task", null);

        mockMvc.perform(get("/api/v1/tasks/{id}", aliceTask)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/tasks/{id}", aliceTask)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/tasks/{id}", aliceTask)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIsFilterableAndPaginated() throws Exception {
        String admin = adminToken();
        long taskId = createTask(admin, "Filter me", null);

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + admin)
                        .param("status", "TODO")
                        .param("size", "5")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.content").value(hasSize(org.hamcrest.Matchers.greaterThan(0))));

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nope\",\"status\":\"TODO\",\"priority\":\"LOW\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidEnumAndSortAreRejected() throws Exception {
        String admin = adminToken();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"status\":\"NOT_A_STATUS\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + admin)
                        .param("sortBy", "password; drop table"))
                .andExpect(status().isBadRequest());
    }
}