package com.devsecops.taskflow.service;

import com.devsecops.taskflow.domain.Role;
import com.devsecops.taskflow.domain.Task;
import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.exception.ResourceNotFoundException;
import com.devsecops.taskflow.repository.TaskRepository;
import com.devsecops.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    private User admin;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);
        admin = user(1L, "admin", Role.ROLE_ADMIN);
        alice = user(2L, "alice", Role.ROLE_USER);
        bob = user(3L, "bob", Role.ROLE_USER);
    }

    private User user(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@taskflow.local");
        user.setPassword("hashed");
        user.setFullName("Test");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Task task(Long id, User createdBy) {
        Task task = Task.builder()
                .id(id)
                .title("Task " + id)
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .createdBy(createdBy)
                .build();
        task.setCreatedAt(java.time.Instant.now());
        task.setUpdatedAt(java.time.Instant.now());
        return task;
    }

    @Test
    void getByIdIsHiddenFromUnrelatedUser() {
        Task task = task(10L, alice);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getById(10L, bob))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdVisibleToAssignee() {
        Task task = task(10L, alice);
        task.setAssignee(bob);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThat(taskService.getById(10L, bob).id()).isEqualTo(10L);
    }

    @Test
    void updateRejectsNonManager() {
        Task task = task(10L, alice);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(10L, null, bob))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteRejectsNonManager() {
        Task task = task(10L, alice);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(10L, bob))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void deleteAllowedForAdmin() {
        Task task = task(10L, alice);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        taskService.delete(10L, admin);

        verify(taskRepository).delete(task);
    }

    @Test
    void createSetsCreatedBy() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new com.devsecops.taskflow.dto.request.TaskCreateRequest(
                "New task", null, TaskStatus.TODO, TaskPriority.LOW, null, null);

        taskService.create(request, alice);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy().getId()).isEqualTo(alice.getId());
        assertThat(captor.getValue().getAssignee()).isNull();
    }

    @Test
    void assigneeCanChangeStatus() {
        Task task = task(10L, alice);
        task.setAssignee(bob);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.changeStatus(10L, TaskStatus.DONE, bob);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void assignRequiresExistingUser() {
        Task task = task(10L, alice);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assign(10L, 99L, admin))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createIgnoresMissingAssigneeByThrowing() {
        var request = new com.devsecops.taskflow.dto.request.TaskCreateRequest(
                "New task", null, TaskStatus.TODO, TaskPriority.LOW, null, 99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(request, alice))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }
}