package com.devsecops.taskflow.dto.response;

import com.devsecops.taskflow.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().authority(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}