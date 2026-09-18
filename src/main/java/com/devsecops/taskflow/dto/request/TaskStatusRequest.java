package com.devsecops.taskflow.dto.request;

import com.devsecops.taskflow.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}