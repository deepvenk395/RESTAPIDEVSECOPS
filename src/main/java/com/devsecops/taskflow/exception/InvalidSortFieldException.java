package com.devsecops.taskflow.exception;

public class InvalidSortFieldException extends IllegalArgumentException {
    public InvalidSortFieldException(String message) {
        super(message);
    }
}