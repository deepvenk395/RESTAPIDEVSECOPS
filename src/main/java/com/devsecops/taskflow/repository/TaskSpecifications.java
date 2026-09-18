package com.devsecops.taskflow.repository;

import com.devsecops.taskflow.domain.Task;
import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import com.devsecops.taskflow.domain.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> withStatus(TaskStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> withPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null
                ? cb.conjunction()
                : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> createdBy(User user) {
        return (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), user.getId());
    }

    public static Specification<Task> assigneeIs(User user) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), user.getId());
    }

    public static Specification<Task> ownedBy(User user) {
        return (root, query, cb) -> {
            Join<Task, User> creator = root.join("createdBy", JoinType.LEFT);
            Join<Task, User> assignee = root.join("assignee", JoinType.LEFT);
            return cb.or(
                    cb.equal(creator.get("id"), user.getId()),
                    cb.equal(assignee.get("id"), user.getId())
            );
        };
    }
}