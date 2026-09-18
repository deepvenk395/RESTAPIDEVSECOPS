package com.devsecops.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        @Size(max = 255)
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}