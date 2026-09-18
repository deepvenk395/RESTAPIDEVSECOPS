package com.devsecops.taskflow.dto.response;

import com.devsecops.taskflow.domain.Task;
import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        UserSummary assignee,
        UserSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                UserSummary.from(task.getAssignee()),
                UserSummary.from(task.getCreatedBy()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}