package com.devsecops.taskflow.controller;

import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.AssignRequest;
import com.devsecops.taskflow.dto.request.TaskCreateRequest;
import com.devsecops.taskflow.dto.request.TaskStatusRequest;
import com.devsecops.taskflow.dto.request.TaskUpdateRequest;
import com.devsecops.taskflow.dto.response.TaskResponse;
import com.devsecops.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task CRUD, assignment and status management")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Search tasks",
            description = "Users see tasks they created or are assigned to; admins can filter by any assignee")
    public Page<TaskResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @AuthenticationPrincipal User current) {
        return taskService.search(page, size, status, priority, assigneeId, sortBy, sortDirection, current);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by id")
    public TaskResponse getById(@PathVariable Long id, @AuthenticationPrincipal User current) {
        return taskService.getById(id, current);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task")
    public TaskResponse create(@Valid @RequestBody TaskCreateRequest request, @AuthenticationPrincipal User current) {
        return taskService.create(request, current);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task (creator or admin)")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskUpdateRequest request,
                               @AuthenticationPrincipal User current) {
        return taskService.update(id, request, current);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change task status (creator, assignee or admin)")
    public TaskResponse changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody TaskStatusRequest request,
                                     @AuthenticationPrincipal User current) {
        return taskService.changeStatus(id, request.status(), current);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a task to a user (creator or admin)")
    public TaskResponse assign(@PathVariable Long id,
                               @Valid @RequestBody AssignRequest request,
                               @AuthenticationPrincipal User current) {
        return taskService.assign(id, request.assigneeId(), current);
    }

    @DeleteMapping("/{id}/assign")
    @Operation(summary = "Unassign a task (creator or admin)")
    public TaskResponse unassign(@PathVariable Long id, @AuthenticationPrincipal User current) {
        return taskService.unassign(id, current);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task (creator or admin)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User current) {
        taskService.delete(id, current);
    }
}