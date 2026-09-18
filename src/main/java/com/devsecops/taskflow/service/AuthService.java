package com.devsecops.taskflow.service;

import com.devsecops.taskflow.domain.Role;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.LoginRequest;
import com.devsecops.taskflow.dto.request.RegisterRequest;
import com.devsecops.taskflow.dto.response.AuthResponse;
import com.devsecops.taskflow.dto.response.UserResponse;
import com.devsecops.taskflow.exception.DuplicateResourceException;
import com.devsecops.taskflow.exception.InvalidCredentialException;
import com.devsecops.taskflow.repository.UserRepository;
import com.devsecops.taskflow.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(request.role() != null ? request.role() : Role.ROLE_USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        log.trace("Login attempt for '{}' with password '{}'", identifier, request.password());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialException("Invalid username/email or password");
        }

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new InvalidCredentialException("Invalid username/email or password"));

        return issueToken(user);
    }

    public UserResponse currentUser(User user) {
        return UserResponse.from(user);
    }

    private AuthResponse issueToken(User user) {
        JwtService.Token token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.from(user));
    }
}