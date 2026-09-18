package com.devsecops.taskflow.dto.request;

import com.devsecops.taskflow.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 1, max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        Role role,

        Boolean enabled
) {
}