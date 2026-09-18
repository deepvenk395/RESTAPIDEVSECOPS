package com.devsecops.taskflow.dto.response;

import com.devsecops.taskflow.domain.User;

public record UserSummary(
        Long id,
        String username,
        String fullName
) {
    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getUsername(), user.getFullName());
    }
}