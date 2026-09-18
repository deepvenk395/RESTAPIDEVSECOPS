package com.devsecops.taskflow.service;

import com.devsecops.taskflow.domain.Task;
import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.TaskCreateRequest;
import com.devsecops.taskflow.dto.request.TaskUpdateRequest;
import com.devsecops.taskflow.dto.response.TaskResponse;
import com.devsecops.taskflow.exception.ResourceNotFoundException;
import com.devsecops.taskflow.repository.TaskRepository;
import com.devsecops.taskflow.repository.TaskSpecifications;
import com.devsecops.taskflow.repository.UserRepository;
import com.devsecops.taskflow.util.SortUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "dueDate", "priority", "title");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public Page<TaskResponse> search(int page, int size, TaskStatus status, TaskPriority priority,
                                     Long assigneeId, String sortBy, String sortDirection, User current) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Sort sort = SortUtils.from(sortBy, sortDirection, SORTABLE_FIELDS);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Specification<Task> spec = Specification
                .where(TaskSpecifications.withStatus(status))
                .and(TaskSpecifications.withPriority(priority));

        if (current.isAdmin()) {
            if (assigneeId != null) {
                spec = spec.and(TaskSpecifications.assigneeIs(getUser(assigneeId)));
            }
        } else {
            spec = spec.and(TaskSpecifications.ownedBy(current));
        }

        return taskRepository.findAll(spec, pageable).map(TaskResponse::from);
    }

    public TaskResponse getById(Long id, User current) {
        Task task = getTask(id);
        if (!canView(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request, User current) {
        Task task = Task.builder()
                .title(request.title().trim())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .dueDate(request.dueDate())
                .createdBy(current)
                .build();
        if (request.assigneeId() != null) {
            task.setAssignee(getUser(request.assigneeId()));
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request, User current) {
        Task task = getTask(id);
        if (!canManage(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus status, User current) {
        Task task = getTask(id);
        if (!canView(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse assign(Long taskId, Long assigneeId, User current) {
        Task task = getTask(taskId);
        if (!canView(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        task.setAssignee(getUser(assigneeId));
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse unassign(Long taskId, User current) {
        Task task = getTask(taskId);
        if (!canView(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        task.setAssignee(null);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id, User current) {
        Task task = getTask(id);
        if (!canManage(task, current)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        taskRepository.delete(task);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    private boolean canManage(Task task, User user) {
        return user.isAdmin() || task.getCreatedBy().getId().equals(user.getId());
    }

    private boolean canView(Task task, User user) {
        return canManage(task, user)
                || (task.getAssignee() != null && task.getAssignee().getId().equals(user.getId()));
    }
}