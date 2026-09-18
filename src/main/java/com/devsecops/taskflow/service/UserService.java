package com.devsecops.taskflow.service;

import com.devsecops.taskflow.domain.Role;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.UserUpdateRequest;
import com.devsecops.taskflow.dto.response.UserResponse;
import com.devsecops.taskflow.exception.DuplicateResourceException;
import com.devsecops.taskflow.exception.ResourceNotFoundException;
import com.devsecops.taskflow.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, User operator) {
        User user = getUser(id);

        if (request.role() != null) {
            if (user.getId().equals(operator.getId())) {
                throw new IllegalStateException("You cannot change your own role");
            }
            user.setRole(request.role());
        }

        if (request.enabled() != null) {
            if (user.getId().equals(operator.getId()) && !request.enabled()) {
                throw new IllegalStateException("You cannot disable your own account");
            }
            user.setEnabled(request.enabled());
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new DuplicateResourceException("Email is already registered");
            }
            user.setEmail(email);
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, User operator) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}