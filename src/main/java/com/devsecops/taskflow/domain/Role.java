package com.devsecops.taskflow.domain;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN;

    public String authority() {
        return name();
    }
}