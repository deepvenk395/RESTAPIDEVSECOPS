package com.devsecops.taskflow.controller;

import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.UserUpdateRequest;
import com.devsecops.taskflow.dto.response.UserResponse;
import com.devsecops.taskflow.service.UserService;
import com.devsecops.taskflow.util.SortUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "User Administration", description = "Admin-only user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "email", "createdAt");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users (paginated)")
    public Page<UserResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        Sort sort = SortUtils.from(sortBy, sortDirection, SORTABLE_FIELDS);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a user (role, enabled, name, email)")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal User operator) {
        return userService.update(id, request, operator);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User operator) {
        userService.delete(id, operator);
    }
}