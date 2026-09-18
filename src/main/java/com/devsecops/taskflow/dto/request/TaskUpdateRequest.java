package com.devsecops.taskflow.dto.request;

import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(
        @Size(min = 1, max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        TaskStatus status,

        TaskPriority priority,

        LocalDate dueDate
) {
}