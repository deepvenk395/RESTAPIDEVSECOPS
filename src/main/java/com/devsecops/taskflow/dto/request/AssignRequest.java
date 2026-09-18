package com.devsecops.taskflow.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull(message = "Assignee id is required")
        Long assigneeId
) {
}