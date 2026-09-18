package com.devsecops.taskflow.util;

import com.devsecops.taskflow.exception.InvalidSortFieldException;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortUtils {

    private SortUtils() {
    }

    public static Sort from(String sortBy, String sortDirection, Set<String> allowedFields) {
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (field.indexOf(' ') >= 0 || field.indexOf(',') >= 0 || !allowedFields.contains(field)) {
            throw new InvalidSortFieldException("Sort field must be one of: " + String.join(", ", allowedFields));
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}